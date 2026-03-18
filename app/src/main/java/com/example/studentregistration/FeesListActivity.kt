package com.example.studentregistration

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentregistration.databinding.ActivityFeesListBinding

class FeesListActivity : AppCompatActivity(), ClickListener {

    private lateinit var binding: ActivityFeesListBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        // ✅ Prevent layout going under the status bar
        WindowCompat.setDecorFitsSystemWindows(window, true)
        super.onCreate(savedInstanceState)

        binding = ActivityFeesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ Apply status bar padding only to the back bar
        val backBar = findViewById<View>(R.id.backBar)
        ViewCompat.setOnApplyWindowInsetsListener(backBar) { v, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(v.paddingLeft, topInset, v.paddingRight, v.paddingBottom)
            insets
        }

        // ✅ Back bar setup
        val backBtn = findViewById<ImageButton>(R.id.btnBack)
        val title = findViewById<TextView>(R.id.tvScreenTitle)
        title.text = "Fees List"

        backBtn.setOnClickListener { navigateUpToDashboard() }

        // ✅ System back → Dashboard
        onBackPressedDispatcher.addCallback(this) {
            navigateUpToDashboard()
        }

        // ✅ Recycler setup
        binding.rvStudents.layoutManager = LinearLayoutManager(this)

        val studentSession = SessionStudentPrefs(this)
        val statusPrefs = StudentStatusPrefs(this)

        val savedId = studentSession.selectedStudentId
        if (savedId != -1) {
            Toast.makeText(
                this,
                "Previously selected student ID = $savedId",
                Toast.LENGTH_SHORT
            ).show()
        }

        val baseList = FakeData.getStudents().shuffled()

        // ✅ Override paid/ due status of only selected student
        val students = if (savedId != -1) {
            val isPaid = statusPrefs.isPaid(savedId)
            baseList.map { s ->
                if (s.id == savedId) s.copy(hasPaidFees = isPaid) else s
            }
        } else baseList

        val adapter = StudentAdapter(students) { selectedStudent ->
            studentSession.selectedStudentId = selectedStudent.id
            Toast.makeText(this, "Selected: ${selectedStudent.name}", Toast.LENGTH_SHORT).show()
        }

        binding.rvStudents.adapter = adapter
    }

    override fun onClick(pos: Int) {
        Log.d("FeesListActivity", "Clicked position = $pos")
    }

    // ✅ Navigate back to Dashboard
    private fun navigateUpToDashboard() {
        startActivity(
            Intent(this, DashboardActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
            }
        )
        finish()
    }
}