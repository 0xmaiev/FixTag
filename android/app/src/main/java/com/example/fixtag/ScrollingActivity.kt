package com.example.fixtag

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.fixtag.databinding.ActivityIncidentsBinding

class ScrollingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncidentsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIncidentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = title
    }
}