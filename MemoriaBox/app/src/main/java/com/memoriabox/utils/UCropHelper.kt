package com.memoriabox.utils

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.model.AspectRatio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object UCropHelper {
    private const val TAG = "UCropHelper"

    data class RatioOption(val title: String, val x: Float, val y: Float)

    @Composable
    fun rememberCropLauncher(
        folder: String,
        onResult: (String?) -> Unit
    ): (Uri, Float) -> Unit {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleCropResult(result, context, scope, folder, onResult)
        }

        return remember(launcher, folder) {
            { sourceUri: Uri, aspectRatio: Float ->
                val cacheDir = File(context.cacheDir, "ucrop").apply { mkdirs() }
                val destination = Uri.fromFile(File(cacheDir, "crop_${System.currentTimeMillis()}.jpg"))

                val options = UCrop.Options().apply {
                    setCompressionQuality(92)
                    setToolbarColor(0xFF7C4DFF.toInt())
                    setStatusBarColor(0xFF3700B3.toInt())
                    setToolbarWidgetColor(android.graphics.Color.WHITE)
                    setRootViewBackgroundColor(android.graphics.Color.WHITE)
                    setActiveControlsWidgetColor(0xFF7C4DFF.toInt())
                    setToolbarTitle("裁剪图片")
                    setLogoColor(0xFF7C4DFF.toInt())
                    if (aspectRatio <= 0f) {
                        setFreeStyleCropEnabled(true)
                    }
                }

                val uCrop = UCrop.of(sourceUri, destination)
                    .withOptions(options)
                    .withMaxResultSize(1920, 1920)

                if (aspectRatio > 0f) {
                    uCrop.withAspectRatio(aspectRatio, 1f)
                }

                launcher.launch(uCrop.getIntent(context))
            }
        }
    }

    @Composable
    fun rememberRatioCropLauncher(
        folder: String,
        onResult: (String?) -> Unit,
        options: List<RatioOption>,
        defaultIndex: Int = 0
    ): (Uri) -> Unit {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleCropResult(result, context, scope, folder, onResult)
        }

        return remember(launcher, folder, options, defaultIndex) {
            { sourceUri: Uri ->
                val cacheDir = File(context.cacheDir, "ucrop").apply { mkdirs() }
                val destination = Uri.fromFile(File(cacheDir, "crop_${System.currentTimeMillis()}.jpg"))

                val ratioList = options.map { AspectRatio(it.title, it.x, it.y) }.toTypedArray()
                val safeIndex = defaultIndex.coerceIn(0, ratioList.size - 1)

                val uCropOptions = UCrop.Options().apply {
                    setCompressionQuality(92)
                    setToolbarColor(0xFF7C4DFF.toInt())
                    setStatusBarColor(0xFF3700B3.toInt())
                    setToolbarWidgetColor(android.graphics.Color.WHITE)
                    setRootViewBackgroundColor(android.graphics.Color.WHITE)
                    setActiveControlsWidgetColor(0xFF7C4DFF.toInt())
                    setToolbarTitle("裁剪图片")
                    setLogoColor(0xFF7C4DFF.toInt())
                    setFreeStyleCropEnabled(true)
                    setAspectRatioOptions(safeIndex, *ratioList)
                }

                val uCrop = UCrop.of(sourceUri, destination)
                    .withOptions(uCropOptions)
                    .withMaxResultSize(1920, 1920)

                launcher.launch(uCrop.getIntent(context))
            }
        }
    }

    private fun handleCropResult(
        result: androidx.activity.result.ActivityResult,
        context: Context,
        scope: CoroutineScope,
        folder: String,
        onResult: (String?) -> Unit
    ) {
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            if (resultUri != null) {
                scope.launch(Dispatchers.IO) {
                    val finalPath = copyToDestination(context, resultUri, folder)
                    runCatching { resultUri.path?.let { File(it).delete() } }
                    onResult(finalPath)
                }
            } else {
                onResult(null)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR && result.data != null) {
            val error = UCrop.getError(result.data!!)
            Log.e(TAG, "uCrop error: ${error?.message}")
            onResult(null)
        } else {
            onResult(null)
        }
    }

    private fun copyToDestination(context: Context, sourceUri: Uri, folder: String): String? {
        return runCatching {
            val dir = File(context.filesDir, folder).apply { mkdirs() }
            val target = File(dir, "image_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            Uri.fromFile(target).toString()
        }.getOrNull()
    }
}
