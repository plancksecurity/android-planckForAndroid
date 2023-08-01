package com.fsck.k9.ui.messageview

import foundation.pEp.jniadapter.Rating

interface SenderKeyResetHelperView {
    fun allowResetSenderKey()
    fun updateRating(rating: Rating)
}