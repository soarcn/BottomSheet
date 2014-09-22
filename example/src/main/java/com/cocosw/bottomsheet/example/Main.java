package com.cocosw.bottomsheet.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.cocosw.query.CocoQuery;

/**
 * Project: gradle
 * Created by LiaoKai(soarcn) on 2014/9/21.
 */
public class Main extends Activity implements AdapterView.OnItemClickListener {

    private CocoQuery q;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ui_main);
        q = new CocoQuery(this);

        String[] items = new String[]{"List","Grid","Divider","FullScreen","Style"};
        q.id(R.id.listView)
                .adapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,android.R.id.text1,items))
                .itemClicked(this);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startActivity(new Intent(this,ListAcitivty.class).setFlags(position));
    }

}
