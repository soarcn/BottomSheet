package com.cocosw.bottomsheet;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

class BottomSheetAdapter extends RecyclerView.Adapter {

    private static final int TYPE_DIVIDER = 1;
    private static final int TYPE_MENUITEM = 0;

    private final List<BSItem> bsItems;
    private final List<BSItem> visibleItems = new ArrayList<>();

    private final OnClickListener clickListener;
    private final LayoutInflater inflater;
    private final boolean isGrid;

    BottomSheetAdapter(Context context, OnClickListener onClickListener, List<BSItem> bsItems, boolean isGrid) {
        inflater = LayoutInflater.from(context);
        clickListener = onClickListener;
        this.isGrid = isGrid;

        this.bsItems = bsItems;
        for (BSItem item : bsItems) {
            if (item.isVisible()) visibleItems.add(item);
        }
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
        BSItem item = visibleItems.get(position);
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
        return visibleItems.size();
    }

    @Override
    public long getItemId(int position) {
        return bsItems.get(position).getId();
    }

    @Override
    public int getItemViewType(int position) {
        return visibleItems.get(position).isDivider() ? TYPE_DIVIDER : TYPE_MENUITEM;
    }

    public void setItemVisibility(int itemId, boolean visible) {
        if (visible) {
            showItem(itemId);
        } else {
            hideItem(itemId);
        }
    }

    public void showItem(int id) {
        if (isItemVisible(id)) return;

        int index = getNewVisibleIndex(id);
        BSItem item = getItemById(id);
        item.setVisible(true);
        visibleItems.add(index, item);
        notifyItemInserted(index);
    }

    private int getNewVisibleIndex(int itemId) {
        boolean foundItem = false;
        for (int i = bsItems.size() - 1; i >= 0; i--) {
            if (foundItem) {
                if (visibleItems.contains(bsItems.get(i))) {
                    return visibleItems.indexOf(bsItems.get(i)) + 1;
                }
                continue;
            }

            if (bsItems.get(i).getId() == itemId) {
                foundItem = true;
            }
        }
        return 0;
    }

    public void hideItem(int id) {
        if (!isItemVisible(id)) return;

        BSItem item = getItemById(id);
        item.setVisible(false);
        int position = visibleItems.indexOf(item);
        visibleItems.remove(item);
        notifyItemRemoved(position);
    }

    public boolean isItemVisible(int itemId) {
        BSItem item = getItemById(itemId);
        return visibleItems.contains(item);
    }

    public BSItem getItemById(int itemId) {
        for (BSItem item : bsItems) {
            if (item.getId() == itemId) return item;
        }
        return null;
    }

    public int getItemCountWithoutDividers() {
        int count = 0;
        for (BSItem item : visibleItems) {
            if (!item.isDivider()) count++;
        }
        return count;
    }

    public int getDividerCount() {
        int count = 0;
        for (BSItem item : visibleItems) {
            if (item.isDivider()) count++;
        }
        return count;
    }
}