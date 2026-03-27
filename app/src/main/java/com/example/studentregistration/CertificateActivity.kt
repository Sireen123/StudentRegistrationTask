package com.example.studentregistration

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import com.example.studentregistration.data.FirebaseRepo
import com.example.studentregistration.data.User
import java.io.File
import java.io.FileOutputStream

class CertificateActivity : AppCompatActivity() {

    private var studentId: String? = null
    private var user: User? = null
    private var hasArrears = false
    private var arrearsCount = 0
    private var collegeName = ""
    private var signatureUri: Uri? = null
    private var signedOn = ""

    override fun onCreate(savedInstanceState: Bundle?) {

        // ✅ APPLY THEME (safe & required)
        val savedTheme = getSharedPreferences("theme_prefs", MODE_PRIVATE)
            .getString("app_theme", "light")
        if (savedTheme == "dark") {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificate)

        user = intent.getParcelableExtra("user")
        hasArrears = intent.getBooleanExtra("hasArrears", false)
        arrearsCount = intent.getIntExtra("arrearsCount", 0)
        collegeName = intent.getStringExtra("collegeName") ?: ""
        signedOn = intent.getStringExtra("signed_on") ?: ""

        signatureUri =
            if (Build.VERSION.SDK_INT >= 33)
                intent.getParcelableExtra("signature_uri", Uri::class.java)
            else @Suppress("DEPRECATION")
            intent.getParcelableExtra("signature_uri")

        studentId = intent.getStringExtra(MainActivity.EXTRA_STUDENT_ID)
            ?: FirebaseRepo.auth.currentUser?.uid

        // ✅ Back arrow → Acknowledgement
        val includeLayout = findViewById<LinearLayout>(R.id.includeBack)
        val btnBackArrow = includeLayout.findViewById<ImageView>(R.id.btnBack)
        btnBackArrow.setOnClickListener { goAcknowledgement() }

        // ✅ Back button → Dashboard
        findViewById<Button>(R.id.btnBackToDashboard).setOnClickListener { goDashboard() }

        // ✅ Android back button → Dashboard
        onBackPressedDispatcher.addCallback(this) { goDashboard() }

        if (user == null) {
            goDashboard()
            return
        }

        if (savedInstanceState == null) loadFragment()

        // ✅ PDF actions
        findViewById<Button>(R.id.btnDownloadPdf).setOnClickListener { savePdf() }
        findViewById<Button>(R.id.btnSharePdf).setOnClickListener { sharePdf() }
    }

    private fun goAcknowledgement() {
        startActivity(Intent(this, AcknowledgementActivity::class.java).apply {
            putExtra("user", user)
            putExtra("hasArrears", hasArrears)
            putExtra("arrearsCount", arrearsCount)
            putExtra("collegeName", collegeName)
            putExtra("signature_uri", signatureUri)
            putExtra("signed_on", signedOn)
            putExtra(MainActivity.EXTRA_STUDENT_ID, studentId)
        })
        finish()
    }

    private fun goDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_STUDENT_ID, studentId)
        })
        finish()
    }

    private fun loadFragment() {
        val u = user!!
        val isArrear = hasArrears || arrearsCount > 0

        updateWorkflow(isArrear)

        val args = Bundle().apply {
            putParcelable("user", u)
            putString("collegeName", collegeName)
            putBoolean("hasArrears", isArrear)
            putInt("arrearsCount", arrearsCount)
            putParcelable("signature_uri", signatureUri)
            putString("signed_on", signedOn)
        }

        val frag =
            if (isArrear) ArrearCertificateFragment().apply { arguments = args }
            else FinalCertificateFragment().apply { arguments = args }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, frag)
            .commit()
    }

    private fun updateWorkflow(isArrear: Boolean) {
        val uid = studentId ?: return

        FirebaseRepo.rtdb.child("details").child(uid).child("workflow").updateChildren(
            mapOf(
                "detailsSaved" to true,
                "ackPending" to false,
                "ackCompleted" to true,
                "certPending" to false,
                "certIssuedAt" to System.currentTimeMillis(),
                "certType" to if (isArrear) "ARREAR" else "FINAL",
                "certArrear" to isArrear,
                "certFinal" to !isArrear
            )
        )
    }

    private fun generateFullBitmap(): Bitmap {
        val fragmentView = supportFragmentManager.findFragmentById(R.id.fragmentContainer)?.view
            ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

        val root = fragmentView.findViewById<LinearLayout>(R.id.certificateRoot)
            ?: fragmentView

        root.measure(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            root.measuredWidth,
            root.measuredHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        root.draw(canvas)

        return bitmap
    }

    private fun savePdf() {
        try {
            val bitmap = generateFullBitmap()

            val pdf = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdf.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdf.finishPage(page)

            val filename = "Certificate_${System.currentTimeMillis()}.pdf"
            val values = ContentValues().apply {
                put(MediaStore.Files.FileColumns.DISPLAY_NAME, filename)
                put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.Files.FileColumns.RELATIVE_PATH, "Documents/StudentCertificates")
            }

            val uri = contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                values
            )!!

            contentResolver.openOutputStream(uri).use { out ->
                pdf.writeTo(out!!)
            }
            pdf.close()

            Toast.makeText(this, "✅ PDF Saved to Documents/StudentCertificates", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdf() {
        try {
            val bitmap = generateFullBitmap()

            val file = File(cacheDir, "certificate_share.pdf")
            val pdf = PdfDocument()

            val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
            val page = pdf.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdf.finishPage(page)

            FileOutputStream(file).use { out -> pdf.writeTo(out) }
            pdf.close()

            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share Certificate PDF"))

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}