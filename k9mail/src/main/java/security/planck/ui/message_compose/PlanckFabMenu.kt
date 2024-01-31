package security.planck.ui.message_compose

import android.content.Context
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.fsck.k9.R
import com.fsck.k9.databinding.FabMenuLayoutBinding
import com.fsck.k9.planck.ui.listeners.OnMessageOptionsListener


class PlanckFabMenu : ConstraintLayout, PlanckFabMenuView {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val slideOutReply = AnimationUtils.loadAnimation(context, R.anim.slide_out_reply)
    private val slideOutReplyAll = AnimationUtils.loadAnimation(context, R.anim.slide_out_reply_all)
    private val slideOutForward = AnimationUtils.loadAnimation(context, R.anim.slide_out_forward)
    private val slideInReply = AnimationUtils.loadAnimation(context, R.anim.slide_in_reply)
    private val slideInReplyAll = AnimationUtils.loadAnimation(context, R.anim.slide_in_reply_all)
    private val slideInForward = AnimationUtils.loadAnimation(context, R.anim.slide_out_forward)

    val presenter: PlanckFabMenuPresenter = PlanckFabMenuPresenter(this)

    private lateinit var binding: FabMenuLayoutBinding

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        presenter.init()
    }

    public override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FabMenuLayoutBinding.bind(this)
        binding.openCloseButton.setOnLongClickListener {
            presenter.onLongClicked()
            true
        }
    }

    fun setClickListeners(listener: OnMessageOptionsListener) {
        presenter.listener = listener

        binding.openCloseButton.setOnClickListener { presenter.onMainActionClicked() }
        binding.fabForward.setOnClickListener { presenter.onForwardClicked() }
        binding.fabReplyAll.setOnClickListener { presenter.onReplyAllClicked() }
        binding.fabReply.setOnClickListener { presenter.onReplyClicked() }
    }

    override fun openMenu() {
        binding.fabForward.startAnimation(slideOutForward)
        binding.fabReplyAll.startAnimation(slideOutReplyAll)
        binding.fabReply.startAnimation(slideOutReply)
        binding.fabReply.show()
        binding.fabReplyAll.show()
        binding.fabForward.show()
        animateOpenCloseFab(R.drawable.reply_to_cross_animated)
    }

    override fun closeMenu() {
        binding.fabForward.startAnimation(slideInForward)
        binding.fabReplyAll.startAnimation(slideInReplyAll)
        binding.fabReply.startAnimation(slideInReply)
        binding.fabReply.hide()
        binding.fabReplyAll.hide()
        binding.fabForward.hide()
        animateOpenCloseFab(R.drawable.cross_to_reply_animated)
    }

    private fun animateOpenCloseFab(@DrawableRes drawable: Int) {
        binding.openCloseButton.setImageResource(drawable)
        if (binding.openCloseButton.drawable is Animatable) {
            (binding.openCloseButton.drawable as Animatable).start()
        }
    }

    override fun showInitialState() {
        binding.openCloseButton.post {
             binding.openCloseButton.setImageResource(R.drawable.ic_reply_planck)
        }
    }

}

