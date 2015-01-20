package com.cocosw.bottomsheet;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemAnimator.ItemAnimatorFinishedListener;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.cocosw.bottomsheet.utils.BuildHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * One way to present a set of actions to a user is with bottom sheets, a sheet of paper that slides up from the bottom edge of the screen. Bottom sheets offer flexibility in the display of clear and simple actions that do not need explanation.
 *
 * https://www.google.com/design/spec/components/bottom-sheets.html
 *
 * Project: BottomSheet
 * Created by LiaoKai(soarcn) on 2014/9/21.
 */
public class BottomSheet extends Dialog implements DialogInterface, View.OnClickListener {

    private static final long RIPPLE_ANIM_LENGTH = 350;
    private RecyclerView list;
    private BottomSheetAdapter adapter;
    
    // Builder Props
    private List<BSItem> bsItems;
    private Dialog.OnClickListener dialogListener;
    private CharSequence text;
    private boolean grid;
    

    // translucent support
    private static final String NAV_BAR_HEIGHT_RES_NAME = "navigation_bar_height";
    private static final String NAV_BAR_HEIGHT_LANDSCAPE_RES_NAME = "navigation_bar_height_landscape";
    private static final String SHOW_NAV_BAR_RES_NAME = "config_showNavigationBar";
    private boolean mInPortrait;
    private String sNavBarOverride;
    private boolean mNavBarAvailable;
    private float mSmallestWidthDp;
    private int itemHeight;

    public BottomSheet(Context context) {
        super(context,R.style.BottomSheet_Dialog);
    }

