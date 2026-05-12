import Foundation
import StoreKit

@available(iOS 15.0, *)
@MainActor
final class PurchaseManager {

    static let shared = PurchaseManager()

    /// Anonymous user ID provider — injected to avoid direct PurchaselyWrapper dependency
    var anonymousUserIdProvider: (() -> String)?

    /// Sign promo offer — injected to avoid direct PurchaselyWrapper dependency
    var signPromotionalOfferProvider: ((_ productId: String, _ offerId: String, _ success: @escaping (PLYOfferSignatureData) -> Void, _ failure: @escaping (Error) -> Void) -> Void)?

    private init() {}

    // MARK: - Purchase

    func purchase(productId: String) async -> TransactionResult {
        do {
            let products = try await Product.products(for: [productId])
            guard let product = products.first else {
                return .error("Product not found in the App Store")
            }

            let result = try await product.purchase(options: purchaseOptions())
            return await resolve(result, label: "native purchase")
        } catch {
            return .error(error.localizedDescription)
        }
    }

    // MARK: - Restore

    func restore() async -> TransactionResult {
        var restoredCount = 0
        for await result in Transaction.currentEntitlements {
            if let transaction = try? checkVerified(result) {
                await transaction.finish()
                restoredCount += 1
            }
        }
        print("[Shaker] Observer mode: restored \(restoredCount) transactions")
        return restoredCount > 0 ? .success : .cancelled
    }

    // MARK: - Promo Offer Purchase

    func purchaseWithPromoOffer(
        productId: String,
        storeOfferId: String
    ) async -> TransactionResult {
        guard let signProvider = signPromotionalOfferProvider else {
            return .error("Promo offer signing not available")
        }

        do {
            let products = try await Product.products(for: [productId])
            guard let product = products.first else {
                return .error("Product not found in the App Store")
            }

            let signature: PLYOfferSignatureData = try await withCheckedThrowingContinuation { continuation in
                signProvider(productId, storeOfferId, { sig in
                    continuation.resume(returning: sig)
                }, { error in
                    continuation.resume(throwing: error)
                })
            }

            var options = purchaseOptions()
            if let decodedSignature = Data(base64Encoded: signature.signature) {
                options.insert(.promotionalOffer(
                    offerID: signature.identifier,
                    keyID: signature.keyIdentifier,
                    nonce: signature.nonce,
                    signature: decodedSignature,
                    timestamp: Int(signature.timestamp)
                ))
            }

            let result = try await product.purchase(options: options)
            return await resolve(result, label: "promo offer purchase")
        } catch {
            return .error(error.localizedDescription)
        }
    }

    // MARK: - Helpers

    private func purchaseOptions() -> Set<Product.PurchaseOption> {
        var options: Set<Product.PurchaseOption> = []
        if let userId = anonymousUserIdProvider?().lowercased(),
           let uuid = UUID(uuidString: userId) {
            options.insert(.appAccountToken(uuid))
        }
        return options
    }

    private func resolve(_ result: Product.PurchaseResult, label: String) async -> TransactionResult {
        switch result {
        case .success(let verification):
            do {
                let transaction = try checkVerified(verification)
                await transaction.finish()
                print("[Shaker] Observer mode: \(label) successful")
                return .success
            } catch {
                return .error(error.localizedDescription)
            }
        case .userCancelled:
            return .cancelled
        case .pending:
            return .error("Purchase pending approval")
        @unknown default:
            return .error("Unknown purchase result")
        }
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified(_, let error):
            throw error
        case .verified(let value):
            return value
        }
    }
}

/// Data structure for promo offer signatures — decouples from PLYOfferSignature
struct PLYOfferSignatureData {
    let identifier: String
    let keyIdentifier: String
    let nonce: UUID
    let signature: String
    let timestamp: Int
}
