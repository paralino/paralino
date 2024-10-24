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
    
    var onSelectGroup: ((GroupItemState) -> Void)?
    var onEditGroup: ((GroupItemState) -> Void)?
    var onCreateGroup: (() -> Void)?
    var onJoinGroup: (() -> Void)?
    
    var isShowingSheet: Binding<Bool> {
        Binding(get: { bottomSheet != nil }, set: { _ in bottomSheet = nil })
    }
    
    func body(content: Content) -> some View {
        content
            .sheet(isPresented: isShowingSheet) {
                let groups = bottomSheet?.groups ?? []
                VStack {
                    ScrollView {
                        LazyVStack(alignment: .center, spacing: 8) {
                            ForEach(groups.indices, id: \.self) { index in
                                GroupListItem(data: groups[index], onClick: { onSelectGroup?(groups[index]) }, onEdit: { onEditGroup?(groups[index]) })
                                    
                            }
                            Spacer()
                            HStack {
                                Button(action: { onCreateGroup?() }) {
                                    Label(MR.shared.strings.group_create_button_text.localizedString(), systemImage: "plus")
                                }
                                Spacer()
                                Button(action: { onJoinGroup?() }) {
                                    Label(MR.shared.strings.dashboard_groups_join_button.localizedString(), systemImage: "key.horizontal")
                                }
                            }
                            .padding()
                        }
                        .padding()
                        .presentationDetents([.medium, .large])
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
        onJoinGroup: @escaping () -> Void
    ) -> some View {
        self.modifier(GroupPickerSheet(bottomSheet: data, onSelectGroup: onSelectGroup, onEditGroup: onEditGroup, onCreateGroup: onCreateGroup, onJoinGroup: onJoinGroup))
    }
}
