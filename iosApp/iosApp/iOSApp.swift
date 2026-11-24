import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    // O push (Firebase, permissão, toque) mora no AppDelegate.
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

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
