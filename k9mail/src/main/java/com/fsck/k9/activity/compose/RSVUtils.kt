package com.fsck.k9.activity.compose

import foundation.pEp.jniadapter.Rating

fun Recipient.toRatedRecipient(rating: Rating) = RatedRecipient(this, rating)
