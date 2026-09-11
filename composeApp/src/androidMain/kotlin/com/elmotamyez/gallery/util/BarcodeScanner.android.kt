package com.elmotamyez.gallery.util

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun BarcodeScannerSheet(onResult: (String) -> Unit, onDismiss: () -> Unit) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission.value = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission.value) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("مسح الباركود", fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp))

            if (!hasPermission.value) {
                Text("يلزم إذن الكاميرا لمسح الباركود",
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("منح الإذن")
                }
            } else {
                // Prevent firing onResult multiple times while the camera is still running
                val resultFired = remember { AtomicBoolean(false) }
                val barcodeScanner = remember { BarcodeScanning.getClient() }

                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    update = { previewView ->
                        val future = ProcessCameraProvider.getInstance(context)
                        future.addListener({
                            val provider = future.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { ia ->
                                    ia.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
                                        val media = proxy.image
                                        if (media != null && !resultFired.get()) {
                                            val img = InputImage.fromMediaImage(
                                                media, proxy.imageInfo.rotationDegrees)
                                            barcodeScanner.process(img)
                                                .addOnSuccessListener { barcodes ->
                                                    val raw = barcodes.firstOrNull()?.rawValue
                                                    if (raw != null && resultFired.compareAndSet(false, true)) {
                                                        onResult(raw)
                                                    }
                                                }
                                                .addOnCompleteListener { proxy.close() }
                                        } else {
                                            proxy.close()
                                        }
                                    }
                                }
                            runCatching {
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview, analysis
                                )
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )

                Text("وجّه الكاميرا نحو الباركود أو رمز QR",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 10.dp))
            }

            TextButton(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) {
                Text("إلغاء")
            }
        }
    }
}
