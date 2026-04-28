import SwiftUI

// MARK: - Public entry (same name as the previous helper for call-site compat)
struct CocktailImage: View {
    let cocktail: Cocktail
    var body: some View {
        CocktailArt(cocktail: cocktail)
    }
}

// MARK: - Procedural cocktail art — drawn with Canvas
struct CocktailArt: View {
    let cocktail: Cocktail

    var body: some View {
        let spec = Self.spec(for: cocktail)
        Canvas { ctx, size in
            Self.draw(spec: spec, in: ctx, size: size)
        }
    }

    // MARK: Spec
    enum Glass { case martini, coupe, rocks, highball }
    enum Garnish { case mint, cherry, lime, orange, pineapple, beans, none }

    struct Spec {
        let hue: Double
        let tint1: Color
        let tint2: Color
        let glass: Glass
        let garnish: Garnish
    }

    static func spec(for cocktail: Cocktail) -> Spec {
        switch cocktail.id {
        case "virgin-mojito", "mojito", "nojito":
            return Spec(hue: 120, tint1: hex(0xC7F0C8), tint2: hex(0x7CCB8F), glass: .highball, garnish: .mint)
        case "shirley-temple", "roy-rogers":
            return Spec(hue: 350, tint1: hex(0xFDD2D8), tint2: hex(0xEF6C7A), glass: .highball, garnish: .cherry)
        case "pina-colada", "virgin-pina-colada":
            return Spec(hue: 48, tint1: hex(0xFFF1C4), tint2: hex(0xF2C061), glass: .highball, garnish: .pineapple)
        case "manhattan":
            return Spec(hue: 28, tint1: hex(0xE6B278), tint2: hex(0x8E4A1E), glass: .martini, garnish: .orange)
        case "espresso-martini":
            return Spec(hue: 25, tint1: hex(0xC9A27A), tint2: hex(0x3B2416), glass: .martini, garnish: .beans)
        case "negroni":
            return Spec(hue: 5, tint1: hex(0xE98672), tint2: hex(0x9A2E1C), glass: .rocks, garnish: .orange)
        case "daiquiri":
            return Spec(hue: 55, tint1: hex(0xF4F0CC), tint2: hex(0xC7B970), glass: .martini, garnish: .lime)
        case "margarita":
            return Spec(hue: 68, tint1: hex(0xE4F2B8), tint2: hex(0x9CB949), glass: .coupe, garnish: .lime)
        case "old-fashioned", "whiskey-sour":
            return Spec(hue: 28, tint1: hex(0xE6B278), tint2: hex(0x8E4A1E), glass: .rocks, garnish: .orange)
        case "cosmopolitan":
            return Spec(hue: 340, tint1: hex(0xF2A0B5), tint2: hex(0xC8365E), glass: .martini, garnish: .lime)
        case "virgin-mary":
            return Spec(hue: 5, tint1: hex(0xF2A090), tint2: hex(0xB42C16), glass: .highball, garnish: .lime)
        case "arnold-palmer":
            return Spec(hue: 38, tint1: hex(0xF4D89C), tint2: hex(0xB86E20), glass: .highball, garnish: .lime)
        case "cinderella":
            return Spec(hue: 28, tint1: hex(0xF7D6A8), tint2: hex(0xE08B3A), glass: .coupe, garnish: .cherry)
        case "italian-soda":
            return Spec(hue: 200, tint1: hex(0xC8E1F2), tint2: hex(0x6A9CC9), glass: .highball, garnish: .lime)
        case "safe-sex-on-the-beach":
            return Spec(hue: 18, tint1: hex(0xF7C2A0), tint2: hex(0xE2753C), glass: .highball, garnish: .orange)
        default:
            switch cocktail.spirit.lowercased() {
            case "rum":
                return Spec(hue: 48, tint1: hex(0xFFF1C4), tint2: hex(0xF2C061), glass: .highball, garnish: .lime)
            case "whiskey":
                return Spec(hue: 28, tint1: hex(0xE6B278), tint2: hex(0x8E4A1E), glass: .rocks, garnish: .orange)
            case "gin":
                return Spec(hue: 200, tint1: hex(0xC8E1F2), tint2: hex(0x6A9CC9), glass: .martini, garnish: .lime)
            case "tequila":
                return Spec(hue: 68, tint1: hex(0xE4F2B8), tint2: hex(0x9CB949), glass: .coupe, garnish: .lime)
            case "vodka":
                return Spec(hue: 200, tint1: hex(0xE3ECF5), tint2: hex(0xADB9CF), glass: .martini, garnish: .lime)
            case "non-alcoholic":
                return Spec(hue: 120, tint1: hex(0xC7F0C8), tint2: hex(0x7CCB8F), glass: .highball, garnish: .mint)
            default:
                return Spec(hue: 350, tint1: hex(0xFDD2D8), tint2: hex(0xEF6C7A), glass: .highball, garnish: .cherry)
            }
        }
    }

