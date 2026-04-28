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

    static let light = ShakerTokens(
        dark: false,
        bg: Color(hex: 0xF3F1F9),
        bgElev: .white,
        bgCard: .white,
        bgSubtle: Color(hex: 0xECE9F4),
        indigo: Color(hex: 0x3C4876),
        indigoText: Color(hex: 0x3C4876),
        indigoSoft: Color(hex: 0xE4E6F3),
        accent: Color(hex: 0x2B79E4),
        accentSoft: Color(hex: 0x2B79E4, alpha: 0.1),
        orange: Color(hex: 0xE8723F),
        gold: Color(hex: 0xF5B93A),
        goldSoft: Color(hex: 0xFFF4D9),
        green: Color(hex: 0x23C071),
        danger: Color(hex: 0xD33A3A),
        text: Color(hex: 0x1C1E2C),
        textSec: Color(hex: 0x6A6F88),
        textTer: Color(hex: 0x9EA2B6),
        hair: Color(hex: 0x3C4876, alpha: 0.12),
        hairStrong: Color(hex: 0x3C4876, alpha: 0.2),
        inputBg: Color(hex: 0xECE9F4),
        onIndigo: .white
    )

    static let dark = ShakerTokens(
        dark: true,
        bg: Color(hex: 0x0F1020),
        bgElev: Color(hex: 0x1B1D30),
        bgCard: Color(hex: 0x1B1D30),
        bgSubtle: Color(hex: 0x13142A),
        indigo: Color(hex: 0x8A96C9),
        indigoText: Color(hex: 0xB3BDE4),
        indigoSoft: Color(hex: 0x8A96C9, alpha: 0.18),
        accent: Color(hex: 0x4F92F0),
        accentSoft: Color(hex: 0x4F92F0, alpha: 0.15),
        orange: Color(hex: 0xF08B5F),
        gold: Color(hex: 0xF5B93A),
        goldSoft: Color(hex: 0xF5B93A, alpha: 0.15),
        green: Color(hex: 0x3ED58B),
        danger: Color(hex: 0xF56A6A),
        text: Color(hex: 0xF4F3FB),
        textSec: Color(hex: 0x9EA2B6),
        textTer: Color(hex: 0x6E7392),
        hair: Color.white.opacity(0.08),
        hairStrong: Color.white.opacity(0.14),
        inputBg: Color.white.opacity(0.06),
        onIndigo: Color(hex: 0x0F1020)
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
