package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
import com.example.studentregistration.model.FaqItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlin.math.roundToInt

// ✅ Firebase repo (for auth + firestore)
import com.example.studentregistration.data.FirebaseRepo

class DashboardActivity : AppCompatActivity() {

    private val items = listOf(
        "Fees", "FAQ", "My Details", "Refer a Student",
        "Event Calendar", "Daily Attendance", "Hourly Attendance",
        "CAE Result", "ESE Result", "LMS",
        "Library", "Time Table", "Transport", "Outing"
    )

    override fun onCreate(savedInstanceState: Bundle?) {

        WindowCompat.setDecorFitsSystemWindows(window, true)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val toolbar = findViewById<MaterialToolbar?>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)
            supportActionBar?.title = "Student Dashboard"

            // ✅ Avoid overlap with status bar
            ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
                val status = insets.getInsets(WindowInsetsCompat.Type.statusBars())
                v.updatePadding(top = status.top)
                insets
            }
        }

        // ✅ Header (email from intent, then update from Firestore if available)
        val tvSubtitle = findViewById<TextView?>(R.id.tvSubtitle)
        val emailFromIntent = intent.getStringExtra("email_from_login")
        tvSubtitle?.text = emailFromIntent ?: "Welcome"

        val uid = FirebaseRepo.auth.currentUser?.uid
        if (uid != null) {
            FirebaseRepo.db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name")
                    val emailDb = doc.getString("email")
                    val display = name?.takeIf { it.isNotBlank() } ?: emailDb ?: tvSubtitle?.text
                    tvSubtitle?.text = display?.toString() ?: "Welcome"
                }
                .addOnFailureListener {
                    // ignore; keep existing subtitle
                }
        }

        // ✅ RecyclerView: 2 columns
        val rv = findViewById<RecyclerView>(R.id.dashboardRecycler)
        val span = 2
        rv.layoutManager = GridLayoutManager(this, span)
        rv.adapter = DashboardAdapter(items) { handleClick(it) }
        rv.setHasFixedSize(true)

        // ✅ Grid spacing
        val spacingPx = dpToPx(12)
        rv.addItemDecoration(GridSpacingItemDecoration(span, spacingPx, includeEdge = true))
    }

    private fun handleClick(position: Int) {
        when (position) {
            0 -> startActivity(Intent(this, FeesListActivity::class.java))      // Fees
            1 -> showFaqBottomSheet()                                           // FAQ
            2 -> startActivity(Intent(this, DetailsActivity::class.java))       // My Details
            3 -> startActivity(Intent(this, ReferStudentActivity::class.java))  // Refer Student
            else -> Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFaqBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.faq_bottom_sheet, null)
        dialog.setContentView(view)

        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            if (sheet != null) {
                val behavior = BottomSheetBehavior.from(sheet)
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