import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    // O Koin e o carregador de imagem sobem uma vez, antes da primeira tela.
    init() {
        MainViewControllerKt.startApp()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
