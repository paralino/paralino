//
//  ImageModelImage.swift
//  paralino-ios
//
//  Created by Zelimir on 09.07.23.
//

import SwiftUI
import shared

struct ImageModelImage: View {
    let imageModel: ImageModel
    var useBackgroundForEmoji: Bool = true
    
    var body: some View {
        if let imageType = imageModel.getImageType() {
            switch imageType {
            case .emoji:
                if let emoji = imageModel.getImageValue() {
                    GeometryReader { geometry in
                        let imageSize = min(geometry.size.height, geometry.size.width) * 0.8
                        ZStack(alignment: .center) {
                            Image(uiImage: Emojis.shared.getResourceFromIcon(icon: emoji).toUIImage()!)
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: imageSize, height: imageSize, alignment: .center)
                                .padding(8)
                        }
                        .aspectRatio(1.0, contentMode: .fill)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                    .aspectRatio(1.0, contentMode: .fill)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(useBackgroundForEmoji ? Color.themeSurface : Color.clear)
                }
            case .placeholder:
                GeometryReader { geometry in
                    let imageSize = min(geometry.size.height, geometry.size.width) * 0.8
                    ZStack(alignment: .center) {
                        Image(systemName: "cart")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: imageSize, height: imageSize, alignment: .center)
                            .padding(8)
                    }
                    .aspectRatio(1.0, contentMode: .fill)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
                .aspectRatio(1.0, contentMode: .fill)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(useBackgroundForEmoji ? Color.themeSurface : Color.clear)
            default:
                EmptyView()
            }
        } else {
            EmptyView()
        }
    }
}


struct ImageModelImage_Previews: PreviewProvider {
    static var previews: some View {
        ImageModelImage(
            imageModel: ImageModel.companion.placeholder(),
            useBackgroundForEmoji: true
        )
    }
}

extension Image {
    init(resource: KeyPath<MR.images, ResourcesImageResource>) {
        self.init(uiImage: MR.images()[keyPath: resource].toUIImage()!)
    }
}
