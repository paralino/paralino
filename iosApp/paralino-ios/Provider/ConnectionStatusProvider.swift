//
//  ConnectionStatusProvider.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared

class ConnectionStatusProvider: IConnectionStatusProvider {
    
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
                            cont.resume(returning: false)
                            return
                        }
                        
                        guard (response as? HTTPURLResponse)?
                            .statusCode == 200 else {
                            cont.resume(returning: false)
                            return
                        }
                        
                        cont.resume(returning: true)
                    }
                    .resume()
            } else {
                cont.resume(returning: false)
            }
        }
    }
}
