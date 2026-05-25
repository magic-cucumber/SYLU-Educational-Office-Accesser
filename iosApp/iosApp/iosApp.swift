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
    
    func makeUIViewController(context: Context) -> UIViewController {
        return MainKt.MainViewController(deepLinkFlow: deepLinks)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Updates will be handled by Compose
    }
}
