package security.planck.group

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import com.fsck.k9.R
import com.fsck.k9.planck.PlanckActivity
import com.fsck.k9.planck.PlanckProvider
import javax.inject.Inject

class GroupTestScreen: PlanckActivity() {

    private lateinit var createGroup: Button
    @Inject
    lateinit var planckProvider: PlanckProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.group_test)
        createGroup = findViewById(R.id.create_group_button)
        createGroup.setOnClickListener {
            //planckProvider.createGroup()
        }
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