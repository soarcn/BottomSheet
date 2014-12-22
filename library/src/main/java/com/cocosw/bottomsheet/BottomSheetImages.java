package com.cocosw.bottomsheet;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Created by imran on 22/12/14 9:49 PM in com.cocosw.bottomsheet.
 */
public class BottomSheetImages extends DialogFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static BottomSheetImages sBottomSheetImages;

    public static BottomSheetImages getInstance() {
        if (sBottomSheetImages == null) {
            sBottomSheetImages = new BottomSheetImages();
        }
        return sBottomSheetImages;
    }

    RecyclerView mRecyclerView;
    ImageAdapter mImageAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_images, container, false);
        loadRecyclerView(view);
        return view;
    }

    private void loadRecyclerView(View view) {
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recycle_images);
        GridLayoutManager manager = new GridLayoutManager(view.getContext(), 3);
        manager.setSpanSizeLookup(new GridLayoutManager.DefaultSpanSizeLookup());
        mRecyclerView.setLayoutManager(manager);
        mImageAdapter = new ImageAdapter(null);
        mRecyclerView.setAdapter(mImageAdapter);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final String orderBy = MediaStore.Images.Media.DATE_TAKEN;

        return new CursorLoader(getActivity(), uri, ImageQuery.PROJECTION, null,
                null, orderBy + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.getCount() == 0) {
            return;
        }
        mImageAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private class ImageAdapter extends RecyclerView.Adapter<ImageHolder> {

        Cursor cursor;

        ImageAdapter(Cursor cursor) {
            this.cursor = cursor;
        }

        public void swapCursor(Cursor cursor) {
            this.cursor = cursor;
            notifyDataSetChanged();
        }

        @Override
        public ImageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image, parent, false);
            return new ImageHolder(view);
        }

        @Override
        public void onBindViewHolder(ImageHolder holder, int position) {
            cursor.moveToPosition(position);
            String uri = cursor.getString(ImageQuery.DATA);
            holder.imageView.setImageURI(Uri.parse(uri));
        }

        @Override
        public int getItemCount() {
            return cursor == null ? 0 : cursor.getCount();
        }
    }

    private class ImageHolder extends RecyclerView.ViewHolder {

        ImageView imageView;

        public ImageHolder(View itemView) {
            super(itemView);
            imageView = (ImageView) itemView.findViewById(R.id.image);
        }
    }

    private interface ImageQuery {
        String[] PROJECTION = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA
        };

        int _ID = 0;
        int DATA = 1;
    }
}
