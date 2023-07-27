package com.fsck.k9.auth

import com.auth0.android.jwt.JWT
import javax.inject.Inject

private const val EMAIL_CLAIM = "email"

class JwtTokenDecoder @Inject constructor(
    private val jwtFactory: JwtFactory
) {
    fun getEmail(token: String): Result<String?> = kotlin.runCatching {
        jwtFactory.createJwt(token).claims[EMAIL_CLAIM]?.asString()
    }
}

class JwtFactory @Inject constructor() {
    fun createJwt(token: String): JWT = JWT(token)
}