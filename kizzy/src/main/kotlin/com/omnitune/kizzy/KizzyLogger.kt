package com.omnitune.kizzy

object KizzyLogger {
    private const val TAG = "Kizzy"

    fun log(message: String) {
        println("[$TAG] $message")
    }

    fun error(message: String, throwable: Throwable? = null) {
        println("[$TAG][ERROR] $message")
        throwable?.printStackTrace()
    }

    fun debug(message: String) {
        println("[$TAG][DEBUG] $message")
    }
}
