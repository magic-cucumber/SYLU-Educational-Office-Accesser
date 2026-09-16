import ComposeApp
import UIKit

@MainActor
final class LongShotScreenshotDelegate: NSObject, UIScreenshotServiceDelegate {
    private let bridge: LongShotBridge

    init(bridge: LongShotBridge) {
        self.bridge = bridge
    }

    func screenshotService(
        _ screenshotService: UIScreenshotService,
        generatePDFRepresentationWithCompletion completionHandler: @escaping (Data?, Int, CGRect) -> Void
    ) {
        bridge.generatePdf { data in
            DispatchQueue.main.async {
                completionHandler(data, 0, .zero)
            }
        }
    }
}

@MainActor
final class LongShotScreenshotServiceHolder {
    private var delegate: LongShotScreenshotDelegate?
    private weak var service: UIScreenshotService?

    func install(for viewController: UIViewController) {
        guard let bridge = ServiceKt.currentLongShotBridge() else {
            retryInstall(for: viewController)
            return
        }
        guard let service = viewController.view.window?.windowScene?.screenshotService else {
            retryInstall(for: viewController)
            return
        }

        let delegate = LongShotScreenshotDelegate(bridge: bridge)
        self.service = service
        self.delegate = delegate
        service.delegate = delegate
    }

    func uninstall() {
        service?.delegate = nil
        delegate = nil
        service = nil
        ServiceKt.currentLongShotBridge()?.dispose()
    }

    private func retryInstall(for viewController: UIViewController) {
        guard delegate == nil else { return }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) { [weak self, weak viewController] in
            guard let self, let viewController else { return }
            self.install(for: viewController)
        }
    }
}
