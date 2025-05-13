package com.example.gestionintelligentedesabsences.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestionintelligentedesabsences.R
import com.example.gestionintelligentedesabsences.data.model.Student
import com.example.gestionintelligentedesabsences.ui.components.FaceRecognitionCamera
import com.example.gestionintelligentedesabsences.ui.components.LoadingIndicator
import com.example.gestionintelligentedesabsences.ui.components.StatCard
import com.example.gestionintelligentedesabsences.ui.theme.GestionIntelligenteDesAbsencesTheme
import com.example.gestionintelligentedesabsences.ui.viewmodel.AdminViewModel
import com.example.gestionintelligentedesabsences.ui.viewmodel.ExamWithSchedule
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboard(
    onLogout: () -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFaceRecognition by remember { mutableStateOf(false) }
    var currentStudentForRecognition by remember { mutableStateOf<Student?>(null) }
    var onRecognitionUpdateCallback by remember { mutableStateOf<((String, Boolean) -> Unit)?>(null) }

    // Day of week names
    val daysOfWeek = listOf("Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")

    LaunchedEffect(key1 = Unit) {
        viewModel.loadDashboardData()
    }

    GestionIntelligenteDesAbsencesTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard Admin - Tronc Commun") },
                    actions = {
                        IconButton(onClick = { viewModel.loadDashboardData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (uiState.isLoading) {
                    LoadingIndicator()
                } else {
                    // Afficher soit le dashboard principal soit l'écran de présence
                    if (uiState.selectedExamId.isNotEmpty()) {
                        AttendanceMarkingScreen(
                            students = uiState.selectedExamStudents,
                            isLoading = uiState.isLoadingStudents,
                            onMarkAttendance = { presentStudentIds, absentStudentIds ->
                                // Mark present students
                                if (presentStudentIds.isNotEmpty()) {
                                    viewModel.markAttendance(uiState.selectedExamId, presentStudentIds, true)
                                }
                                // Mark absent students
                                if (absentStudentIds.isNotEmpty()) {
                                    viewModel.markAttendance(uiState.selectedExamId, absentStudentIds, false)
                                }
                            },
                            onBack = { viewModel.resetSelectedExam() },
                            onFaceRecognition = { student ->
                                currentStudentForRecognition = student
                                showFaceRecognition = true
                            },
                            onRecognitionUpdate = { studentId, isRecognized ->
                                onRecognitionUpdateCallback?.invoke(studentId, isRecognized)
                            }
                        ) { callback ->
                            // Store the callback from AttendanceMarkingScreen
                            onRecognitionUpdateCallback = callback
                        }
                    } else {
                        // Main dashboard content
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Absence Statistics Section
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Statistiques d'Absences - Tronc Commun",
                                            style = MaterialTheme.typography.titleLarge
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            StatCard(
                                                title = "Total Étudiants",
                                                value = uiState.absenceStats.totalStudents.toString(),
                                                icon = painterResource(id = R.drawable.student),
                                                modifier = Modifier.weight(1f)
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            StatCard(
                                                title = "Étudiants avec Absences",
                                                value = uiState.absenceStats.studentsWithAbsences.toString(),
                                                icon = painterResource(id = R.drawable.student),
                                                modifier = Modifier.weight(1f)
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            StatCard(
                                                title = "Total Absences",
                                                value = uiState.absenceStats.totalAbsences.toString(),
                                                icon = painterResource(id = R.drawable.student),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Upcoming Exams Section
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Examens à Venir - Tronc Commun",
                                            style = MaterialTheme.typography.titleLarge
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (uiState.upcomingExams.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Aucun examen à venir")
                                            }
                                        } else {
                                            uiState.upcomingExams.forEach { examWithSchedule ->
                                                UpcomingExamItem(
                                                    examWithSchedule = examWithSchedule,
                                                    dayName = daysOfWeek[examWithSchedule.schedule.day % 7],
                                                    onScanClick = {
                                                        viewModel.getStudentsForExam(examWithSchedule.exam.id)
                                                    }
                                                )
                                                Divider()
                                            }
                                        }
                                    }
                                }
                            }

                            // All TC Exams Section
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Tous les Examens - Tronc Commun",
                                            style = MaterialTheme.typography.titleLarge
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (uiState.tcExams.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Aucun examen trouvé")
                                            }
                                        } else {
                                            uiState.tcExams.forEach { exam ->
                                                ExamItem(
                                                    exam = exam,
                                                    onViewStudents = {
                                                        viewModel.getStudentsForExam(exam.id)
                                                    }
                                                )
                                                Divider()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Ajouter la caméra de reconnaissance faciale par-dessus tout
                if (showFaceRecognition && currentStudentForRecognition != null) {
                    FaceRecognitionCamera(
                        student = currentStudentForRecognition!!,
                        onRecognitionResult = { isRecognized ->
                            if (isRecognized) {
                                // Update the attendance state when face is recognized
                                onRecognitionUpdateCallback?.invoke(currentStudentForRecognition!!.userId, true)
                            }
                            showFaceRecognition = false
                        },
                        onClose = { showFaceRecognition = false }
                    )
                }

                // Show error if any
                uiState.error?.let { error ->
                    Snackbar(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomCenter),
                        action = {
                            TextButton(onClick = { viewModel.resetFlags() }) {
                                Text("OK")
                            }
                        }
                    ) {
                        Text(error)
                    }
                }

                // Show success message for attendance marking
                if (uiState.attendanceMarked) {
                    Snackbar(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.BottomCenter),
                        action = {
                            TextButton(onClick = { viewModel.resetFlags() }) {
                                Text("OK")
                            }
                        }
                    ) {
                        Text("Présences/absences enregistrées avec succès")
                    }

                    // Auto-dismiss after some time
                    LaunchedEffect(key1 = true) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.resetSelectedExam() // Reviens au dashboard
                        viewModel.resetFlags()
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingExamItem(
    examWithSchedule: ExamWithSchedule,
    dayName: String,
    onScanClick: () -> Unit
) {
    val exam = examWithSchedule.exam
    val schedule = examWithSchedule.schedule

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = exam.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$dayName, ${schedule.startTime} - ${schedule.endTime}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Salle: ${schedule.room}",
                style = MaterialTheme.typography.bodySmall
            )
            if (exam.branch.isNotEmpty()) {
                Text(
                    text = "Filière: ${exam.branch}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (exam.group.isNotEmpty()) {
                Text(
                    text = "Groupe: ${exam.group}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Button(
            onClick = onScanClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Scanner"
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Scanner")
        }
    }
}

@Composable
fun ExamItem(
    exam: com.example.gestionintelligentedesabsences.data.model.Course,
    onViewStudents: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = exam.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Code: ${exam.code}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (exam.branch.isNotEmpty()) {
                Text(
                    text = "Filière: ${exam.branch}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (exam.group.isNotEmpty()) {
                Text(
                    text = "Groupe: ${exam.group}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        TextButton(
            onClick = onViewStudents
        ) {
            Text("Scanner")
        }
    }
}

@Composable
fun AttendanceMarkingScreen(
    students: List<Student>,
    isLoading: Boolean,
    onMarkAttendance: (presentStudentIds: List<String>, absentStudentIds: List<String>) -> Unit,
    onBack: () -> Unit,
    onFaceRecognition: (Student) -> Unit,
    onRecognitionUpdate: (String, Boolean) -> Unit,
    setRecognitionCallback: ((String, Boolean) -> Unit) -> Unit
) {
    // Track student attendance state locally
    val attendanceState = remember {
        students.associate { it.userId to mutableStateOf(false) }.toMutableMap()
    }

    // Pass the recognition update callback to the parent composable
    LaunchedEffect(Unit) {
        setRecognitionCallback { studentId, isRecognized ->
            attendanceState[studentId]?.value = isRecognized
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with back button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
            }

            Text(
                text = "Scanner les présences",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (students.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucun étudiant trouvé pour cet examen")
            }
        } else {
            // Student list with scan buttons for attendance
            Text(
                text = "Liste des étudiants (${students.size})",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(students) { student ->
                    val isPresent = attendanceState[student.userId]!!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${student.firstName} ${student.lastName}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "ID: ${student.studentId}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        // Status indicator
                        if (isPresent.value) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Présent",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Scanner button
                        Button(
                            onClick = {
                                onFaceRecognition(student)
                            },
                            enabled = !isPresent.value, // Disable button if already scanned
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isPresent.value)
                                    MaterialTheme.colorScheme.surfaceVariant
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Scanner le visage"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isPresent.value) "Scanné" else "Scanner")
                        }
                    }

                    Divider()
                }
            }

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val presentStudents = attendanceState.filter { it.value.value }.keys.toList()
                        val absentStudents = attendanceState.filter { !it.value.value }.keys.toList()
                        onMarkAttendance(presentStudents, absentStudents)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Enregistrer")
                }

                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Annuler")
                }
            }
        }
    }
}