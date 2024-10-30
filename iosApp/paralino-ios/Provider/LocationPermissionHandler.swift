//
//  LocationPermissionHandler.swift
//  paralino-ios
//
//  Created by Zelimir on 2024-10-25.
//

import shared
import UIKit

class LocationPermissionHandler {
    
    private var requestedInUse: Bool = false
    private var requestedFinePermission: Bool = false
    private var requestedBackground: Bool = false
    
    let locationManager: LocationManager
    
    init(locationManager: LocationManager) {
        self.locationManager = locationManager
    }
    
    func requestLocationServices(
        messageDispather: ICoreMessageDispatcher
    ) {
        let data = AppSnackbarData(
            text: MR.shared.strings.error_location_service_missing_sheet_message.localizedString(),
            actionText: nil,
            duration: SnackbarDuration.short_,
            id: "location_services"
        )
        DispatchQueue.main.async {
            messageDispather.showSnackbarForResult(data: data) { result, error in }
        }
    }
    
    func requestLocationPermission(
        messageDispather: ICoreMessageDispatcher
    ) {
        let status = locationManager.manager.locationCondtionStatus()
        
        guard !status.isFinePermissionAllowed || !status.isBackgroundPermissionAllowed else {
            return
        }
        
        if locationManager.manager.authorizationStatus == .denied {
            showSnackbar(
                messageDispather: messageDispather,
                message: MR.shared.strings.error_location_permission_missing_sheet_message.localizedString()
            )
        } else if !requestedInUse {
            requestedInUse = true
            requestLocationPermission(status)
        } else if !requestedBackground {
            if !status.isFinePermissionAllowed {
                if requestedFinePermission {
                    showSnackbar(
                        messageDispather: messageDispather,
                        message: MR.shared.strings.error_location_precise_permission_missing_sheet_message.localizedString()
                    )
                } else {
                    requestedFinePermission = true
                    requestLocationPermission(status)
                }
            } else if !status.isCoarsePermissionAllowed {
                showSnackbar(
                    messageDispather: messageDispather,
                    message: MR.shared.strings.error_location_permission_missing_sheet_message.localizedString()
                )
            } else {
                requestedBackground = true
                requestLocationPermission(status)
            }
        }  else {
            if !status.isFinePermissionAllowed {
                if requestedFinePermission {
                    showSnackbar(
                        messageDispather: messageDispather,
                        message: MR.shared.strings.error_location_precise_permission_missing_sheet_message.localizedString()
                    )
                } else {
                    requestedFinePermission = true
                    requestLocationPermission(status)
                }
            } else if !status.isBackgroundPermissionAllowed {
                showSnackbar(
                    messageDispather: messageDispather,
                    message: MR.shared.strings.error_location_background_permission_missing_sheet_message.localizedString()
                )
            } else {
                // have all permissions
            }
        }
    }
    
    private func requestLocationPermission(_ status: LocationConditionStatus) {
        if !status.isFinePermissionAllowed {
            requestedInUse = true
            
            if status.isCoarsePermissionAllowed {
                requestedFinePermission = true
            }
            
            locationManager.manager.requestWhenInUseAuthorization()
        } else if !status.isBackgroundPermissionAllowed {
            requestedBackground = true
            locationManager.manager.requestAlwaysAuthorization()
        }
    }
    
    private func showSnackbar(messageDispather: ICoreMessageDispatcher, message: String) {
        let data = AppSnackbarData(
            text: message,
            actionText: MR.shared.strings.button_open_settings.localizedString(),
            duration: SnackbarDuration.long_,
            id: "location_permission"
        )
        DispatchQueue.main.async {
            messageDispather.showSnackbarForResult(data: data) { result, error in
                if result?.result == MaterialSnackbarResult.actionperformed {
                    self.openAppSettingsPage()
                }
            }
        }
    }
 
    private func openAppSettingsPage() {
        DispatchQueue.main.async {
            if let appSettings = URL(string: UIApplication.openSettingsURLString), UIApplication.shared.canOpenURL(appSettings) {
                UIApplication.shared.open(appSettings)
            }
        }
    }
}
