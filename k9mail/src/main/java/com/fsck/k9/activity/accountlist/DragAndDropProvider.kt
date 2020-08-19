package com.fsck.k9.activity.accountlist

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.fsck.k9.Preferences
import com.fsck.k9.R
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class DragAndDropProvider @Inject constructor() : ItemTouchHelper.Callback() {

    private lateinit var listener: ItemReleasedListener
    private lateinit var list: RecyclerView
    private lateinit var data: List<Any>
    private lateinit var touchHelper: ItemTouchHelper

    @Inject
    lateinit var preferences: Preferences

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        onItemMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        if (actionState != ItemTouchHelper.ACTION_STATE_IDLE && viewHolder != null) {
            onItemSelected(viewHolder)
        }
        super.onSelectedChanged(viewHolder, actionState)
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        onItemReleased(viewHolder)

    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        //NOOP
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    fun initialize(
        list: RecyclerView,
        data: List<Any>
    , listener: ItemReleasedListener) {
        this.list = list
        this.data = data
        this.listener = listener
        initTouchHelper()
    }

    private fun initTouchHelper() {
        touchHelper = ItemTouchHelper(this)
        touchHelper.attachToRecyclerView(list)
    }

    private fun onItemReleased(viewHolder: RecyclerView.ViewHolder) {
        when (viewHolder) {
            is AccountViewHolder -> viewHolder.setAlpha(1f)
        }
        list.adapter?.notifyDataSetChanged()
        listener.itemReleased()
    }

    private fun onItemSelected(viewHolder: RecyclerView.ViewHolder) {
        when (viewHolder) {
            is AccountViewHolder -> viewHolder.setAlpha(0.7f)
        }
    }

    private fun onItemMoved(fromPosition: Int, toPosition: Int) {
        when {
            fromPosition < toPosition ->
                (fromPosition until toPosition).forEach { i -> Collections.swap(data, i, i + 1) }
            else ->
                (fromPosition downTo toPosition + 1).forEach { i ->
                    Collections.swap(
                        data,
                        i,
                        i - 1
                    )
                }
        }

        list.adapter?.notifyItemMoved(fromPosition, toPosition)
    }

    fun onPause() {
        touchHelper.attachToRecyclerView(null)
    }

}

interface ItemReleasedListener {

    fun itemReleased()
}