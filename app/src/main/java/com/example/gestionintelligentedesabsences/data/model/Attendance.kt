package com.example.gestionintelligentedesabsences.data.model


// Attendance.kt
data class Attendance(
    val id: String = "",
    val courseId: String = "",
    val studentId: String = "",
    val timestamp: Long = 0,
    val isPresent: Boolean = false,
    val captureMethod: CaptureMethod = CaptureMethod.FACE_RECOGNITION,
    val capturedBy: String = "" // User ID who captured the attendance
)
enum class CaptureMethod {
    FACE_RECOGNITION,
    MANUAL_ENTRY
}
