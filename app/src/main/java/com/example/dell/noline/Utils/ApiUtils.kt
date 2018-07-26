package com.example.dell.noline.Utils

import com.example.dell.noline.Interfaces.TransactionInterface
import com.example.dell.noline.Retrofit.RetrofitClient

object ApiUtils {
    //val BASE_URL = "http://192.168.1.94:8000"
    const val BASE_URL = "http://192.168.1.5:8000"
    const val BASE_URL_WS = "192.168.1.5:8000"
    val apiTransaction: TransactionInterface
        get() = RetrofitClient.getClient(BASE_URL)!!.create(TransactionInterface::class.java)
}