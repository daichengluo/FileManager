package com.android.imagic.files.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.android.imagic.files.R
import com.android.imagic.files.util.shortAnimTime

class CrossfadeSubtitleToolbar : Toolbar {
    private val subtitleAnimator = ObjectAnimator.ofFloat(null, View.ALPHA, 1f, 0f, 1f).apply {
        duration = (2 * context.shortAnimTime).toLong()
        interpolator = FastOutSlowInInterpolator()
        val listener = AnimatorListener()
        addUpdateListener(listener)
        addListener(listener)
    }

    init {
        overflowIcon = ContextCompat.getDrawable(context, R.drawable.menu_icon)
    }

    private var nextSubtitle: CharSequence? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    )

    override fun getSubtitle(): CharSequence? = nextSubtitle ?: super.getSubtitle()

    override fun setSubtitle(subtitle: CharSequence?) {
        if (getSubtitle() == subtitle) {
            return
        }
        nextSubtitle = subtitle
        ensureSubtitleAnimatorTarget()
        if (subtitleAnimator.target == null) {
            // Subtitle text view not available (yet), just delegate to super.
            super.setSubtitle(subtitle)
            return
        }
        if (!subtitleAnimator.isRunning) {
            subtitleAnimator.start()
        }
    }

    fun setNavButtonEnable(enable: Boolean) {
        val navButton = try {
            Toolbar::class.java.getDeclaredField("mNavButtonView")
                .apply { isAccessible = true }
                .get(this) as ImageButton?
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return

        navButton.isEnabled = enable
        if (enable) {
            navButton.alpha = 1.0f
            val context = this.context
            val defaultDescription = context.getString(R.string.file_list_action_navigate_up)
            navigationContentDescription = defaultDescription
        } else {
            navButton.alpha = 0.5f
            navigationContentDescription = null
        }
    }

    private fun ensureSubtitleAnimatorTarget() {
        if (subtitleAnimator.target != null) {
            return
        }
        val subtitleTextView = try {
            Toolbar::class.java.getDeclaredField("mSubtitleTextView")
                .apply { isAccessible = true }
                .get(this) as TextView?
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return
        subtitleTextView.visibility = View.GONE
        subtitleAnimator.target = subtitleTextView
    }

    private inner class AnimatorListener : AnimatorListenerAdapter(), AnimatorUpdateListener {
        private var isTextUpdated = false

        override fun onAnimationUpdate(animator: ValueAnimator) {
            if (animator.animatedFraction < 0.5) {
                isTextUpdated = false
            } else {
                ensureTextUpdated()
            }
        }

        override fun onAnimationEnd(animator: Animator) {
            ensureTextUpdated()
            if (nextSubtitle != null) {
                isTextUpdated = false
                animator.start()
            }
        }

        private fun ensureTextUpdated() {
            if (!isTextUpdated) {
                if (nextSubtitle != null) {
                    super@CrossfadeSubtitleToolbar.setSubtitle(nextSubtitle)
                    nextSubtitle = null
                }
                isTextUpdated = true
            }
        }
    }
}
