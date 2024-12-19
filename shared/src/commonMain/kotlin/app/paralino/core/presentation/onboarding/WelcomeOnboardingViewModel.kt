package app.paralino.core.presentation.onboarding

import app.paralino.core.crypto.AlphabetType
import app.paralino.core.crypto.CryptoCore
import app.paralino.core.db.AppPrefs
import app.paralino.core.presentation.CoreViewModel

class WelcomeOnboardingViewModel(
    private val cryptoCore: CryptoCore,
    private val appPrefs: AppPrefs
): CoreViewModel() {
    fun provideRandomString(): String {
        return cryptoCore.randomId(length = 50, AlphabetType.EXTENDED)
    }

    fun completeOnboarding() {
        appPrefs.didHaveWelcomeOnboarding = true
    }
}