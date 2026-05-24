//
//  TodayCourseWidget.swift
//  TodayCourseWidget
//
//  Created by kagg886 on 2026/5/24.
//

import AppIntents
import WidgetKit
import SwiftUI
import ComposeAppBackend

private let todayCourseWidgetKind = "TodayCourseWidget"

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> CourseEntry {
        CourseEntry(date: Date(), content: .loading)
    }

    func getSnapshot(in context: Context, completion: @escaping (CourseEntry) -> Void) {
        if context.isPreview {
            completion(CourseEntry(date: Date(), content: .loaded(CourseViewModel.samples)))
            return
        }

        Task {
            completion(await loadEntry())
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<CourseEntry>) -> Void) {
        Task {
            let entry = await loadEntry()
            let timeline = Timeline(entries: [entry], policy: .after(Self.nextMidnight()))
            completion(timeline)
        }
    }

    private func loadEntry() async -> CourseEntry {
        let date = Date()

        do {
            WidgetRuntime.bootstrap()
            let result = try await WidgetRepository().getTodayCourses()
            return CourseEntry(date: date, content: CourseResultParser.parse(result))
        } catch {
            return CourseEntry(date: date, content: .message(error.localizedDescription))
        }
    }

    private static func nextMidnight() -> Date {
        Calendar.current.nextDate(
            after: Date(),
            matching: DateComponents(hour: 0, minute: 0, second: 0),
            matchingPolicy: .nextTime
        ) ?? Date().addingTimeInterval(24 * 60 * 60)
    }
}

private enum WidgetRuntime {
    private static let lock = NSLock()
    private static var bootstrapped = false
    private static var database: AppDatabase?

    static func bootstrap() {
        lock.lock()
        defer { lock.unlock() }

        guard !bootstrapped else { return }
        Mmkv_iosKt.initializeMMKV()
        database = DatabaseKt.databaseBuilder().build()
        bootstrapped = true
    }
}

struct CourseEntry: TimelineEntry {
    let date: Date
    let content: CourseContent
}

enum CourseContent {
    case loading
    case loaded([CourseViewModel])
    case message(String)
}

struct CourseViewModel: Identifiable {
    let id: Int64
    let name: String
    let teacher: String
    let location: String
    let period: Int
    let timeRange: String
    let progress: Float?

    init(course: TodayClass) {
        id = course.recordId
        name = course.name
        teacher = course.teacher
        location = course.location
        period = Int(course.period)
        timeRange = Self.formatTimeRange(course)
        progress = course.progress?.floatValue
    }

    init(id: Int64, name: String, teacher: String, location: String, period: Int, timeRange: String, progress: Float?) {
        self.id = id
        self.name = name
        self.teacher = teacher
        self.location = location
        self.period = period
        self.timeRange = timeRange
        self.progress = progress
    }

    private static func formatTimeRange(_ course: TodayClass) -> String {
        guard let start = course.date.first, let end = course.date.second else {
            return ""
        }

        return "\(format(start))-\(format(end))"
    }

    private static func format(_ time: Any) -> String {
        let text = String(describing: time)
        if text.count >= 5 {
            return String(text.prefix(5))
        }
        return text
    }

    static let samples = [
        CourseViewModel(
            id: 1,
            name: "高等数学",
            teacher: "李老师",
            location: "A101",
            period: 1,
            timeRange: "08:00-09:35",
            progress: 0.42
        ),
        CourseViewModel(
            id: 2,
            name: "大学英语",
            teacher: "王老师",
            location: "B203",
            period: 3,
            timeRange: "10:00-11:35",
            progress: nil
        )
    ]
}

private enum CourseResultParser {
    static func parse(_ result: Any?) -> CourseContent {
        if let courses = result as? [TodayClass] {
            return courses.isEmpty ? .message("今日无课程!") : .loaded(courses.map(CourseViewModel.init(course:)))
        }

        if let array = result as? NSArray {
            let courses = array.compactMap { $0 as? TodayClass }
            if courses.count == array.count {
                return courses.isEmpty ? .message("今日无课程!") : .loaded(courses.map(CourseViewModel.init(course:)))
            }
        }

        guard let result else {
            return .message("暂无课程数据")
        }

        return .message(readableFailureMessage(from: result))
    }

