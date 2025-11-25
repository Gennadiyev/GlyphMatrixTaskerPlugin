package com.glyphmatrix.tasker.actions

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.glyphmatrix.tasker.util.GlyphEncoder
import com.glyphmatrix.tasker.util.GlyphMatrixHelper

/**
 * Tasker action to update the Glyph Matrix with a pattern.
 * Input: 625-char hex string (0-F per pixel, supports newlines)
 */

@TaskerInputRoot
class UpdateGlyphInput @JvmOverloads constructor(
    @field:TaskerInputField("glyphData", labelResIdName = "glyph_data_label", descriptionResIdName = "glyph_data_description")
    var glyphData: String? = null
)

class UpdateGlyphHelper(config: TaskerPluginConfig<UpdateGlyphInput>) :
    TaskerPluginConfigHelper<UpdateGlyphInput, Unit, UpdateGlyphRunner>(config) {

    override val runnerClass: Class<UpdateGlyphRunner> get() = UpdateGlyphRunner::class.java
    override val inputClass: Class<UpdateGlyphInput> get() = UpdateGlyphInput::class.java
    override val outputClass: Class<Unit> get() = Unit::class.java

    override fun addToStringBlurb(input: TaskerInput<UpdateGlyphInput>, blurbBuilder: StringBuilder) {
        val data = input.regular.glyphData
        if (!data.isNullOrEmpty()) {
            val preview = if (data.length > 30) "${data.take(30)}..." else data
            blurbBuilder.append("Pattern: $preview")
        } else {
            blurbBuilder.append("No pattern set")
        }
    }
}

/**
 * Config activity that shows a simple input dialog for glyph data.
 * Supports Tasker variables like %glyph which will be substituted at runtime.
 */
class UpdateGlyphConfigActivity : Activity(), TaskerPluginConfig<UpdateGlyphInput> {
    override val context: Context get() = applicationContext

    private val taskerHelper by lazy { UpdateGlyphHelper(this) }

    private var currentGlyphData: String? = null

    override val inputForTasker: TaskerInput<UpdateGlyphInput>
        get() = TaskerInput(UpdateGlyphInput(currentGlyphData))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize helper and get any existing input
        taskerHelper.onCreate()
    }

    override fun assignFromInput(input: TaskerInput<UpdateGlyphInput>) {
        currentGlyphData = input.regular.glyphData
        showInputDialog()
    }

    private fun showInputDialog() {
        val padding = (16 * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val hint = TextView(this).apply {
            text = "Enter glyph pattern (625 hex chars) or a Tasker variable like %glyph"
            textSize = 12f
            setPadding(0, 0, 0, padding / 2)
        }
        layout.addView(hint)

        val editText = EditText(this).apply {
            setText(currentGlyphData ?: "")
            setHint("%glyph")
            isSingleLine = true
        }
        layout.addView(editText)

        AlertDialog.Builder(this)
            .setTitle("Update Glyph Matrix")
            .setView(layout)
            .setPositiveButton("OK") { _, _ ->
                currentGlyphData = editText.text.toString().ifBlank { null }
                taskerHelper.finishForTasker()
            }
            .setNegativeButton("Cancel") { _, _ ->
                finish()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.repeatCount == 0) {
            taskerHelper.onBackPressed()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}

class UpdateGlyphRunner : TaskerPluginRunnerAction<UpdateGlyphInput, Unit>() {
    override fun run(context: Context, input: TaskerInput<UpdateGlyphInput>): TaskerPluginResult<Unit> {
        val glyphData = input.regular.glyphData

        // Validate input
        if (glyphData.isNullOrBlank()) {
            return TaskerPluginResultError(1, "Glyph data is empty. Please provide a 625-character hex string (0-F).")
        }

        // Validate format
        val validationResult = GlyphEncoder.validate(glyphData)
        if (!validationResult.isValid) {
            return TaskerPluginResultError(2, validationResult.errorMessage)
        }

        return try {
            val brightnessData = GlyphEncoder.decode(glyphData)

            // Use blocking call for Tasker
            val helper = GlyphMatrixHelper.getInstance(context)
            val success = helper.updateMatrix(brightnessData)

            if (success) {
                TaskerPluginResultSucess()
            } else {
                TaskerPluginResultError(3, "Failed to connect to Glyph Matrix. Is this a Nothing Phone 3?")
            }
        } catch (e: Exception) {
            TaskerPluginResultError(4, "Error: ${e.message}")
        }
    }
}
