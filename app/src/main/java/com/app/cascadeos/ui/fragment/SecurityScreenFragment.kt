package com.app.cascadeos.ui.fragment

import android.animation.LayoutTransition
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ActivitySecurityScreenBinding


class SecurityScreenFragment(
) : Fragment() {

    private lateinit var binding: ActivitySecurityScreenBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.activity_security_screen, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)

        binding.imgBack.setOnClickListener { requireActivity().onBackPressed() }

    }
}
