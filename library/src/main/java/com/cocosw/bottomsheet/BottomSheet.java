/*
 * Copyright 2011, 2015 Kai Liao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import android.support.annotation.IntegerRes;
import android.support.annotation.MenuRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.text.TextUtils;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.lang.reflect.Field;
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
 * Created by Kai Liao on 2014/9/21.
 */
public class BottomSheet extends Dialog implements DialogInterface {

    private String moreText;
    private Drawable close;
    private Drawable more;
    private boolean collapseListIcons;
    private int mStatusBarHeight;
    private GridView list;
    private List<MenuItem> menuItem;
    private BaseAdapter adapter;
    private Builder builder;

    // translucent support
    private static final String STATUS_BAR_HEIGHT_RES_NAME = "status_bar_height";
    private static final String NAV_BAR_HEIGHT_RES_NAME = "navigation_bar_height";
    private static final String NAV_BAR_HEIGHT_LANDSCAPE_RES_NAME = "navigation_bar_height_landscape";
    private static final String SHOW_NAV_BAR_RES_NAME = "config_showNavigationBar";
    private boolean mInPortrait;
    private boolean mStatusBarAvailable = true;
    private String sNavBarOverride;
    private boolean mNavBarAvailable;
    private float mSmallestWidthDp;


    private ImageView icon;
    private ArrayList<MenuItem> fullMenuItem;
    private List<MenuItem> actions = menuItem;

    private int limit = -1;
    private boolean cancelOnTouchOutside = true;
    private boolean cancelOnSwipeDown = true;

    public BottomSheet(Context context) {
        super(context,R.style.BottomSheet_Dialog);
    }

