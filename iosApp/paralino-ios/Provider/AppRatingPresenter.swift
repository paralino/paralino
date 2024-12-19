//
//  AppRatingPresenter.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-12-17.
//

import Foundation
import shared

class AppRatingPresenter : ObservableObject, IAppRatingPresenter {
    
    @Published var appRatingRequested: Bool? = nil

    func startAppRatingFlow() {
        appRatingRequested = true
    }
}
