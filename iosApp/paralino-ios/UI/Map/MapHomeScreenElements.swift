//
//  MapHomeScreenElements.swift
//  paralino-ios
//
//  Created by Zelimir on 09.07.23.
//

import SwiftUI
import shared

struct MapHomeScreenElements: View {
    
    let state: MapHomeScreenState?
    let pickGroup: () -> Void
    let openProfile: () -> Void
    let focusOnGroup: () -> Void
    
    var body: some View {
        if let readyState = state as? MapHomeScreenState.Ready {
            VStack {
                HStack {
                    ImageModelImage(imageModel: readyState.groupImage, useBackgroundForEmoji: true)
                        .frame(width: 60, height: 60)
                        .clipShape(Circle())
                        .onTapGesture { pickGroup() }
                    
                    Button(readyState.groupName, action: { pickGroup() })
                    Spacer()
                    ImageModelImage(imageModel: readyState.currentUserImage, useBackgroundForEmoji: true)
                        .frame(width: 60, height: 60)
                        .clipShape(Circle())
                        .onTapGesture { openProfile() }
                }
                
                Spacer()
                
                HStack {
                    Spacer()
                    Button(action: { focusOnGroup() }) {
                        Label(MR.strings().button_ok.localizedString(), systemImage: "plus")
                    }
                }.padding(.bottom)
            }
            .padding()
        } else if let emptyState = state as? MapHomeScreenState.Empty {
            ImageModelImage(imageModel: emptyState.currentUserImage, useBackgroundForEmoji: true)
                .frame(width: 60, height: 60)
                .clipShape(Circle())
                .onTapGesture { openProfile() }
        } else {
            EmptyView()
        }
    }
}
