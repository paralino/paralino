//
//  GroupPickerSheet.swift
//  paralino-ios
//
//  Created by Zelimir on 09.07.23.
//

import SwiftUI
import shared

struct GroupPickerSheet: ViewModifier {
    
    @Binding var bottomSheet: BottomSheetPickerTypeGroupPickerBottomSheet?
    var createdGroups: [GroupItemState] {
        return bottomSheet?.createdGroups ?? []
    }
    var joinedGroups: [GroupItemState] {
        return bottomSheet?.joinedGroups ?? []
    }
    var groupsCount: Int { createdGroups.count + joinedGroups.count }
    
    var onSelectGroup: ((GroupItemState) -> Void)?
    var onEditGroup: ((GroupItemState) -> Void)?
    var onCreateGroup: (() -> Void)?
    var onJoinGroup: (() -> Void)?
    var onOpenSubscription: (() -> Void)?
    
    var isShowingSheet: Binding<Bool> {
        Binding(get: { bottomSheet != nil }, set: { _ in bottomSheet = nil })
    }
    
    func body(content: Content) -> some View {
        content
            .sheet(isPresented: isShowingSheet) {
                VStack {
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(MR.shared.strings.title_dashboard.localizedString())
                                    .font(.system(size: 24, weight: .bold))
                                Spacer().frame(width: 8)
                                PlanLimitCount(count: groupsCount, limit: nil)
                            }
                            
                            Spacer().frame(height: 8)
                            
                            ForEach(createdGroups.indices, id: \.self) { index in
                                GroupListItem(data: createdGroups[index], onClick: { onSelectGroup?(createdGroups[index]) }, onEdit: { onEditGroup?(createdGroups[index]) })
                            }
                            
                            ForEach(joinedGroups.indices, id: \.self) { index in
                                GroupListItem(data: joinedGroups[index], onClick: { onSelectGroup?(joinedGroups[index]) }, onEdit: { onEditGroup?(joinedGroups[index]) })
                            }
                            
                            if bottomSheet?.onlyShowGroups == false {
                                Spacer()
                                GroupPickerGroupCreateSection(
                                    groupsCount: createdGroups.count,
                                    groupsLimit: bottomSheet?.groupsLimit?.intValue,
                                    canCreateGroup: bottomSheet?.canCreateGroup ?? true,
                                    onCreateGroup: onCreateGroup,
                                    onJoinGroup: onJoinGroup,
                                    onOpenSubscription: onOpenSubscription
                                )
                            }
                        }
                        .padding()
                        .presentationDetents(groupsCount >= 2 ? [.large] : [.medium, .large])
                        .presentationBackground(Color.themebackground)
                    }
                }
            }
    }
}

extension View {
    func groupPickerSheet(
        _ data: Binding<BottomSheetPickerTypeGroupPickerBottomSheet?>,
        onSelectGroup: @escaping (GroupItemState) -> Void,
        onEditGroup: @escaping (GroupItemState) -> Void,
        onCreateGroup: @escaping () -> Void,
        onJoinGroup: @escaping () -> Void,
        onOpenSubscription: @escaping () -> Void
    ) -> some View {
        self.modifier(GroupPickerSheet(bottomSheet: data, onSelectGroup: onSelectGroup, onEditGroup: onEditGroup, onCreateGroup: onCreateGroup, onJoinGroup: onJoinGroup, onOpenSubscription: onOpenSubscription))
    }
}
