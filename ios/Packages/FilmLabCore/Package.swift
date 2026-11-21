// swift-tools-version: 5.9
import PackageDescription
let package = Package(name: "FilmLabCore", platforms: [.iOS("18.0"), .macOS(.v13)], products: [.library(name: "FilmLabCore", targets: ["FilmLabCore"])], targets: [
    .systemLibrary(name: "CZlib", pkgConfig: "zlib"),
    .target(name: "FilmLabCore", dependencies: ["CZlib"], resources: [.process("Resources")]),
    .testTarget(name: "FilmLabCoreTests", dependencies: ["FilmLabCore"], resources: [.process("colour-vectors.json"),.process("interchange-fixtures.json")])
])
