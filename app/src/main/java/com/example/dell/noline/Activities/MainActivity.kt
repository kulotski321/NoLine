package com.example.dell.noline.Activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.widget.Button
import android.widget.TextView
import com.example.dell.noline.R
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.util.Log
import java.util.ArrayList
import java.util.HashMap
import android.os.Handler
import kotlinx.android.synthetic.main.activity_main.*
import com.example.dell.noline.Data.ResultQR
import com.example.dell.noline.Interfaces.TransactionInterface
import com.example.dell.noline.Utils.ApiUtils
import com.example.dell.noline.Utils.Device
import org.jetbrains.anko.longToast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    private var btn: Button? =null
    private val TAG = "tag"
    private var transactionInterface: TransactionInterface = ApiUtils.apiTransaction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // longToast(Device.code)
        authenticate("", Device.code)
        btn = findViewById<Button>(R.id.scan) as Button
        btn!!.setOnClickListener {
            if (checkAndRequestPermissions()) {
                // carry on the normal flow, as the case of  permissions  granted.
                Handler().postDelayed({
                    val intent = Intent(this@MainActivity, ScanActivity::class.java)
                    startActivity(intent)
                }, SPLASH_TIME_OUT.toLong())
            }
        }

        manual_btn!!.setOnClickListener {
            val i = Intent(this@MainActivity, ManualActivity::class.java)
            startActivity(i)
        }



    }
    private fun checkAndRequestPermissions(): Boolean {
        val camerapermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
        val writepermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permissionLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)


        val listPermissionsNeeded = ArrayList<String>()

        if (camerapermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.CAMERA)
        }
        if (writepermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissionLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), REQUEST_ID_MULTIPLE_PERMISSIONS)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        Log.d(TAG, "Permission callback called-------")
        when (requestCode) {
            REQUEST_ID_MULTIPLE_PERMISSIONS -> {

                val perms = HashMap<String, Int>()
                // Initialize the map with both permissions
                perms[Manifest.permission.CAMERA] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] = PackageManager.PERMISSION_GRANTED
                perms[Manifest.permission.ACCESS_FINE_LOCATION] = PackageManager.PERMISSION_GRANTED
                // Fill with actual results from user
                if (grantResults.isNotEmpty()) {
                    for (i in permissions.indices)
                        perms[permissions[i]] = grantResults[i]
                    if (perms[Manifest.permission.CAMERA] == PackageManager.PERMISSION_GRANTED
                            && perms[Manifest.permission.WRITE_EXTERNAL_STORAGE] == PackageManager.PERMISSION_GRANTED
                            && perms[Manifest.permission.ACCESS_FINE_LOCATION] == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "sms & location services permission granted")
                    } else {
                        Log.d(TAG, "Some permissions are not granted ask again ")
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                            showDialogOK("Service Permissions are required for this app",
                                    DialogInterface.OnClickListener { dialog, which ->
                                        when (which) {
                                            DialogInterface.BUTTON_POSITIVE -> checkAndRequestPermissions()
                                            DialogInterface.BUTTON_NEGATIVE ->
                                                // proceed with logic by disabling the related features or quit the app.
                                                finish()
                                        }
                                    })
                        } else {
                            explain("You need to give some mandatory permissions to continue. Do you want to go to app settings?")
                        }
                    }
                }
            }
        }

    }

    private fun showDialogOK(message: String, okListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", okListener)
                .create()
                .show()
    }

    private fun explain(msg: String) {
        val dialog = android.support.v7.app.AlertDialog.Builder(this)
        dialog.setMessage(msg)
                .setPositiveButton("Yes") { paramDialogInterface, paramInt ->
                    //  permissionsclass.requestPermission(type,code);
                    startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.example.parsaniahardik.kotlin_marshmallowpermission")))
                }
                .setNegativeButton("Cancel") { paramDialogInterface, paramInt -> finish() }
        dialog.show()
    }


    companion object {
        @SuppressLint("StaticFieldLeak")
        var qrResult: TextView?=null
        const val REQUEST_ID_MULTIPLE_PERMISSIONS = 1
        private const val SPLASH_TIME_OUT = 2000
    }

    private fun authenticate(uuid: String, mac: String){
        transactionInterface.authenticateTransaction(uuid, mac).enqueue(object: Callback<ResultQR> {
            override fun onFailure(call: Call<ResultQR>?, t: Throwable?) {
                longToast("Check your internet connection")
            }

            override fun onResponse(call: Call<ResultQR>?, response: Response<ResultQR>?) {
                if(response!!.isSuccessful){
                    val result = response.body()
                    when {
                        result.message == "not a valid customer" -> {
                            // longToast("Please scan a valid QR code")
                        }
                        result.message == "it is your turn" -> {
                            longToast("It is your turn")
                            val i = Intent(this@MainActivity, ETAActivity::class.java)
                            i.putExtra("message", result.message)
                            i.putExtra("uuid", result.uuid)
                            i.putExtra("timeJoined", result.timeJoined)
                            i.putExtra("waitingTime", result.waitingTime)
                            i.putExtra("priorityNumber", result.priorityNumber)
                            i.putExtra("currentServed", result.currentServed)
                            i.putExtra("serviceId", result.serviceId)
                            i.putExtra("serviceName", result.serviceName)
                            i.putExtra("companyName", result.companyName)
                            i.putExtra("teller_no", result.teller)
                            startActivity(i)
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
                            val i = Intent(this@MainActivity, ReserveActivity::class.java)
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
                            val i = Intent(this@MainActivity, ETAActivity::class.java)
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
                        result.message == "not your device" -> {
                            longToast("This QR code is linked to another device")
                        }
                    }
                }
            }

        })
    }
}
