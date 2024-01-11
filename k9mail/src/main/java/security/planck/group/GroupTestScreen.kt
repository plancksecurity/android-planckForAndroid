package security.planck.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import com.fsck.k9.Preferences
import com.fsck.k9.activity.K9Activity
import com.fsck.k9.databinding.GroupTestBinding
import com.fsck.k9.mail.Address
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.ResultCompat
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import com.fsck.k9.planck.ui.tools.FeedbackTools
import dagger.hilt.android.AndroidEntryPoint
import foundation.pEp.jniadapter.Identity
import foundation.pEp.jniadapter.Rating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Vector
import javax.inject.Inject

@AndroidEntryPoint
class GroupTestScreen : K9Activity() {
    private lateinit var binding: GroupTestBinding

    @Inject
    lateinit var planckProvider: PlanckProvider

    @Inject
    lateinit var preferences: Preferences

    private val uiScope = CoroutineScope(Dispatchers.Main)

    private val groupActions = listOf(
        GroupAction(
            title = "Create a group (you are the manager)",
            inputConfigs = listOf(
                InputConfig(
                    labelText = "Group address",
                    hintText = "group1@group.ch",
                ),
                InputConfig(
                    labelText = "Group name (optional)",
                    hintText = "my group",
                    inputType = InputType.TYPE_CLASS_TEXT
                ),
                InputConfig(
                    labelText = "Group members (optional): provide pairs address:name separated by commas, (no spaces in commas), names are optional",
                    hintText = "member1@group.ch:name1,member2@group.ch:name2",
                    inputType = InputType.TYPE_CLASS_TEXT
                )
            ),
            actionText = "Create group (empty group if no members provided)",
            action = ::createGroupFromUserInput,
            progressText = "Creating group...",
            successText = { "Group created successfully" },
        ),
        GroupAction(
            title = "Query group members and manager",
            inputConfigs = listOf(
                InputConfig(
                    labelText = "Group address",
                    hintText = "group1@group.ch",
                ),
                InputConfig(
                    labelText = "Group name (optional)",
                    hintText = "my group",
                    inputType = InputType.TYPE_CLASS_TEXT
                ),
            ),
            actionText = "Query members and manager (manager is first)",
            action = ::queryManagerAndMembersOfGivenGroup,
            progressText = "Querying...",
            successText = { it.joinToString("\n") },
        ),
        GroupAction(
            title = "Dissolve a group",
            inputConfigs = listOf(
                InputConfig(
                    labelText = "Group address",
                    hintText = "group1@group.ch",
                ),
                InputConfig(
                    labelText = "Group name (optional)",
                    hintText = "my group",
                    inputType = InputType.TYPE_CLASS_TEXT
                ),
            ),
            actionText = "Dissolve group",
            action = ::dissolveGroup,
            progressText = "Dissolving group...",
            successText = { "Group dissolved successfully" },
        ),
        GroupAction(
            title = "Invite a new member to a group",
            inputConfigs = listOf(
                InputConfig(
                    labelText = "Group address",
                    hintText = "group1@group.ch",
                ),
                InputConfig(
                    labelText = "Group name (optional)",
                    hintText = "my group",
                    inputType = InputType.TYPE_CLASS_TEXT
                ),
                InputConfig(
                    labelText = "Provide address and user name of member separated by : (user name is optional)",
                    hintText = "newmember@group.ch:member name",
                    inputType = InputType.TYPE_CLASS_TEXT
                ),
            ),
            actionText = "Invite member",
            action = ::inviteMember,
            progressText = "Inviting member...",
            successText = { "Member invited successfully" },
        ),
        GroupAction(
            title = "Remove a member from a group",
            inputConfigs = listOf(
                InputConfig(
                    labelText = "Group address",
                    hintText = "group1@group.ch",
                ),
                InputConfig(
                    labelText = "Group name (optional)",
                    hintText = "my group",
                    inputType = InputType.TYPE_CLASS_TEXT
                ),
                InputConfig(
                    labelText = "Provide address and user name of member separated by : (user name is optional)",
                    hintText = "newmember@group.ch:member name",
                    inputType = InputType.TYPE_CLASS_TEXT
                ),
            ),
            actionText = "Remove member",
            action = ::removeMember,
            progressText = "Removing member...",
            successText = { "Member removed successfully" },
        ),
        GroupAction(
            title = "Get planck rating of a group",
            inputConfigs = listOf(
                InputConfig(
                    labelText = "Group address",
                    hintText = "group1@group.ch",
                ),
                InputConfig(
                    labelText = "Group name (optional)",
                    hintText = "my group",
                    inputType = InputType.TYPE_CLASS_TEXT
                ),
            ),
            actionText = "Get group rating",
            action = ::groupRating,
            progressText = "Getting grup rating...",
            successText = { it.toString() },
        ),
    )

