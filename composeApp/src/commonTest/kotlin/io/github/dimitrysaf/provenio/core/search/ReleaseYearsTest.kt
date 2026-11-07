package io.github.dimitrysaf.provenio.core.search

import io.github.dimitrysaf.provenio.core.home.MetaPreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReleaseYearsTest {

    private fun title(releaseInfo: String? = null, rawReleaseDate: String? = null) =
        MetaPreview(id = "tt1", type = "series", name = "Show", releaseInfo = releaseInfo, rawReleaseDate = rawReleaseDate)

    @Test
    fun aFinishedSeriesCoversEveryYearItRan() {
        val years = title("2002–2010").releaseYears(currentYear = 2026)!!
        assertTrue(2006 in years)
        assertEquals(2002..2010, years)
    }

    @Test
    fun aSeriesStillAiringRunsToThisYear() {
        assertEquals(2019..2026, title("2019–").releaseYears(currentYear = 2026))
        assertEquals(2019..2026, title("2019 - ").releaseYears(currentYear = 2026))
    }

    @Test
    fun aMovieIsItsOneYear() {
        assertEquals(2006..2006, title("2006").releaseYears(currentYear = 2026))
        assertEquals(1999..1999, title(rawReleaseDate = "1999-03-31").releaseYears(currentYear = 2026))
    }

    @Test
    fun noYearMeansNoRange() {
        assertNull(title().releaseYears(currentYear = 2026))
    }
}
