package com.app.cascadeos.base

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Looper
import android.view.*
import android.view.View.*
import android.view.WindowManager.LayoutParams.MATCH_PARENT
import androidx.activity.*
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.*
import com.app.cascadeos.R
import com.app.cascadeos.databinding.ActivityMainBinding
import com.app.cascadeos.interfaces.SnapToGridListener
import com.app.cascadeos.model.AppDetailModel
import com.app.cascadeos.model.Screen
import com.app.cascadeos.ui.CascadeActivity
import com.app.cascadeos.ui.MainActivity
import com.app.cascadeos.utility.*
import kotlin.math.abs


abstract class WindowApp<T : ViewDataBinding>(
    private val context: Context,
    private val lifecycle: Lifecycle,
    private val layoutId: Int,
    private val mBinding: ActivityMainBinding,
    private val configurationLiveData: MutableLiveData<Configuration?>?,
) : DefaultLifecycleObserver {

    lateinit var binding: T
    lateinit var floatWindowLayoutParam: ConstraintLayout.LayoutParams
    var screen = Screen.DEFAULT
    var isMiniMize = false
    var startX = context.resources.getInteger(R.integer.a_start)
    var startY = Y_AXIS_START
    var windowWidth = context.resources.getInteger(R.integer.a_width)
    var windowHeight = context.resources.getInteger(R.integer.a_height)
    val B_BLOCK_HEIGHT = context.resources.getInteger(R.integer.b_height)
    val SCROLL_THRESHOLD = 10f
    val appBarHandler = android.os.Handler(Looper.getMainLooper())
    private lateinit var appBar: ConstraintLayout
    private lateinit var imgResize: AppCompatImageView
    private lateinit var imgResizeLeft: AppCompatImageView

    open val snapToGridListener: SnapToGridListener? = null
    var isObserverActive = false

    init {
        configurationLiveData?.postValue(null)
    }


    private val configObserver = Observer<Configuration?> {

        if (isObserverActive) {
            it?.let {

                var width = floatWindowLayoutParam.width.toFloat().pxToDp(context)
                var height = floatWindowLayoutParam.height.toFloat().pxToDp(context)

                var x = binding.root.translationX.pxToDp(context).toInt()
                var y = binding.root.translationY.pxToDp(context).toInt()

                if (it.orientation == Configuration.ORIENTATION_PORTRAIT) {

                    when (x) {
                        in 1..context.resources.getInteger(R.integer.b_start) -> {
                            x = context.resources.getInteger(R.integer.b_start)
                        }

                        in context.resources.getInteger(R.integer.b_start)..context.resources.getInteger(
                            R.integer.c_start
                        ),
                        -> {
                            x = context.resources.getInteger(R.integer.c_start)
                        }
                    }


                    val aHeight = context.resources.getInteger(R.integer.a_height).toFloat()
                    val bHeight = context.resources.getInteger(R.integer.b_height).toFloat()
                    val cHeight = context.resources.getInteger(R.integer.c_height).toFloat()

                    val aBHeight = aHeight + bHeight
                    val cBHeight = cHeight + bHeight
                    when (width) {
                        in (aHeight - 1)..(aHeight + 1) -> {
                            width = cHeight
                        }

                        in (cHeight - 1)..(cHeight + 1) -> {
                            width = aHeight
                        }

                        in (aBHeight - 1)..(aBHeight + 1) -> {
                            width = cBHeight
                        }

                        in (cBHeight - 1)..(cBHeight + 1) -> {
                            width = aBHeight
                        }
                    }

                } else {

                    when {
                        y in 1..context.resources.getInteger(R.integer.c_start) -> {
                            y = context.resources.getInteger(R.integer.b_start)
                        }

                        y > context.resources.getInteger(R.integer.c_start) -> {
                            y = context.resources.getInteger(R.integer.c_start)
                        }
                    }

                    val aWidth = context.resources.getInteger(R.integer.a_width).toFloat()
                    val bWidth = context.resources.getInteger(R.integer.b_width).toFloat()
                    val cWidth = context.resources.getInteger(R.integer.c_width).toFloat()

                    val aBWidth = aWidth + bWidth
                    val cBWidth = cWidth + bWidth

                    when (height) {
                        in (aWidth - 1)..(aWidth + 1) -> {
                            height = cWidth
                        }

                        in (cWidth - 1)..(cWidth + 1) -> {
                            height = aWidth
                        }

                        in (aBWidth - 1)..(aBWidth + 1) -> {
                            height = cBWidth
                        }

                        in (cBWidth - 1)..(cBWidth + 1) -> {
                            height = aBWidth
                        }
                    }
                }


                floatWindowLayoutParam.width = height.toInt().dpToPx(context).toInt()
                floatWindowLayoutParam.height = width.toInt().dpToPx(context).toInt()

                binding.root.translationX = y.dpToPx(context)
                binding.root.translationY = x.dpToPx(context)


                binding.root.post {
                    mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)
                }
            }
        }
        isObserverActive = true
    }

    override fun onDestroy(owner: LifecycleOwner) {
        close()
        super.onDestroy(owner)
    }

    abstract fun getDetailsModel(): AppDetailModel

    abstract fun appList(): ArrayList<AppDetailModel>

    @SuppressLint("ClickableViewAccessibility")
    open fun start() {
        lifecycle.addObserver(this)

        binding = DataBindingUtil.inflate(LayoutInflater.from(context), layoutId, null, false)/*val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ConstraintLayout.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            ConstraintLayout.LayoutParams.TYPE_TOAST;
        }*/


        appBar = binding.root.findViewById(R.id.action_bar)
        val hiddenAppBar = binding.root.findViewById<View>(R.id.layout_hidden_actionbar)
        val cascadeButton = appBar.findViewById<AppCompatImageView>(R.id.img_cascade)
        imgResize = binding.root.findViewById(R.id.img_resize)
        imgResizeLeft = binding.root.findViewById(R.id.img_resize_left)

        hiddenAppBar.setOnClickListener {
//            if(getDetailsModel().name != context.getString(R.string.txt_video_game)){
            appBar.visibility = VISIBLE
            imgResize.visibility = VISIBLE
            imgResizeLeft.visibility = VISIBLE
            hideAppBar()
//            }
        }

        cascadeButton.setOnClickListener {
            val intent = Intent(context, CascadeActivity::class.java)
            (context as MainActivity).getCascadeResult.launch(intent)
        }

        hideAppBar()

        if (isPortrait()) {
            when (getDetailsModel().gravity) {
                Gravity.START -> {
                    //startX = A_START
                    windowHeight = context.resources.getInteger(R.integer.a_height)
                }

                Gravity.CENTER -> {
                    //startX = context.resources.getInteger(R.integer.b_start)
                    windowHeight = context.resources.getInteger(R.integer.b_height)
                    windowWidth = context.resources.getInteger(R.integer.b_width)
                }

                Gravity.END -> {
                    windowHeight = context.resources.getInteger(R.integer.c_height)
                    //startX = context.resources.getInteger(R.integer.c_start)
                }

                else -> {
                    // startX = A_START
                }
            }
        } else {
            when (getDetailsModel().gravity) {
                Gravity.START -> {
                    //startX = A_START
                    windowWidth = context.resources.getInteger(R.integer.a_width)
                }

                Gravity.CENTER -> {
                    //startX = context.resources.getInteger(R.integer.b_start)
                    windowWidth = context.resources.getInteger(R.integer.b_width)
                    windowHeight = B_BLOCK_HEIGHT
                }

                Gravity.END -> {
                    windowWidth = context.resources.getInteger(R.integer.c_width)
                    //startX = context.resources.getInteger(R.integer.c_start)
                }

                else -> {
                    // startX = A_START
                }
            }
        }


        if (getDetailsModel().height != 0) {
            windowHeight = getDetailsModel().height
        }
        if (getDetailsModel().width != 0) {
            windowWidth = getDetailsModel().width
        }
        startX = getDetailsModel().startX



        floatWindowLayoutParam = ConstraintLayout.LayoutParams(
            windowWidth.dpToPx(context).toInt(), windowHeight.dpToPx(context).toInt()/*layoutType, LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT*/
        )
        //floatWindowLayoutParam.gr = Gravity.START or Gravity.TOP
        //floatWindowLayoutParam.x = startX.dpToPx(context).toInt()
        getDetailsModel().startX = startX
        getDetailsModel().width = windowWidth
        getDetailsModel().height = windowHeight

        /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
             floatWindowLayoutParam.layoutInDisplayCutoutMode = LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
         }



         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
             floatWindowLayoutParam.fitInsetsTypes = WindowInsets.Type.systemBars()
             floatWindowLayoutParam.isFitInsetsIgnoringVisibility = false
         }*/

        // KeyBoard App startY is bottom app the screen.
        if (getDetailsModel().name == context.getString(R.string.key_board)) {
            startY = 125
        }

        startXApp(startX)
        startYApp(startY)


        val layoutTransition = LayoutTransition()
        layoutTransition.enableTransitionType(LayoutTransition.CHANGING)
        binding.root.findViewById<ConstraintLayout>(R.id.layout_main).layoutTransition =
            layoutTransition
        appBar.layoutTransition = layoutTransition
        mBinding.layoutMain.addView(binding.root, floatWindowLayoutParam)




        configurationLiveData?.observe((context as MainActivity), configObserver)


        /*  val toolbar =  binding.root.findViewById<MaterialToolbar>(R.id.action_bar)
          toolbar.setTitle(title)*/
        val toolBarTitle = binding.root.findViewById<AppCompatTextView>(R.id.txt_title)
        toolBarTitle.text = getDetailsModel().name
        toolBarTitle.isSelected = true
        val closeOption: AppCompatImageView = binding.root.findViewById(R.id.img_close)
        closeOption.setOnClickListener {
            close()
        }


        val fullscreenOption: AppCompatImageView = binding.root.findViewById(R.id.img_full_screen)
        fullscreenOption.setOnClickListener {
            if (screen == Screen.FULLSCREEN) {
                defaultMode()
            } else {
                fullScreenMode()
            }

        }

        val minimizeOption: AppCompatImageView = binding.root.findViewById(R.id.img_minimize)
        minimizeOption.setOnClickListener {
            if (isMiniMize) {
                maximizeMode()
            } else {
                minimizeMode()

            }
        }






        appBar.setOnTouchListener(object : OnTouchListener {
            var x = 0.0
            var y = 0.0
            var px = 0.0
            var py = 0.0

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        x = binding.root.translationX.toDouble()
                        y = binding.root.translationY.toDouble()

                        // returns the original raw X
                        // coordinate of this event
                        px = event.rawX.toDouble()

                        // returns the original raw Y
                        // coordinate of this event
                        py = event.rawY.toDouble()
                        binding.root.bringToFront()
                    }

                    MotionEvent.ACTION_MOVE -> {

                        if (isPortrait()) {

                            binding.root.translationY = (y + event.rawY - py).toFloat()
                            if (isMiniMize) {
                                val xDelta = x + event.rawX - px
                                // Y position not set out of display
                                if (xDelta >= 0 && xDelta <= context.resources.getInteger(R.integer.a_height)) {
                                    binding.root.translationX = xDelta.toFloat()
                                }
                            }

                        } else {
                            binding.root.translationX = (x + event.rawX - px).toFloat()
                            if (isMiniMize || getDetailsModel().name == context.getString(R.string.key_board)) {
                                val yDelta = y + event.rawY - py
                                // Y position not set out of display
                                if (yDelta >= 0 && yDelta <= context.resources.getInteger(R.integer.a_height)) {
                                    binding.root.translationY = yDelta.toFloat()
                                }
                            }
                        }
                    }

                    MotionEvent.ACTION_UP -> {

                        if (isPortrait()) {
                            if (py.toInt() != event.rawY.toInt()) {
                                var dpY = (y + event.rawY - py).toFloat().pxToDp(context)


                                if (py < event.rawY) {
                                    dpY += getDetailsModel().width
                                }


                                var actualY: Int = dpY.toInt()
                                if (dpY < context.resources.getInteger(R.integer.b_start)) {
                                    if (checkBlockA()) {
                                        actualY = context.resources.getInteger(R.integer.a_start)
                                        val appStart = getDetailsModel().startX
                                        val appHeight = getDetailsModel().height
                                        windowHeight =
                                            if (appStart + appHeight <= context.resources.getInteger(
                                                    R.integer.c_start
                                                ) + 2
                                            ) {
                                                screen = Screen.DEFAULT
                                                context.resources.getInteger(R.integer.a_height) + context.resources.getInteger(
                                                    R.integer.b_height
                                                )
                                            } else {
                                                screen = Screen.FULLSCREEN
                                                (context.resources.getInteger(R.integer.a_height) + context.resources.getInteger(
                                                    R.integer.b_height
                                                ) + context.resources.getInteger(R.integer.c_height))
                                            }

                                    } else {
                                        screen = Screen.DEFAULT
                                        actualY = context.resources.getInteger(R.integer.a_start)
                                        windowHeight =
                                            context.resources.getInteger(R.integer.a_height)
                                    }
                                    windowWidth = context.resources.getInteger(R.integer.a_width)

                                } else if (dpY > context.resources.getInteger(R.integer.b_start) && dpY < context.resources.getInteger(
                                        R.integer.c_start
                                    )
                                ) {
                                    if (checkBlockB()) {
                                        actualY =
                                            if (getDetailsModel().startX < context.resources.getInteger(
                                                    R.integer.a_height
                                                )
                                            ) {
                                                windowHeight =
                                                    (context.resources.getInteger(R.integer.a_height) + context.resources.getInteger(
                                                        R.integer.b_height
                                                    ))
                                                context.resources.getInteger(R.integer.a_start)
                                            } else {
                                                windowHeight =
                                                    context.resources.getInteger(R.integer.b_height) + context.resources.getInteger(
                                                        R.integer.c_height
                                                    )
                                                context.resources.getInteger(R.integer.b_start)

                                            }
                                        windowWidth =
                                            context.resources.getInteger(R.integer.a_width)
                                    } else {
                                        actualY = context.resources.getInteger(R.integer.b_start)
                                        windowWidth =
                                            context.resources.getInteger(R.integer.b_width)
                                        windowHeight =
                                            context.resources.getInteger(R.integer.b_height)

                                    }
                                    screen = Screen.DEFAULT


                                } else if (dpY > context.resources.getInteger(R.integer.c_start)) {

                                    if (checkBlockC()) {
                                        actualY = getDetailsModel().startX
                                        windowHeight =
                                            if (getDetailsModel().startX >= context.resources.getInteger(
                                                    R.integer.b_start
                                                )
                                            ) {
                                                screen = Screen.DEFAULT
                                                context.resources.getInteger(R.integer.b_height) + context.resources.getInteger(
                                                    R.integer.c_height
                                                )
                                            } else {
                                                screen = Screen.FULLSCREEN
                                                context.resources.getInteger(R.integer.a_height) + context.resources.getInteger(
                                                    R.integer.b_height
                                                ) + context.resources.getInteger(R.integer.c_height)
                                            }
                                    } else {
                                        screen = Screen.DEFAULT
                                        windowHeight =
                                            context.resources.getInteger(R.integer.c_height)
                                        actualY = context.resources.getInteger(R.integer.c_start)
                                    }
                                    windowWidth = context.resources.getInteger(R.integer.a_width)
                                }


                                /* // Multicast system drag to section B open VideoGame App
                                 if (getDetailsModel().name == context.getString(R.string.multicast_system) && actualY == context.resources.getInteger(R.integer.b_start)) {
                                     snapToGridListener?.moveView(getDetailsModel())
                                     close()
                                     return true
                                 }*/

                                /*  // Phone System  drag to section B open Messages App
                                  if (getDetailsModel().name == context.getString(R.string.phone_system) && actualY == context.resources.getInteger(R.integer.b_start)) {
                                      snapToGridListener?.moveView(getDetailsModel())
                                      windowWidth = getDetailsModel().width
                                      actualY = getDetailsModel().startX
                                      windowHeight = getDetailsModel().height
                                  }*/


                                /*   // VideoGame width restricted.
                                   if (getDetailsModel().name == context.getString(R.string.txt_video_game)) {
                                       if (windowWidth < context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(R.integer.b_width)
                                       ) {
                                           windowWidth = if (actualX == context.resources.getInteger(R.integer.a_start)) {
                                              context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(R.integer.b_width)
                                           } else {
                                               context.resources.getInteger(R.integer.b_width) + context.resources.getInteger(R.integer.c_width)
                                           }
                                       }

                                       if (actualX >= context.resources.getInteger(R.integer.b_start)) {
                                           actualX = context.resources.getInteger(R.integer.b_start)
                                       }
                                   }*/


                                floatWindowLayoutParam.width = windowWidth.dpToPx(context).toInt()

                                if (!isMiniMize) {
                                    floatWindowLayoutParam.height =
                                        windowHeight.dpToPx(context).toInt()
                                }
                                binding.root.translationY = actualY.dpToPx(context)
                                binding.root.layoutParams = floatWindowLayoutParam


                                //floatWindowLayoutParam.y = (y + event.rawY - py).toInt()
                                if (screen != Screen.FULLSCREEN) {
                                    getDetailsModel().startX = actualY
                                    getDetailsModel().width = windowWidth
                                    getDetailsModel().height = windowHeight

                                }

                            }

                        } else {

                            if (px.toInt() != event.rawX.toInt()) {
                                var dpX = (x + event.rawX - px).toFloat().pxToDp(context)


                                if (px < event.rawX) {
                                    dpX += getDetailsModel().width
                                }


                                var actualX: Int = dpX.toInt()
                                if (dpX < context.resources.getInteger(R.integer.b_start)) {
                                    if (checkBlockA()) {
                                        actualX = context.resources.getInteger(R.integer.a_start)
                                        val appStart = getDetailsModel().startX
                                        val appWidth = getDetailsModel().width

                                        windowWidth =
                                            if (appStart + appWidth <= context.resources.getInteger(
                                                    R.integer.c_start
                                                ) + 2
                                            ) {
                                                screen = Screen.DEFAULT
                                                context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                                    R.integer.b_width
                                                )
                                            } else {
                                                screen = Screen.FULLSCREEN
                                                context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                                    R.integer.b_width
                                                ) + context.resources.getInteger(R.integer.c_width)
                                            }

                                    } else {
                                        screen = Screen.DEFAULT
                                        actualX = context.resources.getInteger(R.integer.a_start)
                                        windowWidth =
                                            context.resources.getInteger(R.integer.a_width)
                                    }
                                    windowHeight = context.resources.getInteger(R.integer.a_height)

                                } else if (dpX > context.resources.getInteger(R.integer.b_start) && dpX < context.resources.getInteger(
                                        R.integer.c_start
                                    )
                                ) {
                                    if (checkBlockB()) {
                                        actualX =
                                            if (getDetailsModel().startX < context.resources.getInteger(
                                                    R.integer.a_width
                                                )
                                            ) {
                                                windowWidth =
                                                    context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                                        R.integer.b_width
                                                    )
                                                context.resources.getInteger(R.integer.a_start)
                                            } else {
                                                windowWidth =
                                                    context.resources.getInteger(R.integer.b_width) + context.resources.getInteger(
                                                        R.integer.c_width
                                                    )
                                                context.resources.getInteger(R.integer.b_start)

                                            }
                                        windowHeight =
                                            context.resources.getInteger(R.integer.a_height)
                                    } else {
                                        actualX = context.resources.getInteger(R.integer.b_start)
                                        windowWidth =
                                            context.resources.getInteger(R.integer.b_width)
                                        windowHeight =
                                            context.resources.getInteger(R.integer.b_height)

                                    }
                                    screen = Screen.DEFAULT


                                } else if (dpX > context.resources.getInteger(R.integer.c_start)) {

                                    if (checkBlockC()) {
                                        actualX = getDetailsModel().startX
                                        windowWidth =
                                            if (getDetailsModel().startX >= context.resources.getInteger(
                                                    R.integer.b_start
                                                )
                                            ) {
                                                screen = Screen.DEFAULT
                                                context.resources.getInteger(R.integer.b_width) + context.resources.getInteger(
                                                    R.integer.c_width
                                                )
                                            } else {
                                                screen = Screen.FULLSCREEN
                                                context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                                    R.integer.b_width
                                                ) + context.resources.getInteger(R.integer.c_width)
                                            }
                                    } else {
                                        screen = Screen.DEFAULT
                                        windowWidth =
                                            context.resources.getInteger(R.integer.c_width)
                                        actualX = context.resources.getInteger(R.integer.c_start)
                                    }
                                    windowHeight = context.resources.getInteger(R.integer.a_height)
                                }


                                // Multicast system drag to section B open VideoGame App
                                if (getDetailsModel().name == context.getString(R.string.multicast_system) && actualX == context.resources.getInteger(
                                        R.integer.b_start
                                    )
                                ) {
                                    snapToGridListener?.moveView(getDetailsModel())
                                    close()
                                    return true
                                }

                                // Phone System  drag to section B open Messages App
                                if (getDetailsModel().name == context.getString(R.string.phone_system) && (actualX == context.resources.getInteger(
                                        R.integer.b_start
                                    ) || (actualX == context.resources.getInteger(
                                        R.integer.a_start
                                    ) && windowWidth <= context.resources.getInteger(R.integer.c_start) + 3))
                                ) {
                                    snapToGridListener?.moveView(getDetailsModel())
                                    close()
                                    return true
                                }


                                // VideoGame width restricted.
                                if (getDetailsModel().name == context.getString(R.string.txt_video_game)) {
                                    windowHeight = context.resources.getInteger(R.integer.b_height)
                                    if (windowWidth < context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                            R.integer.b_width
                                        )
                                    ) {
                                        windowWidth =
                                            if (actualX == context.resources.getInteger(R.integer.a_start)) {
                                                context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                                    R.integer.b_width
                                                )
                                            } else {
                                                context.resources.getInteger(R.integer.b_width) + context.resources.getInteger(
                                                    R.integer.c_width
                                                )
                                            }
                                    }

                                    if (actualX >= context.resources.getInteger(R.integer.b_start)) {
                                        actualX = context.resources.getInteger(R.integer.b_start)
                                    }
                                }


                                // Chat with video call width restricted.
                                if (getDetailsModel().name == context.getString(R.string.txt_chat_video_call)) {
                                    windowHeight = context.resources.getInteger(R.integer.b_height)
                                    if (windowWidth < context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                            R.integer.b_width
                                        ) || screen == Screen.FULLSCREEN
                                    ) {
                                        windowWidth =
                                            if (actualX == context.resources.getInteger(R.integer.a_start)) {
                                                context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                                    R.integer.b_width
                                                )
                                            } else {
                                                context.resources.getInteger(R.integer.b_width) + context.resources.getInteger(
                                                    R.integer.c_width
                                                )
                                            }
                                    }

                                    if (actualX >= context.resources.getInteger(R.integer.b_start)) {
                                        actualX = context.resources.getInteger(R.integer.b_start)
                                    }
                                }


                                // Keyboard width restricted.
                                if (getDetailsModel().name == context.getString(R.string.key_board)) {
                                    windowHeight = MULTICAST_KEYBOARD_HEIGHT
                                    if (windowWidth < context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                            R.integer.b_width
                                        ) || screen == Screen.FULLSCREEN
                                    ) {
                                        windowWidth =
                                            if (actualX == context.resources.getInteger(R.integer.a_start)) {
                                                context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                                    R.integer.b_width
                                                )
                                            } else {
                                                context.resources.getInteger(R.integer.b_width) + context.resources.getInteger(
                                                    R.integer.c_width
                                                )
                                            }
                                    }

                                    if (actualX >= context.resources.getInteger(R.integer.b_start)) {
                                        actualX = context.resources.getInteger(R.integer.b_start)
                                    }
                                }









                                floatWindowLayoutParam.width = windowWidth.dpToPx(context).toInt()

                                if (!isMiniMize) {
                                    floatWindowLayoutParam.height =
                                        windowHeight.dpToPx(context).toInt()
                                }
                                binding.root.translationX = actualX.dpToPx(context)
                                binding.root.layoutParams = floatWindowLayoutParam


                                //floatWindowLayoutParam.y = (y + event.rawY - py).toInt()
                                if (screen != Screen.FULLSCREEN) {
                                    getDetailsModel().startX = actualX
                                    getDetailsModel().width = windowWidth
                                    getDetailsModel().height = windowHeight

                                }

                            }
                        }
                    }
                }

                return true
            }


        })

        var H = -1f
        var W = -1f
        imgResize.setOnTouchListener { _, motionEvent ->


            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    H = motionEvent.rawX
                    W = motionEvent.rawY


                }

                MotionEvent.ACTION_MOVE -> {

                    floatWindowLayoutParam.width =
                        floatWindowLayoutParam.width - (H - motionEvent.rawX).toInt()

                    floatWindowLayoutParam.height =
                        floatWindowLayoutParam.height - (W - motionEvent.rawY).toInt()
                    mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)

                    H = motionEvent.rawX
                    W = motionEvent.rawY
                }

                MotionEvent.ACTION_UP -> {
                    val startX = getDetailsModel().startX
                    if (isPortrait()) {

                        val height =
                            abs(floatWindowLayoutParam.height - (W - motionEvent.rawY)).pxToDp(
                                context
                            )

                        val dpHeight = height + startX

                        var actualHeight = MATCH_PARENT

                        if (dpHeight > context.resources.getInteger(R.integer.a_start) && dpHeight <= context.resources.getInteger(
                                R.integer.b_start
                            ).plus(2)
                        ) {
                            actualHeight = context.resources.getInteger(R.integer.a_height)
                        } else if (dpHeight > context.resources.getInteger(R.integer.b_start) && dpHeight <= context.resources.getInteger(
                                R.integer.c_start
                            ).plus(2)
                        ) {

                            actualHeight = if (startX == context.resources.getInteger(R.integer.a_start)) {
                                context.resources.getInteger(R.integer.a_height) + context.resources.getInteger(
                                    R.integer.b_height
                                )
                            } else {
                                context.resources.getInteger(
                                    R.integer.b_height
                                )
                            }

                        } else if (dpHeight >= context.resources.getInteger(R.integer.c_start)) {

                            when (startX) {
                                context.resources.getInteger(R.integer.a_start) -> {
                                    actualHeight =
                                        (context.resources.getInteger(R.integer.a_height) + context.resources.getInteger(
                                            R.integer.b_height
                                        ) + context.resources.getInteger(R.integer.c_height))
                                    screen = Screen.FULLSCREEN
                                }

                                context.resources.getInteger(R.integer.b_start) -> {
                                    actualHeight = context.resources.getInteger(
                                        R.integer.b_height
                                    ) + context.resources.getInteger(R.integer.c_height)
                                }

                                else -> {
                                    actualHeight =
                                        context.resources.getInteger(R.integer.c_height)
                                }
                            }
                        }

                        var actualWidth =
                            abs(floatWindowLayoutParam.width).toFloat().pxToDp(context)

                        if (actualWidth < MINIMIZE_WINDOW_HEIGHT) {
                            actualWidth = MINIMIZE_WINDOW_HEIGHT.toFloat()
                        }

                        floatWindowLayoutParam.width =
                            actualWidth.toInt().dpToPx(context).toInt()
                        floatWindowLayoutParam.height = actualHeight.dpToPx(context).toInt()

                        if (screen != Screen.FULLSCREEN) {
                            getDetailsModel().width = actualHeight
                        }

                    } else {
                        val actualWidthDp =
                            abs(floatWindowLayoutParam.width - (H - motionEvent.rawX)).pxToDp(
                                context
                            )

                        val dpWidth = actualWidthDp + startX

                        var actualWidth = MATCH_PARENT

                        if (dpWidth > context.resources.getInteger(R.integer.a_start) && dpWidth <= context.resources.getInteger(
                                R.integer.b_start
                            ).plus(2)
                        ) {
                            actualWidth = context.resources.getInteger(R.integer.a_width)
                        } else if (dpWidth > context.resources.getInteger(R.integer.b_start) && dpWidth <= context.resources.getInteger(
                                R.integer.c_start
                            ).plus(2)
                        ) {

                            actualWidth = if (startX == context.resources.getInteger(R.integer.a_start)) {
                                context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                    R.integer.b_width
                                )
                            } else {
                                context.resources.getInteger(R.integer.b_width)
                            }

                        } else if (dpWidth >= context.resources.getInteger(R.integer.c_start)) {
                            when (startX) {
                                context.resources.getInteger(R.integer.a_start) -> {
                                    actualWidth =
                                        (context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                            R.integer.b_width
                                        ) + context.resources.getInteger(R.integer.c_width))
                                    screen = Screen.FULLSCREEN
                                }

                                context.resources.getInteger(R.integer.b_start) -> {
                                    actualWidth = context.resources.getInteger(
                                        R.integer.b_width
                                    ) + context.resources.getInteger(R.integer.c_width)
                                }

                                else -> {
                                    actualWidth =
                                        context.resources.getInteger(R.integer.c_width)
                                }
                            }

                        }
                        floatWindowLayoutParam.width = actualWidth.dpToPx(context).toInt()

                        var actualHeight =
                            abs(floatWindowLayoutParam.height).toFloat().pxToDp(context)

                        if (actualHeight < MINIMIZE_WINDOW_HEIGHT) {
                            actualHeight = MINIMIZE_WINDOW_HEIGHT.toFloat()
                        }
                        floatWindowLayoutParam.height =
                            actualHeight.toInt().dpToPx(context).toInt()

                        if (screen != Screen.FULLSCREEN) {
                            getDetailsModel().width = actualWidth

                        }
                    }

                    mBinding.layoutMain.updateViewLayout(
                        binding.root, floatWindowLayoutParam
                    )

                    H = -1f
                    W = -1f
                }
            }
            return@setOnTouchListener true
        }


        imgResizeLeft
            .setOnTouchListener { _, motionEvent ->


                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        H = motionEvent.rawX
                        W = motionEvent.rawY


                    }

                    MotionEvent.ACTION_MOVE -> {

                        floatWindowLayoutParam.width =
                            (H - motionEvent.rawX + floatWindowLayoutParam.width).toInt()
                        floatWindowLayoutParam.height =
                            floatWindowLayoutParam.height - (W - motionEvent.rawY).toInt()
                        binding.root.translationX = motionEvent.rawX
                        mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)

                        H = motionEvent.rawX
                        W = motionEvent.rawY
                    }

                    MotionEvent.ACTION_UP -> {
                        val startX = getDetailsModel().startX
                        if (isPortrait()) {

                            val height =
                                abs(floatWindowLayoutParam.height - (W - motionEvent.rawY)).pxToDp(
                                    context
                                )

                            val dpHeight = height + startX

                            var actualHeight = MATCH_PARENT

                            if (dpHeight > context.resources.getInteger(R.integer.a_start) && dpHeight <= context.resources.getInteger(
                                    R.integer.b_start
                                ).plus(2)
                            ) {
                                actualHeight = context.resources.getInteger(R.integer.a_height)
                            } else if (dpHeight > context.resources.getInteger(R.integer.b_start) && dpHeight <= context.resources.getInteger(
                                    R.integer.c_start
                                ).plus(2)
                            ) {

                                actualHeight = if (startX == context.resources.getInteger(R.integer.a_start)) {
                                    context.resources.getInteger(R.integer.a_height) + context.resources.getInteger(
                                        R.integer.b_height
                                    )
                                } else {
                                    context.resources.getInteger(
                                        R.integer.b_height
                                    )
                                }

                            } else if (dpHeight >= context.resources.getInteger(R.integer.c_start)) {

                                when (startX) {
                                    context.resources.getInteger(R.integer.a_start) -> {
                                        actualHeight =
                                            (context.resources.getInteger(R.integer.a_height) + context.resources.getInteger(
                                                R.integer.b_height
                                            ) + context.resources.getInteger(R.integer.c_height))
                                        screen = Screen.FULLSCREEN
                                    }

                                    context.resources.getInteger(R.integer.b_start) -> {
                                        actualHeight = context.resources.getInteger(
                                            R.integer.b_height
                                        ) + context.resources.getInteger(R.integer.c_height)
                                    }

                                    else -> {
                                        actualHeight =
                                            context.resources.getInteger(R.integer.c_height)
                                    }
                                }
                            }

                            var actualWidth =
                                abs(floatWindowLayoutParam.width).toFloat().pxToDp(context)

                            if (actualWidth < MINIMIZE_WINDOW_HEIGHT) {
                                actualWidth = MINIMIZE_WINDOW_HEIGHT.toFloat()
                            }

                            floatWindowLayoutParam.width =
                                actualWidth.toInt().dpToPx(context).toInt()
                            floatWindowLayoutParam.height = actualHeight.dpToPx(context).toInt()

                            if (screen != Screen.FULLSCREEN) {
                                getDetailsModel().width = actualHeight
                            }

                        } else {

                            val dpWidth = binding.root.translationX
                            val actualStartX = startX + getDetailsModel().width

                            var actualWidth = MATCH_PARENT


                            var actualX = context.resources.getInteger(R.integer.a_start)

                            if (dpWidth >= context.resources.getInteger(R.integer.a_start) && dpWidth <= context.resources.getInteger(
                                    R.integer.b_start
                                ).minus(2)
                            ) {
                                actualX = context.resources.getInteger(R.integer.a_start)

                                actualWidth =
                                    if (actualStartX <= context.resources.getInteger(R.integer.b_start).plus(2)) {
                                        context.resources.getInteger(R.integer.a_width)
                                    } else if (actualStartX <= context.resources.getInteger(R.integer.c_start).plus(2)) {
                                        context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                            R.integer.b_width
                                        )
                                    } else {
                                        screen = Screen.FULLSCREEN
                                        context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(
                                            R.integer.b_width
                                        ) + context.resources.getInteger(R.integer.c_width)

                                    }

                            } else if (dpWidth > context.resources.getInteger(R.integer.b_start)
                                    .minus(2) && dpWidth <= context.resources.getInteger(
                                    R.integer.c_start
                                ).minus(2)
                            ) {
                                actualX = context.resources.getInteger(R.integer.b_start)
                                actualWidth =
                                    if (actualStartX <= context.resources.getInteger(R.integer.c_start).plus(2)) {
                                        context.resources.getInteger(R.integer.b_width)
                                    } else {
                                        context.resources.getInteger(
                                            R.integer.b_width
                                        ) + context.resources.getInteger(R.integer.c_width)
                                    }


                            } else if (dpWidth >= context.resources.getInteger(R.integer.c_start).minus(2)) {

                                actualX = context.resources.getInteger(R.integer.c_start)
                                actualWidth = context.resources.getInteger(R.integer.c_width)

                            }

                            floatWindowLayoutParam.width = actualWidth.dpToPx(context).toInt()
                            binding.root.translationX = actualX.dpToPx(context)
                            getDetailsModel().startX = actualX

                            var actualHeight =
                                abs(floatWindowLayoutParam.height).toFloat().pxToDp(context)

                            if (actualHeight < MINIMIZE_WINDOW_HEIGHT) {
                                actualHeight = MINIMIZE_WINDOW_HEIGHT.toFloat()
                            }
                            floatWindowLayoutParam.height =
                                actualHeight.toInt().dpToPx(context).toInt()

                            if (screen != Screen.FULLSCREEN) {
                                getDetailsModel().width = actualWidth
                            }
                        }

                        mBinding.layoutMain.updateViewLayout(
                            binding.root, floatWindowLayoutParam
                        )

                        H = -1f
                        W = -1f
                    }
                }
                return@setOnTouchListener true
            }


        /*  toolbar.setOnMenuItemClickListener {
              when(it.itemId){
                  R.id.action_close ->{
                      close()
                      return@setOnMenuItemClickListener true
                  }
                  else ->{
                      return@setOnMenuItemClickListener false
                  }
              }
          }*/


    }

    open fun close() {
        mBinding.layoutMain.removeView(binding.root)
        configurationLiveData?.removeObserver(configObserver)
    }

    private fun fullScreenMode() {
        screen = Screen.FULLSCREEN

        if (isPortrait()) {

            floatWindowLayoutParam.height =
                (context.resources.getInteger(R.integer.a_height) + context.resources.getInteger(R.integer.b_height) + context.resources.getInteger(
                    R.integer.c_height
                )).dpToPx(context).toInt()

            if (!isMiniMize) {

                floatWindowLayoutParam.width =
                    context.resources.getInteger(R.integer.a_width).dpToPx(context).toInt()
            }

            binding.root.translationY = 0f


        } else {
            floatWindowLayoutParam.width =
                (context.resources.getInteger(R.integer.a_width) + context.resources.getInteger(R.integer.b_width) + context.resources.getInteger(
                    R.integer.c_width
                )).dpToPx(context).toInt()

            if (!isMiniMize) {
                if (getDetailsModel().name != context.getString(R.string.txt_video_game)) {
                    floatWindowLayoutParam.height =
                        context.resources.getInteger(R.integer.a_height).dpToPx(context).toInt()
                }
            }
            binding.root.translationX = 0f
        }


        mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)
        binding.root.bringToFront()
    }

    private fun minimizeMode() {
        isMiniMize = true
        floatWindowLayoutParam.height = MINIMIZE_WINDOW_HEIGHT.dpToPx(context).toInt()
        mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)

    }

    private fun maximizeMode() {
        isMiniMize = false
        floatWindowLayoutParam.height = getDetailsModel().height.dpToPx(context).toInt()
        if (isPortrait()) {
            binding.root.translationX = startY.dpToPx(context)
        } else {
            binding.root.translationY = startY.dpToPx(context)

        }
        mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)
    }

    private fun defaultMode() {
        screen = Screen.DEFAULT


        floatWindowLayoutParam.width = getDetailsModel().width.dpToPx(context).toInt()
        if (!isMiniMize) {
            floatWindowLayoutParam.height = getDetailsModel().height.dpToPx(context).toInt()
        }
        if (isPortrait()) {
            binding.root.translationY = getDetailsModel().startX.dpToPx(context)
        } else {
            binding.root.translationX = getDetailsModel().startX.dpToPx(context)
        }
        mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)
    }

    /*private fun enableKeyboard() {
        if (floatWindowLayoutParam.flags and LayoutParams.FLAG_NOT_FOCUSABLE != 0) {
            floatWindowLayoutParam.flags =
                floatWindowLayoutParam.flags and LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)
        }
    }*/

    /* private fun disableKeyboard() {
         if (floatWindowLayoutParam.flags and LayoutParams.FLAG_NOT_FOCUSABLE == 0) {
             floatWindowLayoutParam.flags =
                 floatWindowLayoutParam.flags or LayoutParams.FLAG_NOT_FOCUSABLE
             mBinding.layoutMain.updateViewLayout(binding.root, floatWindowLayoutParam)
         }
     }*/


    private fun checkBlockA(): Boolean {
        return appList().any {
            it.name != getDetailsModel().name && it.startX < context.resources.getInteger(R.integer.b_start)
        }
    }

    private fun checkBlockB(): Boolean {
        return appList().any {
            it.name != getDetailsModel().name && it.startX < context.resources.getInteger(R.integer.c_start) && it.startX + it.width > context.resources.getInteger(
                R.integer.a_width
            )
        }
    }

    private fun checkBlockC(): Boolean {
        return appList().any {
            it.name != getDetailsModel().name && it.startX + it.width > (context.resources.getInteger(
                R.integer.c_start
            ) + 2)
        }
    }

    private fun hideAppBar() {
        appBarHandler.postDelayed({
            appBar.visibility = GONE
            imgResize.visibility = GONE
            imgResizeLeft.visibility = GONE
        }, 3000)
    }

    private fun startXApp(startX: Int) {
        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.root.translationX = startX.dpToPx(context)
        } else {
            binding.root.translationY = startX.dpToPx(context)
        }
    }

    private fun startYApp(startY: Int) {
        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.root.translationY = startY.dpToPx(context)
        } else {
            binding.root.translationX = startY.dpToPx(context)
        }
    }


    private fun getAppWidth(width: Int): Int {
        return if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            width
        } else {
            MATCH_PARENT
        }
    }

    fun isPortrait(): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }
}