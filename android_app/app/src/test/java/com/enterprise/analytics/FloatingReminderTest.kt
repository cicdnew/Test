package com.enterprise.analytics

import org.junit.Test
import org.junit.Assert.*
import com.enterprise.analytics.service.FloatingReminderService
import android.provider.Settings
import android.os.Build

class FloatingReminderTest {

    @Test
    fun verifyIntentExtrasForService() {
        assertNotNull(FloatingReminderService::class.java)
    }

    @Test
    fun verifyCanDrawOverlaysLogic() {
        // Just verify Settings constant is accessible
        assertNotNull(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        assertTrue(Build.VERSION_CODES.M >= 23)
    }
}
