package com.example.dell.noline.Interfaces

import com.example.dell.noline.Activities.ETAActivity
import com.example.dell.noline.Data.Message
import com.example.dell.noline.Data.ResultQR
import retrofit2.Call
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface TransactionInterface {


    @FormUrlEncoded
    @POST("/transaction/authenticate/")
    fun authenticateTransaction(@Field("uuid") uuid: String,
                                @Field("mac") deviceCode: String) : Call<ResultQR>

    @FormUrlEncoded
    @POST("/transaction/cancel/")
    fun cancelTransaction(@Field("uuid") uuid: String) : Call<Message>

    @FormUrlEncoded
    @POST("/transaction/reserve/")
    fun reserveTransaction(@Field("uuid") uuid: String) : Call<ResultQR>

    @FormUrlEncoded
    @POST("/transaction/joinreservedqueue/")
    fun joinQueue(@Field("uuid") uuid: String) : Call<ResultQR>
}