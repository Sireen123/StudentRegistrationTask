package com.example.studentregistration

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.studentregistration.databinding.ActivityFeesListBinding

class FeesListActivity : AppCompatActivity(),ClickListener {
    private lateinit var binding: ActivityFeesListBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFeesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        binding.rvStudents.layoutManager = LinearLayoutManager(this)


        val baseList = FakeData.getStudents().shuffled()

        // Override PAID/DUe for selected student only
        val students = if (savedId != -1) {
            val isPaid = statusPrefs.isPaid(savedId)
            baseList.map { s ->
                if (s.id == savedId) s.copy(hasPaidFees = isPaid) else s
            }
        } else {
            baseList
        }

        val adapter = StudentAdapter(students) { selectedStudent ->
            // Save selection
            studentSession.selectedStudentId = selectedStudent.id
            Toast.makeText(this, "Selected: ${selectedStudent.name}", Toast.LENGTH_SHORT).show()
        }

        binding.rvStudents.adapter = adapter
    }

    override fun onClick(pos: Int) {
       Log.d("position",pos.toString())
    }
}