    private val actionList: List<String> = groupActions.map { it.title }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = GroupTestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpToolbar(true)
        setupViews()
    }

    private fun setupViews() {
        binding.groupActionSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actionList)
        binding.groupActionSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                hideAllInputs()
                showGroupAction(groupActions[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }
    }

    private fun hideAllInputs() {
        binding.input1Label.isVisible = false
        binding.input1.isVisible = false
        binding.input2Label.isVisible = false
        binding.input2.isVisible = false
        binding.input3Label.isVisible = false
        binding.input3.isVisible = false
    }

    private fun <T> showGroupAction(
        groupAction: GroupAction<T>
    ) {
        with(groupAction) {
            binding.title.text = title
            inputConfigs.forEachIndexed { index, inputConfig ->
                displayInputConfig(
                    inputConfig,
                    index
                )
            }
            binding.groupActionButton.text = actionText
            binding.groupActionButton.setOnClickListener { groupAction.action(this) }
        }
    }

    private fun displayInputConfig(inputConfig: InputConfig, index: Int) {
        when (index) {
            0 -> {
                binding.input1Label.isVisible = true
                binding.input1.isVisible = true
                binding.input1Label.text = inputConfig.labelText
                binding.input1.hint = inputConfig.hintText
                binding.input1.inputType = inputConfig.inputType
            }

            1 -> {
                binding.input2Label.isVisible = true
                binding.input2.isVisible = true
                binding.input2Label.text = inputConfig.labelText
                binding.input2.hint = inputConfig.hintText
                binding.input2.inputType = inputConfig.inputType
            }

            2 -> {
                binding.input3Label.isVisible = true
                binding.input3.isVisible = true
                binding.input3Label.text = inputConfig.labelText
                binding.input3.hint = inputConfig.hintText
                binding.input3.inputType = inputConfig.inputType
            }
        }
    }

    private data class InputConfig(
        val labelText: String,
        val hintText: String,
        val inputType: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
    )

    private data class GroupAction<T>(
        val title: String,
        val inputConfigs: List<InputConfig>,
        val actionText: String,
        val action: (GroupAction<T>) -> Unit,
        val progressText: String = "Group operation in progress...",
        val successText: (T) -> String = { "Group operation finished successfully" },
    )

    private fun queryManagerAndMembersOfGivenGroup(groupAction: GroupAction<Vector<Identity>>) {
        runGroupOperation(groupAction) {
            val groupAddress = binding.input1.text.toString()
            val groupUserName = binding.input2.text?.toString()
            withContext(PlanckDispatcher) {
                val groupIdentity = PlanckUtils.createIdentity(
                    Address(groupAddress, groupUserName),
                    this@GroupTestScreen
                )
                planckProvider.queryGroupMailManagerAndMembers(planckProvider.myselfSuspend(groupIdentity)!!)
            }
        }
    }

    private fun dissolveGroup(groupAction: GroupAction<Unit>) {
        runGroupOperation(groupAction) {
            val groupAddress = binding.input1.text.toString()
            val groupUserName = binding.input2.text?.toString()
            withContext(PlanckDispatcher) {
                val groupIdentity = PlanckUtils.createIdentity(
                    Address(groupAddress, groupUserName),
                    this@GroupTestScreen
                )
                ResultCompat.ofSuspend {
                    val updatedGroup = planckProvider.myselfSuspend(groupIdentity)!!
                    val manager = planckProvider.queryGroupMailManager(updatedGroup)
                    planckProvider.dissolveGroup(
                        updatedGroup,
                        planckProvider.myselfSuspend(manager)!!
                    )
                }
            }
        }
    }

    private fun inviteMember(groupAction: GroupAction<Unit>) {
        runGroupOperation(groupAction) {
            val groupAddress = binding.input1.text.toString()
            val groupUserName = binding.input2.text?.toString()
            val memberAddress = binding.input3.text.toString().split(":").let { parts ->
                var memberUserName: String? = null
                if (parts.isNotEmpty()) {
                    if (parts.size == 2) {
                        memberUserName = parts.last()
                    }
                    val memberAddress = parts.first()
                    Address(memberAddress, memberUserName)
                } else {
                    return@runGroupOperation ResultCompat.failure(IllegalStateException("member missing or bad formatted"))
                }
            }
            withContext(PlanckDispatcher) {
                val groupIdentity = PlanckUtils.createIdentity(
                    Address(groupAddress, groupUserName),
                    this@GroupTestScreen
                )
                val memberIdentity = PlanckUtils.createIdentity(
                    memberAddress,
                    this@GroupTestScreen
                )
                ResultCompat.ofSuspend {
                    planckProvider.inviteMemberToGroup(
                        planckProvider.myselfSuspend(groupIdentity)!!,
                        planckProvider.updateIdentity(memberIdentity)
                    )
                }
            }
        }
    }

    private fun removeMember(groupAction: GroupAction<Unit>) {
        runGroupOperation(groupAction) {
            val groupAddress = binding.input1.text.toString()
            val groupUserName = binding.input2.text?.toString()
            val memberAddress = binding.input3.text.toString().split(":").let { parts ->
                var memberUserName: String? = null
                if (parts.isNotEmpty()) {
                    if (parts.size == 2) {
                        memberUserName = parts.last()
                    }
                    val memberAddress = parts.first()
                    Address(memberAddress, memberUserName)
                } else {
                    return@runGroupOperation ResultCompat.failure(IllegalStateException("member missing or bad formatted"))
                }
            }
            withContext(PlanckDispatcher) {
                val groupIdentity = PlanckUtils.createIdentity(
                    Address(groupAddress, groupUserName),
                    this@GroupTestScreen
                )
                val memberIdentity = PlanckUtils.createIdentity(
                    memberAddress,
                    this@GroupTestScreen
                )
                ResultCompat.ofSuspend {
                    planckProvider.removeMemberFromGroup(
                        planckProvider.myselfSuspend(groupIdentity)!!,
                        planckProvider.updateIdentity(memberIdentity)
                    )
                }
            }
        }
    }

    private fun createGroupFromUserInput(groupAction: GroupAction<Unit>) {
        runGroupOperation(groupAction) {
            val groupAddress = binding.input1.text.toString()
            val groupUserName = binding.input2.text?.toString()
            val memberAddresses = binding.input3.text.toString()
            withContext(PlanckDispatcher) {
                val account = preferences.accounts.first()
                val manager = PlanckUtils.createIdentity(
                    Address(account.email, account.name),
                    this@GroupTestScreen
                )
                val groupIdentity = PlanckUtils.createIdentity(
                    Address(groupAddress, groupUserName),
                    this@GroupTestScreen
                )
                val memberIdentitiesList = memberAddresses.split(",").mapNotNull { memberText ->
                    val parts = memberText.split(":")
                    var memberUserName: String? = null
                    if (parts.isNotEmpty()) {
                        if (parts.size == 2) {
                            memberUserName = parts.last()
                        }
                        val memberAddress = parts.first()
                        PlanckUtils.createIdentity(
                            Address(memberAddress, memberUserName),
                            this@GroupTestScreen
                        )
                    } else {
                        null
                    }
                }

                ResultCompat.ofSuspend {
                    val memberIdentities =
                        if (memberIdentitiesList.isNotEmpty())
                            Vector(memberIdentitiesList.map { planckProvider.updateIdentity(it) })
                        else Vector()
                    planckProvider.createGroup(groupIdentity, planckProvider.myselfSuspend(manager)!!, memberIdentities)
                }
            }
        }
    }

    private fun groupRating(groupAction: GroupAction<Rating>) {
        runGroupOperation(groupAction) {
            val groupAddress = binding.input1.text.toString()
            val groupUserName = binding.input2.text?.toString()
            withContext(PlanckDispatcher) {
                val groupIdentity = PlanckUtils.createIdentity(
                    Address(groupAddress, groupUserName),
                    this@GroupTestScreen
                )
                ResultCompat.ofSuspend {
                    val manager = planckProvider.queryGroupMailManager(groupIdentity)
                    planckProvider.groupRating(planckProvider.myselfSuspend(groupIdentity)!!, manager)
                }
            }
        }
    }

    private fun <T> runGroupOperation(
        groupAction: GroupAction<T>,
        onSuccess: (T) -> Unit = { binding.groupActionFeedback.text = groupAction.successText(it) },
        operation: suspend () -> ResultCompat<T>,
    ) {
        uiScope.launch {
            binding.groupActionFeedback.text = groupAction.progressText
            operation().onFailure {
                Timber.e(it, "error in group operation")
                binding.groupActionFeedback.text = it.message
                displayError(it)
            }.onSuccess {
                onSuccess(it)
            }
        }
    }

    private fun displayError(e: Throwable) {
        FeedbackTools.showLongFeedback(binding.root, e.message)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
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