package com.example.gestionintelligentedesabsences.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val fullName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val role: UserRole = UserRole.STUDENT,
    val profileImageUrl: String = ""
)

enum class UserRole {
    SUPER_ADMIN,
    ADMIN,
    TEACHER,
    STUDENT
}













