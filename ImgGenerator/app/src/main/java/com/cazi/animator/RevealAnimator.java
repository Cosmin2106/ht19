package com.cazi.animator;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Display;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.cazi.imggenerator.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class RevealAnimator {

//    0         1/3  1/2   2/3  5/6    1
//    |----------|----|-----|----|-----|  TOTAL (fab + fabMask)
//
//                    |-----|----|        ALPHA (fab)
//
//                          |----------|  DIMENS (fabMask)


    public static int ANIMATION_DURATION = 400;
    public static final int THEME_GREY = 1;

    private int maskId;

    private boolean isFab = true;
    private boolean noAnimation = true;
    private Context context;

    private ImageView fabMask;

    private Point fabCoord = new Point();
    private Point toolbarCoord = new Point();
    private Point fabDim = new Point();
    private Point toolbarDim = new Point();
    private int paddingV26;

    private FloatingActionButton fab;
    private RelativeLayout toolbar;

    private OnEndTransform listener;
    public interface OnEndTransform {
        void onEnd(boolean isFab);
    }

    //TODO: padding animatie


    public RevealAnimator(Context context, FloatingActionButton fab, RelativeLayout toolbar, int theme) {
        this.context = context;
        this.fab = fab;
        this.toolbar = toolbar;
        maskId = R.drawable.fab_mask;
        this.fab.setRippleColor(theme == THEME_GREY ? Color.BLACK : Color.WHITE);
        init();
    }

    @SuppressWarnings("deprecation")
    private void init() {
        fab.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                initWidthPadding();
                int width = fab.getMeasuredWidth();
                int height = fab.getMeasuredHeight();
                fabDim.set(width, height);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    fab.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                else {
                    fab.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }

                int[] pos = new int[2];
                fab.getLocationOnScreen(pos);
                fabCoord.set(pos[0], pos[1]);

                fabMask = new Mask(context, fabDim.x, fabDim.y);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    fabMask.setBackground(context.getResources().getDrawable(maskId));
                }
                else {
                    fabMask.setBackgroundDrawable(context.getResources().getDrawable(maskId));
                }
                toolbar.addView(fabMask);
                fabMask.setVisibility(View.GONE);
            }
        });

        toolbar.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int width = toolbar.getMeasuredWidth();
                int height = toolbar.getMeasuredHeight();
                toolbarDim.set(width, height);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    toolbar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                else {
                    toolbar.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
                int[] pos = new int[2];
                toolbar.getLocationOnScreen(pos);
                toolbarCoord.set(pos[0], pos[1]);
            }
        });

        TransitionDrawable fabDrawable;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fabDrawable = new TransitionDrawable(new Drawable[]{fab.getDrawable(), context.getResources().getDrawable(R.drawable.empty, context.getTheme())});
        }
        else {
            fabDrawable = new TransitionDrawable(new Drawable[]{fab.getDrawable(), context.getResources().getDrawable(R.drawable.empty)});
        }
        fab.setImageDrawable(fabDrawable);
        fabDrawable.setCrossFadeEnabled(true);
    }

    // padding API 26+
    private void initWidthPadding() {
        paddingV26 = 0;
        if (Build.VERSION.SDK_INT >= 26 && context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Resources res = context.getResources();
            int navBarId = res.getIdentifier("navigation_bar_height", "dimen", "android");
            if (navBarId > 0) {
                paddingV26 = res.getDimensionPixelSize(navBarId);
            }
        }
    }

    public boolean isFab() {
        return this.isFab;
    }

    public void setFab(boolean is) {
        this.isFab = is;
    }

    public void showToolbarAnimation() {
        if (!noAnimation) {
            return;
        }
        noAnimation = false;
        isFab = false;

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int displayWidth = display.getWidth();

        // setare masca
        fabMask.setVisibility(View.VISIBLE);
        fabMask.setX(fabCoord.x - paddingV26);
        fabMask.setY(fabCoord.y - toolbarCoord.y);

        List<Animator> animators = new ArrayList<>();

        // animatie MISCARE
        ObjectAnimator xTranslate = ObjectAnimator.ofFloat(fab, "translationX", 0, displayWidth - (fabCoord.x - paddingV26 + fabDim.x / 2) - (toolbarCoord.x - paddingV26 + toolbarDim.x / 2));
        ObjectAnimator yTranslate = ObjectAnimator.ofFloat(fab, "translationY", 0, toolbarCoord.y + toolbarDim.y / 2 - (fabCoord.y + fabDim.y / 2));
        ObjectAnimator xMaskTranslate = ObjectAnimator.ofFloat(fabMask, "translationX", fabCoord.x - paddingV26, (toolbarCoord.x + toolbarDim.x) / 2 - fabDim.x / 2 - paddingV26 / 2);
        ObjectAnimator yMaskTranslate = ObjectAnimator.ofFloat(fabMask, "translationY", fabCoord.y - toolbarCoord.y, toolbarDim.y / 2 - fabDim.y / 2);

        xTranslate.setDuration(ANIMATION_DURATION);
        yTranslate.setDuration(ANIMATION_DURATION);
        xMaskTranslate.setDuration(ANIMATION_DURATION);
        yMaskTranslate.setDuration(ANIMATION_DURATION);

        xTranslate.setInterpolator(new AccelerateInterpolator());
        yTranslate.setInterpolator(new DecelerateInterpolator());
        xMaskTranslate.setInterpolator(new AccelerateInterpolator());
        yMaskTranslate.setInterpolator(new DecelerateInterpolator());

        animators.add(xTranslate);
        animators.add(yTranslate);
        animators.add(xMaskTranslate);
        animators.add(yMaskTranslate);

        // animatie ALPHA
        ObjectAnimator alpha = ObjectAnimator.ofFloat(fab, "alpha", 1, 0);
        alpha.setStartDelay(ANIMATION_DURATION / 2);
        alpha.setDuration(ANIMATION_DURATION / 3);
        alpha.setInterpolator(new AccelerateInterpolator());

        animators.add(alpha);

        // animatie DIMENSIUNE
        float startRadius = fabDim.x;
        float endRadius = (float) Math.sqrt(Math.pow(toolbarDim.x, 2) + Math.pow(toolbarDim.y, 2));

        ValueAnimator xyScale = ValueAnimator.ofFloat(startRadius, endRadius);
        xyScale.setStartDelay(ANIMATION_DURATION / 3 * 2);
        xyScale.setDuration(ANIMATION_DURATION / 3);
        xyScale.setInterpolator(new AccelerateDecelerateInterpolator());
        xyScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float floatVal = (Float) animation.getAnimatedValue();
                fabMask.setScaleX(floatVal / fabDim.x);
                fabMask.setScaleY(floatVal / fabDim.x);
            }
        });

        animators.add(xyScale);

        // RULARE
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animators);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                noAnimation = true;
                listener.onEnd(false);
            }
        });
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                animatorSet.start();
            }
        });
    }

    public void hideToolbarAnimation() {
        if (!noAnimation) {
            return;
        }
        isFab = true;
        noAnimation = false;

        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        int displayWidth = display.getWidth();

        // setare masca
        fabMask.setX((toolbarCoord.x - paddingV26 + toolbarDim.x) / 2 - fabDim.x / 2);
        fabMask.setY(toolbarDim.y / 2 - fabDim.y / 2);

        List<Animator> animators = new ArrayList<>();

        // animatie MISCARE
        ObjectAnimator xTranslate = ObjectAnimator.ofFloat(fab, "translationX", displayWidth - (fabCoord.x - paddingV26 + fabDim.x / 2) - (toolbarCoord.x - paddingV26 + toolbarDim.x / 2), 0);
        ObjectAnimator yTranslate = ObjectAnimator.ofFloat(fab, "translationY", toolbarCoord.y + toolbarDim.y / 2 - (fabCoord.y + fabDim.y / 2), 0);
        ObjectAnimator xMaskTranslate = ObjectAnimator.ofFloat(fabMask, "translationX", (toolbarCoord.x + toolbarDim.x) / 2 - fabDim.x / 2 - paddingV26 / 2, fabCoord.x - paddingV26);
        ObjectAnimator yMaskTranslate = ObjectAnimator.ofFloat(fabMask, "translationY", toolbarDim.y / 2 - fabDim.y / 2, fabCoord.y - toolbarCoord.y);

        xTranslate.setDuration(ANIMATION_DURATION);
        yTranslate.setDuration(ANIMATION_DURATION);
        xMaskTranslate.setDuration(ANIMATION_DURATION);
        yMaskTranslate.setDuration(ANIMATION_DURATION);

        xTranslate.setInterpolator(new DecelerateInterpolator());
        yTranslate.setInterpolator(new AccelerateInterpolator());
        xMaskTranslate.setInterpolator(new DecelerateInterpolator());
        yMaskTranslate.setInterpolator(new AccelerateInterpolator());

        animators.add(xTranslate);
        animators.add(yTranslate);
        animators.add(xMaskTranslate);
        animators.add(yMaskTranslate);

        // animatie ALPHA
        ObjectAnimator alpha = ObjectAnimator.ofFloat(fab, "alpha", 0, 1);
        alpha.setStartDelay(ANIMATION_DURATION / 6);
        alpha.setDuration(ANIMATION_DURATION / 3);
        alpha.setInterpolator(new AccelerateInterpolator());

        animators.add(alpha);

        // animatie DIMENSIUNE
        float startRadius = (float) Math.sqrt(Math.pow(toolbarDim.x, 2) + Math.pow(toolbarDim.y, 2));
        float endRadius = fabDim.x;

        ValueAnimator xyScale = ValueAnimator.ofFloat(startRadius, endRadius);
        xyScale.setDuration(ANIMATION_DURATION / 3);
        xyScale.setInterpolator(new AccelerateDecelerateInterpolator());
        xyScale.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float floatVal = (Float) animation.getAnimatedValue();
                fabMask.setScaleX(floatVal / fabDim.x);
                fabMask.setScaleY(floatVal / fabDim.x);
            }
        });

        animators.add(xyScale);

        // RULARE
        final AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animators);
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                noAnimation = true;
                listener.onEnd(true);
            }
        });
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                animatorSet.start();
            }
        });
    }

    public void setOnEndTransform(OnEndTransform l) {
        this.listener = l;
    }
}
