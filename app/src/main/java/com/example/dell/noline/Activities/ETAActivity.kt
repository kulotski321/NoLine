package com.example.dell.noline.Activities

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.beust.klaxon.Klaxon
import com.example.dell.noline.Data.Message
import com.example.dell.noline.Data.NewETA
import com.example.dell.noline.Data.ResultQR
import com.example.dell.noline.Interfaces.TransactionInterface
import com.example.dell.noline.R
import java.text.DateFormat
import java.util.*
import com.example.dell.noline.Utils.ApiUtils
import com.example.dell.noline.Utils.Device
import kotlinx.android.synthetic.main.activity_eta_new.*
import okio.ByteString
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.jetbrains.anko.alert
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback


class ETAActivity: AppCompatActivity() {
    private var dateFormat = DateFormat.getDateTimeInstance()
    private var dateTime = Calendar.getInstance()!!
    lateinit var mServiceId: String
    lateinit var mAuthToken: String
    lateinit var client: OkHttpClient
    lateinit var listener : EchoWebSocketListener
    lateinit var ws: WebSocket
    private var transactionInterface: TransactionInterface = ApiUtils.apiTransaction
    private var doubleBackToExitPressedOnce = false

    public override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eta_new)
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
        if(!intent.getStringExtra("teller_no").isNullOrBlank()){
            val teller = intent.getStringExtra("teller_no")
            runOnUiThread {
                alert("Please present this queue ticket to teller " +
                        teller){
                    title = "It's your turn"
                    positiveButton("Okay"){}
                }.show()
            }
            cancel_btn.visibility = View.INVISIBLE
            pause_btn.visibility = View.INVISIBLE
            back_btn.visibility = View.VISIBLE
        }
        val authToken = "uuid%20$uuid"

        // for websocket uri
        mServiceId = serviceId
        mAuthToken = authToken

        convertDateTimeAndDisplay(waitingTime)

        priorityTV.text = priorityNumber
        currentTV.text = currentServed
        companyTV.text = companyName
        serviceTV.text = serviceName

        start()
        cancel_btn.setOnClickListener {
            runOnUiThread {
                alert ("This will stop your current queue"){
                    title = "Cancel Queue"
                    positiveButton("Proceed"){
                        listener.onClosing(ws, NORMAL_CLOSURE_STATUS, "nothing")
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
                        listener.onClosing(ws, NORMAL_CLOSURE_STATUS, "nothing")
                        reserveTransaction(uuid)
                    }
                    negativeButton("Cancel"){}
                }.show()
            }
        }
        back_btn.setOnClickListener {
            listener.onClosing(ws, NORMAL_CLOSURE_STATUS, "nothing")
            val i = Intent(this@ETAActivity, MainActivity::class.java)
            startActivity(i)
            finish()
        }

    }
    private fun convertDateTimeAndDisplay(waitingTime: String){
        val year: String = waitingTime.substring(0, 4)
        val month: String = waitingTime.substring(5, 7)
        val day: String = waitingTime.substring(8, 10)
        val hour: String = waitingTime.substring(11, 13)
        val minute: String = waitingTime.substring(14, 16)
        val second: String = waitingTime.substring(17,19)
        Log.e("year",year)
        Log.e("month", month)
        Log.e("day", day)
        Log.e("hour", hour)
        Log.e("minute", minute)
        Log.e("second", second)

        dateTime.set(Calendar.YEAR, year.toInt())
        dateTime.set(Calendar.MONTH, month.toInt() - 1)
        dateTime.set(Calendar.DAY_OF_MONTH, day.toInt())
        dateTime.set(Calendar.HOUR_OF_DAY, hour.toInt())
        dateTime.set(Calendar.MINUTE, minute.toInt())
        dateTime.set(Calendar.SECOND, second.toInt())

        val dateTimeFinal = dateFormat.format(dateTime.time)
        Log.e("dateTime", dateTimeFinal)
        val date = dateTimeFinal.substring(0,12)
        var time = dateTimeFinal.substring(13,18)
        // "2018-04-03T06:03:54"
        Log.e("date", date)
        Log.e("time", time)
        val num = hour.toInt()
        if(num in 0..12){
            time = dateTimeFinal.substring(13,17)
        }
        time += if(num in 0..12){
            " AM"
        }else{
            " PM"
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

    inner class EchoWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            // webSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye !")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val result = Klaxon()
                    .parse<NewETA>(text)
            Log.e("result", result.toString())
            when {
                result?.message == "successfully connected" -> {

                }
                result?.message == "eta change" -> {
                    convertDateTimeAndDisplay(result.newETA)
                    changeCurrentServe(result.currentServed)
                }
                result?.message == "user turn" -> {
                    runOnUiThread {
                        alert("Please present this queue ticket to teller " +
                                result.teller){
                            title = "It's your turn"
                            positiveButton("Okay"){}
                        }.show()
                        cancel_btn.visibility = View.INVISIBLE
                        pause_btn.visibility = View.INVISIBLE
                        back_btn.visibility = View.VISIBLE
                    }
                }
                result?.message == "user skip" -> {
                    runOnUiThread {
                        alert("Your ticket have been skipped due to your absence"){
                            title = "Sorry, you have been skipped"
                            positiveButton("Okay"){
                                listener.onClosing(ws, NORMAL_CLOSURE_STATUS, "nothing")
                                val j = Intent(this@ETAActivity, MainActivity::class.java)
                                startActivity(j)
                                finish()
                            }
                        }.show().setOnDismissListener {
                            listener.onClosing(ws, NORMAL_CLOSURE_STATUS, "nothing")
                            val j = Intent(this@ETAActivity, MainActivity::class.java)
                            startActivity(j)
                            finish()
                        }
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
        const val NORMAL_CLOSURE_STATUS = 1000
    }

    private fun start() {
        val request = Request.Builder().url("ws://"+ApiUtils.BASE_URL_WS+"/ws/customer/"+
                mServiceId+"/0"+"/?uuid="+mAuthToken).build()
        listener = EchoWebSocketListener()
        ws = client.newWebSocket(request, listener)
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
                        finish()
                    }
                }
            }
        })
    }

    private fun reserveTransaction(uuid: String){
        transactionInterface.reserveTransaction(uuid).enqueue(object: Callback<ResultQR>{
            override fun onFailure(call: Call<ResultQR>?, t: Throwable?) {
                longToast("Check your internet connection")
            }

            override fun onResponse(call: Call<ResultQR>?, response: retrofit2.Response<ResultQR>?) {
                if(response!!.isSuccessful){
                    val result = response.body()
                    if(result.message == "successfully reserved"){
                        longToast("You are now in reserved queue")
                        authenticate(uuid, Device.code)
                    }
                }
            }
        })
    }
    private fun authenticate(uuid: String, mac: String){
        transactionInterface.authenticateTransaction(uuid, mac).enqueue(object: Callback<ResultQR> {
            override fun onFailure(call: Call<ResultQR>?, t: Throwable?) {
                longToast("Check your internet connection")
            }

            override fun onResponse(call: Call<ResultQR>?, response: retrofit2.Response<ResultQR>?) {
                if(response!!.isSuccessful){
                    val result = response.body()
                    when {
                        result.message == "not a valid customer" -> {
                            longToast("Please scan a valid QR code")
                        }
                        result.message == "it is your turn" -> {
                            longToast("It is your turn")
                            cancel_btn.visibility = View.INVISIBLE
                            pause_btn.visibility = View.INVISIBLE
                            back_btn.visibility = View.VISIBLE
                        }
                        result.message == "you have been skipped" -> {
                            longToast("You have been skipped")
                        }
                        result.message == "your transaction is already complete" -> {
                            longToast("Your transaction is already complete")
                        }
                        result.message == "you are in reserved" -> {
                            // longToast("You are now in reserved queue")
                            // go to reserve mode
                            val i = Intent(this@ETAActivity, ReserveActivity::class.java)
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
                        }
                        result.message == "no available tellers" -> {
                            longToast("No available tellers")
                        }
                        result.message == "successfully logged in" -> {
                        }
                        result.message == "not your device" -> {
                            longToast("This QR code is linked to another device")
                        }
                    }
                }
            }

        })
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            this.moveTaskToBack(true);
            return
        }

        this.doubleBackToExitPressedOnce = true
        toast("Please press BACK again to exit")
        Handler().postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }
}