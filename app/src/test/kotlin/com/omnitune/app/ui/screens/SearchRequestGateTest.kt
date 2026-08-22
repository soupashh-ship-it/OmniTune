package com.omnitune.app.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchRequestGateTest {

    @Test
    fun lateResponseForPreviousQueryIsRejected() {
        val gate = SearchRequestGate()
        val first = gate.begin("first", SearchFilterTab.All)
        val second = gate.begin("second", SearchFilterTab.All)

        assertFalse(gate.accepts(first))
        assertTrue(gate.accepts(second))
    }

    @Test
    fun filterChangeRejectsResponseForPriorFilter() {
        val gate = SearchRequestGate()
        val all = gate.begin("runtime", SearchFilterTab.All)
        val songs = gate.begin("runtime", SearchFilterTab.Songs)

        assertFalse(gate.accepts(all))
        assertTrue(gate.accepts(songs))
    }

    @Test
    fun blankQueryInvalidatesOutstandingWorkAndPaginationLookup() {
        val gate = SearchRequestGate()
        val request = gate.begin("runtime", SearchFilterTab.Songs)

        gate.invalidate()

        assertFalse(gate.accepts(request))
        assertNull(gate.currentFor("runtime", SearchFilterTab.Songs))
    }

    @Test
    fun rapidTypingPublishesOnlyTheLatestGeneration() {
        val gate = SearchRequestGate()
        val requests = listOf("r", "ru", "run", "runt", "runtime")
            .map { gate.begin(it, SearchFilterTab.All) }

        assertTrue(gate.accepts(requests.last()))
        assertTrue(requests.dropLast(1).none(gate::accepts))
    }
}
