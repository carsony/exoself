package com.example.exoself.ui.home

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.exoself.R
import com.example.exoself.databinding.TaskItemBinding
import com.example.exoself.domain.Task
import java.text.SimpleDateFormat
import java.util.*

class TaskListAdapter(
    private val tabId: Int,
    private var onTaskEdit: (Task, Int) -> Unit,
    private var onTaskReorder: (Task, Task, Int) -> Unit,
    private var onTaskDelete: (Task, Int) -> Unit,
    private var onTaskComplete: (Task, Int) -> Unit,
    private var onTaskSave: (Task, Int) -> Unit,
    private var onSavedTaskMove: (Task) -> Unit
) : ListAdapter<Task, TaskListAdapter.TaskListViewHolder>(TaskDiffCallback()) {

    inner class TaskListViewHolder(val binding: TaskItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onTaskEdit(currentList[adapterPosition], tabId)
            }
            binding.completeBtn.setOnClickListener {
                onTaskComplete(currentList[adapterPosition], tabId)
            }
            binding.saveBtn.setOnClickListener {
                onTaskSave(currentList[adapterPosition], tabId)
            }
            binding.addToCurrentBtn.setOnClickListener {
                onSavedTaskMove(currentList[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskListViewHolder {
        return TaskListViewHolder(
            TaskItemBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: TaskListViewHolder, position: Int) {
        val current = currentList[position]
        holder.binding.apply {
            taskDesc.text = current.description
            tags.text = current.tags.joinToString { it }
        }
        current.color?.let { holder.binding.root.setBackgroundColor(it) }
        when (tabId) {
            TabPageIds.SCHEDULED.ordinal -> {
                holder.binding.completeBtn.visibility = View.GONE
                holder.binding.saveBtn.visibility = View.GONE
                holder.binding.scheduledDateTv.visibility = View.VISIBLE
                holder.binding.scheduledDateTv.text = formatDate(current.scheduledTime?.toDate())
            }
            TabPageIds.SAVED.ordinal -> {
                holder.binding.completeBtn.visibility = View.GONE
                holder.binding.saveBtn.visibility = View.GONE
                holder.binding.addToCurrentBtn.visibility = View.VISIBLE
            }
            TabPageIds.COMPLETED.ordinal -> {
                holder.binding.completeBtn.setBackgroundResource(R.drawable.ic_baseline_check_circle_24)
            }
        }
        if (current.saved) {
            holder.binding.saveBtn.setBackgroundResource(R.drawable.ic_baseline_star_24)
        } else {
            holder.binding.saveBtn.setBackgroundResource(R.drawable.ic_baseline_star_border_24)
        }
    }

    fun swapItems(fromPosition: Int, toPosition: Int) {
        val modified = currentList.toMutableList()
        Collections.swap(modified, fromPosition, toPosition)
        onTaskReorder(currentList[fromPosition], currentList[toPosition], tabId)
        this.submitList(modified)
    }

    fun deleteItem(position: Int) {
        val modified = currentList.toMutableList()
        modified.removeAt(position)
        onTaskDelete(currentList[position], tabId)
        this.submitList(modified)
    }

    @SuppressLint("SimpleDateFormat")
    fun formatDate(date: Date?): String =
        date?.let { SimpleDateFormat("MM/dd/yyyy").format(it) } ?: ""
}

class TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem.docId == newItem.docId
    }

    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
        return oldItem == newItem
    }
}
