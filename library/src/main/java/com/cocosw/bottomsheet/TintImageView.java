package com.cocosw.bottomsheet;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.AttributeSet;
import android.widget.ImageView;

public class TintImageView extends ImageView {

    public static final int[][] EMPTY = new int[][]{new int[0]};
    private ColorStateList mColorStateList;

    public TintImageView(Context context) {
        this(context, null);
    }

    public TintImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TintImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TintImageView, defStyleAttr, 0);
        mColorStateList = a.getColorStateList(R.styleable.TintImageView_bsTintColor);

        a.recycle();
    }

    private void updateTintColor() {
        final int[] drawableStates = getDrawableState();
        if (mColorStateList == null || drawableStates == null) return;

        int color = mColorStateList.getColorForState(drawableStates, Color.TRANSPARENT);
        ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY);
        setColorFilter(filter);
    }

    public void setTintColor(int color) {
        mColorStateList = new ColorStateList(new int[][]{}, new int[color]);
    }

    public void setTintColor(ColorStateList colorStateList) {
        if (colorStateList != null) {
            mColorStateList = colorStateList;
        }
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        updateTintColor();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateTintColor();
    }

}
