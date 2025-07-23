package ng.wimika.samplebankapp.utils

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.ISODateTimeFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())

    private val formatters = listOf(
        ISODateTimeFormat.dateTime(),
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS")
    )

    fun formatDate(dateTime: String): String {
        var parsedDateTime: DateTime? = null
        var lastError: Exception? = null

        for (formatter in formatters) {
            try {
                parsedDateTime = formatter.parseDateTime(dateTime)
                break
            } catch (e: Exception) {
                lastError = e
                continue
            }
        }

        if (parsedDateTime == null) {
            throw lastError ?: IllegalArgumentException("Could not parse date: $dateTime")
        }

        val outputFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
        return outputFormatter.print(parsedDateTime)
    }
    
    fun formatDate(date: Date): String {
        return dateFormat.format(date)
    }
    
    fun formatDateTime(date: Date): String {
        return dateTimeFormat.format(date)
    }
} 