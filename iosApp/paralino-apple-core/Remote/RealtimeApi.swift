//
//  RealtimeApi.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Appwrite
import Foundation
import shared

class RealtimeApi: IRealtimeApiIOS {

    let remoteModule: AppwriteRemoteModule

    init(remoteModule: AppwriteRemoteModule) {
        self.remoteModule = remoteModule
    }

    func subscribeToChannels(
        channels: [String],
        onResult: @escaping (RealtimeResponse<NSDictionary>) -> Void
    ) async throws -> Payload<RealtimeSubscriptionCallbackIOS> {
        return await remoteModule.execute { module in
            let subscription = try await module.realtime.subscribe(
                channels: Set(channels.map { $0 }),
                payloadType: String.self
            ) { response in
                onResult(
                    RealtimeResponse(
                        events: response.events ?? [],
                        channels: response.channels ?? [],
                        timestamp: response.timestamp ?? "",
                        payload: NSDictionary(
                            dictionary: response.payload ?? [:]
                        )
                    )
                )
            }
            return RealtimeSubscriptionCallbackIOS {
                self.closeSubscription(subscription: subscription)
            }
        }
    }

    private func closeSubscription(subscription: RealtimeSubscription) {
        DispatchQueue.main.async {
            Task {
                do {
                    try await subscription.close()
                } catch let error {
                    Logr.w(
                        tag: "realtime_events",
                        message: "Error closing channel: \(error.localizedDescription)"
                    )
                }
            }
        }
    }
}
