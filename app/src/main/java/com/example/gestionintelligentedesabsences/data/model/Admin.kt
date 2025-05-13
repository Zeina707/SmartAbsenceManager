package com.example.gestionintelligentedesabsences.data.model

// Admin.kt
data class Admin(
    val userId: String = "",
    val adminId: String = "",
    val managedBranches: List<String> = emptyList()
)
