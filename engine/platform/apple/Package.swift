// swift-tools-version: 6.0

import PackageDescription

let package = Package(
    name: "Engine",
    platforms: [
        .macOS(.v11),
        .iOS("16.1"),
    ],
    products: [
        .library(name: "Engine", targets: ["Engine"]),
    ],
    targets: [
        .binaryTarget(
            name: "CEngine",
            path: "Engine.xcframework"
        ),
        .target(
            name: "Engine",
            dependencies: ["CEngine"],
            linkerSettings: [
                .linkedLibrary("c++"),
                .linkedFramework("Security"),
                .linkedFramework("SystemConfiguration"),
                .linkedFramework("CoreFoundation"),
            ]
        ),
        .testTarget(
            name: "EngineTests",
            dependencies: ["Engine"]
        ),
    ]
)
