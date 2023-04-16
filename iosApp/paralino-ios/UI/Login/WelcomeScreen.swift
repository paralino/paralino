//
//  WellcomeScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 14.04.23.
//

import Foundation
import SwiftUI


struct WelcomeScreen: View {
    
    var body: some View {
        ZStack {
            Image("paralino_start_background")
                .centerCropped()
                .edgesIgnoringSafeArea(.all)
            
            VStack(spacing: 10) {
                Text("Paralino")
                    .font(.largeTitle)
                    .foregroundColor(.lightBackground)
                
                Text("SECURE - PRIVATE - ENCRYPTED")
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding(.top, 10)
                Spacer()
                ZStack {
                    Circle()
                        .fill(Color.themePrimary)
                        .frame(width: 120, height: 120)
                        .overlay(Circle().stroke(Color.darkBackground, lineWidth: 8))
                    Image("paralino_logo")
                        .resizable()
                        .scaledToFit()
                        .colorMultiply(.white)
                        .frame(width: 80, height: 80)
                }
                Spacer()
//                NavigationLink(destination: LoginScreen()) {
                    Label("Join", systemImage: "ipad.and.arrow.forward")
                        .font(.subheadline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 64)
                        .background(Color.themePrimary)
                        .cornerRadius(15)
//                }
            }
            .padding()
        }
    }
}

struct WelcomeScreen_Previews: PreviewProvider {
    static var previews: some View {
        WelcomeScreen()
    }
}
