package com.example.foodrecognition.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CameraScreen(onImageCaptured: (Uri) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        Box(modifier = Modifier.fillMaxSize()) {
            val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    imageCapture = ImageCapture.Builder().build()
                    val selector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                    } catch (exc: Exception) {
                        Log.e("CameraScreen", "Camera bind failed", exc)
                    }
                    previewView
                }
            )

            Button(
                onClick = { takePhoto(context, imageCapture, onImageCaptured) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                Text("📷 Take Photo")
            }
        }
    } else {
        Text(
            "Camera permission required",
            modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
        )
    }
}

private fun takePhoto(context: Context, imageCapture: ImageCapture?, onImageCaptured: (Uri) -> Unit) {
    val photoFile = File(
        context.externalCacheDir,
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
    )
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    imageCapture?.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(context, "Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                Toast.makeText(context, "Saved: $savedUri", Toast.LENGTH_SHORT).show()
                onImageCaptured(savedUri)
            }
        }
    )
}

