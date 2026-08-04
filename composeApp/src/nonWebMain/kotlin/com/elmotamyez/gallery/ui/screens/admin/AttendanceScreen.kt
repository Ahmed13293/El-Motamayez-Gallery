package com.elmotamyez.gallery.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.elmotamyez.gallery.data.model.Attendance
import com.elmotamyez.gallery.data.repository.UserSummary
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

class AttendanceScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val vm: AttendanceViewModel = koinInject()
        val records   by vm.records.collectAsState()
        val users     by vm.users.collectAsState()
        val isLoading by vm.isLoading.collectAsState()
        val error     by vm.error.collectAsState()

        var showAddDialog  by remember { mutableStateOf(false) }
        var editingRecord  by remember { mutableStateOf<Attendance?>(null) }

        LaunchedEffect(Unit) { vm.load() }

        if (showAddDialog) {
            AddEditAttendanceDialog(
                record   = null,
                users    = users,
                onDismiss = { showAddDialog = false },
                onSave   = { userId, userName, checkIn, checkOut ->
                    vm.addRecord(userId, userName, checkIn, checkOut)
                    showAddDialog = false
                }
            )
        }

        editingRecord?.let { rec ->
            AddEditAttendanceDialog(
                record   = rec,
                users    = users,
                onDismiss = { editingRecord = null },
                onSave   = { _, _, checkIn, checkOut ->
                    vm.updateRecord(rec.id, checkIn, checkOut)
                    editingRecord = null
                }
            )
        }

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
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "إضافة سجل")
                }
            }
        ) { padding ->
            when {
                isLoading -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                error != null -> Box(
                    Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "خطأ في تحميل البيانات",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            error ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { vm.load() }) { Text("إعادة المحاولة") }
                    }
                }

                records.isEmpty() -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("لا توجد سجلات حضور", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { vm.testInsert() }) {
                            Text("إدراج سجل تجريبي")
                        }
                    }
                }

                else -> {
                    val activeCount = records.count { it.checkOut == null }
                    val totalHours  = records.mapNotNull { it.hoursWorked }.sum()

                    LazyColumn(
                        contentPadding = PaddingValues(
                            start  = 16.dp,
                            end    = 16.dp,
                            top    = padding.calculateTopPadding() + 8.dp,
                            bottom = padding.calculateBottomPadding() + 88.dp
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
                            AttendanceRecordCard(record, onEdit = { editingRecord = record })
                        }
                    }
                }
            }
        }
    }
}

// ─── Datetime helpers ────────────────────────────────────────────────────────

private fun todayDateStr(): String {
    val dt = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return "${fmt2(dt.dayOfMonth)}/${fmt2(dt.monthNumber)}/${dt.year}"
}

private fun isoToDateStr(iso: String): String = runCatching {
    val dt = Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault())
    "${fmt2(dt.dayOfMonth)}/${fmt2(dt.monthNumber)}/${dt.year}"
}.getOrDefault(todayDateStr())

private fun isoToTimeStr(iso: String): String = runCatching {
    val dt = Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault())
    "${fmt2(dt.hour)}:${fmt2(dt.minute)}"
}.getOrDefault("")

private fun buildIso(dateStr: String, timeStr: String): String? = runCatching {
    val (day, month, year) = dateStr.trim().split("/").map { it.toInt() }
    val (hour, minute)     = timeStr.trim().split(":").map { it.toInt() }
    LocalDateTime(year = year, monthNumber = month, dayOfMonth = day,
        hour = hour, minute = minute, second = 0, nanosecond = 0)
        .toInstant(TimeZone.currentSystemDefault()).toString()
}.getOrNull()

private fun fmt2(n: Int) = n.toString().padStart(2, '0')

