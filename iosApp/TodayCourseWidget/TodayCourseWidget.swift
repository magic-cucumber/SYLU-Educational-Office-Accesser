import AppIntents
import WidgetKit
import SwiftUI
import Foundation
import ComposeAppBackend

private let widgetKind = "TodayCourseWidget"

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> CourseEntry {
        CourseEntry(date: .now, state: .loading)
    }

    func getSnapshot(in context: Context, completion: @escaping (CourseEntry) -> Void) {
        if context.isPreview {
            completion(CourseEntry(date: .now, state: .courses(TodayClass.samples)))
            return
        }

        Task {
            completion(await loadEntry())
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<CourseEntry>) -> Void) {
        Task {
            let entry = await loadEntry()
            let nextReloadDate = nextReloadDate(for: entry)
            
            // nextReloadDate 为 UTC 时间，避免用户困惑，改为东八区。
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "zh_CN")
            formatter.timeZone = TimeZone(identifier: "Asia/Shanghai")
            formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
            
            await WidgetRuntime.log(severity: Kermit_coreSeverity.debug,message: "getTimeLine: \(entry.state), nextReloadDate: \(formatter.string(from: nextReloadDate))")
            
            completion(Timeline(entries: [entry], policy: .after(nextReloadDate)))
        }
    }

    private func loadEntry() async -> CourseEntry {
        do {
            let courses = try await WidgetRuntime.repository.getTodayCourses()
            return CourseEntry(date: .now, state: .from(courses))
        } catch {
            return CourseEntry(date: .now, state: .message(error.localizedDescription))
        }
    }

    private func nextReloadDate(for entry: CourseEntry, now: Date = .now) -> Date {
        let calendar = Calendar.current
        let nextMidnight = calendar.nextDate(
            after: now,
            matching: DateComponents(hour: 0, minute: 0, second: 0),
            matchingPolicy: .nextTime
        ) ?? now.addingTimeInterval(24 * 60 * 60)

        guard case .courses(let courses) = entry.state else {
            return nextMidnight
        }

        // 提取每节课在今天的开始和结束时间，后续用于判断当前课程状态。
        let courseTimes = courses.compactMap { course -> (start: Date, end: Date)? in
            guard let start = course.startTime.date(on: now, calendar: calendar),
                  let end = course.endTime.date(on: now, calendar: calendar) else {
                return nil
            }

            return (start, end)
        }

        // 判断是否存在正在上的课程，如果存在则在该课程下课时刷新。
        if let currentCourseEnd = courseTimes
            .filter({ $0.start <= now && now < $0.end })
            .map(\.end)
            .min() {
            return currentCourseEnd
        }

        // 判断是否存在今日待上的课程，如果存在则在最近一节待上课程下课时刷新。
        if let nextCourseEnd = courseTimes
            .filter({ now < $0.start })
            .min(by: { $0.start < $1.start })?
            .end {
            return nextCourseEnd
        }

        // 判断今日已无待上课程，刷新时间推迟到第二天凌晨 0 点。
        return nextMidnight
    }
}

private enum WidgetRuntime {
    static let repository: WidgetRepository = {
        Mmkv_iosKt.initializeMMKV()
        let database = DatabaseKt.databaseBuilder().build()
        LoggerKt.registerKermitLoggerIfExists(appLogDao: database.appLogDao())
        return WidgetRepository(database: database)
    }()
    
    static func log(severity: Kermit_coreSeverity,message:String) async {
        LogKt.asTaggedLogger("TodayClassWidget").log(severity: severity,tag: "TodayClassWidget", throwable: nil, message: message)
    }
}

struct CourseEntry: TimelineEntry {
    let date: Date
    let state: CourseState
}

enum CourseState {
    case loading
    case courses([TodayClass])
    case message(String)

    static func from(_ courses: [TodayClass]?) -> CourseState {
        guard let courses else {
            return .message("暂无课程数据")
        }

        guard !courses.isEmpty else {
            return .message("今日无课程!")
        }

        let now = Calendar.current.dateComponents([.hour, .minute], from: .now)
        let visible = courses.filter {
            $0.endTime.minuteOfDay > now.minuteOfDay
        }

        return visible.isEmpty ? .message("今日课程已结束") : .courses(visible)
    }
}

extension TodayClass {
    var startTime: DateComponents {
        DateComponents(
            hour: Int(date.first?.hour ?? 0),
            minute: Int(date.first?.minute ?? 0)
        )
    }

    var endTime: DateComponents {
        DateComponents(
            hour: Int(date.second?.hour ?? 0),
            minute: Int(date.second?.minute ?? 0)
        )
    }

    static let samples = [
        TodayClass.sample(
            recordId: 1,
            name: "高等数学",
            location: "A101",
            start: 8 * 60,
            end: 9 * 60 + 35,
            progress: 0.42
        ),
        TodayClass.sample(
            recordId: 2,
            name: "大学英语",
            location: "B203",
            start: 10 * 60,
            end: 11 * 60 + 35,
            progress: nil
        )
    ]

