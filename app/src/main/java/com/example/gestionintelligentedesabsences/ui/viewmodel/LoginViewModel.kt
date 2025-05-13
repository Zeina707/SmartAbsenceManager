package com.example.gestionintelligentedesabsences.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionintelligentedesabsences.data.model.UserRole
import com.example.gestionintelligentedesabsences.data.source.remote.FirebaseService
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginViewModel : ViewModel() {

    private val firebaseService = FirebaseService()

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _loginState.value = LoginState.Error("Veuillez remplir tous les champs")
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            firebaseService.signIn(email, password)
                .onSuccess { user ->
                    // Get user data and verify superadmin status
                    val userDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.uid)
                        .get()
                        .await()

                    // Check if user exists in superadmins collection
                    val isSuperAdmin = FirebaseFirestore.getInstance()
                        .collection("superadmins")
                        .document(user.uid)
                        .get()
                        .await()
                        .exists()

                    // Determine role - prioritize superadmin status
                    val role = when {
                        isSuperAdmin -> UserRole.SUPER_ADMIN
                        else -> {
                            val roleStr = userDoc.getString("role") ?: "STUDENT"
                            try {
                                UserRole.valueOf(roleStr.uppercase())
                            } catch (e: IllegalArgumentException) {
                                UserRole.STUDENT // default fallback
                            }
                        }
                    }

                    _loginState.value = LoginState.Success(role)
                }
                .onFailure { e ->
                    _loginState.value = LoginState.Error("Identifiants invalides")
                }
        }
    }

    sealed class LoginState {
        object Idle : LoginState()
        object Loading : LoginState()
        data class Success(val userRole: UserRole) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}