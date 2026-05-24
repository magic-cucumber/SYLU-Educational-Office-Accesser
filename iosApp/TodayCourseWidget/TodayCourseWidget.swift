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
private let widgetLogPrefix = "[TodayCourseWidget]"

struct Provider: TimelineProvider {
    func placeholder(in context: Context) -> CourseEntry {
        Self.log("placeholder requested")

        return CourseEntry(date: Date(), content: .loading)
    }

    func getSnapshot(in context: Context, completion: @escaping (CourseEntry) -> Void) {
        Self.log("getSnapshot requested, isPreview=\(context.isPreview), family=\(String(describing: context.family))")
        if context.isPreview {
            Self.log("getSnapshot uses built-in sample data")
            completion(CourseEntry(date: Date(), content: .loaded(CourseViewModel.samples)))
            return
        }

        Task {
            completion(await loadEntry())
        }
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<CourseEntry>) -> Void) {
        Self.log("getTimeline requested, family=\(String(describing: context.family))")
        Task {
            let entry = await loadEntry()
            let nextReload = Self.nextReloadDate(for: entry)
            let timeline = Timeline(entries: [entry], policy: .after(nextReload))
            Self.log("getTimeline completed, entry=\(entry.content.debugDescription), nextReload=\(nextReload)")
            completion(timeline)
        }
    }

    private func loadEntry() async -> CourseEntry {
        let date = Date()
        Self.log("loadEntry started at \(date)")

        do {
            let repository = WidgetRuntime.repository()
            Self.log("WidgetRuntime repository ready, requesting Kotlin WidgetRepository.getTodayCourses()")
            let courses: [TodayClass]? = try await repository.getTodayCourses()
            Self.log("Kotlin WidgetRepository returned courses count=\(courses?.count ?? 0)")
            let content = CourseResultParser.parse(courses)
            Self.log("parsed content=\(content.debugDescription)")
            return CourseEntry(date: date, content: content)
        } catch {
            Self.log("loadEntry failed: \(error.localizedDescription)")
            return CourseEntry(date: date, content: .error(error.localizedDescription))
        }
    }

    private static func nextMidnight() -> Date {
        Calendar.current.nextDate(
            after: Date(),
            matching: DateComponents(hour: 0, minute: 0, second: 0),
            matchingPolicy: .nextTime
        ) ?? Date().addingTimeInterval(24 * 60 * 60)
    }

    private static func nextReloadDate(for entry: CourseEntry, from now: Date = Date()) -> Date {
        let frequentRefresh = now.addingTimeInterval(15 * 60)
        var candidates = [frequentRefresh, nextMidnight()]

        if case .loaded(let courses) = entry.content {
            candidates += courses
                .compactMap { $0.endDate(onSameDayAs: now) }
                .filter { $0 > now }
                .map { $0.addingTimeInterval(60) }
        }

        return candidates.min() ?? frequentRefresh
    }

    private static func log(_ message: String) {
        print("\(widgetLogPrefix) Provider: \(message)")
    }
}

private enum WidgetRuntime {
    private static let lock = NSLock()
    private static var bootstrapped = false
    private static var database: AppDatabase?
    private static var cachedRepository: WidgetRepository?

    static func repository() -> WidgetRepository {
        log("bootstrap requested")
        lock.lock()
        defer { lock.unlock() }

        if let cachedRepository {
            log("bootstrap skipped, already bootstrapped")
            return cachedRepository
        }

        log("initializing MMKV via shared Kotlin initializeMMKV()")
        Mmkv_iosKt.initializeMMKV()
        log("building Room database")
        let database = DatabaseKt.databaseBuilder().build()
        self.database = database
        log("building WidgetRepository")
        let repository = WidgetRepository(database: database)
        cachedRepository = repository
        bootstrapped = true
        log("bootstrap completed")
        return repository
    }

    private static func log(_ message: String) {
        print("\(widgetLogPrefix) Runtime: \(message)")
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
    case error(String)

    var debugDescription: String {
        switch self {
        case .loading:
            return "loading"
        case .loaded(let courses):
            return "loaded(count=\(courses.count), ids=\(courses.map(\.id)))"
        case .message(let message):
            return "message(\(message))"
        case .error(let message):
            return "error(\(message))"
        }
    }
}

struct CourseViewModel: Identifiable {
    let id: Int64
    let name: String
    let teacher: String
    let location: String
    let period: Int
    let timeRange: String
    let endMinuteOfDay: Int?
    let progress: Float?

