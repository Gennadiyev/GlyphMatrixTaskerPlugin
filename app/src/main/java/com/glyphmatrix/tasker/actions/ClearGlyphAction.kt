package com.glyphmatrix.tasker.actions

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutputOrInput
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigNoInput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import com.glyphmatrix.tasker.util.GlyphMatrixHelper

/**
 * Tasker action to clear the Glyph Matrix (turn off all LEDs).
 * No input required.
 */

class ClearGlyphHelper(config: TaskerPluginConfig<Unit>) :
    TaskerPluginConfigHelperNoOutputOrInput<ClearGlyphRunner>(config) {

    override val runnerClass: Class<ClearGlyphRunner> get() = ClearGlyphRunner::class.java

    override fun addToStringBlurb(input: TaskerInput<Unit>, blurbBuilder: StringBuilder) {
        blurbBuilder.append("Turn off Glyph Matrix display")
    }
}

class ClearGlyphConfigActivity : Activity(), TaskerPluginConfigNoInput {
    override val context: Context get() = applicationContext

    private val taskerHelper by lazy { ClearGlyphHelper(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // No UI needed - just finish immediately for Tasker
        taskerHelper.finishForTasker()
    }
}

class ClearGlyphRunner : TaskerPluginRunnerActionNoOutputOrInput() {
    override fun run(context: Context, input: TaskerInput<Unit>): TaskerPluginResult<Unit> {
        return try {
            val helper = GlyphMatrixHelper.getInstance(context)
            val success = helper.clearMatrix()

            if (success) {
                TaskerPluginResultSucess()
            } else {
                TaskerPluginResultError(Exception("Failed to connect to Glyph Matrix"))
            }
        } catch (e: Exception) {
            TaskerPluginResultError(e)
        }
    }
}
