//
//  GroupStatusTag.swift
//  paralino-ios
//
//  Created by Zelimir on 09.07.23.
//

import SwiftUI
import shared

struct GroupStatusTag: View {
    
    let status: GroupStatusTagType?
    
    var tagColor: Color {
        if let status = status {
            switch status {
            case .sharingActive: return Color.accentDark
            case .unverified: return Color.themeOnError
            default: return Color.themeSecondary
            }
        } else {
            return Color.themeSecondary
        }
    }
    
    let tagTextColor: Color = Color.themeOnPrimary
    var tagText: String {
        if let status = status {
            switch status {
            case .sharingActive: return MR.strings().dashboard_groups_section_active.localizedString()
            case .unverified: return MR.strings().dashboard_groups_section_unverified.localizedString()
            case .inactive: return MR.strings().group_members_status_inactive.localizedString()
            default: return ""
            }
        } else {
            return ""
        }
    }
    var tagIcon: String? {
        if let status = status {
            switch status {
            case .sharingActive: return "location.circle.fill"
            case .unverified: return "exclamationmark.triangle"
            default: return nil
            }
        } else {
            return nil
        }
    }
    
    
    var body: some View {
        if let status = status {
            HStack(alignment: .center, spacing: 4) {
                if let tagIcon = tagIcon {
                    Image(systemName: tagIcon)
                        .resizable()
                        .frame(width: 16, height: 16)
                        .foregroundColor(tagColor)
                    
                }
                
                Text(tagText)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(tagTextColor.opacity(0.8))
                    .padding(EdgeInsets(top: 1, leading: 6, bottom: 1, trailing: 6))
                    .background(tagColor)
                    .clipShape(Capsule())
            }
        } else {
            EmptyView()
        }
    }
}


struct GroupStatusTag_Previews: PreviewProvider {
    static var previews: some View {
        GroupStatusTag(status: .sharingActive)
    }
}
