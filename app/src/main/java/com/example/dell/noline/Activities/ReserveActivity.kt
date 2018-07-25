package com.example.dell.noline.Activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.beust.klaxon.Klaxon
import com.example.dell.noline.Data.Message
import com.example.dell.noline.Data.NewETA
import com.example.dell.noline.Data.ResultQR
import com.example.dell.noline.Interfaces.TransactionInterface
import com.example.dell.noline.R
import com.example.dell.noline.Utils.ApiUtils
import kotlinx.android.synthetic.main.activity_eta.*
import kotlinx.android.synthetic.main.activity_reserve.*
import okhttp3.*
import okio.ByteString
import org.jetbrains.anko.longToast
import retrofit2.Call
import retrofit2.Callback
import java.text.DateFormat
import java.util.*
import org.jetbrains.anko.alert
import org.jetbrains.anko.longToast

class ReserveActivity: AppCompatActivity (){
    private var dateFormat = DateFormat.getDateTimeInstance()
    private var dateTime = Calendar.getInstance()!!
    private var transactionInterface: TransactionInterface = ApiUtils.apiTransaction
    lateinit var mServiceId: String
    lateinit var mAuthToken: String
    lateinit var client: OkHttpClient
    lateinit var listener : EchoWebSocketListener
    lateinit var ws: WebSocket
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reserve)
        client = OkHttpClient()
        val message = intent.getStringExtra("message")
        val timeJoined = intent.getStringExtra("timeJoined")
        val waitingTime = intent.getStringExtra("waitingTime")
        val uuid = intent.getStringExtra("uuid")
        val priorityNumber = intent.getStringExtra("priorityNumber")
        val currentServed = intent.getStringExtra("currentServed")
        val serviceId = intent.getStringExtra("serviceId")
        val serviceName = intent.getStringExtra("serviceName")
        val companyName = intent.getStringExtra("companyName")

        convertDateTimeAndDisplay(waitingTime)

        companyTV1.text = companyName
        serviceTV1.text = serviceName

        val authToken = "uuid%20$uuid"

        // for websocket uri
        mServiceId = serviceId
        mAuthToken = authToken

        start()

        line_up_btn1.setOnClickListener {
            runOnUiThread {
                alert ("This will add you in the queue"){
                    title = "Queue again?"
                    positiveButton("Proceed"){
                         joinQueue(uuid)
                    }
                    negativeButton("Cancel"){}
                }.show()
            }
        }

        cancel_btn1.setOnClickListener {
            runOnUiThread {
                alert ("This will stop your current queue"){
                    title = "Cancel Queue"
                    positiveButton("Proceed"){
                        cancelTransaction(uuid)
                    }
                    negativeButton("Cancel"){}
                }.show()
            }
        }
    }
    private fun joinQueue(uuid: String){
        transactionInterface.joinQueue(uuid).enqueue(object: Callback<ResultQR> {
            override fun onFailure(call: Call<ResultQR>?, t: Throwable?) {
                longToast("Check your internet connection")
            }

            override fun onResponse(call: Call<ResultQR>?, response: retrofit2.Response<ResultQR>?) {
                if(response!!.isSuccessful){
                    val result = response.body()
                    Log.e("result",result.toString())
                    if(result.message == "successfully lined up"){
                        listener.onClosing(ws, ETAActivity.NORMAL_CLOSURE_STATUS, "nothing")
                        longToast("Successfully lined up")
                        val i = Intent(this@ReserveActivity, ETAActivity::class.java)
                        i.putExtra("message", result.message)
                        i.putExtra("uuid", result.uuid)
                        i.putExtra("timeJoined", result.timeJoined)
                        i.putExtra("waitingTime", result.waitingTime)
                        i.putExtra("priorityNumber", result.priorityNumber)
                        i.putExtra("currentServed", result.currentServed)
                        i.putExtra("serviceId", result.serviceId)
                        i.putExtra("serviceName", result.serviceName)
                        i.putExtra("companyName", result.companyName)
                        startActivity(i)
                        finish()
                    }else if(result.message == "no available tellers"){
                        longToast("No available tellers")
                    }
                }
            }
        })
    }

    private fun cancelTransaction(uuid: String){
        transactionInterface.cancelTransaction(uuid).enqueue(object: Callback<Message>{
            override fun onFailure(call: Call<Message>?, t: Throwable?) {
            }

            override fun onResponse(call: Call<Message>?, response: retrofit2.Response<Message>?) {
                if(response!!.isSuccessful){
                    val result = response.body()
                    if(result.message == "successfully cancelled"){
                        val i = Intent(this@ReserveActivity, MainActivity::class.java)
                        listener.onClosing(ws, ETAActivity.NORMAL_CLOSURE_STATUS, "nothing")
                        startActivity(i)
                        finish()
                    }
                }
            }
        })
    }

    private fun convertDateTimeAndDisplay(waitingTime: String){
        val year: String = waitingTime.substring(0, 4)
        val month: String = waitingTime.substring(5, 7)
        val day: String = waitingTime.substring(8, 10)
        val hour: String = waitingTime.substring(11, 13)
        val minute: String = waitingTime.substring(14, 16)
        val second: String = waitingTime.substring(17,19)

        dateTime.set(Calendar.YEAR, year.toInt())
        dateTime.set(Calendar.MONTH, month.toInt() - 1)
        dateTime.set(Calendar.DAY_OF_MONTH, day.toInt())
        dateTime.set(Calendar.HOUR_OF_DAY, hour.toInt())
        dateTime.set(Calendar.MINUTE, minute.toInt())
        dateTime.set(Calendar.SECOND, second.toInt())

        val dateTimeFinal = dateFormat.format(dateTime.time)
        val date = dateTimeFinal.substring(0,12)
        var time = dateTimeFinal.substring(13,21)

        val num = hour.toInt()
        time += if(num in 0..12){
            "AM"
        }else{
            "PM"
        }
        // Display into screen
        runOnUiThread {
            dateTV1.text = date
            timeTV1.text = time
        }
    }


    inner class EchoWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            // webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val result = Klaxon()
                    .parse<NewETA>(text)
            Log.e("result", result.toString())
            when {
                result!!.message == "successfully connected" -> {

                }
                result.message == "eta change" -> {
                    convertDateTimeAndDisplay(result.newETA)
                }
                result.message == "user turn" -> {

                }
                result.message == "user skip" -> {

                }
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            Log.e("bytes", bytes.toString())
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null)
            Log.e("Closed", reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response) {
            Log.e("Failure", t.message)
        }

    }
    companion object {
        private const val NORMAL_CLOSURE_STATUS = 1000
    }

    private fun start() {
        val request = Request.Builder().url("ws://"+ApiUtils.BASE_URL_WS+"/ws/customer/"+
                mServiceId+"/0"+"/?uuid="+mAuthToken).build()
        listener = EchoWebSocketListener()
        ws = client.newWebSocket(request, listener)
        client.dispatcher().executorService().shutdown()
    }
}