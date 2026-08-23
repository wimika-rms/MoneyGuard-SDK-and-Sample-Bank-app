package ng.wimika.samplebankapp

import ng.wimika.samplebankapp.ui.state.moneyGuardSecurityWarning
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionWarningTest {
    @Test
    fun `host attestation warning identifies device verification`() {
        val message = moneyGuardSecurityWarning("host_attestation_failed")

        assertTrue(message.contains("on this device"))
        assertFalse(message.contains("bank integration"))
    }

    @Test
    fun `caller rejection warning identifies bank integration`() {
        val message = moneyGuardSecurityWarning("caller_not_authorised")

        assertTrue(message.contains("bank integration"))
    }
}
