import ComposeApp
import Social
import UIKit
import UniformTypeIdentifiers

class ShareViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        // 1. 基础设置
        view.backgroundColor = .systemBackground  // 避免加载时的黑屏

        // 2. 开始寻找并传递数据
        extractAndPassToKotlin()
    }

    private func extractAndPassToKotlin() {
        // 获取所有输入项
        guard let extensionItems = extensionContext?.inputItems as? [NSExtensionItem] else {
            self.closeExtension()
            return
        }

        // 遍历寻找包含图片的 item
        for item in extensionItems {
            guard let attachments = item.attachments else { continue }

            // 检查附件中是否有图片类型 (public.image)
            let hasImage = attachments.contains { provider in
                provider.hasItemConformingToTypeIdentifier(UTType.image.identifier)
            }

            if hasImage {
                // 3. 找到目标 item，移交给 Kotlin 处理
                loadKotlinPage(with: item)
                return
            }
        }

        // 如果没有找到图片，可以选择直接关闭或者显示错误
        print("未找到图片内容")
        self.closeExtension()
    }

    private func loadKotlinPage(with item: NSExtensionItem) {
        let kotlinVC = MainKt.ImageProcessingViewController(item: item)
        addChild(kotlinVC)
        view.addSubview(kotlinVC.view)

        kotlinVC.view.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            kotlinVC.view.topAnchor.constraint(equalTo: view.topAnchor),
            kotlinVC.view.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            kotlinVC.view.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            kotlinVC.view.trailingAnchor.constraint(equalTo: view.trailingAnchor),
        ])

        kotlinVC.didMove(toParent: self)
    }

    // 如果 Kotlin 侧处理完毕需要关闭扩展，可以通过某种回调或 Notification 调用此方法
    private func closeExtension() {
        extensionContext?.completeRequest(returningItems: [], completionHandler: nil)
    }
}
