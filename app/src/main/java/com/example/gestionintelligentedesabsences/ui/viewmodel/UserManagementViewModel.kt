package com.example.gestionintelligentedesabsences.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionintelligentedesabsences.data.model.Student
import com.example.gestionintelligentedesabsences.data.model.User
import com.example.gestionintelligentedesabsences.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

data class UserManagementUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class UserManagementViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()

    companion object {
        private const val TAG = "UserManagementViewModel"
    }

    fun loadUsers() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val users = mutableListOf<User>()
                val documents = firestore.collection("users").get().await()

                for (document in documents) {
                    val user = document.toObject(User::class.java).copy(id = document.id)
                    users.add(user)
                }

                _uiState.update {
                    it.copy(
                        users = users,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading users: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur lors du chargement des utilisateurs: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun createUser(userData: Map<String, Any>) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val email = userData["email"] as String
                val password = userData["password"] as String
                val firstName = userData["firstName"] as String
                val lastName = userData["lastName"] as String
                val role = UserRole.valueOf(userData["role"] as String)
                val group = userData["group"] as String

                // Create authentication account
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: throw Exception("User creation failed")

                // Create user document
                val user = User(
                    id = uid,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    role = role,
                    profileImageUrl = ""
                )

                firestore.collection("users")
                    .document(uid)
                    .set(user)
                    .await()

                // If user is a student, create student profile
                if (role == UserRole.STUDENT) {
                    val studentId = userData["studentId"] as? String ?: uid
                    val branch = userData["branch"] as? String ?: ""
                    val level = userData["level"] as? String ?: "TC"

                    val student = Student(
                        userId = uid,
                        studentId = studentId,
                        firstName = firstName,
                        lastName = lastName,
                        group = group,
                        branch = branch,
                        level = level,
                        faceData = ""
                    )

                    firestore.collection("students")
                        .document(uid)
                        .set(student)
                        .await()
                }

                // Reload users list
                loadUsers()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Utilisateur créé avec succès"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating user: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur lors de la création de l'utilisateur: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateUser(userId: String, userData: Map<String, Any>) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val firstName = userData["firstName"] as String
                val lastName = userData["lastName"] as String
                val email = userData["email"] as String
                val role = UserRole.valueOf(userData["role"] as String)
                val group = userData["group"] as String

                // Update user document
                val userUpdates = mapOf(
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "email" to email,
                    "role" to role.name,
                    "profileImageUrl" to "" // You might want to preserve existing image URL
                )

                firestore.collection("users")
                    .document(userId)
                    .update(userUpdates)
                    .await()

                // If user is a student, update student profile
                if (role == UserRole.STUDENT) {
                    val studentUpdates = mutableMapOf<String, Any>(
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "group" to group
                    )

                    // Add optional student fields if provided
                    (userData["studentId"] as? String)?.let { studentUpdates["studentId"] = it }
                    (userData["branch"] as? String)?.let { studentUpdates["branch"] = it }
                    (userData["level"] as? String)?.let { studentUpdates["level"] = it }

                    firestore.collection("students")
                        .document(userId)
                        .update(studentUpdates)
                        .await()
                }

                // Reload users list
                loadUsers()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Utilisateur mis à jour avec succès"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur lors de la mise à jour de l'utilisateur: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Delete user from Firestore
                firestore.collection("users")
                    .document(userId)
                    .delete()
                    .await()

                // Delete student profile if exists
                try {
                    firestore.collection("students")
                        .document(userId)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "No student profile to delete for user $userId")
                }

                // Note: Authentication account deletion requires Admin SDK or Cloud Function
                // Consider implementing a Cloud Function for this purpose

                // Reload users list
                loadUsers()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Utilisateur supprimé avec succès"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting user: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur lors de la suppression de l'utilisateur: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun saveFaceData(userId: String, faceData: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Update the student document with face data
                firestore.collection("students")
                    .document(userId)
                    .update("faceData", faceData)
                    .await()

                // Update user's profile image URL (optional)
                firestore.collection("users")
                    .document(userId)
                    .update("profileImageUrl", "face_configured") // You might want to set an actual URL here
                    .await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Données faciales enregistrées avec succès"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving face data: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur lors de l'enregistrement des données faciales: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun resetError() {
        _uiState.update { it.copy(error = null) }
    }

    fun resetSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}