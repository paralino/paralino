//
//  ConnectionStatusProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared

class ConnectionStatusProvider: IConnectionStatusProvider {
    
    // TODO adjust
    override func isNetworkAvailable() async throws -> KotlinBoolean {
        return true
    }
    
    override func isInternetAvailable(url: String) async throws -> KotlinBoolean {
        return try await withCheckedThrowingContinuation { cont in
            if let url = URL(string: url) {
                var request = URLRequest(url: url)
                request.httpMethod = "HEAD"
                
                URLSession(configuration: .default)
                    .dataTask(with: request) { (_, response, error) -> Void in
                        guard error == nil else {
                            Logr.v(tag: "app_state", message: "Network check - error")
                            cont.resume(returning: false)
                            return
                        }
                        
                        guard (response as? HTTPURLResponse)?
                            .statusCode == 200 else {
                            Logr.v(tag: "app_state", message: "Network check - http error")
                            cont.resume(returning: false)
                            return
                        }
                        
                        cont.resume(returning: true)
                    }
                    .resume()
            } else {
                Logr.v(tag: "app_state", message: "Network check - wrong url")
                cont.resume(returning: false)
            }
        }
    }
    
    // TODO adjust
    override func getNetworkType() async throws -> NetworkType? {
        return NetworkType.wifi
    }
    
    // TODO adjust
    override func getSignalStrength() async throws -> KotlinInt? {
        return 4
    }
}
