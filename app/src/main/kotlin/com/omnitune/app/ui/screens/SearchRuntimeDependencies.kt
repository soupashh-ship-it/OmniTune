package com.omnitune.app.ui.screens

import android.content.Context
import com.omnitune.app.utils.isInternetAvailable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Android state seams used by production search and deterministic integration tests. */
interface SearchNetworkStatus {
    fun isOnline(): Boolean
}

@Singleton
class AndroidSearchNetworkStatus @Inject constructor(
    @ApplicationContext private val context: Context,
) : SearchNetworkStatus {
    override fun isOnline(): Boolean = isInternetAvailable(context)
}

interface SearchTiming {
    val debounceMillis: Long
}

@Singleton
class ProductionSearchTiming @Inject constructor() : SearchTiming {
    override val debounceMillis: Long = 400L
}
