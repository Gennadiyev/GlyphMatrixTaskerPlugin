package com.glyphmatrix.tasker.util

/**
 * Utility object for encoding/decoding Glyph Matrix data.
 *
 * Format: 625 characters (25x25 grid), each character is 0-F (hex brightness)
 * - '0' = LED off (brightness 0)
 * - 'F' = maximum brightness (255)
 *
 * Supports both:
 * - Single 625-char string (compact)
 * - 25 lines of 25 chars each (with newlines)
 */
object GlyphEncoder {
    const val GRID_SIZE = 25
    const val TOTAL_PIXELS = GRID_SIZE * GRID_SIZE

    /**
     * Validation result with detailed error message.
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String = ""
    )

    /**
     * Convert a hex character (0-F) to brightness value (0-255).
     */
    fun hexToBrightness(c: Char): Int {
        val hexValue = when (c.uppercaseChar()) {
            in '0'..'9' -> c - '0'
            in 'A'..'F' -> c - 'A' + 10
            else -> 0
        }
        // Map 0-15 to 0-255
        return (hexValue * 255) / 15
    }

    /**
     * Convert a brightness value (0-255) to hex character (0-F).
     */
    fun brightnessToHex(brightness: Int): Char {
        val clamped = brightness.coerceIn(0, 255)
        // Map 0-255 to 0-15
        val hexValue = (clamped * 15) / 255
        return if (hexValue < 10) ('0' + hexValue) else ('A' + hexValue - 10)
    }

    /**
     * Decode a 625-char encoded string (with optional newlines) to brightness array.
     * Returns IntArray of size 625 with values 0-255.
     */
    fun decode(encoded: String): IntArray {
        val result = IntArray(TOTAL_PIXELS) { 0 }

        // Remove all whitespace/newlines and extract only valid hex chars
        val cleanedChars = encoded.filter { it.uppercaseChar() in '0'..'9' || it.uppercaseChar() in 'A'..'F' }

        for (i in 0 until minOf(cleanedChars.length, TOTAL_PIXELS)) {
            result[i] = hexToBrightness(cleanedChars[i])
        }

        return result
    }

    /**
     * Encode brightness array to 625-char string with newlines (25 chars per line).
     */
    fun encode(brightness: IntArray): String {
        val sb = StringBuilder()
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val index = row * GRID_SIZE + col
                val value = if (index < brightness.size) brightness[index] else 0
                sb.append(brightnessToHex(value))
            }
            if (row < GRID_SIZE - 1) {
                sb.append('\n')
            }
        }
        return sb.toString()
    }

    /**
     * Encode brightness array to 625-char string without newlines (compact).
     */
    fun encodeCompact(brightness: IntArray): String {
        val sb = StringBuilder()
        for (i in 0 until TOTAL_PIXELS) {
            val value = if (i < brightness.size) brightness[i] else 0
            sb.append(brightnessToHex(value))
        }
        return sb.toString()
    }

    /**
     * Create an empty (all zeros) brightness array.
     */
    fun createEmpty(): IntArray = IntArray(TOTAL_PIXELS) { 0 }

    /**
     * Create a filled (all max brightness) array.
     */
    fun createFilled(brightness: Int = 255): IntArray = IntArray(TOTAL_PIXELS) { brightness }

    /**
     * Validate if a string is a valid glyph encoding.
     */
    fun isValidEncoding(encoded: String): Boolean {
        val cleanedChars = encoded.filter { it.uppercaseChar() in '0'..'9' || it.uppercaseChar() in 'A'..'F' }
        return cleanedChars.length == TOTAL_PIXELS
    }

    /**
     * Validate input string and return detailed error message if invalid.
     */
    fun validate(encoded: String): ValidationResult {
        // Check for invalid characters (non-hex, non-whitespace)
        val invalidChars = encoded.filter { c ->
            !c.isWhitespace() &&
            c.uppercaseChar() !in '0'..'9' &&
            c.uppercaseChar() !in 'A'..'F'
        }

        if (invalidChars.isNotEmpty()) {
            val preview = if (invalidChars.length > 5) "${invalidChars.take(5)}..." else invalidChars
            return ValidationResult(
                isValid = false,
                errorMessage = "Invalid characters found: '$preview'. Only hex digits (0-9, A-F) and whitespace are allowed."
            )
        }

        // Count valid hex characters
        val hexChars = encoded.filter { it.uppercaseChar() in '0'..'9' || it.uppercaseChar() in 'A'..'F' }
        val hexCount = hexChars.length

        if (hexCount < TOTAL_PIXELS) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Not enough hex digits: found $hexCount, need $TOTAL_PIXELS (25x25 grid)."
            )
        }

        if (hexCount > TOTAL_PIXELS) {
            return ValidationResult(
                isValid = false,
                errorMessage = "Too many hex digits: found $hexCount, need exactly $TOTAL_PIXELS (25x25 grid)."
            )
        }

        return ValidationResult(isValid = true)
    }
}
