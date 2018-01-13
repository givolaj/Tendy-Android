package com.atn.tendy.chat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.atn.tendy.R;
import com.atn.tendy.utils.Utils;
import com.nostra13.universalimageloader.core.ImageLoader;

public class ImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        try{
            String imageUrl = getIntent().getStringExtra("imageUrl");
            ImageLoader.getInstance().displayImage(imageUrl, (ImageView)findViewById(R.id.image), Utils.displayImageOptions);
        }catch (Exception e){e.printStackTrace();}
    }
}
