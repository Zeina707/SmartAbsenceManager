package com.example.gestionintelligentedesabsences.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionintelligentedesabsences.data.model.Attendance
import com.example.gestionintelligentedesabsences.data.model.CaptureMethod
import com.example.gestionintelligentedesabsences.data.model.Course
import com.example.gestionintelligentedesabsences.data.model.Schedule
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
import java.util.Calendar
import java.util.UUID

class AdminViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Load exams for TC students
                val tcExams = loadTCExams()

                // Load TC students
                val tcStudents = loadTCStudents()

                // Load absence statistics for TC
                val absenceStats = calculateAbsenceStats(tcStudents)

                // Load upcoming exams
                val upcomingExams = loadUpcomingExams()

                _uiState.update {
                    it.copy(
                        tcExams = tcExams,
                        tcStudents = tcStudents,
                        absenceStats = absenceStats,
                        upcomingExams = upcomingExams,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading dashboard data: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur de chargement des données: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    private suspend fun loadTCExams(): List<Course> {
        return firestore.collection("courses")
            .whereEqualTo("level", "TC")
            .whereEqualTo("isExam", true)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val course = doc.toObject(Course::class.java) ?: return@mapNotNull null
                course.copy(id = doc.id)
            }
    }

    private suspend fun loadTCStudents(): List<Student> {
        return firestore.collection("students")
            .whereEqualTo("level", "TC")
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val student = doc.toObject(Student::class.java) ?: return@mapNotNull null
                student.copy(userId = doc.id)
            }
    }

    private suspend fun loadUpcomingExams(): List<ExamWithSchedule> {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Adjust to 0-based index

        // Get all TC exams
        val exams = loadTCExams()

        // Get schedules for these exams
        val examSchedules = mutableListOf<ExamWithSchedule>()

        for (exam in exams) {
            val schedules = firestore.collection("schedule")
                .whereEqualTo("courseId", exam.id)
                .whereGreaterThanOrEqualTo("day", currentDay)
                .orderBy("day", Query.Direction.ASCENDING)
                .limit(5)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val schedule = doc.toObject(Schedule::class.java) ?: return@mapNotNull null
                    schedule.copy(id = doc.id)
                }

            if (schedules.isNotEmpty()) {
                schedules.forEach { schedule ->
                    examSchedules.add(ExamWithSchedule(exam, schedule))
                }
            } else {
                // If no upcoming schedule, get any schedule for this exam
                val anySchedule = firestore.collection("schedule")
                    .whereEqualTo("courseId", exam.id)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.let { doc ->
                        val schedule = doc.toObject(Schedule::class.java) ?: return@let null
                        schedule.copy(id = doc.id)
                    }

                if (anySchedule != null) {
                    examSchedules.add(ExamWithSchedule(exam, anySchedule))
                }
            }
        }

        return examSchedules.sortedBy { it.schedule.day }
    }

    private suspend fun calculateAbsenceStats(students: List<Student>): AbsenceStatistics {
        // Total students in TC
        val totalStudents = students.size

        // Count students with absences
        var studentsWithAbsences = 0
        var totalAbsences = 0
        var totalExcusedAbsences = 0

        for (student in students) {
            val absences = firestore.collection("attendance")
                .whereEqualTo("studentId", student.userId)
                .whereEqualTo("isPresent", false)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Attendance::class.java) }

            if (absences.isNotEmpty()) {
                studentsWithAbsences++
                totalAbsences += absences.size

                // You might want to add an "excused" field to your Attendance model
                // This is just a placeholder calculation
                val excused = absences.count { attendance ->
                    // Logic to determine if an absence is excused
                    // For now, let's just say 20% of absences are excused
                    false // Replace with actual logic when you have a field for this
                }
                totalExcusedAbsences += excused
            }
        }

        return AbsenceStatistics(
            totalStudents = totalStudents,
            studentsWithAbsences = studentsWithAbsences,
            totalAbsences = totalAbsences,
            excusedAbsences = totalExcusedAbsences
        )
    }

    fun markAttendance(examId: String, studentIds: List<String>, isPresent: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isMarkingAttendance = true) }

                val timestamp = System.currentTimeMillis()
                val userId = auth.currentUser?.uid ?: ""

                for (studentId in studentIds) {
                    val attendanceId = UUID.randomUUID().toString()
                    val attendance = mapOf(
                        "id" to attendanceId,
                        "courseId" to examId,
                        "studentId" to studentId,
                        "timestamp" to timestamp,
                        "isPresent" to isPresent,
                        "captureMethod" to CaptureMethod.FACE_RECOGNITION.toString(),
                        "capturedBy" to userId
                    )

                    firestore.collection("attendance")
                        .document(attendanceId)
                        .set(attendance)
                        .await()
                }

                _uiState.update { it.copy(
                    isMarkingAttendance = false,
                    attendanceMarked = true
                )}

                // Reload data to reflect changes
                loadDashboardData()

            } catch (e: Exception) {
                Log.e(TAG, "Error marking attendance: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur d'enregistrement des présences/absences: ${e.message}",
                        isMarkingAttendance = false
                    )
                }
            }
        }
    }

    fun getStudentsForExam(examId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingStudents = true) }

                // Get the course details
                val course = firestore.collection("courses")
                    .document(examId)
                    .get()
                    .await()
                    .toObject(Course::class.java)
                    ?: throw Exception("Examen non trouvé")

                // Get students enrolled in this course
                val query = firestore.collection("students")
                    .whereEqualTo("level", "TC") // Base filter for TC level
                    .whereEqualTo("branch", course.branch) // Match course branch
                    .whereEqualTo("group", course.group)    // Match course group

                val students = query
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        val student = doc.toObject(Student::class.java) ?: return@mapNotNull null
                        student.copy(userId = doc.id)
                    }

                _uiState.update {
                    it.copy(
                        selectedExamId = examId,
                        selectedExamStudents = students,
                        isLoadingStudents = false
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading students for exam: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur de chargement des étudiants: ${e.message}",
                        isLoadingStudents = false
                    )
                }
            }
        }
    }

    fun resetFlags() {
        _uiState.update {
            it.copy(
                attendanceMarked = false,
                error = null
            )
        }
    }

    fun resetSelectedExam() {
        _uiState.update {
            it.copy(
                selectedExamId = "",
                selectedExamStudents = emptyList()
            )
        }
    }

    companion object {
        private const val TAG = "AdminViewModel"
    }
}

data class AdminUiState(
    val tcExams: List<Course> = emptyList(),
    val tcStudents: List<Student> = emptyList(),
    val absenceStats: AbsenceStatistics = AbsenceStatistics(),
    val upcomingExams: List<ExamWithSchedule> = emptyList(),
    val selectedExamId: String = "",
    val selectedExamStudents: List<Student> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingStudents: Boolean = false,
    val isMarkingAttendance: Boolean = false,
    val attendanceMarked: Boolean = false,
    val error: String? = null
)

data class AbsenceStatistics(
    val totalStudents: Int = 0,
    val studentsWithAbsences: Int = 0,
    val totalAbsences: Int = 0,
    val excusedAbsences: Int = 0
)

data class ExamWithSchedule(
    val exam: Course,
    val schedule: Schedule
)