package com.example.gestionintelligentedesabsences.ui.screens.management

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestionintelligentedesabsences.data.model.Student
import com.example.gestionintelligentedesabsences.ui.components.FaceRegistrationDialog
import com.example.gestionintelligentedesabsences.ui.viewmodel.ProfileCaptureViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCaptureScreen(
    userId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProfileCaptureViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var showFaceRegistrationDialog by remember { mutableStateOf(false) }

    // Load user data when the screen is first shown
    LaunchedEffect(key1 = userId) {
        viewModel.loadUserData(userId, context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture de profil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // User info
                    uiState.user?.let { user ->
                        Text(
                            text = "${user.firstName} ${user.lastName}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Rôle: ${user.role.name}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Student-specific info
                        if (user.role.name == "STUDENT") {
                            uiState.student?.let { student ->
                                Text(
                                    text = "ID Étudiant: ${student.studentId}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Niveau: ${student.level}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Filière: ${student.branch}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Button(
                                onClick = { showFaceRegistrationDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Capturer le visage")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Add status indicator for photo
                            val hasPhoto = uiState.hasPhoto
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (hasPhoto)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = if (hasPhoto)
                                            "Photo enregistrée"
                                        else
                                            "Aucune photo",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (hasPhoto)
                                            "L'étudiant peut être identifié par reconnaissance faciale"
                                        else
                                            "Veuillez capturer le visage de l'étudiant pour activer la reconnaissance faciale"
                                    )
                                }
                            }
                        }
                    } ?: run {
                        Text(
                            text = "Utilisateur non trouvé",
                            style = MaterialTheme.typography.bodyLarge
                        )
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
                        TextButton(onClick = { viewModel.resetError() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // Show success message
            if (uiState.successMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { viewModel.resetSuccessMessage() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(uiState.successMessage!!)
                }

                // Auto-dismiss after a delay
                LaunchedEffect(key1 = uiState.successMessage) {
                    kotlinx.coroutines.delay(3000)
                    viewModel.resetSuccessMessage()
                }
            }
        }
    }

    // Face Registration Dialog
    if (showFaceRegistrationDialog && uiState.student != null) {
        FaceRegistrationDialog(
            student = uiState.student!!,
            onRegistrationComplete = { success ->
                if (success) {
                    coroutineScope.launch {
                        viewModel.updatePhotoStatus(true)
                    }
                }
                showFaceRegistrationDialog = false
            },
            onClose = { showFaceRegistrationDialog = false }
        )
    }
}