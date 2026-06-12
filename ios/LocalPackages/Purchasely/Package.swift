// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Purchasely",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v16)
    ],
    products: [
        .library(name: "Purchasely", targets: ["Purchasely"])
    ],
    targets: [
        .target(
            name: "Purchasely",
            path: ".",
            exclude: [
                "Purchasely/Classes/specific/tvos",
                "Purchasely/Classes/specific/uikit/View/PLYLottieView.h",
                "Purchasely/Classes/specific/uikit/View/PLYLottieView.m",
                "Purchasely/Assets/specific/tvos",
                "Purchasely/Tests"
            ],
            sources: [
                "Purchasely/Classes/common",
                "Purchasely/Classes/specific/ios",
                "Purchasely/Classes/specific/uikit",
                "Purchasely/Classes/specific/swiftUI",
                "Shims"
            ],
            resources: [
                .process("Purchasely/Assets/common"),
                .process("Purchasely/Assets/specific/ios"),
                .process("Purchasely/PrivacyInfo.xcprivacy")
            ]
        )
    ]
)
