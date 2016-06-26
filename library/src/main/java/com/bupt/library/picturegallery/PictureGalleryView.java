package com.bupt.library.picturegallery;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bupt.library.R;
import com.bupt.library.pinch2zoom.GestureImageViewTouchListener;

import java.util.ArrayList;


/**
 * 采用recyclerview的横向滑动图集
 * Created by lishuo on 16/5/11.
 */
public class PictureGalleryView extends FrameLayout {
    private static final String TAG = PictureGalleryView.class.getSimpleName();

    private RecyclerGallery mRecyclerGallery;
    private TextView mDescribeContent;
    private TextView mDescribeTitle;
    private ScrollView mDescribeContainer;

    private PictureGalleryAdapter mAdapter;

    private ArrayList<String> mImageList = new ArrayList<>();  //图片地址
    private ArrayList<String> mDescribeList = new ArrayList<>();  //图片信息地址
    private ArrayList<String> relatedGallery = new ArrayList<>();
    private String mTitle;

    private boolean isDescribeContainerVisible = true; //描述文字可见

    private ImageClickListener mImageClickListener;

    private float mDefaultPageNumSize = 10.0f;  //默认总页数字号
    private float mDefaultTitleSize = 14.0f;   //默认title字号

    private LinearLayoutManager mLinearLayoutManager;

    private boolean isBackgroundTransparent = true;

    public PictureGalleryView(Context context) {
        super(context);
        init();
    }

