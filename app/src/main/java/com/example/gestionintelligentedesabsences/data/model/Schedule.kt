package com.example.gestionintelligentedesabsences.data.model

// Schedule.kt
data class Schedule(
    val id: String = "",
    val courseId: String = "",
    val day: Int = 0,
    val startTime: String = "",
    val endTime: String = "",
    val room: String = "",
    val recurring: Boolean = true
)