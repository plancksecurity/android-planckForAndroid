package com.fsck.k9.ui.messageview

import com.fsck.k9.Account
import com.fsck.k9.activity.MessageReference
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.MutableStateFlow

class MessageViewUpdate(
    val stateFlow: MutableStateFlow<MessageViewState> = MutableStateFlow(MessageViewState.Idle),
    val effectFlow: MutableStateFlow<MessageViewEffect> = MutableStateFlow(MessageViewEffect.NoEffect)
) {
    lateinit var account: Account
    lateinit var messageReference: MessageReference
}