package com.purchasely.shaker.domain.model

/**
 * Mood-based discovery filter. Each mood matches cocktails through their tags
 * (or spirit for [ZERO_PROOF]) and is reported to Purchasely as the
 * `preferred_mood` user attribute for audience targeting.
 */
enum class CocktailMood(
    val key: String,
    val label: String,
    val emoji: String,
    val matchingTags: Set<String>,
) {
    REFRESHING(
        key = "refreshing",
        label = "Refreshing",
        emoji = "🌿",
        matchingTags = setOf("refreshing", "crisp", "minty", "citrus", "light"),
    ),
    STRONG(
        key = "strong",
        label = "Strong",
        emoji = "🥃",
        matchingTags = setOf("strong", "bold", "smoky", "bitter", "complex"),
    ),
    SWEET(
        key = "sweet",
        label = "Sweet",
        emoji = "🍑",
        matchingTags = setOf("sweet", "fruity", "creamy", "tropical"),
    ),
    ZERO_PROOF(
        key = "zero_proof",
        label = "Zero proof",
        emoji = "🧃",
        matchingTags = emptySet(), // matches on spirit == non-alcoholic instead
    ),
    PARTY(
        key = "party",
        label = "Party",
        emoji = "🎉",
        matchingTags = setOf("celebratory", "bubbly", "sparkling", "colorful", "fizzy"),
    );

    fun matches(cocktail: Cocktail): Boolean =
        if (this == ZERO_PROOF) {
            cocktail.spirit == "non-alcoholic"
        } else {
            cocktail.tags.any { it in matchingTags }
        }
}
