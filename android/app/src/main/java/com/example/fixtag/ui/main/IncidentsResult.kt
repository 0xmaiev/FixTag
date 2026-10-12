package com.example.fixtag.ui.main

import com.example.fixtag.data.model.Incident

/**
 * Incidents result : success (incidents and their details) or error message.
 */
data class IncidentsResult(
    val success: List<Incident>? = null,
    val error: Int? = null
)