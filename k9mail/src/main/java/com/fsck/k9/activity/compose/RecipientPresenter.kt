package com.fsck.k9.activity.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import androidx.annotation.VisibleForTesting
import androidx.loader.app.LoaderManager
import com.fsck.k9.Account
import com.fsck.k9.K9
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.activity.MessageReference
import com.fsck.k9.activity.compose.ComposeCryptoStatus.AttachErrorState
import com.fsck.k9.activity.compose.ComposeCryptoStatus.ComposeCryptoStatusBuilder
import com.fsck.k9.activity.compose.ComposeCryptoStatus.SendErrorState
import com.fsck.k9.helper.Contacts
import com.fsck.k9.helper.MailTo
import com.fsck.k9.helper.ReplyToParser
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Flag
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.message.ComposePgpInlineDecider
import com.fsck.k9.message.MessageBuilder
import com.fsck.k9.message.PgpMessageBuilder
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckProvider.ProtectionScope
import com.fsck.k9.planck.PlanckUIArtefactCache
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.Poller
import com.fsck.k9.ui.getEnum
import com.fsck.k9.ui.putEnum
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.openintents.openpgp.OpenPgpApiManager
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpApiManagerCallback
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderError
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderState
import org.openintents.openpgp.util.OpenPgpServiceConnection
import security.planck.echo.MessageReceivedListener
import timber.log.Timber
import java.util.Collections

