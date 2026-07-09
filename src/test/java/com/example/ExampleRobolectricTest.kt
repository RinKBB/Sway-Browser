package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.downloader.ImageSearchService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Sway Browser", appName)
  }

  @Test
  fun `test image search matching and empty state yields`() = runBlocking {
    // 1. A random non-matching query like 'ananan' or 'pqiqhbs' should return absolutely nothing, triggering empty state
    val emptyResultOne = ImageSearchService.searchImages("ananan")
    val emptyResultTwo = ImageSearchService.searchImages("pqiqhbs")
    assertEquals(0, emptyResultOne.size)
    assertEquals(0, emptyResultTwo.size)

    // 2. A valid standard query like 'Космос' or 'space' should return high-fidelity matched images
    val spaceResult = ImageSearchService.searchImages("Космос")
    assertTrue(spaceResult.isNotEmpty())
    assertTrue(spaceResult.any { it.title.contains("галактика") || it.title.contains("Путь") })

    // 3. A valid query like 'котик' should match and return cat images with appropriate titles
    val catResult = ImageSearchService.searchImages("котик")
    assertTrue(catResult.isNotEmpty())
    assertTrue(catResult.any { it.title.contains("кот") || it.title.contains("кошка") })
  }
}
