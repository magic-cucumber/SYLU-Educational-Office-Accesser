import ComposeApp
import UIKit

@MainActor
final class LongShotScreenshotDelegate: NSObject, UIScreenshotServiceDelegate {
    private let bridge: LongShotBridge
    private weak var view: UIView?
    private var snapshot: UIView?

    init(bridge: LongShotBridge, view: UIView) {
        self.bridge = bridge
        self.view = view
    }

    func screenshotService(
        _ screenshotService: UIScreenshotService,
        generatePDFRepresentationWithCompletion completionHandler: @escaping (Data?, Int, CGRect) -> Void
    ) {
        // Freeze the visible UIKit surface before Compose changes its screenshot layout.
        // PDF strips come from the Compose GraphicsLayer, which excludes this native overlay.
        guard snapshot == nil, let view, view.window != nil,
              let snapshot = view.snapshotView(afterScreenUpdates: false) else {
            completionHandler(nil, 0, .zero)
            return
        }
        snapshot.frame = view.bounds
        snapshot.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        snapshot.isUserInteractionEnabled = true
        snapshot.accessibilityElementsHidden = true
        self.snapshot = snapshot
        view.addSubview(snapshot)

        bridge.generatePdf { data in
            DispatchQueue.main.async {
                // The bridge waits for the restored Compose frame before completing.
                snapshot.removeFromSuperview()
                self.snapshot = nil
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

        let delegate = LongShotScreenshotDelegate(bridge: bridge, view: viewController.view)
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
