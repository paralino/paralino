//
//  MapGroupPicker.swift
//  paralino-ios
//
//  Created by Zelimir on 09.07.23.
//

import SwiftUI
import shared

struct MapGroupPicker: View {
    
    let data: MapHomeScreenState.Ready
    let onClick: () -> Void
    
    var body: some View {
        HStack(alignment: .center) {
            ImageModelImage(imageModel: data.groupImage, useBackgroundForEmoji: true, imageScale: 0.7)
                .frame(width: 46, height: 46)
                .clipShape(Circle())
                .overlay(
                    Circle().stroke(Color.themeSecondary, lineWidth: 2)
                )
            
            Spacer().frame(width: 8)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(data.groupName)
                    .font(.headline)
                    .lineLimit(1)
                
                HStack(alignment: .center, spacing: 4) {
                    Image(systemName: "person.2.fill")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 16, height: 16)
                        .foregroundColor(.secondary)
                    Text("\(data.userCount)")
                        .font(.caption)
                        .fontWeight(.bold)
                    
                    HStack {
                        HStack(alignment: .center, spacing: 2) {
                            Image(systemName: "largecircle.fill.circle")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 12, height: 12)
                                .foregroundColor(.accent)
                            Text("\(data.userCount - data.inactiveUserCount)")
                                .font(.caption)
                                .fontWeight(.bold)
                        }
                        
                        HStack(alignment: .center, spacing: 2) {
                            Image(systemName: "largecircle.fill.circle")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 12, height: 12)
                                .foregroundColor(Color.redMain)
                            Text("\(data.inactiveUserCount)")
                                .font(.caption)
                                .fontWeight(.bold)
                        }
                    }
                    .padding(.leading, 4)
                    .padding(.trailing, 6)
                    .padding(.vertical, 2)
                    .background(Color.themePrimary.opacity(0.2))
                    .clipShape(Capsule())
                }
            }
            
            
            Spacer().frame(width: 12)
            
            Image(systemName: "chevron.down")
                .resizable()
                .aspectRatio(contentMode: .fit)
                .padding(18)
                .frame(width: 46, height: 46)
                .foregroundColor(.themePrimary)
                .clipShape(Circle())
                .overlay(Circle().stroke(Color.themeSecondary, lineWidth: 1))
        }
        .padding(4)
        .background(Color.themeSurface)
        .clipShape(Capsule())
        .contentShape(Capsule())
        .onTapGesture(perform: onClick)
    }
}

