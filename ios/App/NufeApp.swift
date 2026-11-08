import SwiftUI
import WidgetKit

@MainActor final class ScheduleStore: ObservableObject {
    @Published var schedule: Schedule?
    @Published var error: String?
    init() { reload() }
    func reload() { do { schedule = try ScheduleDisk.load() } catch { self.error = error.localizedDescription } }
    private func reloadWidgets() {
        #if !FREE_SIGNING
        WidgetCenter.shared.reloadAllTimelines()
        #endif
    }
    func save(_ value: Schedule) throws { try ScheduleDisk.save(value); schedule = value; reloadWidgets() }
    func clear() { do { try ScheduleDisk.remove(); schedule = nil; reloadWidgets() } catch { self.error = error.localizedDescription } }
}

@main struct NufeApp: App {
    @StateObject private var store = ScheduleStore()
    var body: some Scene { WindowGroup { ContentView().environmentObject(store).tint(.indigo) } }
}

struct ContentView: View {
    @EnvironmentObject var store: ScheduleStore
    @State private var selected = Date()
    @State private var importing = false
    @State private var settings = false
    @State private var detail: Lesson?
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text(store.schedule.map { "\($0.semester) · \($0.lessons.count) 条排课 · 本地保存" } ?? "把这一学期，收在手机里。")
                        .font(.subheadline).foregroundStyle(.secondary).frame(minHeight: 42, alignment: .center)
                    Button { importing = true } label: { Label(store.schedule == nil ? "登录教务导入" : "更新课表", systemImage: "arrow.down.circle.fill").frame(maxWidth: .infinity, minHeight: 48) }.buttonStyle(.borderedProminent)
                    if let schedule = store.schedule {
                        HStack {
                            Button { selected = SchoolClock.adding(-7, to: selected) } label: { Image(systemName: "chevron.left") }.accessibilityLabel("上一周")
                            Spacer()
                            Button("第 \(schedule.week(on: selected)) 周 · 回本周") { selected = Date() }
                            Spacer()
                            Button { selected = SchoolClock.adding(7, to: selected) } label: { Image(systemName: "chevron.right") }.accessibilityLabel("下一周")
                        }.buttonStyle(.bordered)
                        Text("\(SchoolClock.label(SchoolClock.monday(selected))) — \(SchoolClock.label(SchoolClock.adding(6, to: SchoolClock.monday(selected))))").font(.subheadline).foregroundStyle(.secondary).frame(maxWidth: .infinity)
                        WeekGrid(schedule: schedule, monday: SchoolClock.monday(selected)) { day, lesson in selected = day; detail = lesson }
                        ForEach(0..<7, id: \.self) { n in
                            let day = SchoolClock.adding(n, to: SchoolClock.monday(selected))
                            VStack(alignment: .leading, spacing: 10) {
                                Text(SchoolClock.label(day, "EEEE M/d")).font(.headline)
                                if schedule.on(day).isEmpty { Text("没有课程").foregroundStyle(.secondary) }
                                ForEach(schedule.on(day)) { lesson in Button { selected = day; detail = lesson } label: { LessonRow(lesson: lesson) }.buttonStyle(.plain) }
                            }
                        }
                    } else {
                        ContentUnavailableView("还没有课表", systemImage: "calendar", description: Text("在学校页面登录，进入课表后点击读取。保存后可离线查看。"))
                    }
                }.padding(20)
            }.background(Color(uiColor: .systemGroupedBackground))
                .navigationTitle("南财课表")
                .toolbar { Button { settings = true } label: { Image(systemName: "gearshape") }.accessibilityLabel("设置") }
                .sheet(isPresented: $importing) { SchoolImportView() }
                .sheet(isPresented: $settings) { SettingsView() }
                .sheet(item: $detail) { lesson in NavigationStack { List { Section(lesson.name) { Text("\(SchoolClock.label(selected, "M月d日 EEEE"))"); Text("\(lesson.start) — \(lesson.end)"); Text(lesson.room.isEmpty ? "教室未提供" : lesson.room); Text(lesson.teacher.isEmpty ? "教师未提供" : lesson.teacher); Text("上课周次：" + lesson.weeks.map(String.init).joined(separator: "、")) } }.navigationTitle("课程详情").toolbar { Button("完成") { detail = nil } } } }
                .alert("提示", isPresented: Binding(get: { store.error != nil }, set: { if !$0 { store.error = nil } })) { Button("知道了") { store.error = nil } } message: { Text(store.error ?? "") }
                .onOpenURL { url in guard url.scheme == "nufeschedule", url.host == "today" else { return }; selected = Date() }
        }
    }
}

struct LessonRow: View {
    let lesson: Lesson
    var body: some View { HStack(alignment: .top, spacing: 14) { VStack(alignment: .leading) { Text(lesson.start); Text(lesson.end).foregroundStyle(.secondary) }.font(.subheadline.monospacedDigit()); VStack(alignment: .leading, spacing: 4) { Text(lesson.name).font(.headline); Text([lesson.room, lesson.teacher].filter { !$0.isEmpty }.joined(separator: " · ")).font(.subheadline).foregroundStyle(.secondary) }; Spacer(minLength: 0) }.padding(14).frame(maxWidth: .infinity, alignment: .leading).background(.background, in: RoundedRectangle(cornerRadius: 14)) }
}
