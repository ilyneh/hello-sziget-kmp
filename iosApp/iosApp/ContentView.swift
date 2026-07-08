import GoogleSignIn
import Shared
import SwiftUI
import UIKit

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        let controller = MainViewControllerKt.MainViewController()
        GoogleSignInBridge.configure(presenting: controller)
        return controller
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea()
    }
}

/// Bridges the native GoogleSignIn-iOS SDK to the `googleSignInHandler` /
/// `googleSignOutHandler` / `googleCurrentUserHandler` hooks that
/// `GoogleAuthProvider.ios.kt` (shared/iosMain) reads from.
private enum GoogleSignInBridge {
    static func configure(presenting: UIViewController) {
        GoogleAuthProvider_iosKt.googleSignInHandler = { onResult in
            GIDSignIn.sharedInstance.signIn(withPresenting: presenting) { result, error in
                if let error {
                    onResult(nil, error.localizedDescription)
                    return
                }
                guard let user = result?.user, let idToken = user.idToken?.tokenString else {
                    onResult(nil, "Google Sign-In did not return an ID token")
                    return
                }
                onResult(authUser(from: user, idToken: idToken), nil)
            }
        }

        GoogleAuthProvider_iosKt.googleSignOutHandler = {
            GIDSignIn.sharedInstance.signOut()
        }

        GoogleAuthProvider_iosKt.googleCurrentUserHandler = {
            guard let user = GIDSignIn.sharedInstance.currentUser,
                  let idToken = user.idToken?.tokenString else { return nil }
            return authUser(from: user, idToken: idToken)
        }
    }

    private static func authUser(from user: GIDGoogleUser, idToken: String) -> AuthUser {
        AuthUser(
            idToken: idToken,
            email: user.profile?.email ?? "",
            displayName: user.profile?.name ?? "",
            photoUrl: user.profile?.imageURL(withDimension: 320)?.absoluteString
        )
    }
}
