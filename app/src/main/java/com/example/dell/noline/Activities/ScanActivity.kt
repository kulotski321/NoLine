package com.example.dell.noline.Activities

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.example.dell.noline.Data.ResultQR
import com.example.dell.noline.Interfaces.TransactionInterface
import com.example.dell.noline.Utils.ApiUtils
import com.google.zxing.Result
import me.dm7.barcodescanner.zxing.ZXingScannerView
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ScanActivity: AppCompatActivity(), ZXingScannerView.ResultHandler {
    private var mScannerView : ZXingScannerView ?= null
    private var transactionInterface: TransactionInterface = ApiUtils.apiTransaction

    public override fun onCreate(state: Bundle?){
        super.onCreate(state)
        mScannerView = ZXingScannerView(this)
        setContentView(mScannerView)
    }

    public override fun onResume(){
        super.onResume()
        mScannerView!!.setResultHandler(this)
        mScannerView!!.startCamera()
    }

    public override fun onPause(){
        super.onPause()
        mScannerView!!.stopCamera()
    }

    override fun handleResult(p0: Result?) {
        //MainActivity.qrResult!!.text = p0?.text
        authenticate(p0?.text.toString())
        onBackPressed()
    }

    private fun authenticate(uuid: String){
        transactionInterface.authenticateTransaction(uuid).enqueue(object: Callback<ResultQR> {
            override fun onFailure(call: Call<ResultQR>?, t: Throwable?) {
                longToast("Check your internet connection")
            }

            override fun onResponse(call: Call<ResultQR>?, response: Response<ResultQR>?) {
                if(response!!.isSuccessful){
                    val result = response.body()
                    when {
                        result.message == "not a valid customer" -> {
                            longToast("Please scan a valid QR code")
                        }
                        result.message == "it is your turn" -> {
                            longToast("It is your turn")
                        }
                        result.message == "you have been skipped" -> {
                            longToast("You have been skipped")
                        }
                        result.message == "your transaction is already complete" -> {
                            longToast("Your transaction is already complete")
                        }
                        result.message == "you are in reserved" -> {
                            longToast("You are in reserved")
                            // go to reserve mode
                            val i = Intent(this@ScanActivity, ReserveActivity::class.java)
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
                        }
                        result.message == "no available tellers" -> {
                            longToast("No available tellers")
                        }
                        result.message == "successfully logged in" -> {
                            val i = Intent(this@ScanActivity, ETAActivity::class.java)
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
                            Log.e(ContentValues.TAG,result.toString())
                            longToast("QR code scanned successfully")
                        }
                    }
                }
            }

        })
    }
}