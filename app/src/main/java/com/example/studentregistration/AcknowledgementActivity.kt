package com.example.studentregistration

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentregistration.databinding.ActivityAcknowledgementBinding

class AcknowledgementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAcknowledgementBinding

    // Raw incoming values
    private var name = ""
    private var reg = ""
    private var roll = ""
    private var dept = ""
    private var sem = ""
    private var parent = ""
    private var collegeName = ""
    private var hasArrears = false
    private var arrearsCount = "0"
    private var signatureUri: Uri? = null
    private var signedOn = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAcknowledgementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ⭐ CUSTOM BACK ARROW (from include_back_bar.xml)
        binding.root.findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        binding.root.findViewById<TextView>(R.id.tvScreenTitle)?.text = "Acknowledgement"

        // Title
        binding.tvTitle.text = "ACKNOWLEDGEMENT"

        // Read extras
        intent.extras?.let { b ->
            name         = b.getString("name", "")
            reg          = b.getString("reg", "")
            roll         = b.getString("roll", "")
            dept         = b.getString("dept", "")
            sem          = b.getString("sem", "")
            parent       = b.getString("parent", "")
            collegeName  = b.getString("collegeName", "")
            hasArrears   = b.getBoolean("hasArrears", false)
            arrearsCount = b.getString("arrearsCount", "0") ?: "0"
            signedOn     = b.getString("signed_on", "")
            signatureUri = b.getParcelable("signature_uri")
        }

        // Clean labels like "Name:" etc
        fun clean(v: String): String = v
            .replace(Regex("(?i)^\\s*name:\\s*"), "")
            .replace(Regex("(?i)^\\s*register\\s*no:\\s*"), "")
            .replace(Regex("(?i)^\\s*roll\\s*no:\\s*"), "")
            .replace(Regex("(?i)^\\s*department:\\s*"), "")
            .replace(Regex("(?i)^\\s*semester:\\s*"), "")
            .replace(Regex("(?i)^\\s*parent/?guardian:\\s*"), "")
            .trim()

        val cName    = clean(name)
        val cReg     = clean(reg)
        val cRoll    = clean(roll)
        val cDept    = clean(dept)
        val cSem     = clean(sem)
        val cParent  = clean(parent)
        val cCollege = clean(collegeName)

        // Fill details
        binding.tvName.text     = cName
        binding.tvReg.text      = cReg
        binding.tvRoll.text     = cRoll
        binding.tvDept.text     = cDept
        binding.tvSem.text      = cSem
        binding.tvParent.text   = cParent
        binding.tvCollege.text  = cCollege
        binding.tvArrearFlag.text = if (hasArrears) "Yes ($arrearsCount)" else "No"

        // SUBMIT → CertificateActivity
        binding.btnSubmit.setOnClickListener {

            if (!binding.cbAcknowledge.isChecked) {
                Toast.makeText(this, "Please confirm these details.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val i = Intent(this, CertificateActivity::class.java).apply {
                putExtra("name", cName)
                putExtra("reg", cReg)
                putExtra("roll", cRoll)
                putExtra("dept", cDept)
                putExtra("sem", cSem)
                putExtra("parent", cParent)
                putExtra("collegeName", cCollege)

                putExtra("hasArrears", hasArrears)
                putExtra("arrearsCount", arrearsCount)

                putExtra("signature_uri", signatureUri)
                putExtra("signed_on", signedOn)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(i)
        }
    }
}