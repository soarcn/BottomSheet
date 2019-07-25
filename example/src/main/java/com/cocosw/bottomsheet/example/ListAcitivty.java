package com.cocosw.bottomsheet.example;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.cocosw.bottomsheet.BottomSheet;
import com.cocosw.bottomsheet.BottomSheetHelper;
import com.cocosw.query.CocoQuery;

/**
 * Project: gradle
 * Created by LiaoKai(soarcn) on 2014/9/22.
 */
public class ListAcitivty extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private CocoQuery q;
    private int action;
    private ArrayAdapter<String> adapter;
    private BottomSheet sheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().getBooleanExtra("style", false)) {
            setTheme(R.style.StyleTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_main);
        action = getIntent().getFlags();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        q = new CocoQuery(this);
        setTitle(getIntent().getStringExtra("title"));
        String[] items = new String[]{"Janet Perkins", "Mary Johnson", "Peter Carlsson", "Trevor Hansen", "Aaron Bennett"};
        q.id(R.id.listView)
                .adapter(adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, items))
                .itemClicked(this);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home)
            finish();
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        showDialog(position);
    }

    private Drawable getRoundedBitmap(int imageId) {
        Bitmap src = BitmapFactory.decodeResource(getResources(), imageId);
        Bitmap dst;
        if (src.getWidth() >= src.getHeight()) {
            dst = Bitmap.createBitmap(src, src.getWidth() / 2 - src.getHeight() / 2, 0, src.getHeight(), src.getHeight());
        } else {
            dst = Bitmap.createBitmap(src, 0, src.getHeight() / 2 - src.getWidth() / 2, src.getWidth(), src.getWidth());
        }
        RoundedBitmapDrawable roundedBitmapDrawable = RoundedBitmapDrawableFactory.create(getResources(), dst);
        roundedBitmapDrawable.setCornerRadius(dst.getWidth() / 2);
        roundedBitmapDrawable.setAntiAlias(true);
        return roundedBitmapDrawable;
    }

    @SuppressWarnings("deprecation")
    @Nullable
    @Override
    protected Dialog onCreateDialog(final int position, Bundle args) {
        switch (action) {
            case 0:
                sheet = new BottomSheet.Builder(this).icon(getRoundedBitmap(R.drawable.icon)).title("To " + adapter.getItem(position)).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position), which);
                    }
                }).build();
                break;
            case 1:
                sheet = new BottomSheet.Builder(this).sheet(R.menu.noicon).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position), which);
                    }
                }).build();
                break;

            case 2:
                sheet = new BottomSheet.Builder(this).darkTheme().title("To " + adapter.getItem(position)).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position), which);
                    }
                }).build();
                break;
            case 3:
                sheet = new BottomSheet.Builder(this).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position), which);
                    }
                }).grid().build();
                break;
            case 4:
                sheet = new BottomSheet.Builder(this, R.style.BottomSheet_StyleDialog).title("To " + adapter.getItem(position)).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position), which);
                    }
                }).build();
                break;
            case 5:
                sheet = new BottomSheet.Builder(this).title("To " + adapter.getItem(position)).sheet(R.menu.longlist).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position), which);
                    }
                }).limit(R.integer.bs_initial_list_row).build();
                break;
            case 6:
                sheet = getShareActions("Hello " + adapter.getItem(position)).title("Share To " + adapter.getItem(position)).limit(R.integer.no_limit).build();
                break;
            case 7:
                sheet = getShareActions("Hello " + adapter.getItem(position)).title("Share To " + adapter.getItem(position)).build();
                break;
            case 8:
                sheet = new BottomSheet.Builder(this).icon(getRoundedBitmap(R.drawable.icon)).title("To " + adapter.getItem(position)).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position), which);
                    }
                }).build();
                final Menu menu = sheet.getMenu();
                menu.getItem(0).setTitle("MenuClickListener");
                menu.getItem(0).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        q.alert("OnMenuItemClickListener", "You can set OnMenuItemClickListener for each item");
                        return true;
                    }
                });
                menu.getItem(1).setVisible(false);
                menu.getItem(2).setEnabled(false);
                menu.add(Menu.NONE, 23, Menu.NONE, "Fresh meal!");
                menu.findItem(23).setIcon(R.drawable.perm_group_user_dictionary);
                menu.findItem(23).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        q.toast("Hello");
                        return true;
                    }
                });
                menu.setGroupVisible(android.R.id.empty,false);
                break;
            case 9:
                sheet = new BottomSheet.Builder(this, R.style.BottomSheet_CustomizedDialog).grid().title("To " + adapter.getItem(position)).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position), which);
                    }
                }).build();
                sheet.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        q.toast("I'm showing");
                    }
                });
                sheet.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        q.toast("I'm dismissing");
                    }
                });
                break;

        }
        return sheet;
    }

    private BottomSheet.Builder getShareActions(String text) {
        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);

        return BottomSheetHelper.shareAction(this, shareIntent);
    }


    void onClick(String name, int which) {
        switch (which) {
            case R.id.share:
                q.toast("Share to " + name);
                break;
            case R.id.upload:
                q.toast("Upload for " + name);
                break;
            case R.id.call:
                q.toast("Call to " + name);
                break;
            case R.id.help:
                q.toast("Help me!");
                break;
        }
    }
}
