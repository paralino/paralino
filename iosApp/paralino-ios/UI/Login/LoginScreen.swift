//
//  LoginScreen.swift
//  paralino-ios
//
//  Created by Zelimir on 14.04.23.
//

import SwiftUI

struct LoginScreen: View {
    
    @StateObject var screenState = LoginScreenObject()
    @State var alertDetails: AlertDialogData?
    
    private var isLoginMode: Bool { screenState.uiState.data?.uiType == .login }
    private var email: Binding<String> {
        Binding (get: { screenState.uiState.data?.email ?? "" }, set: { screenState.vm.onEmailChange(email: $0) })
    }
    private var password : Binding<String> {
        Binding (get: { screenState.uiState.data?.password ?? "" }, set: { screenState.vm.onPasswordChange(password: $0) })
    }
    private var confirmPassword : Binding<String> {
        Binding (get: { screenState.uiState.data?.passwordConfirm ?? "" }, set: { screenState.vm.onPasswordConfirmChange(password: $0) })
    }
    
    @State private var showConfirmPassword = false
    @State private var showPasswordContent = false
    @State private var showConfirmPasswordContent = false
    
    var body: some View {
        VStack {
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
            HStack {
                Button(action: {
                    withAnimation(.interactiveSpring()) {
                        screenState.vm.setScreenUiType(type: .login)
                    }
                }) {
                    Text("Login")
                        .font(.title2)
                        .foregroundColor(isLoginMode ? .white : .themePrimary)
                        .frame(maxWidth: .infinity, minHeight: 50)
                        .background(isLoginMode ? Color.themePrimary : Color.clear)
                        .cornerRadius(10)
                }
                Button(action: {
                    withAnimation(.interactiveSpring()) {
                        screenState.vm.setScreenUiType(type: .signup)
                    }
                }) {
                    Text("Sign Up")
                        .font(.title2)
                        .foregroundColor(!isLoginMode ? .white : .themePrimary)
                        .frame(maxWidth: .infinity, minHeight: 50)
                        .background(!isLoginMode ? Color.themePrimary : Color.clear)
                        .cornerRadius(10)
                }
            }
            Spacer()
            VStack(spacing: 10) {
                TextField("Email", text: email)
                    .font(.title2)
                    .padding()
                    .background(Color.gray.opacity(0.2))
                    .cornerRadius(10)
                    .overlay(
                        RoundedRectangle(cornerRadius: 10)
                            .stroke(.red, lineWidth: screenState.uiState.data?.hasEmailError == true ? 2 : 0)
                    )
                ZStack {
                    SecureField("Password", text: password)
                        .font(.title2)
                        .padding()
                        .background(Color.gray.opacity(0.2))
                    .cornerRadius(10)
                    Button(action: {
                        showPasswordContent.toggle()
                    }) {
                        Image(systemName: showPasswordContent ? "eye.fill" : "eye.slash.fill")
                            .foregroundColor(.gray)
                    }
                    .frame(maxWidth: .infinity, alignment: .trailing)
                    .padding(.trailing, 10)
                }
                if screenState.uiState.data?.uiType == .signup {
                    SecureField("Confirm Password", text: confirmPassword)
                        .font(.title2)
                        .padding()
                        .background(Color.gray.opacity(0.2))
                        .cornerRadius(10)
                }
                Button(action: {
                    // Handle button action
                    alertDetails = AlertDialogData.guestAccountWarning()
                }) {
                    Text(isLoginMode ? "Login" : "Sign Up")
                        .font(.title2)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 20)
                }
                .frame(maxWidth: .infinity)
                .background(Color.themePrimary)
                .cornerRadius(10)
                .padding(.top, 40)
            }
            Spacer()
        }
        .padding()
        .navigationBarBackButtonHidden(true)
        .mainAlert($alertDetails) { key in
            if key == AlertDialogButton.keyPositive {
                screenState.vm.proceed()
            } else {
                alertDetails = nil
            }
        }
    }
}

struct LoginScreen_Previews: PreviewProvider {
    static var previews: some View {
        LoginScreen()
    }
}
