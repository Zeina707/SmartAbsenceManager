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

class ProfessorViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ProfessorUiState())
    val uiState: StateFlow<ProfessorUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Current filters
    private var currentLevel = "L" // Default branch filter
    private var currentIsExam = true // Default isExam filter

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Load user (professor) details
                val professorId = auth.currentUser?.uid ?: ""
                val professorCourses = loadProfessorCourses(professorId)

                // Load L branch stats
                val lStudents = loadBranchStudents("L")
                val lAbsenceStats = calculateAbsenceStats(lStudents)

                // Load M branch stats
                val mStudents = loadBranchStudents("M")
                val mAbsenceStats = calculateAbsenceStats(mStudents)

                // Load upcoming courses/exams for L branch
                val upcomingLCoursesAndExams = loadUpcomingCoursesAndExams(professorId, "L")

                // Load upcoming courses/exams for M branch
                val upcomingMCoursesAndExams = loadUpcomingCoursesAndExams(professorId, "M")

                _uiState.update {
                    it.copy(
                        professorCourses = professorCourses,
                        lStudents = lStudents,
                        mStudents = mStudents,
                        lAbsenceStats = lAbsenceStats,
                        mAbsenceStats = mAbsenceStats,
                        upcomingLCoursesAndExams = upcomingLCoursesAndExams,
                        upcomingMCoursesAndExams = upcomingMCoursesAndExams,
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

    private suspend fun loadProfessorCourses(professorId: String): List<Course> {
        return firestore.collection("courses")
            .whereEqualTo("teacherId", professorId)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Log.d(TAG, "Course data from Firestore: $data")
                Log.d(TAG, "isExam raw value: ${data["isExam"]}, type: ${data["isExam"]?.javaClass?.name}")

                Course(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    teacherId = data["teacherId"] as? String ?: "",
                    level = data["level"] as? String ?: "",
                    branch = data["branch"] as? String ?: "",
                    group = data["group"] as? String ?: "",
                    isExam = data["isExam"] as? Boolean ?: false
                )
            }
    }

    private suspend fun loadBranchStudents(branch: String): List<Student> {
        return firestore.collection("students")
            .whereEqualTo("level", branch) // L or M
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val student = doc.toObject(Student::class.java) ?: return@mapNotNull null
                student.copy(userId = doc.id)
            }
    }

    private suspend fun loadFilteredCourses(professorId: String, branch: String, isExam: Boolean): List<Course> {
        var query = firestore.collection("courses")
            .whereEqualTo("teacherId", professorId)
            .whereEqualTo("isExam", isExam)

        query = query.whereEqualTo("level", branch)

        return query.get()
            .await()
            .documents
            .mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Log.d(TAG, "Filtered course data: $data, isExam value: ${data["isExam"]}")

                Course(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    teacherId = data["teacherId"] as? String ?: "",
                    level = data["level"] as? String ?: "",
                    branch = data["branch"] as? String ?: "",
                    group = data["group"] as? String ?: "",
                    isExam = data["isExam"] as? Boolean ?: false
                )
            }
    }

    private suspend fun loadUpcomingCoursesAndExams(professorId: String, branch: String): List<CourseWithSchedule> {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Adjust to 0-based index

        // Get courses with manual mapping
        val courses = firestore.collection("courses")
            .whereEqualTo("teacherId", professorId)
            .whereEqualTo("level", branch)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                Course(
                    id = doc.id,
                    name = data["name"] as? String ?: "",
                    teacherId = data["teacherId"] as? String ?: "",
                    level = data["level"] as? String ?: "",
                    branch = data["branch"] as? String ?: "",
                    group = data["group"] as? String ?: "",
                    isExam = data["isExam"] as? Boolean ?: false
                )
            }

        // Get schedules for these courses
        val courseSchedules = mutableListOf<CourseWithSchedule>()

        for (course in courses) {
            val schedules = firestore.collection("schedule")
                .whereEqualTo("courseId", course.id)
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
                    courseSchedules.add(CourseWithSchedule(course, schedule))
                }
            } else {
                // If no upcoming schedule, get any schedule for this course
                val anySchedule = firestore.collection("schedule")
                    .whereEqualTo("courseId", course.id)
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
                    courseSchedules.add(CourseWithSchedule(course, anySchedule))
                }
            }
        }

        return courseSchedules.sortedBy { it.schedule.day }
    }

    private suspend fun calculateAbsenceStats(students: List<Student>): AbsenceStatistics2 {
        val totalStudents = students.size
        var studentsWithAbsences = 0
        var totalAbsences = 0

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

            }
        }

        return AbsenceStatistics2(
            totalStudents = totalStudents,
            studentsWithAbsences = studentsWithAbsences,
            totalAbsences = totalAbsences,
        )
    }

    fun markAttendance(courseId: String, studentIds: List<String>, isPresent: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isMarkingAttendance = true) }

                val timestamp = System.currentTimeMillis()
                val userId = auth.currentUser?.uid ?: ""

                for (studentId in studentIds) {
                    val attendanceId = UUID.randomUUID().toString()
                    val attendance = mapOf(
                        "id" to attendanceId,
                        "courseId" to courseId,
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

    fun getStudentsForCourse2(courseId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoadingStudents = true) }

                // Get the course details
                val course = firestore.collection("courses")
                    .document(courseId)
                    .get()
                    .await()
                    .toObject(Course::class.java)
                    ?: throw Exception("Cours/Examen non trouvé")

                // Log course details for debugging
                Log.d(TAG, "Fetching students for course: ${course.name}, ID: ${course.id}, Level: ${course.level}, Branch: ${course.branch}, Group: ${course.group}")

                // Get students enrolled in this course, filtered by branch and group
                val query = firestore.collection("students")
                    .whereEqualTo("level", course.level)
                    .apply {
                        if (course.branch.isNotEmpty()) {
                            whereEqualTo("branch", course.branch)
                            Log.d(TAG, "Applying branch filter: ${course.branch}")
                        } else {
                            Log.w(TAG, "Branch is empty for course: ${course.id}")
                        }
                        if (course.group.isNotEmpty()) {
                            whereEqualTo("group", course.group)
                            Log.d(TAG, "Applying group filter: ${course.group}")
                        } else {
                            Log.w(TAG, "Group is empty for course: ${course.id}")
                        }
                    }

                val students = query
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        val student = doc.toObject(Student::class.java) ?: return@mapNotNull null
                        Log.d(TAG, "Student: ${student.firstName} ${student.lastName}, ID: ${student.studentId}, Branch: ${student.branch}, Group: ${student.group}")
                        student.copy(userId = doc.id)
                    }

                _uiState.update {
                    it.copy(
                        selectedCourseId = courseId,
                        selectedCourseStudents = students,
                        isLoadingStudents = false
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading students for course: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur de chargement des étudiants: ${e.message}",
                        isLoadingStudents = false
                    )
                }
            }
        }
    }

    fun updateFilters(level: String, isExam: Boolean) {
        currentLevel = level
        currentIsExam = isExam

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val professorId = auth.currentUser?.uid ?: ""
                val filteredCourses = loadFilteredCourses(professorId, level, isExam)

                _uiState.update {
                    it.copy(
                        currentFilteredCourses = filteredCourses,
                        currentLevel = level,
                        currentIsExam = isExam,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error updating filters: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur de filtrage des cours: ${e.message}",
                        isLoading = false
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

    fun resetSelectedCourse() {
        _uiState.update {
            it.copy(
                selectedCourseId = "",
                selectedCourseStudents = emptyList()
            )
        }
    }

    companion object {
        private const val TAG = "ProfessorViewModel"
    }
}

data class ProfessorUiState(
    val professorCourses: List<Course> = emptyList(),
    val lStudents: List<Student> = emptyList(),
    val mStudents: List<Student> = emptyList(),
    val lAbsenceStats: AbsenceStatistics2 = AbsenceStatistics2(),
    val mAbsenceStats: AbsenceStatistics2 = AbsenceStatistics2(),
    val upcomingLCoursesAndExams: List<CourseWithSchedule> = emptyList(),
    val upcomingMCoursesAndExams: List<CourseWithSchedule> = emptyList(),
    val currentFilteredCourses: List<Course> = emptyList(),
    val currentLevel: String = "L",
    val currentIsExam: Boolean = true,
    val selectedCourseId: String = "",
    val selectedCourseStudents: List<Student> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingStudents: Boolean = false,
    val isMarkingAttendance: Boolean = false,
    val attendanceMarked: Boolean = false,
    val error: String? = null
)

data class CourseWithSchedule(
    val course: Course,
    val schedule: Schedule
)

data class AbsenceStatistics2(
    val totalStudents: Int = 0,
    val studentsWithAbsences: Int = 0,
    val totalAbsences: Int = 0,
)