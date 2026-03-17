package com.example.studentregistration

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.studentregistration.databinding.ActivityCertificateBinding
import com.example.studentregistration.StartActivity
import java.io.File
import java.io.FileOutputStream

class CertificateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCertificateBinding
    private var lastSavedPdf: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCertificateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // BACK + TITLE
        binding.root.findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        binding.root.findViewById<TextView>(R.id.tvScreenTitle)?.text = "Certificate"

        // LOGOUT
        binding.btnLogout.setOnClickListener {
            val intent = Intent(this, StartActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                )
            }
            startActivity(intent)
            finish()
        }

        // LOAD FRAGMENT DATA
        val hasArrears = intent.getBooleanExtra("hasArrears", false)

        val args = Bundle().apply {
            putString("name", intent.getStringExtra("name") ?: "")
            putString("reg", intent.getStringExtra("reg") ?: "")
            putString("roll", intent.getStringExtra("roll") ?: "")
            putString("dept", intent.getStringExtra("dept") ?: "")
            putString("sem", intent.getStringExtra("sem") ?: "")
            putString("parent", intent.getStringExtra("parent") ?: "")
            putString("collegeName", SessionPrefs(this@CertificateActivity).collegeName ?: "")
            putString("arrearsCount", intent.getStringExtra("arrearsCount") ?: "0")

            @Suppress("DEPRECATION")
            putParcelable("signature_uri", intent.getParcelableExtra<Uri>("signature_uri"))

            // ✅ NEW: pass student photo
            putString("profile_photo_uri", intent.getStringExtra("profile_photo_uri"))

            // QR SHORT DATA
            putString(
                "qr_data",
                "Name: ${intent.getStringExtra("name") ?: ""}\n" +
                        "Reg: ${intent.getStringExtra("reg") ?: ""}\n" +
                        "Dept: ${intent.getStringExtra("dept") ?: ""}\n" +
                        "Sem: ${intent.getStringExtra("sem") ?: ""}"
            )
        }

        if (savedInstanceState == null) {
            val frag: Fragment =
                if (hasArrears) ArrearCertificateFragment()
                else FinalCertificateFragment()
            frag.arguments = args

            supportFragmentManager.beginTransaction()
                .replace(binding.fragmentContainer.id, frag)
                .commit()
        }

        // DOWNLOAD PDF
        binding.btnDownloadPdf.setOnClickListener {
            val dialog = showLoadingDialog()
            lastSavedPdf = generatePdfOrNull(dialog)

            if (lastSavedPdf != null) {
                Toast.makeText(this, "PDF saved successfully!", Toast.LENGTH_LONG).show()
            }
        }

        // SHARE PDF
        binding.btnSharePdf.setOnClickListener {
            val dialog = showLoadingDialog()

            val pdf = lastSavedPdf ?: generatePdfOrNull(dialog)
            if (pdf == null) {
                dialog.dismiss()
                Toast.makeText(this, "Unable to generate PDF.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()

            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                pdf
            )

            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(share, "Share Certificate PDF"))
        }
    }

    // PDF CREATION
    private fun generatePdfOrNull(dialog: AlertDialog): File? {
        val view = binding.fragmentContainer

        if (view.width == 0 || view.height == 0) {
            dialog.dismiss()
            Toast.makeText(this, "Certificate not fully loaded!", Toast.LENGTH_SHORT).show()
            return null
        }

        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDoc.finishPage(page)

        val downloads =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()

        val studentName =
            (intent.getStringExtra("name") ?: "Student").trim().replace("\\s+".toRegex(), "_")

        val file = File(downloads, "Certificate_${studentName}_${System.currentTimeMillis()}.pdf")

        return try {
            FileOutputStream(file).use { fos -> pdfDoc.writeTo(fos) }
            file
        } catch (e: Exception) {
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            null
        } finally {
            pdfDoc.close()
            dialog.dismiss()
        }
    }

    private fun showLoadingDialog(): AlertDialog {
        val view = layoutInflater.inflate(R.layout.dialog_loading, null)
        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(0x00000000))
        dialog.show()
        return dialog
    }
}