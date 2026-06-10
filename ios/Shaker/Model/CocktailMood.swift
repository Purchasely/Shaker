import Foundation

/// Mood-based discovery filter. Each mood matches cocktails through their tags
/// (or spirit for `.zeroProof`) and is reported to Purchasely as the
/// `preferred_mood` user attribute for audience targeting.
///
/// Kept strictly in sync with the Android app
/// (android/.../domain/model/CocktailMood.kt) — same keys, labels, emojis and
/// matching tags so console audiences behave identically on both platforms.
enum CocktailMood: String, CaseIterable, Identifiable {
    case refreshing
    case strong
    case sweet
    case zeroProof = "zero_proof"
    case party

    var id: String { rawValue }

    /// Stable key reported as the `preferred_mood` Purchasely user attribute.
    var key: String { rawValue }

    var label: String {
        switch self {
        case .refreshing: return "Refreshing"
        case .strong: return "Strong"
        case .sweet: return "Sweet"
        case .zeroProof: return "Zero proof"
        case .party: return "Party"
        }
    }

    var emoji: String {
        switch self {
        case .refreshing: return "🌿"
        case .strong: return "🥃"
        case .sweet: return "🍑"
        case .zeroProof: return "🧃"
        case .party: return "🎉"
        }
    }

    var matchingTags: Set<String> {
        switch self {
        case .refreshing: return ["refreshing", "crisp", "minty", "citrus", "light"]
        case .strong: return ["strong", "bold", "smoky", "bitter", "complex"]
        case .sweet: return ["sweet", "fruity", "creamy", "tropical"]
        case .zeroProof: return [] // matches on spirit == non-alcoholic instead
        case .party: return ["celebratory", "bubbly", "sparkling", "colorful", "fizzy"]
        }
    }

    func matches(_ cocktail: Cocktail) -> Bool {
        if self == .zeroProof {
            return cocktail.spirit == "non-alcoholic"
        }
        return cocktail.tags.contains { matchingTags.contains($0) }
    }
}
