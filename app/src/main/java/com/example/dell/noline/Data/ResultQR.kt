package com.example.dell.noline.Data

import com.google.gson.annotations.SerializedName

data class ResultQR (
        @SerializedName("message")
        val message: String,
        @SerializedName("uuid")
        val uuid: String,
        @SerializedName("time_joined")
        val timeJoined: String,
        @SerializedName("waiting_time")
        val waitingTime: String,
        @SerializedName("priority_number")
        val priorityNumber: String,
        @SerializedName("current_served")
        val currentServed: String,
        @SerializedName("service_id")
        val serviceId: String,
        @SerializedName("service_name")
        val serviceName: String,
        @SerializedName("company_name")
        val companyName: String,
        @SerializedName("teller_no")
        val teller: String
)