package com.akustom15.mint.dashboard

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge moderno sin colorear barras (compat Google Play Android 15)
        val lightScrim = android.graphics.Color.TRANSPARENT
        val darkScrim = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(darkScrim),
            navigationBarStyle = SystemBarStyle.dark(darkScrim)
        )

        setContentView(R.layout.activity_splash)

        val appNamePart1 = findViewById<TextView>(R.id.appNamePart1)
        val appNamePart2 = findViewById<TextView>(R.id.appNamePart2)
        val appNamePart3 = findViewById<TextView>(R.id.appNamePart3)

        val anim1 = AnimationUtils.loadAnimation(this, R.anim.mint_slide_up_1)
        val anim2 = AnimationUtils.loadAnimation(this, R.anim.mint_slide_up_2)
        val anim3 = AnimationUtils.loadAnimation(this, R.anim.mint_slide_up_3)

        // Cuando la última animación termine, navegamos a MainActivity
        anim3.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationRepeat(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                // Pequeña pausa antes de entrar
                appNamePart1.postDelayed({
                    startMainActivity()
                }, 300)
            }
        })

        appNamePart1.startAnimation(anim1)
        appNamePart2.startAnimation(anim2)
        appNamePart3.startAnimation(anim3)
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
        startActivity(intent)
        finish()
    }
}
