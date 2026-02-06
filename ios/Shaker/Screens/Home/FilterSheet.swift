import SwiftUI

struct FilterSheet: View {

    @ObservedObject var viewModel: HomeViewModel
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    // Spirit (multi-select)
                    filterSection(title: "Spirit") {
                        FlowLayout(spacing: 8) {
                            ForEach(viewModel.availableSpirits, id: \.self) { spirit in
                                FilterChipView(
                                    text: spirit.capitalized,
                                    isSelected: viewModel.selectedSpirits.contains(spirit),
                                    action: { viewModel.toggleSpirit(spirit) }
                                )
                            }
                        }
                    }

                    // Category (multi-select)
                    filterSection(title: "Category") {
                        FlowLayout(spacing: 8) {
                            ForEach(viewModel.availableCategories, id: \.self) { category in
                                FilterChipView(
                                    text: category.capitalized,
                                    isSelected: viewModel.selectedCategories.contains(category),
                                    action: { viewModel.toggleCategory(category) }
                                )
                            }
                        }
                    }

                    // Difficulty (single-select)
                    filterSection(title: "Difficulty") {
                        FlowLayout(spacing: 8) {
                            ForEach(viewModel.availableDifficulties, id: \.self) { difficulty in
                                FilterChipView(
                                    text: difficulty.capitalized,
                                    isSelected: viewModel.selectedDifficulty == difficulty,
                                    action: { viewModel.selectDifficulty(difficulty) }
                                )
                            }
                        }
                    }
                }
                .padding(16)
            }
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Clear") {
                        viewModel.clearFilters()
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") {
                        dismiss()
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }

    @ViewBuilder
    private func filterSection<Content: View>(title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundStyle(.secondary)
            content()
        }
    }
}

struct FilterChipView: View {

    let text: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(text)
                .font(.subheadline)
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(isSelected ? Color.orange : Color(.secondarySystemBackground))
                .foregroundStyle(isSelected ? .white : .primary)
                .clipShape(Capsule())
        }
    }
}

/// Simple flow layout that wraps children to next line
struct FlowLayout: Layout {

    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var currentX: CGFloat = 0
        var currentY: CGFloat = 0
        var lineHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if currentX + size.width > maxWidth && currentX > 0 {
                currentX = 0
                currentY += lineHeight + spacing
                lineHeight = 0
            }
            currentX += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }

        return CGSize(width: maxWidth, height: currentY + lineHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var currentX: CGFloat = bounds.minX
        var currentY: CGFloat = bounds.minY
        var lineHeight: CGFloat = 0

        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if currentX + size.width > bounds.maxX && currentX > bounds.minX {
                currentX = bounds.minX
                currentY += lineHeight + spacing
                lineHeight = 0
            }
            subview.place(at: CGPoint(x: currentX, y: currentY), proposal: .unspecified)
            currentX += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
    }
}
