package com.jzplusplus.glassonlineindicator;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class OnlineIndicatorService extends Service {

    private WindowManager windowManager;
    private TextView onlineIndicator;
    BroadcastReceiver broadcastReceiver;

    Timer timer;

    Handler handler;
    
    final int onlineColor = 0x4400FF00;
    final int offlineColor = 0x44FF0000;
    final int transparentColor = 0x00000000;
    
    int currentColor = onlineColor;

    @Override public IBinder onBind(Intent intent) {
        // Not used
        return null;
    }

    @Override public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        //Log.d("ONLINEINDICATOR", "Connected? = " + (cm.getActiveNetworkInfo()));

        onlineIndicator = new TextView(this);
        onlineIndicator.setWidth(210);
        onlineIndicator.setHeight(60);
        
        
        
        if(cm.getActiveNetworkInfo() == null) currentColor = offlineColor;
        else currentColor = onlineColor;
        
        onlineIndicator.setBackgroundColor(currentColor);
        
        //onlineIndicator.setBackgroundColor(Color.TRANSPARENT);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.CENTER;
        params.x = 2;
        params.y = 25;

        windowManager.addView(onlineIndicator, params);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent)
            {
                if (intent.getAction().compareTo(ConnectivityManager.CONNECTIVITY_ACTION) == 0) {
                	boolean noConn = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                	if(noConn) currentColor = offlineColor;
                    else currentColor = onlineColor;
                    
                    onlineIndicator.setBackgroundColor(currentColor);
                }
            }
        };

        registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Online indicator")
                .getNotification();

        startForeground(124, notification);

        scehduleFadeout();

        registerReceiver(new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                    	ObjectAnimator oa = ObjectAnimator.ofInt(onlineIndicator, "backgroundColor", transparentColor, currentColor);
                    	oa.setEvaluator(new ArgbEvaluator());
                    	oa.setDuration(1000);
                    	oa.start();
                        scehduleFadeout();
                    }
                });
            }
        }, new IntentFilter(Intent.ACTION_SCREEN_ON));
    }

    private void scehduleFadeout() {
        if (timer != null)
            timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                    	ObjectAnimator oa = ObjectAnimator.ofInt(onlineIndicator, "backgroundColor", currentColor, transparentColor);
                    	oa.setEvaluator(new ArgbEvaluator());
                    	oa.start();
                    }
                });
            }
        }, 2000);
    }

    @Override
    public void onDestroy() {

        stopForeground(true);

        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);

        super.onDestroy();
        if (onlineIndicator != null) windowManager.removeView(onlineIndicator);
    }

}
