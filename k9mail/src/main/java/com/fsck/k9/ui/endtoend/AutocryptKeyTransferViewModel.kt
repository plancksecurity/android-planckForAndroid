package com.fsck.k9.ui.endtoend

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class AutocryptKeyTransferViewModel @Inject constructor(
        val autocryptSetupMessageLiveEvent: AutocryptSetupMessageLiveEvent,
        val autocryptSetupTransferLiveEvent: AutocryptSetupTransferLiveEvent) : ViewModel()
