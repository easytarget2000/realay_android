package org.eztarget.realay.ui.utils;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.animation.ScaleAnimation;

import org.eztarget.realay.R;

/**
 * Created by michel on 07/02/15.
 */
public class ViewAnimator {

    private static final String TAG = ViewAnimator.class.getSimpleName();

    public interface Callback {
        public void onAnimationEnd();
    }

    public static void wiggleIt(final View view) {
        if (view == null) return;

        final RotateAnimation rotation = new RotateAnimation(
                -30f,
                30f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
        );
        rotation.setInterpolator(new LinearInterpolator());
        rotation.setRepeatCount(4);
        rotation.setRepeatMode(Animation.REVERSE);
        rotation.setDuration(60L);

        view.startAnimation(rotation);
    }

    public static void wiggleIt(final View view, final Callback callback) {
        if (view == null) return;

        final RotateAnimation rotation = new RotateAnimation(
                -20f,
                20f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
        );
        rotation.setInterpolator(new LinearInterpolator());
        rotation.setRepeatCount(1);
        rotation.setDuration(100L);

        if (callback != null) {
            rotation.setAnimationListener(
                    new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            callback.onAnimationEnd();
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    }
            );
        }

        view.startAnimation(rotation);
    }

    public static void fadeView(final View view, final boolean doFadeIn) {
        if (view == null) return;

        if (doFadeIn && view.getVisibility() != View.VISIBLE) {
            final Animation fadeInAnimation;
            fadeInAnimation = AnimationUtils.loadAnimation(view.getContext(), R.anim.alpha_fade_in);
            fadeInAnimation.setAnimationListener(
                    new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            view.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            view.setClickable(true);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    }
            );
            view.startAnimation(fadeInAnimation);
        } else if (!doFadeIn && view.getVisibility() == View.VISIBLE) {

            final Animation fadeOut;
            fadeOut = AnimationUtils.loadAnimation(view.getContext(), R.anim.fade_out);
            fadeOut.setAnimationListener(
                    new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            view.setClickable(false);
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            view.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    }
            );
            view.startAnimation(fadeOut);
        }
    }

    private static final long ANIMATION_TIME_FAST_SCALE = 600L;

    private static final long ANIMATION_TIME_SLOW_SCALE = 2000L;

    private static final float POUND_SCALE = 1.2f;

    public static void slowPulse(final View view) {
        if (view == null) return;

        final float fromX;
        final float fromY;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            fromX = 1f;
            fromY = 1f;
        } else {
            fromX = view.getScaleX();
            fromY = view.getScaleY();
        }

        final ScaleAnimation firstPulse = new ScaleAnimation(
                fromX,
                POUND_SCALE,
                fromY,
                POUND_SCALE,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
        );

        firstPulse.setDuration(ANIMATION_TIME_SLOW_SCALE);
        firstPulse.setAnimationListener(
                new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        final ScaleAnimation secondPulse = new ScaleAnimation(
                                POUND_SCALE,
                                1.0f,
                                POUND_SCALE,
                                1.0f,
                                Animation.RELATIVE_TO_SELF,
                                0.5f,
                                Animation.RELATIVE_TO_SELF,
                                0.5f
                        );
                        secondPulse.setDuration(ANIMATION_TIME_SLOW_SCALE);
                        view.startAnimation(secondPulse);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                }
        );
        view.startAnimation(firstPulse);
    }

    public static void scaleAnimateView(
            final View view,
            final boolean doAppear
    ) {
        scaleAnimateView(
                view,
                doAppear,
                true,
                ANIMATION_TIME_FAST_SCALE,
                0.5f,
                0.5f,
                null
        );
    }

    public static void scaleAnimateView(
            final View view,
            final boolean doAppear,
            final boolean doUseScale,
            final float pivotXValue,
            final float pivotYValue,
            final Callback callback
    ) {
        scaleAnimateView(
                view,
                doAppear,
                doUseScale,
                ANIMATION_TIME_FAST_SCALE,
                pivotXValue,
                pivotYValue,
                callback
        );
    }

    public static void scaleAnimateView(
            final View view,
            final boolean doAppear,
            final boolean doUseScale,
            final long scaleAnimationTime,
            final float pivotXValue,
            final float pivotYValue,
            final Callback callback
    ) {
        if (view == null || (view.getVisibility() == View.VISIBLE) == doAppear) {
            if (callback != null) callback.onAnimationEnd();
            return;
        }

        final float fromX;
        final float fromY;
        if (!doUseScale || (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB)) {
            fromX = doAppear ? 0.1f : 1f;
            fromY = doAppear ? 0.1f : 1f;
        } else {
            fromX = view.getScaleX();
            fromY = view.getScaleY();
        }

        final ScaleAnimation poundInAnimation = new ScaleAnimation(
                fromX,
                POUND_SCALE,
                fromY,
                POUND_SCALE,
                Animation.RELATIVE_TO_SELF,
                pivotXValue,
                Animation.RELATIVE_TO_SELF,
                pivotYValue
        );
        poundInAnimation.setDuration(scaleAnimationTime / 3L);

        // The Pound Animation starts another Animation at the end (onAnimationEnd)
        // which leaves the View in the final, settled, state
        // VISIBLE or GONE with a scale of 1 or 0.
        poundInAnimation.setAnimationListener(
                new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setClickable(doAppear);
                        final ScaleAnimation settleAnimation = new ScaleAnimation(
                                POUND_SCALE,
                                doAppear ? 1f : 0f,
                                POUND_SCALE,
                                doAppear ? 1f : 0f,
                                Animation.RELATIVE_TO_SELF,
                                pivotXValue,
                                Animation.RELATIVE_TO_SELF,
                                pivotYValue
                        );
                        settleAnimation.setDuration(scaleAnimationTime / 2L);

                        // Second "level" of AnimationListeners,
                        // used in order to hide the collapsing View onAnimationEnd.
                        settleAnimation.setAnimationListener(
                                new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        if (!doAppear) view.setVisibility(View.GONE);
                                        if (callback != null) callback.onAnimationEnd();
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {
                                    }
                                }
                        );

                        view.startAnimation(settleAnimation);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                }
        );

        if (doAppear) view.setVisibility(View.VISIBLE);
        view.startAnimation(poundInAnimation);
    }

    public static void scaleAnimateViewFillScreen(
            final View scaleView,
            final boolean doAppear,
            final View fillView,
            final Callback callback
    ) {
        if (scaleView == null || fillView == null) {
            if (callback != null) {
                callback.onAnimationEnd();
            }
            return;
        }

        if (doAppear == (fillView.getVisibility() == View.VISIBLE)) {
            if (callback != null) {
                callback.onAnimationEnd();
            }
            return;
        }

        final float startScale = doAppear ? 1f : 20f;
        final float settleScale = doAppear ? 20f : 1f;

        scaleView.setVisibility(View.VISIBLE);

        final ScaleAnimation animation = new ScaleAnimation(
                startScale,
                settleScale,
                startScale,
                settleScale,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f
        );
        animation.setDuration(ANIMATION_TIME_FAST_SCALE);

        // The Pound Animation starts another Animation at the end (onAnimationEnd)
        // which leaves the View in the final, settled, state
        // VISIBLE or GONE with a scale of 1 or 0.
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (!doAppear) fillView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                fillView.setClickable(doAppear);

                scaleView.setVisibility(View.GONE);

                if (doAppear) fillView.setVisibility(View.VISIBLE);
                if (callback != null) callback.onAnimationEnd();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        scaleView.startAnimation(animation);
    }

    public static void quickFade(final View view) {
        quickFade(view, null);
    }

    public static void quickFade(final View view, final Callback callback) {
        if (view == null) return;
        Context context = view.getContext();

        final Animation fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in_fast);
        if (callback != null) {
            fadeIn.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    callback.onAnimationEnd();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }

        final Animation fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out_fast);
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.startAnimation(fadeIn);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(fadeOut);
    }

    public static void disappearRight(
            final View view,
            final boolean doFadeBackIn,
            final Callback callback
    ) {
        if (view == null) return;

        final Animation animation;
        animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.set_disappear_to_right);
        animation.setAnimationListener(
                new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        if (callback != null) callback.onAnimationEnd();

                        if (doFadeBackIn) fadeView(view, true);
                        else view.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                }
        );

        view.startAnimation(animation);
    }

    public static void appearUp(
            final View view,
            final Callback callback
    ) {
        if (view == null || view.getVisibility() == View.VISIBLE) {
            if (callback != null) callback.onAnimationEnd();
            return;
        }

        final Animation animation;
        animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.set_appear_up);
        animation.setAnimationListener(
                new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        view.setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setClickable(true);
                        if (callback != null) callback.onAnimationEnd();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                }
        );

        view.startAnimation(animation);
    }

    public static void disappearDown(
            final View view,
            final Callback callback
    ) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            if (callback != null) callback.onAnimationEnd();
            return;
        }

        final Animation animation;
        animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.set_disappear_down);
        animation.setAnimationListener(
                new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        ViewAnimator.fadeView(view, false);
                        if (callback != null) callback.onAnimationEnd();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                }
        );

        view.startAnimation(animation);
    }
}
