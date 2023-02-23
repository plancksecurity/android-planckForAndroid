package com.fsck.k9.auth

import com.auth0.android.jwt.JWT

private const val EMAIL_CLAIM = "email"

class JwtTokenDecoder(
    private val jwtFactory: JwtFactory = JwtFactory()
) {
    fun getEmail(token: String): Result<String?> = kotlin.runCatching {
        jwtFactory.createJwt(token).claims[EMAIL_CLAIM]?.asString()
    }
}

class JwtFactory {
    fun createJwt(token: String): JWT = JWT(token)
}