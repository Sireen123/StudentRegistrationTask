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
import com.example.studentregistration.databinding.FragmentFinalCertificateBinding
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FinalCertificateFragment : Fragment() {

    private var _binding: FragmentFinalCertificateBinding? = null
    private val binding get() = _binding!!

    private val todayFormatter = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinalCertificateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments ?: return

        // ✅ Receive User
        val user = args.getParcelable<User>("user") ?: return
        val collegeName = args.getString("collegeName") ?: ""
        val signatureUri =
            if (Build.VERSION.SDK_INT >= 33)
                args.getParcelable("signature_uri", Uri::class.java)
            else @Suppress("DEPRECATION")
            args.getParcelable("signature_uri")

        val profilePhotoUri = user.profilePhoto?.let { Uri.parse(it) }
        val qrData = args.getString("qr_data") ?: ""

        // ---------------- PROFILE PHOTO ----------------
        if (profilePhotoUri != null) {
            loadAndBindProfile(profilePhotoUri)
        } else {
            binding.imgProfile.visibility = View.GONE
        }

        // ---------------- CERTIFICATE CONTENT ----------------
        binding.tvTitle.text = "COLLEGE CERTIFICATE"

        binding.tvBody.text = """
            This is to certify that Mr./Ms. ${user.name} — S/O or D/O of Mr./Ms. ${user.parentName} —
            bearing roll no ${user.rollNo} — is a student of ${user.semester} — ${user.department} —
            for the academic year ${academicYear()}.
            He/She is a bonafide student of $collegeName.
        """.trimIndent()

        binding.tvDate.text = "Date: ${todayFormatter.format(Date())}"
        binding.tvCollege.text = collegeName

        signatureUri?.let { binding.ivSignature.setImageURI(it) }

        // ---------------- QR CODE ----------------
        if (qrData.isNotBlank()) {
            try {
                val size = (160 * resources.displayMetrics.density).toInt().coerceAtLeast(256)
                val bmp = BarcodeEncoder().encodeBitmap(qrData, BarcodeFormat.QR_CODE, size, size)
                binding.imgQr.setImageBitmap(bmp)
                binding.tvScanToVerify.text = "Scan to verify"
                binding.tvScanToVerify.visibility = View.VISIBLE
            } catch (_: Exception) {
                binding.imgQr.visibility = View.GONE
                binding.tvScanToVerify.visibility = View.GONE
            }
        } else {
            binding.imgQr.visibility = View.GONE
            binding.tvScanToVerify.visibility = View.GONE
        }
    }

    // ✅ Load & rotate image (EXIF-aware)
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

    private fun loadBitmap(uri: Uri, maxSize: Int = 1024): Bitmap? {
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
            val scale = (maxOf(w, h).toFloat() / maxSize).coerceAtLeast(1f)

            val scaled = if (scale > 1f)
                Bitmap.createScaledBitmap(original, (w / scale).toInt(), (h / scale).toInt(), true)
            else original

            val matrix = Matrix().apply {
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                }
            }

            if (!matrix.isIdentity)
                Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
            else scaled

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