/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.backup

/**
 * Makes the failure boundary around the Room restore transaction explicit and
 * testable. The caller supplies a single Room withTransaction block, so an
 * exception can never continue into media promotion.
 */
internal object RestoreTransactionBoundary {
    suspend fun <T> run(block: suspend () -> T): T = try {
        block()
    } catch (error: Exception) {
        throw RestoreFailureException(
            RestoreFailurePhase.DATABASE_TRANSACTION,
            error.message ?: "Library changes were rolled back",
            error,
        )
    }
}
