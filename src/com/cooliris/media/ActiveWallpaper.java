package com.cooliris.media;

import com.cooliris.wallpaper.RandomDataSource;
import com.cooliris.wallpaper.Slideshow;

import android.app.*;
import android.os.Bundle;

public class ActiveWallpaper extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Slideshow slideshow = new Slideshow(this);
        slideshow.setDataSource(new RandomDataSource());
        setContentView(slideshow);
    }
}
