package com.example.studentregistration

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.studentregistration.databinding.FragmentArrearCertificateBinding
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.text.SimpleDateFormat
import java.util.*

class ArrearCertificateFragment : Fragment() {

    private var _binding: FragmentArrearCertificateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArrearCertificateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val b = requireArguments()

        // ✅ Load student photo (SAFE - bitmap decode)
        val photoUriStr = b.getString("profile_photo_uri")
        if (!photoUriStr.isNullOrBlank()) {
            try {
                val uri = Uri.parse(photoUriStr)
                val bmp = loadBitmapFromUri(uri)
                if (bmp != null) {
                    binding.imgProfile.setImageBitmap(bmp)
                    binding.imgProfile.visibility = View.VISIBLE
                } else {
                    binding.imgProfile.visibility = View.GONE
                }
            } catch (e: Exception) {
                binding.imgProfile.visibility = View.GONE
            }
        } else {
            binding.imgProfile.visibility = View.GONE
        }

        // ✅ Basic details
        val name = b.getString("name", "")
        val parent = b.getString("parent", "")
        val roll = b.getString("roll", "")
        val sem = b.getString("sem", "")
        val dept = b.getString("dept", "")
        val college = b.getString("collegeName", "")
        val arrears = b.getString("arrearsCount", "0")

        val signatureUri: Uri? =
            if (Build.VERSION.SDK_INT >= 33)
                b.getParcelable("signature_uri", Uri::class.java)
            else @Suppress("DEPRECATION") b.getParcelable("signature_uri")

        // ✅ Certificate text
        binding.tvTitle.text = "COLLEGE CERTIFICATE"

        binding.tvBody.text = """
            This is to certify that Mr./Ms. $name — S/O or D/O of Mr./Ms. $parent —
            bearing roll no $roll — is a student of $sem — $dept —
            for the academic year ${academicYear()}.
            He/She is a bonafide student of $college.
        """.trimIndent()

        binding.tvWarn.text = "NOTE: You currently have $arrears pending arrear(s)."

        binding.tvSteps.text = """
            • Register for the next backlog exam
            • Meet department office for eligibility
            • Pay exam fees before last date
            • Follow timetable & hall-ticket rules
        """.trimIndent()

        binding.tvDate.text = "Date: ${today()}"

        signatureUri?.let { binding.ivSignature.setImageURI(it) }

        binding.tvCollege.text = college

        // ✅ QR Code
        val qrData = b.getString("qr_data") ?: ""
        if (qrData.isNotBlank()) {
            try {
                val encoder = BarcodeEncoder()
                val bmp: Bitmap = encoder.encodeBitmap(
                    qrData,
                    BarcodeFormat.QR_CODE,
                    800,
                    800
                )
                binding.imgQr.setImageBitmap(bmp)
                binding.tvScanToVerify.text = "Scan to verify"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ✅ SAF-compatible bitmap loader
    private fun loadBitmapFromUri(uri: Uri, maxSizePx: Int = 512): Bitmap? {
        return try {
            requireContext().contentResolver.openInputStream(uri).use { input ->
                if (input == null) return null
                val original = android.graphics.BitmapFactory.decodeStream(input) ?: return null

                val w = original.width
                val h = original.height
                val scale = (maxOf(w, h).toFloat() / maxSizePx).coerceAtLeast(1f)
                val newW = (w / scale).toInt()
                val newH = (h / scale).toInt()

                Bitmap.createScaledBitmap(original, newW, newH, true)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun academicYear(): String {
        val c = Calendar.getInstance()
        val y = c.get(Calendar.YEAR)
        val start = if (c.get(Calendar.MONTH) >= Calendar.JUNE) y else y - 1
        return "$start–${start + 1}"
    }

    private fun today(): String =
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}