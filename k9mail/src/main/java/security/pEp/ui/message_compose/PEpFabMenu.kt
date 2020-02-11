package security.pEp.ui.message_compose

import android.content.Context
import android.graphics.drawable.Animatable
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import com.fsck.k9.R
import com.fsck.k9.pEp.ui.infrastructure.MessageAction
import com.fsck.k9.pEp.ui.listeners.OnMessageOptionsListener
import kotlinx.android.synthetic.main.fab_menu_layout.view.*


class PEpFabMenu(context: Context?, attrs: AttributeSet?) : ConstraintLayout(context, attrs) {

    private val fabOpenAnimation = AnimationUtils.loadAnimation(context, R.anim.fab_open)
    private val fabCloseAnimation = AnimationUtils.loadAnimation(context, R.anim.fab_close)
    private val slideUpReply = AnimationUtils.loadAnimation(context, R.anim.slide_up_reply)
    private val slideUpReplyAll = AnimationUtils.loadAnimation(context, R.anim.slide_up_reply_all)
    private val slideUpForward = AnimationUtils.loadAnimation(context, R.anim.slide_up_forward)
    private val slideDownReply = AnimationUtils.loadAnimation(context, R.anim.slide_down_reply)
    private val slideDownReplyAll = AnimationUtils.loadAnimation(context, R.anim.slide_down_reply_all)
    private val slideDownForward = AnimationUtils.loadAnimation(context, R.anim.slide_down_forward)
    var open = false

    override fun onAttachedToWindow() {
        init()
        super.onAttachedToWindow()
    }

    public override fun onFinishInflate() {
        super.onFinishInflate()
        openCloseButton.setOnLongClickListener {
            if (open) closeMenu()
            else openMenu()
            true
        }
    }

    fun setClickListeners(listener: OnMessageOptionsListener) {
        openCloseButton.setOnClickListener { if (open) closeMenu() else listener.OnMessageOptionsListener(MessageAction.REPLY) }
        fabForward.setOnClickListener { listener.OnMessageOptionsListener(MessageAction.FORWARD) }
        fabReplyAll.setOnClickListener { listener.OnMessageOptionsListener(MessageAction.REPLY_ALL) }
        fabReply.setOnClickListener { listener.OnMessageOptionsListener(MessageAction.REPLY) }
    }

    private fun openMenu() {
        textAnimation(fabOpenAnimation)

        fabForward.startAnimation(slideUpForward)
        fabReplyAll.startAnimation(slideUpReplyAll)
        fabReply.startAnimation(slideUpReply)

        animateOpenCloseFab()
        setNewVisibility(View.VISIBLE)
        open = true
    }

    private fun closeMenu() {
        textAnimation(fabCloseAnimation)

        fabForward.startAnimation(slideDownForward)
        fabReplyAll.startAnimation(slideDownReplyAll)
        fabReply.startAnimation(slideDownReply)

        animateOpenCloseFab()
        setNewVisibility(GONE)
        open = false
    }

    private fun animateOpenCloseFab() {
        if (open)
            openCloseButton.setImageResource(R.drawable.cross_to_reply_animated)
        else
            openCloseButton.setImageResource(R.drawable.reply_to_cross_animated)
        if (openCloseButton.drawable is Animatable) {
            (openCloseButton.drawable as Animatable).start()
        }
    }


    private fun textAnimation(animation: Animation) {
        textviewForward.startAnimation(animation)
        textviewReplyAll.startAnimation(animation)
        textviewReply.startAnimation(animation)
    }

    private fun setNewVisibility(visible: Int) {
        textviewForward.visibility = visible
        textviewReplyAll.visibility = visible
        textviewReply.visibility = visible
    }

    fun init() {
        openCloseButton.post {
            setNewVisibility(GONE)
            openCloseButton.setImageResource(R.drawable.ic_reply_green)
        }

    }

}