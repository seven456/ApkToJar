package com.android.apk2jar.demolib;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;

public class Apk2Jar {

    public static View getView(Context context, String fileName) {
        try {
            XmlResourceParser parser = context.getAssets().openXmlResourceParser(fileName);
            return LayoutInflater.from(context).inflate(parser, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap getBitmap(Context context, String fileName) {
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            is = Apk2Jar.class.getResource("/" + fileName).openStream(); // 使用类加载器读取jar里面的资源；
            Bitmap temp = BitmapFactory.decodeStream(is);
            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            DisplayMetrics outMetrics = new DisplayMetrics();
            wm.getDefaultDisplay().getMetrics(outMetrics);
            if (outMetrics.densityDpi > DisplayMetrics.DENSITY_XXHIGH) {
                Matrix matrix = new Matrix();
                matrix.postScale(2.0F, 2.0F);
                bitmap = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);
                temp.recycle();
            } else if ((outMetrics.densityDpi > DisplayMetrics.DENSITY_XHIGH) && (outMetrics.densityDpi <= DisplayMetrics.DENSITY_XXHIGH)) {
                Matrix matrix = new Matrix();
                matrix.postScale(1.5F, 1.5F);
                bitmap = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), matrix, true);
                temp.recycle();
            } else {
                bitmap = temp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    public static Drawable getBitmapDrawable(Context context, String drawableNormal, String drawablePressed) {
        StateListDrawable stalistDrawable = new StateListDrawable();
        Bitmap normalBitmap = getBitmap(context, drawableNormal);
        Bitmap pressedBitmap = getBitmap(context, drawablePressed);
        stalistDrawable.addState(new int[]{android.R.attr.state_pressed}, new BitmapDrawable(context.getResources(), pressedBitmap));
        stalistDrawable.addState(new int[]{}, new BitmapDrawable(context.getResources(), normalBitmap));
        return stalistDrawable;
    }
}