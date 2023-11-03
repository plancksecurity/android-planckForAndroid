package com.fsck.k9.activity.setup

import net.openid.appauth.BuildConfig

sealed interface AuthFlowState {
    object Idle : AuthFlowState

    object Success : AuthFlowState

    object NotSupported : AuthFlowState

    object BrowserNotFound : AuthFlowState

    object UnsuitableBrowserFound : AuthFlowState

    object Canceled : AuthFlowState

    data class Failed(val errorCode: String?, val errorMessage: String?) : AuthFlowState {

        constructor(throwable: Throwable): this(
            errorCode = null,
            errorMessage = if (BuildConfig.DEBUG) throwable.stackTraceToString()
            else throwable.message
        )

        override fun toString(): String {
            return listOfNotNull(errorCode, errorMessage).joinToString(separator = " - ")
        }
    }

    data class WrongEmailAddress(
        val adminEmail: String,
        val userWrongEmail: String
        ): AuthFlowState {
            constructor(exception: WrongEmailAddressException) : this(
                exception.adminEmail,
                exception.userWrongEmail
            )
        }
}