import SwiftUI

struct ShakerTokens {
    let dark: Bool
    let bg: Color
    let bgElev: Color
    let bgCard: Color
    let bgSubtle: Color
    let indigo: Color
    let indigoText: Color
    let indigoSoft: Color
    let accent: Color
    let accentSoft: Color
    let orange: Color
    let gold: Color
    let goldSoft: Color
    let green: Color
    let danger: Color
    let text: Color
    let textSec: Color
    let textTer: Color
    let hair: Color
    let hairStrong: Color
    let inputBg: Color
    let onIndigo: Color

    // "Cocktail lounge" palette — kept strictly in sync with the Android app
    // (android/.../ui/theme/ShakerTokens.kt): deep bottle green as primary
    // (token still named `indigo` to avoid a mechanical rename), burnished gold
    // accents, warm cream backgrounds (light) / charred green (dark).
    static let light = ShakerTokens(
        dark: false,
        bg: Color(hex: 0xF7F3EA),
        bgElev: .white,
        bgCard: Color(hex: 0xFFFDF8),
        bgSubtle: Color(hex: 0xEFE9DB),
        indigo: Color(hex: 0x1E3B2F),
        indigoText: Color(hex: 0x1E3B2F),
        indigoSoft: Color(hex: 0xDFE8DF),
        accent: Color(hex: 0xB05A2E),
        accentSoft: Color(hex: 0xB05A2E, alpha: 0.1),
        orange: Color(hex: 0xC96F3B),
        gold: Color(hex: 0xC9921E),
        goldSoft: Color(hex: 0xF6EDD4),
        green: Color(hex: 0x2E8B57),
        danger: Color(hex: 0xC23B3B),
        text: Color(hex: 0x20251F),
        textSec: Color(hex: 0x6B7066),
        textTer: Color(hex: 0x9CA095),
        hair: Color(hex: 0x1E3B2F, alpha: 0.12),
        hairStrong: Color(hex: 0x1E3B2F, alpha: 0.2),
        inputBg: Color(hex: 0xEFE9DB),
        onIndigo: Color(hex: 0xF7F3EA)
    )

    static let dark = ShakerTokens(
        dark: true,
        bg: Color(hex: 0x0E120F),
        bgElev: Color(hex: 0x1A211B),
        bgCard: Color(hex: 0x1A211B),
        bgSubtle: Color(hex: 0x141A15),
        indigo: Color(hex: 0x9BC4A8),
        indigoText: Color(hex: 0xBCD9C6),
        indigoSoft: Color(hex: 0x9BC4A8, alpha: 0.18),
        accent: Color(hex: 0xE08A52),
        accentSoft: Color(hex: 0xE08A52, alpha: 0.15),
        orange: Color(hex: 0xE08A52),
        gold: Color(hex: 0xD8A93E),
        goldSoft: Color(hex: 0xD8A93E, alpha: 0.15),
        green: Color(hex: 0x4FBF82),
        danger: Color(hex: 0xE96A6A),
        text: Color(hex: 0xF2F4EE),
        textSec: Color(hex: 0x9FA89D),
        textTer: Color(hex: 0x6F7870),
        hair: Color.white.opacity(0.08),
        hairStrong: Color.white.opacity(0.14),
        inputBg: Color.white.opacity(0.06),
        onIndigo: Color(hex: 0x0E120F)
    )
}

private struct ShakerTokensKey: EnvironmentKey {
    static let defaultValue: ShakerTokens = .light
}

extension EnvironmentValues {
    var shakerTokens: ShakerTokens {
        get { self[ShakerTokensKey.self] }
        set { self[ShakerTokensKey.self] = newValue }
    }
}

struct ShakerThemeModifier: ViewModifier {
    let themeMode: String

    @Environment(\.colorScheme) private var systemScheme

    func body(content: Content) -> some View {
        let useDark: Bool
        switch themeMode {
        case "dark": useDark = true
        case "light": useDark = false
        default: useDark = systemScheme == .dark
        }
        let tokens: ShakerTokens = useDark ? .dark : .light
        return content
            .environment(\.shakerTokens, tokens)
            .preferredColorScheme(themeMode == "system" ? nil : (useDark ? .dark : .light))
    }
}

extension View {
    func shakerTheme(mode: String) -> some View {
        modifier(ShakerThemeModifier(themeMode: mode))
    }
}

extension Color {
    init(hex: UInt32, alpha: Double = 1.0) {
        let r = Double((hex >> 16) & 0xFF) / 255
        let g = Double((hex >> 8) & 0xFF) / 255
        let b = Double(hex & 0xFF) / 255
        self.init(red: r, green: g, blue: b, opacity: alpha)
    }
}
