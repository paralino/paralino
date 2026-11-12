//
//  ConnectionStatusProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import Network
import CoreTelephony
import NetworkExtension

class ConnectionStatusProvider: IConnectionStatusProvider, @unchecked Sendable {
    
    private var monitor: NWPathMonitor? = nil
    
    private var connectionActive = false
    private var mobileNetwork = false
    private var wifiNetwork = false
    
    override init(appBuildConfig: AppBuildConfig) {
        super.init(appBuildConfig: appBuildConfig)
        
        monitor = NWPathMonitor()
        guard monitor?.pathUpdateHandler == nil else { return }
        
        monitor?.pathUpdateHandler = { [weak self] update in
            self?.connectionActive = update.status == .satisfied ? true : false
            self?.mobileNetwork = update.isExpensive ? true : false
            self?.wifiNetwork = update.usesInterfaceType(.wifi)
            Task { [weak self] in
                _ = try? await self?.doInitAndGetStatus(forceCheck: false)
            }
        }
        
        monitor?.start(queue: DispatchQueue(label: "ConnectionMonitor"))
    }
    
    override func isNetworkAvailable() async throws -> KotlinBoolean {
        if super.appBuildConfig.appHost == AppHost.iosLocationExtension {
            // assume true for LSE
            return true
        }
        
        return KotlinBoolean(bool: connectionActive)
    }
    
    override func isInternetAvailable(url: String) async throws -> KotlinBoolean {
        if super.appBuildConfig.appHost == AppHost.iosLocationExtension {
            // assume true for LSE
            return true
        }
        
        Logr.v(tag: "app_state", message: "Server ping - init")
        return try await withCheckedThrowingContinuation { cont in
            if let url = URL(string: url) {
                Logr.v(tag: "app_state", message: "Server ping - start")
                var request = URLRequest(url: url)
                request.httpMethod = "HEAD"
                
                URLSession(configuration: .default)
                    .dataTask(with: request) { (_, response, error) -> Void in
                        guard error == nil else {
                            Logr.v(tag: "app_state", message: "Server ping - error")
                            cont.resume(returning: false)
                            return
                        }
                        
                        guard (response as? HTTPURLResponse)?.statusCode == 200 else {
                            Logr.v(tag: "app_state", message: "Server ping - http error")
                            cont.resume(returning: false)
                            return
                        }
                        
                        Logr.v(tag: "app_state", message: "Server ping - success")
                        cont.resume(returning: true)
                    }
                    .resume()
            } else {
                Logr.v(tag: "app_state", message: "Server ping - wrong url")
                cont.resume(returning: false)
            }
        }
    }
    
    override func getNetworkType() async throws -> NetworkType? {
        guard connectionActive else {
            return nil
        }
        
        if mobileNetwork {
            let networkInfo = CTTelephonyNetworkInfo()
            guard let dataServiceIdentifier = networkInfo.dataServiceIdentifier,
                  let currentRadioTechnology = networkInfo.serviceCurrentRadioAccessTechnology?[dataServiceIdentifier] else {
                return .genericCell
            }
            
            switch currentRadioTechnology {
            case CTRadioAccessTechnologyGPRS,
                 CTRadioAccessTechnologyEdge,
                 CTRadioAccessTechnologyCDMA1x:
                return .g2
                
            case CTRadioAccessTechnologyWCDMA,
                 CTRadioAccessTechnologyHSDPA,
                 CTRadioAccessTechnologyHSUPA,
                 CTRadioAccessTechnologyCDMAEVDORev0,
                 CTRadioAccessTechnologyCDMAEVDORevA,
                 CTRadioAccessTechnologyCDMAEVDORevB:
                return .g3
                
            case CTRadioAccessTechnologyLTE:
                return .g4
                
            case CTRadioAccessTechnologyNRNSA,
                 CTRadioAccessTechnologyNR:
                return .g5
                
            default:
                return .genericCell
            }
        } else {
            return .wifi
        }
    }
    // can't have this on iOS
    override func getSignalStrength() async throws -> KotlinInt? {
        return nil
    }

    override func getConnectedWifi() async throws -> ConnectedWifi? {
        guard super.appBuildConfig.appHost != AppHost.iosLocationExtension, wifiNetwork else {
            return nil
        }

        guard let network = await NEHotspotNetwork.fetchCurrent() else {
            return nil
        }
       
        guard !network.bssid.isEmpty else {
            return nil
        }
        
        // expensive wifi is usually a hotspot
        return ConnectedWifi(
            bssid: network.bssid,
            ssid: network.ssid.isEmpty ? nil : network.ssid,
            isMetered: mobileNetwork
        )
    }

    deinit {
        guard let monitor = monitor else { return }
        monitor.cancel()
    }
}
