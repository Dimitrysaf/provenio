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

        assertEquals(HomeHeroStyle.Immersive, layout.style)
        assertEquals(452.4f, layout.heroHeight.value, 0.001f)
    }

    @Test
    fun `wide hero switches to the carousel and stays width driven`() {
        val layout = homeHeroLayout(
            maxWidthDp = 840f,
            viewportHeightDp = 1200f,
        )

        assertEquals(HomeHeroStyle.Carousel, layout.style)
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
    fun `immersive hero occupies only its own height`() {
        val layout = homeHeroLayout(maxWidthDp = 390f, viewportHeightDp = 844f)

        assertEquals(layout.heroHeight.value, layout.totalHeight.value, 0.001f)
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

        assertEquals(HomeHeroStyle.Immersive, layout.style)
        assertEquals(268f, layout.heroHeight.value, 0.001f)
    }
}
