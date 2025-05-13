package com.example.gestionintelligentedesabsences.data.model

// Teacher.kt
data class Teacher(
    val userId: String = "",
    val teacherId: String = "",
    val specialization: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val assignedCourses: List<String> = emptyList() // Course IDs
)