package com.example.dell.noline.Data

import com.beust.klaxon.Json

data class NewETA(
        @Json(name = "new_eta")
        val newETA: String,
        @Json(name = "current_served")
        val currentServed: String,
        @Json(name = "message")
        val message: String,
        @Json(name ="teller")
        val teller: String
)