class RecipientPresenter(
    // transient state, which is either obtained during construction and initialization, or cached
    private val context: Context,
    loaderManager: LoaderManager,
    private val openPgpApiManager: OpenPgpApiManager,
    private val recipientMvpView: RecipientMvpView,
    account: Account,
    private val composePgpInlineDecider: ComposePgpInlineDecider,
    private val planck: PlanckProvider,
    private val replyToParser: ReplyToParser,
    private val listener: RecipientsChangedListener
) : MessageReceivedListener {
    private lateinit var toPresenter: RecipientSelectPresenter
    private lateinit var ccPresenter: RecipientSelectPresenter
    private lateinit var bccPresenter: RecipientSelectPresenter
    private var poller: Poller? = null
    private lateinit var account: Account
    private var hasContactPicker: Boolean? = null
    private var cachedCryptoStatus: ComposeCryptoStatus? = null
    private var openPgpServiceConnection: OpenPgpServiceConnection? = null

    // persistent state, saved during onSaveInstanceState
    private var lastFocusedType = RecipientType.TO

    // TODO initialize cryptoMode to other values under some circumstances, e.g. if we reply to an encrypted e-mail
    private var currentCryptoMode = CryptoMode.OPPORTUNISTIC
    private var cryptoEnablePgpInline = false
    var isForceUnencrypted = false
        private set

    var isAlwaysSecure = false
    private var privacyState: Rating = Rating.pEpRatingUndefined
    private val isReplyToEncryptedMessage = false
    private var lastRequestTime: Long = 0
    private var planckUiCache: PlanckUIArtefactCache
    private val uiScope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun setPresenter(presenter: RecipientSelectPresenter, type: RecipientType?) {
        when (type) {
            RecipientType.TO -> toPresenter = presenter
            RecipientType.CC -> ccPresenter = presenter
            RecipientType.BCC -> bccPresenter = presenter
            else -> {}
        }
    }

    private fun setupPlanckStatusPolling() {
        if (poller == null) {
            poller = Poller(Handler())
            poller?.init(POLLING_INTERVAL.toLong()) { loadPEpStatus() }
        } else {
            poller?.stopPolling()
        }
        poller?.startPolling()
    }

    val toAddresses: List<Address>
        get() = toPresenter.addresses
    val ccAddresses: List<Address>
        get() = ccPresenter.addresses
    val bccAddresses: List<Address>
        get() = bccPresenter.addresses

    fun clearUnsecureRecipients() {
        toPresenter.clearUnsecureAddresses()
        ccPresenter.clearUnsecureAddresses()
        bccPresenter.clearUnsecureAddresses()
    }

    fun startHandshakeWithSingleRecipient(relatedMessageReference: MessageReference?) {
        uiScope.launch {
            refreshRecipients()
            if (canHandshakeSingleAddress(
                    toAddresses,
                    ccAddresses,
                    bccAddresses
                )
            ) {
                recipientMvpView.setMessageReference(relatedMessageReference)
                recipientMvpView.onPlanckPrivacyStatus()
            }
        }
    }

    fun refreshRecipients() {
        val recipients = ArrayList<Identity>()
        recipients.addAll(PlanckUtils.createIdentities(toAddresses, context.applicationContext))
        recipients.addAll(PlanckUtils.createIdentities(ccAddresses, context.applicationContext))
        recipients.addAll(PlanckUtils.createIdentities(bccAddresses, context.applicationContext))
        planckUiCache.setRecipients(account, recipients)
    }

    private val allRecipients: List<Recipient>
        get() {
            val result = mutableListOf<Recipient>()
            result.addAll(toPresenter.recipients)
            result.addAll(ccPresenter.recipients)
            result.addAll(bccPresenter.recipients)
            return result
        }

    fun checkRecipientsOkForSending(): Boolean {
        toPresenter.tryPerformCompletion()
        ccPresenter.tryPerformCompletion()
        bccPresenter.tryPerformCompletion()
        if (toPresenter.reportedUncompletedRecipients()
            || ccPresenter.reportedUncompletedRecipients()
            || bccPresenter.reportedUncompletedRecipients()
        ) return true
        if (addressesAreEmpty(toAddresses, ccAddresses, bccAddresses)) {
            toPresenter.showNoRecipientsError()
            return true
        }
        return false
    }

    fun initFromReplyToMessage(message: Message?, isReplyAll: Boolean) {
        val replyToAddresses = if (isReplyAll) replyToParser.getRecipientsToReplyAllTo(
            message,
            account
        ) else replyToParser.getRecipientsToReplyTo(message, account)
        addToAddresses(*replyToAddresses.to)
        addCcAddresses(*replyToAddresses.cc)
        val shouldSendAsPgpInline = composePgpInlineDecider.shouldReplyInline(message)
        if (shouldSendAsPgpInline) {
            cryptoEnablePgpInline = true
        }
    }

    fun initFromMailto(mailTo: MailTo) {
        addToAddresses(*mailTo.to)
        addCcAddresses(*mailTo.cc)
        addBccAddresses(*mailTo.bcc)
    }

    fun initFromSendOrViewIntent(intent: Intent) {
        val extraEmail = intent.getStringArrayExtra(Intent.EXTRA_EMAIL)
        val extraCc = intent.getStringArrayExtra(Intent.EXTRA_CC)
        val extraBcc = intent.getStringArrayExtra(Intent.EXTRA_BCC)
        if (extraEmail != null) {
            addToAddresses(*addressFromStringArray(extraEmail))
        }
        if (extraCc != null) {
            addCcAddresses(*addressFromStringArray(extraCc))
        }
        if (extraBcc != null) {
            addBccAddresses(*addressFromStringArray(extraBcc))
        }
    }

    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        recipientMvpView.setCcVisibility(savedInstanceState.getBoolean(STATE_KEY_CC_SHOWN))
        recipientMvpView.setBccVisibility(savedInstanceState.getBoolean(STATE_KEY_BCC_SHOWN))
        lastFocusedType = savedInstanceState.getEnum(STATE_KEY_LAST_FOCUSED_TYPE, RecipientType.TO)
        currentCryptoMode =
            savedInstanceState.getEnum(STATE_KEY_CURRENT_CRYPTO_MODE, CryptoMode.OPPORTUNISTIC)
        cryptoEnablePgpInline = savedInstanceState.getBoolean(STATE_KEY_CRYPTO_ENABLE_PGP_INLINE)
        isForceUnencrypted = savedInstanceState.getBoolean(STATE_FORCE_UNENCRYPTED)
        isAlwaysSecure = savedInstanceState.getBoolean(STATE_ALWAYS_SECURE)
        privacyState = savedInstanceState.getEnum<Rating>(STATE_RATING, Rating.pEpRatingUndefined)
        updateRecipientExpanderVisibility()
        recipientMvpView.planckRating = privacyState
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_KEY_CC_SHOWN, recipientMvpView.isCcVisible)
        outState.putBoolean(STATE_KEY_BCC_SHOWN, recipientMvpView.isBccVisible)
        outState.putEnum(STATE_KEY_LAST_FOCUSED_TYPE, lastFocusedType)
        outState.putEnum(STATE_KEY_CURRENT_CRYPTO_MODE, currentCryptoMode)
        outState.putBoolean(STATE_KEY_CRYPTO_ENABLE_PGP_INLINE, cryptoEnablePgpInline)
        outState.putBoolean(STATE_FORCE_UNENCRYPTED, isForceUnencrypted)
        outState.putBoolean(STATE_ALWAYS_SECURE, isAlwaysSecure)
        outState.putEnum(STATE_RATING, privacyState)
    }

    fun initFromDraftMessage(message: Message) {
        initRecipientsFromDraftMessage(message)
        initPgpInlineFromDraftMessage(message)
        isAlwaysSecure = message.isSet(Flag.X_PEP_NEVER_UNSECURE)
    }

    private fun initRecipientsFromDraftMessage(message: Message) {
        addToAddresses(*message.getRecipients(RecipientType.TO))
        val ccRecipients = message.getRecipients(RecipientType.CC)
        addCcAddresses(*ccRecipients)
        val bccRecipients = message.getRecipients(RecipientType.BCC)
        addBccAddresses(*bccRecipients)
    }

    private fun initPgpInlineFromDraftMessage(message: Message) {
        cryptoEnablePgpInline = message.isSet(Flag.X_DRAFT_OPENPGP_INLINE)
    }

    private fun addToAddresses(vararg toAddresses: Address) {
        addRecipientsFromAddresses(RecipientType.TO, *toAddresses)
    }

    private fun addCcAddresses(vararg ccAddresses: Address) {
        if (ccAddresses.isNotEmpty()) {
            addRecipientsFromAddresses(RecipientType.CC, *ccAddresses)
            recipientMvpView.setCcVisibility(true)
            updateRecipientExpanderVisibility()
        }
    }

    fun addBccAddresses(vararg bccRecipients: Address) {
        if (bccRecipients.isNotEmpty()) {
            addRecipientsFromAddresses(RecipientType.BCC, *bccRecipients)
            val bccAddress = account.alwaysBcc

            // If the auto-bcc is the only entry in the BCC list, don't show the Bcc fields.
            val alreadyVisible = recipientMvpView.isBccVisible
            val singleBccRecipientFromAccount =
                bccRecipients.size == 1 && bccRecipients.first().toString() == bccAddress
            recipientMvpView.setBccVisibility(alreadyVisible || !singleBccRecipientFromAccount)
            updateRecipientExpanderVisibility()
        }
    }

    fun onPrepareOptionsMenu(menu: Menu) {
        val noContactPickerAvailable = !hasContactPicker()
        if (noContactPickerAvailable) {
            menu.findItem(R.id.add_from_contacts).setVisible(false)
        }
    }

    fun onSwitchAccount(account: Account) {
        this.account = account
        if (account.isAlwaysShowCcBcc) {
            recipientMvpView.setCcVisibility(true)
            recipientMvpView.setBccVisibility(true)
            updateRecipientExpanderVisibility()
        }
        val openPgpProvider = account.openPgpProvider
        recipientMvpView.setCryptoProvider(openPgpProvider)
        openPgpApiManager.setOpenPgpProvider(openPgpProvider, openPgpCallback)
    }

    fun onSwitchIdentity(identity: com.fsck.k9.Identity?) {

        // TODO decide what actually to do on identity switch?
        /*
        if (mIdentityChanged) {
            mBccWrapper.setVisibility(View.VISIBLE);
        }
        mBccView.setText("");
        mBccView.addAddress(new Address(mAccount.getAlwaysBcc(), ""));
        */
    }

    fun onClickToLabel() {
        recipientMvpView.requestFocusOnToField()
    }

    fun onClickCcLabel() {
        recipientMvpView.requestFocusOnCcField()
    }

    fun onClickBccLabel() {
        recipientMvpView.requestFocusOnBccField()
    }

    fun onClickRecipientExpander() {
        recipientMvpView.setCcVisibility(true)
        recipientMvpView.setBccVisibility(true)
        updateRecipientExpanderVisibility()
    }

    private fun hideEmptyExtendedRecipientFields() {
        if (ccAddresses.isEmpty()) {
            recipientMvpView.setCcVisibility(false)
            if (lastFocusedType == RecipientType.CC) {
                lastFocusedType = RecipientType.TO
            }
        }
        if (bccAddresses.isEmpty()) {
            recipientMvpView.setBccVisibility(false)
            if (lastFocusedType == RecipientType.BCC) {
                lastFocusedType = RecipientType.TO
            }
        }
        updateRecipientExpanderVisibility()
    }

    private fun updateRecipientExpanderVisibility() {
        val notBothAreVisible = !(recipientMvpView.isCcVisible && recipientMvpView.isBccVisible)
        recipientMvpView.setRecipientExpanderVisibility(notBothAreVisible)
    }

    fun updateCryptoStatus() {
        cachedCryptoStatus = null
        loadPEpStatus()
        val openPgpProviderState = openPgpApiManager.openPgpProviderState
        var accountCryptoKey: Long? = account.openPgpKey
        if (accountCryptoKey == Account.NO_OPENPGP_KEY) {
            accountCryptoKey = null
        }
    }

    val currentCryptoStatus: ComposeCryptoStatus
        get() {
            if (cachedCryptoStatus == null) {
                val builder = ComposeCryptoStatusBuilder()
                    .setOpenPgpProviderState(OpenPgpProviderState.UNCONFIGURED)
                    .setCryptoMode(currentCryptoMode)
                    .setEnablePgpInline(cryptoEnablePgpInline)
                    .setRecipients(allRecipients)
                val accountCryptoKey = account.openPgpKey
                if (accountCryptoKey != Account.NO_OPENPGP_KEY) {
                    // TODO split these into individual settings? maybe after key is bound to identity
                    builder.setSigningKeyId(accountCryptoKey)
                    builder.setSelfEncryptId(accountCryptoKey)
                }
                cachedCryptoStatus = builder.build()
            }
            return cachedCryptoStatus!!
        }
    val isForceTextMessageFormat: Boolean
        get() = if (cryptoEnablePgpInline) {
            val cryptoStatus = currentCryptoStatus
            cryptoStatus.isEncryptionEnabled || cryptoStatus.isSigningEnabled
        } else {
            false
        }

    fun onRecipientsChanged() {
        updateCryptoStatus()
        listener.onRecipientsChanged()
    }

    fun showError(throwable: Throwable?) {
        recipientMvpView.showError(throwable)
    }

    fun onCryptoModeChanged(cryptoMode: CryptoMode) {
        currentCryptoMode = cryptoMode
        updateCryptoStatus()
    }

    fun onCryptoPgpInlineChanged(enablePgpInline: Boolean) {
        cryptoEnablePgpInline = enablePgpInline
        updateCryptoStatus()
    }

    private fun addRecipientsFromAddresses(
        recipientType: RecipientType,
        vararg addresses: Address
    ) {
        object : RecipientLoader(context, account.openPgpProvider, *addresses) {
            override fun deliverResult(result: List<Recipient>?) {
                result?.let {
                    val recipientArray = result.toTypedArray<Recipient>()
                    addRecipients(recipientType, *recipientArray)
                    stopLoading()
                    abandon()
                }
            }
        }.startLoading()
    }

    fun addRecipients(recipientType: RecipientType?, vararg recipients: Recipient) {
        when (recipientType) {
            RecipientType.TO -> {
                toPresenter.addRecipients(*recipients)
            }

            RecipientType.CC -> {
                ccPresenter.addRecipients(*recipients)
            }

            RecipientType.BCC -> {
                bccPresenter.addRecipients(*recipients)
            }

            else -> {}
        }
    }

    private fun addRecipientFromContactUri(recipientType: RecipientType, uri: Uri?) {
        object : RecipientLoader(context, account.openPgpProvider, uri, false) {
            override fun deliverResult(result: List<Recipient>?) {
                // TODO handle multiple available mail addresses for a contact?
                result?.let {
                    if (result.isEmpty()) {
                        recipientMvpView.showErrorContactNoAddress()
                        return
                    }
                    val recipient = result.first()
                    addRecipients(recipientType, recipient)
                    stopLoading()
                    abandon()
                }
            }
        }.startLoading()
    }

    fun onToFocused() {
        lastFocusedType = RecipientType.TO
    }

    fun onCcFocused() {
        lastFocusedType = RecipientType.CC
    }

    fun onBccFocused() {
        lastFocusedType = RecipientType.BCC
    }

    fun onMenuAddFromContacts() {
        val requestCode = recipientTypeToRequestCode(lastFocusedType)
        recipientMvpView.showContactPicker(requestCode)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            CONTACT_PICKER_TO,
            CONTACT_PICKER_CC,
            CONTACT_PICKER_BCC -> {
                if (resultCode != Activity.RESULT_OK || data == null) {
                    return
                }
                val recipientType = recipientTypeFromRequestCode(requestCode)
                addRecipientFromContactUri(recipientType, data.data)
            }

            OPENPGP_USER_INTERACTION -> openPgpApiManager.onUserInteractionResult()
        }
    }

    fun handleVerifyPartnerIdentityResult() {
        loadPEpStatus()
    }

    fun onNonRecipientFieldFocused() {
        if (!account.isAlwaysShowCcBcc) {
            hideEmptyExtendedRecipientFields()
        }
    }

    fun onClickCryptoStatus() {
        when (openPgpApiManager.openPgpProviderState) {
            OpenPgpProviderState.UNCONFIGURED -> {
                Timber.e("click on crypto status while unconfigured - this should not really happen?!")
                return
            }

            OpenPgpProviderState.OK -> {
                if (cachedCryptoStatus?.isSignOnly == true) {
                    recipientMvpView.showErrorIsSignOnly()
                } else {
                    recipientMvpView.showCryptoDialog(currentCryptoMode)
                }
                return
            }

            OpenPgpProviderState.UI_REQUIRED -> {
                // TODO show openpgp settings
                recipientMvpView.launchUserInteractionPendingIntent(
                    openPgpApiManager.userInteractionPendingIntent,
                    OPENPGP_USER_INTERACTION
                )
            }

            OpenPgpProviderState.UNINITIALIZED, OpenPgpProviderState.ERROR -> openPgpApiManager.refreshConnection()
        }
    }

    /**
     * Does the device actually have a Contacts application suitable for
     * picking a contact. As hard as it is to believe, some vendors ship
     * without it.
     *
     * @return True, if the device supports picking contacts. False, otherwise.
     */
    private fun hasContactPicker(): Boolean {
        if (hasContactPicker == null) {
            val contacts = Contacts.getInstance(
                context
            )
            val packageManager = context.packageManager
            val resolveInfoList =
                packageManager.queryIntentActivities(contacts.contactPickerIntent(), 0)
            hasContactPicker = resolveInfoList.isNotEmpty()
        }
        return hasContactPicker ?: false
    }

    fun showPgpSendError(sendErrorState: SendErrorState?) {
        when (sendErrorState) {
            SendErrorState.PROVIDER_ERROR -> recipientMvpView.showErrorOpenPgpConnection()
            SendErrorState.SIGN_KEY_NOT_CONFIGURED -> recipientMvpView.showErrorMissingSignKey()
            SendErrorState.PRIVATE_BUT_MISSING_KEYS -> recipientMvpView.showErrorPrivateButMissingKeys()
            else -> throw AssertionError("not all error states handled, this is a bug!")
        }
    }

    fun showPgpAttachError(attachErrorState: AttachErrorState?) {
        when (attachErrorState) {
            AttachErrorState.IS_INLINE -> recipientMvpView.showErrorInlineAttach()
            else -> throw AssertionError("not all error states handled, this is a bug!")
        }
    }

    fun builderSetProperties(messageBuilder: MessageBuilder) {
        require(messageBuilder !is PgpMessageBuilder) { "PpgMessageBuilder must be called with ComposeCryptoStatus argument!" }
        messageBuilder.setTo(toAddresses)
        messageBuilder.setCc(ccAddresses)
        messageBuilder.setBcc(bccAddresses)
    }

    fun builderSetProperties(pgpBuilder: PgpMessageBuilder) {
        pgpBuilder.setOpenPgpApi(openPgpApiManager.openPgpApi)
        pgpBuilder.setCryptoStatus(currentCryptoStatus)
    }

    fun onCryptoPgpSignOnlyDisabled() {
        onCryptoPgpInlineChanged(false)
        onCryptoModeChanged(CryptoMode.OPPORTUNISTIC)
    }

    private fun checkAndIncrementPgpInlineDialogCounter(): Boolean {
        val pgpInlineDialogCounter = K9.getPgpInlineDialogCounter()
        if (pgpInlineDialogCounter < PGP_DIALOG_DISPLAY_THRESHOLD) {
            K9.setPgpInlineDialogCounter(pgpInlineDialogCounter + 1)
            return true
        }
        return false
    }

    fun switchPrivacyProtection(scope: ProtectionScope?, vararg protection: Boolean) {
        if (bccAddresses.isEmpty()) {
            when (scope) {
                ProtectionScope.MESSAGE -> {
                    if (protection.isNotEmpty()) throw RuntimeException("On message only switch allowed")
                    isForceUnencrypted = !isForceUnencrypted
                }

                ProtectionScope.ACCOUNT -> {
                    if (protection.isEmpty()) throw RuntimeException("On account only explicit boolean allowed")
                    isForceUnencrypted = !protection.first()
                }

                else -> {}
            }
        } else {
            isForceUnencrypted = !isForceUnencrypted
        }
        updateCryptoStatus()
    }

    fun onResume() {
        notifyRecipientsChanged()
        updateCryptoStatus()
    }

    fun handlepEpState() {
        recipientMvpView.handlepEpState(allRecipients.isEmpty())
    }

    fun isForwardedMessageWeakestThanOriginal(originalMessageRating: Rating): Boolean {
        val currentRating = recipientMvpView.planckRating
        return currentRating.value < Rating.pEpRatingReliable.value && currentRating.value < originalMessageRating.value
    }

    @VisibleForTesting
    fun setOpenPgpServiceConnection(
        openPgpServiceConnection: OpenPgpServiceConnection?,
        cryptoProvider: String?
    ) {
        this.openPgpServiceConnection = openPgpServiceConnection
        //this.cryptoProvider = cryptoProvider;
    }

    fun shouldSaveRemotely(): Boolean {
        // TODO more appropriate logic?
        return cachedCryptoStatus == null || (cachedCryptoStatus?.isEncryptionEnabled ?: false)
    }

    override fun messageReceived() {
        updateCryptoStatus()
        if (account.isPlanckPrivacyProtected && K9.isPlanckForwardWarningEnabled()) {
            toPresenter.updateRecipientsFromMessage()
            ccPresenter.updateRecipientsFromMessage()
        }
    }

    fun handleResetPartnerKeyResult() {
        loadPEpStatus()
    }

    fun canResetSenderKeys(
        newToAdresses: List<Address>,
        newCcAdresses: List<Address>,
        newBccAdresses: List<Address>
    ): Boolean {
        return (recipientConditionsForKeyReset(newToAdresses, newCcAdresses, newBccAdresses)
                && ratingConditionsForSenderKeyReset())
    }

    private fun handleSingleAddressKeyResetAllowance(
        newToAdresses: List<Address>,
        newCcAdresses: List<Address>,
        newBccAdresses: List<Address>
    ) {
        if (canResetSenderKeys(newToAdresses, newCcAdresses, newBccAdresses)) {
            recipientMvpView.showResetPartnerKeyOption()
        } else {
            recipientMvpView.hideResetPartnerKeyOption()
        }
    }

    private fun recipientConditionsForKeyReset(
        newToAdresses: List<Address>,
        newCcAdresses: List<Address>,
        newBccAdresses: List<Address>
    ): Boolean {
        return account.isPlanckPrivacyProtected
                && newToAdresses.size == ONE_ADDRESS
                && newBccAdresses.isEmpty()
                && newCcAdresses.isEmpty()
                && !Preferences.getPreferences(context)
            .containsAccountByEmail(newToAdresses.first().address)
    }

    private fun ratingConditionsForSenderKeyReset(): Boolean {
        return !PlanckUtils.isRatingUnsecure(privacyState) || privacyState === Rating.pEpRatingMistrust
    }

    fun resetPartnerKeys() {
        recipientMvpView.resetPartnerKeys(toPresenter.addresses.first().address)
    }

    interface RecipientsChangedListener {
        fun onRecipientsChanged()
    }

    private val openPgpCallback: OpenPgpApiManagerCallback = object : OpenPgpApiManagerCallback {
        override fun onOpenPgpProviderStatusChanged() {
            //asyncUpdateCryptoStatus();
            if (openPgpApiManager.openPgpProviderState == OpenPgpProviderState.UI_REQUIRED) {
                recipientMvpView.showErrorOpenPgpUserInteractionRequired()
            }
        }

        override fun onOpenPgpProviderError(error: OpenPgpProviderError) {
            when (error) {
                OpenPgpProviderError.ConnectionLost -> openPgpApiManager.refreshConnection()
                OpenPgpProviderError.VersionIncompatible -> {
                    //recipientMvpView.showErrorOpenPgpIncompatible();
                }

                else -> recipientMvpView.showErrorOpenPgpConnection()
            }
        }
    }

    init {
        recipientMvpView.setPresenter(this)
        planckUiCache = PlanckUIArtefactCache.getInstance(context.applicationContext)
        recipientMvpView.setLoaderManager(loaderManager)
        onSwitchAccount(account)
        updateCryptoStatus()
    }

    enum class CryptoMode {
        DISABLE,
        SIGN_ONLY,
        OPPORTUNISTIC,
        PRIVATE
    }

    private fun loadPEpStatus() {
        val fromAddress = recipientMvpView.fromAddress
        val newToAdresses = toAddresses
        val newCcAdresses = ccAddresses
        val newBccAdresses = bccAddresses
        if (addressesAreEmpty(newToAdresses, newCcAdresses, newBccAdresses)) {
            showDefaultStatus()
            recipientMvpView.hideUnsecureDeliveryWarning()
            recipientMvpView.hideSingleRecipientHandshakeBanner()
            recipientMvpView.messageRatingLoaded()
            return
        }
        recipientMvpView.messageRatingIsBeingLoaded()
        val requestTime = System.currentTimeMillis()
        lastRequestTime = requestTime
        planck.getRating(
            fromAddress,
            newToAdresses,
            newCcAdresses,
            newBccAdresses,
            object : PlanckProvider.ResultCallback<Rating> {
                override fun onLoaded(rating: Rating) {
                    if (isRequestOutdated(requestTime)) {
                        return
                    }
                    if (addressesAreEmpty(newToAdresses, newCcAdresses, newBccAdresses)) {
                        showDefaultStatus()
                        recipientMvpView.hideUnsecureDeliveryWarning()
                        recipientMvpView.hideSingleRecipientHandshakeBanner()
                        recipientMvpView.hideResetPartnerKeyOption()
                    } else {
                        privacyState = rating
                        handleSingleAddressHandshakeFeedback(
                            newToAdresses,
                            newCcAdresses,
                            newBccAdresses
                        )
                        handleSingleAddressKeyResetAllowance(
                            newToAdresses,
                            newCcAdresses,
                            newBccAdresses
                        )
                        showRatingFeedback(rating)
                    }
                    recipientMvpView.messageRatingLoaded()
                }

                override fun onError(throwable: Throwable) {
                    recipientMvpView.showError(throwable)
                    recipientMvpView.hideResetPartnerKeyOption()
                    if (isRequestOutdated(requestTime)) {
                        return
                    }
                    showDefaultStatus()
                    recipientMvpView.messageRatingLoaded()
                }
            })
    }

    private fun isRequestOutdated(requestTime: Long): Boolean {
        return lastRequestTime > requestTime
    }

    private fun addressesAreEmpty(
        newToAdresses: List<Address>,
        newCcAdresses: List<Address>,
        newBccAdresses: List<Address>
    ): Boolean {
        return newToAdresses.isEmpty() && newCcAdresses.isEmpty() && newBccAdresses.isEmpty()
    }

    fun handleUnsecureDeliveryWarning() {
        val unsecureRecipientsCount = if (K9.isPlanckForwardWarningEnabled()
            && account.isPlanckPrivacyProtected
        ) unsecureRecipientsCount else ZERO_RECIPIENTS
        handleUnsecureDeliveryWarning(unsecureRecipientsCount)
    }

    private fun handleUnsecureDeliveryWarning(unsecureRecipientsCount: Int) {
        if (unsecureRecipientsCount > ZERO_RECIPIENTS) {
            recipientMvpView.showUnsecureDeliveryWarning(unsecureRecipientsCount)
        } else {
            recipientMvpView.hideUnsecureDeliveryWarning()
        }
    }

    private fun handleSingleAddressHandshakeFeedback(
        newToAdresses: List<Address>,
        newCcAdresses: List<Address>,
        newBccAdresses: List<Address>
    ) {
        uiScope.launch {
            if (canHandshakeSingleAddress(newToAdresses, newCcAdresses, newBccAdresses)) {
                recipientMvpView.showSingleRecipientHandshakeBanner()
            } else {
                recipientMvpView.hideSingleRecipientHandshakeBanner()
            }
        }
    }

    private suspend fun canHandshakeSingleAddress(
        newToAdresses: List<Address>,
        newCcAdresses: List<Address>,
        newBccAdresses: List<Address>
    ): Boolean {
        return newToAdresses.size == ONE_ADDRESS && newBccAdresses.isEmpty()
                && newCcAdresses.isEmpty()
                && PlanckUtils.isRatingReliable(privacyState)
                && account.isPlanckPrivacyProtected
                && !newToAdresses.first().address.equals(
            account.email,
            ignoreCase = true
        ) // recipient not my own account
                && !planck.isGroupAddress(newToAdresses.first())
            .onFailure {
                recipientMvpView.showError(it)
            }.getOrDefault(true)
    }

    private val unsecureRecipientsCount: Int
        get() = toPresenter.unsecureAddressChannelCount +
                ccPresenter.unsecureAddressChannelCount +
                bccPresenter.unsecureAddressChannelCount

    private fun showDefaultStatus() {
        privacyState = Rating.pEpRatingUndefined
        showRatingFeedback(privacyState)
    }

    private fun showRatingFeedback(rating: Rating) {
        recipientMvpView.planckRating = rating
        handlepEpState()
    }

    fun notifyRecipientsChanged() {
        recipientMvpView.doUiOperationRestoringFocus {
            toPresenter.notifyRecipientsChanged()
            ccPresenter.notifyRecipientsChanged()
            bccPresenter.notifyRecipientsChanged()
        }
    }

    companion object {
        private const val STATE_KEY_CC_SHOWN = "state:ccShown"
        private const val STATE_KEY_BCC_SHOWN = "state:bccShown"
        private const val STATE_KEY_LAST_FOCUSED_TYPE = "state:lastFocusedType"
        private const val STATE_KEY_CURRENT_CRYPTO_MODE = "state:currentCryptoMode"
        private const val STATE_KEY_CRYPTO_ENABLE_PGP_INLINE = "state:cryptoEnablePgpInline"
        private const val STATE_FORCE_UNENCRYPTED = "forceUnencrypted"
        private const val STATE_ALWAYS_SECURE = "alwaysSecure"
        private const val CONTACT_PICKER_TO = 1
        private const val CONTACT_PICKER_CC = 2
        private const val CONTACT_PICKER_BCC = 3
        private const val OPENPGP_USER_INTERACTION = 4
        const val POLLING_INTERVAL = 1200
        const val STATE_RATING = "rating"
        private const val PGP_DIALOG_DISPLAY_THRESHOLD = 2
        private const val ZERO_RECIPIENTS = 0
        private const val ONE_ADDRESS = 1

        private fun addressFromStringArray(addresses: Array<String>): Array<Address> {
            val result = ArrayList<Address>(addresses.size)
            for (addressStr in addresses) {
                Collections.addAll(result, *Address.parseUnencoded(addressStr))
            }
            return result.toTypedArray<Address>()
        }

        private fun recipientTypeToRequestCode(type: RecipientType): Int {
            return when (type) {
                RecipientType.TO -> {
                    CONTACT_PICKER_TO
                }

                RecipientType.CC -> {
                    CONTACT_PICKER_CC
                }

                RecipientType.BCC -> {
                    CONTACT_PICKER_BCC
                }

                else -> throw AssertionError("Unhandled case: $type")
            }
        }

        private fun recipientTypeFromRequestCode(type: Int): RecipientType {
            return when (type) {
                CONTACT_PICKER_TO -> {
                    RecipientType.TO
                }

                CONTACT_PICKER_CC -> {
                    RecipientType.CC
                }

                CONTACT_PICKER_BCC -> {
                    RecipientType.BCC
                }

                else -> throw AssertionError("Unhandled case: $type")
            }
        }
    }
}
