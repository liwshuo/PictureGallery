package com.bupt.picturegallery;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
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
import android.widget.ScrollView;
import android.widget.TextView;

import com.yidian.news.HipuApplication;
import com.yidian.news.HipuConstants;
import com.yidian.news.api.misc.ReadDocReportApi;
import com.yidian.news.data.card.Card;
import com.yidian.news.event.gallery.GalleryEvent;
import com.yidian.news.image.roundedimageview.RecyclerGalleryAdapter;
import com.yidian.news.ui.comment.BottomChannelUtil;
import com.yidian.news.ui.content.ContentViewToolBar;
import com.yidian.news.ui.content.NewsActivity;
import com.yidian.news.ui.content.SlideViewItem;
import com.yidian.news.ui.newslist.data.PictureGalleryCard;
import com.yidian.news.ui.pinch2zoom.GestureImageViewTouchListener;
import com.yidian.news.util.CustomizedToastUtil;
import com.yidian.news.util.Log;
import com.yidian.news.util.StorageUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;

import de.greenrobot.event.EventBus;

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

    private PictureGalleryCard mCard;
    private PictureGalleryAdapter mAdapter;

    private ArrayList<String> mImageList = new ArrayList<>();  //图片地址
    private ArrayList<String> mDescribeList = new ArrayList<>();  //图片信息地址

    private boolean isDescribeContainerVisible = true; //描述文字可见

    private ImageClickListener mImageClickListener;

    private ContentViewToolBar mToolbar;
    private View mBottomBar;  //NewsActivity中的底部tab
    private View mTopBar;     //NewsActivity中的顶部tab

    private int mViewedNum = 0;  //已浏览图片数量,实时日志使用
    private int mTotalNum;   //图集中所有图片数量,实时日志使用

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
        mViewedNum = 1;
        mRecyclerGallery.addOnPageChangedListener(new RecyclerGallery.OnPageChangedListener() {
            @Override
            public void OnPageChanged(int oldPosition, int newPosition) {
                if (newPosition < mDescribeList.size()) {
                    if(mViewedNum - 1 < newPosition) { //由于newPosition是从0开始的,需要减一来比较
                        mViewedNum = newPosition + 1;
                    }
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

    /**
     * 
     * @param card
     * @param channelId
     * @param activity
     * @param sourceType
     */
    public void setNewsData(PictureGalleryCard card, String channelId, NewsActivity activity, int sourceType) {
        this.mCard = card;
        PictureGalleryCard.ImageEntry[] galleryItems = card.gallery_items;
        for (int i = 0; i < galleryItems.length; i++) {
            PictureGalleryCard.ImageEntry entry = galleryItems[i];
            mImageList.add(entry.image);
            mDescribeList.add(entry.description);
        }
        if (mDescribeList.size() != 0) {
            setDescribeData(0, mDescribeList.size());
        }
        mAdapter.updateGallery(mImageList);
        resetPictureGallery();  //当切换夜间模式时,需要重置图集显示第一张图片
        mTotalNum = mImageList.size();
        setAddressBarChannel(card);
    }

    public void resetPictureGallery() {
        if(mAdapter.getItemCount() > 0) {
            mLinearLayoutManager.scrollToPositionWithOffset(0, 0);
        }
    }

    public void updateFontSize() {
        setDescribeData(mRecyclerGallery.getCurrentPosition(), mDescribeList.size());
    }

    private void setAddressBarChannel(PictureGalleryCard card){
        if (TextUtils.isEmpty(card.fullJsonContent)) return;
        try {
            JSONObject json = new JSONObject(card.fullJsonContent);
            BottomChannelUtil.initRecommendedChannels(mToolbar, json, null, mCard.id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
        SpannableString s = new SpannableString("" + (position + 1) + "/" + length + "    " + mCard.title);
        s.setSpan(new AbsoluteSizeSpan(dp2px(convertTextSize(mDefaultPageNumSize))), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mDescribeTitle.setText(s);
        mDescribeTitle.setTextSize(convertTextSize(mDefaultTitleSize));
        mDescribeContent.setText(mDescribeList.get(position));
        mDescribeContent.setTextSize(convertTextSize(mDefaultTitleSize));
        mDescribeContainer.fullScroll(View.FOCUS_UP); //scrollview scroll到顶部
    }

    private float convertTextSize(float textSize) {
        float deltaSize = 2.0f;
        float convertedTextSize = textSize;
        switch (HipuApplication.getApplication().getmFontSize()) {
            case HipuConstants.FONT_SIZE_SMALL:
                convertedTextSize = textSize - deltaSize;
                break;
            case HipuConstants.FONT_SIZE_LARGE:
                convertedTextSize = textSize + deltaSize;
                break;
            case HipuConstants.FONT_SIZE_SUPER_LARGE:
                convertedTextSize = textSize + deltaSize * 2;
                break;
        }
        return convertedTextSize;
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
        private String[] imageNameList;
        private static final int PICTURE_TYPE = 0;
        private static final int RECOMMEND_TYPE = 1;
        private SlideViewItem slideViewItem;

        public PictureGalleryAdapter() {
        }

        public void updateGallery(ArrayList<String> imageList) {
            this.imageList = imageList;
            imageNameList = new String[imageList.size()];
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            if (imageList.size() == 0) {
                return 0;
            }
            if(mCard.relatedGallery == null) { //如果没有相关图集,则不加一
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
                //记录文件名称,用于下载图片
                //TODO:下期上线该功能,不要删除
//                saveFileName(pictureViewHolder, position);
            } else {
//                bindRelatedGallery(holder);
            }
        }

        private void saveFileName(PictureViewHolder pictureViewHolder, final int position) {
            String fileName = pictureViewHolder.mImage.getImageFileName();
            if (fileName != null && new File(fileName).exists()) {
                imageNameList[position] = fileName;
                return;
            }
            imageNameList[position] = pictureViewHolder.mImage.getImageFileName();
            pictureViewHolder.mImage.setOnImageDownloadListener(new SlideViewItem.OnImageDownloadListener() {
                @Override
                public void onFinish(String fileName) {
                    imageNameList[position] = fileName;
                }
            });
        }

        //设置相关图集
        private void setRecommendView(YdRatioImageView imageView, TextView textView, final PictureGalleryCard.RelatedDocsEntry relatedDocsEntry, int width, int height) {
            imageView.setCustomizedImageSize(width, height);
            imageView.setImageUrl(relatedDocsEntry.image, StorageUtil.IMAGE_SIZE_CUSTOMIZED, false);
            textView.setText(relatedDocsEntry.title);
            imageView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    reportRelatedGalleryClick(relatedDocsEntry.docId);
                    NewsActivity.launchActivity((Activity) getContext(), relatedDocsEntry.docId, 0, 0, null, null, null, null, relatedDocsEntry.impId, Card.PageType.PictureGallery);
                }
            });
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
            //当相关图集页面出现时,发送实时日志,并展示下载相关图集图片
            if(holder instanceof RelatedViewHolder) {
                if(mRecyclerGallery.getCurrentPosition() == getItemCount() - 2) { //在attach时,currentposition其实是相关图集的前一页,因此需要总数减二来判断是否展现了相关图集
                    doViewDocReport();
                }
                bindRelatedGallery(holder);
            }
            //当有第一个图片展示出来时,设置background为黑色,盖住进度条
            if(isBackgroundTransparent) {
                findViewById(R.id.galleryBackground).setBackgroundColor(getContext().getResources().getColor(R.color.picture_gallery_background));
                isBackgroundTransparent = false;
            }
        }

        private void doViewDocReport() {
            if (Log.getLogLevel() <= android.util.Log.VERBOSE) {
                android.util.Log.d(TAG, "doViewDocReport: ");
            }
            ReadDocReportApi api = new ReadDocReportApi(null);
            if (mCard.relatedGallery != null) {
                api.reportRelatedGalleryViewed(mCard.relatedGallery, mCard, "news2news", mCard.id);
                api.dispatch();
            }
        }

        private void bindRelatedGallery(RecyclerView.ViewHolder holder) {
            RelatedViewHolder relatedViewHolder = (RelatedViewHolder) holder;
            ArrayList<PictureGalleryCard.RelatedDocsEntry> relatedGallery = mCard.relatedGallery;
            if(relatedGallery != null) {
                if (relatedGallery.size() >= 1) {
                    setRecommendView(relatedViewHolder.mTopLeftImage, relatedViewHolder.mTopLeftText,
                            relatedGallery.get(0), 300, 200);
                }
                if (relatedGallery.size() >= 2) {
                    setRecommendView(relatedViewHolder.mTopRightImage, relatedViewHolder.mTopRightText,
                            relatedGallery.get(1), 300, 200);
                }
                if (relatedGallery.size() >= 3) {
                    setRecommendView(relatedViewHolder.mMiddleImage, relatedViewHolder.mMiddleText,
                            relatedGallery.get(2), 600, 300);
                }
                if (relatedGallery.size() >= 4) {
                    setRecommendView(relatedViewHolder.mBottomLeftImage, relatedViewHolder.mBottomLeftText,
                            relatedGallery.get(3), 300, 200);
                }
                if (relatedGallery.size() >= 5) {
                    setRecommendView(relatedViewHolder.mBottomRightImage, relatedViewHolder.mBottomRightText,
                            relatedGallery.get(4), 300, 200);
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

        public String getCurrentImageName() {
            int position = mRecyclerGallery.getCurrentPosition();
            if(position < imageNameList.length) {
                return imageNameList[position];
            }
            return "";
        }

        public boolean isAlignLeftEdge() {
            if(slideViewItem != null) {
                return slideViewItem.isAlignLeftEdge();
            }
            return false;
        }
    }

    /**
     * 屏蔽掉YdNetworkImageView在detachfromwindow的时候执行回收的策略
     * @param imageView
     */
    private void setImageDiposed(YdRatioImageView imageView) {
        imageView.setDisposeImageOnDetach(false);
    }

    public interface ImageClickListener {
        void onClick();
    }

    public void setImageClickListener(ImageClickListener imageClickListener) {
        mImageClickListener = imageClickListener;
    }

    public void setToolbar(ContentViewToolBar toolbar){
        mToolbar = toolbar;
    }

    public void setTopBar(View topBar) {
        mTopBar = topBar;
    }
    public void setBottomBar(View bottomBar) {
        mBottomBar = bottomBar;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    /**
     * 发送实时日志
     * @param event
     */
    public void onEventMainThread(GalleryEvent event) {
        String docId = event.getDocId();
        String actionSrc = event.getActionSrc();
        int sourceType = event.getSourceType();
        long readTime = event.getReadTime();
        ReadDocReportApi api = new ReadDocReportApi(null);
        api.reportGalleryDuration(docId, mCard, sourceType, actionSrc, readTime, mViewedNum, mTotalNum);
        api.dispatch();
    }

    private void reportRelatedGalleryClick(String docId) {
        ReadDocReportApi api = new ReadDocReportApi(null);
        api.reportRelatedGalleryClick(docId, mCard, 0, "contentView", mCard.id);
        api.dispatch();
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
            final Animation hideTopBar = getAnimation(mTopBar, R.anim.fade_out, false);
            final Animation hideBottomBar = getAnimation(mBottomBar, R.anim.fade_out, false);
            final Animation hideDescribe = getAnimation(mDescribeContainer, R.anim.fade_out, false);
            if(mTopBar != null) {
                mTopBar.startAnimation(hideTopBar);
            }
            if (mBottomBar != null) {
                mBottomBar.startAnimation(hideBottomBar);
            }
            mDescribeContainer.startAnimation(hideDescribe);

        }

        private void doShowAnimation() {
            final Animation showTopBar = getAnimation(mTopBar, R.anim.fade_in, true);
            final Animation showDescribe = getAnimation(mDescribeContainer, R.anim.fade_in, true);
            final Animation showBottomBar = getAnimation(mBottomBar, R.anim.fade_in, true);
            if(mTopBar != null) {
                mTopBar.startAnimation(showTopBar);
            }
            mDescribeContainer.startAnimation(showDescribe);
            if(mBottomBar != null) {
                mBottomBar.startAnimation(showBottomBar);
            }
        }

        private Animation getAnimation(final View view, final int anim, final boolean isShow) {
            final Animation animation = AnimationUtils.loadAnimation(getContext(), anim);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    if (isShow && view != null) {
                        view.setVisibility(VISIBLE);
                    }
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if(!isShow && view != null) {
                        view.setVisibility(GONE);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            return animation;
        }
    }

    /**
     * 相关图集的viewholder
     */
    public class RelatedViewHolder extends RecyclerView.ViewHolder {

        public YdRatioImageView mTopLeftImage;
        public YdRatioImageView mTopRightImage;
        public YdRatioImageView mMiddleImage;
        public YdRatioImageView mBottomLeftImage;
        public YdRatioImageView mBottomRightImage;
        public TextView mTopLeftText;
        public TextView mTopRightText;
        public TextView mMiddleText;
        public TextView mBottomLeftText;
        public TextView mBottomRightText;

        public RelatedViewHolder(View itemView) {
            super(itemView);
            mTopLeftImage = (YdRatioImageView) itemView.findViewById(R.id.topLeftImage);
            mTopRightImage = (YdRatioImageView) itemView.findViewById(R.id.topRightImage);
            mMiddleImage = (YdRatioImageView) itemView.findViewById(R.id.middleImage);
            mBottomLeftImage = (YdRatioImageView) itemView.findViewById(R.id.bottomLeftImage);
            mBottomRightImage = (YdRatioImageView) itemView.findViewById(R.id.bottomRightImage);
            setImageDiposed(mTopLeftImage);
            setImageDiposed(mTopRightImage);
            setImageDiposed(mMiddleImage);
            setImageDiposed(mBottomLeftImage);
            setImageDiposed(mBottomRightImage);
            mTopLeftText = (TextView) itemView.findViewById(R.id.topLeftText);
            mTopRightText = (TextView) itemView.findViewById(R.id.topRightText);
            mMiddleText = (TextView) itemView.findViewById(R.id.middleText);
            mBottomLeftText = (TextView) itemView.findViewById(R.id.bottomLeftText);
            mBottomRightText = (TextView) itemView.findViewById(R.id.bottomRightText);
        }
    }

    public void onSave(View v) {
        if (!StorageUtil.isExternalStorageWritable()) {
            CustomizedToastUtil.showPrompt(R.string.sdcard_not_ready, false);
        }

        String imageFile = getCurrentImageFile();
        if (imageFile != null) {
            File f = new File(imageFile);
            if (!f.exists()) {
                return;
            }
        }
        // save to the camera photo path
        Date d = new Date();
        String fileName = String.format("yidian_%d%d%d%d%d%d", d.getYear(), d.getMonth(), d.getDate(), d.getHours(),
                d.getMinutes(), d.getSeconds());
        File baseFilePath = Environment.getExternalStorageDirectory();
        String fullName = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                + Environment.DIRECTORY_DCIM + "/" + fileName + ".jpg";
        String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/"
                + Environment.DIRECTORY_DCIM;
        if (!baseFilePath.exists()) {
            fullName = "/sdcard/" + Environment.DIRECTORY_DCIM + "/" + fileName + ".jpg";
            baseDir = "/sdcard/" + Environment.DIRECTORY_DCIM;
        }

        File f1 = new File(fullName);
        File f2 = new File(baseDir);
        if (!f2.exists()) {
            f2.mkdirs();
        }
        try {
            copyFile(imageFile, fullName);

            // call the media manager to scan the image file
            Uri uri = Uri.fromFile(new File(fullName));
            getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
            CustomizedToastUtil.showPrompt(getContext().getString(R.string.save_image_finish, fullName), true);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCurrentImageFile() {
        return mAdapter.getCurrentImageName();
    }

    private void copyFile(String org, String dest) throws Exception {
        File f = new File(org);
        File f1 = new File(dest);

        f1.createNewFile();
        FileInputStream in = new FileInputStream(f);
        FileOutputStream os = new FileOutputStream(f1);
        byte[] buff = new byte[2048];
        int len = 0;
        while ((len = in.read(buff)) > 0) {
            os.write(buff, 0, len);
        }
        in.close();
        os.close();
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
