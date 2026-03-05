package com.example.studentregistration

import android.text.InputFilter
import android.text.Spanned

/**
 * Limits ONLY digit count. Spaces or formatting characters DO NOT count.
 */
class DigitMaxLengthFilter(private val maxDigits: Int) : InputFilter {

    override fun filter(
        source: CharSequence?, start: Int, end: Int,
        dest: Spanned?, dstart: Int, dend: Int
    ): CharSequence? {

        val destStr = dest?.toString() ?: ""

        val existingDigits = destStr.filter { it.isDigit() }.length
        val replacedDigits = destStr.substring(dstart, dend).filter { it.isDigit() }.length

        var newDigits = 0
        if (source != null) {
            for (i in start until end) {
                if (source[i].isDigit()) newDigits++
            }
        }

        val allowedDigits = maxDigits - (existingDigits - replacedDigits)

        if (allowedDigits <= 0) {
            return "" // block entire insertion
        }

        // Allow only the part of source that fits into remaining digit count
        val result = StringBuilder()
        var added = 0

        if (source != null) {
            for (i in start until end) {
                val ch = source[i]
                if (ch.isDigit()) {
                    if (added >= allowedDigits) break
                    result.append(ch)
                    added++
                } else {
                    // Keep spaces or other formatting
                    result.append(ch)
                }
            }
        }

        return result.toString()
    }
}