//
//  FunctionsApi.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import Appwrite
import AppwriteEnums

class FunctionsApi: IFunctionsApiIOS {
    
    let remoteModule: AppwriteRemoteModule
    
    init(remoteModule: AppwriteRemoteModule, deviceApiProvider: IDeviceApiProvider) {
        self.remoteModule = remoteModule
        super.init(deviceApiProvider: deviceApiProvider)
    }
    
    override func runAsync(function: ServerFunction, data: String?) async throws -> Payload<NSString> {
        return await remoteModule.execute { module in
            let result = try await module.functions.createExecution(
                functionId: function.functionId,
                body: data,
                async: true,
                path: function.path,
                method: AppwriteEnums.ExecutionMethod(rawValue: function.method),
                headers: [clientHeader.first : clientHeader.second]
            ).id
            return NSString(string: result)
        }
    }
    
    override func runAndWait(function: ServerFunction, data: String?) async throws -> Payload<FunctionExecutionRaw> {
        return await remoteModule.execute { module in
            let result = try await module.functions.createExecution(
                functionId: function.functionId,
                body: data,
                async: false,
                path: function.path,
                method: AppwriteEnums.ExecutionMethod(rawValue: function.method),
                headers: [clientHeader.first : clientHeader.second]
            )
            return FunctionExecutionRaw(
                id: result.id,
                functionId: result.functionId,
                status: result.status,
                statusCode: Int32(result.responseStatusCode),
                response: result.responseBody,
                errors: result.errors
            )
        }
    }
}
