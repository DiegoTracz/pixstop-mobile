import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import ComposeApp

/// O push do iPhone, do Firebase ao Kotlin.
///
/// O servidor manda pelo FCM v1, que entrega no iPhone pelo APNs desde que a
/// chave `.p8` esteja no projeto Firebase. Aqui o app pede a permissão, entrega
/// o token do APNs ao Firebase, e o token FCM que volta segue para
/// `PushTokenRegistry` — o mesmo caminho do Android. O toque na notificação
/// vira alvo de navegação em `IosPush.onNotificationTapped`.
///
/// Sem o `GoogleService-Info.plist` no bundle (ele não vai ao Git), o Firebase
/// não sobe e o app roda sem push em vez de fechar na abertura.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self

        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            print("[Push] GoogleService-Info.plist ausente: o app roda sem push.")
            return true
        }

        FirebaseApp.configure()
        Messaging.messaging().delegate = self

        // A resposta não muda nada aqui: negar só faz o aviso não aparecer.
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
        application.registerForRemoteNotifications()

        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("[Push] O APNs recusou o registro: \(error.localizedDescription)")
    }

    // MARK: - MessagingDelegate

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        IosPush.shared.onNewToken(token: fcmToken)
    }

    // MARK: - UNUserNotificationCenterDelegate

    /// Com o app aberto o iOS não mostra nada por conta própria; sem isto o
    /// aviso se perderia, como no `PixelstopMessagingService` do Android.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }

    /// O toque, com o app aberto, em segundo plano ou fechado: no arranque frio
    /// o barramento guarda o alvo até a navegação existir.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        IosPush.shared.onNotificationTapped(userInfo: response.notification.request.content.userInfo)
        completionHandler()
    }
}
