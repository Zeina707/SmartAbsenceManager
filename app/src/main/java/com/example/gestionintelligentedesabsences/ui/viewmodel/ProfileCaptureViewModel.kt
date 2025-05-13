package com.example.gestionintelligentedesabsences.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionintelligentedesabsences.data.model.Student
import com.example.gestionintelligentedesabsences.data.model.User
import com.example.gestionintelligentedesabsences.data.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

data class ProfileCaptureUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val student: Student? = null,
    val hasPhoto: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class ProfileCaptureViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(ProfileCaptureUiState())
    val uiState: StateFlow<ProfileCaptureUiState> = _uiState.asStateFlow()

    fun loadUserData(userId: String, context: android.content.Context) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Load user data from Firestore
                val userDoc = db.collection("users").document(userId).get().await()
                if (!userDoc.exists()) {
                    _uiState.update { it.copy(isLoading = false, error = "Utilisateur non trouvé") }
                    return@launch
                }

                val userData = userDoc.data ?: run {
                    _uiState.update { it.copy(isLoading = false, error = "Données utilisateur introuvables") }
                    return@launch
                }

                // Check if the user is a student and load additional data
                if (userData["role"] == UserRole.STUDENT.name) {
                    val studentDoc = db.collection("students").document(userId).get().await()
                    val studentData = studentDoc.data ?: mapOf()

                    val student = Student(
                        userId = userId,
                        firstName = userData["firstName"].toString(),
                        lastName = userData["lastName"].toString(),
                        group = userData["group"]?.toString() ?: "",
                        studentId = studentData["studentId"]?.toString() ?: "",
                        branch = studentData["branch"]?.toString() ?: "",
                        level = studentData["level"]?.toString() ?: "",
                        faceData = "" // No longer used
                    )

                    // Check if a photo exists locally
                    val photoFile = File(context.filesDir, "student_photos/$userId.jpg")
                    val hasPhoto = photoFile.exists()

                    // Create a User object for the UI
                    val user = User(
                        id = userId,
                        email = userData["email"]?.toString() ?: "",
                        firstName = student.firstName,
                        lastName = student.lastName,
                        role = UserRole.STUDENT
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            student = student,
                            hasPhoto = hasPhoto
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = User(
                                id = userId,
                                email = userData["email"]?.toString() ?: "",
                                firstName = userData["firstName"]?.toString() ?: "",
                                lastName = userData["lastName"]?.toString() ?: "",
                                role = try {
                                    UserRole.valueOf(userData["role"]?.toString() ?: UserRole.STUDENT.name)
                                } catch (e: Exception) {
                                    UserRole.STUDENT
                                },
                                profileImageUrl = userData["profileImageUrl"]?.toString() ?: ""
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erreur: ${e.message}"
                    )
                }
            }
        }
    }

    fun updatePhotoStatus(hasPhoto: Boolean) {
        _uiState.update { it.copy(hasPhoto = hasPhoto) }
    }

    fun resetError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}