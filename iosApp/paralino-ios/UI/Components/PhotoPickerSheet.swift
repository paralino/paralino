//
//  PhotoPickerSheet.swift
//  paralino-ios
//
//  Created by Zelimir on 07.07.23.
//

import Foundation
import SwiftUI
import shared

struct PhotoPickerSheet: ViewModifier {
    
    @Binding var bottomSheet: BottomSheetPickerTypeEmojiPickerBottomSheet?
    var onAction: ((ImageModel) -> Void)?
    
    var isShowingSheet: Binding<Bool> {
        Binding(get: { bottomSheet != nil }, set: { _ in bottomSheet = nil })
    }
    
    func body(content: Content) -> some View {
        content
            .sheet(isPresented: isShowingSheet) {
                let emojis = bottomSheet?.emojis ?? []
                VStack {
                    ScrollView {
                        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8, alignment: .center), count: 6), spacing: 8) {
                            ForEach(emojis.indices, id: \.self) { index in
                                Button(
                                    action: {
                                        onAction?(ImageModel.companion.emoji(emoji: Emojis.shared.getResourceFromIcon(icon: emojis[index]).assetImageName))
                                    }
                                ) {
                                    Image(uiImage: Emojis.shared.getResourceFromIcon(icon: emojis[index]).toUIImage()!)
                                        .resizable()
                                        .imageScale(.medium)
                                        .padding(8)
                                }
                                .aspectRatio(1.0, contentMode: .fill)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                                .background(Color.themeSurface)
                                .cornerRadius(10)
                                .clipped()
                            }
                        }
                        .padding()
                        .presentationDetents([.medium, .large])
                    }
                }
            }
    }
}

extension View {
    func photoPickerSheet(_ data: Binding<BottomSheetPickerTypeEmojiPickerBottomSheet?>, onAction: @escaping (ImageModel) -> Void) -> some View {
        self.modifier(PhotoPickerSheet(bottomSheet: data, onAction: onAction))
    }
}
