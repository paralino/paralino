//
//  GroupListPicker.swift
//  paralino-ios
//
//  Created by Zelimir on 09.07.23.
//

import SwiftUI
import shared

struct GroupListItem: View {
    let data: GroupItemState
    let onClick: () -> Void
    let onEdit: () -> Void
    
    var body: some View {
        HStack(alignment: .center) {
            ImageModelImage(imageModel: data.image, useBackgroundForEmoji: true)
                .frame(width: 72, height: 72)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.themeSecondary, lineWidth: 1)
                )
            
            Spacer().frame(width: 16)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(data.name)
                    .font(.headline)
                    .lineLimit(3)
                
                HStack(alignment: .center) {
                    Image(systemName: "person.2.fill")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 16, height: 16)
                        .foregroundColor(.secondary)
                    Text("\(data.userCount)")
                        .font(.caption)
                        .fontWeight(.bold)
                }
                
                if let status = getStatus(for: data.uiType) {
                    GroupStatusTag(status: status)
                }
            }
            
            Spacer()
            
            if data.canEdit {
                Image(systemName: "gearshape")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .padding(10)
                    .frame(width: 40, height: 40)
                    .foregroundColor(.white)
                    .background(Color.themePrimary)
                    .clipShape(Circle())
                    .onTapGesture(perform: onEdit)
            }
        }
        .padding(EdgeInsets(top: 16, leading: 16, bottom: 16, trailing: 16))
        .frame(maxWidth: .infinity)
        .background(Color.themeSurface)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            data.isSelected ?
            RoundedRectangle(cornerRadius: 16, style: .circular)
                .inset(by: 1)
                .stroke(Color.accent, lineWidth: 2) : nil
        )
        .contentShape(RoundedRectangle(cornerRadius: 16))
        .onTapGesture(perform: onClick)
    }
    
    private func getStatus(for uiType: GroupItemState.GroupUiType) -> GroupStatusTagType? {
        switch uiType {
        case .unverified:
            return .unverified
        case .active:
            return .sharingActive
        default:
            return nil
        }
    }
}

