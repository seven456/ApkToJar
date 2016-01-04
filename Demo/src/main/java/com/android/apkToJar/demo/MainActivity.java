package com.android.apkToJar.demo;

import android.app.Activity;
import android.os.Bundle;

import com.android.apkToJar.demoLib.UIManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        ViewGroup view = (ViewGroup) ApkToJar.getView(this, R2.layout.apktojar_activity_main);
//        setContentView(view);

        UIManager.getInstance().showMain(this);
    }
}
