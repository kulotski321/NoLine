package com.example.dell.noline.Activities
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AppCompatActivity
import com.example.dell.noline.R

class SplashActivity : AppCompatActivity(){
    private var delayHandler: Handler? = null
    private val delay: Long = 3000 //2.5 seconds

    private val runnable: Runnable = Runnable {
        if (!isFinishing) {
            val intent = Intent(applicationContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Initialize the Handler
        delayHandler = Handler()
        // Navigate with delay
        delayHandler!!.postDelayed(runnable, delay)
    }

    public override fun onDestroy() {
        if (delayHandler != null) {
            delayHandler!!.removeCallbacks(runnable)
        }
        super.onDestroy()
    }
}