package com.example.gestionintelligentedesabsences.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.gestionintelligentedesabsences.R
import com.example.gestionintelligentedesabsences.data.model.Schedule

@Composable
fun ScheduleItem(
    schedule: Schedule,
    courseName: String,
    level: String = "",
    branch: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.school),
            contentDescription = "School Icon",
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = "$courseName - ${getDayName(schedule.day)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "De ${schedule.startTime} à ${schedule.endTime} | Salle: ${schedule.room}",
                style = MaterialTheme.typography.bodyMedium
            )

            if (level.isNotEmpty() || branch.isNotEmpty()) {
                Text(
                    text = buildString {
                        if (level.isNotEmpty()) append("Niveau: $level")
                        if (level.isNotEmpty() && branch.isNotEmpty()) append(" | ")
                        if (branch.isNotEmpty()) append("Filière: $branch")
                    },
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    } // This closing brace was missing
}

// Helper function pour convertir un Int -> nom du jour
fun getDayName(day: Int): String = when (day) {
    1 -> "Lundi"
    2 -> "Mardi"
    3 -> "Mercredi"
    4 -> "Jeudi"
    5 -> "Vendredi"
    6 -> "Samedi"
    7 -> "Dimanche"
    else -> "Inconnu"
}