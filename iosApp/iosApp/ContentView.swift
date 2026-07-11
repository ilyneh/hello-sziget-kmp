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
        PhotoLibraryBridge.configure(presenting: controller)
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

/// Bridges PhotosUI (PHPhotoLibrary/PHImageManager) to the `photoLibrary*Handler` hooks
/// that `DevicePhotoLibrary.ios.kt` (shared/iosMain) reads from, following the same
/// pattern as GoogleSignInBridge above.
private enum PhotoLibraryBridge {
    static func configure(presenting: UIViewController) {
        DevicePhotoLibrary_iosKt.photoLibraryAccessStatusHandler = {
            accessStatus(from: PHPhotoLibrary.authorizationStatus(for: .readWrite))
        }

        DevicePhotoLibrary_iosKt.photoLibraryRequestAccessHandler = { onResult in
            PHPhotoLibrary.requestAuthorization(for: .readWrite) { status in
                onResult(accessStatus(from: status))
            }
        }

        DevicePhotoLibrary_iosKt.photoLibraryLoadPhotosHandler = { onResult in
            let options = PHFetchOptions()
            options.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
            let assets = PHAsset.fetchAssets(with: .image, options: options)

            let imageManager = PHImageManager.default()
            let requestOptions = PHImageRequestOptions()
            requestOptions.isSynchronous = false
            requestOptions.deliveryMode = .opportunistic

            var photos: [DevicePhoto] = []
            let group = DispatchGroup()

            assets.enumerateObjects { asset, _, _ in
                group.enter()
                imageManager.requestImage(
                    for: asset,
                    targetSize: CGSize(width: 300, height: 300),
                    contentMode: .aspectFill,
                    options: requestOptions
                ) { image, _ in
                    if let data = image?.jpegData(compressionQuality: 0.8) {
                        photos.append(DevicePhoto(id: asset.localIdentifier, thumbnail: data.toKotlinByteArray()))
                    }
                    group.leave()
                }
            }

            group.notify(queue: .main) {
                onResult(photos)
            }
        }

        DevicePhotoLibrary_iosKt.photoLibraryLoadFullImageHandler = { id, onResult in
            let fetchResult = PHAsset.fetchAssets(withLocalIdentifiers: [id], options: nil)
            guard let asset = fetchResult.firstObject else {
                onResult(nil, nil)
                return
            }

            let contentType = PHAssetResource.assetResources(for: asset).first
                .flatMap { UTType($0.uniformTypeIdentifier) }
                .flatMap { $0.preferredMIMEType } ?? "image/jpeg"

            let requestOptions = PHImageRequestOptions()
            requestOptions.isSynchronous = false
            requestOptions.deliveryMode = .highQualityFormat
            requestOptions.version = .current

            PHImageManager.default().requestImageDataAndOrientation(for: asset, options: requestOptions) { data, _, _, _ in
                onResult(data?.toKotlinByteArray(), contentType)
            }
        }

        DevicePhotoLibrary_iosKt.photoLibraryManageAccessHandler = { onComplete in
            PHPhotoLibrary.shared().presentLimitedLibraryPicker(from: presenting) { _ in
                onComplete()
            }
        }

        DevicePhotoLibrary_iosKt.photoLibraryOpenSettingsHandler = {
            guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
            UIApplication.shared.open(url)
        }
    }

    private static func accessStatus(from status: PHAuthorizationStatus) -> PhotoAccessStatus {
        switch status {
        case .authorized: return .full
        case .limited: return .limited
        case .denied, .restricted: return .denied
        case .notDetermined: return .notdetermined
        @unknown default: return .notdetermined
        }
    }
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
