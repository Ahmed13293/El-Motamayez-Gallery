package com.elmotamyez.gallery.ui.screens.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.ui.components.GradientDivider
import com.elmotamyez.gallery.util.exportReceiptToPdf
import com.elmotamyez.gallery.util.formatPrice
import com.elmotamyez.gallery.util.twoDigit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

class ReceiptScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: ReceiptViewModel = koinInject()
        val receipt       by vm.currentReceipt.collectAsState()
        val items          = receipt?.items         ?: emptyList()
        val total          = receipt?.total         ?: 0.0
        val discount       = receipt?.discount      ?: 0.0
        val subtotal       = total + discount
        val paymentMethod  = receipt?.paymentMethod ?: ""
        val orderNumber   = receipt?.orderNumber
        val customerPhone = receipt?.customerPhone
        val customerInfo  = receipt?.customerInfo
        val dateTimeText = receipt?.createdAt?.let { raw ->
            runCatching {
                val normalized = raw
                    .replace(" ", "T")
                    .replace(Regex("\\.\\d+"), "")
                    .replace(Regex("[+-]\\d{2}(:\\d{2})?$"), "Z")
                    .let { if (!it.endsWith("Z")) "${it}Z" else it }
                val instant = Instant.parse(normalized)
                val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                "${twoDigit(local.dayOfMonth)}/${twoDigit(local.monthNumber)}/${local.year}  ${twoDigit(local.hour)}:${twoDigit(local.minute)}"
            }.getOrElse { raw }
        } ?: ""

        Scaffold { padding ->
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Back + PDF row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                        }
                        TextButton(onClick = {
                            receipt?.let { exportReceiptToPdf(it, "${it.id}.pdf") }
                        }) {
                            Text("إصدار PDF", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                // Header
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "مكتبة المتميز",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "فرع الشيخ زايد",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "فاتورة الطلب",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        if (orderNumber != null) {
                            Text(
                                "رقم الفاتورة: $orderNumber",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (dateTimeText.isNotEmpty()) {
                            Text(
                                "تاريخ الفاتورة: $dateTimeText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (!customerPhone.isNullOrBlank()) {
                            Text(
                                "رقم العميل: $customerPhone",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (!customerInfo.isNullOrBlank()) {
                            Text(
                                "معلومات العميل: $customerInfo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (paymentMethod.isNotEmpty()) {
                            Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                                color = androidx.compose.ui.graphics.Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    "طريقة الدفع: $paymentMethod",
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        GradientDivider()
                    }
                }

                // Column headers
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("المنتج", fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text("الكمية", fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                        Text("السعر", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("الإجمالي", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    GradientDivider()
                }

                // Items
                items(items) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(item.product.name, modifier = Modifier.weight(2f))
                        Text("${item.quantity}", modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                        Text(item.product.price.formatPrice(), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(item.totalPrice.formatPrice(), modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Total section
                item {
                    GradientDivider()
                    Spacer(Modifier.height(6.dp))

                    // Subtotal (only when discount applied)
                    if (discount > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المجموع", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(subtotal.formatPrice(), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الخصم", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error)
                            Text("-${discount.formatPrice()}", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))
                    }

                    // Final total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("الإجمالي", style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                        Text(
                            total.formatPrice(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "شكراً لتسوقكم معنا!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
