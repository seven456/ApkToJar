package com.android.apk2jar.demo;

import android.app.Activity;
import android.os.Bundle;

import com.android.apk2jar.demolib.UIManager;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        ViewGroup view = (ViewGroup) Apk2Jar.getView(this, R2.layout.apk2jar_activity_main);
//        setContentView(view);

        UIManager.getInstance().showMain(this);
    }
}
