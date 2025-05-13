package com.example.gestionintelligentedesabsences.data.model

// Student.kt
data class Student(
    val userId: String = "",
    val studentId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val group: String = "",
    val branch: String = "",
    val level: String = "",
    val faceData: String = "",  // Base64 encoded face features
    val absences: List<Attendance> = emptyList()
)