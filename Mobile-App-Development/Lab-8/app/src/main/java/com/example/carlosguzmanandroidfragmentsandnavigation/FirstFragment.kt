package com.example.carlosguzmanandroidfragmentsandnavigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class FirstFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.goToSecondFragmentBtn).setOnClickListener {
            findNavController().navigate(R.id.action_firstFragment_to_secondFragment)
        }

        view.findViewById<View>(R.id.goToThirdFragmentBtn).setOnClickListener {
            findNavController().navigate(R.id.action_firstFragment_to_thirdFragment)
        }

        view.findViewById<View>(R.id.goToActionsActivityBtn).setOnClickListener {
            findNavController().navigate(R.id.action_firstFragment_to_actionsActivity)
        }
    }
}