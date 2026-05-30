package com.rajpawardotin.kosh.data.agent.native

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.rajpawardotin.kosh.domain.agent.PermissionRequester
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CalendarSkillTest {

    @Test
    fun testPermissionDenied() = runBlocking {
        val context = mock<Context>()
        val permissionRequester = mock<PermissionRequester>()
        
        val calendarSkill = CalendarSkill(context, permissionRequester)

        Mockito.mockStatic(ContextCompat::class.java).use { mockedContextCompat ->
            mockedContextCompat.`when`<Int> {
                ContextCompat.checkSelfPermission(anyOrNull(), anyOrNull())
            }.thenReturn(PackageManager.PERMISSION_DENIED)

            whenever(permissionRequester.requestPermission(android.Manifest.permission.READ_CALENDAR))
                .thenReturn(false)

            val result = calendarSkill.getCalendarEvents(1)
            assertTrue(result.contains("permission denied"))
        }
    }

    @Test
    fun testPermissionGrantedAndEventsFound() = runBlocking {
        val context = mock<Context>()
        val permissionRequester = mock<PermissionRequester>()
        val contentResolver = mock<ContentResolver>()
        val cursor = mock<Cursor>()

        whenever(context.contentResolver).thenReturn(contentResolver)
        
        // Setup cursor columns
        whenever(cursor.getColumnIndex(CalendarContract.Events.TITLE)).thenReturn(0)
        whenever(cursor.getColumnIndex(CalendarContract.Events.DTSTART)).thenReturn(1)
        whenever(cursor.getColumnIndex(CalendarContract.Events.DTEND)).thenReturn(2)
        whenever(cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)).thenReturn(3)

        // Setup cursor iteration (one event)
        whenever(cursor.moveToNext()).thenReturn(true).thenReturn(false)
        whenever(cursor.getString(0)).thenReturn("Team Standup")
        whenever(cursor.getLong(1)).thenReturn(1717000000000L) // Some fixed timestamp
        whenever(cursor.getLong(2)).thenReturn(1717003600000L)
        whenever(cursor.getString(3)).thenReturn("Room 101")

        whenever(contentResolver.query(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(cursor)

        val calendarSkill = CalendarSkill(context, permissionRequester)

        Mockito.mockStatic(ContextCompat::class.java).use { mockedContextCompat ->
            mockedContextCompat.`when`<Int> {
                ContextCompat.checkSelfPermission(anyOrNull(), anyOrNull())
            }.thenReturn(PackageManager.PERMISSION_GRANTED)

            val result = calendarSkill.getCalendarEvents(1)
            assertTrue(result.contains("Team Standup"))
            assertTrue(result.contains("Room 101"))
        }
    }
}
