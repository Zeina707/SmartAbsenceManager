package com.example.gestionintelligentedesabsences


import android.app.Application
import com.google.firebase.FirebaseApp

class AbsenceManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}