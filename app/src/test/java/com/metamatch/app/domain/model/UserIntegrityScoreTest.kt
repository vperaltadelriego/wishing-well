package com.metamatch.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserIntegrityScoreTest {

    @Test
    fun `brand new user defaults to a neutral score`() {
        val score = UserIntegrityScore(userId = "new-user")
        assertEquals(4.0, score.score, 0.001)
        assertEquals(TrustLevel.GOOD, score.trustLevel)
    }

    @Test
    fun `perfect history reaches maximum score`() {
        var score = UserIntegrityScore(userId = "reliable-user")
        repeat(5) { score = score.recordFulfillment() }
        assertEquals(5.0, score.score, 0.001)
        assertEquals(TrustLevel.EXCELLENT, score.trustLevel)
    }

    @Test
    fun `mixed history produces a proportional score`() {
        // 3 fulfilled, 1 canceled -> 3/4 of 5.0 = 3.75
        var score = UserIntegrityScore(userId = "mixed-user")
        repeat(3) { score = score.recordFulfillment() }
        score = score.recordCancellation()
        assertEquals(3.75, score.score, 0.001)
    }
}