    private static func hex(_ v: UInt32) -> Color { Color(hex: v) }

    // MARK: Drawing
    static func draw(spec: Spec, in ctx: GraphicsContext, size: CGSize) {
        let sx = size.width / 200
        let sy = size.height / 200
        func x(_ v: Double) -> CGFloat { CGFloat(v) * sx }
        func y(_ v: Double) -> CGFloat { CGFloat(v) * sy }

        // Background gradient
        let bgA = hslColor(h: spec.hue, s: 0.45, l: 0.88)
        let bgB = hslColor(h: spec.hue + 20, s: 0.55, l: 0.72)
        let bgRect = Path(CGRect(origin: .zero, size: size))
        ctx.fill(
            bgRect,
            with: .linearGradient(
                Gradient(colors: [bgA, bgB]),
                startPoint: .zero,
                endPoint: CGPoint(x: size.width, y: size.height)
            )
        )

        // Bokeh
        func bokeh(cx: Double, cy: Double, r: Double, alpha: Double) {
            let rect = CGRect(x: x(cx) - CGFloat(r) * sx, y: y(cy) - CGFloat(r) * sy,
                              width: CGFloat(r) * sx * 2, height: CGFloat(r) * sy * 2)
            ctx.fill(Path(ellipseIn: rect), with: .color(Color.white.opacity(alpha)))
        }
        bokeh(cx: 40, cy: 40, r: 30, alpha: 0.35)
        bokeh(cx: 165, cy: 55, r: 22, alpha: 0.25)
        bokeh(cx: 170, cy: 160, r: 35, alpha: 0.18)

        // Countertop
        var counter = Path()
        counter.move(to: CGPoint(x: x(0), y: y(160)))
        counter.addQuadCurve(to: CGPoint(x: x(200), y: y(160)), control: CGPoint(x: x(100), y: y(150)))
        counter.addLine(to: CGPoint(x: x(200), y: y(200)))
        counter.addLine(to: CGPoint(x: x(0), y: y(200)))
        counter.closeSubpath()
        ctx.fill(counter, with: .color(.black.opacity(0.08)))

        let liquid = GraphicsContext.Shading.linearGradient(
            Gradient(colors: [spec.tint1, spec.tint2]),
            startPoint: CGPoint(x: size.width / 2, y: 0),
            endPoint: CGPoint(x: size.width / 2, y: size.height)
        )

        switch spec.glass {
        case .martini: drawMartini(ctx, sx, sy, liquid)
        case .coupe: drawCoupe(ctx, sx, sy, liquid)
        case .rocks: drawRocks(ctx, sx, sy, liquid)
        case .highball: drawHighball(ctx, sx, sy, liquid)
        }

        switch spec.garnish {
        case .mint: drawMint(ctx, sx, sy)
        case .cherry: drawCherry(ctx, sx, sy)
        case .lime: drawLime(ctx, sx, sy)
        case .orange: drawOrange(ctx, sx, sy)
        case .pineapple: drawPineapple(ctx, sx, sy)
        case .beans: drawBeans(ctx, sx, sy)
        case .none: break
        }
    }

    private static func pt(_ sx: CGFloat, _ sy: CGFloat, _ x: Double, _ y: Double) -> CGPoint {
        CGPoint(x: CGFloat(x) * sx, y: CGFloat(y) * sy)
    }

