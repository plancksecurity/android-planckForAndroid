package security.planck.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.fsck.k9.Preferences
import com.fsck.k9.databinding.GroupTestBinding
import com.fsck.k9.mail.Address
import com.fsck.k9.planck.PlanckActivity
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import com.fsck.k9.planck.ui.tools.FeedbackTools
import foundation.pEp.jniadapter.Group
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

    private var group = Group()

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
            withContext(PlanckDispatcher) {
                group.print()
                val account = preferences.accounts.first()
                val manager = PlanckUtils.createIdentity(Address(account.email, account.name), this@GroupTestScreen)
                val groupIdentity = PlanckUtils.createIdentity(Address("juanito.valderrama@rama.ch", "juanitoeh"), this@GroupTestScreen)
                kotlin.runCatching { group = planckProvider.createGroup(groupIdentity, manager, Vector()) }
                    .onFailure { displayError(it) }
                group.print()
            }
            binding.emptyGroupCreationFeedback.text = "Empty group created:\n"+group.getDataString()
        }
    }

    private fun createGroupFromUserInput() {
        uiScope.launch {
            val groupAddress = binding.groupAddress.text.toString()
            val memberAddresses = binding.groupMemberAddresses.text.toString()
            withContext(PlanckDispatcher) {
                group.print()
                val account = preferences.accounts.first()
                val manager = PlanckUtils.createIdentity(Address(account.email, account.name), this@GroupTestScreen)
                val groupIdentity = PlanckUtils.createIdentity(Address(groupAddress), this@GroupTestScreen)
                val memberIdentities = Vector(memberAddresses.split(" ").map { PlanckUtils.createIdentity(Address(it), this@GroupTestScreen) })
                kotlin.runCatching { group = planckProvider.createGroup(groupIdentity, manager, memberIdentities) }
                    .onFailure { displayError(it) }
                group.print()
            }
            binding.groupCreationFeedback.text = "Group created:\n"+group.getDataString()
        }
    }

    private fun displayError(e: Throwable) {
        FeedbackTools.showLongFeedback(binding.root, e.message)
    }

    private fun Group.print() {
        Timber.e(getDataString())
    }

    private fun Group.getDataString(): String =
        """
            GROUP:
            Group identity: ${this.group_identity}
            Group manager: ${this.manager}
            Members: ${this.members}
            Active: ${this.active}
        """.trimIndent()


    override fun inject() {
        planckComponent.inject(this)
    }

    companion object {
        @JvmStatic
        fun start(context: Activity) {
            context.startActivity(Intent(context, GroupTestScreen::class.java))
        }
    }
}