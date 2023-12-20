package security.planck.ui.message_compose

import android.content.Context
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.fsck.k9.R
import com.fsck.k9.planck.ui.listeners.OnMessageOptionsListener
import kotlinx.android.synthetic.main.fab_menu_layout.view.fabForward
import kotlinx.android.synthetic.main.fab_menu_layout.view.fabReply
import kotlinx.android.synthetic.main.fab_menu_layout.view.fabReplyAll
import kotlinx.android.synthetic.main.fab_menu_layout.view.openCloseButton


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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        presenter.init()
    }

    public override fun onFinishInflate() {
        super.onFinishInflate()
        openCloseButton.setOnLongClickListener {
            presenter.onLongClicked()
            true
        }
    }

    fun setClickListeners(listener: OnMessageOptionsListener) {
        presenter.listener = listener

        openCloseButton?.setOnClickListener { presenter.onMainActionClicked() }
        fabForward?.setOnClickListener { presenter.onForwardClicked() }
        fabReplyAll?.setOnClickListener { presenter.onReplyAllClicked() }
        fabReply?.setOnClickListener { presenter.onReplyClicked() }
    }

    override fun openMenu() {
        fabForward.startAnimation(slideUpForward)
        fabReplyAll.startAnimation(slideUpReplyAll)
        fabReply.startAnimation(slideUpReply)
        fabReply.show()
        fabReplyAll.show()
        fabForward.show()
        animateOpenCloseFab(R.drawable.reply_to_cross_animated)
    }

    override fun closeMenu() {
        fabForward.startAnimation(slideDownForward)
        fabReplyAll.startAnimation(slideDownReplyAll)
        fabReply.startAnimation(slideDownReply)
        fabReply.hide()
        fabReplyAll.hide()
        fabForward.hide()
        animateOpenCloseFab(R.drawable.cross_to_reply_animated)
    }

    private fun animateOpenCloseFab(@DrawableRes drawable: Int) {
        openCloseButton.setImageResource(drawable)
        if (openCloseButton.drawable is Animatable) {
            (openCloseButton.drawable as Animatable).start()
        }
    }

    override fun showInitialState() {
        openCloseButton?.post {
            openCloseButton.setImageResource(R.drawable.ic_reply_planck)
        }
    }

}

