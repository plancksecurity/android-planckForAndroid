package com.fsck.k9.planck

import com.fsck.k9.mail.Body
import com.fsck.k9.mail.internet.MimeMessage

internal class NotRemovingTransientFilespEpMessageBuilder(
    message: MimeMessage
) : PlanckMessageBuilder(message) {

    override fun extractBodyContent(body: Body?): ByteArray {
        return PlanckUtils.extractBodyContent(body, false)
    }
}
