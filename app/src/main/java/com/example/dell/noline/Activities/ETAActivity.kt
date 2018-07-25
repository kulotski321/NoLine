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
import kotlinx.android.synthetic.main.activity_eta.*
import java.text.DateFormat
import java.util.*
import com.example.dell.noline.Utils.ApiUtils
import okio.ByteString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.jetbrains.anko.alert
import org.jetbrains.anko.longToast
import retrofit2.Call
import retrofit2.Callback


class ETAActivity: AppCompatActivity() {
    private var dateFormat = DateFormat.getDateTimeInstance()
    private var dateTime = Calendar.getInstance()!!
    lateinit var mServiceId: String
    lateinit var mAuthToken: String
    lateinit var client: OkHttpClient
    private var transactionInterface: TransactionInterface = ApiUtils.apiTransaction

    public override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eta)
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
        val authToken = "uuid%20$uuid"

        // for websocket uri
        mServiceId = serviceId
        mAuthToken = authToken

        convertDateTimeAndDisplay(waitingTime)

        priorityTV.text = priorityNumber
        currentTV.text = currentServed
        companyTV.text = companyName
        serviceTV.text = serviceName

        // Log.e("date", date)
        // Log.e("time", time)
        Log.e("waitingTime", waitingTime)
        Log.e("PN", priorityNumber)
        Log.e("CS", currentServed)
        Log.e("SI", serviceId)
        Log.e("serviceName", serviceName)
        Log.e("companyName", companyName)

        start()
        cancel_btn.setOnClickListener {
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
        pause_btn.setOnClickListener {
            runOnUiThread {
                alert ("This will temporarily stop your current queue but you can choose to " +
                        "queue back and get another ETA"){
                    title = "Pause Queue"
                    positiveButton("Proceed"){
                        reserveTransaction(uuid)
                    }
                    negativeButton("Cancel"){}
                }.show()
            }
        }
        line_up_btn.setOnClickListener {
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
            dateTV.text = date
            timeTV.text = time
        }


    }
    private fun changeCurrentServe(currentServed: String){
        runOnUiThread {
            currentTV.text = currentServed
        }
    }

    private inner class EchoWebSocketListener : WebSocketListener() {
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
                    changeCurrentServe(result.currentServed)
                }
                result.message == "user turn" -> {
                    runOnUiThread {
                        alert("Please present this queue ticket to teller " +
                                result.teller){
                            title = "It's your turn"
                            positiveButton("Okay"){}
                        }.show()
                    }
                }
                result.message == "user skip" -> {
                    runOnUiThread {
                        alert("Get another ticket huhuhu :("){
                            title = "Sorry, you have been skipped"
                            positiveButton("Okay"){
                                val j = Intent(this@ETAActivity, MainActivity::class.java)
                                startActivity(j)
                                webSocket.close(NORMAL_CLOSURE_STATUS, null)
                                finish()
                            }
                        }.show()
                    }
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
        val listener = EchoWebSocketListener()
        val ws = client.newWebSocket(request, listener)
        client.dispatcher().executorService().shutdown()
    }

    private fun cancelTransaction(uuid: String){
        transactionInterface.cancelTransaction(uuid).enqueue(object: Callback<Message>{
            override fun onFailure(call: Call<Message>?, t: Throwable?) {
            }

            override fun onResponse(call: Call<Message>?, response: retrofit2.Response<Message>?) {
                if(response!!.isSuccessful){
                    val result = response.body()
                    if(result.message == "successfully cancelled"){
                        val i = Intent(this@ETAActivity, MainActivity::class.java)
                        startActivity(i)
                    }
                }
            }
        })
    }

    private fun reserveTransaction(uuid: String){
        transactionInterface.reserveTransaction(uuid).enqueue(object: Callback<ResultQR>{
            override fun onFailure(call: Call<ResultQR>?, t: Throwable?) {
            }

            override fun onResponse(call: Call<ResultQR>?, response: retrofit2.Response<ResultQR>?) {
                if(response!!.isSuccessful){
                    val result = response.body()
                    if(result.message == "successfully reserved"){
                        longToast("you are now reserved")
                    }
                }
            }
        })
    }

    private fun joinQueue(uuid: String){
        transactionInterface.joinQueue(uuid).enqueue(object: Callback<ResultQR>{
            override fun onFailure(call: Call<ResultQR>?, t: Throwable?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun onResponse(call: Call<ResultQR>?, response: retrofit2.Response<ResultQR>?) {
                if(response!!.isSuccessful){
                    val result = response.body()
                    Log.e("result",result.toString())
                    if(result.message == "successfully lined up"){
                        longToast("Successfully lined up")
                        runOnUiThread {
                            changeCurrentServe(result.currentServed)
                            convertDateTimeAndDisplay(result.waitingTime)
                            priorityTV.text = result.priorityNumber
                        }
                    }else if(result.message == "no available tellers"){
                        longToast("No available tellers")
                    }
                }
            }
        })
    }
}