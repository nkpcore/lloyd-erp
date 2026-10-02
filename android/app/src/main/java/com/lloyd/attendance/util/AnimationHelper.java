package com.lloyd.attendance.util;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import java.util.Locale;

public class AnimationHelper {

    /**
     * Tactile spring press animation on cards and chips.
     */
    public static void animateCardPress(View v) {
        if (v == null) return;
        v.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(90)
                .setInterpolator(new FastOutSlowInInterpolator())
                .withEndAction(() -> {
                    v.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(180)
                            .setInterpolator(new OvershootInterpolator(1.3f))
                            .start();
                })
                .start();
    }

    /**
     * Rolling number animation for percentage text (e.g. 60.8% -> 64.2%).
     */
    public static void animateRollingPercentage(TextView tv, float from, float to, long duration) {
        if (tv == null) return;
        ValueAnimator animator = ValueAnimator.ofFloat(from, to);
        animator.setDuration(duration > 0 ? duration : 400);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            tv.setText(String.format(Locale.US, "%.1f%%", val));
        });
        animator.start();
    }

    /**
     * Rolling integer animation for counts (e.g. 0 -> 45).
     */
    public static void animateRollingInt(TextView tv, int from, int to, long duration) {
        if (tv == null) return;
        ValueAnimator animator = ValueAnimator.ofInt(from, to);
        animator.setDuration(duration > 0 ? duration : 350);
        animator.setInterpolator(new FastOutSlowInInterpolator());
        animator.addUpdateListener(animation -> {
            int val = (int) animation.getAnimatedValue();
            tv.setText(String.valueOf(val));
        });
        animator.start();
    }

    /**
     * Smooth 220ms cross-fade between views.
     */
    public static void animateCrossFade(View fromView, View toView) {
        if (fromView != null && fromView.getVisibility() == View.VISIBLE) {
            fromView.animate()
                    .alpha(0f)
                    .setDuration(120)
                    .withEndAction(() -> {
                        fromView.setVisibility(View.GONE);
                        if (toView != null) {
                            toView.setAlpha(0f);
                            toView.setVisibility(View.VISIBLE);
                            toView.animate()
                                    .alpha(1f)
                                    .setDuration(180)
                                    .start();
                        }
                    })
                    .start();
        } else if (toView != null) {
            toView.setAlpha(0f);
            toView.setVisibility(View.VISIBLE);
            toView.animate()
                    .alpha(1f)
                    .setDuration(180)
                    .start();
        }
    }

    /**
     * Gentle breathing pulse animation on live status dot.
     */
    public static ValueAnimator startBreathingAnimation(View dotView) {
        if (dotView == null) return null;
        ObjectAnimator pulse = ObjectAnimator.ofFloat(dotView, "alpha", 0.35f, 1.0f);
        pulse.setDuration(750);
        pulse.setRepeatCount(ValueAnimator.INFINITE);
        pulse.setRepeatMode(ValueAnimator.REVERSE);
        pulse.setInterpolator(new FastOutSlowInInterpolator());
        pulse.start();
        return pulse;
    }
}
