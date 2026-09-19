package com.nuvio.app.features.home.components

import com.nuvio.app.features.watchprogress.ContinueWatchingSectionStyle
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeHeroSectionTest {

    @Test
    fun `mobile hero height stays compact without continue watching`() {
        val layout = homeHeroLayout(
            maxWidthDp = 390f,
            viewportHeightDp = 844f,
        )

        assertEquals(true, layout.centerTitle)
        assertEquals(452.4f, layout.heroHeight.value, 0.001f)
    }

    @Test
    fun `wide hero stays width driven and keeps its title leading`() {
        val layout = homeHeroLayout(
            maxWidthDp = 840f,
            viewportHeightDp = 1200f,
        )

        assertEquals(false, layout.centerTitle)
        assertEquals(336f, layout.heroHeight.value, 0.001f)
    }

    @Test
    fun `carousel hero reserves room for its indicator row`() {
        val layout = homeHeroLayout(maxWidthDp = 840f)

        assertEquals(
            layout.heroHeight.value + layout.contentVerticalPadding.value + 24f,
            layout.totalHeight.value,
            0.001f,
        )
    }

    @Test
    fun `mobile hero also reserves room for its indicator row`() {
        val layout = homeHeroLayout(maxWidthDp = 390f, viewportHeightDp = 844f)

        assertEquals(
            layout.heroHeight.value + layout.contentVerticalPadding.value + 24f,
            layout.totalHeight.value,
            0.001f,
        )
    }

    @Test
    fun `mobile hero height leaves room for continue watching card section`() {
        val viewportHeight = 844f
        val continueWatchingLayout = rememberContinueWatchingLayout(maxWidthDp = 390f)
        val continueWatchingHeight = continueWatchingSectionHeightEstimate(
            style = ContinueWatchingSectionStyle.Card,
            layout = continueWatchingLayout,
            basePosterWidthDp = 110,
        )
        val reserveHeight = continueWatchingHeroViewportReserveHeight(
            style = ContinueWatchingSectionStyle.Card,
            layout = continueWatchingLayout,
            basePosterWidthDp = 110,
        )
        val layout = homeHeroLayout(
            maxWidthDp = 390f,
            viewportHeightDp = viewportHeight,
            mobileBelowSectionHeightHintDp = reserveHeight.value,
        )

        assertEquals(24f, viewportHeight - layout.heroHeight.value - continueWatchingHeight.value, 0.001f)
    }

    @Test
    fun `mobile hero can shrink below default minimum to fit short viewport`() {
        val layout = homeHeroLayout(
            maxWidthDp = 390f,
            viewportHeightDp = 568f,
            mobileBelowSectionHeightHintDp = 300f,
        )

        assertEquals(true, layout.centerTitle)
        assertEquals(268f, layout.heroHeight.value, 0.001f)
    }
}
