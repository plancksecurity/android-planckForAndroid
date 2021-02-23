package com.fsck.k9.pEp.testutils

import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.testutils.AssertUtils.identityThat
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.eq
import foundation.pEp.jniadapter.Identity
import java.util.*

class PEpProviderStubber(private val providerMock: PEpProvider) {

    fun stubImportKey(importedIdentitiesList: List<Identity>?) {
        val importedIdentities: Vector<Identity>? =
            importedIdentitiesList?.let {
                Vector<Identity>().apply { addAll(importedIdentitiesList) }
            }
        doReturn(importedIdentities).`when`(providerMock).importKey(any())
    }

    fun stubProviderMethodsForIdentity(
        identity: Identity,
        importedFingerPrint: String,
        returnOnCanEncrypt: Boolean = true,
        setOwnIdentityBehavior: SetOwnIdentityBehavior = SetOwnIdentityBehavior.Return(identity)
    ) {
        stubMyself(identity)
        stubSetOwnIdentity(identity, setOwnIdentityBehavior, importedFingerPrint)
        stubCanEncrypt(identity.address, returnOnCanEncrypt)
    }

    fun stubMyself(identity: Identity) {
        doReturn(identity).`when`(providerMock).myself(
            identityThat { it.address == identity.address })
    }

    fun stubSetOwnIdentity(
        identity: Identity,
        setOwnIdentityBehavior: SetOwnIdentityBehavior,
        importedFingerPrint: String
    ) {
        when (setOwnIdentityBehavior) {
            is SetOwnIdentityBehavior.Throw ->
                doThrow(setOwnIdentityBehavior.e).`when`(providerMock).setOwnIdentity(
                    identityThat { it.address == identity.address },
                    eq(importedFingerPrint)
                )
            is SetOwnIdentityBehavior.Return ->
                doReturn(setOwnIdentityBehavior.identity).`when`(providerMock).setOwnIdentity(
                    identityThat { it.address == identity.address },
                    eq(importedFingerPrint)
                )
        }
    }

    fun stubCanEncrypt(email: String, returnOnCanEncrypt: Boolean) {
        doReturn(returnOnCanEncrypt).`when`(providerMock).canEncrypt(eq(email))
    }

    sealed class SetOwnIdentityBehavior {
        class Throw(val e: Throwable) : SetOwnIdentityBehavior()
        class Return(val identity: Identity?) : SetOwnIdentityBehavior()
    }
}