    private BottomSheet(Builder builder) {
        super(builder.activity, builder.theme);
        // https://github.com/jgilfelt/SystemBarTint/blob/master/library/src/com/readystatesoftware/systembartint/SystemBarTintManager.java
        if (BuildHelper.HAS_KITKAT) {
            reflectOnNavBar(builder);
            mInPortrait = (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
            setupTheme(builder);
            setupWidow(builder);
        }
        setCanceledOnTouchOutside(true);
        setPropertiesFromBuilder(builder);
    }

    @TargetApi(19)
    private void setupWidow(Builder builder) {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        
        WindowManager.LayoutParams winParams = (builder.activity).getWindow().getAttributes();
        int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
        if ((winParams.flags & bits) != 0) {
            mNavBarAvailable = true;
        }
        mSmallestWidthDp = getSmallestWidthDp(wm);
        if (mNavBarAvailable)
            setTranslucentStatus(true);
    }

    @TargetApi(19)
    private void setTranslucentStatus(boolean on) {
        Window win = getWindow();
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

    @TargetApi(19)
    /** Let's be honest, reflecting on the system is bad. */
    private void reflectOnNavBar(Builder builder) {
        try {
            Class c = Class.forName("android.os.SystemProperties");
            Method m = c.getDeclaredMethod("get", String.class);
            m.setAccessible(true);
            sNavBarOverride = (String) m.invoke(null, "qemu.hw.mainkeys");
        } catch (Throwable e) {
            sNavBarOverride = null;
        }
    }

    @TargetApi(19)
    private void setupTheme(Builder builder) {
        int[] as = {android.R.attr.windowTranslucentStatus,
                android.R.attr.windowTranslucentNavigation};
        TypedArray a = getContext().obtainStyledAttributes(as);
        try {
            mNavBarAvailable = a.getBoolean(1, false);
        } finally {
            a.recycle();
        }
    }

    private void setPropertiesFromBuilder(Builder builder) {
        dialogListener = builder.listener;

        text = builder.title;
        grid = builder.grid;

        bsItems = builder.bsItems;
        // Grid mode does not support divider, we will remove them all here
        if (builder.grid) {
            Iterator<BSItem> i = bsItems.iterator();
            while (i.hasNext()) {
                BSItem item = i.next();
                if (item.isDivider())
                    i.remove();
                else if (item.getIcon()==null) {
                    throw new IllegalArgumentException("You should set icon for each items in grid style");
                }
            }
        }
    }

    @SuppressLint("NewApi")
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

    @TargetApi(14)
    private int getNavigationBarHeight(Context context) {
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

    @TargetApi(14)
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

    @Override
    public void onClick(View view) {
        if (dialogListener != null) {
            dialogListener.onClick(BottomSheet.this, (Integer) view.getTag());
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflateViews(getContext());
        setDialogLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        postInvalidateDialogLayout();
    }

    private void inflateViews(final Context context) {
        View dialogView = View.inflate(context, R.layout.bottom_sheet_dialog, null);
        setContentView(dialogView);

        this.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                list.startLayoutAnimation();
            }
        });

        if (BuildHelper.HAS_KITKAT && mNavBarAvailable) {
            dialogView.setPadding(0, 0, 0, getNavigationBarHeight(getContext())+dialogView.getPaddingBottom());
        }

        TextView title = (TextView) dialogView.findViewById(R.id.bottom_sheet_title);
        if (text != null) {
            title.setVisibility(View.VISIBLE);
            title.setText(text);
        }

        setupListView(dialogView);
    }

    private void setupListView(View dialogView) {
        list = (RecyclerView) dialogView.findViewById(R.id.bottom_sheet_recyclerview);

        if (grid) {
            list.setLayoutManager(new GridLayoutManager(getContext(), getContext().getResources().getInteger(R.integer.bs_grid_colum)));
        } else {
            list.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        list.setAdapter(getAdapter());
        list.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < 16) {
                    //noinspection deprecation
                    list.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                View lastChild = list.getChildAt(list.getChildCount() - 1);
                if (lastChild != null) {
                    itemHeight = lastChild.getHeight() + lastChild.getPaddingTop() + lastChild.getPaddingBottom();
                }
            }
        });
    }

    private void setDialogLocation() {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.BOTTOM;
        getWindow().setAttributes(params);
    }

    private BottomSheetAdapter getAdapter() {
        if (adapter == null) {
            adapter = new BottomSheetAdapter(getContext(), this, bsItems, grid);
        }
        return adapter;
    }

    public void showItem(int itemId) {
        getAdapter().showItem(itemId);

        invalidateDialogLayout();
    }

    public void hideItem(int itemId) {
        getAdapter().hideItem(itemId);

        list.post(new Runnable() {
            @Override
            public void run() {
                if (list == null) return;

                list.getItemAnimator().isRunning(new ItemAnimatorFinishedListener() {
                    @Override
                    public void onAnimationsFinished() {
                        invalidateDialogLayout();
                    }
                });
            }
        });
    }

    public void postInvalidateDialogLayout() {
        if (list == null) return;

        list.post(new Runnable() {
            @Override
            public void run() {
                invalidateDialogLayout();
            }
        });
    }

    public void invalidateDialogLayout() {
        if (list == null) return;

        int height = grid ? getGridHeight() : getListHeight();
        list.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, height));
    }

    private int getGridHeight() {
        Resources res = getContext().getResources();

        int columnCount = res.getInteger(R.integer.bs_grid_colum);
        int rowCount = (int) Math.ceil((float) getAdapter().getItemCountWithoutDividers() / columnCount);

        return rowCount * itemHeight;
    }

    private int getListHeight() {

        Resources res = getContext().getResources();

        int itemCount = getAdapter().getItemCountWithoutDividers();
        int dividerCount = getAdapter().getDividerCount();

        int dividerHeight = res.getDimensionPixelSize(R.dimen.bs_divider_height)
                + res.getDimensionPixelSize(R.dimen.bs_divider_margin_top)
                + res.getDimensionPixelSize(R.dimen.bs_divider_margin_bottom);
        return (itemCount * itemHeight) + (dividerCount * dividerHeight);
    }

    public void dismissAfterRipple() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, RIPPLE_ANIM_LENGTH);
    }

    public void hideAfterRipple() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                hide();
            }
        }, RIPPLE_ANIM_LENGTH);
    }

    /***
     *  Constructor using a activity for this builder and the {@link com.cocosw.bottomsheet.BottomSheet} it creates.
     */
    public static class Builder {

        private final Activity activity;
        private int theme;
        private final List<BSItem> bsItems = new ArrayList<>();
        private CharSequence title;
        private boolean grid;
        private OnClickListener listener;

        /**
         * Constructor using a activity for this builder and the {@link com.cocosw.bottomsheet.BottomSheet} it creates.
         *
         * @param activity A Context for built BottomSheet.
         */
        public Builder(@NonNull Activity activity) {
            this(activity, R.style.BottomSheet_Dialog);
            TypedArray ta = activity.getTheme().obtainStyledAttributes(new int[]{R.attr.bottomSheetStyle});
            try {
                theme = ta.getResourceId(0, R.style.BottomSheet_Dialog);
            } finally {
                ta.recycle();
            }
        }

        /**
         * Constructor using a activity for this builder and the {@link com.cocosw.bottomsheet.BottomSheet} it creates with given style
         *
         * @param activity An Activity for built BottomSheet.
         */
        public Builder(Activity activity, @StyleRes int theme) {
            this.activity = activity;
            this.theme = theme;


        }

        /**
         * Set menu resources as list item to display in BottomSheet
         *
         * @param xmlRes menu resource id
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(@MenuRes int xmlRes) {
            bsItems.addAll(BSItem.parseMenuXml(activity, xmlRes));
            return this;
        }

        @SuppressWarnings("UnusedReturnValue")
        private Builder addItem(@NonNull BSItem item) {
            bsItems.add(item);
            return this;
        }

        /**
         * Add one item into BottomSheet
         *
         * @param id ID of item
         * @param iconRes icon resource
         * @param textRes text resource
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id, @DrawableRes int iconRes, @StringRes int textRes) {
            addItem(new BSItem(id, activity.getText(textRes), activity.getResources().getDrawable(iconRes)));
            return this;
        }

        /**
         * Add one item into BottomSheet
         *
         * @param id ID of item
         * @param icon icon
         * @param text text
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id,@NonNull Drawable icon, @NonNull CharSequence text) {
            addItem(new BSItem(id, text, icon));
            return this;
        }

        /**
         * Add one item without icon into BottomSheet
         *
         * @param id ID of item
         * @param textRes text resource
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id, @StringRes int textRes) {
            addItem(new BSItem(id, activity.getText(textRes), null));
            return this;
        }

        /**
         * Add one item without icon into BottomSheet
         *
         * @param id ID of item
         * @param text text
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(int id,@NonNull CharSequence text) {
            addItem(new BSItem(id, text, null));
            return this;
        }

        /**
         * Set title for BottomSheet
         * @param titleRes title for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder title(@StringRes int titleRes) {
            title = activity.getText(titleRes);
            return this;
        }

        /**
         * Add a divider in to the list
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder divider() {
            BSItem item = new BSItem();
            item.setIsDivider(true);
            addItem(item);
            return this;
        }

        /**
         * Set OnclickListener for BottomSheet
         *
         * @param listener OnclickListener for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder listener(@NonNull OnClickListener listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Show BottomSheet in dark color theme looking
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder darkTheme() {
            theme = R.style.BottomSheet_Dialog_Dark;
            return this;
        }


        /**
         * Show BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public BottomSheet show() {
            BottomSheet dialog = create();
            dialog.show();
            return dialog;
        }

        /**
         * Show items in grid instead of list
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder grid() {
            this.grid = true;
            return this;
        }

        /**
         * Create a BottomSheet but not show it
         * @return This Builder object to allow for chaining of calls to set methods
         */
        @SuppressLint("Override")
        public BottomSheet create() {
            BottomSheet dialog = new BottomSheet(this);
            return dialog;
        }

        /**
         * Set title for BottomSheet
         *
         * @param title title for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder title(CharSequence title) {
            this.title = title;
            return this;
        }
    }


}
