//
//  Image+Extensions.swift
//  paralino-ios
//
//  Created by Zelimir on 15.04.23.
//

import Foundation
import SwiftUI

extension Image {
    func centerCropped() -> some View {
        GeometryReader { geo in
            self
                .resizable()
                .scaledToFill()
                .frame(width: geo.size.width, height: geo.size.height, alignment: .center)
                .clipped()
        }
    }
}
