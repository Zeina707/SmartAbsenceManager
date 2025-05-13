package com.example.gestionintelligentedesabsences.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionintelligentedesabsences.data.model.Student
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class StudentDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(StudentDashboardUiState())
    val uiState: StateFlow<StudentDashboardUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    fun loadStudentData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")

                // Get student data
                val studentDoc = firestore.collection("students")
                    .whereEqualTo("userId", userId.trim())
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?: throw IllegalStateException("Student not found for user ID: $userId")

                val student = studentDoc.toObject(Student::class.java)?.copy(
                    studentId = studentDoc.getString("studentId") ?: userId,
                    userId = userId,
                    firstName = studentDoc.getString("firstName") ?: "",
                    lastName = studentDoc.getString("lastName") ?: "",
                    group = studentDoc.getString("group") ?: "",
                    branch = studentDoc.getString("branch") ?: "",
                    level = studentDoc.getString("level") ?: ""
                ) ?: throw IllegalStateException("Student data not found")

                // Get attendance records using userId as studentId
                val attendanceRecords = firestore.collection("attendance")
                    .whereEqualTo("studentId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        try {
                            val courseId = doc.getString("courseId") ?: return@mapNotNull null
                            val courseDoc = firestore.collection("courses").document(courseId).get().await()
                            val courseName = courseDoc.getString("name") ?: "Cours inconnu"

                            AttendanceRecord(
                                id = doc.id,
                                studentId = userId,
                                courseId = courseId,
                                courseName = courseName,
                                date = Date(doc.getLong("timestamp") ?: 0L),
                                status = if (doc.getBoolean("isPresent") == true) "present" else "absent"
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping attendance record: ${e.message}", e)
                            null
                        }
                    }

                // Calculate attendance percentage
                val totalSessions = attendanceRecords.size
                val presentSessions = attendanceRecords.count { it.status == "present" }
                val attendancePercentage = if (totalSessions > 0) presentSessions.toFloat() / totalSessions else 0f

                // Get today's schedule
                val todayCourses = getTodaySchedule(student)

                _uiState.update {
                    it.copy(
                        student = student,
                        attendanceRecords = attendanceRecords,
                        attendancePercentage = attendancePercentage,
                        totalSessions = totalSessions,
                        presentSessions = presentSessions,
                        absences = totalSessions - presentSessions,
                        todayCourses = todayCourses,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading student data: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur de chargement des données: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun getTodaySchedule(student: Student): List<CourseSchedule> {
        try {
            val calendar = Calendar.getInstance()
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            // Map Calendar day to schedule.day (1=Monday, 7=Sunday)
            val adjustedDayOfWeek = when (dayOfWeek) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }

            // Get enrolled courses by checking courses/<courseId>/enrolledStudents/<userId>
            val courses = firestore.collection("courses").get().await().documents
            val enrolledCourseIds = mutableListOf<String>()
            for (courseDoc in courses) {
                val enrollmentDoc = firestore.collection("courses")
                    .document(courseDoc.id)
                    .collection("enrolledStudents")
                    .document(student.userId)
                    .get()
                    .await()
                if (enrollmentDoc.exists()) {
                    enrolledCourseIds.add(courseDoc.id)
                }
            }

            // Get schedules for enrolled courses on this day
            val courseSchedules = mutableListOf<CourseSchedule>()
            for (courseId in enrolledCourseIds) {
                val schedules = firestore.collection("schedule")
                    .whereEqualTo("courseId", courseId)
                    .whereEqualTo("day", adjustedDayOfWeek)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        try {
                            val courseDoc = firestore.collection("courses").document(courseId).get().await()
                            val courseName = courseDoc.getString("name") ?: "Cours inconnu"
                            val teacherId = courseDoc.getString("teacherId")
                            var instructorName = "Prof. Inconnu"

                            if (teacherId != null) {
                                val teacherDoc = firestore.collection("users").document(teacherId).get().await()
                                instructorName = "${teacherDoc.getString("firstName") ?: ""} ${teacherDoc.getString("lastName") ?: ""}".trim()
                            }

                            CourseSchedule(
                                courseId = courseId,
                                courseName = courseName,
                            dayOfWeek = doc.getLong("day")?.toInt() ?: 0,
                            startTime = doc.getString("startTime") ?: "00:00",
                            endTime = doc.getString("endTime") ?: "00:00",
                            room = doc.getString("room") ?: "Salle non spécifiée",
                            instructor = instructorName
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error mapping course schedule: ${e.message}", e)
                            null
                        }
                    }
                courseSchedules.addAll(schedules)
            }

            // Sort by start time
            return courseSchedules.sortedBy { it.startTime }

        } catch (e: Exception) {
            Log.e(TAG, "Error getting today's schedule: ${e.message}", e)
            return emptyList()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    companion object {
        private const val TAG = "StudentDashboardVM"
    }
}

data class StudentDashboardUiState(
    val student: Student? = null,
    val attendanceRecords: List<AttendanceRecord> = emptyList(),
    val attendancePercentage: Float = 0f,
    val todayCourses: List<CourseSchedule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalSessions: Int = 0,
    val presentSessions: Int = 0,
    val absences: Int = 0
)

data class AttendanceRecord(
    val id: String,
    val studentId: String,
    val courseId: String,
    val courseName: String,
    val date: Date,
    val status: String // "present", "absent"
)

data class CourseSchedule(
    val courseId: String,
    val courseName: String,
    val dayOfWeek: Int, // 1 = Monday, 7 = Sunday
    val startTime: String, // Format: "HH:mm"
    val endTime: String, // Format: "HH:mm"
    val room: String,
    val instructor: String
)