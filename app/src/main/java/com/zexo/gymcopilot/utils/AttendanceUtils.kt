package com.zexo.gymcopilot.utils

import com.zexo.gymcopilot.ScheduleEntry
import java.util.Calendar

object AttendanceUtils {

    /**
     * Calcula cuántas clases se esperan en el mes calendario actual para un alumno específico,
     * basándose en las clases en las que está inscrito.
     */
    fun calculateExpectedClassesInMonth(schedules: List<ScheduleEntry>, memberEmail: String): Int {
        val normalizedEmail = memberEmail.lowercase().trim()
        
        // 1. Filtrar las clases donde el alumno está inscrito y que estén activas
        val myClasses = schedules.filter { entry ->
            entry.isEnabled && entry.assignedMemberEmails.any { it.lowercase().trim() == normalizedEmail }
        }

        if (myClasses.isEmpty()) return 0

        // 2. Obtener los días de la semana únicos donde tiene clase
        val classDaysOfWeek = myClasses.mapNotNull { it.dayOfWeek }.distinct()
        
        if (classDaysOfWeek.isEmpty()) return 0

        // 3. Contar cuántas veces ocurre cada día en el mes actual
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Ir al primer día del mes
        calendar.set(currentYear, currentMonth, 1)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        var totalExpected = 0
        for (day in 1..daysInMonth) {
            calendar.set(currentYear, currentMonth, day)
            // Ajustar Calendar.DAY_OF_WEEK (Sun=1, Mon=2...) al formato de la App (Mon=1...Sun=7)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val appDayOfWeek = if (dayOfWeek == Calendar.SUNDAY) 7 else dayOfWeek - 1
            
            if (classDaysOfWeek.contains(appDayOfWeek)) {
                totalExpected++
            }
        }
        
        return totalExpected
    }
}
