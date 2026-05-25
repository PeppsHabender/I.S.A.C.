package org.inquest.discord.isac.embed

import org.inquest.analysis.model.PlayerAnalysis
import org.inquest.analysis.model.PlayerPull
import org.inquest.analysis.model.RunAnalysis
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class TopStatsCalculatorTest {
    @Test
    fun `calculate excludes top stats tied at zero`() {
        val analysis = runAnalysis(
            player("Alice", pull(cc = 0)),
            player("Bob", pull(cc = 0)),
        )

        val lines = TopStatsCalculator.calculate(analysis, withHeal = false)

        assertFalse(lines.any { it.title == "Cc            >>" })
    }

    @Test
    fun `calculate excludes second best stats when second best value is zero`() {
        val analysis = runAnalysis(
            player("Alice", pull(cc = 10)),
            player("Bob", pull(cc = 0)),
            player("Carol", pull(cc = 0)),
        )

        val lines = TopStatsCalculator.calculate(analysis, withHeal = false, idx = 1)

        assertFalse(lines.any { it.title == "Cc            >>" })
    }

    @Test
    fun `calculate keeps tied stats when tied value is positive`() {
        val analysis = runAnalysis(
            player("Alice", pull(cc = 10)),
            player("Bob", pull(cc = 10)),
            player("Carol", pull(cc = 5)),
        )

        val lines = TopStatsCalculator.calculate(analysis, withHeal = false)
            .filter { it.title == "Cc            >>" }

        assertEquals(listOf("Alice", "Bob"), lines.map { it.player })
    }

    private fun runAnalysis(vararg players: PlayerAnalysis) = RunAnalysis(
        start = OffsetDateTime.now(),
        end = OffsetDateTime.now(),
        downtimeMillis = 0,
        durationMillis = 0,
        pulls = emptyList(),
        groupDps = 0,
        playerStats = players.toList(),
    )

    private fun player(name: String, pull: PlayerPull) = PlayerAnalysis(
        name,
        mutableMapOf("pull" to pull),
    )

    private fun pull(cc: Int) = PlayerPull().copy(
        cc = cc,
        dps = 1,
        profession = PlayerPull().profession.copy(name = "Test"),
    )
}