    private static func sample(
        recordId: Int64,
        name: String,
        location: String,
        start: Int,
        end: Int,
        progress: Float?
    ) -> TodayClass {
        TodayClass(
            recordId: recordId,
            courseId: recordId,
            name: name,
            teacher: "",
            location: location,
            date: KotlinPair(
                first: Kotlinx_datetimeLocalTime(
                    hour: Int32(start / 60),
                    minute: Int32(start % 60),
                    second: 0,
                    nanosecond: 0
                ),
                second: Kotlinx_datetimeLocalTime(
                    hour: Int32(end / 60),
                    minute: Int32(end % 60),
                    second: 0,
                    nanosecond: 0
                )
            ),
            period: 1,
            progress: progress.map { KotlinFloat(value: $0) }
        )
    }
}

private extension DateComponents {

    var minuteOfDay: Int {
        (hour ?? 0) * 60 + (minute ?? 0)
    }

    var timeText: String {
        String(format: "%02d:%02d", hour ?? 0, minute ?? 0)
    }

    func date(on date: Date, calendar: Calendar = .current) -> Date? {
        var components = calendar.dateComponents([.year, .month, .day], from: date)
        components.hour = hour
        components.minute = minute
        components.second = 0
        return calendar.date(from: components)
    }
}

struct RefreshTodayCourseWidgetIntent: AppIntent {
    static var title: LocalizedStringResource = "刷新今日课程"

    func perform() async throws -> some IntentResult {
        WidgetCenter.shared.reloadTimelines(ofKind: widgetKind)
        return .result()
    }
}

struct TodayCourseWidgetEntryView: View {
    let entry: CourseEntry

    var body: some View {
        VStack(spacing: 8) {
            header

            switch entry.state {
            case .loading:
                MessageView("Loading...")
            case .message(let text):
                MessageView(text)
            case .courses(let courses):
                CourseListView(courses: courses)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var header: some View {
        HStack {
            Text("今日课程")
                .font(.headline.bold())
                .lineLimit(1)

            Spacer()

            Button(intent: RefreshTodayCourseWidgetIntent()) {
                Image(systemName: "arrow.clockwise")
                    .font(.caption.bold())
                    .frame(width: 28, height: 28)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("刷新今日课程")
        }
    }
}

private struct MessageView: View {
    let text: String

    init(_ text: String) {
        self.text = text
    }

    var body: some View {
        Text(text)
            .font(.footnote)
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.center)
            .lineLimit(4)
            .minimumScaleFactor(0.75)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct CourseListView: View {
    let courses: [TodayClass]

    var body: some View {
        VStack(spacing: 6) {
            ForEach(Array(courses.prefix(2)), id: \.recordId) { course in
                CourseRow(course: course)
                    .frame(maxHeight: .infinity)
            }

            if courses.count == 1 {
                Color.clear.frame(maxHeight: .infinity)
            }
        }
        .padding(6)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}

private struct CourseRow: View {
    let course: TodayClass

    var body: some View {
        HStack(spacing: 6) {
            RoundedRectangle(cornerRadius: 2)
                .fill(color)
                .frame(width: 4)

            VStack(alignment: .leading, spacing: 2) {
                Text(course.name)
                    .font(.caption.bold())
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)

                HStack(spacing: 4) {
                    Text("\(course.startTime.timeText)-\(course.endTime.timeText) - \(course.location)")
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)

                    Spacer()

                    if let progress = course.progress {
                        Text("\(String(format: "%.2f", progress.doubleValue))")
                            .lineLimit(1)
                    }
                }
                .font(.caption2)
                .foregroundStyle(.secondary)
            }
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 5)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
        .background(.quaternary, in: RoundedRectangle(cornerRadius: 8))
        .clipped()
    }

    private var color: Color {
        let seed = course.name.unicodeScalars.reduce(UInt32(0)) {
            $0 &* 31 &+ $1.value
        }

        return Color(
            hue: Double(seed % 360) / 360,
            saturation: 0.64,
            brightness: 0.95
        )
    }
}

struct TodayCourseWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: widgetKind, provider: Provider()) { entry in
            if #available(iOS 17.0, *) {
                TodayCourseWidgetEntryView(entry: entry)
                    .containerBackground(.fill.tertiary, for: .widget)
            } else {
                TodayCourseWidgetEntryView(entry: entry)
                    .background(Color(.systemBackground))
            }
        }
        .configurationDisplayName("今日课程")
        .description("查看今天的课程安排。")
        .supportedFamilies([.systemSmall])
        .contentMarginsDisabled()
    }
}

#Preview(as: .systemSmall) {
    TodayCourseWidget()
} timeline: {
    CourseEntry(date: .now, state: .loading)
    CourseEntry(date: .now, state: .courses(TodayClass.samples))
}

