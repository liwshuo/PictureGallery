package com.bupt.library.picturegallery;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bupt.library.R;
import com.bupt.library.picturegallery.CircleProgress;
import com.bupt.library.pinch2zoom.GestureImageView;

import java.io.File;


public class SlideViewItem extends FrameLayout {
    private final static String TAG = "SlideViewItem";

    GestureImageView mImage = null;
    TextView mtxtFailed = null;
    CircleProgress mProgress = null;

    String mUrl = null;
    String mOriginalUrl = null;
    boolean mbCancel = false; //cancel download when destroy
    boolean mbLargeImageFind = false; //where large image exist
    boolean mbOriginalDownloaded = false; //original image download finished yet?

    String mImageFileName = null; //original size file name
    String mLargeName = null;  //640x320 file name
    String mStoragePath = null;

    private boolean mbEnableLowResolutionImagePreload = false;  //For Picture channel, load the low resolution image first, then load high resolution one.

    private OnImageDownloadListener mOnImageDownloadListener;

    public SlideViewItem(Context context) {
        this(context, null);
        init();
    }

    public SlideViewItem(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init();
    }

    public SlideViewItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setBackgroundColor(0);
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.slide_view_item, this, true);
        mImage = (GestureImageView) view.findViewById(R.id.image);
        mtxtFailed = (TextView) view.findViewById(R.id.txtFailed);
        mProgress = (CircleProgress) view.findViewById(R.id.progress);

    }

    /**
     * this function return the gesture imageview
     *
     * @return
     */
    public GestureImageView getImageView() {
        return mImage;
    }

    /**
     * this function return the image file name currently being displayed.
     *
     * @return
     */
    public String getImageFileName() {
        if (mbOriginalDownloaded) {
            return mImageFileName;
        } else if (mbLargeImageFind) {
            return mLargeName;
        }

        return null;
    }

    @TargetApi(11)
    public void setImageUrl(String url) {
        mUrl = url;
        Glide.with(getContext()).load(url).fitCenter().crossFade().into(mImage);
    }


    private Drawable getDrawable(int resid) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPurgeable = true; // bitmap can be purged to disk
        options.inInputShareable = true;

        Bitmap bm = BitmapFactory.decodeStream(getResources().openRawResource(resid));
        Drawable img = new BitmapDrawable(getResources(), bm);

        return img;
    }

    /**
     * this function is provided to release the resources
     */
    public void onDestroy() {
        if (mImage != null) {
            mImage.setImageResource(-1);
        }
        mbCancel = true;
    }


    private Bitmap getBitmap(String fileName) {
        Bitmap bitmap = null;
        File file = new File(fileName);
        if (!file.exists()) return null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(fileName, options);
            int max = Math.max(options.outWidth, options.outHeight);
            int sampleRate = 1;
            while (max > 2048) {
                max = max >> 1;
                sampleRate = sampleRate << 1;
            }
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleRate;
            bitmap = BitmapFactory.decodeFile(fileName, options);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private void showImage(String fileName) {
        Bitmap bitmap = getBitmap(fileName);

//      try {
//          bitmap = BitmapFactory.decodeFile(fileName);
//
//      } catch (OutOfMemoryError e) {
//      } catch (Exception e) {
//          e.printStackTrace();
//      }
////      mtxtFailed.setVisibility(View.VISIBLE);

        if (bitmap != null) {
            // setting downloaded into image view
            mImage.setImageResource(-1); //force it to recycle old image
            mImage.setImageBitmap(bitmap);
        } else {
            if (!mbLargeImageFind) {
                mtxtFailed.setVisibility(View.VISIBLE);
            }
        }
    }

    public void enableLowResolutionImagePreload() {
        mbEnableLowResolutionImagePreload = true;
    }

    public void setRecycle(boolean recycle) {
        mImage.setRecycle(recycle);
    }

    public void setMaxScale(float maxScale) {
        mImage.setMaxScale(maxScale);
//        mImage.setGestureTouchListenerMaxScale(maxScale);
    }

    public void setMinScale(float minScale) {
        mImage.setMinScale(minScale);
//        mImage.setGestureTouchListenerMinScale(minScale);
    }

    public void setMidScale(float midScale) {
        mImage.setMidScale(midScale);
//        mImage.setGestureTouchListenerMidScale(midScale);
    }

    public void setZoomType(int zoomType) {
        mImage.setZoomType(zoomType);
    }

    public void resetToOriginScale() {
        mImage.reset();
    }

    public void setImageOnClickListener(OnClickListener onClickListener) {
        mImage.setOnClickListener(onClickListener);
    }

    public void setImageOnLongClickListener(OnLongClickListener onLongClickListener) {
        mImage.setOnLongClickListener(onLongClickListener);
    }

    public void setOnImageDownloadListener(OnImageDownloadListener onImageDownloadListener) {
        mOnImageDownloadListener = onImageDownloadListener;
    }

    public interface OnImageDownloadListener {
        void onFinish(String fileName);
    }

    public boolean isAlignLeftEdge() {
        return mImage.isAlignLeftEdge();
    }
}
