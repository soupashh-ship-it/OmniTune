package com.omnitune.app.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginSessionVerificationPolicyTest {
    @Test
    fun `complete account context is accepted`() {
        assertNull(
            LoginSessionVerificationPolicy.failureMessage(
                cookieHeader = "SID=one; SAPISID=two",
                visitorData = "visitor",
                dataSyncId = "sync",
                accountName = "OmniTune listener",
            ),
        )
    }

    @Test
    fun `missing account context is rejected before persistence`() {
        assertEquals(
            "YouTube Music did not provide an account sync session.",
            LoginSessionVerificationPolicy.failureMessage(
                cookieHeader = "SID=one; SAPISID=two",
                visitorData = "visitor",
                dataSyncId = null,
                accountName = null,
            ),
        )
    }
}
