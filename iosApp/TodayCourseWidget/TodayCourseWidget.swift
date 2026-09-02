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

        // 提取扫描线分段后的实际开始和结束时间。
        let courseTimes = courses.compactMap { course -> (start: Date, end: Date)? in
            guard let start = course.startDate,
                  let end = course.endDate else {
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

        // 在下一个分段开始时刷新，使 Kotlin 重新把进度赋给当前唯一条目。
        if let nextCourseStart = courseTimes
            .filter({ now < $0.start })
            .map(\.start)
            .min() {
            return nextCourseStart
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

        let now = Date.now
        let visible = courses.filter {
            ($0.endDate ?? .distantPast) > now
        }

        return visible.isEmpty ? .message("今日课程已结束") : .courses(visible)
    }
}

extension TodayClass {
    var startDate: Date? {
        date.first?.foundationDate
    }

    var endDate: Date? {
        date.second?.foundationDate
    }

    var timeText: String {
        "\(date.first?.timeText ?? "--:--")–\(date.second?.timeText ?? "--:--")"
    }

    static let samples = [
        TodayClass.sample(
            name: "高等数学",
            location: "A101",
            start: 8 * 60,
            end: 9 * 60 + 35,
            progress: 0.42
        ),
        TodayClass.sample(
            name: "大学英语",
            location: "B203",
            start: 10 * 60,
            end: 11 * 60 + 35,
            progress: nil
        )
    ]

    private static func sample(
        name: String,
        location: String,
        start: Int,
        end: Int,
        progress: Float?
    ) -> TodayClass {
        let components = Calendar.current.dateComponents([.year, .month, .day], from: .now)
        let year = Int32(components.year ?? 1970)
        let month = Int32(components.month ?? 1)
        let day = Int32(components.day ?? 1)
        let startDate = Kotlinx_datetimeLocalDateTime(
            year: year,
            monthNumber: month,
            dayOfMonth: day,
            hour: Int32(start / 60),
            minute: Int32(start % 60),
            second: 0,
            nanosecond: 0
        )
        let endDate = Kotlinx_datetimeLocalDateTime(
            year: year,
            monthNumber: month,
            dayOfMonth: day,
            hour: Int32(end / 60),
            minute: Int32(end % 60),
            second: 0,
            nanosecond: 0
        )
        let date = KotlinPair(first: startDate, second: endDate)
        let kotlinProgress = progress.map { KotlinFloat(value: $0) }

        return TodayClass(
            recordId: 1,
            courseId: 1,
            name: name,
            teacher: "",
            location: location,
            date: date,
            progress: kotlinProgress,
            conflict: false
        )
    }
}

private extension Kotlinx_datetimeLocalDateTime {
    var timeText: String {
        String(format: "%02d:%02d", hour, minute)
    }

    var foundationDate: Date? {
        var components = DateComponents()
        components.year = Int(year)
        components.month = Int(monthNumber)
        components.day = Int(dayOfMonth)
        components.hour = Int(hour)
        components.minute = Int(minute)
        components.second = Int(second)
        components.nanosecond = Int(nanosecond)
        return Calendar.current.date(from: components)
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
            ForEach(Array(courses.indices.prefix(2)), id: \.self) { index in
                let course = courses[index]
                Link(destination: course.deepLinkURL) {
                    CourseRow(course: course)
                        .frame(maxHeight: .infinity)
                }
                .buttonStyle(.plain)
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
                    Text(course.timeText + (course.conflict ? "" : " - \(course.location)"))
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)

                    Spacer()

                    if let progress = course.progress {
                        Text("\(String(format: "%.2f", progress.doubleValue * 100))%")
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

private extension TodayClass {
    var deepLinkURL: URL {
        if conflict, let startDate, let endDate {
            let start = Int64(startDate.timeIntervalSince1970 * 1_000)
            let end = Int64(endDate.timeIntervalSince1970 * 1_000)
            return URL(string: "eoa://course/conflict/\(start)/\(end)")!
        }

        return URL(string: "eoa://course/profile/\(recordId)")!
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
