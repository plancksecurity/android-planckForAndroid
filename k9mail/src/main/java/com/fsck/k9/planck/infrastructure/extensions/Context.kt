package com.fsck.k9.planck.infrastructure.extensions

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.util.TypedValue

val Context.rootContext: Context
    get() {
        var context = this
        while (context !is Activity && context !is Application && context !is Service && context is ContextWrapper) {
            context = context.baseContext
        }
        return context
    }

fun Context.dpToPx(px: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, px, resources.displayMetrics)
        .toInt()