package com.example.studentregistration

import android.graphics.Bitmap
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.studentregistration.databinding.ActivityQrBinding
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

class QrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val qrData = intent.getStringExtra("qr_data") ?: "No Data"
        generateQR(qrData)
    }

    private fun generateQR(text: String) {
        val encoder = BarcodeEncoder()
        val bitmap: Bitmap = encoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 600, 600)
        binding.imgQr.setImageBitmap(bitmap)
    }
}
