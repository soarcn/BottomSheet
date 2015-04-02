package com.cocosw.bottomsheet;

import android.support.annotation.Nullable;

/**
 * Interface for events triggered by BottomSheet
 * Created by Kenny Campagna 4/2/2015
 */
public interface BottomSheetListener {

    /**
     * Called when the Sheet has been dismissed
     *
     * @param object Optional object that can be passed for BottomSheet
     */
    void onSheetDismissed(@Nullable Object object);

    /**
     * Called when an item is clicked from a Sheet
     *
     * @param id     The id of item clicked
     * @param object Optional object that can be passed for BottomSheet
     */
    void onItemClicked(int id, @Nullable Object object);

    /**
     * Called when the Sheet is first shown
     *
     * @param object Optional object that can be passed for BottomSheet
     */
    void onSheetShown(@Nullable Object object);
}
