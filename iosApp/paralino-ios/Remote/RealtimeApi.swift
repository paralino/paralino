//
//  RealtimeApi.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import Appwrite

class RealtimeApi: IRealtimeApiIOS {
    
    let remoteModule: AppwriteRemoteModule
    
    init(remoteModule: AppwriteRemoteModule) {
        self.remoteModule = remoteModule
    }
    
    func subscribeWithFilter(channels: [String], filters: [String], onResult: @escaping (RealtimeResponse<NSDictionary>) -> Void) async throws -> () -> KotlinUnit {
        let subscription = await remoteModule.execute { module in
            module.realtime.subscribe(channels: Set(channels.map { $0 })) { response in
                if filters.isEmpty || (response.events?.contains(where: { event in filters.contains(where: { filter in filter == event}) }) ?? false) {
                    onResult(
                        RealtimeResponse(
                            events: response.events ?? [],
                            channels: response.channels ?? [],
                            timestamp: response.timestamp ?? "",
                            payload: NSDictionary(dictionary: response.payload ?? [:])
                        )
                    )
                }
            }
        }
        
        return {
            PayloadSwift(subscription).dataOrNil?.close()
            return KotlinUnit()
        }
    }
}
