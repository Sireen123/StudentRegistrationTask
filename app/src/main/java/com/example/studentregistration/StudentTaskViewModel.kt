package com.example.studentregistration

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StudentTaskViewModel : ViewModel() {

    private val _task1 = MutableLiveData("")
    private val _task2 = MutableLiveData("")
    private val _task3 = MutableLiveData("")

    val task1: LiveData<String> = _task1
    val task2: LiveData<String> = _task2
    val task3: LiveData<String> = _task3

    val tasks: MediatorLiveData<List<String>> = MediatorLiveData<List<String>>().apply {
        fun recompute() {
            value = listOf(
                _task1.value.orEmpty(),
                _task2.value.orEmpty(),
                _task3.value.orEmpty()
            )
        }
        addSource(_task1) { recompute() }
        addSource(_task2) { recompute() }
        addSource(_task3) { recompute() }
        value = listOf("", "", "")
    }

    fun setTask(index: Int, text: String) {
        when (index) {
            0 -> _task1.value = text
            1 -> _task2.value = text
            2 -> _task3.value = text
            else -> Unit
        }
    }

    fun clearTask(index: Int) = setTask(index, "")

    fun clearAll() {
        _task1.value = ""
        _task2.value = ""
        _task3.value = ""
    }
}