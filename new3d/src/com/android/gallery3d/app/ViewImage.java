package com.android.gallery3d.app;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.os.Bundle;

import com.android.gallery3d.R;
import com.android.gallery3d.ui.GLRootView;
import com.android.gallery3d.ui.ImageViewer;

import java.util.ArrayList;

public class ViewImage extends Activity {

    private static final int BACKUP_SIZE = 512;
    private static final String FILE_NAME = "/sdcard/image.jpg";

    private GLRootView mGLRootView;
    private Bitmap mScaledBitmaps[];
    private Bitmap mBackupBitmap;
    private ImageViewer mImageViewer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mGLRootView = (GLRootView) findViewById(R.id.gl_root_view);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(FILE_NAME, options);
        mScaledBitmaps = getScaledBitmaps(bitmap);
        mBackupBitmap = getBackupBitmap(bitmap);
        mImageViewer = new ImageViewer(this, mScaledBitmaps, mBackupBitmap);
        mGLRootView.setContentPane(mImageViewer);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGLRootView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImageViewer.releaseTiles();
        mGLRootView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for(Bitmap bitmap : mScaledBitmaps) {
            bitmap.recycle();
        }
        mBackupBitmap.recycle();
    }

    static private Bitmap getBackupBitmap(Bitmap bitmap) {
        Config config = bitmap.hasAlpha() ? Config.ARGB_8888 : Config.RGB_565;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scale = (float) BACKUP_SIZE / (width > height ? width : height);
        width = (int) (width * scale + 0.5f);
        height = (int) (height * scale + 0.5f);
        Bitmap result = Bitmap.createBitmap(width, height, config);
        Canvas canvas = new Canvas(result);
        canvas.scale(scale, scale);
        canvas.drawBitmap(bitmap, 0, 0, null);
        return result;
    }

    static private Bitmap[] getScaledBitmaps(Bitmap bitmap) {
        Config config = bitmap.hasAlpha() ? Config.ARGB_8888 : Config.RGB_565;

        int width = bitmap.getWidth() / 2;
        int height = bitmap.getHeight() / 2;

        ArrayList<Bitmap> list = new ArrayList<Bitmap>();
        list.add(bitmap);
        while (width > BACKUP_SIZE || height > BACKUP_SIZE) {
            Bitmap half = Bitmap.createBitmap(width, height, config);
            Canvas canvas = new Canvas(half);
            canvas.scale(0.5f, 0.5f);
            canvas.drawBitmap(bitmap, 0, 0, null);
            width /= 2;
            height /= 2;
            bitmap = half;
            list.add(bitmap);
        }
        return list.toArray(new Bitmap[list.size()]);
    }

}