    private static func drawMartini(_ ctx: GraphicsContext, _ sx: CGFloat, _ sy: CGFloat, _ liquid: GraphicsContext.Shading) {
        var bowl = Path()
        bowl.move(to: pt(sx, sy, 65, 75))
        bowl.addLine(to: pt(sx, sy, 135, 75))
        bowl.addLine(to: pt(sx, sy, 102, 115))
        bowl.closeSubpath()
        ctx.fill(bowl, with: liquid)

        var outline = Path()
        outline.move(to: pt(sx, sy, 60, 72))
        outline.addLine(to: pt(sx, sy, 140, 72))
        outline.addLine(to: pt(sx, sy, 102, 118))
        outline.closeSubpath()
        outline.move(to: pt(sx, sy, 102, 118))
        outline.addLine(to: pt(sx, sy, 102, 155))
        outline.move(to: pt(sx, sy, 82, 158))
        outline.addLine(to: pt(sx, sy, 122, 158))
        ctx.stroke(outline, with: .color(.white.opacity(0.85)), style: StrokeStyle(lineWidth: 2 * sx, lineCap: .round, lineJoin: .round))

        var shine = Path()
        shine.move(to: pt(sx, sy, 72, 76))
        shine.addLine(to: pt(sx, sy, 92, 106))
        ctx.stroke(shine, with: .color(.white.opacity(0.6)), style: StrokeStyle(lineWidth: 2 * sx, lineCap: .round))
    }

    private static func drawCoupe(_ ctx: GraphicsContext, _ sx: CGFloat, _ sy: CGFloat, _ liquid: GraphicsContext.Shading) {
        var bowl = Path()
        bowl.move(to: pt(sx, sy, 65, 78))
        bowl.addQuadCurve(to: pt(sx, sy, 135, 78), control: pt(sx, sy, 100, 110))
        bowl.closeSubpath()
        ctx.fill(bowl, with: liquid)

        var outline = Path()
        outline.move(to: pt(sx, sy, 60, 78))
        outline.addQuadCurve(to: pt(sx, sy, 140, 78), control: pt(sx, sy, 100, 120))
        outline.move(to: pt(sx, sy, 102, 120))
        outline.addLine(to: pt(sx, sy, 102, 155))
        outline.move(to: pt(sx, sy, 82, 158))
        outline.addLine(to: pt(sx, sy, 122, 158))
        ctx.stroke(outline, with: .color(.white.opacity(0.85)), style: StrokeStyle(lineWidth: 2 * sx, lineCap: .round))
    }

    private static func drawRocks(_ ctx: GraphicsContext, _ sx: CGFloat, _ sy: CGFloat, _ liquid: GraphicsContext.Shading) {
        let glass = Path(CGRect(x: 70 * sx, y: 80 * sy, width: 60 * sx, height: 65 * sy))
        ctx.fill(glass, with: .color(.white.opacity(0.15)))
        ctx.stroke(glass, with: .color(.white.opacity(0.8)), lineWidth: 2 * sx)
        let liquidRect = Path(CGRect(x: 72 * sx, y: 105 * sy, width: 56 * sx, height: 38 * sy))
        ctx.fill(liquidRect, with: liquid)

        let cube1 = Path(CGRect(x: 80 * sx, y: 100 * sy, width: 16 * sx, height: 16 * sy))
        ctx.fill(cube1, with: .color(.white.opacity(0.6)))
        let cube2 = Path(CGRect(x: 102 * sx, y: 108 * sy, width: 16 * sx, height: 16 * sy))
        ctx.fill(cube2, with: .color(.white.opacity(0.55)))

        var shine = Path()
        shine.move(to: pt(sx, sy, 76, 85))
        shine.addLine(to: pt(sx, sy, 76, 138))
        ctx.stroke(shine, with: .color(.white.opacity(0.55)), lineWidth: 2 * sx)
    }

    private static func drawHighball(_ ctx: GraphicsContext, _ sx: CGFloat, _ sy: CGFloat, _ liquid: GraphicsContext.Shading) {
        let glass = Path(CGRect(x: 75 * sx, y: 55 * sy, width: 50 * sx, height: 100 * sy))
        ctx.fill(glass, with: .color(.white.opacity(0.15)))
        ctx.stroke(glass, with: .color(.white.opacity(0.8)), lineWidth: 2 * sx)
        let liquidRect = Path(CGRect(x: 77 * sx, y: 85 * sy, width: 46 * sx, height: 68 * sy))
        ctx.fill(liquidRect, with: liquid)

        for (cx, cy, r, a) in [(92.0, 110.0, 2.5, 0.8), (102.0, 125.0, 1.8, 0.7), (112.0, 100.0, 2.0, 0.7), (97.0, 140.0, 1.5, 0.6)] {
            let rect = CGRect(x: (cx - r) * sx, y: (cy - r) * sy, width: r * 2 * sx, height: r * 2 * sy)
            ctx.fill(Path(ellipseIn: rect), with: .color(.white.opacity(a)))
        }

        var straw1 = Path()
        straw1.move(to: pt(sx, sy, 112, 52))
        straw1.addLine(to: pt(sx, sy, 105, 150))
        ctx.stroke(straw1, with: .color(Color(hex: 0xE85A68)), style: StrokeStyle(lineWidth: 3 * sx, lineCap: .round))

        var straw2 = Path()
        straw2.move(to: pt(sx, sy, 115, 52))
        straw2.addLine(to: pt(sx, sy, 108, 150))
        ctx.stroke(straw2, with: .color(Color(hex: 0xF4F4F4).opacity(0.6)), style: StrokeStyle(lineWidth: 3 * sx, lineCap: .round))

        var shine = Path()
        shine.move(to: pt(sx, sy, 80, 60))
        shine.addLine(to: pt(sx, sy, 80, 148))
        ctx.stroke(shine, with: .color(.white.opacity(0.55)), lineWidth: 2 * sx)
    }

