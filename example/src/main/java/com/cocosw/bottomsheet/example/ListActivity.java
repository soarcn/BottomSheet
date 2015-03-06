package com.cocosw.bottomsheet.example;

import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.cocosw.bottomsheet.BottomSheet;
import com.cocosw.query.CocoQuery;

import java.util.List;

/**
 * Project: gradle
 * Created by LiaoKai(soarcn) on 2014/9/22.
 */
public class ListActivity extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private CocoQuery q;
    private int action;
    private ArrayAdapter<String> adapter;
    private BottomSheet sheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().getBooleanExtra("style",false)) {
            setTheme(R.style.StyleTheme);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_main);
        action = getIntent().getFlags();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        q = new CocoQuery(this);
        setTitle(getIntent().getStringExtra("title"));
        String[] items = new String[]{"Janet Perkins","Mary Johnson","Peter Carlsson","Trevor Hansen","Aaron Bennett"};
        q.id(R.id.listView)
                .adapter(adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, items))
                .itemClicked(this);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==android.R.id.home)
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
        if (src.getWidth() >= src.getHeight()){
            dst = Bitmap.createBitmap(src, src.getWidth()/2 - src.getHeight()/2, 0, src.getHeight(), src.getHeight()
            );
        }else{
            dst = Bitmap.createBitmap(src, 0, src.getHeight()/2 - src.getWidth()/2, src.getWidth(), src.getWidth()
            );
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
                        ListActivity.this.onClick(adapter.getItem(position),which);
                    }
                }).build();
                break;
            case 1:
                sheet = new BottomSheet.Builder(this).sheet(R.menu.noicon).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListActivity.this.onClick(adapter.getItem(position), which);
                    }
                }).build();
                break;

            case 2:
                sheet = new BottomSheet.Builder(this).darkTheme().title("To " + adapter.getItem(position)).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListActivity.this.onClick(adapter.getItem(position),which);
                    }
                }).build();
                break;
            case 3:
                sheet = new BottomSheet.Builder(this).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListActivity.this.onClick(adapter.getItem(position),which);
                    }
                }).grid().build();
                break;
            case 4:
                sheet = new BottomSheet.Builder(this,R.style.BottomSheet_StyleDialog).title("To "+adapter.getItem(position)).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListActivity.this.onClick(adapter.getItem(position),which);
                    }
                }).build();
                break;
            case 5:
                sheet = new BottomSheet.Builder(this).title("To "+adapter.getItem(position)).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListActivity.this.onClick(adapter.getItem(position),which);
                    }
                }).grid().build();
                break;
            case 6:
                sheet = getShareActions(new BottomSheet.Builder(this).grid().title("Share To "+adapter.getItem(position)),"Hello "+adapter.getItem(position)).build();
                break;
            case 7:
                sheet = getShareActions(new BottomSheet.Builder(this).grid().title("Share To "+adapter.getItem(position)),"Hello "+adapter.getItem(position)).limit(R.integer.bs_initial_grid_row).build();
                break;

        }
        return sheet;
    }

    private BottomSheet.Builder getShareActions(BottomSheet.Builder builder, String text) {
        PackageManager pm = this.getPackageManager();

        final Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        final List<ResolveInfo> list = pm.queryIntentActivities(shareIntent, 0);

        for (int i = 0; i < list.size(); i++) {
            builder.sheet(i,list.get(i).loadIcon(pm),list.get(i).loadLabel(pm));
        }

        builder.listener(new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityInfo activity = list.get(which).activityInfo;
                ComponentName name = new ComponentName(activity.applicationInfo.packageName,
                        activity.name);
                Intent newIntent = (Intent) shareIntent.clone();
                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                newIntent.setComponent(name);
                startActivity(newIntent);
            }
        });
        return builder;
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
