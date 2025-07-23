package com.example.digi_diary

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Developer : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_developer)

        // Set up toolbar with back button
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            // Optional: Set a back icon if you want a custom one
            // setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        }

        // Set up bottom navigation
        setupBottomNavigation()

        // Set up email click
        findViewById<View>(R.id.emailContainer).setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("developer@example.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Regarding DigiDiary App")
            }
            if (emailIntent.resolveActivity(packageManager) != null) {
                startActivity(emailIntent)
            }
        }

        // Set up phone click
        findViewById<View>(R.id.phoneContainer).setOnClickListener {
            val phoneIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:+1234567890")
            }
            startActivity(phoneIntent)
        }
    }

    private fun setupBottomNavigation() {
        // Set up home button
        findViewById<ImageView>(R.id.nav_home).setOnClickListener {
            finish() // Go back to home
        }

        // Set up profile button
        findViewById<ImageView>(R.id.nav_profile).setOnClickListener {
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
            finish()
        }

        // FAB button has been removed as per user request
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    override fun onBackPressed() {
        super.onBackPressed()
        // Optional: Add a nice transition animation
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}