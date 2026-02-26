package com.example.studentregistration

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentAdapter(
    private val students: List<Student>,
    private val onStudentClick: (Student) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

     class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvBadge: TextView = itemView.findViewById(R.id.tvBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = students[position]

        holder.tvName.text = student.name
        holder.tvBadge.text = student.badgeText


        if (student.hasPaidFees) {
            holder.tvBadge.setBackgroundColor(0xFF4CAF50.toInt()) // Green
            holder.tvBadge.setTextColor(0xFFFFFFFF.toInt())       // White
        } else {
            holder.tvBadge.setBackgroundColor(0xFFF44336.toInt()) // Red
            holder.tvBadge.setTextColor(0xFFFFFFFF.toInt())       // White
        }


        holder.itemView.setOnClickListener {
            onStudentClick(student)
        }
    }

    override fun getItemCount(): Int = students.size
}