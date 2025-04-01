package com.android.imagic.files.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import me.zhanghai.android.foregroundcompat.ForegroundImageButton

class FloatingButtonView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ForegroundImageButton(context, attrs, defStyleAttr), View.OnTouchListener {

    private var lastX = 0f
    private var lastY = 0f
    private var touchX = 0f
    private var touchY = 0f
    private var isDragging = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        post {
            val parent = parent as? View
            if (parent != null) {
                translationX = (parent.width - width - 48).toFloat()
                translationY = (parent.height - height - 200).toFloat()
            }
        }

        setOnTouchListener(this)
    }

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = translationX
                lastY = translationY
                touchX = event.rawX
                touchY = event.rawY
                isDragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - touchX
                val dy = event.rawY - touchY

                if (!isDragging && (dx * dx + dy * dy > touchSlop * touchSlop)) {
                    isDragging = true
                }

                if (isDragging) {
                    val parent = parent as? View
                    if (parent != null) {
                        translationX = (lastX + dx).coerceIn(0f, (parent.width - width).toFloat())
                        translationY = (lastY + dy).coerceIn(0f, (parent.height - height).toFloat())
                    } else {
                        translationX = lastX + dx
                        translationY = lastY + dy
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging) {
                    performClick()
                }
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return true
    }
}
