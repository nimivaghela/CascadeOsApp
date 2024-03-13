package com.app.cascadeos.window

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.app.cascadeos.R
import com.app.cascadeos.base.WindowApp
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.databinding.LayoutEmailBinding
import com.app.cascadeos.interfaces.CloseWindowListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.ui.MainActivity
import com.app.cascadeos.utility.hideKeyboard
import com.app.cascadeos.utility.showToast


class EmailApp(
    private val appDetailModel: AppDetailModel,
    val context: Context,
    val lifecycle: Lifecycle,
    val closeWindowListener: CloseWindowListener,
    val appList: ArrayList<AppDetailModel>,
    mBinding: ActivityMainBinding,
    private val configurationLiveData: MutableLiveData<Configuration?>,
    val onFocusClick: View.OnFocusChangeListener,
    private val keyTextLiveData: MutableLiveData<String>?,
) : WindowApp<LayoutEmailBinding>(
    context,
    lifecycle,
    R.layout.layout_email,
    mBinding,
    configurationLiveData
) {

    private val textObserver = Observer<String> {
        if (isObserverActive) {
            try {
                it?.let {
                    binding.apply {
                        if (edtSenderMail.hasFocus()) {
                            val cursorPosition: Int = edtSenderMail.selectionStart
                            if (it == context.getString(R.string.clear)) {
                                if (cursorPosition > 0) {
                                    if (edtSenderMail.text.toString().length > cursorPosition) {
                                        edtSenderMail.text = edtSenderMail.text?.delete(
                                            cursorPosition - 1,
                                            cursorPosition
                                        )
                                        edtSenderMail.setSelection(cursorPosition - 1)
                                    } else {
                                        edtSenderMail.setText(
                                            edtSenderMail.text.toString().dropLast(1)
                                        )
                                        edtSenderMail.setSelection(edtSenderMail.text.toString().length)
                                    }
                                }
                            } else {
                                edtSenderMail.text?.insert(cursorPosition, it).toString()
                                edtSenderMail.setSelection(cursorPosition + 1)
                            }
                        } else if (edtSubject.hasFocus()) {
                            val cursorPosition: Int = edtSubject.selectionStart
                            if (it == context.getString(R.string.clear)) {
                                if (cursorPosition > 0) {
                                    if (edtSubject.text.toString().length > cursorPosition) {
                                        edtSubject.text = edtSubject.text?.delete(
                                            cursorPosition - 1,
                                            cursorPosition
                                        )
                                        edtSubject.setSelection(cursorPosition - 1)
                                    } else {
                                        edtSubject.setText(
                                            edtSubject.text.toString().dropLast(1)
                                        )
                                        edtSubject.setSelection(edtSubject.text.toString().length)
                                    }
                                }
                            } else {
                                edtSubject.text?.insert(cursorPosition, it).toString()
                                edtSubject.setSelection(cursorPosition + 1)
                            }
                        } else if (edtEmailBody.hasFocus()) {
                            val cursorPosition: Int = edtEmailBody.selectionStart
                            if (it == context.getString(R.string.clear)) {
                                if (cursorPosition > 0) {
                                    if (edtEmailBody.text.toString().length > cursorPosition) {
                                        edtEmailBody.text = edtEmailBody.text?.delete(
                                            cursorPosition - 1,
                                            cursorPosition
                                        )
                                        edtEmailBody.setSelection(cursorPosition - 1)
                                    } else {
                                        edtEmailBody.setText(
                                            edtEmailBody.text.toString().dropLast(1)
                                        )
                                        edtEmailBody.setSelection(edtEmailBody.text.toString().length)
                                    }
                                }
                            } else {
                                edtEmailBody.text?.insert(cursorPosition, it).toString()
                                edtEmailBody.setSelection(cursorPosition + 1)
                            }
                        } else {

                        }
                    }

                }
            } catch (e: Exception) {

            }

        }
    }

    override fun getDetailsModel() = appDetailModel

    override fun appList() = appList

    @SuppressLint("ClickableViewAccessibility")
    override fun start() {
        super.start()

        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.layoutMain.layoutTransition = layoutTransition
        binding.layoutContent.layoutTransition = layoutTransition
        keyTextLiveData?.observe((context as MainActivity), textObserver)
        binding.edtSenderMail.showSoftInputOnFocus = false
        binding.edtEmailBody.showSoftInputOnFocus = false
        binding.edtSubject.showSoftInputOnFocus = false
        binding.focusListener = onFocusClick
        binding.apply {
            clickListener = View.OnClickListener {
                when (it.id) {
                    R.id.img_send -> {
                        if (edtSenderMail.text.toString()
                                .isNotEmpty() && edtEmailBody.text.toString()
                                .isNotEmpty() && edtSubject.text.toString().isNotEmpty()
                        ) {
                            context.showToast("Email Sent")
                            (context as MainActivity).hideKeyboard()
                            clearFields()
                        }
                    }
                }
            }
        }
    }

    private fun clearFields() {
        binding.edtSenderMail.text?.clear()
        binding.edtSenderMail.clearFocus()
        binding.edtSubject.text?.clear()
        binding.edtSubject.clearFocus()
        binding.edtEmailBody.text?.clear()
        binding.edtEmailBody.clearFocus()
    }

    override fun close() {
        super.close()
        clearFields()
        closeWindowListener.closeWindow(appDetailModel)
        keyTextLiveData?.removeObserver(textObserver)
    }

    fun top() {
        binding.root.bringToFront()
    }

    fun resumeDefaultText() {
        binding.edtSenderMail.setText(context.getString(R.string.client_email))
        binding.edtSubject.setText(context.getString(R.string.meeting))
        binding.edtEmailBody.setText(context.getString(R.string.hello_thanks_for_sharing_information_we_will_connect_as_soon_as_possible))
    }
}