    init(course: TodayClass) {
        id = course.recordId
        name = course.name
        teacher = course.teacher
        location = course.location
        period = Int(course.period)
        let formattedTime = Self.formattedTimeRange(course)
        timeRange = formattedTime.text
        endMinuteOfDay = Self.minuteOfDay(from: formattedTime.end)
        progress = course.progress?.floatValue
    }

    init(id: Int64, name: String, teacher: String, location: String, period: Int, timeRange: String, endMinuteOfDay: Int?, progress: Float?) {
        self.id = id
        self.name = name
        self.teacher = teacher
        self.location = location
        self.period = period
        self.timeRange = timeRange
        self.endMinuteOfDay = endMinuteOfDay
        self.progress = progress
    }

    var isFinished: Bool {
        guard let endMinuteOfDay else {
            return false
        }

        let components = Calendar.current.dateComponents([.hour, .minute], from: Date())
        let currentMinute = (components.hour ?? 0) * 60 + (components.minute ?? 0)
        return currentMinute >= endMinuteOfDay
    }

    func endDate(onSameDayAs date: Date) -> Date? {
        guard let endMinuteOfDay else {
            return nil
        }

        var components = Calendar.current.dateComponents([.year, .month, .day], from: date)
        components.hour = endMinuteOfDay / 60
        components.minute = endMinuteOfDay % 60
        components.second = 0
        return Calendar.current.date(from: components)
    }

    private static func formattedTimeRange(_ course: TodayClass) -> (text: String, end: String) {
        guard let start = course.date.first, let end = course.date.second else {
            return ("", "")
        }

        let startText = format(start)
        let endText = format(end)
        return ("\(startText)-\(endText)", endText)
    }

    private static func format(_ time: Any) -> String {
        let text = String(describing: time)
        if text.count >= 5 {
            return String(text.prefix(5))
        }
        return text
    }

    private static func minuteOfDay(from time: String) -> Int? {
        let parts = time.split(separator: ":")
        guard parts.count >= 2,
              let hour = Int(parts[0]),
              let minute = Int(parts[1]) else {
            return nil
        }

        return hour * 60 + minute
    }

    static let samples = [
        CourseViewModel(
            id: 1,
            name: "高等数学",
            teacher: "李老师",
            location: "A101",
            period: 1,
            timeRange: "08:00-09:35",
            endMinuteOfDay: 9 * 60 + 35,
            progress: 0.42
        ),
        CourseViewModel(
            id: 2,
            name: "大学英语",
            teacher: "王老师",
            location: "B203",
            period: 3,
            timeRange: "10:00-11:35",
            endMinuteOfDay: 11 * 60 + 35,
            progress: nil
        )
    ]
}

private enum CourseResultParser {
    static func parse(_ courses: [TodayClass]?) -> CourseContent {
        guard let courses else {
            log("received nil courses")
            return .error("暂无课程数据")
        }

        log("received courses, count=\(courses.count)")
        let visibleCourses = courses
            .map(CourseViewModel.init(course:))
            .filter { !$0.isFinished }

        if courses.isEmpty {
            return .message("今日无课程!")
        }

        return visibleCourses.isEmpty ? .message("今日课程已结束") : .loaded(visibleCourses)
    }

    private static func log(_ message: String) {
        print("\(widgetLogPrefix) Parser: \(message)")
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
        Group {
            switch entry.content {
            case .error(let message):
                CenterMessageView(message: message)
            default:
                VStack(spacing: 8) {
                    header

                    switch entry.content {
                    case .loading:
                        EmptyCourseView(message: "Loading...")
                    case .message(let message):
                        EmptyCourseView(message: message)
                    case .loaded(let courses):
                        CourseListView(courses: courses)
                    case .error:
                        EmptyView()
                    }
                }
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
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

private struct CenterMessageView: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.footnote.weight(.medium))
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.center)
            .lineLimit(4)
            .minimumScaleFactor(0.75)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
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
    private let rowPadding: CGFloat = 6
    private let maxVisibleCourseCount = 2

    var body: some View {
        let visibleCourses = Array(courses.prefix(maxVisibleCourseCount))

        VStack(spacing: rowPadding) {
            ForEach(0..<maxVisibleCourseCount, id: \.self) { index in
                Group {
                    if index < visibleCourses.count {
                        CourseItemView(
                            course: visibleCourses[index],
                            position: itemPosition(at: index, count: visibleCourses.count)
                        )
                    } else {
                        Color.clear
                    }
                }
                .frame(maxHeight: .infinity)
            }
        }
        .padding(rowPadding)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
    }

    private func itemPosition(at index: Int, count: Int) -> CourseItemPosition {
        if count <= 1 {
            return .single
        }

        if index == 0 {
            return .top
        }

        if index == count - 1 {
            return .bottom
        }

        return .middle
    }
}

private struct CourseItemView: View {
    let course: CourseViewModel
    let position: CourseItemPosition

