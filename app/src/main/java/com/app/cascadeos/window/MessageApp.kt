package com.app.cascadeos.window

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import com.app.cascadeos.R
import com.app.cascadeos.adapter.ChatMessageAdapter
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutMessageBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.utility.Y_AXIS_START
import com.app.cascadeos.utility.dpToPx
import com.app.cascadeos.utility.logError
import com.app.cascadeos.utility.pxToDp
import com.app.cascadeos.utility.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch


class MessageApp(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    lifecycle: Lifecycle,
    private val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    val mBinding: ActivityMainBinding,
    private val configurationLiveData: MutableLiveData<Configuration?>,
) : WindowApp<LayoutMessageBinding>(
    context, lifecycle, R.layout.layout_message, mBinding, configurationLiveData
) {
    private var tempStringLength = 0
    private val translationYFlow = MutableStateFlow(Y_AXIS_START)
    override fun getDetailsModel(): AppDetailModel {
        return appDetailModel
    }

    override fun appList(): ArrayList<AppDetailModel> {
        return appList
    }

    fun top() {
        binding.root.bringToFront()
    }

    @FlowPreview
    override fun start() {
        super.start()


        CoroutineScope(Dispatchers.Main).launch {
            translationYFlow.debounce(500).collectLatest {
                      "TranslationFlow $it".logError(javaClass)
                binding.root.translationY = it.toFloat()
            }
        }


        binding.editMsg.viewTreeObserver.addOnGlobalLayoutListener {
//            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            val r = Rect()
            binding.root.getWindowVisibleDisplayFrame(r)
            val screenHeight: Int = mBinding.root.rootView.height

            if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                val heightDifference = (screenHeight - r.bottom - r.top) - (screenHeight - binding.root.height)

                if (heightDifference > 10) {
                    binding.layoutContent.setPadding(0, 0, 0, heightDifference)

                } else {
                    binding.layoutContent.setPadding(0, 0, 0, 0)
                }

            } else {
                binding.layoutContent.setPadding(0, 0, 0, 0)
                var transitionY = binding.root.translationY - ((binding.root.translationY + binding.root.height) - r.height())


                if (binding.root.translationY > 1 && transitionY <= 330) {
                   // Keyboard Open
                } else {
                    transitionY = binding.root.translationY
                    when (transitionY.pxToDp(context).toInt()) {
                        in 1..context.resources.getInteger(R.integer.b_start) -> {
                            transitionY = context.resources.getInteger(R.integer.b_start).dpToPx(context)
                        }

                        in context.resources.getInteger(R.integer.b_start)..context.resources.getInteger(
                            R.integer.c_start
                        ),
                        -> {
                            transitionY = context.resources.getInteger(R.integer.c_start).dpToPx(context)
                        }

                        else -> {
                            transitionY = 0f
                        }
                    }



                    //binding.root.translationY = transitionMarginY.toFloat()
                }

                "Translation $transitionY".logError(javaClass)

                CoroutineScope(Dispatchers.Default).launch {
                    translationYFlow.emit(transitionY.toInt())
                }


            }
        }


        /* binding.layoutContent.fitsSystemWindows = true
         binding.layoutContent.setOnApplyWindowInsetsListener { v, insets ->

             val bottomPadding = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                 insets.getInsets(WindowInsets.Type.ime()).bottom - insets.getInsets(WindowInsets.Type.statusBars()).bottom
             } else {
                 insets.systemWindowInsetBottom
             }
             if (bottomPadding > 0) {
                 v.setPadding(0, 0, 0, bottomPadding)
             } else {
                 v.setPadding(0, 0, 0, 0)
             }
             insets
         }*/

        val adapter = ChatMessageAdapter()
        binding.rcvMessages.adapter = adapter
        val layoutInflater = LayoutInflater.from(context)

        binding.btnSend.setOnClickListener {
            if (!binding.editMsg.text?.trim().isNullOrEmpty()) {
                adapter.addMessage(binding.editMsg.text.toString())
                binding.rcvMessages.scrollToPosition(adapter.itemCount - 1)
                binding.editMsg.text?.clear()
                binding.editMsg.clearFocus()
            } else {
                context.showToast(context.getString(R.string.please_enter_message))

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
                            val hangingTextView =
                                layoutInflater.inflate(
                                    R.layout.layout_hanging_text,
                                    binding.hangingLayout,
                                    false
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
                                    context,
                                    R.anim.top_to_bottom
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

    override fun close() {
        super.close()
        closeWindowListener.closeWindow(appDetailModel)
    }

}
