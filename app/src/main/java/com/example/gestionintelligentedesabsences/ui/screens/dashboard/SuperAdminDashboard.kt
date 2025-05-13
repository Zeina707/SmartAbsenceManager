package com.example.gestionintelligentedesabsences.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestionintelligentedesabsences.R
import com.example.gestionintelligentedesabsences.data.model.*
import com.example.gestionintelligentedesabsences.ui.components.ScheduleItem
import com.example.gestionintelligentedesabsences.ui.components.LoadingIndicator
import com.example.gestionintelligentedesabsences.ui.components.StatCard
import com.example.gestionintelligentedesabsences.ui.theme.GestionIntelligenteDesAbsencesTheme
import com.example.gestionintelligentedesabsences.ui.viewmodel.SuperAdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminDashboard(
    onLogout: () -> Unit,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToCourseManagement: () -> Unit,
    onNavigateToScheduleManagement: () -> Unit,
    viewModel: SuperAdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var showAddCourseDialog by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) {
        viewModel.loadDashboardData()
    }

    GestionIntelligenteDesAbsencesTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Super Admin Dashboard") },
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // User Statistics Section
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Statistiques",
                                            style = MaterialTheme.typography.titleLarge
                                        )

                                        Button(onClick = { showAddUserDialog = true }) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ajouter")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        StatCard(
                                            title = "Étudiants",
                                            value = uiState.userStats.totalStudents.toString(),
                                            icon =painterResource(id = R.drawable.student),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        StatCard(
                                            title = "Enseignants",
                                            value = uiState.userStats.totalTeachers.toString(),
                                            icon =painterResource(id = R.drawable.teacher),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        StatCard(
                                            title = "Admins",
                                            value = uiState.userStats.totalAdmins.toString(),
                                            icon =painterResource(id = R.drawable.admin),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Utilisateurs récents",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    uiState.userStats.recentUsers.forEach { user ->
                                        RecentUserItem(user = user)
                                        Divider()
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = onNavigateToUserManagement,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Gérer les utilisateurs")
                                    }
                                }
                            }
                        }

                        // Courses Section
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Cours",
                                            style = MaterialTheme.typography.titleLarge
                                        )

                                        Button(onClick = { showAddCourseDialog = true }) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ajouter")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Display some course stats
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        val tcCourses = uiState.courses.count { it.level == "TC" }
                                        val licenseCourses = uiState.courses.count { it.level == "L" }
                                        val masterCourses = uiState.courses.count { it.level == "M" }

                                        StatCard(
                                            title = "TC",
                                            value = tcCourses.toString(),
                                            icon = painterResource(id = R.drawable.tronc_commun),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        StatCard(
                                            title = "Licence",
                                            value = licenseCourses.toString(),
                                            icon =painterResource(id = R.drawable.license),
                                            modifier = Modifier.weight(1f)
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        StatCard(
                                            title = "Master",
                                            value = masterCourses.toString(),
                                            icon =painterResource(id = R.drawable.master),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = "Cours récents",
                                        style = MaterialTheme.typography.titleMedium
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    uiState.courses.take(5).forEach { course ->
                                        CourseItem(course = course)
                                        Divider()
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = onNavigateToCourseManagement,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Gérer les cours")
                                    }
                                }
                            }
                        }

                        // Schedules Section (remplace les Events)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Emplois du temps",
                                            style = MaterialTheme.typography.titleLarge
                                        )

                                        Button(onClick = { showAddScheduleDialog = true }) {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Ajouter")
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (uiState.schedules.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Aucun emploi du temps trouvé")
                                        }
                                    } else {
                                        Column {
                                            uiState.schedules.forEach { schedule ->
                                                // Trouver le cours correspondant au schedule
                                                val course = uiState.courses.find { it.id == schedule.courseId }
                                                ScheduleItem(
                                                    schedule = schedule,
                                                    courseName = course?.name ?: "Cours inconnu",
                                                    level = course?.level ?: "",
                                                    branch = course?.branch ?: ""
                                                )
                                                Divider()
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = onNavigateToScheduleManagement,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("Gérer l'emploi du temps")
                                    }
                                }
                            }
                        }
                    }
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

                // Show success messages
                if (uiState.userCreated || uiState.courseCreated || uiState.scheduleCreated) {
                    val message = when {
                        uiState.userCreated -> "Utilisateur créé avec succès"
                        uiState.courseCreated -> "Cours créé avec succès"
                        uiState.scheduleCreated -> "Emploi du temps ajouté avec succès"
                        else -> ""
                    }

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
                        Text(message)
                    }

                    // Auto-dismiss after some time
                    LaunchedEffect(key1 = true) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.resetFlags()
                    }
                }
            }
        }
    }

    // Add User Dialog
    if (showAddUserDialog) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var firstName by remember { mutableStateOf("") }
        var lastName by remember { mutableStateOf("") }
        var group by remember { mutableStateOf("") }
        var selectedRole by remember { mutableStateOf("STUDENT") }
        var studentId by remember { mutableStateOf("") }
        var teacherId by remember { mutableStateOf("") }
        var branch by remember { mutableStateOf("") }
        var level by remember { mutableStateOf("TC") }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Ajouter un utilisateur") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Mot de passe") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Prénom") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Nom") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = group,
                        onValueChange = { group = it },
                        label = { Text("Groupe") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dropdown pour sélectionner le rôle
                    var expandedDropdown by remember { mutableStateOf(false) }
                    val roles = UserRole.values().map { it.name }

                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = it },
                    ) {
                        TextField(
                            value = selectedRole,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("Rôle") }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            roles.forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role) },
                                    onClick = {
                                        selectedRole = role
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Show student-specific fields if role is Student
                    if (selectedRole == "STUDENT") {
                        OutlinedTextField(
                            value = studentId,
                            onValueChange = { studentId = it },
                            label = { Text("ID Étudiant") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Level dropdown
                        var expandedLevel by remember { mutableStateOf(false) }
                        val levels = listOf("TC", "L", "M")

                        ExposedDropdownMenuBox(
                            expanded = expandedLevel,
                            onExpandedChange = { expandedLevel = it },
                        ) {
                            TextField(
                                value = level,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLevel) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                label = { Text("Niveau") }
                            )
                            ExposedDropdownMenu(
                                expanded = expandedLevel,
                                onDismissRequest = { expandedLevel = false }
                            ) {
                                levels.forEach { lvl ->
                                    DropdownMenuItem(
                                        text = { Text(lvl) },
                                        onClick = {
                                            level = lvl
                                            expandedLevel = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = branch,
                            onValueChange = { branch = it },
                            label = { Text("Filière") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Show teacher-specific field if role is Teacher
                    if (selectedRole == "TEACHER") {
                        OutlinedTextField(
                            value = teacherId,
                            onValueChange = { teacherId = it },
                            label = { Text("ID Enseignant") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createUser(
                            email,
                            password,
                            firstName,
                            lastName,
                            selectedRole,
                            studentId,
                            branch,
                            level,
                            group,
                            teacherId  // Pass the teacherId parameter
                        )
                        showAddUserDialog = false
                    }
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddUserDialog = false }
                ) {
                    Text("Annuler")
                }
            }
        )
    }

// Add Course Dialog
    if (showAddCourseDialog) {
        var courseName by remember { mutableStateOf("") }
        var courseCode by remember { mutableStateOf("") }
        var courseLevel by remember { mutableStateOf("TC") }
        var teacherId by remember { mutableStateOf("") }
        var branch by remember { mutableStateOf("") }
        var group by remember { mutableStateOf("") }
        var isExam by remember { mutableStateOf(false) }
        var selectedTeacher by remember { mutableStateOf<User?>(null) }
        var teachers by remember { mutableStateOf<List<User>>(emptyList()) }

        // Load teachers when dialog is shown
        LaunchedEffect(showAddCourseDialog) {
            if (showAddCourseDialog) {
                teachers = viewModel.loadTeachers()
            }
        }

        AlertDialog(
            onDismissRequest = { showAddCourseDialog = false },
            title = { Text("Ajouter un cours") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = courseName,
                        onValueChange = { courseName = it },
                        label = { Text("Nom du cours") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = courseCode,
                        onValueChange = { courseCode = it },
                        label = { Text("Code du cours") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Dropdown pour le niveau
                    var expandedLevel by remember { mutableStateOf(false) }
                    val levels = listOf("TC", "L", "M")

                    ExposedDropdownMenuBox(
                        expanded = expandedLevel,
                        onExpandedChange = { expandedLevel = it },
                    ) {
                        TextField(
                            value = courseLevel,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLevel) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("Niveau") }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedLevel,
                            onDismissRequest = { expandedLevel = false }
                        ) {
                            levels.forEach { level ->
                                DropdownMenuItem(
                                    text = { Text(level) },
                                    onClick = {
                                        courseLevel = level
                                        expandedLevel = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = branch,
                        onValueChange = { branch = it },
                        label = { Text("Filière") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = group,
                        onValueChange = { group = it },
                        label = { Text("Groupe") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Teacher dropdown replacement
                    var expandedTeacher by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedTeacher,
                        onExpandedChange = { expandedTeacher = it },
                    ) {
                        TextField(
                            value = selectedTeacher?.let { "${it.firstName} ${it.lastName}" } ?: "Sélectionner un enseignant",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTeacher) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("Enseignant") }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedTeacher,
                            onDismissRequest = { expandedTeacher = false }
                        ) {
                            teachers.forEach { teacher ->
                                DropdownMenuItem(
                                    text = { Text("${teacher.firstName} ${teacher.lastName}") },
                                    onClick = {
                                        selectedTeacher = teacher
                                        teacherId = teacher.id
                                        expandedTeacher = false
                                    }
                                )
                            }
                        }
                    }

                    // Add checkbox for isExam
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isExam,
                            onCheckedChange = { isExam = it }
                        )
                        Text("Est un examen")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createCourse(
                            courseName,
                            courseCode,
                            courseLevel,
                            selectedTeacher?.id ?: "",  // Use selected teacher ID
                            branch,
                            group,
                            isExam
                        )
                        showAddCourseDialog = false
                    }
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddCourseDialog = false }
                ) {
                    Text("Annuler")
                }
            }
        )
    }
    // Add Schedule Dialog
    if (showAddScheduleDialog) {
        var courseId by remember { mutableStateOf("") }
        var day by remember { mutableStateOf(1) } // Par défaut lundi
        var startTime by remember { mutableStateOf("08:30") }
        var endTime by remember { mutableStateOf("10:00") }
        var room by remember { mutableStateOf("") }
        var recurring by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddScheduleDialog = false },
            title = { Text("Ajouter un emploi du temps") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Course dropdown
                    var expandedCourse by remember { mutableStateOf(false) }
                    val courses = uiState.courses

                    ExposedDropdownMenuBox(
                        expanded = expandedCourse,
                        onExpandedChange = { expandedCourse = it },
                    ) {
                        TextField(
                            value = courses.find { it.id == courseId }?.name ?: "Sélectionner un cours",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCourse) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("Cours") }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCourse,
                            onDismissRequest = { expandedCourse = false }
                        ) {
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text(course.name) },
                                    onClick = {
                                        courseId = course.id
                                        expandedCourse = false
                                    }
                                )
                            }
                        }
                    }

                    // Day dropdown
                    var expandedDay by remember { mutableStateOf(false) }
                    val days = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")

                    ExposedDropdownMenuBox(
                        expanded = expandedDay,
                        onExpandedChange = { expandedDay = it },
                    ) {
                        TextField(
                            value = days[day - 1],
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDay) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            label = { Text("Jour") }
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDay,
                            onDismissRequest = { expandedDay = false }
                        ) {
                            days.forEachIndexed { index, dayName ->
                                DropdownMenuItem(
                                    text = { Text(dayName) },
                                    onClick = {
                                        day = index + 1
                                        expandedDay = false
                                    }
                                )
                            }
                        }
                    }

                    // Start time
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Heure de début") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // End time
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Heure de fin") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Room
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        label = { Text("Salle") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Recurring checkbox
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = recurring,
                            onCheckedChange = { recurring = it }
                        )
                        Text("Récurrent")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createSchedule(
                            courseId = courseId,
                            day = day,
                            startTime = startTime,
                            endTime = endTime,
                            room = room,
                            recurring = recurring
                        )
                        showAddScheduleDialog = false
                    }
                ) {
                    Text("Ajouter")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddScheduleDialog = false }
                ) {
                    Text("Annuler")
                }
            }
        )
    }


}

@Composable
fun RecentUserItem(user: User) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when (user.role) {
                UserRole.STUDENT -> Icons.Default.Person
                UserRole.TEACHER -> Icons.Default.Face
                UserRole.ADMIN -> Icons.Default.Star
                UserRole.SUPER_ADMIN -> Icons.Default.Lock
            },
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = "${user.firstName} ${user.lastName}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = user.role.toString(),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun CourseItem(course: Course) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (course.isExam) Icons.Default.Warning else Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = course.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Code: ${course.code}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "Niveau: ${course.level}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

