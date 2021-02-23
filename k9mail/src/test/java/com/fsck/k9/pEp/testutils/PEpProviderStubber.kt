package com.fsck.k9.pEp.testutils

import com.fsck.k9.pEp.PEpProvider
import com.fsck.k9.pEp.testutils.AssertUtils.identityThat
import com.nhaarman.mockito_kotlin.*
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

    fun stubImportKeyThrowing(exception: Throwable) {
        given(providerMock.importKey(any())).will {
            throw exception
        }
    }

    fun stubProviderMethodsForIdentity(
        identity: Identity,
        importedFingerPrint: String,
        returnOnCanEncrypt: Boolean = true,
        returnBehavior: ReturnBehavior<Identity> = ReturnBehavior.Return(identity)
    ) {
        stubMyself(identity)
        stubSetOwnIdentity(identity, returnBehavior, importedFingerPrint)
        stubCanEncrypt(identity.address, returnOnCanEncrypt)
    }

    fun stubMyself(identity: Identity) {
        doReturn(identity).`when`(providerMock).myself(
            identityThat { it.address == identity.address })
    }

    fun stubSetOwnIdentity(
        identity: Identity,
        returnBehavior: ReturnBehavior<Identity>,
        importedFingerPrint: String
    ) {
        when (returnBehavior) {
            is ReturnBehavior.Throw ->
                doThrow(returnBehavior.e).`when`(providerMock).setOwnIdentity(
                    identityThat { it.address == identity.address },
                    eq(importedFingerPrint)
                )
            is ReturnBehavior.Return ->
                doReturn(returnBehavior.value).`when`(providerMock).setOwnIdentity(
                    identityThat { it.address == identity.address },
                    eq(importedFingerPrint)
                )
        }
    }

    fun stubCanEncrypt(email: String, returnOnCanEncrypt: Boolean) {
        doReturn(returnOnCanEncrypt).`when`(providerMock).canEncrypt(eq(email))
    }
}