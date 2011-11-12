/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cooliris.app;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.Toast;

import java.util.HashMap;
import java.util.TimeZone;

import com.cooliris.media.ReverseGeocoder;

/*
 *  TODO: consider adding HashMap<object, object> for globals globals
 *  TODO: hook up other activity classes to App (besides Gallery and Search)
 */
public class App {
	static private final HashMap<Context, App> mMap = new HashMap<Context, App>();
		
	static public App get(Context context) {
		return mMap.get(context);
	}
	
    public static final TimeZone CURRENT_TIME_ZONE = TimeZone.getDefault();
    public static float PIXEL_DENSITY = 0.0f;
    public static int PIXEL_DENSITY_DPI = 0;

    /*
     * The upscaling factor used for the respatch.
     * This is the value that is used to change the scaling of the Bitmap loader.
     * Values :
     * 		0 -> Determine automatically
     * 		1 -> Disable respatch
     * 		n -> Custom value to use (note : most devices can't handle more than 2 by default)
     */
    public static int RESPATCH_FACTOR = 0;
    /*
     * The divisor for adaptive respatch
     * Defines the number that is used to divide the heap size in order to obtain the
     * respatch factor.
     * Most devices should be able to handle 9 memory-wise, but the images returned
     * might be somewhat CPU-intensive. Adjust accordingly.
     */
    public static final int RESPATCH_DIVISOR = 9;

    private final Context mContext;

    private final Handler mHandler;	
    
    private ReverseGeocoder mReverseGeocoder = null;
    
    private boolean mPaused = false;
    private Toast mToast;
    
	public App(Context context) {
		// register
		mMap.put(context, this);
		
		mContext = context;
				
		if(PIXEL_DENSITY == 0.0f) {
			DisplayMetrics metrics = new DisplayMetrics();
			((Activity)mContext).getWindowManager().getDefaultDisplay().getMetrics(metrics);
			PIXEL_DENSITY = metrics.density;
			PIXEL_DENSITY_DPI = metrics.densityDpi;
		}
		
		/*
		 *  Adaptive ResPatch by Petar Segina <psegina@ymail.com>
		 *  By default, Gallery3D is made to load very low resolution images in
		 *  order to avoid running into outOfMemory situations. Since some devices come with enormous
		 *  amounts of memory and processing power, it is pointless not to use those resources. So, 
		 *  depending on the maximum heap size available, we want to determine the highest
		 *  possible upscale factor. This is calculated from the heap size and then used in
		 *  com.cooliris.media.Utils.java to return higher resolution images.
		 */
		if(RESPATCH_FACTOR == 0){
			RESPATCH_FACTOR = (int) (Runtime.getRuntime().maxMemory()/1048576) / RESPATCH_DIVISOR;
		}

        mHandler = new Handler(Looper.getMainLooper());
		
	    mReverseGeocoder = new ReverseGeocoder(mContext);					
	}
		
	public void shutdown() {
        dismissToast();
        mReverseGeocoder.shutdown();
        
        // unregister
        mMap.remove(mContext);
	}
	
    public Context getContext() {
        return mContext;
    }	
	
    public Handler getHandler() {
        while (mHandler == null) {
            // Wait till the handler is created.
            ;
        }
        return mHandler;
    }
    
    public ReverseGeocoder getReverseGeocoder() {
        return mReverseGeocoder;
    }    
    
    public boolean isPaused() {
    	return mPaused;
    }
	
//    public void onCreate(Bundle savedInstanceState) {
//    }
//
//    public void onStart() {
//    }
//    
//    public void onRestart() {
//    }

    public void onResume() {
    	mPaused = false;
    }

    public void onPause() {
    	mReverseGeocoder.flushCache();
    	mPaused = true;
    }

//    public void onStop() {
//    	  
//    }
//
//    public void onDestroy() {
//    }

    public void showToast(final String string, final int duration, final boolean centered) {
        mHandler.post(new Runnable() {
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
                mToast = Toast.makeText(mContext, string, duration);
                if (centered) {
                    mToast.setGravity(Gravity.CENTER, 0, 0);
                }
                mToast.show();
            }
       });
    }

    public void dismissToast() {
        mHandler.post(new Runnable() {
            public void run() {
                if (mToast != null) {
                    mToast.cancel();
                }
            }
        });
    }
}
