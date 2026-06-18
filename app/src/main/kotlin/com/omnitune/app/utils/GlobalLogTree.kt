package com.omnitune.app.utils

import timber.log.Timber

class GlobalLogTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        GlobalLog.append(priority, tag ?: "OmniTune", message)
    }
}
