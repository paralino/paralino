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

class ConnectionStatusProvider: IConnectionStatusProvider {
    
    private var monitor: NWPathMonitor? = nil
    
    private var connectionActive = false
    private var mobileNetwork = false
    
    override init(backendConfig: BackendConfig) {
        super.init(backendConfig: backendConfig)
        
        monitor = NWPathMonitor()
        guard monitor?.pathUpdateHandler == nil else { return }
        
        monitor?.pathUpdateHandler = { [weak self] update in
            self?.connectionActive = update.status == .satisfied ? true : false
            self?.mobileNetwork = update.isExpensive ? true : false
            DispatchQueue.main.async { [weak self] in
                Task {
                    try? await self?.doInitAndGetStatus(forceCheck: false)
                }
            }
        }
        
        monitor?.start(queue: DispatchQueue(label: "ConnectionMonitor"))
    }
    
    // TODO adjust
    override func isNetworkAvailable() async throws -> KotlinBoolean {
        return KotlinBoolean(bool: connectionActive)
    }
    
    override func isInternetAvailable(url: String) async throws -> KotlinBoolean {
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
    
    // TODO adjust
    override func getNetworkType() async throws -> NetworkType? {
        if connectionActive {
            if mobileNetwork {
                guard let currentRadioAccessTechnology = CTTelephonyNetworkInfo().serviceCurrentRadioAccessTechnology else { return .genericCell }
                
                switch currentRadioAccessTechnology.first?.value {
                case CTRadioAccessTechnologyGPRS, CTRadioAccessTechnologyEdge, CTRadioAccessTechnologyCDMA1x: return .g2
                case CTRadioAccessTechnologyHSDPA,
                    CTRadioAccessTechnologyHSUPA,
                    CTRadioAccessTechnologyHSDPA,
                    CTRadioAccessTechnologyeHRPD,
                    CTRadioAccessTechnologyWCDMA,
                    CTRadioAccessTechnologyNRNSA,
                    CTRadioAccessTechnologyCDMAEVDORev0,
                    CTRadioAccessTechnologyCDMAEVDORevA, CTRadioAccessTechnologyCDMAEVDORevB: return .g3
                case CTRadioAccessTechnologyLTE: return .g4
                case CTRadioAccessTechnologyNR: return .g5
                default:
                    return .genericCell
                }
            } else {
                return .wifi
            }
        } else {
            return nil
        }
    }
    
    // TODO adjust
    override func getSignalStrength() async throws -> KotlinInt? {
        return nil
    }
    
    deinit {
        guard let monitor = monitor else { return }
        monitor.cancel()
    }
}
