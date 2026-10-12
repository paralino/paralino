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
    let locationStatusProvider: ILocationStatusProvider

    init(locationManager: LocationManager, locationStatusProvider: ILocationStatusProvider) {
        self.locationManager = locationManager
        self.locationStatusProvider = locationStatusProvider
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
            messageDispather.showSnackbarForResult(data: data) { result, error in
                self.requestFinished(messageDispather: messageDispather, type: .locationService)
            }
        }
    }

    func requestLocationPermission(
        messageDispather: ICoreMessageDispatcher
    ) {
        switch requestLocationPermissionOrShowInfo(messageDispather: messageDispather) {
        case .prompted:
            // authorization request has no completion, request is finished once app becomes active again
            messageDispather.onMessageEvent(event: ConditionRequestPromptedEvent(type: .locationPermission))
        case .infoShown:
            // request is finished once info is dismissed or user returns from settings
            break
        case .nothing:
            requestFinished(messageDispather: messageDispather, type: .locationPermission)
        }
    }

    private enum LocationRequestOutcome {
        case prompted, infoShown, nothing
    }

    private func requestLocationPermissionOrShowInfo(
        messageDispather: ICoreMessageDispatcher
    ) -> LocationRequestOutcome {
        let status = locationStatusProvider.createCurrentStatus()

        guard !status.isFinePermissionAllowed || !status.isBackgroundPermissionAllowed else {
            return .nothing
        }

        if locationManager.manager.authorizationStatus == .denied {
            showSnackbar(
                messageDispather: messageDispather,
                message: MR.shared.strings.error_location_permission_missing_sheet_message.localizedString()
            )
            return .infoShown
        } else if !requestedInUse {
            requestedInUse = true
            requestLocationPermission(status)
            return .prompted
        } else if !requestedBackground {
            if !status.isFinePermissionAllowed {
                if requestedFinePermission {
                    showSnackbar(
                        messageDispather: messageDispather,
                        message: MR.shared.strings.error_location_precise_permission_missing_sheet_message.localizedString()
                    )
                    return .infoShown
                } else {
                    requestedFinePermission = true
                    requestLocationPermission(status)
                    return .prompted
                }
            } else if !status.isCoarsePermissionAllowed {
                showSnackbar(
                    messageDispather: messageDispather,
                    message: MR.shared.strings.error_location_permission_missing_sheet_message.localizedString()
                )
                return .infoShown
            } else {
                requestedBackground = true
                requestLocationPermission(status)
                return .prompted
            }
        }  else {
            if !status.isFinePermissionAllowed {
                if requestedFinePermission {
                    showSnackbar(
                        messageDispather: messageDispather,
                        message: MR.shared.strings.error_location_precise_permission_missing_sheet_message.localizedString()
                    )
                    return .infoShown
                } else {
                    requestedFinePermission = true
                    requestLocationPermission(status)
                    return .prompted
                }
            } else if !status.isBackgroundPermissionAllowed {
                showSnackbar(
                    messageDispather: messageDispather,
                    message: MR.shared.strings.error_location_background_permission_missing_sheet_message.localizedString()
                )
                return .infoShown
            } else {
                // have all permissions
                return .nothing
            }
        }
    }

    func requestBackgroundRefreshPermission(
        messageDispather: ICoreMessageDispatcher
    ) {
        let status = locationStatusProvider.createCurrentStatus()

        if !status.isUnrestrictedBackgroundPermissionAllowed {
            showSnackbar(
                messageDispather: messageDispather,
                message: MR.shared.strings.error_unrestricted_background_access_message_ios.localizedString(),
                requestType: .backgroundAccess
            )
        } else {
            requestFinished(messageDispather: messageDispather, type: .backgroundAccess)
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

    private func requestFinished(messageDispather: ICoreMessageDispatcher, type: ConditionRequestType) {
        messageDispather.onMessageEvent(event: ConditionRequestFinishedEvent(type: type))
    }

    private func showSnackbar(
        messageDispather: ICoreMessageDispatcher,
        message: String,
        requestType: ConditionRequestType = .locationPermission
    ) {
        let data = AppSnackbarData(
            text: message,
            actionText: MR.shared.strings.button_open_settings.localizedString(),
            duration: SnackbarDuration.long_,
            id: "location_permission"
        )
        DispatchQueue.main.async {
            messageDispather.showSnackbarForResult(data: data) { result, error in
                if result?.result == AppSnackbarResultType.actionperformed {
                    self.openAppSettingsPage()
                } else {
                    self.requestFinished(messageDispather: messageDispather, type: requestType)
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
