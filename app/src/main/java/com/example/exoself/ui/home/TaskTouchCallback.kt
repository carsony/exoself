package com.example.exoself.ui.home

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class TaskTouchCallback(var adapter: TaskListAdapter, dragDirs: Int, swipeDirs: Int) :
    ItemTouchHelper.SimpleCallback(dragDirs, swipeDirs) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.swapItems(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        adapter.deleteItem(viewHolder.adapterPosition)
    }
}