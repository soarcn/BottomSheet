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
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;


/**
 * One way to present a set of actions to a user is with bottom sheets, a sheet of paper that slides up from the bottom edge of the screen. Bottom sheets offer flexibility in the display of clear and simple actions that do not need explanation.
 *
 * https://www.google.com/design/spec/components/bottom-sheets.html
 *
 * Project: BottomSheet
 * Created by LiaoKai(soarcn) on 2014/9/21.
 */
public class BottomSheet extends Dialog implements DialogInterface {

    private RecyclerView list;
    private ArrayList<MenuItem> menuItem;
    private RecyclerView.Adapter adapter;
    private Builder builder;

    // translucent support
    private static final String NAV_BAR_HEIGHT_RES_NAME = "navigation_bar_height";
    private static final String NAV_BAR_HEIGHT_LANDSCAPE_RES_NAME = "navigation_bar_height_landscape";
    private static final String SHOW_NAV_BAR_RES_NAME = "config_showNavigationBar";
    private boolean mInPortrait;
    private String sNavBarOverride;
    private boolean mNavBarAvailable;
    private float mSmallestWidthDp;

    public BottomSheet(Context context) {
        super(context,R.style.BottomSheet_Dialog);
    }

    @SuppressWarnings("WeakerAccess")
    public BottomSheet(Context context, int theme) {
        super(context, theme);
        // https://github.com/jgilfelt/SystemBarTint/blob/master/library/src/com/readystatesoftware/systembartint/SystemBarTintManager.java
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            mInPortrait = (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
            try {
                Class c = Class.forName("android.os.SystemProperties");
                Method m = c.getDeclaredMethod("get", String.class);
                m.setAccessible(true);
                sNavBarOverride = (String) m.invoke(null, "qemu.hw.mainkeys");
            } catch (Throwable e) {
                sNavBarOverride = null;
            }

            // check theme attrs
            int[] as = {android.R.attr.windowTranslucentStatus,
                    android.R.attr.windowTranslucentNavigation};
            TypedArray a = context.obtainStyledAttributes(as);
            try {
                mNavBarAvailable = a.getBoolean(1, false);
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


    private void init(final Context context) {
        setCanceledOnTouchOutside(true);
        View mDialogView = View.inflate(context, R.layout.bottom_sheet_dialog, null);
        setContentView(mDialogView);

        this.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                list.startLayoutAnimation();
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && mNavBarAvailable) {
              mDialogView.setPadding(0, 0, 0, getNavigationBarHeight(getContext())+mDialogView.getPaddingBottom());
        }

        TextView title = (TextView) mDialogView.findViewById(R.id.bottom_sheet_title);
        if (builder.title != null) {
            title.setVisibility(View.VISIBLE);
            title.setText(builder.title);
        }

        list = (RecyclerView) mDialogView.findViewById(R.id.bottom_sheet_recyclerview);

        if (builder.grid) {
            list.setLayoutManager(new GridLayoutManager(context, context.getResources().getInteger(R.integer.bs_grid_colum)));
        } else {
            list.setLayoutManager(new LinearLayoutManager(context));
        }

        menuItem = builder.menuItems;
        // Grid mode do not support divider, we will remove them all here
        if (builder.grid) {
            Iterator<MenuItem> i = menuItem.iterator();
            while (i.hasNext()) {
                MenuItem item = i.next();
                if (item.divider)
                    i.remove();
                else if (item.icon==null) {
                    throw new IllegalArgumentException("You should set icon for each items in grid style");
                }
            }
        //    list.setPadding(R.dimen.bs_grid_left_padding,R.dimen.bs_grid_top_padding,R.dimen.bs_grid_right_padding,R.dimen.bs_grid_bottom_padding);
        }

        adapter = new RecyclerView.Adapter() {

            private static final int TYPE_DIVIDER = 1;
            private static final int TYPE_MENUITEM = 0;

            @Override
            public int getItemCount() {
                return menuItem.size();
            }

            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
                View v;
                if (viewType == TYPE_DIVIDER) {
                    v = LayoutInflater.from(context).inflate(R.layout.bs_list_divider, viewGroup, false);
                    return new DividerViewHolder(v);
                } else {
                    if (builder.grid) {
                        v = LayoutInflater.from(context).inflate(R.layout.bs_grid_entry, viewGroup, false);
                    } else {
                        v = LayoutInflater.from(context).inflate(R.layout.bs_list_entry, viewGroup, false);
                    }
                    return new MenuItemViewHolder(v, builder.listener);
                }
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int i) {
                MenuItem item = menuItem.get(i);
                if (holder.getItemViewType() == TYPE_MENUITEM) {
                    MenuItemViewHolder viewHolder = (MenuItemViewHolder) holder;
                    viewHolder.title.setText(item.text);
                    if (item.icon == null)
                        viewHolder.image.setVisibility(View.GONE);
                    else {
                        viewHolder.image.setVisibility(View.VISIBLE);
                        viewHolder.image.setImageDrawable(item.icon);
                    }
                }
            }

            @Override
            public int getItemViewType(int position) {
                return menuItem.get(position).divider ? TYPE_DIVIDER : TYPE_MENUITEM;
            }

        };

        list.setAdapter(adapter);

        list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (Build.VERSION.SDK_INT < 16) {
                    //noinspection deprecation
                    list.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                } else {
                    list.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                View lastChild = list.getChildAt(list.getChildCount() - 1);
                if (lastChild!=null)
                    list.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, lastChild.getBottom() + lastChild.getPaddingBottom()));
            }
        });

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(getContext());
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.BOTTOM;
        getWindow().setAttributes(params);
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

    /**
     * MenuItem
     */
    private static class MenuItem {
        private int id;
        private CharSequence text;
        private Drawable icon;
        boolean divider;

        private MenuItem() {
        }

        private MenuItem(int id, CharSequence text, Drawable icon) {
            this.id = id;
            this.text = text;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return "MenuItem{" +
                    "id=" + id +
                    ", text=" + text +
                    ", icon=" + icon +
                    ", divider=" + divider +
                    '}';
        }
    }

    private class MenuItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView title;
        private ImageView image;
        private DialogInterface.OnClickListener listener;

        public MenuItemViewHolder(View itemView, DialogInterface.OnClickListener onClickListener) {
            super(itemView);
            listener = onClickListener;
            title = (TextView) itemView.findViewById(R.id.bs_list_title);
            image = (ImageView) itemView.findViewById(R.id.bs_list_image);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (listener != null) {
                listener.onClick(BottomSheet.this, menuItem.get(getPosition()).id);
            }
        }
    }

    private class DividerViewHolder extends RecyclerView.ViewHolder {

        public DividerViewHolder(View itemView) {
            super(itemView);
        }
    }

    /***
     *  Constructor using a context for this builder and the {@link com.cocosw.bottomsheet.BottomSheet} it creates.
     */
    public static class Builder {

        private final Context context;
        private int theme;
        private final ArrayList<MenuItem> menuItems = new ArrayList<>();
        private CharSequence title;
        private boolean grid;
        private OnClickListener listener;

        /**
         * Constructor using a context for this builder and the {@link com.cocosw.bottomsheet.BottomSheet} it creates.
         *
         * @param context A Context for built BottomSheet.
         */
        public Builder(@NonNull Activity context) {
            this(context,R.style.BottomSheet_Dialog);
            TypedArray ta = context.getTheme().obtainStyledAttributes(new int[]{R.attr.bottomSheetStyle});
            try {
                theme = ta.getResourceId(0, R.style.BottomSheet_Dialog);
            } finally {
                ta.recycle();
            }
        }

        /**
         * Constructor using a context for this builder and the {@link com.cocosw.bottomsheet.BottomSheet} it creates with given style
         *
         * @param context A Context for built BottomSheet.
         */
        public Builder(Context context, @StyleRes int theme) {
            this.context = context;
            this.theme = theme;


        }

        /**
         * Set menu resources as list item to display in BottomSheet
         *
         * @param xmlRes menu resource id
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder sheet(@MenuRes int xmlRes) {
            parseXml(xmlRes);
            return this;
        }

        private void parseXml(int menu) {
            try {
                XmlResourceParser xpp = context.getResources().getXml(menu);
                xpp.next();
                int eventType = xpp.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        String elemName = xpp.getName();
                        if (elemName.equals("item")) {
                            String textId = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "title");
                            String iconId = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "icon");
                            String resId = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "id");

                            MenuItem item = new MenuItem();
                            item.id = Integer.valueOf(resId.replace("@", ""));
                            item.text = resourceIdToString(textId);
                            if (!TextUtils.isEmpty(iconId))
                                item.icon = context.getResources().getDrawable(Integer.valueOf(iconId.replace("@", "")));

                            menuItems.add(item);
                        } else if (elemName.equals("divider")) {
                            MenuItem item = new MenuItem();
                            item.divider = true;
                            menuItems.add(item);
                        }
                    }
                    eventType = xpp.next();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("UnusedReturnValue")
        private Builder item(@NonNull MenuItem item) {
            menuItems.add(item);
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
            item(new MenuItem(id, context.getText(textRes), context.getResources().getDrawable(iconRes)));
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
            item(new MenuItem(id, text, icon));
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
            item(new MenuItem(id, context.getText(textRes), null));
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
            item(new MenuItem(id, text, null));
            return this;
        }

        /**
         * Set title for BottomSheet
         * @param titleRes title for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder title(@StringRes int titleRes) {
            title = context.getText(titleRes);
            return this;
        }

        /**
         * Add a divider in to the list
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder divider() {
            MenuItem item = new MenuItem();
            item.divider = true;
            item(item);
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

        private CharSequence resourceIdToString(String text) {
            if (!text.contains("@")) {
                return text;
            } else {
                String id = text.replace("@", "");
                return context.getResources().getText(Integer.valueOf(id));
            }
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
            BottomSheet dialog = new BottomSheet(context, theme);
            dialog.builder = this;
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
