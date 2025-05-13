package com.example.gestionintelligentedesabsences.ui.components


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestionintelligentedesabsences.data.model.User
import com.example.gestionintelligentedesabsences.data.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListItem(
    user: User,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onProfileCapture: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User icon based on role
                val userIcon = when (user.role) {
                    UserRole.STUDENT -> Icons.Default.Person
                    UserRole.TEACHER -> Icons.Default.Home
                    UserRole.ADMIN -> Icons.Default.Lock
                    UserRole.SUPER_ADMIN -> Icons.Default.AccountBox
                }

                Icon(
                    imageVector = userIcon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "${user.firstName} ${user.lastName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = user.role.name,
                            style = MaterialTheme.typography.bodySmall
                        )

                        // Show face recognition status indicator for students
                        if (user.role == UserRole.STUDENT) {
                            Spacer(modifier = Modifier.width(8.dp))

                            val hasFaceProfile = user.profileImageUrl.isNotEmpty()
                            val faceIcon = if (hasFaceProfile) Icons.Default.Face else Icons.Default.Face
                            val faceColor = if (hasFaceProfile) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error


                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Profile capture button (only for students)
                if (user.role == UserRole.STUDENT) {
                    OutlinedButton(
                        onClick = onProfileCapture
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Capturer")
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Edit button
                OutlinedButton(
                    onClick = onEdit
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modifier")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete button
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                }
            }
        }
    }
}