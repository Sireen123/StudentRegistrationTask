package com.example.studentregistration

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ReferralDetailsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private val referralList = ArrayList<ReferralModel>()
    private lateinit var adapter: ReferralAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_referral_details)

        recycler = findViewById(R.id.rvReferrals)
        progress = findViewById(R.id.progress)

        recycler.layoutManager = LinearLayoutManager(this)
        adapter = ReferralAdapter(referralList)
        recycler.adapter = adapter

        loadReferrals()
    }

    private fun loadReferrals() {
        progress.visibility = View.VISIBLE

        FirebaseDatabase.getInstance().getReference("referrals")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {

                    referralList.clear()

                    if (snapshot.exists()) {
                        for (child in snapshot.children) {
                            val model = child.getValue(ReferralModel::class.java)
                            if (model != null) referralList.add(model)
                        }
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@ReferralDetailsActivity, "No referrals found", Toast.LENGTH_SHORT).show()
                    }

                    progress.visibility = View.GONE
                }

                override fun onCancelled(error: DatabaseError) {
                    progress.visibility = View.GONE
                    Toast.makeText(this@ReferralDetailsActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }
}