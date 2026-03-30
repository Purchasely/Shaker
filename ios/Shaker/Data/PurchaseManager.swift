import Foundation
import StoreKit
import Purchasely

@available(iOS 15.0, *)
class PurchaseManager {

    static let shared = PurchaseManager()

    private init() {}

    /// Purchase a product natively via StoreKit 2.
    /// Called from the paywall interceptor when in Observer mode.
    func purchase(productId: String) async throws -> Transaction {
        let products = try await Product.products(for: [productId])
        guard let product = products.first else {
            throw PurchaseError.productNotFound
        }

        // Use Purchasely anonymous user ID (lowercased) as the app account token
        let userId = Purchasely.anonymousUserId.lowercased()

        var options: Set<Product.PurchaseOption> = []
        if let uuid = UUID(uuidString: userId) {
            options.insert(.appAccountToken(uuid))
        }

        let result = try await product.purchase(options: options)

        switch result {
        case .success(let verification):
            let transaction = try checkVerified(verification)
            await transaction.finish()
            Purchasely.synchronize()
            print("[Shaker] Observer mode: native purchase successful, synchronized")
            return transaction
        case .userCancelled:
            throw PurchaseError.cancelled
        case .pending:
            throw PurchaseError.pending
        @unknown default:
            throw PurchaseError.unknown
        }
    }

    /// Purchase a product with a promotional offer via StoreKit 2.
    /// Uses Purchasely.signPromotionalOffer() to generate the required signature.
    func purchaseWithPromoOffer(
        productId: String,
        storeOfferId: String
    ) async throws -> Transaction {
        let products = try await Product.products(for: [productId])
        guard let product = products.first else {
            throw PurchaseError.productNotFound
        }

        // Sign the promotional offer via Purchasely backend
        let signature = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<PLYOfferSignature, Error>) in
            Purchasely.signPromotionalOffer(
                storeProductId: productId,
                storeOfferId: storeOfferId,
                success: { signature in
                    continuation.resume(returning: signature)
                },
                failure: { error in
                    continuation.resume(throwing: error)
                }
            )
        }

        let userId = Purchasely.anonymousUserId.lowercased()
        var options: Set<Product.PurchaseOption> = []
        if let uuid = UUID(uuidString: userId) {
            options.insert(.appAccountToken(uuid))
        }

        // Add promotional offer to purchase options
        if let decodedSignature = Data(base64Encoded: signature.signature) {
            let offerOption: Product.PurchaseOption = .promotionalOffer(
                offerID: signature.identifier,
                keyID: signature.keyIdentifier,
                nonce: signature.nonce,
                signature: decodedSignature,
                timestamp: Int(signature.timestamp)
            )
            options.insert(offerOption)
        }

        let result = try await product.purchase(options: options)

        switch result {
        case .success(let verification):
            let transaction = try checkVerified(verification)
            await transaction.finish()
            Purchasely.synchronize()
            print("[Shaker] Observer mode: promo offer purchase successful, synchronized")
            return transaction
        case .userCancelled:
            throw PurchaseError.cancelled
        case .pending:
            throw PurchaseError.pending
        @unknown default:
            throw PurchaseError.unknown
        }
    }

    /// Restore all completed transactions via StoreKit 2.
    func restoreAllTransactions() async throws {
        var restoredCount = 0
        for await result in Transaction.currentEntitlements {
            if let transaction = try? checkVerified(result) {
                await transaction.finish()
                restoredCount += 1
            }
        }
        Purchasely.synchronize()
        print("[Shaker] Observer mode: restored \(restoredCount) transactions, synchronized")
    }

    private func checkVerified<T>(_ result: VerificationResult<T>) throws -> T {
        switch result {
        case .unverified(_, let error):
            throw error
        case .verified(let value):
            return value
        }
    }

    enum PurchaseError: LocalizedError {
        case productNotFound
        case cancelled
        case pending
        case unknown

        var errorDescription: String? {
            switch self {
            case .productNotFound: return "Product not found in the App Store"
            case .cancelled: return "Purchase cancelled"
            case .pending: return "Purchase pending approval"
            case .unknown: return "Unknown purchase error"
            }
        }
    }
}
