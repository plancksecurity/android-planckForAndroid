package com.fsck.k9.pEp.ui.renderers

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.fsck.k9.R
import com.fsck.k9.activity.FolderInfoHolder
import com.fsck.k9.pEp.models.FolderModel
import security.pEp.animatedlevellist.defaultimplementations.renderer.DefaultLevelItemRenderer
import security.pEp.animatedlevellist.model.LevelListItem
import security.pEp.animatedlevellist.util.Constants

class FolderRenderer : DefaultLevelItemRenderer<FolderModel>() {

    private lateinit var folderName: TextView
    private lateinit var folderNewMessages: TextView
    private lateinit var showChildrenButton: ImageView
    private lateinit var showChildrenClicker: View

    override fun inflate(inflater: LayoutInflater, parent: ViewGroup): View {
        val inflatedView = inflater.inflate(R.layout.folder_navigation_list_item, parent, false)
        folderName = inflatedView.findViewById(R.id.folder_name)
        folderNewMessages = inflatedView.findViewById(R.id.folder_new_messages)
        showChildrenButton = inflatedView.findViewById(R.id.showchildrenbutton)
        showChildrenClicker = inflatedView.findViewById(R.id.showchildrenclicker)
        return inflatedView
    }

    override fun bind() {
        differenciateParentOrChildDisplay()
        differenciateUnfoldedCondition()
        val folderModel = content.item
        folderName.text =
            FolderInfoHolder.getDisplayName(context, folderModel.account, content.levelListItemName)
        renderUnreadMessages(folderModel.unreadCount)
    }

    override fun indentByDepth(view: View, item: LevelListItem<FolderModel>) {
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, indent, view.context.resources.displayMetrics)
            .toInt()
        val params : RelativeLayout.LayoutParams = showChildrenButton.layoutParams as RelativeLayout.LayoutParams
        params.leftMargin = item.depth * px
        showChildrenButton.layoutParams = params
    }

    override fun differenciateParentOrChildDisplay() {
        showChildrenButton.visibility = if(content.children.isEmpty()) View.INVISIBLE else View.VISIBLE
        showChildrenClicker.visibility = if(content.children.isEmpty()) View.INVISIBLE else View.VISIBLE
    }

    private fun differenciateUnfoldedCondition() {
        showChildrenButton.rotation =
            if(!content.areChildrenUnfolded) Constants.ARROW_ORIGINAL_ROTATION
            else Constants.ARROW_FINAL_ROTATION
    }

    private fun renderUnreadMessages(unreadMessageCount: Int) {
        when {
            unreadMessageCount > 0 -> {
                folderNewMessages.visibility = View.VISIBLE
                folderNewMessages.text = unreadMessageCount.toString()
            }
            else -> folderNewMessages.visibility = View.GONE
        }
    }
}
