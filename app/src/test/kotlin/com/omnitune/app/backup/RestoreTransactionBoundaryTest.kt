/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RestoreTransactionBoundaryTest {
    @Test
    fun databaseTransactionFailureStopsRestoreBeforeMediaPromotion() {
        val error = assertThrows(RestoreFailureException::class.java) {
            runBlocking {
                RestoreTransactionBoundary.run<Int> {
                    throw IllegalStateException("forced database transaction failure")
                }
            }
        }

        assertEquals(RestoreFailurePhase.DATABASE_TRANSACTION, error.phase)
        assertEquals("forced database transaction failure", error.cause?.message)
    }

    @Test
    fun successfulTransactionReturnsItsVerifiedResult() = runTest {
        assertEquals("verified", RestoreTransactionBoundary.run { "verified" })
    }
}
