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

    var body: some View {
        ZStack {
            backgroundColor

            if let uiImage = loadBundleImage() {
                Image(uiImage: uiImage)
                    .resizable()
                    .aspectRatio(contentMode: .fill)
            }
        }
        .clipped()
    }

    private func loadBundleImage() -> UIImage? {
        let imageName = cocktail.image.replacingOccurrences(of: ".jpg", with: "")
        if let path = Bundle.main.path(forResource: imageName, ofType: "jpg") {
            return UIImage(contentsOfFile: path)
        }
        return nil
    }
}
