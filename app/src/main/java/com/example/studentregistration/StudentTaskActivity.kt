package com.example.studentregistration

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentregistration.databinding.ActivityStudentTaskBinding

class StudentTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentTaskBinding
    private val viewModel: StudentTaskViewModel by viewModels()

    private lateinit var tasksAdapter: TasksAdapter
    private var fieldsLocked: Boolean = false


    private fun saveTasksToPrefs(task1: String, task2: String, task3: String) {
        val prefs = getSharedPreferences("tasks_prefs", MODE_PRIVATE)
        prefs.edit()
            .putString("task1", task1)
            .putString("task2", task2)
            .putString("task3", task3)
            .apply()
    }

    private fun loadTasksFromPrefs(): List<String> {
        val prefs = getSharedPreferences("tasks_prefs", MODE_PRIVATE)
        val t1 = prefs.getString("task1", "") ?: ""
        val t2 = prefs.getString("task2", "") ?: ""
        val t3 = prefs.getString("task3", "") ?: ""
        return listOf(t1, t2, t3)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStudentTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val saved = loadTasksFromPrefs()
        viewModel.setTask(0, saved[0])
        viewModel.setTask(1, saved[1])
        viewModel.setTask(2, saved[2])


        tasksAdapter = TasksAdapter(
            onRowEditClick = { index, text ->
                unlockFields()
                when (index) {
                    0 -> setTextAndFocus(binding.etTask1, text)
                    1 -> setTextAndFocus(binding.etTask2, text)
                    2 -> setTextAndFocus(binding.etTask3, text)
                }
            },
            onRowClearClick = { index ->
                viewModel.clearTask(index)
                saveAllThree()
            }
        )

        binding.recyclerTasks.apply {
            layoutManager = LinearLayoutManager(this@StudentTaskActivity)
            adapter = tasksAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        // Observe VM -> UI
        viewModel.tasks.observe(this) { list -> tasksAdapter.submitList(list) }
        viewModel.task1.observe(this) { setTextIfDifferent(binding.etTask1, it) }
        viewModel.task2.observe(this) { setTextIfDifferent(binding.etTask2, it) }
        viewModel.task3.observe(this) { setTextIfDifferent(binding.etTask3, it) }

        binding.etTask1.addTextChangedListener(makeWatcher {
            viewModel.setTask(0, it); saveAllThree()
        })
        binding.etTask2.addTextChangedListener(makeWatcher {
            viewModel.setTask(1, it); saveAllThree()
        })
        binding.etTask3.addTextChangedListener(makeWatcher {
            viewModel.setTask(2, it); saveAllThree()
        })


        binding.btnSave.setOnClickListener {
            saveAllThree()
            lockFields()
            clearFocusAndHideKeyboard()
        }


        binding.btnToggleEdit.setOnClickListener {
            if (fieldsLocked) unlockFields() else lockFields()
        }


        binding.btnClearAll.setOnClickListener {
            viewModel.clearAll()
            saveTasksToPrefs("", "", "")
            clearFocusAndHideKeyboard()
        }


        unlockFields()
    }


    private fun lockFields() {
        fieldsLocked = true
        binding.etTask1.setEditable(false)
        binding.etTask2.setEditable(false)
        binding.etTask3.setEditable(false)
        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saved"
        setDimmed(true)
    }

    private fun unlockFields() {
        fieldsLocked = false
        binding.etTask1.setEditable(true)
        binding.etTask2.setEditable(true)
        binding.etTask3.setEditable(true)
        binding.btnSave.isEnabled = true
        binding.btnSave.text = "Save"
        setDimmed(false)
    }

    private fun setDimmed(dim: Boolean) {
        val a = if (dim) 0.6f else 1f
        binding.etTask1.alpha = a
        binding.etTask2.alpha = a
        binding.etTask3.alpha = a
        binding.btnToggleEdit.alpha = if (fieldsLocked) 0.9f else 0.9f
    }

    // -------------------- Small utilities --------------------
    private fun saveAllThree() {
        saveTasksToPrefs(
            binding.etTask1.text.toString(),
            binding.etTask2.text.toString(),
            binding.etTask3.text.toString()
        )
    }

    private fun makeWatcher(onTextChanged: (String) -> Unit) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { onTextChanged(s?.toString().orEmpty()) }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun setTextIfDifferent(editText: EditText, newText: String) {
        val current = editText.text?.toString() ?: ""
        if (current == newText) return
        val oldCursor = editText.selectionStart
        editText.setText(newText)
        val safePos = when {
            oldCursor < 0 -> newText.length
            oldCursor <= newText.length -> oldCursor
            else -> newText.length
        }
        editText.setSelection(safePos)
    }

    private fun setTextAndFocus(editText: EditText, newText: String) {
        setTextIfDifferent(editText, newText)
        editText.requestFocus()
        editText.setSelection(editText.text?.length ?: 0)
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun clearFocusAndHideKeyboard() {
        currentFocus?.clearFocus()
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}


private fun EditText.setEditable(editable: Boolean) {
    isEnabled = editable
    isFocusable = editable
    isFocusableInTouchMode = editable
    if (!editable) clearFocus()
}