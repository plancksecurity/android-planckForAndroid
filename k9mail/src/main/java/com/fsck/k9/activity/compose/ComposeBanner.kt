package com.fsck.k9.activity.compose

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.fsck.k9.BuildConfig
import com.fsck.k9.R
import com.fsck.k9.databinding.ComposeBannerBinding
import com.fsck.k9.planck.infrastructure.NEW_LINE
import com.fsck.k9.planck.infrastructure.extensions.getStackTrace


private const val DEBUG_STACK_TRACE_DEPTH = 1
private const val SUPER_STATE = "super_state"
private const val STATE_LAST_ERROR = "lastError"

class ComposeBanner(context: Context, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {
    private val binding: ComposeBannerBinding

    init {
        val inflater = LayoutInflater.from(context)
        binding = ComposeBannerBinding.inflate(inflater, this, true)
        isVisible = false
    }

    private val bannerText: TextView = binding.bannerText
    private var lastError: StringBuilder? = null
    private var currentBanner = BannerType.NONE

    fun showUnsecureDeliveryWarning(
        unsecureRecipientsCount: Int,
        onClickListener: OnClickListener
    ) {
        if (wasAbleToChangeBanner(BannerType.UNSECURE_DELIVERY)) {
            binding.removeRecipients.isVisible = true
            binding.inviteRecipients.isVisible = true
            bannerText.setTextColor(
                ContextCompat.getColor(
                    context, R.color.compose_unsecure_delivery_warning
                )
            )
            bannerText.text = resources.getQuantityString(
                R.plurals.compose_unsecure_delivery_warning,
                unsecureRecipientsCount,
                unsecureRecipientsCount
            )
            bannerText.setOnClickListener(onClickListener)
            showUserActionBanner()
        }
    }

    private fun showUserActionBanner() {
        isVisible = true
    }

    fun hideUnsecureDeliveryWarning() {
        hideUserActionBanner(BannerType.UNSECURE_DELIVERY)
    }

    fun hideUserActionBanner() {
        hideUserActionBanner(BannerType.ERROR)
    }

    private fun hideUserActionBanner(bannerType: BannerType) {
        if (currentBanner == bannerType) {
            currentBanner = BannerType.NONE
            isVisible = false
        }
    }

    fun showSingleRecipientHandshakeBanner(onClickListener: OnClickListener) {
        if (wasAbleToChangeBanner(BannerType.HANDSHAKE)) {
            bannerText.setTextColor(ContextCompat.getColor(context, R.color.planck_green))
            bannerText.setText(R.string.compose_single_recipient_handshake_banner)
            bannerText.setOnClickListener(onClickListener)
            showUserActionBanner()
        }
    }

    fun hideSingleRecipientHandshakeBanner() {
        hideUserActionBanner(BannerType.HANDSHAKE)
    }

    fun setAndShowError(throwable: Throwable) {
        bannerText.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.compose_unsecure_delivery_warning
            )
        )
        val errorText: String = getErrorText(throwable)
        if (shouldInitializeError()) {
            lastError = java.lang.StringBuilder(errorText)
        } else {
            addNewDebugErrorText(errorText)
        }
        showError(lastError.toString())
    }

    private fun showError(error: String) {
        currentBanner = BannerType.ERROR
        bannerText.text = error
        bannerText.setOnClickListener(null)
        showUserActionBanner()
    }

    private fun addNewDebugErrorText(newErrorText: String) {
        lastError!!.append(NEW_LINE)
        lastError!!.append(newErrorText)
    }

    private fun shouldInitializeError(): Boolean {
        return !BuildConfig.DEBUG || lastError == null
    }

    private fun wasAbleToChangeBanner(bannerType: BannerType): Boolean {
        return if (currentBanner.priority <= bannerType.priority) {
            currentBanner = bannerType
            true
        } else false
    }

    private fun getErrorText(throwable: Throwable): String {
        return if (BuildConfig.DEBUG) throwable.getStackTrace(DEBUG_STACK_TRACE_DEPTH) else context.getString(
            R.string.error_happened_restart_app
        )
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val state = Bundle()
        state.putParcelable(SUPER_STATE, superState)
        state.putString(STATE_LAST_ERROR, lastError?.toString())
        return state
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var customState = state
        if (customState is Bundle) {
            customState.getString(STATE_LAST_ERROR)?.let { errorText ->
                lastError = java.lang.StringBuilder(errorText)
                showError(errorText)
            }

            customState = customState.getParcelable(SUPER_STATE)
        }
        super.onRestoreInstanceState(customState)
    }

    private enum class BannerType(val priority: Int) {
        NONE(0),
        HANDSHAKE(1),
        UNSECURE_DELIVERY(2),
        ERROR(Int.MAX_VALUE)
    }

}
