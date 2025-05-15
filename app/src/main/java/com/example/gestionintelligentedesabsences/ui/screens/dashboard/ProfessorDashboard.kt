package com.example.gestionintelligentedesabsences.ui.screens.dashboard

import android.util.Log
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
import com.example.gestionintelligentedesabsences.ui.viewmodel.CourseWithSchedule
import com.example.gestionintelligentedesabsences.ui.viewmodel.ProfessorViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessorDashboard(
    onLogout: () -> Unit,
    viewModel: ProfessorViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFaceRecognition by remember { mutableStateOf(false) }
    var currentStudentForRecognition by remember { mutableStateOf<Student?>(null) }
    var onRecognitionUpdateCallback by remember { mutableStateOf<((String, Boolean) -> Unit)?>(null) }

    // Tabs for branch selection (L and M)
    var selectedBranchTab by remember { mutableStateOf(0) } // 0 for L, 1 for M

    // Dropdown state for content type (Exams/Courses)
    var currentContentType by remember { mutableStateOf("Examens") } // "Examens" ou "Cours"
    var expandedDropdown by remember { mutableStateOf(false) }

    // Day of week names
    val daysOfWeek = listOf("Dimanche", "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi")

    LaunchedEffect(key1 = Unit) {
        viewModel.loadDashboardData()
    }

    // Effect to update filters when tabs or dropdown change
    LaunchedEffect(selectedBranchTab, currentContentType) {
        val branch = if (selectedBranchTab == 0) "L" else "M"
        val isExam = currentContentType == "Examens"
        viewModel.updateFilters(branch, isExam)
    }

    GestionIntelligenteDesAbsencesTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Dashboard Professeur") },
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
                } else if (uiState.selectedCourseId.isNotEmpty()) {
                    // Show selected course's students for attendance marking
                    AttendanceMarkingScreen2(
                        students = uiState.selectedCourseStudents,
                        isLoading = uiState.isLoadingStudents,
                        onMarkAttendance = { presentStudentIds, absentStudentIds ->
                            // Mark present students
                            if (presentStudentIds.isNotEmpty()) {
                                viewModel.markAttendance(uiState.selectedCourseId, presentStudentIds, true)
                            }
                            // Mark absent students
                            if (absentStudentIds.isNotEmpty()) {
                                viewModel.markAttendance(uiState.selectedCourseId, absentStudentIds, false)
                            }
                        },
                        onBack = { viewModel.resetSelectedCourse() },
                        onFaceRecognition = { student ->
                            currentStudentForRecognition = student
                            showFaceRecognition = true
                        },
                        onRecognitionUpdate = { studentId, isRecognized ->
                            onRecognitionUpdateCallback?.invoke(studentId, isRecognized)
                        }
                    ) { callback ->
                        // Store the callback from AttendanceMarkingScreen2
                        onRecognitionUpdateCallback = callback
                    }
                } else {
                    // Main dashboard content
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Branch selection tabs (L and M)
                        TabRow(selectedTabIndex = selectedBranchTab) {
                            Tab(
                                selected = selectedBranchTab == 0,
                                onClick = { selectedBranchTab = 0 },
                                text = { Text("Licence (L)") }
                            )
                            Tab(
                                selected = selectedBranchTab == 1,
                                onClick = { selectedBranchTab = 1 },
                                text = { Text("Master (M)") }
                            )
                        }

                        // Content type dropdown (Exams/Courses)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Type de contenu:",
                                style = MaterialTheme.typography.bodyLarge
                            )

                            Box {
                                OutlinedButton(
                                    onClick = { expandedDropdown = true },
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(currentContentType)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Sélectionner"
                                    )
                                }

                                DropdownMenu(
                                    expanded = expandedDropdown,
                                    onDismissRequest = { expandedDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Examens") },
                                        onClick = {
                                            currentContentType = "Examens"
                                            expandedDropdown = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Cours") },
                                        onClick = {
                                            currentContentType = "Cours"
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Content area
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Statistics Section based on selected branch
                            item {
                                val branchLabel = if (selectedBranchTab == 0) "Licence (L)" else "Master (M)"
                                val stats = if (selectedBranchTab == 0) uiState.lAbsenceStats else uiState.mAbsenceStats

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Statistiques d'Absences - $branchLabel",
                                            style = MaterialTheme.typography.titleLarge
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            StatCard(
                                                title = "Total Étudiants",
                                                value = stats.totalStudents.toString(),
                                                icon = painterResource(id = R.drawable.student),
                                                modifier = Modifier.weight(1f)
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            StatCard(
                                                title = "Étudiants avec Absences",
                                                value = stats.studentsWithAbsences.toString(),
                                                icon = painterResource(id = R.drawable.student),
                                                modifier = Modifier.weight(1f)
                                            )

                                            Spacer(modifier = Modifier.width(8.dp))

                                            StatCard(
                                                title = "Total Absences",
                                                value = stats.totalAbsences.toString(),
                                                icon = painterResource(id = R.drawable.student),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }

                            // Upcoming Events Section (Based on selected branch)
                            item {
                                val branchLabel = if (selectedBranchTab == 0) "Licence (L)" else "Master (M)"
                                val upcomingCoursesAndExams = if (selectedBranchTab == 0)
                                    uiState.upcomingLCoursesAndExams else uiState.upcomingMCoursesAndExams

                                // Filter based on selected content type (Exams/Courses)
                                val filteredUpcoming = upcomingCoursesAndExams.filter {
                                    it.course.isExam == (currentContentType == "Examens")
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Événements à Venir ",
                                            style = MaterialTheme.typography.titleLarge
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (filteredUpcoming.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Aucun événement à venir")
                                            }
                                        } else {
                                            filteredUpcoming.forEach { courseWithSchedule ->
                                                UpcomingCourseItem(
                                                    courseWithSchedule = courseWithSchedule,
                                                    dayName = daysOfWeek[courseWithSchedule.schedule.day % 7],
                                                    onScanClick = {
                                                        viewModel.getStudentsForCourse2(courseWithSchedule.course.id)
                                                    }
                                                )
                                                Divider()
                                            }
                                        }
                                    }
                                }
                            }

                            // Filtered Courses/Exams Section
                            item {
                                val contentTypeLabel = currentContentType
                                val branchLabel = if (selectedBranchTab == 0) "Licence (L)" else "Master (M)"

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "$contentTypeLabel - $branchLabel",
                                            style = MaterialTheme.typography.titleLarge
                                        )

                                        Spacer(modifier = Modifier.height(16.dp))

                                        if (uiState.currentFilteredCourses.isEmpty()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(32.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Aucun élément trouvé")
                                            }
                                        } else {
                                            uiState.currentFilteredCourses.forEach { course ->
                                                CourseItem(
                                                    course = course,
                                                    onViewStudents = {
                                                        viewModel.getStudentsForCourse2(course.id)
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

                // Add face recognition camera overlay
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
                        viewModel.resetSelectedCourse() // Return to dashboard
                        viewModel.resetFlags()
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingCourseItem(
    courseWithSchedule: CourseWithSchedule,
    dayName: String,
    onScanClick: () -> Unit
) {
    val course = courseWithSchedule.course
    val schedule = courseWithSchedule.schedule

    // Determine type and icon based on isExam
    val (courseTypeText, courseIcon) = if (course.isExam) {
        "Examen" to Icons.Default.Warning
    } else {
        "Cours" to Icons.Default.Home
    }

    // Disable scan button if branch or group is empty
    val isScanEnabled = course.branch.isNotEmpty() && course.group.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = courseIcon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row {
                Text(
                    text = "${course.name} ",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "($courseTypeText)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "$dayName, ${schedule.startTime} - ${schedule.endTime}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Salle: ${schedule.room}",
                style = MaterialTheme.typography.bodySmall
            )
            if (course.branch.isNotEmpty()) {
                Text(
                    text = "Filière: ${course.branch}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Filière: Non spécifiée",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (course.group.isNotEmpty()) {
                Text(
                    text = "Groupe: ${course.group}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Groupe: Non spécifié",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Button(
            onClick = onScanClick,
            enabled = isScanEnabled,
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
fun CourseItem(
    course: com.example.gestionintelligentedesabsences.data.model.Course,
    onViewStudents: () -> Unit
) {
    val courseTypeText = if (course.isExam) "Examen" else "Cours"
    val courseIcon = if (course.isExam) Icons.Default.Warning else Icons.Default.Home

    // Disable scan button if branch or group is empty
    val isScanEnabled = course.branch.isNotEmpty() && course.group.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = courseIcon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row {
                Text(
                    text = "${course.name} ",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "($courseTypeText)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (course.branch.isNotEmpty()) {
                Text(
                    text = "Filière: ${course.branch}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Filière: Non spécifiée",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (course.group.isNotEmpty()) {
                Text(
                    text = "Groupe: ${course.group}",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Groupe: Non spécifié",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        TextButton(
            onClick = onViewStudents,
            enabled = isScanEnabled
        ) {
            Text("Scanner")
        }
    }
}

@Composable
fun AttendanceMarkingScreen2(
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
                Text("Aucun étudiant trouvé pour ce cours/examen")
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
                            Text(
                                text = "Filière: ${student.branch.ifEmpty { "Non spécifiée" }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Groupe: ${student.group.ifEmpty { "Non spécifié" }}",
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