package com.cocosw.bottomsheet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;


/**
 * Project: gradle
 * Created by LiaoKai(soarcn) on 2014/9/21.
 */
public class BottomSheet extends Dialog implements DialogInterface, AdapterView.OnItemClickListener {

    private View mDialogView;
    private TextView title;
    private GridView list;
    private ArrayList<MenuItem> menuItem;
    private BaseAdapter adapter;
    private Builder builder;


    public BottomSheet(Context context) {
        super(context,R.style.BottomSheet_Dialog);
    }

    public BottomSheet(Context context, int theme) {
        super(context, theme);
    }

    private void init(Context context) {
        setCanceledOnTouchOutside(true);
        mDialogView = View.inflate(context, R.layout.bottom_sheet_dialog, null);
        setContentView(mDialogView);

        this.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                list.startLayoutAnimation();
            }
        });


        title = (TextView) mDialogView.findViewById(R.id.bottom_sheet_title);
        if (builder.title != null) {
            title.setVisibility(View.VISIBLE);
            title.setText(builder.title);
        }

        list = (GridView) mDialogView.findViewById(R.id.bottom_sheet_gridview);

        if (builder.grid) {
            list.setNumColumns(3);
        }

        menuItem = builder.menuItems;
        adapter = new BaseAdapter() {

            @Override
            public int getCount() {
                return menuItem.size();
            }

            @Override
            public MenuItem getItem(int position) {
                return menuItem.get(position);
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
                return getItemViewType(position) == 0;
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
                            convertView = inflater.inflate(R.layout.grid_entry, null);
                        else
                            convertView = inflater.inflate(R.layout.list_entry, null);
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
                        holder.image.setVisibility(View.GONE);
                    else {
                        holder.image.setVisibility(View.VISIBLE);
                        holder.image.setImageDrawable(item.icon);
                    }

                    return convertView;
                } else {
                    if (convertView == null) {
                        LayoutInflater inflater = (LayoutInflater) getContext()
                                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        convertView = inflater.inflate(R.layout.list_divider, null);
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
        list.setOnItemClickListener(this);

        list.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                list.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                View lastChild = list.getChildAt(list.getChildCount() - 1);
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



    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (builder.listener != null) {
            builder.listener.onClick(BottomSheet.this, ((MenuItem) adapter.getItem(position)).id);
        }
        dismiss();
    }

    private void setBuilder(Builder builder) {
        this.builder = builder;
    }


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

    public static class Builder {

        private final Context context;
        private ArrayList<MenuItem> menuItems = new ArrayList<MenuItem>();
        private CharSequence title;
        private boolean grid;
        private OnClickListener listener;
        private boolean dark;

        public Builder(Context context) {
            this.context = context;
        }

        public Builder sheet(int xmlRes) {
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

        private Builder item(MenuItem item) {
            menuItems.add(item);
            return this;
        }

        public Builder sheet(int id, int icon, int text) {
            item(new MenuItem(id, context.getText(text), context.getResources().getDrawable(icon)));
            return this;
        }

        public Builder sheet(int id, Drawable icon, CharSequence text) {
            item(new MenuItem(id, text, icon));
            return this;
        }

        public Builder sheet(int id, int text) {
            item(new MenuItem(id, context.getText(text), null));
            return this;
        }

        public Builder sheet(int id, CharSequence text) {
            item(new MenuItem(id, text, null));
            return this;
        }

        public Builder title(int titleRes) {
            title = context.getText(titleRes);
            return this;
        }

        public Builder divider() {
            MenuItem item = new MenuItem();
            item.divider = true;
            item(item);
            return this;
        }

        public Builder listener(OnClickListener listener) {
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

        public Builder darkTheme() {
            dark = true;
            return this;
        }


        public BottomSheet show() {
            BottomSheet dialog = create();
            dialog.show();
            return dialog;
        }

        private void showgrid() {
            this.grid = true;
            show();
        }

        @SuppressLint("Override")
        public BottomSheet create() {
            BottomSheet dialog = new BottomSheet(context, dark ? R.style.BottomSheet_Dialog_Dark : R.style.BottomSheet_Dialog);
            dialog.setBuilder(this);
            return dialog;
        }

        public Builder title(CharSequence s) {
            title = s;
            return this;
        }
    }


}
