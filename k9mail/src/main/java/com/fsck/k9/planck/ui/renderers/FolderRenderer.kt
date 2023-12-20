package com.fsck.k9.planck.ui.renderers

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import com.fsck.k9.R
import com.fsck.k9.activity.FolderInfoHolder
import com.fsck.k9.planck.models.FolderModel
import security.planck.foldable.folders.defaults.renderer.DefaultLevelItemRenderer
import security.planck.foldable.folders.model.LevelListItem
import security.planck.foldable.folders.util.Constants

class FolderRenderer : DefaultLevelItemRenderer<FolderModel>() {

    private lateinit var folderNewMessages: TextView
    private lateinit var folderIcon: View

    override fun inflate(inflater: LayoutInflater, parent: ViewGroup): View {
        val inflatedView = inflater.inflate(R.layout.folder_navigation_list_item, parent, false)
        folderName = inflatedView.findViewById(R.id.folder_name)
        folderNewMessages = inflatedView.findViewById(R.id.folder_new_messages)
        showChildrenButton = inflatedView.findViewById(R.id.showchildrenbutton)
        showChildrenClicker = inflatedView.findViewById(R.id.showchildrenclicker)
        folderIcon = inflatedView.findViewById(R.id.folder_icon)
        return inflatedView
    }

    override fun bind() {
        differentiateParentOrChildDisplay()
        differentiateUnfoldedCondition()
        val folderModel = content.item
        folderName.text =
            FolderInfoHolder.getDisplayName(
                folderModel.account,
                content.levelListItemName,
                content.fullName
            )

        renderUnreadMessages(folderModel.unreadCount)
    }

    override fun indentByDepth(view: View, item: LevelListItem<FolderModel>) {
        setIndentForView(folderIcon, 0)
        setIndentForView(showChildrenButton, 0)

        val indentView: View = if(content.children.isEmpty()) folderIcon else showChildrenButton
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, indent, view.context.resources.displayMetrics)
            .toInt()
        setIndentForView(indentView, item.depth * px)
    }

    private fun setIndentForView(view: View, indent: Int) {
        val params : RelativeLayout.LayoutParams = view.layoutParams as RelativeLayout.LayoutParams
        params.leftMargin = indent
        view.layoutParams = params
    }

    override fun differentiateParentOrChildDisplay() {
        showChildrenButton.visibility = if(content.children.isEmpty()) {
            if(this.isFlatList && content.depth == 0) {
                View.GONE
            }
            else {
                View.INVISIBLE
            }
        } else {
            View.VISIBLE
        }
        showChildrenClicker.visibility = if(content.children.isEmpty()) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }
    }

    override fun differentiateUnfoldedCondition() {
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
            else -> {
                folderNewMessages.text = ""
                folderNewMessages.visibility = View.INVISIBLE
            }
        }
    }
}