    @SuppressWarnings("WeakerAccess")
    public BottomSheet(Context context, int theme) {
        super(context, theme);

        TypedArray a = getContext()
                .obtainStyledAttributes(null, R.styleable.BottomSheet, R.attr.bottomSheetStyle, 0);
        try {
            more = a.getDrawable(R.styleable.BottomSheet_bs_moreDrawable);
            close = a.getDrawable(R.styleable.BottomSheet_bs_closeDrawable);
            moreText = a.getString(R.styleable.BottomSheet_bs_moreText);
            collapseListIcons = a.getBoolean(R.styleable.BottomSheet_bs_collapseListIcons, true);
        }finally {
            a.recycle();
        }

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
            int[] as = {android.R.attr.windowTranslucentNavigation};
            a = context.obtainStyledAttributes(as);
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

    /**
     * Hacky way to get gridview's column number
     */
    private int getNumColumns() {
        try {
            Field numColumns = GridView.class.getDeclaredField("mRequestedNumColumns");
            numColumns.setAccessible(true);
            return numColumns.getInt(list);
        } catch (Exception e) {
            return 1;
        }
    }

    @Override
    public void setCanceledOnTouchOutside(boolean cancel) {
        super.setCanceledOnTouchOutside(cancel);
        cancelOnTouchOutside = cancel;
    }

    /**
     * Sets whether this dialog is canceled when swipe it down
     *
     * @param cancel whether this dialog is canceled when swipe it down
     */
    public void setCanceledOnSwipeDown(boolean cancel) {
        cancelOnSwipeDown = cancel;
    }

    private void init(final Context context) {
        setCanceledOnTouchOutside(cancelOnTouchOutside);
        final ClosableSlidingLayout mDialogView = (ClosableSlidingLayout) View.inflate(context, R.layout.bottom_sheet_dialog, null);
        setContentView(mDialogView);
        if (!cancelOnSwipeDown)
            mDialogView.swipeable = cancelOnSwipeDown;
        mDialogView.setSlideListener(new ClosableSlidingLayout.SlideListener() {
            @Override
            public void onClosed() {
                BottomSheet.this.dismiss();
            }

            @Override
            public void onOpened() {
                showFullItems();
            }
        });


        this.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                actions = menuItem;
                list.setAdapter(adapter);
                list.startLayoutAnimation();
                if (builder.icon == null)
                    icon.setVisibility(View.GONE);
                else {
                    icon.setVisibility(View.VISIBLE);
                    icon.setImageDrawable(builder.icon);
                }
            }
        });
        int[] location = new  int[2] ;
        mDialogView.getLocationOnScreen(location);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mDialogView.setPadding(0, location[0]==0?mStatusBarHeight:0, 0, 0);
            mDialogView.getChildAt(0).setPadding(0, 0,0, mNavBarAvailable?getNavigationBarHeight(getContext())+mDialogView.getPaddingBottom():0);
        }

        final TextView title = (TextView) mDialogView.findViewById(R.id.bottom_sheet_title);
        if (builder.title != null) {
            title.setVisibility(View.VISIBLE);
            title.setText(builder.title);
        }

        icon = (ImageView) mDialogView.findViewById(R.id.bottom_sheet_title_image);


        list = (GridView) mDialogView.findViewById(R.id.bottom_sheet_gridview);
        mDialogView.mTarget = list;
        if (!builder.grid) {
            list.setNumColumns(1);
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
            handleNewLine();
        }

        if (builder.limit >0)
            limit = builder.limit*getNumColumns();
        else
            limit = Integer.MAX_VALUE;

        mDialogView.setCollapsible(false);

        // over the initial numbers
        if (menuItem.size() > limit) {
            fullMenuItem = new ArrayList<>(menuItem);
            menuItem = menuItem.subList(0,limit-1);
            menuItem.add(new MenuItem(R.id.bs_more, moreText,more));
            mDialogView.setCollapsible(true);
        }
        actions = menuItem;

        adapter = new BaseAdapter() {

            @Override
            public int getCount() {
                return actions.size();
            }

            @Override
            public MenuItem getItem(int position) {
                return actions.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public boolean isEnabled(int position) {
                return !getItem(position).empty && getItemViewType(position) == 0;
            }

            @Override
            public boolean areAllItemsEnabled() {
                return false;
            }

            @Override
            public int getItemViewType(int position) {
                return getItem(position).divider ? 1 : 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                ViewHolder holder;
                if (getItemViewType(position) == 0) {
                    if (convertView == null) {
                        LayoutInflater inflater = (LayoutInflater) getContext()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        if (builder.grid)
                            convertView = inflater.inflate(R.layout.bs_grid_entry, parent,false);
                        else
                            convertView = inflater.inflate(R.layout.bs_list_entry, parent,false);
                        holder = new ViewHolder();
                        holder.title = (TextView) convertView.findViewById(R.id.bs_list_title);
                        holder.image = (ImageView) convertView.findViewById(R.id.bs_list_image);
                        convertView.setTag(holder);
                    } else {
                        holder = (ViewHolder) convertView.getTag();
                    }

                    MenuItem item = getItem(position);

                    holder.title.setText(item.text);
                    if (item.icon == null)
                        holder.image.setVisibility(collapseListIcons ? View.GONE : View.INVISIBLE);
                    else {
                        holder.image.setVisibility(View.VISIBLE);
                        holder.image.setImageDrawable(item.icon);
                    }

                    return convertView;
                } else {
                    if (convertView == null) {
                        LayoutInflater inflater = (LayoutInflater) getContext()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        convertView = inflater.inflate(R.layout.bs_list_divider, parent,false);
                        convertView.setVisibility(View.VISIBLE);
                    }
                    return convertView;
                }
            }

            class ViewHolder {
                private TextView title;
                private ImageView image;
            }
        };

        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (((MenuItem) adapter.getItem(position)).id==R.id.bs_more) {
                    showFullItems();
                    mDialogView.setCollapsible(false);
                    return;
                }

                if (builder.listener != null) {
                    builder.listener.onClick(BottomSheet.this, ((MenuItem) adapter.getItem(position)).id);
                }
                dismiss();
            }
        });

        if(builder.dismissListener != null){
            setOnDismissListener(builder.dismissListener);
        }
        setListLayout();
    }

    private void handleNewLine() {
        int columnsNum = getNumColumns();
        List<MenuItem> newLineItems = builder.getNewLineItems();
        Drawable transparentDrawable = builder.context.getResources().getDrawable(R.drawable.transparent);
        for (MenuItem item : newLineItems) {
            int lastNewLineIndex = menuItem.indexOf(item);
            int tempSize = lastNewLineIndex + 1;// = nextIndex
            int leftNum = tempSize % columnsNum;
            int needItemCount = leftNum == 0 ? 0 : (columnsNum - leftNum) ;
            for (int i = 0 ; i < needItemCount ; i++) {
                MenuItem emptyItem = new MenuItem(-1, "", transparentDrawable);
                emptyItem.empty = true;
                menuItem.add(tempSize + i, emptyItem);
            }
        }
    }

    private void showFullItems() {
        actions = fullMenuItem;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Transition changeBounds = new ChangeBounds();
            changeBounds.setDuration(300);
            TransitionManager.beginDelayedTransition(list,changeBounds);
        }

        adapter.notifyDataSetChanged();
        setListLayout();
        icon.setVisibility(View.VISIBLE);
        icon.setImageDrawable(close);
        icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showShortItems();
            }
        });
    }

    private void showShortItems() {
        actions = menuItem;
        adapter.notifyDataSetChanged();
        setListLayout();

        if (builder.icon==null)
            icon.setVisibility(View.GONE);
        else {
            icon.setVisibility(View.VISIBLE);
            icon.setImageDrawable(builder.icon);
        }
    }

    private boolean hasDivider() {
        if (builder.grid) return false;
        else {
            for (MenuItem item : menuItem) {
                if (item.divider) return true;
            }
            return false;
        }
    }

    private void setListLayout() {
        // without divider, the height of gridview is correct
        if (!hasDivider())
            return;
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
                    list.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, lastChild.getBottom() + lastChild.getPaddingBottom()+list.getPaddingBottom()));
            }
        });
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init(getContext());

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.BOTTOM;

        TypedArray a = getContext().obtainStyledAttributes(new int[]{android.R.attr.layout_width});
        try {
            params.width = a.getLayoutDimension(0, ViewGroup.LayoutParams.MATCH_PARENT);
        } finally {
            a.recycle();
        }

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
        boolean empty;
        private int layout = -1;

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
                    ", layout=" + layout +
                    '}';
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
        private OnDismissListener dismissListener;
        private Drawable icon;
        private int limit = -1;


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
         * @param theme The theme id will be apply to BottomSheet
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
                            String layoutId = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "actionLayout");

                            MenuItem item = new MenuItem();
                            item.id = Integer.valueOf(resId.replace("@", ""));
                            item.text = resourceIdToString(textId);
                            if (!TextUtils.isEmpty(iconId))
                                item.icon = context.getResources().getDrawable(Integer.valueOf(iconId.replace("@", "")));

                            if (!TextUtils.isEmpty(layoutId))
                                item.layout = context.getResources().getInteger(Integer.valueOf(layoutId.replace("@", "")));

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
         * Remove an item from BottomSheet
         * @param id ID of item
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder remove(int id) {
            for (MenuItem item : menuItems){
                if (item.id == id) {
                    menuItems.remove(item);
                    break;
                }
            }
            return this;
        }

        /**
         * Set title for BottomSheet
         * @param icon icon for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder icon(Drawable icon) {
            this.icon = icon;
            return this;
        }

        /**
         * Set title for BottomSheet
         * @param iconRes icon resource id for BottomSheet
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder icon(@DrawableRes int iconRes) {
            this.icon = context.getResources().getDrawable(iconRes);
            return this;
        }


        /**
         * Add a divider in to the list
         * Be aware divider would not be shown in grid mode
         *
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder divider() {
            MenuItem item = new MenuItem();
            item.divider = true;
            item(item);
            return this;
        }
        private List<MenuItem> mNewLineItems = new ArrayList<>();

        /**
         * just for grid mode
         */
        public void newLine() {
            mNewLineItems.add(menuItems.get(menuItems.size() - 1));
        }
        public List<MenuItem> getNewLineItems() {
            return mNewLineItems;
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
            BottomSheet dialog = build();
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
         * Set initial number of actions which will be shown in current sheet.
         * If more actions need to be shown, a "more" action will be display in the last position.
         *
         * @param limitRes resource id for initial number of actions
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder limit(@IntegerRes int limitRes) {
            limit = context.getResources().getInteger(limitRes);
            return this;
        }

        /**
         * Deprecated please use build()
         * Create a BottomSheet but not show it
         * @return This Builder object to allow for chaining of calls to set methods
         */
        @SuppressLint("Override")
        @Deprecated
        public BottomSheet create() {
            return build();
        }

        @SuppressLint("Override")
        public BottomSheet build() {
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

        /***
         * Set the OnDismissListener for BottomSheet
         * @param listener OnDismissListener for Bottom
         * @return This Builder object to allow for chaining of calls to set methods
         */
        public Builder setOnDismissListener(@NonNull OnDismissListener listener){
            this.dismissListener = listener;
            return this;
        }
    }


}
