import Foundation

enum TransactionResult {
    case success
    case cancelled
    case error(String?)
    case idle
}
