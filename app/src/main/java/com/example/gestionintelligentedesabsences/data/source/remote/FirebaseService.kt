package com.example.gestionintelligentedesabsences.data.source.remote

import com.example.gestionintelligentedesabsences.data.model.Admin
import com.example.gestionintelligentedesabsences.data.model.Attendance
import com.example.gestionintelligentedesabsences.data.model.Course
import com.example.gestionintelligentedesabsences.data.model.Schedule
import com.example.gestionintelligentedesabsences.data.model.Student
import com.example.gestionintelligentedesabsences.data.model.SuperAdmin
import com.example.gestionintelligentedesabsences.data.model.Teacher
import com.example.gestionintelligentedesabsences.data.model.User
import com.example.gestionintelligentedesabsences.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FirebaseService {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    // Auth Functions
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            authResult.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Authentication failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun createUser(email: String, password: String, user: User): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(Exception("User creation failed"))

            // Store user data in Firestore
            val userWithId = user.copy(id = userId)
            firestore.collection("users").document(userId).set(userWithId).await()

            // Create specific role collection
            when(user.role) {
                UserRole.STUDENT -> {
                    val student = Student(userId = userId)
                    firestore.collection("students").document(userId).set(student).await()
                }
                UserRole.TEACHER -> {
                    val teacher = Teacher(userId = userId)
                    firestore.collection("teachers").document(userId).set(teacher).await()
                }
                UserRole.ADMIN -> {
                    val admin = Admin(userId = userId)
                    firestore.collection("admins").document(userId).set(admin).await()
                }
                UserRole.SUPER_ADMIN -> {
                    val superAdmin = SuperAdmin(userId = userId)
                    firestore.collection("super_admins").document(userId).set(superAdmin).await()
                }
            }

            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // User Functions
    suspend fun getUser(userId: String): Result<User> {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            val user = document.toObject(User::class.java)
            user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("User not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByRole(role: UserRole): Flow<List<User>> = flow {
        try {
            val snapshot = firestore.collection("users")
                .whereEqualTo("role", role)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
            emit(users)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // Student Functions
    suspend fun getStudent(studentId: String): Result<Student> {
        return try {
            val document = firestore.collection("students").document(studentId).get().await()
            val student = document.toObject(Student::class.java)
            student?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Student not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStudentFaceData(studentId: String, faceData: String): Result<Boolean> {
        return try {
            firestore.collection("students").document(studentId)
                .update("faceData", faceData)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Course Functions
    suspend fun getCourses(): Flow<List<Course>> = flow {
        try {
            val snapshot = firestore.collection("courses").get().await()
            val courses = snapshot.documents.mapNotNull { it.toObject(Course::class.java) }
            emit(courses)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getTeacherCourses(teacherId: String): Flow<List<Course>> = flow {
        try {
            val snapshot = firestore.collection("courses")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()

            val courses = snapshot.documents.mapNotNull { it.toObject(Course::class.java) }
            emit(courses)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun createCourse(course: Course): Result<String> {
        return try {
            val courseRef = firestore.collection("courses").document()
            val courseWithId = course.copy(id = courseRef.id)
            courseRef.set(courseWithId).await()
            Result.success(courseRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Attendance Functions
    suspend fun recordAttendance(attendance: Attendance): Result<String> {
        return try {
            val attendanceRef = firestore.collection("attendance").document()
            val attendanceWithId = attendance.copy(id = attendanceRef.id)
            attendanceRef.set(attendanceWithId).await()
            Result.success(attendanceRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudentAttendance(studentId: String): Flow<List<Attendance>> = flow {
        try {
            val snapshot = firestore.collection("attendance")
                .whereEqualTo("studentId", studentId)
                .get()
                .await()

            val attendances = snapshot.documents.mapNotNull { it.toObject(Attendance::class.java) }
            emit(attendances)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun getCourseAttendance(courseId: String): Flow<List<Attendance>> = flow {
        try {
            val snapshot = firestore.collection("attendance")
                .whereEqualTo("courseId", courseId)
                .get()
                .await()

            val attendances = snapshot.documents.mapNotNull { it.toObject(Attendance::class.java) }
            emit(attendances)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    // Schedule Functions
    suspend fun getSchedule(): Flow<List<Schedule>> = flow {
        try {
            val snapshot = firestore.collection("schedules").get().await()
            val schedules = snapshot.documents.mapNotNull { it.toObject(Schedule::class.java) }
            emit(schedules)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun createSchedule(schedule: Schedule): Result<String> {
        return try {
            val scheduleRef = firestore.collection("schedules").document()
            val scheduleWithId = schedule.copy(id = scheduleRef.id)
            scheduleRef.set(scheduleWithId).await()
            Result.success(scheduleRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Storage Functions
    suspend fun uploadProfileImage(userId: String, imageUri: android.net.Uri): Result<String> {
        return try {
            val storageRef = storage.reference.child("profile_images/$userId.jpg")
            val uploadTask = storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Update user's profile image URL
            firestore.collection("users").document(userId)
                .update("profileImageUrl", downloadUrl)
                .await()

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}