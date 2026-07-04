/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 *
 * Based on Kizzy (c) yzziK(Vaibhav) 2022
 */

package com.omnitune.kizzy

interface KizzyLogger {
    fun info(message: String)
    fun fine(message: String)
    fun warning(message: String)
    fun severe(message: String, throwable: Throwable? = null)
}
