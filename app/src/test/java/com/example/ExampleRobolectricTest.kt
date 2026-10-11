package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.Camera3D
import com.example.game.LevelGenerator
import com.example.game.Vec3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Rolling Rush", appName)
  }

  @Test
  fun `verify 20 levels generation`() {
    for (i in 1..20) {
      val level = LevelGenerator.getLevel(i)
      assertEquals(i, level.number)
      assertTrue("Level $i should have positive length", level.totalLengthZ > 100f)
      assertTrue("Level $i should have segments", level.segments.isNotEmpty())
      assertTrue("Level $i should have base speed > 10", level.baseSpeed > 10f)
      assertNotNull("Level $i should have theme", level.theme)
    }
  }

  @Test
  fun `test 3D math and camera projection`() {
    val camera = Camera3D(
      position = Vec3(0f, 5f, -8f),
      target = Vec3(0f, 1f, 8f)
    )
    camera.updateVectors()

    val pCenter = camera.project(Vec3(0f, 0f, 10f), 1080f, 1920f)
    val pRight = camera.project(Vec3(4f, 0f, 10f), 1080f, 1920f)
    val pLeft = camera.project(Vec3(-4f, 0f, 10f), 1080f, 1920f)

    assertNotNull(pCenter)
    assertNotNull(pRight)
    assertNotNull(pLeft)
    assertTrue("Right side point must have x > center", pRight!!.x > pCenter!!.x)
    assertTrue("Left side point must have x < center", pLeft!!.x < pCenter.x)
  }
}
