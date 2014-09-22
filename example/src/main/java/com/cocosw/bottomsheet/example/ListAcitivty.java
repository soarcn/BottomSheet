package com.cocosw.bottomsheet.example;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.cocosw.bottomsheet.BottomSheet;
import com.cocosw.query.CocoQuery;

/**
 * Project: gradle
 * Created by LiaoKai(soarcn) on 2014/9/22.
 */
public class ListAcitivty extends Activity implements AdapterView.OnItemClickListener {

    private CocoQuery q;
    private int action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_main);
        q = new CocoQuery(this);

        String[] items = new String[]{"Item1","Item1","Item1","Item1","Style"};
        q.id(R.id.listView)
                .adapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, items))
                .itemClicked(this);
        action = getIntent().getFlags();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (action) {
            case 0:
                new BottomSheet.Builder(this).title(R.string.bottomsheet).xml(R.menu.list).show();
                break;
            case 1:
                new BottomSheet.Builder(this).grid().xml(R.menu.list).show();
                break;
            case 2:

                break;
        }
    }
}
