package com.example.studentregistration

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.studentregistration.databinding.ItemTaskBinding


class TasksAdapter(
    private val onRowEditClick: (index: Int, text: String) -> Unit,
    private val onRowClearClick: (index: Int) -> Unit
) : ListAdapter<String, TasksAdapter.TaskViewHolder>(DIFF) {

    var editMode: Boolean = false
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(text: String, index: Int) {

            binding.tvTask.text = if (text.isBlank()) "(empty)" else text

            val vis = if (editMode) View.VISIBLE else View.GONE
            binding.btnRowEdit.visibility = vis
            binding.btnRowClear.visibility = vis

            binding.btnRowEdit.setOnClickListener { onRowEditClick(index, text) }
            binding.btnRowClear.setOnClickListener { onRowClearClick(index) }
        }
    }
}