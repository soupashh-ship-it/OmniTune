/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.di

import javax.inject.Qualifier

/**
 * Dispatcher qualifiers for coroutine injection.
 *
 * New code must inject dispatchers through these qualifiers instead of hardcoding
 * Dispatchers.IO / Dispatchers.Default / Dispatchers.Main so that coroutine timing and
 * threading becomes testable (see docs/architecture/music-service-decomposition-plan.md
 * and docs/architecture/god-object-prevention.md).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainImmediateDispatcher
