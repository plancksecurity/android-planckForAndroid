package security.planck.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import com.fsck.k9.Preferences
import com.fsck.k9.databinding.GroupTestBinding
import com.fsck.k9.mail.Address
import com.fsck.k9.planck.PlanckActivity
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import com.fsck.k9.planck.ui.tools.FeedbackTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Vector
import javax.inject.Inject

class GroupTestScreen: PlanckActivity() {
    private lateinit var binding: GroupTestBinding

    @Inject
    lateinit var planckProvider: PlanckProvider

    @Inject
    lateinit var preferences: Preferences

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GroupTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpToolbar(true)
        binding.createGroupButton.setOnClickListener {
            createGroupFromUserInput()
        }
        binding.createEmptyGroupButton.setOnClickListener {
            createGroup()
        }
    }

    private fun createGroup() {
        uiScope.launch {
            binding.emptyGroupCreationFeedback.text = ""
            withContext(PlanckDispatcher) {
                val account = preferences.accounts.first()
                val manager = PlanckUtils.createIdentity(Address(account.email, account.name), this@GroupTestScreen)
                val groupIdentity = PlanckUtils.createIdentity(Address("empty.test.group@testgroup.ch", "groupName"), this@GroupTestScreen)
                kotlin.runCatching { planckProvider.createGroup(groupIdentity, manager, Vector()) }
            }.onFailure {
                Timber.e(it, "error creating empty group")
                binding.emptyGroupCreationFeedback.text = it.message
                displayError(it)
            }.onSuccess {
                binding.emptyGroupCreationFeedback.text = "Empty group created successfully"
            }

        }
    }

    private fun createGroupFromUserInput() {
        uiScope.launch {
            binding.groupCreationFeedback.text = ""
            val groupAddress = binding.groupAddress.text.toString()
            val groupUserName = binding.groupUserName.text?.toString()
            val memberAddresses = binding.groupMemberAddresses.text.toString()
            withContext(PlanckDispatcher) {
                val account = preferences.accounts.first()
                val manager = PlanckUtils.createIdentity(Address(account.email, account.name), this@GroupTestScreen)
                val groupIdentity = PlanckUtils.createIdentity(Address(groupAddress, groupUserName), this@GroupTestScreen)
                val memberIdentities = Vector(memberAddresses.split(",").mapNotNull { memberText ->
                    val parts = memberText.split(":")
                    var memberUserName: String? = null
                    var memberAddress: String? = null
                    if (parts.isNotEmpty()) {
                        if (parts.size == 2) {
                            memberUserName = parts.last()
                        }
                        memberAddress = parts.first()
                        PlanckUtils.createIdentity(Address(memberAddress, memberUserName), this@GroupTestScreen)
                    } else {
                        null
                    }
                }
                )
                kotlin.runCatching {
                    if (memberIdentities.isNotEmpty()) {
                        planckProvider.createGroup(groupIdentity, manager, memberIdentities)
                    } else {
                        error("group members was empty")
                    }
                }
            }.onFailure {
                Timber.e(it, "error creating group from user input")
                binding.groupCreationFeedback.text = it.message
                displayError(it)
            }.onSuccess {
                binding.groupCreationFeedback.text = "Group created successfully"
            }
        }
    }

    private fun displayError(e: Throwable) {
        FeedbackTools.showLongFeedback(binding.root, e.message)
    }

    override fun inject() {
        planckComponent.inject(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        @JvmStatic
        fun start(context: Activity) {
            context.startActivity(Intent(context, GroupTestScreen::class.java))
        }
    }
}