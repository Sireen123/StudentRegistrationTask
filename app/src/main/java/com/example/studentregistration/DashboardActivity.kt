package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.studentregistration.adapter.FaqAdapter
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.model.FaqItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import kotlin.math.roundToInt

class DashboardActivity : AppCompatActivity() {

    private val items = listOf(
        "Fees", "FAQ", "My Details", "Refer a Student",
        "Event Calendar", "Daily Attendance", "Hourly Attendance",
        "CAE Result", "ESE Result", "LMS",
        "Library", "Time Table", "Transport", "Outing"
    )

    private var userRef: DatabaseReference? = null
    private var userListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val toolbar = findViewById<MaterialToolbar?>(R.id.toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.title = "Student Dashboard"

            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                v.updatePadding(top = statusBars.top)
                insets
            }
        }

        findViewById<TextView?>(R.id.tvSubtitle)?.text =
            intent.getStringExtra("email_from_login") ?: "Welcome..."

        // Logout
        findViewById<Button?>(R.id.btnLogout)?.setOnClickListener {
            userListener?.let { listener -> userRef?.removeEventListener(listener) }
            FirebaseRepo.auth.signOut()

            val intent = Intent(this, StartActivity::class.java)
            intent.addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            )
            startActivity(intent)
            finish()
        }

        // Recycler Grid
        val rv = findViewById<RecyclerView>(R.id.dashboardRecycler)
        val span = 2
        rv.layoutManager = GridLayoutManager(this, span)
        rv.adapter = DashboardAdapter(items) { handleClick(it) }
        rv.setHasFixedSize(true)
        rv.addItemDecoration(GridSpacingItemDecoration(span, dpToPx(12), true))
    }

    override fun onStart() {
        super.onStart()

        val tv = findViewById<TextView?>(R.id.tvSubtitle)
        tv?.text = "Welcome..."

        val uid = FirebaseRepo.auth.currentUser?.uid ?: return
        val ref = FirebaseRepo.rtdb.child("users").child(uid)

        userListener?.let { listener -> userRef?.removeEventListener(listener) }
        userRef = ref

        userListener = object : ValueEventListener {
            override fun onDataChange(snap: DataSnapshot) {
                val name = snap.child("name").getValue(String::class.java)
                val email = snap.child("email").getValue(String::class.java)

                tv?.text = when {
                    !name.isNullOrBlank() -> name
                    !email.isNullOrBlank() -> email
                    else -> "Welcome"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                tv?.text = "Welcome"
            }
        }

        ref.addValueEventListener(userListener!!)
    }

    override fun onStop() {
        super.onStop()
        userListener?.let { listener -> userRef?.removeEventListener(listener) }
        userListener = null
        userRef = null
    }

    private fun handleClick(position: Int) {
        when (position) {
            0 -> startActivity(Intent(this, FeesListActivity::class.java))
            1 -> showFaqBottomSheet()
            2 -> startActivity(Intent(this, DetailsActivity::class.java))
            3 -> startActivity(Intent(this, ReferStudentActivity::class.java))
            else -> Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFaqBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.faq_bottom_sheet, null, false)
        dialog.setContentView(view)

        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            sheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.65f).toInt()
            }
        }

        view.findViewById<ImageView?>(R.id.btnClose)?.setOnClickListener { dialog.dismiss() }

        val rvFaq = view.findViewById<RecyclerView>(R.id.rvFaq)
        rvFaq.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvFaq.adapter = FaqAdapter(
            listOf(
                FaqItem("How to create account?", "Tap New User and fill details."),
                FaqItem("Why OTP?", "For account security."),
                FaqItem("Invalid input?", "Fill all fields properly."),
                FaqItem("Why select department?", "Required for certificate."),
                FaqItem("Where is PDF saved?", "Downloads folder.")
            )
        )

        dialog.show()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).roundToInt()

    private class GridSpacingItemDecoration(
        private val spanCount: Int,
        private val spacingPx: Int,
        private val includeEdge: Boolean
    ) : RecyclerView.ItemDecoration() {

        override fun getItemOffsets(
            outRect: android.graphics.Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State
        ) {
            val position = parent.getChildAdapterPosition(view)
            val column = position % spanCount

            if (includeEdge) {
                outRect.left = spacingPx - column * spacingPx / spanCount
                outRect.right = (column + 1) * spacingPx / spanCount
                if (position < spanCount) outRect.top = spacingPx
                outRect.bottom = spacingPx
            } else {
                outRect.left = column * spacingPx / spanCount
                outRect.right = spacingPx - (column + 1) * spacingPx / spanCount
                if (position >= spanCount) outRect.top = spacingPx
            }
        }
    }
}
