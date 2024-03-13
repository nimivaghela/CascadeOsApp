package com.app.cascadeos.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.cascadeos.R
import com.app.cascadeos.adapter.CoolEcallAdapter
import com.app.cascadeos.databinding.FragmentCoolEcallListBinding
import com.app.cascadeos.model.CoolEcallModel
import com.app.cascadeos.utility.showToast

class CoolEcallListFragment : Fragment() {
    private lateinit var binding: FragmentCoolEcallListBinding
    var coolEcallModel: CoolEcallModel? = null
    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                coolEcallModel?.let { startVideoCall(it) }
            } else {
                requireContext().showToast(getString(R.string.permission_camera_rationale_message))
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        // Inflate the layout for this fragment
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_cool_ecall_list, container, false)
        // binding.clickListener = mainOnClickListener
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
    }

    private fun setUpRecyclerView() {
        val onCoolEcall: MutableList<CoolEcallModel> = mutableListOf()
        binding.rvEcallList.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        onCoolEcall.add(
            CoolEcallModel(
                1,
                R.drawable.ic_baby_cakes,
                "Baby Cakes",
                "+1-305-206-1589",
                R.drawable.ic_fake_videoimage
            )
        )
        onCoolEcall.add(
            CoolEcallModel(
                2,
                R.drawable.ic_jeanette,
                "Jeanette",
                "+1-305-206-1589",
                R.drawable.iv_jeanette
            )
        )
        onCoolEcall.add(CoolEcallModel(3, R.drawable.ic_mom, "Mom", "+1-305-206-1589"))
        onCoolEcall.add(CoolEcallModel(4, R.drawable.ic_dad, "Dad", "+1-305-206-1589"))
        onCoolEcall.add(
            CoolEcallModel(
                5,
                R.drawable.ic_emily_street,
                "Emily A Street",
                "+1-305-206-1589"
            )
        )
        onCoolEcall.add(
            CoolEcallModel(
                6,
                R.drawable.ic_andrea_james,
                "Andrea James",
                "+1-305-206-1589"
            )
        )
        onCoolEcall.add(
            CoolEcallModel(
                7,
                R.drawable.ic_joshva_glad,
                "Joshva Glad",
                "+1-305-206-1589"
            )
        )
        onCoolEcall.add(
            CoolEcallModel(
                8,
                R.drawable.ic_ratani_glues,
                "Ratani glues",
                "+1-305-206-1589"
            )
        )
        binding.rvEcallList.adapter =
            CoolEcallAdapter(requireContext(), object : CoolEcallAdapter.CoolEcallItemClicked {
                override fun onCoolEcallItemClicked(position: Int, coolEcallModel: CoolEcallModel) {
                    this@CoolEcallListFragment.coolEcallModel = coolEcallModel
                    if (position == 0 || position == 1) {
                        cameraPermission(coolEcallModel)
                        //startVideoCall(coolEcallModel)
                    }
                }

            }, onCoolEcall)
    }

    private fun startVideoCall(appModel: CoolEcallModel) {
        val ft: FragmentTransaction =
            parentFragmentManager.beginTransaction()
        ft.replace(
            R.id.fragment_container,
            CoolEcallVideoCallFragment(appModel)
        )
        ft.addToBackStack(null)
        ft.commit()
    }

    private fun cameraPermission(appModel: CoolEcallModel) {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startVideoCall(appModel)
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

}