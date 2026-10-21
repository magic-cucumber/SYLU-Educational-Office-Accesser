import SwiftUI
import UIKit
import ComposeApp

final class AppDelegate: NSObject, UIApplicationDelegate {
    let database: AppDatabase

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        MainKt.initializeLightSyncTask(database: database)
        return true
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
    let deepLinks = MainKt.createEmptyFlow()

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
