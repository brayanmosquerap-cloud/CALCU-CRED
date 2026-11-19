package com.example

import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppOfficialIconAndLabelVerificationTest {

    @Test
    fun `01 visible launcher label is CRED & EDI and manifest points to official launcher icons`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("CRED & EDI", context.getString(R.string.app_name))

        val appInfo = context.applicationInfo
        assertEquals(R.mipmap.ic_launcher, appInfo.icon)
        assertEquals("CRED & EDI", context.packageManager.getApplicationLabel(appInfo).toString())
    }

    @Test
    fun `02 MEDIO-07 ICON-01 official icon and adaptive resources load and render without clipping`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val launcherDrawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
        val roundLauncherDrawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher_round)
        val foregroundDrawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)
        val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
        val foregroundLogoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground_logo)
        val fullOfficialLogoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_cred_edi_official_logo)

        assertNotNull(launcherDrawable)
        assertNotNull(roundLauncherDrawable)
        assertNotNull(foregroundDrawable)
        assertNotNull(backgroundDrawable)
        assertNotNull(foregroundLogoDrawable)
        assertNotNull(fullOfficialLogoDrawable)

        // Verify raw unscaled official logo preserves 1:1 square aspect ratio (1254x1254)
        val opts = BitmapFactory.Options().apply { inScaled = false }
        val rawOfficialBmp = BitmapFactory.decodeResource(context.resources, R.drawable.ic_cred_edi_official_logo, opts)
        assertNotNull(rawOfficialBmp)
        assertEquals(1254, rawOfficialBmp.width)
        assertEquals(1254, rawOfficialBmp.height)

        val rawForegroundBmp = BitmapFactory.decodeResource(context.resources, R.drawable.ic_launcher_foreground_logo, opts)
        assertNotNull(rawForegroundBmp)
        val w = rawForegroundBmp.width
        val h = rawForegroundBmp.height
        assertEquals(1400, w)
        assertEquals(1400, h)

        // Verify outer perimeter of the 1400x1400 foreground master has pure white (#FFFFFF) safe margin
        val edgePoints = listOf(
            0 to 0,
            w - 1 to 0,
            0 to h - 1,
            w - 1 to h - 1,
            w / 2 to 0,
            w / 2 to h - 1,
            0 to h / 2,
            w - 1 to h / 2
        )
        for ((x, y) in edgePoints) {
            val pixel = rawForegroundBmp.getPixel(x, y)
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            assertTrue("Expected white safe margin at ($x,$y), got rgb=($r,$g,$b)", r >= 250 && g >= 250 && b >= 250)
        }
    }
}
