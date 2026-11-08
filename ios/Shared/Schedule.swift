import Foundation

enum SchoolClock {
    static var calendar: Calendar { var c = Calendar(identifier: .gregorian); c.timeZone = TimeZone(identifier: "Asia/Shanghai")!; c.firstWeekday = 2; return c }
    static func day(_ date: Date) -> Date { calendar.startOfDay(for: date) }
    static func monday(_ date: Date) -> Date { calendar.date(byAdding: .day, value: -(calendar.component(.weekday, from: date) + 5) % 7, to: day(date))! }
    static func adding(_ days: Int, to date: Date) -> Date { calendar.date(byAdding: .day, value: days, to: date)! }
    static func parseDay(_ text: String) -> Date? { let f = DateFormatter(); f.calendar = calendar; f.locale = Locale(identifier: "en_US_POSIX"); f.timeZone = calendar.timeZone; f.dateFormat = "yyyy-MM-dd"; f.isLenient = false; return f.date(from: text) }
    static func label(_ date: Date, _ format: String = "M/d") -> String { let f = DateFormatter(); f.calendar = calendar; f.locale = Locale(identifier: "zh_CN"); f.timeZone = calendar.timeZone; f.dateFormat = format; return f.string(from: date) }
    static func minutes(_ text: String) -> Int? { let p = text.split(separator: ":"); guard p.count == 2, let h = Int(p[0]), let m = Int(p[1]), (0...23).contains(h), (0...59).contains(m) else { return nil }; return h * 60 + m }
    static func time(_ text: String, on date: Date) -> Date { calendar.date(byAdding: .minute, value: minutes(text) ?? 0, to: day(date))! }
}

struct Lesson: Codable, Identifiable, Hashable {
    var id: String
    var name: String
    var room: String
    var teacher: String
    var start: String
    var end: String
    var weekday: Int
    var weeks: [Int]
}

struct Schedule: Codable {
    var semester: String
    var weekOne: Date
    var lessons: [Lesson]
    var savedAt: Date
    func week(on date: Date) -> Int { Int(floor(Double(SchoolClock.calendar.dateComponents([.day], from: SchoolClock.day(weekOne), to: SchoolClock.day(date)).day ?? 0) / 7)) + 1 }
    func on(_ date: Date) -> [Lesson] { let weekday = (SchoolClock.calendar.component(.weekday, from: date) + 5) % 7 + 1; return lessons.filter { $0.weekday == weekday && $0.weeks.contains(week(on: date)) }.sorted { ($0.start, $0.name) < ($1.start, $1.name) } }
    func upcoming(at date: Date) -> Lesson? { on(date).first { SchoolClock.time($0.start, on: date) > date } }
    static func imported(_ raw: Data, weekOne: Date) throws -> Schedule {
        guard raw.count <= 2_097_152, let root = try JSONSerialization.jsonObject(with: raw) as? [String: Any], let activities = root["activities"] as? [[String: Any]], !activities.isEmpty, activities.count <= 2000 else { throw ImportError.invalid("没有找到有效课程数据") }
        guard SchoolClock.calendar.component(.weekday, from: weekOne) == 2 else { throw ImportError.invalid("第一周起始日必须是周一") }
        var starts: [Int: String] = [:], ends: [Int: String] = [:]
        for a in activities { if let s = a["startTime"] as? String, SchoolClock.minutes(s) != nil, let u = a["startUnit"] as? Int { starts[u] = s }; if let s = a["endTime"] as? String, SchoolClock.minutes(s) != nil, let u = a["endUnit"] as? Int { ends[u] = s } }
        var lessons: [Lesson] = []
        for (index, a) in activities.enumerated() {
            let start = (a["startTime"] as? String).flatMap { SchoolClock.minutes($0) == nil ? nil : $0 } ?? starts[a["startUnit"] as? Int ?? 0] ?? ""
            let end = (a["endTime"] as? String).flatMap { SchoolClock.minutes($0) == nil ? nil : $0 } ?? ends[a["endUnit"] as? Int ?? 0] ?? ""
            let name = (a["courseName"] as? String ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
            let weeks = (a["weekIndexes"] as? [Int] ?? []).filter { (1...60).contains($0) }
            guard !name.isEmpty, let weekday = a["weekday"] as? Int, (1...7).contains(weekday), !weeks.isEmpty, let s = SchoolClock.minutes(start), let e = SchoolClock.minutes(end), e > s else { throw ImportError.invalid("第 \(index + 1) 条课程缺少时间、星期或周次，未覆盖原课表") }
            let teachers = (a["teachers"] as? [Any] ?? []).compactMap { value -> String? in if let s = value as? String { return s }; let d = value as? [String: Any]; return d?["name"] as? String ?? d?["teacherName"] as? String }
            lessons.append(Lesson(id: String(index), name: name, room: a["room"] as? String ?? a["building"] as? String ?? "", teacher: teachers.joined(separator: "、"), start: start, end: end, weekday: weekday, weeks: weeks))
        }
        return Schedule(semester: root["semester_name"] as? String ?? "学校课表", weekOne: SchoolClock.day(weekOne), lessons: lessons, savedAt: Date())
    }
}

enum ImportError: LocalizedError {
    case invalid(String)
    var errorDescription: String? { if case .invalid(let text) = self { return text }; return nil }
}

enum ScheduleDisk {
    static func url() throws -> URL {
        #if FREE_SIGNING
        let container = try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
        return container.appendingPathComponent("schedule.json")
        #else
        guard let group = Bundle.main.object(forInfoDictionaryKey: "ScheduleAppGroup") as? String, let container = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group) else { throw ImportError.invalid("应用共享空间不可用，请检查 App Groups 签名配置") }
        return container.appendingPathComponent("schedule.json")
        #endif
    }
    static func load() throws -> Schedule? { let path = try url(); guard FileManager.default.fileExists(atPath: path.path) else { return nil }; return try JSONDecoder().decode(Schedule.self, from: Data(contentsOf: path)) }
    static func save(_ schedule: Schedule) throws { try JSONEncoder().encode(schedule).write(to: url(), options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication]) }
    static func remove() throws { let path = try url(); if FileManager.default.fileExists(atPath: path.path) { try FileManager.default.removeItem(at: path) } }
}
