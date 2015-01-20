package com.cocosw.bottomsheet;


import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.List;

class BSItem {
    private int id;
    private CharSequence text;
    private Drawable icon;
    private boolean divider;
    private boolean isVisible = true;

    BSItem() {
    }

    BSItem(int id, CharSequence text, Drawable icon) {
        this(id, text, icon, true);
    }

    BSItem(int id, CharSequence text, Drawable icon, boolean isVisible) {
        this.id = id;
        this.text = text;
        this.icon = icon;
        this.isVisible = isVisible;
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

    public int getId() {
        return id;
    }

    public CharSequence getText() {
        return text;
    }

    public Drawable getIcon() {
        return icon;
    }

    public boolean isDivider() {
        return divider;
    }

    public void setIsDivider(boolean divider) {
        this.divider = divider;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    static List<BSItem> parseMenuXml(Context context, int menu) {
        List<BSItem> bsItems = new ArrayList<>();
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
                        String visible = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "visible");;

                        BSItem item = new BSItem();
                        item.id = Integer.valueOf(resId.replace("@", ""));
                        item.text = resourceIdToString(context, textId);
                        item.isVisible = visible == null || visible.equals("true");
                        if (!TextUtils.isEmpty(iconId))
                            item.icon = context.getResources().getDrawable(Integer.valueOf(iconId.replace("@", "")));

                        bsItems.add(item);
                    } else if (elemName.equals("divider")) {
                        BSItem item = new BSItem();
                        item.divider = true;
                        bsItems.add(item);
                    }
                }
                eventType = xpp.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bsItems;
    }

    private static CharSequence resourceIdToString(Context context, String text) {
        if (!text.contains("@")) {
            return text;
        } else {
            String id = text.replace("@", "");
            return context.getResources().getText(Integer.valueOf(id));
        }
    }
}