    var body: some View {
        HStack(spacing: 6) {
            RoundedRectangle(cornerRadius: 2)
                .fill(color(for: course.name))
                .frame(width: 4)

            VStack(alignment: .leading, spacing: 2) {
                Text(course.name)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.primary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)

                HStack(spacing: 4) {
                    Text(detailText)
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)

                    Spacer(minLength: 4)

                    if let progress = course.progress {
                        Text(progress, format: .percent.precision(.fractionLength(0)))
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
        .background(.quaternary, in: CourseItemShape(position: position, radius: 8))
        .clipped()
    }

    private var detailText: String {
        if course.timeRange.isEmpty {
            return course.location
        }

        return "\(course.timeRange) - \(course.location)"
    }

    private func color(for text: String) -> Color {
        let seed = text.unicodeScalars.reduce(UInt32(0)) { partial, scalar in
            partial &* 31 &+ scalar.value
        }
        return Color(hue: Double(seed % 360) / 360.0, saturation: 0.64, brightness: 0.95)
    }
}

private enum CourseItemPosition {
    case single
    case top
    case middle
    case bottom
}

private struct CourseItemShape: Shape {
    let position: CourseItemPosition
    let radius: CGFloat

    func path(in rect: CGRect) -> Path {
        let corners = roundedCorners(for: position)
        let radius = min(radius, min(rect.width, rect.height) / 2)
        var path = Path()

        path.move(to: CGPoint(x: rect.minX + (corners.topLeft ? radius : 0), y: rect.minY))
        path.addLine(to: CGPoint(x: rect.maxX - (corners.topRight ? radius : 0), y: rect.minY))

        if corners.topRight {
            path.addArc(
                center: CGPoint(x: rect.maxX - radius, y: rect.minY + radius),
                radius: radius,
                startAngle: .degrees(-90),
                endAngle: .degrees(0),
                clockwise: false
            )
        }

        path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY - (corners.bottomRight ? radius : 0)))

        if corners.bottomRight {
            path.addArc(
                center: CGPoint(x: rect.maxX - radius, y: rect.maxY - radius),
                radius: radius,
                startAngle: .degrees(0),
                endAngle: .degrees(90),
                clockwise: false
            )
        }

        path.addLine(to: CGPoint(x: rect.minX + (corners.bottomLeft ? radius : 0), y: rect.maxY))

        if corners.bottomLeft {
            path.addArc(
                center: CGPoint(x: rect.minX + radius, y: rect.maxY - radius),
                radius: radius,
                startAngle: .degrees(90),
                endAngle: .degrees(180),
                clockwise: false
            )
        }

        path.addLine(to: CGPoint(x: rect.minX, y: rect.minY + (corners.topLeft ? radius : 0)))

        if corners.topLeft {
            path.addArc(
                center: CGPoint(x: rect.minX + radius, y: rect.minY + radius),
                radius: radius,
                startAngle: .degrees(180),
                endAngle: .degrees(270),
                clockwise: false
            )
        }

        path.closeSubpath()
        return path
    }

    private func roundedCorners(for position: CourseItemPosition) -> RoundedCorners {
        switch position {
        case .single:
            return RoundedCorners(topLeft: true, topRight: true, bottomLeft: true, bottomRight: true)
        case .top:
            return RoundedCorners(topLeft: true, topRight: true, bottomLeft: false, bottomRight: false)
        case .middle:
            return RoundedCorners(topLeft: false, topRight: false, bottomLeft: false, bottomRight: false)
        case .bottom:
            return RoundedCorners(topLeft: false, topRight: false, bottomLeft: true, bottomRight: true)
        }
    }
}

private struct RoundedCorners {
    let topLeft: Bool
    let topRight: Bool
    let bottomLeft: Bool
    let bottomRight: Bool
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
        .supportedFamilies([.systemSmall])
        .contentMarginsDisabled()
    }
}

#Preview(as: .systemSmall) {
    TodayCourseWidget()
} timeline: {
    CourseEntry(date: .now, content: .loading)
    CourseEntry(date: .now, content: .loaded(CourseViewModel.samples))
}
