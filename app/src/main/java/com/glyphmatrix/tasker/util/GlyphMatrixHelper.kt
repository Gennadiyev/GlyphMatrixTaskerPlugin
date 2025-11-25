package com.glyphmatrix.tasker.util

import android.content.ComponentName
import android.content.Context
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Helper class for interacting with the Glyph Matrix SDK.
 * Uses a singleton pattern to maintain a single connection.
 */
class GlyphMatrixHelper private constructor(private val context: Context) {
    private var manager: GlyphMatrixManager? = null
    @Volatile
    private var isConnected = false
    @Volatile
    private var isInitializing = false

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(name: ComponentName?) {
            manager?.register(Glyph.DEVICE_23112)
            isConnected = true
            isInitializing = false
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isConnected = false
        }
    }

    /**
     * Ensure connection is established. Blocks until connected or timeout.
     */
    @Synchronized
    private fun ensureConnected(): Boolean {
        if (isConnected && manager != null) {
            return true
        }

        if (isInitializing) {
            // Wait for existing initialization
            repeat(50) {
                if (isConnected) return true
                Thread.sleep(100)
            }
            return isConnected
        }

        isInitializing = true

        // Release old manager if exists
        manager?.unInit()
        manager = null

        manager = GlyphMatrixManager.getInstance(context.applicationContext)
        manager?.init(callback)

        // Wait for connection (up to 5 seconds)
        repeat(50) {
            if (isConnected) return true
            Thread.sleep(100)
        }

        isInitializing = false
        return isConnected
    }

    /**
     * Update the Glyph Matrix with brightness data.
     * @param brightnessData Array of 625 brightness values (0-255)
     */
    fun updateMatrix(brightnessData: IntArray): Boolean {
        if (!ensureConnected()) return false
        manager?.setAppMatrixFrame(brightnessData)
        return true
    }

    /**
     * Clear the Glyph Matrix (turn off all LEDs).
     */
    fun clearMatrix(): Boolean {
        if (!ensureConnected()) return false
        manager?.closeAppMatrix()
        return true
    }

    /**
     * Release resources and disconnect.
     */
    @Synchronized
    fun release() {
        manager?.closeAppMatrix()
        manager?.unInit()
        manager = null
        isConnected = false
        isInitializing = false
    }

    companion object {
        @Volatile
        private var instance: GlyphMatrixHelper? = null

        /**
         * Get singleton instance.
         */
        fun getInstance(context: Context): GlyphMatrixHelper {
            return instance ?: synchronized(this) {
                instance ?: GlyphMatrixHelper(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Update the matrix with the given brightness data.
         */
        fun quickUpdate(context: Context, brightnessData: IntArray, onComplete: ((Boolean) -> Unit)? = null) {
            Thread {
                val success = getInstance(context).updateMatrix(brightnessData)
                onComplete?.invoke(success)
            }.start()
        }

        /**
         * Clear the matrix display.
         */
        fun quickClear(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
            Thread {
                val success = getInstance(context).clearMatrix()
                onComplete?.invoke(success)
            }.start()
        }
    }
}
