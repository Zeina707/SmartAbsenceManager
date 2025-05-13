package com.example.gestionintelligentedesabsences.ui.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gestionintelligentedesabsences.data.model.Course
import com.example.gestionintelligentedesabsences.data.model.Schedule
import com.example.gestionintelligentedesabsences.data.model.Student
import com.example.gestionintelligentedesabsences.data.model.User
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

class SuperAdminViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SuperAdminUiState())
    val uiState: StateFlow<SuperAdminUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Load users statistics
                val userStats = loadUserStatistics()

                // Load courses
                val courses = loadAllCourses()

                // Load schedules (renommé)
                val schedules = loadSchedules()

                _uiState.update {
                    it.copy(
                        userStats = userStats,
                        courses = courses,
                        schedules = schedules,
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

    private suspend fun loadUserStatistics(): UserStatistics {
        val userCollection = firestore.collection("users")

        // Count users by role
        val studentsCount = firestore.collection("students").get().await().size()
        val teachersCount = userCollection.whereEqualTo("role", "TEACHER").get().await().size()
        val adminsCount = userCollection.whereIn("role", listOf("ADMIN", "SUPER_ADMIN")).get().await().size()

        // Get recent users
        val recentUsers = userCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .await()
            .toObjects(User::class.java)

        return UserStatistics(
            totalStudents = studentsCount,
            totalTeachers = teachersCount,
            totalAdmins = adminsCount,
            recentUsers = recentUsers
        )
    }

    private suspend fun loadAllCourses(): List<Course> {
        return firestore.collection("courses")
            .get()
            .await()
            .documents
            .map { doc ->
                val course = doc.toObject(Course::class.java) ?: Course()
                val isExam = doc.getBoolean("isExam") ?: false
                Log.d(TAG, "Course ${doc.id} - ${course.name} isExam: $isExam")
                course.copy(id = doc.id, isExam = isExam)            }
            .sortedBy { it.name }
    }

    private suspend fun loadSchedules(): List<Schedule> {
        val calendar = Calendar.getInstance()
        val currentDate = calendar.timeInMillis

        return firestore.collection("schedule")
            //.whereGreaterThan("date", currentDate)
            //.orderBy("date", Query.Direction.ASCENDING)
            .limit(10)
            .get()
            .await()
            .documents
            .map { doc ->
                val schedule = doc.toObject(Schedule::class.java) ?: Schedule()
                schedule.copy(id = doc.id)  // Ensure ID is properly set
            }
    }

    fun createUser(email: String, password: String, firstName: String, lastName: String,
                   role: String, studentId: String = "", branch: String = "", level: String = "", group: String ="",teacherId: String = "") {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isCreatingUser = true) }

                // Create authentication account
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: throw Exception("User creation failed")

                // Create user document with the CURRENT admin's ID as creator
                val userData = hashMapOf(
                    "id" to uid,  // Add this explicit id field
                    "email" to email,
                    "firstName" to firstName,
                    "lastName" to lastName,
                    "role" to role.uppercase(),
                    "createdAt" to Calendar.getInstance().time,
                    "createdBy" to auth.currentUser?.uid  // Use the current admin's ID
                )

                firestore.collection("users")
                    .document(uid)
                    .set(userData)
                    .await()

                // If user is a student, create a student profile as well
                if (role.equals("student", ignoreCase = true)) {
                    val studentData = hashMapOf(
                        "userId" to uid,  // Associate with the auth user
                        "email" to email,
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "studentId" to (if (studentId.isNotEmpty()) studentId else uid),
                        "level" to level.ifEmpty { "TC" },  // Default to Tronc Commun if empty
                        "branch" to branch,
                        "group" to group,
                        "status" to "active",
                        "enrollmentYear" to Calendar.getInstance().get(Calendar.YEAR)
                    )

                    firestore.collection("students")
                        .document(uid)  // Use the auth UID to link the student to the user
                        .set(studentData)
                        .await()


                }
                // If user is a teacher, create a teacher profile as well
                if (role.equals("teacher", ignoreCase = true)) {
                    val teacherData = hashMapOf(
                        "userId" to uid,
                        "teacherId" to (if (teacherId.isNotEmpty()) teacherId else uid),
                        "firstName" to firstName,
                        "lastName" to lastName
                    )

                    firestore.collection("teachers")
                        .document(uid)
                        .set(teacherData)
                        .await()
                }

                // Refresh data
                loadDashboardData()

                _uiState.update { it.copy(
                    isCreatingUser = false,
                    userCreated = true
                )}

            } catch (e: Exception) {
                Log.e(TAG, "Error creating user: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur de création d'utilisateur: ${e.message}",
                        isCreatingUser = false
                    )
                }
            }
        }
    }

    // Add function to load teachers for dropdown selection
    suspend fun loadTeachers(): List<User> {
        return firestore.collection("users")
            .whereEqualTo("role", "TEACHER")
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }
    }
    fun createCourse(name: String, code: String, level: String, teacherId: String,  branch: String = "", group: String = "", isExam: Boolean = false) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isCreatingCourse = true) }

                val courseData = hashMapOf(
                    "name" to name,
                    "code" to code,
                    "level" to level,
                    "teacherId" to teacherId,
                    //"description" to description,
                    "branch" to branch,
                    "group" to group,
                   // "semester" to getCurrentSemester(),
                    "academicYear" to getCurrentAcademicYear(),
                    "createdAt" to Calendar.getInstance().time,
                    "createdBy" to auth.currentUser?.uid,
                    "isExam" to isExam
                )
                Log.d(TAG, "Creating course with isExam: $isExam")  // Ajoutez ce log pour déboguer
                firestore.collection("courses")
                    .add(courseData)
                    .await()

                // Refresh data
                loadDashboardData()

                _uiState.update { it.copy(
                    isCreatingCourse = false,
                    courseCreated = true
                )}

            } catch (e: Exception) {
                Log.e(TAG, "Error creating course: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur de création de cours: ${e.message}",
                        isCreatingCourse = false
                    )
                }
            }
        }
    }

    fun createSchedule(courseId: String, day: Int, startTime: String, endTime: String,
                       room: String, recurring: Boolean = true) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isCreatingSchedule = true) }  // Renommé

                val scheduleData = hashMapOf(
                    "courseId" to courseId,
                    "day" to day,
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "room" to room,
                    "recurring" to recurring,
                    "createdAt" to Calendar.getInstance().time,
                    "createdBy" to auth.currentUser?.uid
                )

                firestore.collection("schedule")  // Changé de "events" à "schedule"
                    .add(scheduleData)
                    .await()

                // Refresh data
                loadDashboardData()

                _uiState.update { it.copy(
                    isCreatingSchedule = false,  // Renommé
                    scheduleCreated = true       // Renommé
                )}

            } catch (e: Exception) {
                Log.e(TAG, "Error creating schedule: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        error = "Erreur de création d'emploi du temps: ${e.message}",
                        isCreatingSchedule = false  // Renommé
                    )
                }
            }
        }
    }

    fun enrollStudentInCourse(studentId: String, courseId: String) {
        viewModelScope.launch {
            try {
                val enrollmentData = hashMapOf(
                    "enrolledAt" to Calendar.getInstance().time,
                    "enrolledBy" to auth.currentUser?.uid
                )

                firestore.collection("courses")
                    .document(courseId)
                    .collection("enrolledStudents")
                    .document(studentId)
                    .set(enrollmentData)
                    .await()

                // Create a notification for the student
                val student = firestore.collection("students")
                    .document(studentId)
                    .get()
                    .await()
                    .toObject(Student::class.java)

                val course = firestore.collection("courses")
                    .document(courseId)
                    .get()
                    .await()
                    .toObject(Course::class.java)

                if (student != null && course != null) {
                    val notificationData = hashMapOf(
                        "userId" to studentId,
                        "title" to "Inscription à un cours",
                        "message" to "Vous avez été inscrit au cours ${course.name}",
                        "type" to "info",
                        "read" to false,
                        "createdAt" to Calendar.getInstance().time
                    )

                    firestore.collection("notifications")
                        .add(notificationData)
                        .await()
                }

                _uiState.update { it.copy(studentEnrolled = true) }

            } catch (e: Exception) {
                Log.e(TAG, "Error enrolling student: ${e.message}", e)
                _uiState.update {
                    it.copy(error = "Erreur d'inscription d'étudiant: ${e.message}")
                }
            }
        }
    }

    fun assignTeacherToCourse(teacherId: String, courseId: String) {
        viewModelScope.launch {
            try {
                // Update the course's teacherId
                firestore.collection("courses")
                    .document(courseId)
                    .update("teacherId", teacherId)
                    .await()

                // Refresh data
                loadDashboardData()

                _uiState.update { it.copy(teacherAssigned = true) }

            } catch (e: Exception) {
                Log.e(TAG, "Error assigning teacher: ${e.message}", e)
                _uiState.update {
                    it.copy(error = "Erreur d'affectation d'enseignant: ${e.message}")
                }
            }
        }
    }

    fun resetFlags() {
        _uiState.update {
            it.copy(
                userCreated = false,
                courseCreated = false,
                scheduleCreated  = false,
                studentEnrolled = false,
                teacherAssigned = false,
                error = null
            )
        }
    }

    private fun getCurrentSemester(): Int {
        val month = Calendar.getInstance().get(Calendar.MONTH)
        // Assuming September-January is semester 1, February-June is semester 2
        return if (month in 8..0) 1 else 2
    }

    private fun getCurrentAcademicYear(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)

        // Academic year is generally formatted as YYYY-YYYY+1
        return if (month >= 8) {
            "$year-${year + 1}"
        } else {
            "${year - 1}-$year"
        }
    }

    companion object {
        private const val TAG = "SuperAdminViewModel"
    }
}

data class SuperAdminUiState(
    val userStats: UserStatistics = UserStatistics(),
    val courses: List<Course> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = false,
    val isCreatingUser: Boolean = false,
    val isCreatingCourse: Boolean = false,
    val isCreatingSchedule: Boolean = false,  // Renommé
    val userCreated: Boolean = false,
    val courseCreated: Boolean = false,
    val scheduleCreated: Boolean = false,     // Renommé
    val studentEnrolled: Boolean = false,
    val teacherAssigned: Boolean = false,
    val error: String? = null
)

data class UserStatistics(
    val totalStudents: Int = 0,
    val totalTeachers: Int = 0,
    val totalAdmins: Int = 0,
    val recentUsers: List<User> = emptyList()
)