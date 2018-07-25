package com.example.dell.noline.Activities

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.example.dell.noline.Data.Message
import com.example.dell.noline.Data.ResultQR
import com.example.dell.noline.Interfaces.TransactionInterface
import com.example.dell.noline.R
import com.example.dell.noline.Utils.ApiUtils
import kotlinx.android.synthetic.main.activity_eta.*
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reserve)


        line_up_btn.setOnClickListener {
            runOnUiThread {
                alert ("This will add you in the queue"){
                    title = "Queue again?"
                    positiveButton("Proceed"){
                        // joinQueue(uuid)
                    }
                    negativeButton("Cancel"){}
                }.show()
            }
        }

        cancel_btn.setOnClickListener {
            runOnUiThread {
                alert ("This will stop your current queue"){
                    title = "Cancel Queue"
                    positiveButton("Proceed"){
                        // cancelTransaction(uuid)
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
                        longToast("Successfully lined up")
                        runOnUiThread {
                            changeCurrentServe(result.currentServed)
                            convertDateTimeAndDisplay(result.waitingTime)
                            // priorityTV.text = result.priorityNumber
                        }
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
                        startActivity(i)
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
            dateTV.text = date
            timeTV.text = time
        }


    }
    private fun changeCurrentServe(currentServed: String){
        runOnUiThread {
            currentTV.text = currentServed
        }
    }
}