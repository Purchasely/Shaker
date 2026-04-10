import Foundation
import StoreKit
import Combine

@available(iOS 15.0, *)
class PurchaseManager {

    static let shared = PurchaseManager()

    /// Subjects for reactive communication (set by PurchaselyWrapper)
    var purchaseSubject = PassthroughSubject<PurchaseRequest, Never>()
    var restoreSubject = PassthroughSubject<Void, Never>()
    let resultSubject = PassthroughSubject<TransactionResult, Never>()

    /// Anonymous user ID provider — injected to avoid direct PurchaselyWrapper dependency
    var anonymousUserIdProvider: (() -> String)?

    /// Sign promo offer — injected to avoid direct PurchaselyWrapper dependency
    var signPromotionalOfferProvider: ((_ productId: String, _ offerId: String, _ success: @escaping (PLYOfferSignatureData) -> Void, _ failure: @escaping (Error) -> Void) -> Void)?

    private var cancellables = Set<AnyCancellable>()

    private init() {
        purchaseSubject
            .sink { [weak self] request in
                Task { [weak self] in
                    await self?.handlePurchase(productId: request.productId)
                }
            }
            .store(in: &cancellables)

        restoreSubject
            .sink { [weak self] in
                Task { [weak self] in
                    await self?.handleRestore()
                }
            }
            .store(in: &cancellables)
    }

    // MARK: - Purchase

    private func handlePurchase(productId: String) async {
        do {
            let products = try await Product.products(for: [productId])
            guard let product = products.first else {
                resultSubject.send(.error("Product not found in the App Store"))
                return
            }

            var options: Set<Product.PurchaseOption> = []
            if let userId = anonymousUserIdProvider?().lowercased(),
               let uuid = UUID(uuidString: userId) {
                options.insert(.appAccountToken(uuid))
            }

            let result = try await product.purchase(options: options)

            switch result {
            case .success(let verification):
                let transaction = try checkVerified(verification)
                await transaction.finish()
                print("[Shaker] Observer mode: native purchase successful")
                resultSubject.send(.success)
            case .userCancelled:
                resultSubject.send(.cancelled)
            case .pending:
                resultSubject.send(.error("Purchase pending approval"))
            @unknown default:
                resultSubject.send(.error("Unknown purchase result"))
            }
        } catch {
            resultSubject.send(.error(error.localizedDescription))
        }
    }

    // MARK: - Restore

    private func handleRestore() async {
        var restoredCount = 0
        for await result in Transaction.currentEntitlements {
            if let transaction = try? checkVerified(result) {
                await transaction.finish()
                restoredCount += 1
            }
        }
        print("[Shaker] Observer mode: restored \(restoredCount) transactions")
        resultSubject.send(restoredCount > 0 ? .success : .cancelled)
    }

    // MARK: - Promo Offer Purchase (public for direct calls)

    func purchaseWithPromoOffer(
        productId: String,
        storeOfferId: String
    ) async throws {
        guard let signProvider = signPromotionalOfferProvider else {
            resultSubject.send(.error("Promo offer signing not available"))
            return
        }

        let products = try await Product.products(for: [productId])
        guard let product = products.first else {
            resultSubject.send(.error("Product not found in the App Store"))
            return
        }

        let signature = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<PLYOfferSignatureData, Error>) in
            signProvider(productId, storeOfferId, { sig in
                continuation.resume(returning: sig)
            }, { error in
                continuation.resume(throwing: error)
            })
        }

        var options: Set<Product.PurchaseOption> = []
        if let userId = anonymousUserIdProvider?().lowercased(),
           let uuid = UUID(uuidString: userId) {
            options.insert(.appAccountToken(uuid))
        }

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
            print("[Shaker] Observer mode: promo offer purchase successful")
            resultSubject.send(.success)
        case .userCancelled:
            resultSubject.send(.cancelled)
        case .pending:
            resultSubject.send(.error("Purchase pending approval"))
        @unknown default:
            resultSubject.send(.error("Unknown purchase result"))
        }
    }

    // MARK: - Verification

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
