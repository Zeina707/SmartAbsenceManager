package com.example.gestionintelligentedesabsences.data.model

// Course.kt
data class Course(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val teacherId: String = "",
    val branch: String = "",
    val level: String = "",
    val group: String = "",
    val isExam: Boolean = false
)