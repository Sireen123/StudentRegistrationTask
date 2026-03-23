package com.example.studentregistration

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.studentregistration.data.User
import com.example.studentregistration.databinding.FragmentArrearCertificateBinding
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ArrearCertificateFragment : Fragment() {

    private var _binding: FragmentArrearCertificateBinding? = null
    private val binding get() = _binding!!

    private val todayFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentArrearCertificateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments ?: return

        // ✅ Receive User parcelable
        val user = args.getParcelable<User>("user") ?: return
        val college = args.getString("collegeName") ?: ""
        val arrears = args.getInt("arrearsCount", 0)

        val signatureUri: Uri? =
            if (Build.VERSION.SDK_INT >= 33)
                args.getParcelable("signature_uri", Uri::class.java)
            else @Suppress("DEPRECATION")
            args.getParcelable("signature_uri")

        val profilePhotoUriStr = user.profilePhoto
        val qrData = args.getString("qr_data", "")

        // ---------------- Profile Photo ----------------
        if (!profilePhotoUriStr.isNullOrBlank()) {
            val uri = Uri.parse(profilePhotoUriStr)
            loadAndBindProfile(uri)
        } else {
            binding.imgProfile.visibility = View.GONE
        }

        // ---------------- Certificate Body ----------------
        binding.tvTitle.text = "COLLEGE CERTIFICATE"

        binding.tvBody.text = """
            This is to certify that Mr./Ms. ${user.name} — S/O or D/O of Mr./Ms. ${user.parentName} —
            bearing roll no ${user.rollNo} — is a student of ${user.semester} — ${user.department} —
            for the academic year ${academicYear()}.
            He/She is a bonafide student of $college.
        """.trimIndent()

        binding.tvWarn.text =
            "NOTE: You currently have $arrears pending ${if (arrears == 1) "arrear" else "arrears"}."

        binding.tvSteps.text = """
            • Register for the next backlog exam
            • Meet department office for eligibility
            • Pay exam fees before last date
            • Follow timetable & hall-ticket rules
        """.trimIndent()

        binding.tvDate.text = "Date: ${todayFormatter.format(Date())}"
        binding.tvCollege.text = college

        signatureUri?.let { binding.ivSignature.setImageURI(it) }

        // ---------------- QR Code ----------------
        if (qrData.isNotBlank()) {
            generateQr(qrData)
        } else {
            binding.imgQr.visibility = View.GONE
            binding.tvScanToVerify.visibility = View.GONE
        }
    }

    // ✅ Load & Rotate Profile Photo (EXIF aware)
    private fun loadAndBindProfile(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val bmp = loadBitmap(uri)
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                if (bmp != null) {
                    binding.imgProfile.setImageBitmap(bmp)
                    binding.imgProfile.visibility = View.VISIBLE
                } else binding.imgProfile.visibility = View.GONE
            }
        }
    }

    // ✅ QR Code
    private fun generateQr(data: String) {
        try {
            val size = (160 * resources.displayMetrics.density).toInt().coerceAtLeast(256)
            val bmp = BarcodeEncoder().encodeBitmap(data, BarcodeFormat.QR_CODE, size, size)
            binding.imgQr.setImageBitmap(bmp)
            binding.imgQr.visibility = View.VISIBLE
            binding.tvScanToVerify.visibility = View.VISIBLE
        } catch (_: Exception) {
            binding.imgQr.visibility = View.GONE
            binding.tvScanToVerify.visibility = View.GONE
        }
    }

    // ✅ EXIF + Bitmap Scaling
    private fun loadBitmap(uri: Uri, maxSizePx: Int = 1024): Bitmap? {
        return try {
            val resolver = requireContext().contentResolver

            val orientation = resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val original = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                ?: return null

            val w = original.width
            val h = original.height
            val scale = (maxOf(w, h).toFloat() / maxSizePx).coerceAtLeast(1f)

            val scaled = if (scale > 1f)
                Bitmap.createScaledBitmap(original, (w / scale).toInt(), (h / scale).toInt(), true)
            else original

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            }

            if (!matrix.isIdentity) {
                Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
            } else scaled

        } catch (_: Exception) {
            null
        }
    }

    private fun academicYear(): String {
        val c = Calendar.getInstance()
        val y = c.get(Calendar.YEAR)
        val start = if (c.get(Calendar.MONTH) >= Calendar.JUNE) y else y - 1
        return "$start–${start + 1}"
    }

    override fun onDestroyView() {
        binding.imgProfile.setImageDrawable(null)
        binding.imgQr.setImageDrawable(null)
        binding.ivSignature.setImageDrawable(null)
        _binding = null
        super.onDestroyView()
    }
}