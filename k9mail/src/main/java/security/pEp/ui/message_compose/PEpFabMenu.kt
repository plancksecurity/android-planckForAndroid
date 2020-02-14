package security.pEp.ui.message_compose

import android.content.Context
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.fsck.k9.R
import com.fsck.k9.pEp.ui.listeners.OnMessageOptionsListener
import kotlinx.android.synthetic.main.fab_menu_layout.view.*


class PEpFabMenu(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs), PEpFabMenuView {

    private val fabOpenAnimation = AnimationUtils.loadAnimation(context, R.anim.fab_open)
    private val fabCloseAnimation = AnimationUtils.loadAnimation(context, R.anim.fab_close)
    private val slideUpReply = AnimationUtils.loadAnimation(context, R.anim.slide_up_reply)
    private val slideUpReplyAll = AnimationUtils.loadAnimation(context, R.anim.slide_up_reply_all)
    private val slideUpForward = AnimationUtils.loadAnimation(context, R.anim.slide_up_forward)
    private val slideDownReply = AnimationUtils.loadAnimation(context, R.anim.slide_down_reply)
    private val slideDownReplyAll = AnimationUtils.loadAnimation(context, R.anim.slide_down_reply_all)
    private val slideDownForward = AnimationUtils.loadAnimation(context, R.anim.slide_down_forward)

    lateinit var presenter: PEpFabMenuPresenter

    override fun onAttachedToWindow() {
        presenter = PEpFabMenuPresenter(this)
        super.onAttachedToWindow()
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

        openCloseButton.setOnClickListener { presenter.onMainActionClicked() }
        fabForward.setOnClickListener { presenter.onForwardClicked() }
        fabReplyAll.setOnClickListener { presenter.onReplyAllClicked() }
        fabReply.setOnClickListener { presenter.onReplyClicked() }
    }

    override fun openMenu() {
        textAnimation(fabOpenAnimation)
        fabForward.startAnimation(slideUpForward)
        fabReplyAll.startAnimation(slideUpReplyAll)
        fabReply.startAnimation(slideUpReply)
        animateOpenCloseFab(R.drawable.reply_to_cross_animated)
        setTextHintsVisibility(VISIBLE)
    }

    override fun closeMenu() {
        textAnimation(fabCloseAnimation)
        fabForward.startAnimation(slideDownForward)
        fabReplyAll.startAnimation(slideDownReplyAll)
        fabReply.startAnimation(slideDownReply)
        animateOpenCloseFab(R.drawable.cross_to_reply_animated)
        setTextHintsVisibility(GONE)
    }

    private fun animateOpenCloseFab(@DrawableRes drawable: Int) {
        openCloseButton.setImageResource(drawable)
        if (openCloseButton.drawable is Animatable) {
            (openCloseButton.drawable as Animatable).start()
        }
    }

    private fun textAnimation(animation: Animation) {
        textviewForward.startAnimation(animation)
        textviewReplyAll.startAnimation(animation)
        textviewReply.startAnimation(animation)
    }

    private fun setTextHintsVisibility(visible: Int) {
        textviewForward.visibility = visible
        textviewReplyAll.visibility = visible
        textviewReply.visibility = visible
    }

    override fun showInitialState() {
        openCloseButton.post {
            setTextHintsVisibility(GONE)
            openCloseButton.setImageResource(R.drawable.ic_reply_green)
        }
    }

}

