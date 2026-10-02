package dev.fitiavana.learning_mgmt.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ThemeTest {
    @get:Rule
    val compose = createComposeRule()

    private fun contrast(a: Color, b: Color): Float {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05f) / (dark + 0.05f)
    }

    private fun readable(scheme: ColorScheme) = mapOf(
        "onPrimary on primary" to (scheme.onPrimary to scheme.primary),
        "onPrimaryContainer on primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
        "onSurface on surface" to (scheme.onSurface to scheme.surface),
        "onSurface on background" to (scheme.onBackground to scheme.background),
        "onSurfaceVariant on surface" to (scheme.onSurfaceVariant to scheme.surface),
        "onSurfaceVariant on surfaceVariant" to (scheme.onSurfaceVariant to scheme.surfaceVariant),
        "primary on surface" to (scheme.primary to scheme.surface),
        "error on surface" to (scheme.error to scheme.surface),
        "onError on error" to (scheme.onError to scheme.error),
    )

    private fun assertReadable(scheme: ColorScheme) {
        readable(scheme).forEach { (pair, colors) ->
            val ratio = contrast(colors.first, colors.second)
            assertTrue("$pair has contrast $ratio, expected at least 4.5", ratio >= 4.5f)
        }
    }

    @Test
    fun lightSchemeUsesTheIndigoAccent() = assertEquals(Color(0xFF3F51B5), LightColorScheme.primary)

    @Test
    fun darkSchemeUsesALighterIndigoAccent() {
        assertEquals(Color(0xFF9FA8DA), DarkColorScheme.primary)
        assertTrue(DarkColorScheme.primary.luminance() > LightColorScheme.primary.luminance())
    }

    @Test
    fun lightSchemeIsReadable() = assertReadable(LightColorScheme)

    @Test
    fun darkSchemeIsReadable() = assertReadable(DarkColorScheme)

    @Test
    fun darkSurfaceIsDarkAndLightSurfaceIsLight() {
        assertTrue(DarkColorScheme.surface.luminance() < 0.1f)
        assertTrue(LightColorScheme.surface.luminance() > 0.9f)
    }

    @Test
    fun errorRedIsDistinctFromTheAccent() {
        assertTrue(LightColorScheme.error != LightColorScheme.primary)
        assertTrue(DarkColorScheme.error != DarkColorScheme.primary)
    }

    @Test
    fun theThemeFollowsTheSystemLightMode() {
        var primary = Color.Unspecified
        compose.setContent { LearningmgmtTheme { primary = MaterialTheme.colorScheme.primary } }

        assertEquals(LightColorScheme.primary, primary)
    }

    @Test
    @Config(qualifiers = "night")
    fun theThemeFollowsTheSystemDarkMode() {
        var primary = Color.Unspecified
        compose.setContent { LearningmgmtTheme { primary = MaterialTheme.colorScheme.primary } }

        assertEquals(DarkColorScheme.primary, primary)
    }
}
