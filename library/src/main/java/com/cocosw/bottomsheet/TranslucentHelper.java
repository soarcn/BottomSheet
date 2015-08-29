package com.cocosw.bottomsheet;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;

import java.lang.reflect.Method;

/**
 * Project: BottomSheet
 * Created by LiaoKai(soarcn) on 2015/8/29.
 */

@TargetApi(19)
class TranslucentHelper {

    // translucent support
    private static final String STATUS_BAR_HEIGHT_RES_NAME = "status_bar_height";
    private static final String NAV_BAR_HEIGHT_RES_NAME = "navigation_bar_height";
    private static final String NAV_BAR_HEIGHT_LANDSCAPE_RES_NAME = "navigation_bar_height_landscape";
    private static final String SHOW_NAV_BAR_RES_NAME = "config_showNavigationBar";
    private final Dialog dialog;
    boolean mNavBarAvailable;
    int mStatusBarHeight;
    private boolean mInPortrait;
    private String sNavBarOverride;
    private float mSmallestWidthDp;

    TranslucentHelper(Dialog dialog, Context context) {
        this.dialog = dialog;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mInPortrait = (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
        try {
            Class c = Class.forName("android.os.SystemProperties");
            @SuppressWarnings("unchecked") Method m = c.getDeclaredMethod("get", String.class);
            m.setAccessible(true);
            sNavBarOverride = (String) m.invoke(null, "qemu.hw.mainkeys");
        } catch (Throwable e) {
            sNavBarOverride = null;
        }

        // check theme attrs
        int[] as = {android.R.attr.windowTranslucentNavigation};
        TypedArray a = context.obtainStyledAttributes(as);
        try {
            mNavBarAvailable = a.getBoolean(0, false);
        } finally {
            a.recycle();
        }

        // check window flags
        WindowManager.LayoutParams winParams = ((Activity) context).getWindow().getAttributes();

        int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
        if ((winParams.flags & bits) != 0) {
            mNavBarAvailable = true;
        }

        mSmallestWidthDp = getSmallestWidthDp(wm);
        if (mNavBarAvailable)
            setTranslucentStatus(true);
        mStatusBarHeight = getInternalDimensionSize(context.getResources(), STATUS_BAR_HEIGHT_RES_NAME);
    }

    @SuppressWarnings("SameParameterValue")
    private void setTranslucentStatus(boolean on) {
        Window win = dialog.getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }

        win.setAttributes(winParams);
        // instance
        win.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
    }

    private float getSmallestWidthDp(WindowManager wm) {
        DisplayMetrics metrics = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            wm.getDefaultDisplay().getRealMetrics(metrics);
        } else {
            //this is not correct, but we don't really care pre-kitkat
            wm.getDefaultDisplay().getMetrics(metrics);
        }
        float widthDp = metrics.widthPixels / metrics.density;
        float heightDp = metrics.heightPixels / metrics.density;
        return Math.min(widthDp, heightDp);
    }

    int getNavigationBarHeight(Context context) {
        Resources res = context.getResources();
        int result = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (hasNavBar(context)) {
                String key;
                if (mInPortrait) {
                    key = NAV_BAR_HEIGHT_RES_NAME;
                } else {
                    if (!isNavigationAtBottom())
                        return 0;
                    key = NAV_BAR_HEIGHT_LANDSCAPE_RES_NAME;
                }
                return getInternalDimensionSize(res, key);
            }
        }
        return result;
    }

    private boolean hasNavBar(Context context) {
        Resources res = context.getResources();
        int resourceId = res.getIdentifier(SHOW_NAV_BAR_RES_NAME, "bool", "android");
        if (resourceId != 0) {
            boolean hasNav = res.getBoolean(resourceId);
            // check override flag (see static block)
            if ("1".equals(sNavBarOverride)) {
                hasNav = false;
            } else if ("0".equals(sNavBarOverride)) {
                hasNav = true;
            }
            return hasNav;
        } else { // fallback
            return !ViewConfiguration.get(context).hasPermanentMenuKey();
        }
    }

    private int getInternalDimensionSize(Resources res, String key) {
        int result = 0;
        int resourceId = res.getIdentifier(key, "dimen", "android");
        if (resourceId > 0) {
            result = res.getDimensionPixelSize(resourceId);
        }
        return result;
    }

    /**
     * Should a navigation bar appear at the bottom of the screen in the current
     * device configuration? A navigation bar may appear on the right side of
     * the screen in certain configurations.
     *
     * @return True if navigation should appear at the bottom of the screen, False otherwise.
     */
    private boolean isNavigationAtBottom() {
        return (mSmallestWidthDp >= 600 || mInPortrait);
    }

}
