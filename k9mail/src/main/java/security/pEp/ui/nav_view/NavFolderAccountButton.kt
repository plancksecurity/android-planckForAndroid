package security.pEp.ui.nav_view

import android.content.Context
import android.util.AttributeSet
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.AppCompatImageView
import com.fsck.k9.R

class NavFolderAccountButton(context: Context?, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {

    private var clockwise: Animation? = AnimationUtils.loadAnimation(context,R.anim.rotate_clockwise);
    private var anticlockwise: Animation? = AnimationUtils.loadAnimation(context,R.anim.rotate_anti_clockwise);
    var showingFolders = false

    fun showFolders() {
        startAnimation(clockwise)
        showingFolders = true
    }
    fun showAccounts() {
        startAnimation(anticlockwise)
        showingFolders = false
    }
}