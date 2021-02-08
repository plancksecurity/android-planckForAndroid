package com.fsck.k9.ui.settings.account
import kotlinx.serialization.Serializable

@Serializable
data class ConfiguredSetting<SETTING_TYPE>(var value: SETTING_TYPE, val locked: Boolean)