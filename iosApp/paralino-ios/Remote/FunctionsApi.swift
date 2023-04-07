//
//  FunctionsApi.swift
//  paralino-ios
//
//  Created by Zelimir on 05.04.23.
//

import Foundation
import shared
import Appwrite

class FunctionsApi: IFunctionsApi {
    
    let remoteModule: AppwriteRemoteModule
    
    init(remoteModule: AppwriteRemoteModule) {
        self.remoteModule = remoteModule
    }
    
    func createExecution(functionId: String, data: String?) async throws -> String {
        return await remoteModule.execute { module in
            try! await module.functions.createExecution(functionId: functionId, data: data, async: true).id
        }
    }
    
    func getExecution(functionId: String, executionId: String) async throws -> FunctionExecutionRaw {
        return await remoteModule.execute { module in
            let result = try! await module.functions.getExecution(functionId: functionId, executionId: executionId)
            return FunctionExecutionRaw(
                id: result.id,
                functionId: result.functionId,
                status: result.status,
                statusCode: Int64(result.statusCode),
                response: result.response,
                stderr: result.stderr
            )
        }
    }
    
    func runAndWait(functionId: String, data: String?) async throws -> FunctionExecutionRaw {
        return await remoteModule.execute { module in
            let result = try! await module.functions.createExecution(
                functionId: functionId,
                data: data,
                async: false
            )
            return FunctionExecutionRaw(
                id: result.id,
                functionId: result.functionId,
                status: result.status,
                statusCode: Int64(result.statusCode),
                response: result.response,
                stderr: result.stderr
            )
        }
    }
    
}
