package com.omnitune.app.db

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicDatabaseRecoveryTest {
    @Test
    fun onlySchemaFailuresTriggerRepair() {
        assertTrue(
            isRecoverableDatabaseSchemaFailure(
                IllegalStateException("Migration didn't properly handle playlist"),
            ),
        )
        assertTrue(
            isRecoverableDatabaseSchemaFailure(
                RuntimeException("open failed", IllegalStateException("Room cannot verify the data integrity")),
            ),
        )
        assertFalse(isRecoverableDatabaseSchemaFailure(RuntimeException("database or disk is full")))
    }
}
