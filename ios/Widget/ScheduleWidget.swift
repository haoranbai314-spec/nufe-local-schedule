import SwiftUI
import WidgetKit

struct ScheduleEntry: TimelineEntry {
    let date: Date
    let schedule: Schedule?
    var preview = false
}
struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> ScheduleEntry { ScheduleEntry(date: Date(), schedule: nil, preview: true) }
    func getSnapshot(in context: Context, completion: @escaping (ScheduleEntry) -> Void) { completion(ScheduleEntry(date: Date(), schedule: try? ScheduleDisk.load(), preview: context.isPreview)) }
    func getTimeline(in context: Context, completion: @escaping (Timeline<ScheduleEntry>) -> Void) {
        let now = Date(), schedule = try? ScheduleDisk.load()
        var dates = [now, SchoolClock.adding(1, to: SchoolClock.day(now))]
        if let schedule { for lesson in schedule.on(now) { let start = SchoolClock.time(lesson.start, on: now), end = SchoolClock.time(lesson.end, on: now); if start > now { dates.append(start) }; if end > now { dates.append(end) } } }
        let entries = Set(dates).sorted().map { ScheduleEntry(date: $0, schedule: schedule) }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

struct ScheduleWidgetView: View {
    let entry: ScheduleEntry
    let nextOnly: Bool
    @Environment(\.widgetFamily) private var family
    private var limit: Int { nextOnly || family == .systemSmall ? 1 : family == .systemLarge ? 5 : 2 }
    private var lessons: [Lesson] { guard let schedule = entry.schedule else { return [] }; return nextOnly ? schedule.upcoming(at: entry.date).map { [$0] } ?? [] : schedule.on(entry.date).filter { SchoolClock.time($0.end, on: entry.date) > entry.date } }
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack { Text(nextOnly ? "下一节课" : "今天课程").font(.headline); Spacer(); Text(SchoolClock.label(entry.date)).font(.caption).foregroundStyle(.secondary) }
            if entry.schedule == nil { Text(entry.preview ? "南财课表" : "打开应用导入课表").font(.headline); Text("本地保存 · 离线查看").font(.caption).foregroundStyle(.secondary) }
            else if lessons.isEmpty { Text("今天没有后续课程").font(.subheadline).foregroundStyle(.secondary) }
            else {
                ForEach(Array(lessons.prefix(limit))) { lesson in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(lesson.name).font(.subheadline.bold()).lineLimit(2)
                        Text("\(lesson.start) — \(lesson.end)").font(.caption.monospacedDigit())
                        if !lesson.room.isEmpty { Text(lesson.room).font(.caption).foregroundStyle(.secondary).lineLimit(1) }
                        if nextOnly, SchoolClock.time(lesson.start, on: entry.date) > entry.date { HStack(spacing: 3) { Text("距上课"); Text(SchoolClock.time(lesson.start, on: entry.date), style: .timer) }.font(.caption).foregroundStyle(.indigo) }
                    }
                }
                if lessons.count > limit { Text("另有 \(lessons.count-limit) 节 · 点开查看").font(.caption2).foregroundStyle(.secondary) }
            }
            Spacer(minLength: 0)
        }.widgetURL(URL(string: "nufeschedule://today")).containerBackground(.background, for: .widget)
    }
}

struct TodayWidget: Widget {
    let kind = "NufeToday"
    var body: some WidgetConfiguration { StaticConfiguration(kind: kind, provider: Provider()) { ScheduleWidgetView(entry: $0, nextOnly: false) }.configurationDisplayName("今天课程").description("查看今天尚未结束的课程，点击打开完整课表。").supportedFamilies([.systemSmall, .systemMedium, .systemLarge]) }
}
struct NextWidget: Widget {
    let kind = "NufeNext"
    var body: some WidgetConfiguration { StaticConfiguration(kind: kind, provider: Provider()) { ScheduleWidgetView(entry: $0, nextOnly: true) }.configurationDisplayName("下一节课").description("显示今天下一节课程、教室与倒计时。").supportedFamilies([.systemSmall, .systemMedium]) }
}
@main struct NufeWidgets: WidgetBundle { var body: some Widget { TodayWidget(); NextWidget() } }
