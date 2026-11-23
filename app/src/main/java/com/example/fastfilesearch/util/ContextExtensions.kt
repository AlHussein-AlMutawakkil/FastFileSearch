package com.example.fastfilesearch.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

inline fun <reified T : Activity> Context.findActivity(): T? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is T) return ctx
        ctx = ctx.baseContext
    }
    return null
}
