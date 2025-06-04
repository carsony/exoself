package com.example.exoself.ui.taskedit

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.exoself.R
import com.example.exoself.databinding.FragmentTaskEditBinding
import com.example.exoself.domain.Task
import com.google.android.material.chip.Chip
import dev.sasikanth.colorsheet.ColorSheet
import java.text.SimpleDateFormat
import java.util.*

class TaskEditFragment : Fragment() {

    enum class EditingState {
        NEW_TASK, EXISTING_TASK
    }

    private var _binding: FragmentTaskEditBinding? = null
    private val binding get() = _binding!!
    private val taskEditViewModel: TaskEditViewModel by activityViewModels()
    private var selectedColor: Int = ColorSheet.NO_COLOR

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args: TaskEditFragmentArgs by navArgs()
        val editingState = if (args.taskId != null) {
            EditingState.EXISTING_TASK
        } else {
            EditingState.NEW_TASK
        }

        taskEditViewModel.nameToTagMap.observe(viewLifecycleOwner) {}

        if (editingState == EditingState.EXISTING_TASK) {
            taskEditViewModel.getTask(args.taskId!!).observe(viewLifecycleOwner) { task ->
                setupExistingTags(task)
                binding.descriptionET.setText(task.description)
            }
            binding.scheduleLaterBtn.visibility = View.GONE
            binding.scheduledDateTv.visibility = View.GONE
        }

        binding.scheduleLaterBtn.setOnClickListener {
            showDatePickerDialogue()
        }
        binding.submitBtn.setOnClickListener {
            submitButtonHandler(editingState)
            findNavController().popBackStack()
        }
        binding.cancelBtn.setOnClickListener {
            findNavController().popBackStack()
        }

        setupTagsChipGroup()
        setupScheduleTimeLayout()
        setupColorPicker()
    }

    override fun onDestroyView() {
        taskEditViewModel.tagsNameSet.clear()
        taskEditViewModel.setWillNotSchedule()
        _binding = null
        super.onDestroyView()
    }

    @SuppressLint("SimpleDateFormat")
    private fun setupScheduleTimeLayout() {
        taskEditViewModel.scheduledTime.observe(viewLifecycleOwner) { cal ->
            taskEditViewModel.willSchedule.observe(viewLifecycleOwner) {
                if (it) {
                    binding.scheduledDateTv.text = SimpleDateFormat("MM/dd/yyyy").format(cal.time)
                } else {
                    binding.scheduledDateTv.text = ""
                }
            }
        }
    }

    private fun showDatePickerDialogue() {
        val c = Calendar.getInstance()
        val year = c[Calendar.YEAR]
        val month = c[Calendar.MONTH]
        val day = c[Calendar.DAY_OF_MONTH]
        val dialog = DatePickerDialog(
            requireActivity(), { _, y, m, d ->
                val scheduled = Calendar.getInstance().also {
                    it.set(y, m, d)
                }
                if (c.time < scheduled.time) {
                    taskEditViewModel.setScheduleDate(y, m, d)
                    taskEditViewModel.setWillSchedule()
                } else {
                    taskEditViewModel.setWillNotSchedule()
                }
            },
            year, month, day
        )
        dialog.show()
    }

    private fun setupExistingTags(task: Task) {
        taskEditViewModel.idToTagMap.observe(viewLifecycleOwner) { tagsMap ->
            binding.tagsList.removeAllViews()
            taskEditViewModel.tagsNameSet.clear()
            task.tags.forEach {
                val tagName = tagsMap?.get(it)!!.name
                taskEditViewModel.tagsNameSet.add(tagName)
                addTagToChipGroup(tagName)
            }
            selectedColor = task.color ?: ColorSheet.NO_COLOR
            binding.colorSheet.setBackgroundColor(selectedColor)
        }
    }

    private fun setupColorPicker() {
        binding.colorSheet.setOnClickListener {
            ColorSheet().cornerRadius(6)
                .colorPicker(
                    colors = resources.getIntArray(R.array.colors),
                    noColorOption = true,
                    selectedColor = selectedColor,
                    listener = { color ->
                        selectedColor = color
                        binding.colorSheet.setBackgroundColor(selectedColor)
                    })
                .show(parentFragmentManager)
        }
    }

    private fun setupTagsChipGroup() {
        binding.tagsEt.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val string = s.toString()
                if (string.isNotEmpty()) {
                    if (string.last() == ',' || string.last() == '\n' || string.last() == ' ') {
                        binding.tagsEt.setText("")
                    }
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val string = s.toString()
                if (!s.isNullOrEmpty() && string.length > 1) {
                    if (string.last() == ',' || string.last() == '\n' || string.last() == ' ') {
                        val tag = string.replace(",", "").trim()
                        if (tag !in taskEditViewModel.tagsNameSet) {
                            taskEditViewModel.tagsNameSet.add(tag)
                            addTagToChipGroup(tag)
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        })
    }

    private fun addTagToChipGroup(tagText: String) {
        val chip = Chip(context).apply {
            text = tagText
            tag = tagText
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                taskEditViewModel.tagsNameSet.remove(it.tag)
                binding.tagsList.removeView(this as View)
            }
        }
        binding.tagsList.addView(chip as View)
    }

    private fun submitButtonHandler(editingState: EditingState) {
        if (editingState == EditingState.EXISTING_TASK) {
            taskEditViewModel.editTask(binding.descriptionET.text.toString(), selectedColor)
        } else {
            taskEditViewModel.addTask(binding.descriptionET.text.toString(), selectedColor)
        }
    }
}