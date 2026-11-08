import SwiftUI

struct WeekGrid: View {
    let schedule: Schedule
    let monday: Date
    let select: (Date, Lesson) -> Void
    private var first: Int { min(480, (0..<7).flatMap { schedule.on(SchoolClock.adding($0, to: monday)) }.compactMap { SchoolClock.minutes($0.start) }.min() ?? 480) / 60 * 60 }
    private var last: Int { max(1260, (0..<7).flatMap { schedule.on(SchoolClock.adding($0, to: monday)) }.compactMap { SchoolClock.minutes($0.end) }.max() ?? 1260) }
    var body: some View {
        ScrollView(.horizontal) {
            HStack(alignment: .top, spacing: 4) {
                VStack(spacing: 0) { Text("时间").frame(height: 44); ZStack(alignment: .topLeading) { ForEach(Array(stride(from: first, through: last, by: 60)), id: \.self) { minute in Text(String(format: "%02d:00", minute / 60)).font(.caption.monospacedDigit()).offset(y: CGFloat(minute-first) - 6) } }.frame(width: 42, height: CGFloat(last-first)+18) }.foregroundStyle(.secondary)
                ForEach(0..<7, id: \.self) { n in
                    let day = SchoolClock.adding(n, to: monday)
                    VStack(spacing: 0) {
                        Text(SchoolClock.label(day, "EEE\nM/d")).font(.caption).multilineTextAlignment(.center).frame(height: 44)
                        ZStack(alignment: .topLeading) {
                            ForEach(Array(stride(from: first, through: last, by: 60)), id: \.self) { minute in Rectangle().fill(.gray.opacity(0.15)).frame(height: 1).offset(y: CGFloat(minute-first)) }
                            ForEach(schedule.on(day)) { lesson in
                                let start = SchoolClock.minutes(lesson.start) ?? first
                                let end = SchoolClock.minutes(lesson.end) ?? start+1
                                Button { select(day, lesson) } label: { VStack(alignment: .leading, spacing: 3) { Text(lesson.name).font(.caption.bold()); Text("\(lesson.start)\n\(lesson.end)").font(.caption.monospacedDigit()); Text(lesson.room).font(.caption2) }.padding(6).frame(width: 94, height: max(1, CGFloat(end-start)), alignment: .topLeading).background(.indigo.opacity(0.10), in: RoundedRectangle(cornerRadius: 9)).clipped() }.buttonStyle(.plain).offset(y: CGFloat(start-first))
                            }
                        }.frame(width: 94, height: CGFloat(last-first)+18).background(SchoolClock.day(day) == SchoolClock.day(Date()) ? Color.indigo.opacity(0.04) : Color.clear)
                    }
                }
            }.padding(.vertical, 8)
        }.accessibilityLabel("周课表，可横向查看七天；点击课程查看完整信息")
    }
}
