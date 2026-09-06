package ng.wimika.samplebankapp

import ng.wimika.samplebankapp.ui.screens.launchMoneyGuardForFix
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchMoneyGuardForFixTest {
    @Test
    fun `does not use fallback when MoneyGuard launches`() {
        var fallbackUsed = false

        launchMoneyGuardForFix(
            launchMoneyGuard = { true },
            onMoneyGuardUnavailable = { fallbackUsed = true }
        )

        assertFalse(fallbackUsed)
    }

    @Test
    fun `uses fallback when MoneyGuard cannot launch`() {
        var fallbackUsed = false

        launchMoneyGuardForFix(
            launchMoneyGuard = { false },
            onMoneyGuardUnavailable = { fallbackUsed = true }
        )

        assertTrue(fallbackUsed)
    }
}