    // MARK: - Garnishes
    private static func ellipse(_ ctx: GraphicsContext, cx: Double, cy: Double, rx: Double, ry: Double, rotate: Double, color: Color, _ sx: CGFloat, _ sy: CGFloat) {
        var c = ctx
        c.translateBy(x: cx * sx, y: cy * sy)
        c.rotate(by: .degrees(rotate))
        let rect = CGRect(x: -rx * sx, y: -ry * sy, width: rx * 2 * sx, height: ry * 2 * sy)
        c.fill(Path(ellipseIn: rect), with: .color(color))
    }

    private static func drawMint(_ ctx: GraphicsContext, _ sx: CGFloat, _ sy: CGFloat) {
        ellipse(ctx, cx: 95, cy: 58, rx: 8, ry: 12, rotate: -20, color: Color(hex: 0x4FA265), sx, sy)
        ellipse(ctx, cx: 108, cy: 52, rx: 9, ry: 13, rotate: 15, color: Color(hex: 0x5BB673), sx, sy)
        ellipse(ctx, cx: 100, cy: 48, rx: 7, ry: 10, rotate: -5, color: Color(hex: 0x6ECB86), sx, sy)
    }

    private static func drawCherry(_ ctx: GraphicsContext, _ sx: CGFloat, _ sy: CGFloat) {
        var stem = Path()
        stem.move(to: pt(sx, sy, 108, 50))
        stem.addQuadCurve(to: pt(sx, sy, 118, 46), control: pt(sx, sy, 112, 42))
        ctx.stroke(stem, with: .color(Color(hex: 0x6A4A2A)), lineWidth: 1.5 * sx)

        let berry = CGRect(x: (106 - 5) * sx, y: (54 - 5) * sy, width: 10 * sx, height: 10 * sy)
        ctx.fill(Path(ellipseIn: berry), with: .color(Color(hex: 0xC42E3A)))
        let shine = CGRect(x: (104 - 1.5) * sx, y: (53 - 1.5) * sy, width: 3 * sx, height: 3 * sy)
        ctx.fill(Path(ellipseIn: shine), with: .color(.white.opacity(0.6)))
    }

    private static func drawLime(_ ctx: GraphicsContext, _ sx: CGFloat, _ sy: CGFloat) {
        var c = ctx
        c.translateBy(x: 120 * sx, y: 52 * sy)
        c.rotate(by: .degrees(20))
        let big = CGRect(x: -8 * sx, y: -8 * sy, width: 16 * sx, height: 16 * sy)
        c.fill(Path(ellipseIn: big), with: .color(Color(hex: 0xB6D84A)))
        c.stroke(Path(ellipseIn: big), with: .color(Color(hex: 0x8AA836)), lineWidth: 1 * sx)
        let inner = CGRect(x: -5 * sx, y: -5 * sy, width: 10 * sx, height: 10 * sy)
        c.fill(Path(ellipseIn: inner), with: .color(Color(hex: 0xD9EC8E)))
    }

    private static func drawOrange(_ ctx: GraphicsContext, _ sx: CGFloat, _ sy: CGFloat) {
        var peel = Path()
        peel.move(to: pt(sx, sy, 100, 55))
        peel.addQuadCurve(to: pt(sx, sy, 115, 58), control: pt(sx, sy, 110, 48))
        peel.addQuadCurve(to: pt(sx, sy, 105, 62), control: pt(sx, sy, 112, 63))
        peel.addQuadCurve(to: pt(sx, sy, 100, 55), control: pt(sx, sy, 99, 60))
        peel.closeSubpath()
        ctx.fill(peel, with: .color(Color(hex: 0xF4A64A)))
    }

