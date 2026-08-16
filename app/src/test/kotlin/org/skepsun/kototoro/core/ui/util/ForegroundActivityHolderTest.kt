package com.mangaverse.app.core.ui.util

import android.app.Activity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk

class ForegroundActivityHolderTest : FunSpec({

    lateinit var holder: ForegroundActivityHolder

    beforeTest {
        holder = ForegroundActivityHolder()
    }

    test("resumed activity is exposed as current") {
        val activity = mockk<Activity>(relaxed = true)

        holder.onActivityResumed(activity)

        holder.current shouldBe activity
    }

    test("paused activity is cleared from current") {
        val activity = mockk<Activity>(relaxed = true)

        holder.onActivityResumed(activity)
        holder.onActivityPaused(activity)

        holder.current.shouldBeNull()
    }

    test("destroyed activity is cleared from current") {
        val activity = mockk<Activity>(relaxed = true)

        holder.onActivityResumed(activity)
        holder.onActivityDestroyed(activity)

        holder.current.shouldBeNull()
    }

    test("destroying an old activity does not clear the newer activity") {
        val oldActivity = mockk<Activity>(relaxed = true)
        val newActivity = mockk<Activity>(relaxed = true)

        holder.onActivityResumed(oldActivity)
        holder.onActivityDestroyed(oldActivity)
        holder.onActivityResumed(newActivity)
        holder.onActivityDestroyed(oldActivity)

        holder.current shouldBe newActivity
    }
})
