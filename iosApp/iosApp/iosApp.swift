import SwiftUI
import ComposeApp

@main
struct ComposeApp: App {
    let deepLinks = MainKt.createEmptyFlow()

    var body: some Scene {
        WindowGroup {
            ContentView(deepLinks: deepLinks).ignoresSafeArea(.all).onOpenURL { url in
                let result = deepLinks.tryEmit(value: url.absoluteString)
                print("detect deeplinks: \(url), jump result is \(result)")
            }
        }
    }
}

struct ContentView: UIViewControllerRepresentable {
    let deepLinks: Kotlinx_coroutines_coreMutableSharedFlow

    func makeCoordinator() -> LongShotScreenshotServiceHolder {
        LongShotScreenshotServiceHolder()
    }
    
    func makeUIViewController(context: Context) -> UIViewController {
        let viewController = MainKt.MainViewController(deepLinkFlow: deepLinks)
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
