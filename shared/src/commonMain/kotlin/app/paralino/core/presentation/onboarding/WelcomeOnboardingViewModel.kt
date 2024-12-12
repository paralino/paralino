package app.paralino.core.presentation.onboarding

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.presentation.CoreViewModel

class WelcomeOnboardingViewModel(
    private val cryptoCore: CryptoCore
): CoreViewModel() {
    fun provideRandomString(): String {
        return cryptoCore.randomId(length = 50, AlphabetType.EXTENDED)
    }
}