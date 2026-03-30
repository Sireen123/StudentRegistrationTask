package com.example.studentregistration.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
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
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val icon: LottieAnimationView = itemView.findViewById(R.id.imgIcon)

        init {
            itemView.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onItemClick(pos)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val label = items[position]
        holder.title.text = label

        val ctx = holder.itemView.context
        val colorRes = colorCycle[position % colorCycle.size]

        holder.itemView.setBackgroundColor(ContextCompat.getColor(ctx, colorRes))
        holder.title.setTextColor(ContextCompat.getColor(ctx, android.R.color.black))

        holder.title.maxLines =
            if (label.length <= 10 && !label.contains(" ")) 1 else 2

        if (label.equals("Daily Attendance", ignoreCase = true)) {
            holder.title.text = "Daily\nAttendance"
        }

        val animRes = when (position) {
            0 -> R.raw.anim_fees
            1 -> R.raw.anim_faq
            2 -> R.raw.anim_details
            3 -> R.raw.anim_refer
            4 -> R.raw.anim_calendar
            5 -> R.raw.anim_daily_attendance
            6 -> R.raw.anim_hourly_attendance
            7 -> R.raw.anim_cae
            8 -> R.raw.anim_ese
            9 -> R.raw.anim_lms
            10 -> R.raw.anim_library
            11 -> R.raw.anim_timetable
            12 -> R.raw.anim_transport
            13 -> R.raw.anim_outing
            else -> R.raw.anim_default
        }

        holder.icon.setAnimation(animRes)
        holder.icon.playAnimation()

        holder.itemView.scaleX = 0.85f
        holder.itemView.scaleY = 0.85f
        holder.itemView.alpha = 0f

        holder.itemView.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay((position * 60).toLong())
            .setDuration(280)
            .start()
    }

    override fun getItemCount(): Int = items.size
}