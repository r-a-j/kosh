package com.rajpawardotin.kosh.data.agent.native

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.rajpawardotin.kosh.domain.agent.Tool
import com.rajpawardotin.kosh.domain.agent.ToolParam
import java.util.Date

class CalendarSkill(
    private val context: Context,
    private val permissionRequester: com.rajpawardotin.kosh.domain.agent.PermissionRequester
) {

    @Tool(name = "get_calendar_events", description = "Retrieves upcoming calendar events on the device for the next N days")
    suspend fun getCalendarEvents(
        @ToolParam(name = "days", description = "Number of days in the future to query. Defaults to 1 (today)", required = false) days: Int?
    ): String {
        val daysToQuery = days ?: 1
        
        // Check and dynamically request permission
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            val granted = permissionRequester.requestPermission(android.Manifest.permission.READ_CALENDAR)
            if (!granted) {
                return "Error: READ_CALENDAR permission denied. Tell the user to enable calendar permissions in Android settings."
            }
        }

        return try {
            val contentResolver = context.contentResolver
            val uri = CalendarContract.Events.CONTENT_URI
            
            val startMillis = System.currentTimeMillis()
            val endMillis = startMillis + (daysToQuery * 24 * 60 * 60 * 1000L)

            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_LOCATION
            )
            
            val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
            val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

            val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
            val eventsList = mutableListOf<String>()

            cursor?.use {
                val titleIdx = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = it.getColumnIndex(CalendarContract.Events.DTEND)
                val locIdx = it.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

                while (it.moveToNext()) {
                    val title = if (titleIdx != -1) it.getString(titleIdx) else "No Title"
                    val start = if (startIdx != -1) it.getLong(startIdx) else 0L
                    val end = if (endIdx != -1) it.getLong(endIdx) else 0L
                    val location = if (locIdx != -1) it.getString(locIdx) else null

                    val startDate = Date(start).toString()
                    val locStr = if (location.isNullOrEmpty()) "" else " at $location"
                    eventsList.add("- $title ($startDate)$locStr")
                }
            }

            if (eventsList.isEmpty()) {
                "No calendar events scheduled for the next $daysToQuery days."
            } else {
                "Calendar events for the next $daysToQuery days:\n" + eventsList.joinToString("\n")
            }
        } catch (e: Exception) {
            "Error querying calendar: ${e.message}"
        }
    }
}
