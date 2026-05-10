package com.gramaangana.utils

object DateUtils {
    fun formatDate(dateString: String): String {
        return try {
            val parts = dateString.split("-")
            if (parts.size == 3) {
                val months = listOf("Jan","Feb","Mar","Apr","May","Jun",
                    "Jul","Aug","Sep","Oct","Nov","Dec")
                val month = months[parts[1].toInt() - 1]
                "${parts[2]} $month ${parts[0]}"
            } else dateString
        } catch (e: Exception) { dateString }
    }

    fun getTodayString(): String {
        val cal = java.util.Calendar.getInstance()
        return "%04d-%02d-%02d".format(
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH) + 1,
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    fun formatTime(time: String): String {
        return try {
            val parts = time.split(":")
            val hour = parts[0].toInt()
            val min = parts[1]
            val ampm = if (hour < 12) "AM" else "PM"
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            "$h:$min $ampm"
        } catch (e: Exception) { time }
    }
}

object ValidationUtils {
    fun validatePhone(phone: String): Boolean =
        phone.matches(Regex("^[6-9]\\d{9}$"))

    fun validateName(name: String): Boolean =
        name.trim().length >= 2

    fun validateTime(start: String, end: String): Boolean {
        return try {
            val (sh, sm) = start.split(":").map { it.toInt() }
            val (eh, em) = end.split(":").map { it.toInt() }
            (sh * 60 + sm) < (eh * 60 + em)
        } catch (e: Exception) { false }
    }
}
