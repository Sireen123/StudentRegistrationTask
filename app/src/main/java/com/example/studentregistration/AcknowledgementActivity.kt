package com.example.studentregistration

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.studentregistration.databinding.ActivityAcknowledgementBinding

// ✅ Firebase
import com.example.studentregistration.data.FirebaseRepo
import com.google.firebase.firestore.FieldValue
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AcknowledgementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAcknowledgementBinding

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

    // ✅ from DetailsActivity (may be local uri string)
    private var profilePhotoUri: String? = null

    // ✅ Firebase Storage
    private val storage by lazy { FirebaseStorage.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAcknowledgementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Back & Title
        binding.root.findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        binding.root.findViewById<TextView>(R.id.tvScreenTitle)?.text = "Acknowledgement"

        binding.tvTitle.text = "ACKNOWLEDGEMENT"

        // ✅ Read values from DetailsActivity
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
            profilePhotoUri = b.getString("profile_photo_uri")
        }

        // Clean text (remove any "Label: " prefixes passed from previous screens)
        fun clean(v: String) = v.replace(Regex("(?i)^\\s*[a-z ]+:\\s*"), "").trim()

        val cName = clean(name)
        val cReg = clean(reg)
        val cRoll = clean(roll)
        val cDept = clean(dept)
        val cSem = clean(sem)
        val cParent = clean(parent)
        val cCollege = clean(collegeName)

        // Fill summary UI
        binding.tvName.text = cName
        binding.tvReg.text = cReg
        binding.tvRoll.text = cRoll
        binding.tvDept.text = cDept
        binding.tvSem.text = cSem
        binding.tvParent.text = cParent
        binding.tvCollege.text = cCollege
        binding.tvArrearFlag.text = if (hasArrears) "Yes ($arrearsCount)" else "No"

        // ✅ SUBMIT → (1) fire-and-forget save to Firebase, (2) navigate to Certificate
        binding.btnSubmit.setOnClickListener {
            if (!binding.cbAcknowledge.isChecked) {
                Toast.makeText(this, "Please confirm these details.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // (1) Save to Firebase (non-blocking)
            saveAcknowledgementToFirebase(
                name = cName,
                reg = cReg,
                roll = cRoll,
                dept = cDept,
                sem = cSem,
                parent = cParent,
                college = cCollege,
                hasArrears = hasArrears,
                arrearsCount = arrearsCount,
                signatureUri = signatureUri
            )

            // (2) Go to Certificate screen
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
                putExtra("profile_photo_uri", profilePhotoUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(i)
        }
    }

    // =================== FIREBASE SAVE ===================
    private fun saveAcknowledgementToFirebase(
        name: String,
        reg: String,
        roll: String,
        dept: String,
        sem: String,
        parent: String,
        college: String,
        hasArrears: Boolean,
        arrearsCount: String,
        signatureUri: Uri?
    ) {
        val uid = FirebaseRepo.auth.currentUser?.uid ?: return

        // 1) If we have a signature image, upload to Storage → then write doc
        if (signatureUri != null) {
            val fileName = "ack_${System.currentTimeMillis()}.png"
            val ref = storage.reference.child("users/$uid/acknowledgements/$fileName")

            ref.putFile(signatureUri)
                .continueWithTask { ref.downloadUrl }
                .addOnSuccessListener { url ->
                    writeAckDoc(
                        uid = uid,
                        name = name, reg = reg, roll = roll,
                        dept = dept, sem = sem, parent = parent, college = college,
                        hasArrears = hasArrears, arrearsCount = arrearsCount,
                        signatureUrl = url.toString()
                    )
                }
                .addOnFailureListener {
                    // upload failed → still write doc without URL
                    writeAckDoc(
                        uid = uid,
                        name = name, reg = reg, roll = roll,
                        dept = dept, sem = sem, parent = parent, college = college,
                        hasArrears = hasArrears, arrearsCount = arrearsCount,
                        signatureUrl = ""
                    )
                }
        } else {
            // No signature image → just write doc
            writeAckDoc(
                uid = uid,
                name = name, reg = reg, roll = roll,
                dept = dept, sem = sem, parent = parent, college = college,
                hasArrears = hasArrears, arrearsCount = arrearsCount,
                signatureUrl = ""
            )
        }
    }

    private fun writeAckDoc(
        uid: String,
        name: String,
        reg: String,
        roll: String,
        dept: String,
        sem: String,
        parent: String,
        college: String,
        hasArrears: Boolean,
        arrearsCount: String,
        signatureUrl: String
    ) {
        val fallbackSignedOn = if (signedOn.isNotBlank()) signedOn
        else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())

        val data = hashMapOf(
            "name" to name,
            "registerNo" to reg,
            "rollNo" to roll,
            "department" to dept,
            "semester" to sem,
            "parentName" to parent,
            "collegeName" to college,
            "hasArrears" to hasArrears,
            "arrearsCount" to arrearsCount,
            "signatureUrl" to signatureUrl,          // Firebase Storage URL (may be empty on failure)
            "profilePhotoUri" to (profilePhotoUri ?: ""), // whatever was passed (may be local uri)
            "signedOn" to fallbackSignedOn,
            "createdAt" to FieldValue.serverTimestamp()
        )

        FirebaseRepo.db.collection("users")
            .document(uid)
            .collection("acknowledgements")
            .add(data)
            .addOnSuccessListener { /* no-op */ }
            .addOnFailureListener { /* no-op */ }
    }
}