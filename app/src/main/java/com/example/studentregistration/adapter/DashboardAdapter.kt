package com.example.studentregistration.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.studentregistration.R

class DashboardAdapter(
    private val items: List<String>,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<DashboardAdapter.ViewHolder>() {

    private val colorCycle = listOf(
        R.color.tile_pink,
        R.color.tile_green,
        R.color.tile_blue,
        R.color.tile_purple,
        R.color.tile_orange,
        R.color.tile_mint,
        R.color.tile_beige,
        R.color.tile_yellow,
        R.color.tile_lavender
    )

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val title: TextView = itemView.findViewById(R.id.itemTitle)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onItemClick(pos)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val label = items[position]
        holder.title.text = label

        val ctx = holder.itemView.context
        val colorRes = colorCycle[position % colorCycle.size]
        holder.itemView.setBackgroundColor(ContextCompat.getColor(ctx, colorRes))
        holder.title.setTextColor(ContextCompat.getColor(ctx, android.R.color.black))

        // ✅ Keep short words in one line
        holder.title.maxLines =
            if (label.length <= 10 && !label.contains(" ")) 1 else 2

        // ✅ Format "Daily Attendance"
        if (label.equals("Daily Attendance", ignoreCase = true)) {
            holder.title.text = "Daily\nAttendance"
        }
    }

    override fun getItemCount(): Int = items.size
}