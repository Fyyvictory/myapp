package com.example.imdemo.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imdemo.BuildConfig;
import com.example.imdemo.R;
import com.example.imdemo.entities.videoEntity;
import com.example.imdemo.videoUtils.ImageCache;
import com.example.imdemo.videoUtils.ImageResizer;
import com.example.imdemo.videoUtils.Utils;
import com.example.imdemo.widget.RecyclingImageView;
import com.hyphenate.util.DateUtils;
import com.hyphenate.util.EMLog;
import com.hyphenate.util.TextFormater;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by SH on 2017/1/5.
 */

public class ImageGridFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final String TAG = "ImageGridFragment";
    private int mImageThumbSize; // 图片尺寸
    private int mImageThumbSpacing; // 图片大小
    private ImageResizer mResize;
    List<videoEntity> list;
    ImageAdapter adapter;
    videoEntity entity;

    public static ImageGridFragment newInstance() {

        Bundle args = new Bundle();

        ImageGridFragment fragment = new ImageGridFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
        mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
        list = new ArrayList<videoEntity>();
        getVideoFile();
        adapter = new ImageAdapter(getActivity());
        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams();
        cacheParams.setMemCacheSizePercent(0.25f);  // 设置图片占APP所分配的总缓存的25%

        // The ImageFetcher takes care of loading images into our ImageView
        // children asynchronously
        mResize = new ImageResizer(getActivity(), mImageThumbSize);
        mResize.setmLoadingBitmap(R.drawable.em_empty_photo);
        mResize.addImagCache(getActivity().getSupportFragmentManager(), cacheParams);
    }

    private void getVideoFile() {
        ContentResolver contentResolver = getActivity().getContentResolver();
        Cursor cursor = contentResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Video.DEFAULT_SORT_ORDER);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                // ID:MediaStore.Audio.Media._ID
                int id = cursor.getInt(cursor
                        .getColumnIndexOrThrow(MediaStore.Video.Media._ID));

                // title：MediaStore.Audio.Media.TITLE
                String title = cursor.getString(cursor
                        .getColumnIndexOrThrow(MediaStore.Video.Media.TITLE));
                // path：MediaStore.Audio.Media.DATA
                String url = cursor.getString(cursor
                        .getColumnIndexOrThrow(MediaStore.Video.Media.DATA));

                // duration：MediaStore.Audio.Media.DURATION
                int duration = cursor
                        .getInt(cursor
                                .getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));

                // 大小：MediaStore.Audio.Media.SIZE
                int size = (int) cursor.getLong(cursor
                        .getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));

                entity = new videoEntity();
                entity.ID = id;
                entity.title = title;
                entity.filePath = url;
                entity.duration = duration;
                entity.size = size;
                list.add(entity);
            } while (cursor.moveToNext());
        }
        if (cursor != null) {
            cursor.close();
            cursor = null;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.image_grid_fragment, container, false);
        final GridView grid = (GridView) view.findViewById(R.id.fragment_gridimage);
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(this);
        grid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    if (!Utils.hasHoneycomb()) {
                        mResize.setPauseWork(true);
                    }
                } else {
                    mResize.setPauseWork(false);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });

        grid.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onGlobalLayout() {
                int numColumns = (int) Math.floor(grid.getWidth()
                        / (mImageThumbSize + mImageThumbSpacing));
                if (numColumns > 0) {
                    int columbWidth = (grid.getWidth() / numColumns) - mImageThumbSpacing;
                    adapter.setItemHeight(columbWidth);
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG,
                                "onCreateView - numColumns set to "
                                        + numColumns);
                    }
                    if (Utils.hasJellyBean()) {
                        grid.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                    } else {
                        grid.getViewTreeObserver()
                                .removeGlobalOnLayoutListener(this);
                    }
                }
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        mResize.setExitTasksEarly(false);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mResize.closeCache();
        mResize.clearCache();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mResize.setPauseWork(true);
        if (position == 0) {
            Intent intent = new Intent();
            intent.setClass(getActivity(), RecordVideoActivity.class);
            startActivityForResult(intent, 100);
        } else {
            videoEntity entity = list.get(position - 1);
            if (entity.size > 1024 * 1024 * 70) {
                String str = getResources().getString(R.string.temporary_does_not);
                Toast.makeText(getActivity(), "str", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = getActivity().getIntent().putExtra("path", entity.filePath)
                    .putExtra("dur", entity.duration);
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == 100) {
                Uri uri = data.getParcelableExtra("uri");
                String[] projects = new String[]{MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.DURATION};
                Cursor cursor = getActivity().getContentResolver().query(uri, projects, null,
                        null, null);
                int dur = 0;
                String filePath = null;
                if (cursor.moveToFirst()) {
                    filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                    dur = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                    EMLog.d(TAG, "duration:" + dur);
                }
                if (cursor != null) {
                    cursor.close();
                    cursor = null;
                }
                getActivity().setResult(Activity.RESULT_OK, getActivity().getIntent().putExtra("path", filePath).putExtra("dur", dur));
                getActivity().finish();
            }
        }
    }

    private class ImageAdapter extends BaseAdapter {

        private Context mCon;
        private int mItemHeight = 0;
        private RelativeLayout.LayoutParams mImageLayoutParams;

        public ImageAdapter(Context context) {
            super();
            mCon = context;
            mImageLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        @Override
        public int getCount() {
            return list.size() + 1;
        }

        @Override
        public Object getItem(int position) {
            return position == 0 ? null : list.get(position - 1);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;
            if (convertView == null) {
                convertView = View.inflate(parent.getContext(), R.layout.choose_griditem, null);
                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            if (viewHolder.recyImagview.getLayoutParams().height != mItemHeight) {
                viewHolder.recyImagview.setLayoutParams(mImageLayoutParams);
            }

            String str = getResources().getString(R.string.Video_footage);
            if (position == 0) {
                viewHolder.videoIcon.setVisibility(View.GONE);
                viewHolder.chattingLengthIv.setVisibility(View.GONE);
                viewHolder.chattingSizeIv.setText(str);
                viewHolder.recyImagview.setImageResource(R.drawable.em_actionbar_camera_icon);
            } else {
                viewHolder.videoIcon.setVisibility(View.VISIBLE);
                videoEntity entity1 = list.get(position - 1);
                viewHolder.chattingLengthIv.setVisibility(View.VISIBLE);

                viewHolder.chattingLengthIv.setText(DateUtils.toTime(entity1.duration));
                viewHolder.chattingSizeIv.setText(TextFormater.getDataSize(entity1.size));
                viewHolder.recyImagview.setImageResource(R.drawable.em_empty_photo);
                mResize.loadImage(entity1.filePath, viewHolder.recyImagview);
            }
            return convertView;
        }

        /**
         * 手动设置item的高度，当宽度已知的时候，可以把高度设置成match
         *
         * @param height
         */
        public void setItemHeight(int height) {
            if (height == mItemHeight)
                return;
            mItemHeight = height;
            mImageLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mItemHeight);
            mResize.setImageSize(height);
            notifyDataSetChanged();
        }

        class ViewHolder {
            protected RecyclingImageView recyImagview;
            protected ImageView videoIcon;
            protected TextView chattingSizeIv;
            protected TextView chattingLengthIv;
            protected LinearLayout videodata;

            ViewHolder(View rootView) {
                initView(rootView);
            }

            private void initView(View rootView) {
                recyImagview = (RecyclingImageView) rootView.findViewById(R.id.recyimagview);
                videoIcon = (ImageView) rootView.findViewById(R.id.videoicon);
                chattingSizeIv = (TextView) rootView.findViewById(R.id.chatting_size_iv);
                chattingLengthIv = (TextView) rootView.findViewById(R.id.chatting_length_iv);
                videodata = (LinearLayout) rootView.findViewById(R.id.videodata);
                recyImagview.setLayoutParams(mImageLayoutParams);
                recyImagview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        }
    }
}
