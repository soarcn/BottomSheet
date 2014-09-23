package com.cocosw.bottomsheet.example;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import com.cocosw.bottomsheet.BottomSheet;
import com.cocosw.query.CocoQuery;

/**
 * Project: gradle
 * Created by LiaoKai(soarcn) on 2014/9/22.
 */
public class ListAcitivty extends ActionBarActivity implements AdapterView.OnItemClickListener {

    private CocoQuery q;
    private int action;
    private ArrayAdapter<String> adapter;
    private BottomSheet sheet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent().getBooleanExtra("dark",false)) {
            setTheme(R.style.DarkTheme);
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

    @Override
    public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
        showDialog(position);
    }

    @Nullable
    @Override
    protected Dialog onCreateDialog(final int position, Bundle args) {
        switch (action) {
            case 0:
                sheet = new BottomSheet.Builder(this).title("To "+adapter.getItem(position)).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position),which);
                    }
                }).create();
                break;
            case 1:
                sheet = new BottomSheet.Builder(this).sheet(R.menu.noicon).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position), which);
                    }
                }).create();
                break;

            case 2:
                sheet = new BottomSheet.Builder(this).darkTheme().title("To " + adapter.getItem(position)).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position),which);
                    }
                }).create();
                break;
            case 3:
                sheet = new BottomSheet.Builder(this).sheet(R.menu.list).listener(new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ListAcitivty.this.onClick(adapter.getItem(position),which);
                    }
                }).grid().create();
        }
        return sheet;
    }

    public void onClick(String name, int which) {
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