    public PictureGalleryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PictureGalleryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public PictureGalleryView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.picture_gallery_view, this, true);
        mLinearLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        mRecyclerGallery = (RecyclerGallery) findViewById(R.id.recyclerGallery);
        mRecyclerGallery.setLayoutManager(mLinearLayoutManager);
        mRecyclerGallery.setHasFixedSize(true);
        mRecyclerGallery.setClickable(true);
        mRecyclerGallery.setLongClickable(true);
        mAdapter = new PictureGalleryAdapter();
        mRecyclerGallery.setAdapter(mAdapter);
        mDescribeContent = (TextView) findViewById(R.id.describeContent);
        mDescribeTitle = (TextView) findViewById(R.id.describeTitle);
        mDescribeContainer = (ScrollView) findViewById(R.id.describeContainer);
        //当在文字上滑动时,屏蔽掉gallery的左右滑动手势
        mDescribeContainer.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                PictureGalleryView.this.requestDisallowInterceptTouchEvent(true);
                return false;
            }
        });
        mRecyclerGallery.addOnPageChangedListener(new RecyclerGallery.OnPageChangedListener() {
            @Override
            public void OnPageChanged(int oldPosition, int newPosition) {
                if (newPosition < mDescribeList.size()) {
                    if(isDescribeContainerVisible) {
                        mDescribeContainer.setVisibility(VISIBLE);
                    }
                    setDescribeData(newPosition, mDescribeList.size());
                } else {
                    mDescribeContainer.setVisibility(GONE);
                }
            }
        });
    }

    public void updateData(String title, ArrayList<String> imageList, ArrayList<String> describeList, ArrayList<String> relatedGallerie) {
        updateTitle(title);
        updateImages(imageList);
        updateDescribes(describeList);
        updateRelatedGalleries(relatedGallerie);
        mAdapter.updateGallery(imageList, relatedGallerie);
        if(mDescribeList.size() > 0) {
            setDescribeData(0, mDescribeList.size());
        }
    }

    public void updateTitle(String title) {
        mTitle = title;
    }

    public void updateImages(ArrayList<String> imageList) {
        if (imageList != null) {
            mImageList = imageList;
        }
    }

    public void updateDescribes(ArrayList<String> describeList) {
        if (describeList != null) {
            mDescribeList = describeList;
        }
    }

    public void updateRelatedGalleries(ArrayList<String> relatedGalleries) {
        if (relatedGalleries != null) {
            this.relatedGallery = relatedGalleries;
        }
    }

    public void resetPictureGallery() {
        if(mAdapter.getItemCount() > 0) {
            mLinearLayoutManager.scrollToPositionWithOffset(0, 0);
        }
    }

    public void updateFontSize() {
        setDescribeData(mRecyclerGallery.getCurrentPosition(), mDescribeList.size());
    }

    /**
     * 设置字体样式
     *
     * @param position
     * @param length
     */
    private void setDescribeData(int position, int length) {
        int start;
        int end;
        if (length < 10) { //页数小于10的时候
            start = 1;
            end = 3;
        } else if (length < 100) { //页数小于100大于10的时候
            if (position + 1 < 10) {
                start = 1;
                end = 4;
            } else {
                start = 2;
                end = 5;
            }
        } else {  //页数大于100小于1000的时候，应该不会超过1000
            if (position + 1 < 10) {
                start = 1;
                end = 5;
            } else if (position + 1 < 100) {
                start = 2;
                end = 6;
            } else {
                start = 3;
                end = 7;
            }
        }
        SpannableString s = new SpannableString("" + (position + 1) + "/" + length + "    " + mTitle);
        s.setSpan(new AbsoluteSizeSpan(dp2px(mDefaultPageNumSize)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mDescribeTitle.setText(s);
        mDescribeContent.setText(mDescribeList.get(position));
        mDescribeContainer.fullScroll(View.FOCUS_UP); //scrollview scroll到顶部
    }

    public int dp2px(float dpValue) {
        final float scale = getContext().getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
    /**
     * gallery适配器
     */
    public class PictureGalleryAdapter extends RecyclerGalleryAdapter<RecyclerView.ViewHolder> {
        private ArrayList<String> imageList = new ArrayList<>();
        private ArrayList<String> relatedGalleries = new ArrayList<>();
        private static final int PICTURE_TYPE = 0;
        private static final int RECOMMEND_TYPE = 1;
        private SlideViewItem slideViewItem;

        public PictureGalleryAdapter() {
        }

        public void updateGallery(ArrayList<String> imageList, ArrayList<String> relatedGalleries) {
            this.imageList = imageList;
            this.relatedGalleries = relatedGalleries;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            if (imageList.size() == 0) {
                return 0;
            }
            if(relatedGallery.size() == 0) { //如果没有相关图集,则不加一
                return imageList.size();
            }
            return imageList.size() + 1;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case PICTURE_TYPE: {
                    View view = LayoutInflater.from(getContext()).inflate(R.layout.recycler_gallery_picture_item, parent, false);
                    return new PictureViewHolder(view);
                }
                case RECOMMEND_TYPE: {
                    View view = LayoutInflater.from(getContext()).inflate(R.layout.recycler_gallery_recommend_item, parent, false);
                    return new RelatedViewHolder(view);
                }
                default:
                    return null;
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            super.onBindViewHolder(holder, position);
            if (holder instanceof PictureViewHolder) {
                PictureViewHolder pictureViewHolder = (PictureViewHolder) holder;
                pictureViewHolder.mImage.setImageUrl(imageList.get(position));
                if (position == 0) {
                    slideViewItem = pictureViewHolder.mImage;
                }
            } else {
                bindRelatedGallery(holder);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == imageList.size()) {
                return RECOMMEND_TYPE;
            }
            return PICTURE_TYPE;
        }

        @Override
        public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
        }

        private void bindRelatedGallery(RecyclerView.ViewHolder holder) {
            RelatedViewHolder relatedViewHolder = (RelatedViewHolder) holder;
            if(relatedGallery != null) {
                if (relatedGallery.size() >= 1) {
                    Glide.with(getContext()).load(relatedGallery.get(0)).fitCenter().crossFade().into(relatedViewHolder.mTopLeftImage);
                }
                if (relatedGallery.size() >= 2) {
                    Glide.with(getContext()).load(relatedGallery.get(1)).fitCenter().crossFade().into(relatedViewHolder.mTopRightImage);
                }
                if (relatedGallery.size() >= 3) {
                    Glide.with(getContext()).load(relatedGallery.get(2)).fitCenter().crossFade().into(relatedViewHolder.mMiddleImage);
                }
                if (relatedGallery.size() >= 4) {
                    Glide.with(getContext()).load(relatedGallery.get(3)).fitCenter().crossFade().into(relatedViewHolder.mBottomLeftImage);
                }
                if (relatedGallery.size() >= 5) {
                    Glide.with(getContext()).load(relatedGallery.get(4)).fitCenter().crossFade().into(relatedViewHolder.mBottomRightImage);
                }
            }
        }

        @Override
        public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
            super.onViewDetachedFromWindow(holder);
            //当图片不可见的时候，重置图片为原来大小
            if (holder instanceof PictureViewHolder) {
                ((PictureViewHolder) holder).mImage.resetToOriginScale();
            }
        }

        public boolean isAlignLeftEdge() {
            if(slideViewItem != null) {
                return slideViewItem.isAlignLeftEdge();
            }
            return false;
        }

    }

    public interface ImageClickListener {
        void onClick();
    }

    public void setImageClickListener(ImageClickListener imageClickListener) {
        mImageClickListener = imageClickListener;
    }

    /**
     * 图片类型的viewholder
     */
    public class PictureViewHolder extends RecyclerView.ViewHolder {
        public SlideViewItem mImage;

        public PictureViewHolder(View itemView) {
            super(itemView);
            mImage = (SlideViewItem) itemView.findViewById(R.id.gallery_image);
            mImage.setRecycle(false);
            mImage.setMaxScale(3);
            mImage.setMidScale(1.75f);
            mImage.setMinScale(0.75f);
            mImage.setZoomType(GestureImageViewTouchListener.threeLevelZoom);
//            mImage.setImageOnLongClickListener(new OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    onSave(v);
//                    return true;
//                }
//            });
            mImage.setImageOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(isDescribeContainerVisible) {
                        doHideAnimation();
                        isDescribeContainerVisible = false;
                    }else {
                        doShowAnimation();
                        isDescribeContainerVisible = true;
                    }
                    if (mImageClickListener != null) {
                        mImageClickListener.onClick();
                    }
                }
            });
        }

        private void doHideAnimation() {
            mDescribeContainer.setVisibility(GONE);
            final Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
            mDescribeContainer.startAnimation(animation);

        }

        private void doShowAnimation() {
            mDescribeContainer.setVisibility(VISIBLE);
            final Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
            mDescribeContainer.startAnimation(animation);
        }

    }

    /**
     * 相关图集的viewholder
     */
    public class RelatedViewHolder extends RecyclerView.ViewHolder {

        public ImageView mTopLeftImage;
        public ImageView mTopRightImage;
        public ImageView mMiddleImage;
        public ImageView mBottomLeftImage;
        public ImageView mBottomRightImage;
        public TextView mTopLeftText;
        public TextView mTopRightText;
        public TextView mMiddleText;
        public TextView mBottomLeftText;
        public TextView mBottomRightText;

        public RelatedViewHolder(View itemView) {
            super(itemView);
            mTopLeftImage = (ImageView) itemView.findViewById(R.id.topLeftImage);
            mTopRightImage = (ImageView) itemView.findViewById(R.id.topRightImage);
            mMiddleImage = (ImageView) itemView.findViewById(R.id.middleImage);
            mBottomLeftImage = (ImageView) itemView.findViewById(R.id.bottomLeftImage);
            mBottomRightImage = (ImageView) itemView.findViewById(R.id.bottomRightImage);
            mTopLeftText = (TextView) itemView.findViewById(R.id.topLeftText);
            mTopRightText = (TextView) itemView.findViewById(R.id.topRightText);
            mMiddleText = (TextView) itemView.findViewById(R.id.middleText);
            mBottomLeftText = (TextView) itemView.findViewById(R.id.bottomLeftText);
            mBottomRightText = (TextView) itemView.findViewById(R.id.bottomRightText);
        }
    }

    private float mLastMotionX;
    private VelocityTracker mVelocityTracker = null;
    private float mVelocity = 0;
    private OnSwipeOffListener mOnSwipeOffListener;



    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = ev.getX();
                startVelocityTracker(ev);
                break;
            case MotionEvent.ACTION_UP:
                int position = mRecyclerGallery.getCurrentPosition();
                if (position == 0 && mAdapter.isAlignLeftEdge()) {
                    if (ev.getX() - mLastMotionX > 100 && mVelocity > 1000) {
                        if(mOnSwipeOffListener != null) {
                            mOnSwipeOffListener.onSwipeRight();
                        }
                    }
                }else if(position == mAdapter.getItemCount() - 1) {
                    if (ev.getX() - mLastMotionX < -100 && mVelocity < -1000) {
                        if(mOnSwipeOffListener != null) {
                            mOnSwipeOffListener.onSwipeLeft();
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                caculateVelocityTracker(ev);
                break;
            case MotionEvent.ACTION_CANCEL:
                recycleVelocityTracker();

        }
        return super.onInterceptTouchEvent(ev);
    }

    private void startVelocityTracker(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
        mVelocityTracker.addMovement(event);
    }

    private void caculateVelocityTracker(MotionEvent event) {
        mVelocityTracker.addMovement(event);
        mVelocityTracker.computeCurrentVelocity(1000);
        mVelocity = VelocityTrackerCompat.getXVelocity(mVelocityTracker, event.getPointerId(event.getActionIndex()));
    }

    private void recycleVelocityTracker() {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    /**
     * 在图集首页和最后一页左右滑动的接口
     */
    public interface OnSwipeOffListener {
        void onSwipeRight();
        void onSwipeLeft();
    }

    public void setOnSwipeOffListener(OnSwipeOffListener onSwipeOffListener) {
        mOnSwipeOffListener = onSwipeOffListener;
    }
}
