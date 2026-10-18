package com.enterprise.analytics.ui.panels

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.enterprise.analytics.MainViewModel
import com.enterprise.analytics.data.Reminder
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PlannerPanel(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var showAddDialog by remember { mutableStateOf(false) }
    val reminders by viewModel.reminders.collectAsState()
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text("Your Reminders", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))

            if (reminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No reminders scheduled.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(reminders) { reminder ->
                        ReminderItem(reminder)
                    }
                }
            }
        }

        if (showAddDialog) {
            AddReminderDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { title, timeInMillis, isRecurring, recurrenceType, videoUri ->
                    viewModel.addReminder(context, title, timeInMillis, isRecurring, recurrenceType, videoUri)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun ReminderItem(reminder: Reminder) {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val dateString = formatter.format(Date(reminder.dateInMillis))

    Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = reminder.title, style = MaterialTheme.typography.titleMedium)
            Text(text = "Date: $dateString", style = MaterialTheme.typography.bodyMedium)
            if (reminder.isRecurring) {
                Text(text = "Recurring: ${reminder.recurrenceType}", style = MaterialTheme.typography.bodySmall)
            }
            if (reminder.videoUri != null) {
                Text(text = "Video attached", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Long, Boolean, String, String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrenceType by remember { mutableStateOf("NONE") }

    var selectedDateInMillis by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current

    val calendar = Calendar.getInstance()

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            selectedDateInMillis = calendar.timeInMillis
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            timePickerDialog.show()
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistable permission
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedVideoUri = it
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { datePickerDialog.show() }) {
                    Text(if (selectedDateInMillis != null) {
                        val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                        "Time: " + formatter.format(Date(selectedDateInMillis!!))
                    } else "Select Date & Time")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                    Text("Recurring")
                }

                if (isRecurring) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = recurrenceType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Recurrence") },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("DAILY") },
                                onClick = { recurrenceType = "DAILY"; expanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("WEEKLY") },
                                onClick = { recurrenceType = "WEEKLY"; expanded = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = { videoPickerLauncher.launch(arrayOf("video/*")) }) {
                    Text(if (selectedVideoUri == null) "Attach Video" else "Video Selected")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedDateInMillis != null) {
                        onAdd(title, selectedDateInMillis!!, isRecurring, recurrenceType, selectedVideoUri?.toString())
                    }
                },
                enabled = title.isNotBlank() && selectedDateInMillis != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
