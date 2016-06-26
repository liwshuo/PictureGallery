package com.bupt.picturegallery;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private PictureGalleryView mPictureGalleryView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPictureGalleryView = (PictureGalleryView) findViewById(R.id.gallery_view);
        ArrayList<String> imageList = new ArrayList<>();
        imageList.add("http://i3.go2yd.com/image.php?url=0DfNgbY9JQ");
        imageList.add("http://i3.go2yd.com/image.php?url=0DfNgbqA6S");
        imageList.add("http://i3.go2yd.com/image.php?url=0DfNgbldl5");
        ArrayList<String> describeList = new ArrayList<>();
        describeList.add("picture one");
        describeList.add("picture two");
        describeList.add("picture three");
        mPictureGalleryView.updateData(imageList, describeList, null);
//        mPictureGalleryView.setVisibility(View.GONE);
//        ImageView imageView = (ImageView) findViewById(R.id.image);
//        try {
//            Glide.with(this).load(new URL("http://i.imgur.com/DvpvklR.png")).placeholder(R.drawable.app_icon_small).centerCrop().crossFade().into(imageView);
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
    }
}
