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

    @Test
    fun `calculate ranks dps by average position including absences`() {
        val analysis = runAnalysis(
            player(
                "Alice",
                *List(13) { pull(dps = 20_000, dpsPos = 3) }.toTypedArray(),
            ),
            player(
                "Bob",
                *List(12) { PlayerPull() }.toTypedArray(),
                pull(dps = 50_000, dpsPos = 0),
            ),
        )

        val lines = TopStatsCalculator.calculate(analysis, withHeal = false)
            .filter { it.title == "Dps           >>" }

        assertEquals(listOf("Alice"), lines.map { it.player })
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

    private fun player(name: String, vararg pulls: PlayerPull) = PlayerAnalysis(
        name,
        pulls.mapIndexed { index, pull -> "pull-$index" to pull }.toMap(mutableMapOf()),
    )

    private fun pull(cc: Int = 0, dps: Int = 1, dpsPos: Int = 0) = PlayerPull().copy(
        cc = cc,
        dps = dps,
        dpsPos = dpsPos,
        profession = PlayerPull().profession.copy(name = "Test"),
    )
}
