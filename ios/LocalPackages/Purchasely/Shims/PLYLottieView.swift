import UIKit

@objc protocol LottieBridgeProtocol: AnyObject {
    func view() -> UIView?
    func loop(_ loop: Bool)
    func fill(_ fill: Bool)
    func play()
    func pause()
    func stop()
}

@objc final class PLYLottieView: UIView {
    @objc var bridge: LottieBridgeProtocol?
    @objc var lottieView: UIView?

    @objc func configure(with url: URL) {
        guard let lottieClass = NSClassFromString("PLYLottieBridge") as? NSObjectProtocol,
              let bridgeFactory = lottieClass as? AnyObject else {
            PLYLogger.log(
                message: "You are trying to display a Lottie animation but you don't have the `PLYLottieBridge` bridge installed. Refer to the documentation for more explanations.",
                level: .error
            )
            return
        }

        if let bridge {
            bridge.view()?.removeFromSuperview()
            self.bridge = nil
        }

        let selector = NSSelectorFromString("bridgeWith:")
        guard bridgeFactory.responds(to: selector),
              let unmanaged = bridgeFactory.perform(selector, with: url),
              let lottieBridge = unmanaged.takeUnretainedValue() as? LottieBridgeProtocol,
              let view = lottieBridge.view() else {
            PLYLogger.log(
                message: "You are trying to display a Lottie animation but you don't have the `PLYLottieBridge` bridge installed. Refer to the documentation for more explanations.",
                level: .error
            )
            return
        }

        bridge = lottieBridge
        lottieView = view
        view.translatesAutoresizingMaskIntoConstraints = false
        addSubview(view)
        NSLayoutConstraint.activate([
            view.topAnchor.constraint(equalTo: topAnchor),
            view.bottomAnchor.constraint(equalTo: bottomAnchor),
            view.leadingAnchor.constraint(equalTo: leadingAnchor),
            view.trailingAnchor.constraint(equalTo: trailingAnchor)
        ])
    }
}
