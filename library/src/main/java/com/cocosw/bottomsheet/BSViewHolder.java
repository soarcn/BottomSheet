package com.cocosw.bottomsheet;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

class BSViewHolder extends RecyclerView.ViewHolder {
    final TextView title;
    final ImageView image;

    public BSViewHolder(View itemView) {
        super(itemView);
        title = (TextView) itemView.findViewById(R.id.bs_list_title);
        image = (ImageView) itemView.findViewById(R.id.bs_list_image);
    }

    void setOnClickListener(OnClickListener listener) {
        itemView.setOnClickListener(listener);
    }
}
