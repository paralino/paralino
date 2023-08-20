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
    func subscribeWithFilter(channels: [String], filters: [String], onResult: @escaping (RealtimeResponse<NSDictionary>) -> Void) -> () -> Void {
        
        let subscription = remoteModule.realtime.subscribe(channels: Set(channels.map { $0 })) { response in
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

        return {
            subscription.close()
        }
    }
    
    
    let remoteModule: AppwriteRemoteModule
    
    init(remoteModule: AppwriteRemoteModule) {
        self.remoteModule = remoteModule
    }
    
    
}
