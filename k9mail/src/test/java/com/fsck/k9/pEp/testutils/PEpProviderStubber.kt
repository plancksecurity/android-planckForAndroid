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

    fun stubProviderMethodsForIdentity(
            identity: Identity,
    ) {
        stubMyself(identity)
    }

    fun stubMyself(identity: Identity) {
        doReturn(identity).`when`(providerMock).myself(
                identityThat { it.address == identity.address })
    }
}