package com.fsck.k9.message.html

import javax.inject.Inject

data class HtmlSettings @Inject constructor(
    val useFixedWidthFont: Boolean
)
