package com.example.studentregistration

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ReferralAdapter(private val list: List<ReferralModel>) :
    RecyclerView.Adapter<ReferralAdapter.ReferralViewHolder>() {

    class ReferralViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPhone: TextView = view.findViewById(R.id.tvPhone)
        val tvDept: TextView = view.findViewById(R.id.tvDept)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReferralViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_referral, parent, false)
        return ReferralViewHolder(v)
    }

    override fun onBindViewHolder(holder: ReferralViewHolder, position: Int) {
        val item = list[position]

        holder.ivIcon.setImageResource(R.drawable.ic_user)
        holder.tvName.text = item.name
        holder.tvPhone.text = "📞 ${item.phone}"
        holder.tvDept.text = "🎓 ${item.department}"
    }

    override fun getItemCount(): Int = list.size
}