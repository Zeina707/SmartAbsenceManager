package com.example.gestionintelligentedesabsences.ui.screens.management

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.gestionintelligentedesabsences.data.model.User
import com.example.gestionintelligentedesabsences.data.model.UserRole
import com.example.gestionintelligentedesabsences.ui.components.UserListItem
import com.example.gestionintelligentedesabsences.ui.viewmodel.UserManagementViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagement(
    onNavigateBack: () -> Unit,
    onNavigateToProfileCapture: (String) -> Unit,
    viewModel: UserManagementViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var showEditUserDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filteredRole by remember { mutableStateOf<UserRole?>(null) }

    // Load users when the screen is first shown
    LaunchedEffect(key1 = Unit) {
        viewModel.loadUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestion des utilisateurs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadUsers() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddUserDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Ajouter un utilisateur")
            }
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
                        .padding(16.dp)
                ) {
                    // Search and filter section
                    Text(
                        text = "Recherche et filtrage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Rechercher") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Role filter - full width below search
                    var expandedRoleFilter by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedRoleFilter,
                        onExpandedChange = { expandedRoleFilter = it }
                    ) {
                        TextField(
                            value = filteredRole?.name ?: "Tous les rôles",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRoleFilter) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = expandedRoleFilter,
                            onDismissRequest = { expandedRoleFilter = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Tous les rôles") },
                                onClick = {
                                    filteredRole = null
                                    expandedRoleFilter = false
                                }
                            )

                            UserRole.values().forEach { role ->
                                DropdownMenuItem(
                                    text = { Text(role.name) },
                                    onClick = {
                                        filteredRole = role
                                        expandedRoleFilter = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // User list section
                    Text(
                        text = "Liste des utilisateurs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Filter users based on search query and selected role
                    val filteredUsers = uiState.users.filter { user ->
                        val matchesQuery = searchQuery.isEmpty() ||
                                user.firstName.contains(searchQuery, ignoreCase = true) ||
                                user.lastName.contains(searchQuery, ignoreCase = true) ||
                                user.email.contains(searchQuery, ignoreCase = true)

                        val matchesRole = filteredRole == null || user.role == filteredRole

                        matchesQuery && matchesRole
                    }

                    if (filteredUsers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.users.isEmpty()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Aucun utilisateur n'est enregistré")
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Ajoutez des utilisateurs en utilisant le bouton +",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Aucun utilisateur ne correspond à votre recherche")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredUsers) { user ->
                                UserListItem(
                                    user = user,
                                    onEdit = {
                                        selectedUser = user
                                        showEditUserDialog = true
                                    },
                                    onDelete = {
                                        selectedUser = user
                                        showDeleteConfirmDialog = true
                                    },
                                    onProfileCapture = {
                                        onNavigateToProfileCapture(user.id)
                                    }
                                )
                            }
                        }

                        // Summary text showing number of users
                        Text(
                            text = "${filteredUsers.size} utilisateur(s) affiché(s)",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
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

    // Add User Dialog
    if (showAddUserDialog) {
        AddOrEditUserDialog(
            isAdd = true,
            user = null,
            onDismiss = { showAddUserDialog = false },
            onConfirm = { userData ->
                viewModel.createUser(userData)
                showAddUserDialog = false
            }
        )
    }

    // Edit User Dialog
    if (showEditUserDialog && selectedUser != null) {
        AddOrEditUserDialog(
            isAdd = false,
            user = selectedUser,
            onDismiss = { showEditUserDialog = false },
            onConfirm = { userData ->
                viewModel.updateUser(selectedUser!!.id, userData)
                showEditUserDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && selectedUser != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Confirmer la suppression") },
            text = {
                Column {
                    Text("Êtes-vous sûr de vouloir supprimer cet utilisateur ?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${selectedUser?.firstName} ${selectedUser?.lastName}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(selectedUser?.email ?: "", style = MaterialTheme.typography.bodySmall)
                    Text(selectedUser?.role?.name ?: "", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        // Delete the user's photo if it exists
                        val photoFile = File(context.filesDir, "student_photos/${selectedUser!!.id}.jpg")
                        if (photoFile.exists()) {
                            photoFile.delete()
                        }
                        viewModel.deleteUser(selectedUser!!.id)
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditUserDialog(
    isAdd: Boolean,
    user: User?,
    onDismiss: () -> Unit,
    onConfirm: (Map<String, Any>) -> Unit
) {
    var email by remember { mutableStateOf(user?.email ?: "") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf(user?.firstName ?: "") }
    var lastName by remember { mutableStateOf(user?.lastName ?: "") }
    var group by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(user?.role?.name ?: UserRole.STUDENT.name) }
    var studentId by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("TC") }

    // Form validation state
    var isEmailValid by remember { mutableStateOf(true) }
    var isPasswordValid by remember { mutableStateOf(true) }
    var isFirstNameValid by remember { mutableStateOf(true) }
    var isLastNameValid by remember { mutableStateOf(true) }

    val title = if (isAdd) "Ajouter un utilisateur" else "Modifier l'utilisateur"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        isEmailValid = it.contains("@") && it.isNotEmpty()
                    },
                    label = { Text("Email *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isEmailValid,
                    supportingText = {
                        if (!isEmailValid) {
                            Text("Veuillez entrer une adresse email valide")
                        }
                    },
                    singleLine = true
                )

                if (isAdd) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            isPasswordValid = it.length >= 6
                        },
                        label = { Text("Mot de passe *") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        isError = !isPasswordValid,
                        supportingText = {
                            if (!isPasswordValid) {
                                Text("Le mot de passe doit contenir au moins 6 caractères")
                            }
                        },
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = firstName,
                    onValueChange = {
                        firstName = it
                        isFirstNameValid = it.isNotEmpty()
                    },
                    label = { Text("Prénom *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isFirstNameValid,
                    supportingText = {
                        if (!isFirstNameValid) {
                            Text("Ce champ est obligatoire")
                        }
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = {
                        lastName = it
                        isLastNameValid = it.isNotEmpty()
                    },
                    label = { Text("Nom *") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isLastNameValid,
                    supportingText = {
                        if (!isLastNameValid) {
                            Text("Ce champ est obligatoire")
                        }
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("Groupe") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Role dropdown
                var expandedDropdown by remember { mutableStateOf(false) }
                val roles = UserRole.values().map { it.name }

                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = it }
                ) {
                    TextField(
                        value = selectedRole,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Rôle *") }
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

                // Student-specific fields
                if (selectedRole == UserRole.STUDENT.name) {
                    Text(
                        "Informations étudiant",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = studentId,
                        onValueChange = { studentId = it },
                        label = { Text("ID Étudiant") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Level dropdown
                    var expandedLevel by remember { mutableStateOf(false) }
                    val levels = listOf("TC", "L", "M")

                    ExposedDropdownMenuBox(
                        expanded = expandedLevel,
                        onExpandedChange = { expandedLevel = it }
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
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Text(
                    "* Champs obligatoires",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Validate form
                    isEmailValid = email.contains("@") && email.isNotEmpty()
                    isPasswordValid = if (isAdd) password.length >= 6 else true
                    isFirstNameValid = firstName.isNotEmpty()
                    isLastNameValid = lastName.isNotEmpty()

                    if (isEmailValid && isPasswordValid && isFirstNameValid && isLastNameValid) {
                        val userData = mutableMapOf<String, Any>(
                            "email" to email,
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "role" to selectedRole,
                            "group" to group
                        )

                        if (isAdd) {
                            userData["password"] = password
                        }

                        if (selectedRole == UserRole.STUDENT.name) {
                            userData["studentId"] = studentId
                            userData["branch"] = branch
                            userData["level"] = level
                        }

                        onConfirm(userData)
                    }
                },
                enabled = email.isNotEmpty() &&
                        firstName.isNotEmpty() &&
                        lastName.isNotEmpty() &&
                        (if (isAdd) password.isNotEmpty() else true)
            ) {
                Icon(
                    imageVector = if (isAdd) Icons.Default.Add else Icons.Default.Check,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isAdd) "Ajouter" else "Modifier")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}