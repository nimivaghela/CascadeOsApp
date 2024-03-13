package com.app.cascadeos.ui.fragment

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.app.cascadeos.R
import com.app.cascadeos.adapter.VideoChatMessageAdapter
import com.app.cascadeos.databinding.FragmentCoolEcallVideoMessageBinding
import com.app.cascadeos.utility.dpToPx
import com.app.cascadeos.utility.showToast

class CoolEcallVideoMessageFragment : Fragment() {

    private lateinit var binding: FragmentCoolEcallVideoMessageBinding
    private var tempStringLength = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_cool_ecall_video_message, container, false
        )
        // binding.clickListener = mainOnClickListener
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.layoutMain.viewTreeObserver.addOnGlobalLayoutListener {
//            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val r = Rect()
            binding.root.getWindowVisibleDisplayFrame(r)
            val screenHeight: Int = binding.root.rootView.height

            if (!isAdded) {
                return@addOnGlobalLayoutListener
            }


            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                val heightDifference = (screenHeight - r.bottom - r.top) - (screenHeight - binding.root.height - 16.dpToPx(
                    requireContext()
                ).toInt())

                if (heightDifference > 10) {
                    binding.layoutContent.setPadding(0, 0, 0, heightDifference)

                } else {
                    binding.layoutContent.setPadding(0, 0, 0, 0)
                }

            } else {
                val transitionY =
                    binding.root.translationY - ((binding.root.translationY + binding.root.height) - r.height())

                if (binding.root.translationY > 1 && transitionY <= 330) {
                    binding.root.translationY = transitionY
                } else {
                    var transitionMarginY = binding.root.translationY.toInt()
                    when (transitionMarginY) {
                        in 1..resources.getInteger(R.integer.b_start) -> {
                            transitionMarginY = resources.getInteger(R.integer.b_start)
                        }

                        in resources.getInteger(R.integer.b_start)..resources.getInteger(
                            R.integer.c_start
                        ),
                        -> {
                            transitionMarginY = resources.getInteger(R.integer.c_start)
                        }

                        else -> {
                            transitionMarginY = 0
                        }
                    }

                    binding.root.translationY = transitionMarginY.toFloat()

                }

            }
        }

        val adapter = VideoChatMessageAdapter()
        binding.rcvMessages.adapter = adapter
        val layoutInflater = LayoutInflater.from(context)

        binding.btnSend.setOnClickListener {
            if (!binding.editMsg.text?.trim().isNullOrEmpty()) {
                adapter.addMessage(binding.editMsg.text.toString())
                binding.rcvMessages.scrollToPosition(adapter.itemCount - 1)
                binding.editMsg.text?.clear()
                binding.editMsg.clearFocus()
            } else {
                context?.showToast(context?.getString(R.string.please_enter_message))

            }
        }
        binding.editMsg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    if (s!!.isNotEmpty()) {
                        val hangingText = s.toString().substringAfterLast(" ")
                        val characterList = hangingText.toCharArray().toList()
                        if (characterList.isNotEmpty()) {
                            val hangingTextView = layoutInflater.inflate(
                                R.layout.layout_hanging_text, binding.hangingLayout, false
                            ) as TextView

                            hangingTextView.text = characterList.last().toString()
                            if (tempStringLength > s.length) {
                                if (binding.hangingLayout.childCount > 0) {
                                    val cursorPosition: Int = binding.editMsg.selectionStart
                                    println("position is $cursorPosition")
                                    if (s.contains(" ")) {
                                        val char: Char = s.elementAt(cursorPosition)
                                        println("came in $cursorPosition")
                                        println("char is $char")
                                        if (characterList.contains(char)) {
                                            binding.hangingLayout.removeViewAt(
                                                characterList.indexOf(
                                                    char
                                                )
                                            )
                                        }
                                    } else if (s.length > cursorPosition) {
                                        binding.hangingLayout.removeViewAt(cursorPosition)
                                    } else {
                                        binding.hangingLayout.removeViewAt(characterList.size)
                                    }
                                }
                            } else {
                                binding.hangingLayout.addView(hangingTextView)
                            }
                            tempStringLength = s.length
                            hangingTextView.startAnimation(
                                AnimationUtils.loadAnimation(
                                    context, R.anim.top_to_bottom
                                )
                            )
                        } else {
                            binding.hangingLayout.removeAllViews()
                            tempStringLength = s.length
                        }
                    } else {
                        binding.hangingLayout.removeAllViews()
                        tempStringLength = s.length
                    }
                } catch (e: Exception) {

                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

}

