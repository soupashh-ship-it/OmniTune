package com.omnitune.app.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.omnitune.app.constants.DataSyncIdKey
import com.omnitune.app.constants.InnerTubeCookieKey
import com.omnitune.app.constants.PoTokenGvsKey
import com.omnitune.app.constants.PoTokenKey
import com.omnitune.app.constants.PoTokenPlayerKey
import com.omnitune.app.constants.VisitorDataKey
import kotlinx.coroutines.runBlocking

fun forgetAccount(context: Context) {
    runBlocking {
        context.dataStore.edit { settings ->
            settings.remove(InnerTubeCookieKey)
            settings.remove(VisitorDataKey)
            settings.remove(PoTokenKey)
            settings.remove(PoTokenGvsKey)
            settings.remove(PoTokenPlayerKey)
            settings.remove(DataSyncIdKey)
        }
    }
}
