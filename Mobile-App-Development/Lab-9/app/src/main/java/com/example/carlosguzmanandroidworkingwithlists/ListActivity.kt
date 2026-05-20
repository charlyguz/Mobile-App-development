package com.example.carlosguzmanandroidworkingwithlists

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.carlosguzmanandroidworkingwithlists.databinding.ActivityListBinding

class ListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListBinding

    private val flowerNames = mutableListOf<String>()
    private val flowerImageResIds = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initFlowerNames()
        initFlowerImages()

        val adapter = FlowerNameAdapter(this, flowerNames, flowerImageResIds)
        binding.flowerList.adapter = adapter
    }

    private fun initFlowerNames() {
        flowerNames.addAll(
            listOf(
                "Rose",
                "Tulip",
                "Daisy",
                "Sunflower",
                "Peony",
                "Orchid"
            )
        )
    }

    private fun initFlowerImages() {
        flowerImageResIds.addAll(
            listOf(
                R.drawable.rose,
                R.drawable.tulip,
                R.drawable.daisy,
                R.drawable.rose,
                R.drawable.tulip,
                R.drawable.daisy
            )
        )
    }
}
