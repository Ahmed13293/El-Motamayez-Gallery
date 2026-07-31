package com.elmotamyez.gallery.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.data.model.Attendance
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

class AttendanceScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: AttendanceViewModel = koinInject()
        val records   by vm.records.collectAsState()
        val isLoading by vm.isLoading.collectAsState()

        LaunchedEffect(Unit) { vm.load() }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("تقرير الحضور", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    }
                )
            }
        ) { padding ->
            when {
                isLoading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                records.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("لا توجد سجلات حضور", color = MaterialTheme.colorScheme.onSurfaceVariant) }

                else -> {
                    val activeCount  = records.count { it.checkOut == null }
                    val totalHours   = records.mapNotNull { it.hoursWorked }.sum()

                    LazyColumn(
                        contentPadding = PaddingValues(
                            start  = 16.dp,
                            end    = 16.dp,
                            top    = padding.calculateTopPadding() + 8.dp,
                            bottom = padding.calculateBottomPadding() + 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AttendanceSummaryChip("حاضر الآن", "$activeCount", Modifier.weight(1f))
                                AttendanceSummaryChip("إجمالي الساعات", formatHoursShort(totalHours), Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        items(records, key = { it.id }) { record ->
                            AttendanceRecordCard(record)
                        }
                    }
                }
            }
        }
    }
}

private fun formatHoursShort(h: Double): String {
    val hours = h.toInt()
    val mins  = ((h - hours) * 60).toInt()
    return when {
        hours > 0 && mins > 0 -> "$hours س $mins د"
        hours > 0              -> "$hours ساعة"
        else                   -> "$mins دقيقة"
    }
}

@Composable
private fun AttendanceSummaryChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun AttendanceRecordCard(record: Attendance) {
    val tz         = TimeZone.currentSystemDefault()
    val checkInDt  = runCatching { Instant.parse(record.checkIn).toLocalDateTime(tz) }.getOrNull()
    val checkOutDt = record.checkOut?.let { runCatching { Instant.parse(it).toLocalDateTime(tz) }.getOrNull() }

    fun fmt2(n: Int) = n.toString().padStart(2, '0')
    fun timeStr(dt: LocalDateTime?) = dt?.let { "${fmt2(it.hour)}:${fmt2(it.minute)}" } ?: "--:--"
    fun dateStr(dt: LocalDateTime?) = dt?.let { "${fmt2(it.dayOfMonth)}/${fmt2(it.monthNumber)}/${it.year}" } ?: ""

    Card(
        shape  = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier  = Modifier.fillMaxWidth(),
        colors = if (record.checkOut == null)
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        else
            CardDefaults.cardColors()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(record.userName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

                if (record.checkOut == null) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) {
                        Text(
                            "حاضر",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    record.hoursWorked?.let { h ->
                        Text(
                            formatHoursShort(h),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Text(
                dateStr(checkInDt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                AttendanceTimeLabel("دخول", timeStr(checkInDt))
                AttendanceTimeLabel("خروج", timeStr(checkOutDt))
            }
        }
    }
}

@Composable
private fun AttendanceTimeLabel(label: String, time: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
