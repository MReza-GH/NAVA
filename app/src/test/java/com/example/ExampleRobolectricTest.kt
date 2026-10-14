package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.components.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  @Config(qualifiers = "fa")
  fun `read Persian string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("نوا", appName)
  }

  @Test
  @Config(qualifiers = "en")
  fun `read English string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Nava", appName)
  }

  @Test
  fun `format duration tests`() {
    assertEquals("0:00", FormatUtils.formatDuration(0L))
    assertEquals("1:05", FormatUtils.formatDuration(65000L))
    assertEquals("1:00:00", FormatUtils.formatDuration(3600000L))
  }
}
