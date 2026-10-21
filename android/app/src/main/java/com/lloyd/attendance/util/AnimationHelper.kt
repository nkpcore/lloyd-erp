package com.lloyd.attendance.util

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.widget.TextView
import android.view.animation.OvershootInterpolator
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import java.util.Locale

object AnimationHelper {

    /** Tactile spring press animation on cards and chips. */
    fun animateCardPress(v: View?) {
        v ?: return
        v.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(90)
            .setInterpolator(FastOutSlowInInterpolator())
            .withEndAction {
                v.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(180)
                    .setInterpolator(OvershootInterpolator(1.3f))
                    .start()
            }
            .start()
    }

    /** Rolling number animation for percentage text (e.g. 60.8% → 64.2%). */
    fun animateRollingPercentage(tv: TextView?, from: Float, to: Float, duration: Long) {
        tv ?: return
        ValueAnimator.ofFloat(from, to).apply {
            this.duration = if (duration > 0) duration else 400
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animation ->
                val value = animation.animatedValue as Float
                tv.text = String.format(Locale.US, "%.1f%%", value)
            }
            start()
        }
    }

    /** Rolling integer animation for counts (e.g. 0 → 45). */
    fun animateRollingInt(tv: TextView?, from: Int, to: Int, duration: Long) {
        tv ?: return
        ValueAnimator.ofInt(from, to).apply {
            this.duration = if (duration > 0) duration else 350
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { animation ->
                tv.text = (animation.animatedValue as Int).toString()
            }
            start()
        }
    }

    /** Smooth 220ms cross-fade between views. */
    fun animateCrossFade(fromView: View?, toView: View?) {
        if (fromView != null && fromView.visibility == View.VISIBLE) {
            fromView.animate()
                .alpha(0f)
                .setDuration(120)
                .withEndAction {
                    fromView.visibility = View.GONE
                    toView?.let {
                        it.alpha = 0f
                        it.visibility = View.VISIBLE
                        it.animate().alpha(1f).setDuration(180).start()
                    }
                }
                .start()
        } else {
            toView?.let {
                it.alpha = 0f
                it.visibility = View.VISIBLE
                it.animate().alpha(1f).setDuration(180).start()
            }
        }
    }

    /** Gentle breathing pulse animation on live status dot. */
    fun startBreathingAnimation(dotView: View?): ValueAnimator? {
        dotView ?: return null
        return ObjectAnimator.ofFloat(dotView, "alpha", 0.35f, 1.0f).apply {
            duration = 750
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = FastOutSlowInInterpolator()
            start()
        }
    }
}
