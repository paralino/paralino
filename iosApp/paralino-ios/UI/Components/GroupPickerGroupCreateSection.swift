//
//  GroupPickerGroupCreateSection.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-11-10.
//
import SwiftUI
import shared

struct GroupPickerGroupCreateSection: View {
    let groupsCount: Int
    let groupsLimit: Int?
    let canCreateGroup: Bool
    
    var onCreateGroup: (() -> Void)?
    var onJoinGroup: (() -> Void)?
    var onOpenSubscription: (() -> Void)?
    
    @Environment(\.colorScheme) private var colorScheme
    
    var body: some View {
        VStack(spacing: 0) {
            // Create New Group Button
            VStack {
                HStack {
                    Image(systemName: "plus")
                        .resizable()
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 14, height: 14)
                        .foregroundColor(colorScheme == .dark ? Color.accent : Color.accentDark)
                    
                    Text(MR.shared.strings.dashboard_groups_add_button.localizedString())
                        .fontWeight(.semibold)
                        .foregroundColor(colorScheme == .dark ? Color.accent : Color.accentDark)
                        .padding(.horizontal, 6)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    PlanLimitCount(
                        count: groupsCount,
                        limit: groupsLimit,
                        backgroundColor: .clear,
                        contentColor: colorScheme == .dark ? Color.accent : Color.accentDark
                    )
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 20)
                
                if !canCreateGroup {
                    Button(action: { onOpenSubscription?() }) {
                        HStack {
                            Text(MR.shared.strings.dashboard_groups_create_plan_limit.localizedString())
                                .foregroundColor(.red)
                                .font(.system(size: 12))
                                .lineLimit(1)
                                .frame(maxWidth: .infinity, alignment: .leading)
                            
                            Image(systemName: "chevron.right")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .padding(8)
                                .frame(width: 24, height: 24)
                                .foregroundColor(Color.themePrimary)
                                .background(Color.lightBackground)
                                .clipShape(Circle())
                        }
                        .padding(.horizontal, 16)
                        .padding(.bottom, 12)
                    }
                }
            }
            .background(canCreateGroup ? Color.accentLight.opacity(0.2) : Color.errorBackground)
            .background(Color.themeSurface)
            .cornerRadius(12)
            .padding(.vertical, 4)
            .zIndex(1)
            .onTapGesture { onCreateGroup?() }
            
            // Join Group Button
            HStack {
                Image(systemName: "link.badge.plus")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .frame(width: 18, height: 18)
                    .foregroundColor(.primary)
                
                Text(MR.shared.strings.dashboard_groups_join_button.localizedString())
                    .font(.system(size: 12))
                    .foregroundColor(.primary)
                    .padding(.horizontal, 6)
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                Image(systemName: "chevron.right")
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .padding(8)
                    .frame(width: 24, height: 24)
                    .foregroundColor(Color.themeOnSurface.opacity(0.4))
                    .clipShape(Circle())
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 16)
            .background(Color.themeSurface)
            .cornerRadius(8)
            .padding(.top, 8)
            .zIndex(0)
            .onTapGesture { onJoinGroup?() }
        }
        .padding(.top, 8)
        .padding(.bottom, 16)
    }
}
