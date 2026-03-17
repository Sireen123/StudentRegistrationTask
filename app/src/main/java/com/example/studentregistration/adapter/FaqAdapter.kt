package com.example.studentregistration.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.studentregistration.R
import com.example.studentregistration.model.FaqItem

class FaqAdapter(private val list: List<FaqItem>) :
    RecyclerView.Adapter<FaqAdapter.FaqVH>() {

    inner class FaqVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val q: TextView = itemView.findViewById(R.id.tvQuestion)
        val a: TextView = itemView.findViewById(R.id.tvAnswer)
        val arrow: ImageView = itemView.findViewById(R.id.ivArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqVH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_faq, parent, false)
        return FaqVH(v)
    }

    override fun onBindViewHolder(holder: FaqVH, position: Int) {
        val item = list[position]

        holder.q.text = item.question
        holder.a.text = item.answer

        holder.a.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
        holder.arrow.rotation = if (item.isExpanded) 180f else 0f

        holder.itemView.setOnClickListener {
            val expanded = !item.isExpanded
            item.isExpanded = expanded

            holder.arrow.animate().rotation(if (expanded) 180f else 0f).setDuration(150).start()

            if (expanded) {
                holder.a.visibility = View.VISIBLE
                holder.a.alpha = 0f
                holder.a.animate().alpha(1f).setDuration(150).start()
            } else {
                holder.a.animate().alpha(0f).setDuration(150).withEndAction {
                    holder.a.visibility = View.GONE
                }.start()
            }
        }
    }

    override fun getItemCount() = list.size
}
