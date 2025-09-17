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
            if data.isSelected {
                Image(systemName: "checkmark")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 8, height: 8)
                    .foregroundColor(.white)
                    .padding([.leading, .trailing], 4)
                    .padding([.top, .bottom], 10)
                    .background(Color.themePrimary)
                    .clipShape(
                        .rect(
                            topLeadingRadius: 0,
                            bottomLeadingRadius: 0,
                            bottomTrailingRadius: 8,
                            topTrailingRadius: 8
                        )
                    )
            }
            
            if data.isSelected {
                Spacer().frame(width: 6)
            } else {
                Spacer().frame(width: 16)
            }
            
            ImageModelImage(imageModel: data.image, useBackgroundForEmoji: false)
                .frame(width: data.isPersonalSpace ? 72 : 72, height: data.isPersonalSpace ? 72 : 72)
                .clipShape(data.isPersonalSpace ? AnyShape(Circle()) : AnyShape(RoundedRectangle(cornerRadius: 12)))
                .overlay(
                    Group {
                        if data.isPersonalSpace {
                            Circle()
                                .stroke(Color.accent.opacity(0.3), lineWidth: 3)
                        } else {
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(Color.themeSecondary, lineWidth: 1)
                        }
                    }
                )
            
            Spacer().frame(width: 16)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(data.name)
                    .font(.headline)
                    .lineLimit(3)
                
                HStack(alignment: .center, spacing: 10) {
                    if let membersCount = data.membersCount {
                        HStack(alignment: .center, spacing: 2) {
                            Image(systemName: "person.2.fill")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 16, height: 16)
                                .foregroundColor(.secondary)
                            Text("\(membersCount)")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    if let devicesCount = data.devicesCount {
                        HStack(alignment: .center, spacing: 2) {
                            Image(systemName: "iphone")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .frame(width: 16, height: 16)
                                .foregroundColor(.secondary)
                            Text("\(devicesCount)")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    HStack(alignment: .center, spacing: 2) {
                        Image(systemName: "building.2.fill")
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(width: 16, height: 16)
                            .foregroundColor(.secondary)
                        Text("\(data.placesCount)")
                            .font(.caption)
                            .fontWeight(.bold)
                            .foregroundColor(.secondary)
                    }
                }
                
                if let status = getStatus(for: data.uiType) {
                    GroupStatusTag(status: status)
                }
            }
            
            Spacer()
            
            if data.canEdit {
                Divider()
                    .frame(width: 0.5, height: 50)
                    .background(Color.themeSecondary.opacity(0.4))
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
        .padding(EdgeInsets(top: 16, leading: 0, bottom: 16, trailing: 12))
        .frame(maxWidth: .infinity)
        .background(data.isPersonalSpace ? Color.accent.opacity(0.1) : Color.themeSurface)
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .overlay(
            data.isSelected ?
            RoundedRectangle(cornerRadius: 16, style: .circular)
                .inset(by: 1)
                .stroke(Color.themePrimary, lineWidth: 2) : nil
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

// Helper to use different shapes
struct AnyShape: Shape {
    private let path: (CGRect) -> Path
    
    init<S: Shape>(_ shape: S) {
        self.path = shape.path(in:)
    }
    
    func path(in rect: CGRect) -> Path {
        return path(rect)
    }
}

