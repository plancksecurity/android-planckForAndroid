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

    private val slideUpReply = AnimationUtils.loadAnimation(context, R.anim.slide_up_reply)
    private val slideUpReplyAll = AnimationUtils.loadAnimation(context, R.anim.slide_up_reply_all)
    private val slideUpForward = AnimationUtils.loadAnimation(context, R.anim.slide_up_forward)
    private val slideDownReply = AnimationUtils.loadAnimation(context, R.anim.slide_down_reply)
    private val slideDownReplyAll = AnimationUtils.loadAnimation(context, R.anim.slide_down_reply_all)
    private val slideDownForward = AnimationUtils.loadAnimation(context, R.anim.slide_down_forward)

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
        binding.fabForward.startAnimation(slideUpForward)
        binding.fabReplyAll.startAnimation(slideUpReplyAll)
        binding.fabReply.startAnimation(slideUpReply)
        binding.fabReply.show()
        binding.fabReplyAll.show()
        binding.fabForward.show()
        animateOpenCloseFab(R.drawable.reply_to_cross_animated)
    }

    override fun closeMenu() {
        binding.fabForward.startAnimation(slideDownForward)
        binding.fabReplyAll.startAnimation(slideDownReplyAll)
        binding.fabReply.startAnimation(slideDownReply)
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