    private static func readableFailureMessage(from result: Any) -> String {
        let text = String(describing: result)
        let patterns = [
            #"message=([^,)]+)"#,
            #"IllegalStateException: ([^)]*)"#,
            #"Failure\((.*)\)"#
        ]

        for pattern in patterns {
            if let range = text.range(of: pattern, options: .regularExpression) {
                return String(text[range])
                    .replacingOccurrences(of: "message=", with: "")
                    .replacingOccurrences(of: "IllegalStateException: ", with: "")
                    .replacingOccurrences(of: "Failure(", with: "")
                    .replacingOccurrences(of: ")", with: "")
            }
        }

        return text.isEmpty ? "暂无课程数据" : text
    }
}

struct RefreshTodayCourseWidgetIntent: AppIntent {
    static var title: LocalizedStringResource = "刷新今日课程"

    func perform() async throws -> some IntentResult {
        WidgetCenter.shared.reloadTimelines(ofKind: todayCourseWidgetKind)
        return .result()
    }
}

struct TodayCourseWidgetEntryView: View {
    var entry: Provider.Entry

    var body: some View {
        VStack(spacing: 8) {
            header

            switch entry.content {
            case .loading:
                EmptyCourseView(message: "Loading...")
            case .message(let message):
                EmptyCourseView(message: message)
            case .loaded(let courses):
                CourseListView(courses: courses)
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private var header: some View {
        HStack(spacing: 8) {
            Text("今日课程")
                .font(.headline.weight(.bold))
                .lineLimit(1)

            Spacer(minLength: 4)

            Button(intent: RefreshTodayCourseWidgetIntent()) {
                Image(systemName: "arrow.clockwise")
                    .font(.caption.weight(.semibold))
                    .frame(width: 28, height: 28)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .tint(.primary)
            .accessibilityLabel("刷新今日课程")
        }
    }
}

private struct EmptyCourseView: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.footnote)
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.center)
            .lineLimit(3)
            .minimumScaleFactor(0.75)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

private struct CourseListView: View {
    let courses: [CourseViewModel]

    var body: some View {
        VStack(spacing: 8) {
            ForEach(courses.prefix(3)) { course in
                CourseItemView(course: course)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }
}

private struct CourseItemView: View {
    let course: CourseViewModel

    var body: some View {
        HStack(spacing: 8) {
            RoundedRectangle(cornerRadius: 2)
                .fill(color(for: course.name))
                .frame(width: 4)

            VStack(alignment: .leading, spacing: 3) {
                Text(course.name)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(2)
                    .minimumScaleFactor(0.8)

                HStack(spacing: 4) {
                    Text("\(formatPeriod(course.period)) · \(course.location)")
                        .lineLimit(1)
                        .minimumScaleFactor(0.75)

                    Spacer(minLength: 4)

                    if let progress = course.progress {
                        Text(progress, format: .percent.precision(.fractionLength(0)))
                            .lineLimit(1)
                    }
                }
                .font(.caption2)
                .foregroundStyle(.secondary)

                if !course.timeRange.isEmpty {
                    Text(course.timeRange)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
        }
        .padding(8)
        .frame(maxWidth: .infinity, minHeight: 52, alignment: .leading)
        .background(.quaternary, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
    }

    private func formatPeriod(_ period: Int) -> String {
        "第\(period)-\(period + 1)节"
    }

    private func color(for text: String) -> Color {
        let seed = text.unicodeScalars.reduce(UInt32(0)) { partial, scalar in
            partial &* 31 &+ scalar.value
        }
        return Color(hue: Double(seed % 360) / 360.0, saturation: 0.64, brightness: 0.95)
    }
}

struct TodayCourseWidget: Widget {
    let kind: String = todayCourseWidgetKind

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: Provider()) { entry in
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
        .supportedFamilies([.systemSmall, .systemMedium])
    }
}

#Preview(as: .systemSmall) {
    TodayCourseWidget()
} timeline: {
    CourseEntry(date: .now, content: .loading)
    CourseEntry(date: .now, content: .loaded(CourseViewModel.samples))
}
