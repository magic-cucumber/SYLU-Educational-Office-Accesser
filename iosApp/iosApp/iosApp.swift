import SwiftUI
import UIKit
import ComposeApp

private enum AppDeepLinks {
    // Share the same buffered flow between Scene callbacks, URLs, and Compose.
    static let flow = MainKt.createEmptyFlow()

    static func handleShortcut(_ shortcutItem: UIApplicationShortcutItem) -> Bool {
        switch shortcutItem.type {
        case "eoa://home/summary", "eoa://course/list", "eoa://home/second-class",
             "eoa://exam/list", "eoa://gpa/list":
            return flow.tryEmit(value: shortcutItem.type)
        default:
            return false
        }
    }
}

final class ShortcutSceneDelegate: NSObject, UIWindowSceneDelegate {
    private var pendingShortcut: UIApplicationShortcutItem?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        pendingShortcut = connectionOptions.shortcutItem
    }

    func sceneDidBecomeActive(_ scene: UIScene) {
        guard let shortcutItem = pendingShortcut else { return }
        pendingShortcut = nil
        _ = AppDeepLinks.handleShortcut(shortcutItem)
    }

    func windowScene(
        _ windowScene: UIWindowScene,
        performActionFor shortcutItem: UIApplicationShortcutItem,
        completionHandler: @escaping (Bool) -> Void
    ) {
        completionHandler(AppDeepLinks.handleShortcut(shortcutItem))
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    let database: AppDatabase

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        MainKt.initializeLightSyncTask(database: database)
        return true
    }

    func application(
        _ application: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let configuration = UISceneConfiguration(
            name: nil,
            sessionRole: connectingSceneSession.role
        )
        configuration.delegateClass = ShortcutSceneDelegate.self
        return configuration
    }

    override init() {
        Mmkv_iosKt.initializeMMKV()
        database = DatabaseKt.databaseBuilder(path: DatabaseKt.databasePath).build()
        super.init()
        LoggerKt.registerKermitLoggerIfExists(appLogDao: database.appLogDao())
    }
}

@main
struct ComposeApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    let deepLinks = AppDeepLinks.flow

    var body: some Scene {
        WindowGroup {
            ContentView(database: appDelegate.database, deepLinks: deepLinks).ignoresSafeArea(.all).onOpenURL { url in
                let result = deepLinks.tryEmit(value: url.absoluteString)
                print("detect deeplinks: \(url), jump result is \(result)")
            }
        }
    }
}

struct ContentView: UIViewControllerRepresentable {
    let database: AppDatabase
    let deepLinks: Kotlinx_coroutines_coreMutableSharedFlow

    func makeCoordinator() -> LongShotScreenshotServiceHolder {
        LongShotScreenshotServiceHolder()
    }
    
    func makeUIViewController(context: Context) -> UIViewController {
        let viewController = MainKt.MainViewController(database: database, deepLinkFlow: deepLinks)
        context.coordinator.install(for: viewController)
        return viewController
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Updates will be handled by Compose
    }

    static func dismantleUIViewController(_ uiViewController: UIViewController, coordinator: LongShotScreenshotServiceHolder) {
        coordinator.uninstall()
    }
}
