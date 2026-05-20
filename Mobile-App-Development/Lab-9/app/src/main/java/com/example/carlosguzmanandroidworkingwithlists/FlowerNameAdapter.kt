package com.example.carlosguzmanandroidworkingwithlists

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.content.ContextCompat
import com.example.carlosguzmanandroidworkingwithlists.databinding.FlowerListItem1Binding

class FlowerNameAdapter(
    private val context: Context,
    private val flowerNames: List<String>,
    private val flowerImageResIds: List<Int>
) : BaseAdapter() {

    // Alternating card background colors for a softer, more elegant look
    private val cardColors = intArrayOf(
        ContextCompat.getColor(context, R.color.list_row_even),
        ContextCompat.getColor(context, R.color.list_row_odd)
    )

    override fun getCount(): Int = flowerNames.size

    override fun getItem(position: Int): Any = flowerNames[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: FlowerListItem1Binding
        val itemView = if (convertView == null) {
            binding = FlowerListItem1Binding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
            binding.root
        } else {
            binding = convertView.tag as FlowerListItem1Binding
            convertView
        }

        binding.flowerName.text = flowerNames[position]
        binding.flowerImage.setImageResource(flowerImageResIds[position])

        // Apply alternating card background color
        val card = binding.root as com.google.android.material.card.MaterialCardView
        card.setCardBackgroundColor(cardColors[position % 2])

        return itemView
    }
}
