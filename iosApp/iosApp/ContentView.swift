import GoogleSignIn
import Photos
import PhotosUI
import Shared
import SwiftUI
import UIKit
import UniformTypeIdentifiers

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        let controller = MainViewControllerKt.MainViewController()
        GoogleSignInBridge.configure(presenting: controller)
        ProfileImagePickerBridge.configure(presenting: controller)
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

/// Presents a single-selection PHPickerViewController and reports the picked image's raw
/// bytes + MIME type back through `profileImagePickerHandler`, which
/// `ProfileImagePicker.ios.kt` (shared/iosMain) reads from. PHPickerViewController runs
/// out-of-process, so no photo-library permission/usage-description is needed.
private enum ProfileImagePickerBridge {
    private static var coordinator: ProfileImagePickerCoordinator?

    static func configure(presenting: UIViewController) {
        ProfileImagePicker_iosKt.profileImagePickerHandler = { onResult in
            var configuration = PHPickerConfiguration(photoLibrary: .shared())
            configuration.filter = .images
            configuration.selectionLimit = 1

            let picker = PHPickerViewController(configuration: configuration)
            let coordinator = ProfileImagePickerCoordinator { data, mimeType in
                onResult(data?.toKotlinByteArray(), mimeType)
                ProfileImagePickerBridge.coordinator = nil
            }
            ProfileImagePickerBridge.coordinator = coordinator
            picker.delegate = coordinator
            presenting.present(picker, animated: true)
        }
    }
}

private final class ProfileImagePickerCoordinator: NSObject, PHPickerViewControllerDelegate {
    private let onResult: (Data?, String?) -> Void

    init(onResult: @escaping (Data?, String?) -> Void) {
        self.onResult = onResult
    }

    func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
        picker.dismiss(animated: true)

        guard let provider = results.first?.itemProvider else {
            onResult(nil, nil)
            return
        }

        let typeIdentifier = provider.registeredTypeIdentifiers.first ?? UTType.jpeg.identifier
        let mimeType = UTType(typeIdentifier)?.preferredMIMEType ?? "image/jpeg"

        provider.loadDataRepresentation(forTypeIdentifier: typeIdentifier) { data, _ in
            DispatchQueue.main.async {
                guard let data else {
                    self.onResult(nil, nil)
                    return
                }
                if data.count > maxProfileImageBytes, let downscaled = downscaledJPEG(from: data, maxBytes: maxProfileImageBytes) {
                    self.onResult(downscaled, "image/jpeg")
                } else {
                    self.onResult(data, mimeType)
                }
            }
        }
    }
}

// Keep in sync with MAX_PROFILE_IMAGE_BYTES in ProfileImagePicker.kt (shared/commonMain).
private let maxProfileImageBytes = 10 * 1024 * 1024

private func downscaledJPEG(from data: Data, maxBytes: Int) -> Data? {
    guard let originalImage = UIImage(data: data) else { return nil }

    let scale = (Double(maxBytes) / Double(data.count)).squareRoot()
    let targetSize = CGSize(width: originalImage.size.width * scale, height: originalImage.size.height * scale)
    let scaledImage = UIGraphicsImageRenderer(size: targetSize).image { _ in
        originalImage.draw(in: CGRect(origin: .zero, size: targetSize))
    }

    var quality: CGFloat = 0.9
    var result = scaledImage.jpegData(compressionQuality: quality)
    while let current = result, current.count > maxBytes, quality > 0.1 {
        quality -= 0.1
        result = scaledImage.jpegData(compressionQuality: quality)
    }
    return result
}

private extension Data {
    func toKotlinByteArray() -> KotlinByteArray {
        let byteArray = KotlinByteArray(size: Int32(count))
        withUnsafeBytes { (rawBufferPointer: UnsafeRawBufferPointer) in
            let bytes = rawBufferPointer.bindMemory(to: Int8.self)
            for (index, byte) in bytes.enumerated() {
                byteArray.set(index: Int32(index), value: byte)
            }
        }
        return byteArray
    }
}
