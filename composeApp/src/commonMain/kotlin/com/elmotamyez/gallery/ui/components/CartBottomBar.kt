package com.elmotamyez.gallery.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elmotamyez.gallery.util.formatPrice

@Composable
fun CartBottomBar(
    totalPrice: Double,
    itemCount: Int,
    onNavigateToCart: () -> Unit
) {
    if (itemCount == 0) return
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("$itemCount منتج", style = MaterialTheme.typography.labelMedium)
                Text(
                    "الإجمالي: ${totalPrice.formatPrice()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(onClick = onNavigateToCart) {
                Text("عرض السلة")
            }
        }
    }
}
