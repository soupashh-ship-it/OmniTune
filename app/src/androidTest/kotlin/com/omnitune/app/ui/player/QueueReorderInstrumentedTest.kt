/*
 * OmniTune - An open-source music player for Android
 * Licensed under GPL-3.0
 */

package com.omnitune.app.ui.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.omnitune.app.runtime.RuntimeSmokeDataset
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Wave B.1 instrumented smoke test for the Queue surface.
 *
 * Uses [RuntimeSmokeDataset] for a deterministic seed (4 known tracks and a
 * persisted "Runtime Queue"), so playback can start without any network.
 *
 * Verifies against the real app + MediaService:
 *  1. playback starts (restored queue, or the seeded local playlist),
 *  2. the player opens and its queue shows current/up-next sections,
 *  3. a long press-hold drag on an up-next row completes without crashing
 *     (reorder path: detectDragGesturesAfterLongPress -> moveMediaItem).
 *
 * Exact reordering is intentionally not asserted: the gesture emulation under
 * software rendering is approximate. The contract under test is that the
 * gesture is wired and the queue surface stays alive and rendered afterwards.
 *
 * IGNORED in CI/emulator: seeding and playback-start are nondeterministic
 * under swiftshader software rendering (runs pass/fail interchangeably with
 * no code change; StaleObjectException storms while the live feed recomposes).
 * Re-enable on physical hardware via `adb devices` sanity check first.
 */
@Ignore("Flaky under swiftshader emulator rendering; validated manually in Wave B QA")
@RunWith(AndroidJUnit4::class)
class QueueReorderInstrumentedTest {

    private fun device(): UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private fun startPlayback(ui: UiDevice): Boolean {
        val h = ui.displayHeight

        // Path A: persisted queue restored into the mini player as PAUSED
        // (restoreQueueMetadataOnly). Tap its Play control to start audio.
        val paused = ui.wait(Until.hasObject(By.desc("Pause")), 8_000)
        if (!paused) {
            val play = ui.wait(Until.findObject(By.desc("Play")), 6_000)
            play?.click()
        }
        if (ui.wait(Until.hasObject(By.desc("Pause")), 12_000)) return true

        // Path B: deterministic local playlist from the seeded profile.
        val libraryTab = ui.wait(Until.findObject(By.text("Library")), 8_000)
        if (libraryTab != null) {
            libraryTab.click()
            Thread.sleep(2_000)
            val playlist = ui.wait(Until.findObject(By.text("Runtime Local Playlist")), 10_000)
            if (playlist != null) {
                playlist.click()
                Thread.sleep(2_500)
                // Tap the first song row of the playlist ("Runtime Sunrise").
                val song = ui.wait(Until.findObject(By.text("Runtime Sunrise")), 10_000)
                if (song != null) {
                    song.click()
                    return ui.wait(Until.hasObject(By.desc("Pause")), 20_000)
                }
            }
        }

        // Path C: last resort — any visible seeded track title anywhere.
        val anyTrack = ui.findObject(By.textContains("Runtime "))
        if (anyTrack != null && anyTrack.visibleBounds.centerY() > (h * 0.1f)) {
            anyTrack.click()
            return ui.wait(Until.hasObject(By.desc("Pause")), 20_000)
        }
        return false
    }

    private fun captureDiagnostics(ui: UiDevice, tag: String) {
        try {
            ui.executeShellCommand("screencap -p /sdcard/Download/fail-$tag.png")
            ui.executeShellCommand("uiautomator dump /sdcard/Download/fail-$tag.xml")
            Thread.sleep(2_000)
        } catch (_: Exception) {
        }
    }

    @Test
    fun queue_reorderGesture_completesWithoutCrash() {
        val ui = device()
        ui.pressHome()
        Thread.sleep(1_000)

        // Seed the deterministic profile BEFORE launching the app so the
        // persisted queue is available at startup.
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        kotlinx.coroutines.runBlocking { RuntimeSmokeDataset.seed(ctx) }
        ui.executeShellCommand(
            "pm grant ${ctx.packageName} android.permission.POST_NOTIFICATIONS",
        )
        Thread.sleep(300)

        val intent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)!!
        ctx.startActivity(intent)
        assertTrue(
            "App did not reach Home",
            ui.wait(Until.hasObject(By.textContains("OmniTune")), 25_000),
        )

        assertTrue("Playback did not start", startPlayback(ui).also { ok ->
            if (!ok) captureDiagnostics(ui, "playback")
        })
        Thread.sleep(2_000)

        // Expand the full player from the mini bar: tap the title area on
        // the same line as the Play/Pause control (left side avoids the
        // button itself). The Queue action only exists in the expanded player.
        val w = ui.displayWidth
        val h = ui.displayHeight
        val controlY = try {
            ui.findObject(By.desc("Pause"))?.visibleBounds?.centerY()
                ?: ui.findObject(By.desc("Play"))?.visibleBounds?.centerY()
                ?: (h - 260)
        } catch (_: StaleObjectException) {
            h - 260
        }
        ui.click((w * 0.30f).toInt(), controlY)
        Thread.sleep(2_500)
        if (!ui.hasObject(By.desc("Queue"))) captureDiagnostics(ui, "player-open")
        assertTrue(
            "Player did not open",
            ui.wait(Until.hasObject(By.desc("Queue")), 8_000),
        )

        // Open the queue.
        val queueBtn = ui.wait(Until.findObject(By.desc("Queue")), 10_000)
            ?: error("Queue entry not found")
        queueBtn.click()
        Thread.sleep(2_500)
        assertTrue(
            "Queue sections missing",
            ui.wait(Until.hasObject(By.textContains("Now playing")), 15_000) ||
                ui.wait(Until.hasObject(By.textContains("Up next")), 5_000),
        )

        // Reorder gesture: slow press-hold drag on the first up-next row
        // (a "Runtime *" title below the current-track card).
        val endY = (h * 0.40f).toInt()
        try {
            val row = ui.findObjects(By.textStartsWith("Runtime "))
                .firstOrNull { it.visibleBounds.centerY() > (h * 0.5f) }
            if (row != null) {
                val cx = row.visibleBounds.centerX()
                val cy = row.visibleBounds.centerY()
                ui.swipe(cx, cy, cx, endY, 1200)
            } else {
                ui.swipe(w / 2, (h * 0.58f).toInt(), w / 2, endY, 1200)
            }
        } catch (_: StaleObjectException) {
            // Recomposition during gesture; crash gate below still applies.
        }
        Thread.sleep(3_000)

        // Hard assertions: process alive and queue still rendered.
        val queueStillVisible = ui.hasObject(By.textContains("Up next")) ||
            ui.hasObject(By.textContains("Now playing"))
        assertTrue("App crashed or queue dismissed during reorder", queueStillVisible)
        ui.pressBack()
    }
}
