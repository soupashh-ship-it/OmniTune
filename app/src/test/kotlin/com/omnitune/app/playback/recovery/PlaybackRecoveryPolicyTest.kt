package com.omnitune.app.playback.recovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRecoveryPolicyTest {

    @Test
    fun testCanRetry_initialState_returnsTrue() {
        val policy = PlaybackRecoveryPolicy(maxRetries = 2)
        assertTrue(policy.canRetry("media1", PlaybackErrorType.Unknown))
    }

    @Test
    fun testIncrementRetry_exceedsMaxRetries_returnsFalse() {
        val policy = PlaybackRecoveryPolicy(maxRetries = 2)
        
        policy.incrementRetry("media1")
        assertTrue(policy.canRetry("media1", PlaybackErrorType.Unknown))
        
        policy.incrementRetry("media1")
        assertFalse(policy.canRetry("media1", PlaybackErrorType.Unknown))
    }

    @Test
    fun testResetRetry_clearsRetriesForMedia_returnsTrue() {
        val policy = PlaybackRecoveryPolicy(maxRetries = 2)
        
        policy.incrementRetry("media1")
        policy.incrementRetry("media1")
        assertFalse(policy.canRetry("media1", PlaybackErrorType.Unknown))
        
        policy.resetRetry("media1")
        assertTrue(policy.canRetry("media1", PlaybackErrorType.Unknown))
    }

    @Test
    fun testClear_clearsAllRetries() {
        val policy = PlaybackRecoveryPolicy(maxRetries = 2)
        
        policy.incrementRetry("media1")
        policy.incrementRetry("media1")
        policy.incrementRetry("media2")
        policy.incrementRetry("media2")
        
        assertFalse(policy.canRetry("media1", PlaybackErrorType.Unknown))
        assertFalse(policy.canRetry("media2", PlaybackErrorType.Unknown))
        
        policy.clear()
        
        assertTrue(policy.canRetry("media1", PlaybackErrorType.Unknown))
        assertTrue(policy.canRetry("media2", PlaybackErrorType.Unknown))
    }
}
