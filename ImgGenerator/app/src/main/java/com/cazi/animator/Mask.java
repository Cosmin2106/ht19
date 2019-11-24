package com.cazi.animator;


import android.content.Context;
import android.widget.ImageView;

public class Mask extends ImageView {

    private int width;
    private int height;

    public Mask(Context context, int width, int height) {
        super(context);
        this.width = width;
        this.height = height;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }
}
