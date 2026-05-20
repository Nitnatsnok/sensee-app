import UIKit
import SwiftUI
import SenseeKit

struct ComposeView: UIViewControllerRepresentable {

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIViewController(context: Context) -> UIViewController {
        let rootHolder = context.coordinator.rootHolder
        rootHolder.resume()

        return MainViewControllerKt.MainViewController(
            rootHolder: rootHolder
        )
    }

    func updateUIViewController(
        _ uiViewController: UIViewController,
        context: Context
    ) {
    }

    static func dismantleUIViewController(
        _ uiViewController: UIViewController,
        coordinator: Coordinator
    ) {
        coordinator.rootHolder.stop()
        coordinator.rootHolder.destroy()
    }

    final class Coordinator {
        let rootHolder = IosRootHolder()
    }
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}
