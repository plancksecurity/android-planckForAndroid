package security.planck.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.fsck.k9.Preferences
import com.fsck.k9.R
import com.fsck.k9.mail.Address
import com.fsck.k9.planck.PlanckActivity
import com.fsck.k9.planck.PlanckProvider
import com.fsck.k9.planck.PlanckUtils
import com.fsck.k9.planck.infrastructure.threading.PlanckDispatcher
import foundation.pEp.jniadapter.Group
import foundation.pEp.jniadapter.Member
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Vector
import javax.inject.Inject

class GroupTestScreen: PlanckActivity() {

    private lateinit var createGroup: Button
    @Inject
    lateinit var planckProvider: PlanckProvider

    @Inject
    lateinit var preferences: Preferences

    private var group = Group()

    private val uiScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_test)
        setUpToolbar(true)
        createGroup = findViewById(R.id.create_group_button)
        createGroup.setOnClickListener {
            createGroup()
        }
    }

    private fun createGroup() {
        uiScope.launch {
            withContext(PlanckDispatcher) {
                group.print()
                val account = preferences.accounts.first()
                val identity = PlanckUtils.createIdentity(Address(account.email, account.name), this@GroupTestScreen)
                group = planckProvider.createGroup(identity, identity, Vector(listOf(identity)))
                group.print()
            }
        }
    }

    private fun Group.print() {
        Timber.e(
            """
                GROUP:
                Group identity: ${this.group_identity}
                Group manager: ${this.manager}
                Members: ${this.members}
                Active: ${this.active}
            """.trimIndent()
        )
    }


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