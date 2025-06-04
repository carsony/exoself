package com.example.exoself.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.exoself.databinding.FragmentTasksBinding
import com.example.exoself.domain.Task
import com.google.android.material.snackbar.Snackbar

class TasksFragment : Fragment() {

    companion object {
        private const val ARG_TAB_NUMBER = "tab_number"

        @JvmStatic
        fun newInstance(tabNumber: Int) = TasksFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_TAB_NUMBER, tabNumber)
            }
        }
    }

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private val tasksViewModel: TasksViewModel by activityViewModels()
    private var tabId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tabId = arguments?.getInt(ARG_TAB_NUMBER) ?: 0

        val rvAdapter =
            TaskListAdapter(
                tabId,
                ::onTaskEdit,
                ::onTaskReorder,
                ::onTaskDelete,
                ::onTaskComplete,
                ::onTaskSave,
                ::onSavedTaskMove
            )
        val rv = binding.taskListRecyclerView.apply {
            adapter = rvAdapter
            layoutManager = LinearLayoutManager(context)
        }

        val itemTouchHelper = ItemTouchHelper(
            TaskTouchCallback(
                rvAdapter, ItemTouchHelper.UP.or(ItemTouchHelper.DOWN), ItemTouchHelper.LEFT
            )
        ).also { it.attachToRecyclerView(rv) }

        val dividerItemDecoration = DividerItemDecoration(rv.context, LinearLayoutManager.VERTICAL)
        rv.addItemDecoration(dividerItemDecoration)

        when (tabId) {
            TabPageIds.CURRENT.ordinal -> {
                tasksViewModel.currentTasks.observe(viewLifecycleOwner) {
                    rvAdapter.submitList(it?.filter { task -> !task.completed && task.current })
                }
                initAddTaskFab(tabId)
            }
            TabPageIds.SAVED.ordinal -> {
                tasksViewModel.savedTasks.observe(viewLifecycleOwner) {
                    rvAdapter.submitList(it)
                }
            }
            TabPageIds.SCHEDULED.ordinal -> {
                tasksViewModel.scheduledTasks.observe(viewLifecycleOwner) {
                    itemTouchHelper.attachToRecyclerView(null)
                    rvAdapter.submitList(it)
                }
            }
            TabPageIds.COMPLETED.ordinal -> {
                tasksViewModel.currentTasks.observe(viewLifecycleOwner) {
                    rvAdapter.submitList(it?.filter { task -> task.completed && !task.current })
                }
            }
        }

        binding.startTasksFab.setOnClickListener {
            val direction = HomeFragmentDirections.actionNavHomeToNavTask()
            findNavController().navigate(direction)
        }
    }

    private fun initAddTaskFab(tabId: Int) {
        binding.addTaskFab.apply {
            setOnClickListener {
                val direction = HomeFragmentDirections.actionNavHomeToNavTaskEdit(null, tabId)
                findNavController().navigate(direction)
            }
            visibility = View.VISIBLE
        }
    }

    private fun onTaskEdit(task: Task, tabId: Int) {
        findNavController().navigate(
            HomeFragmentDirections.actionNavHomeToNavTaskEdit(task.docId, tabId)
        )
    }

    private fun onTaskSave(first: Task, tabId: Int) {
        tasksViewModel.saveTask(first, tabId)
    }

    private fun onSavedTaskMove(first: Task) {
        Snackbar.make(binding.root, "Added task to current", Snackbar.LENGTH_SHORT).show()
        tasksViewModel.addSavedTaskToCurrent(first)
    }

    private fun onTaskReorder(first: Task, second: Task, tabId: Int) {
        tasksViewModel.reorderTaskList(first, second, tabId)
    }

    private fun onTaskDelete(task: Task, tabId: Int) {
        Snackbar.make(binding.root, "Deleted task", Snackbar.LENGTH_SHORT).show()
        tasksViewModel.deleteTask(task, tabId)
    }

    private fun onTaskComplete(task: Task, tabId: Int) {
        tasksViewModel.completeTask(task, tabId)
    }

    override fun onResume() {
        tasksViewModel.startScheduledCheckTimer()
        super.onResume()
    }

    override fun onPause() {
        tasksViewModel.stopScheduledCheckTimer()
        super.onPause()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}