    private static func drawPineapple(_ ctx: GraphicsContext, _ sx: CGFloat, _ sy: CGFloat) {
        var leaves = Path()
        leaves.move(to: pt(sx, sy, 115, 40))
        leaves.addLine(to: pt(sx, sy, 118, 55))
        leaves.addLine(to: pt(sx, sy, 122, 38))
        leaves.addLine(to: pt(sx, sy, 126, 55))
        leaves.addLine(to: pt(sx, sy, 129, 42))
        leaves.addLine(to: pt(sx, sy, 127, 60))
        leaves.addLine(to: pt(sx, sy, 115, 60))
        leaves.closeSubpath()
        ctx.fill(leaves, with: .color(Color(hex: 0x3E8B3E)))

        var body = Path()
        body.move(to: pt(sx, sy, 115, 58))
        body.addQuadCurve(to: pt(sx, sy, 129, 58), control: pt(sx, sy, 122, 50))
        body.addLine(to: pt(sx, sy, 128, 65))
        body.addQuadCurve(to: pt(sx, sy, 116, 65), control: pt(sx, sy, 122, 68))
        body.closeSubpath()
        ctx.fill(body, with: .color(Color(hex: 0xF5C94C)))
    }

    private static func drawBeans(_ ctx: GraphicsContext, _ sx: CGFloat, _ sy: CGFloat) {
        ellipse(ctx, cx: 95, cy: 72, rx: 3, ry: 5, rotate: -15, color: Color(hex: 0x3B2416), sx, sy)
        ellipse(ctx, cx: 102, cy: 68, rx: 3, ry: 5, rotate: 5, color: Color(hex: 0x2F1D10), sx, sy)
        ellipse(ctx, cx: 109, cy: 72, rx: 3, ry: 5, rotate: 20, color: Color(hex: 0x3B2416), sx, sy)
    }
}

// HSL → Color (used for background hue sweep)
private func hslColor(h: Double, s: Double, l: Double) -> Color {
    let c = (1 - abs(2 * l - 1)) * s
    let hp = ((h.truncatingRemainder(dividingBy: 360)) + 360).truncatingRemainder(dividingBy: 360) / 60
    let x = c * (1 - abs(hp.truncatingRemainder(dividingBy: 2) - 1))
    let (r1, g1, b1): (Double, Double, Double)
    switch hp {
    case 0..<1: (r1, g1, b1) = (c, x, 0)
    case 1..<2: (r1, g1, b1) = (x, c, 0)
    case 2..<3: (r1, g1, b1) = (0, c, x)
    case 3..<4: (r1, g1, b1) = (0, x, c)
    case 4..<5: (r1, g1, b1) = (x, 0, c)
    default: (r1, g1, b1) = (c, 0, x)
    }
    let m = l - c / 2
    return Color(red: min(max(r1 + m, 0), 1), green: min(max(g1 + m, 0), 1), blue: min(max(b1 + m, 0), 1))
}

struct ShakerLogoView: View {
    var size: CGFloat = 40
    var color: Color = Color(hex: 0x3C4876)

    var body: some View {
        Canvas { ctx, s in
            let k = s.width / 40
            func p(_ x: Double, _ y: Double) -> CGPoint { CGPoint(x: x * k, y: y * k) }

            var cap = Path()
            cap.move(to: p(13, 6)); cap.addLine(to: p(27, 6))
            cap.addLine(to: p(25.5, 10)); cap.addLine(to: p(14.5, 10)); cap.closeSubpath()
            ctx.fill(cap, with: .color(color))

            var body = Path()
            body.move(to: p(14, 10))
            body.addLine(to: p(26, 10))
            body.addLine(to: p(24.5, 28))
            body.addCurve(to: p(20.5, 31.6), control1: p(24.4, 30), control2: p(22.5, 31.6))
            body.addLine(to: p(19.5, 31.6))
            body.addCurve(to: p(15.5, 28), control1: p(17.5, 31.6), control2: p(15.6, 30))
            body.closeSubpath()
            ctx.fill(body, with: .color(color))

            func dot(_ x: Double, _ y: Double, _ r: Double, _ a: Double) {
                let rect = CGRect(x: (x - r) * k, y: (y - r) * k, width: 2 * r * k, height: 2 * r * k)
                ctx.fill(Path(ellipseIn: rect), with: .color(.white.opacity(a)))
            }
            dot(20, 18, 2.5, 1.0)
            dot(22, 23, 1.5, 0.7)
            dot(18, 25, 1.2, 0.5)
        }
        .frame(width: size, height: size)
    }
}
