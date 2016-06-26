package com.bupt.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bupt.library.picturegallery.PictureGalleryView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    PictureGalleryView mPictureGalleryView;

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
        mPictureGalleryView.updateData("Test", imageList, describeList, null);
    }
}
