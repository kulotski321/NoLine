package com.example.dell.noline.Data

import com.google.gson.annotations.SerializedName

data class Message (
        @SerializedName("message")
        val message: String
)