package com.example.studentregistration

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studentregistration.databinding.ItemTaskBinding

class TasksAdapter(
    private val onRowEditClick: (index: Int, text: String) -> Unit,
    private val onRowClearClick: (index: Int) -> Unit
) : ListAdapter<String, TasksAdapter.TaskViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnRowEdit.setOnClickListener {
                val idx = bindingAdapterPosition
                if (idx != RecyclerView.NO_POSITION) {
                    onRowEditClick(idx, getItem(idx))
                }
            }
            binding.btnRowClear.setOnClickListener {
                val idx = bindingAdapterPosition
                if (idx != RecyclerView.NO_POSITION) {
                    onRowClearClick(idx)
                }
            }
        }

        fun bind(text: String) {
            binding.tvTask.text = if (text.isBlank()) "(empty)" else text

        }
    }
}