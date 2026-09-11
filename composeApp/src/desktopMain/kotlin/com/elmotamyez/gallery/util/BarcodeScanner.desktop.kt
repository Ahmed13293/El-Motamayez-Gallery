package com.elmotamyez.gallery.util

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun BarcodeScannerSheet(onResult: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("مسح الباركود غير متاح على سطح المكتب",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline)
            TextButton(onClick = onDismiss) { Text("إغلاق") }
        }
    }
}
