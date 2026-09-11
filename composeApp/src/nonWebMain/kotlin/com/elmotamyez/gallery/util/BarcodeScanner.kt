package com.elmotamyez.gallery.util

import androidx.compose.runtime.Composable

/**
 * Shows a camera sheet that scans barcodes/QR codes.
 * Calls [onResult] once with the raw value of the first barcode detected, then dismisses.
 * Calls [onDismiss] when the user closes the sheet without scanning.
 */
@Composable
expect fun BarcodeScannerSheet(onResult: (String) -> Unit, onDismiss: () -> Unit)
