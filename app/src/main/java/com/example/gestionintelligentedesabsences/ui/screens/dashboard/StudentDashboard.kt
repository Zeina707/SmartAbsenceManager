package com.example.gestionintelligentedesabsences.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestionintelligentedesabsences.data.model.Student
import com.example.gestionintelligentedesabsences.ui.viewmodel.AttendanceRecord
import com.example.gestionintelligentedesabsences.ui.viewmodel.CourseSchedule
import com.example.gestionintelligentedesabsences.ui.viewmodel.StudentDashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboard(
    onLogout: () -> Unit,
    viewModel: StudentDashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadStudentData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tableau de bord étudiant") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Déconnexion")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.student == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Données de l'étudiant non disponibles",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Student Profile
                item {
                    StudentProfileCard(student = uiState.student)
                }

                // Attendance Summary
                item {
                    AttendanceSummaryCard(
                        attendancePercentage = uiState.attendancePercentage,
                        totalSessions = uiState.totalSessions,
                        presentSessions = uiState.presentSessions
                    )
                }

                // Today's Schedule
                item {
                    TodayScheduleCard(courses = uiState.todayCourses)
                }

                // Attendance History
                item {
                    Text(
                        text = "Historique des présences",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Attendance Records
                items(uiState.attendanceRecords) { record ->
                    AttendanceRecordItem(record = record)
                }

                // Show message if no attendance records
                if (uiState.attendanceRecords.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aucun enregistrement de présence disponible",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Show error if any
        if (uiState.error != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Erreur") },
                text = { Text(uiState.error.toString()) },
                confirmButton = {
                    Button(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun StudentProfileCard(student: Student?) {
    if (student == null) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = student.firstName.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Student Name
            Text(
                text = "${student.firstName} ${student.lastName}",
                style = MaterialTheme.typography.headlineSmall
            )

            // Student ID
            Text(
                text = "ID: ${student.studentId}",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Student Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StudentInfoItem(
                    icon = Icons.Default.Home,
                    label = "Filière",
                    value = student.branch.ifEmpty { "Non spécifiée" }
                )

                StudentInfoItem(
                    icon = Icons.Default.Info,
                    label = "Groupe",
                    value = student.group.ifEmpty { "Non spécifié" }
                )

                StudentInfoItem(
                    icon = Icons.Default.DateRange,
                    label = "Niveau",
                    value = student.level.ifEmpty { "Non spécifié" }
                )
            }
        }
    }
}

@Composable
fun StudentInfoItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun AttendanceSummaryCard(
    attendancePercentage: Float,
    totalSessions: Int,
    presentSessions: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Résumé des présences",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attendance Percentage
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${(attendancePercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Attendance Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Sessions totales: $totalSessions",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Sessions présent: $presentSessions",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Sessions absent: ${totalSessions - presentSessions}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Attendance Progress Bar
            LinearProgressIndicator(
                progress = { attendancePercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun TodayScheduleCard(courses: List<CourseSchedule>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Emploi du temps d'aujourd'hui",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (courses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun cours programmé aujourd'hui",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                courses.forEach { course ->
                    ScheduleItem(course = course)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
fun ScheduleItem(course: CourseSchedule) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = course.startTime,
                style = MaterialTheme.typography.bodyMedium
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = course.endTime,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Course details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = course.courseName,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Prof: ${course.instructor}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Salle: ${course.room}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AttendanceRecordItem(record: AttendanceRecord) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val statusColor = if (record.status == "present")
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Record details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = record.courseName,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = dateFormat.format(record.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status text
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (record.status == "present")
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (record.status == "present") "Présent" else "Absent",
                    color = if (record.status == "present")
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}