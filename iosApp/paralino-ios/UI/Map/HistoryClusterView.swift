import SwiftUI

struct HistoryClusterView: View {
    let count: Int
    let isSelected: Bool
    
    var body: some View {
        HStack {
            Text("\(count)")
                .font(.caption)
                .fontWeight(.bold)
                .foregroundColor(Color.darkBackground)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.highlightGold)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .scaleEffect(isSelected ? 1.2 : 1.0)
        .animation(.easeInOut(duration: 0.2), value: isSelected)
        .shadow(color: .black.opacity(0.3), radius: 2, x: 0, y: 1)
    }
}

#Preview {
    VStack(spacing: 20) {
        HistoryClusterView(count: 3, isSelected: false)
        HistoryClusterView(count: 25, isSelected: true)
        HistoryClusterView(count: 150, isSelected: false)
    }
    .padding()
} 