// ─── Add / Edit dialog ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditAttendanceDialog(
    record: Attendance?,
    users: List<UserSummary>,
    onDismiss: () -> Unit,
    onSave: (userId: String, userName: String, checkIn: String, checkOut: String?) -> Unit
) {
    val isEdit = record != null

    var selectedUser  by remember { mutableStateOf(users.firstOrNull()) }
    var showUserMenu  by remember { mutableStateOf(false) }
    var dateStr       by remember { mutableStateOf(if (isEdit) isoToDateStr(record!!.checkIn) else todayDateStr()) }
    var checkInTime   by remember { mutableStateOf(if (isEdit) isoToTimeStr(record!!.checkIn) else "") }
    var checkOutTime  by remember { mutableStateOf(if (isEdit && record!!.checkOut != null) isoToTimeStr(record.checkOut!!) else "") }

    val canSave = dateStr.isNotBlank() && checkInTime.isNotBlank() &&
        buildIso(dateStr, checkInTime) != null &&
        (isEdit || selectedUser != null)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "تعديل سجل الحضور" else "إضافة سجل حضور") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                if (isEdit) {
                    Text(
                        "الموظف: ${record!!.userName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = showUserMenu,
                        onExpandedChange = { showUserMenu = it }
                    ) {
                        OutlinedTextField(
                            value = selectedUser?.name ?: "اختر موظفاً",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الموظف") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showUserMenu) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = showUserMenu,
                            onDismissRequest = { showUserMenu = false }
                        ) {
                            users.forEach { user ->
                                DropdownMenuItem(
                                    text = { Text(user.name) },
                                    onClick = { selectedUser = user; showUserMenu = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    label = { Text("التاريخ") },
                    placeholder = { Text("يوم/شهر/سنة — مثال: 15/01/2024") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = checkInTime,
                    onValueChange = { checkInTime = it },
                    label = { Text("وقت الدخول") },
                    placeholder = { Text("مثال: 09:00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = checkOutTime,
                    onValueChange = { checkOutTime = it },
                    label = { Text("وقت الخروج (اختياري)") },
                    placeholder = { Text("مثال: 17:00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = canSave,
                onClick = {
                    val checkIn  = buildIso(dateStr, checkInTime) ?: return@Button
                    val checkOut = checkOutTime.trim().takeIf { it.isNotBlank() }?.let { buildIso(dateStr, it) }
                    val userId   = if (isEdit) record!!.userId   else selectedUser?.id   ?: return@Button
                    val userName = if (isEdit) record!!.userName else selectedUser?.name ?: return@Button
                    onSave(userId, userName, checkIn, checkOut)
                }
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

// ─── Summary chip ────────────────────────────────────────────────────────────

private fun formatHoursShort(h: Double): String {
    val sign  = if (h < 0) "-" else ""
    val total = kotlin.math.abs(h)
    val hours = total.toInt()
    val mins  = ((total - hours) * 60).toInt()
    return when {
        hours > 0 && mins > 0 -> "$sign$hours س $mins د"
        hours > 0              -> "$sign$hours ساعة"
        else                   -> "$sign$mins دقيقة"
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

// ─── Record card ─────────────────────────────────────────────────────────────

@Composable
private fun AttendanceRecordCard(record: Attendance, onEdit: () -> Unit) {
    val tz         = TimeZone.currentSystemDefault()
    val checkInDt  = runCatching { Instant.parse(record.checkIn).toLocalDateTime(tz) }.getOrNull()
    val checkOutDt = record.checkOut?.let { runCatching { Instant.parse(it).toLocalDateTime(tz) }.getOrNull() }

    fun timeStr(dt: kotlinx.datetime.LocalDateTime?) = dt?.let { "${fmt2(it.hour)}:${fmt2(it.minute)}" } ?: "--:--"
    fun dateStr(dt: kotlinx.datetime.LocalDateTime?) = dt?.let { "${fmt2(it.dayOfMonth)}/${fmt2(it.monthNumber)}/${it.year}" } ?: ""

    Card(
        shape     = RoundedCornerShape(12.dp),
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
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
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "تعديل",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
