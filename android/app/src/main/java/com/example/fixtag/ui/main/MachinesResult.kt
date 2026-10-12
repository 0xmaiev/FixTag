package com.example.fixtag.ui.main

import com.example.fixtag.data.model.Machine

/**
 * Machines result : success (machine details) or error message.
 */
data class MachinesResult(
    val success: List<Machine>? = null,
    val error: Int? = null
)