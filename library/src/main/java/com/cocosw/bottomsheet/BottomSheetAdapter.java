package com.cocosw.bottomsheet;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import java.util.List;

class BottomSheetAdapter extends RecyclerView.Adapter {

    private static final int TYPE_DIVIDER = 1;
    private static final int TYPE_MENUITEM = 0;

    private final List<BSItem> bsItems;

    private final OnClickListener clickListener;
    private final LayoutInflater inflater;
    private final boolean isGrid;

    BottomSheetAdapter(Context context, OnClickListener onClickListener, List<BSItem> bsItems, boolean isGrid) {
        inflater = LayoutInflater.from(context);
        clickListener = onClickListener;
        this.bsItems = bsItems;
        this.isGrid = isGrid;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View v;
        if (viewType == TYPE_DIVIDER) {
            v = inflater.inflate(R.layout.bs_list_divider, viewGroup, false);
            return new DividerViewHolder(v);
        } else {
            if (isGrid) {
                v = inflater.inflate(R.layout.bs_grid_entry, viewGroup, false);
            } else {
                v = inflater.inflate(R.layout.bs_list_entry, viewGroup, false);
            }
            return new BSViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        BSItem item = bsItems.get(position);
        if (holder.getItemViewType() == TYPE_MENUITEM) {
            BSViewHolder viewHolder = (BSViewHolder) holder;

            viewHolder.itemView.setTag(item.getId());
            viewHolder.setOnClickListener(clickListener);

            viewHolder.title.setText(item.getText());
            if (item.getIcon() == null)
                viewHolder.image.setVisibility(View.GONE);
            else {
                viewHolder.image.setVisibility(View.VISIBLE);
                viewHolder.image.setImageDrawable(item.getIcon());
            }
        }
    }

    @Override
    public int getItemCount() {
        return bsItems.size();
    }

    @Override
    public long getItemId(int position) {
        return bsItems.get(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return bsItems.get(position).isDivider() ? TYPE_DIVIDER : TYPE_MENUITEM;
    }
}
