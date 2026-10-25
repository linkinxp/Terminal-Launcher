package dev.linkinxp.termlauncher

import android.app.Application
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The launcher has its own adaptive icon, with a monochrome layer so Android's themed icons can tint it. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppIconTest {
    @Test fun theAppIconIsAnAdaptiveIconWithAMonochromeLayer() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        assertEquals("the manifest points at the icon", R.mipmap.ic_launcher, app.applicationInfo.icon)
        val icon = androidx.core.content.ContextCompat.getDrawable(app, R.mipmap.ic_launcher)
        assertTrue("the icon is $icon", icon is AdaptiveIconDrawable)
        assertNotNull((icon as AdaptiveIconDrawable).monochrome)
        assertNotNull(icon.foreground)
        assertNotNull(icon.background)
    }
}
