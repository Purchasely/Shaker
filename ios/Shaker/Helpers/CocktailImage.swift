import SwiftUI

struct CocktailImage: View {

    let cocktail: Cocktail

    private var backgroundColor: Color {
        switch cocktail.spirit.lowercased() {
        case "rum": return Color(red: 0.83, green: 0.65, blue: 0.45)
        case "whiskey": return Color(red: 0.78, green: 0.56, blue: 0.09)
        case "gin": return Color(red: 0.53, green: 0.81, blue: 0.92)
        case "tequila": return Color(red: 0.60, green: 0.98, blue: 0.60)
        case "vodka": return Color(red: 0.91, green: 0.91, blue: 0.91)
        default: return Color(red: 1.0, green: 0.71, blue: 0.76)
        }
    }

    private var foregroundColor: Color {
        switch cocktail.spirit.lowercased() {
        case "rum": return Color(red: 0.55, green: 0.41, blue: 0.08)
        case "whiskey": return Color(red: 0.55, green: 0.36, blue: 0.05)
        case "gin": return Color(red: 0.27, green: 0.51, blue: 0.70)
        case "tequila": return Color(red: 0.13, green: 0.55, blue: 0.13)
        case "vodka": return Color(red: 0.50, green: 0.50, blue: 0.50)
        default: return Color(red: 0.70, green: 0.35, blue: 0.45)
        }
    }

    var body: some View {
        ZStack {
            backgroundColor

            Circle()
                .fill(foregroundColor.opacity(0.3))
                .padding(40)

            VStack(spacing: 4) {
                Spacer()
                Text(cocktail.name)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(foregroundColor)

                Text(cocktail.spirit.capitalized)
                    .font(.system(size: 12))
                    .foregroundStyle(foregroundColor.opacity(0.7))

                Spacer()
                    .frame(height: 20)
            }
        }
    }
}
