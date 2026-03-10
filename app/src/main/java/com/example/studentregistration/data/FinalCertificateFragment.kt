package com.example.studentregistration

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.studentregistration.databinding.FragmentFinalCertificateBinding
import java.text.SimpleDateFormat
import java.util.*

class FinalCertificateFragment : Fragment() {

    private var _binding: FragmentFinalCertificateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFinalCertificateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val b = requireArguments()

        val name = b.getString("name", "")
        val parent = b.getString("parent", "")
        val roll = b.getString("roll", "")
        val sem = b.getString("sem", "")
        val dept = b.getString("dept", "")
        val college = b.getString("collegeName", "")

        val signatureUri: Uri? =
            if (Build.VERSION.SDK_INT >= 33)
                b.getParcelable("signature_uri", Uri::class.java)
            else
                @Suppress("DEPRECATION")
                b.getParcelable("signature_uri")

        binding.tvTitle.text = "COLLEGE CERTIFICATE"

        binding.tvBody.text = """
            This is to certify that Mr./Ms. $name — S/O or D/O of Mr./Ms. $parent —
            bearing roll no $roll — is a student of $sem — $dept —
            for the academic year ${academicYear()}. 
            He/She is a bonafide student of $college.
        """.trimIndent()

        binding.tvDate.text = "Date: ${today()}"

        signatureUri?.let { binding.ivSignature.setImageURI(it) }

        binding.tvCollege.text = college
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