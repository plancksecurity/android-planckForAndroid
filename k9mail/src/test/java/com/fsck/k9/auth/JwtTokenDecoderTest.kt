package com.fsck.k9.auth

import com.auth0.android.jwt.Claim
import com.auth0.android.jwt.JWT
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test


class JwtTokenDecoderTest {
    private val jwtFactory: JwtFactory = mockk()
    private val jwt: JWT = mockk()
    private val jwtTokenDecoder = JwtTokenDecoder(jwtFactory)


    @Before
    fun setUp() {
        every { jwtFactory.createJwt(any()) }.returns(jwt)
    }

    @Test
    fun `decode() uses JWT to decode token`() {
        val emailClaim: Claim = mockk()
        every { emailClaim.asString() }.returns(EMAIL_ADDRESS)
        every { jwt.claims }.returns(mapOf(EMAIL_CLAIM to emailClaim))


        val result = jwtTokenDecoder.getEmail(TOKEN)


        verify { jwtFactory.createJwt(TOKEN) }
        verify { jwt.claims }
        assert(result.isSuccess)
        assertEquals(EMAIL_ADDRESS, result.getOrThrow())
    }

    @Test
    fun `decode() returns Result_success with null if claim not found`() {
        val emailClaim: Claim = mockk()
        every { emailClaim.asString() }.returns(EMAIL_ADDRESS)
        every { jwt.claims }.returns(mapOf("otherClaim" to emailClaim))


        val result = jwtTokenDecoder.getEmail(TOKEN)


        verify { jwtFactory.createJwt(TOKEN) }
        verify { jwt.claims }
        assert(result.isSuccess)
        assertEquals(null, result.getOrThrow())
    }

    @Test
    fun `decode() returns failure if an exception is thrown`() {
        every { jwtFactory.createJwt(any()) }.throws(RuntimeException("test"))


        val result = jwtTokenDecoder.getEmail(TOKEN)


        verify { jwtFactory.createJwt(TOKEN) }
        assert(result.isFailure)
        TestCase.assertTrue(result.exceptionOrNull() is RuntimeException)
        assertEquals("test", result.exceptionOrNull()!!.message)
    }

    companion object {
        private const val TOKEN = "token"
        private const val EMAIL_CLAIM = "email"
        private const val EMAIL_ADDRESS = "email@sample.com"
    }

}