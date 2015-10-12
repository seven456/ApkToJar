package com.android.apk2jar.demolib;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.TextView;
import android.widget.Toast;

public class UIManager {
	private static UIManager sInstance;

	public static UIManager getInstance() {
		if (sInstance == null) {
			synchronized (UIManager.class) {
				if (sInstance == null) {
					sInstance = new UIManager();
				}
			}
		}
		return sInstance;
	}

	public Dialog showMain(final Context context) {
		View view = Apk2Jar.getView(context, R2.layout.apk2jar_activity_main);
		final TextView textView00 = (TextView) view.findViewWithTag("textview_00");
		textView00.setBackgroundDrawable(Apk2Jar.getBitmapDrawable(context, R2.drawable.apk2jar_button_bg_normal, R2.drawable.apk2jar_button_bg_pressed));
		textView00.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(v.getContext(), textView00.getText(), Toast.LENGTH_SHORT).show();
			}
		});
		final TextView textView01 = (TextView) view.findViewWithTag("textview_01");
		textView01.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(v.getContext(), textView01.getText(), Toast.LENGTH_SHORT).show();
			}
		});
		final TextView textView02 = (TextView) view.findViewWithTag("textview_02");
		textView02.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Toast.makeText(v.getContext(), textView02.getText(), Toast.LENGTH_SHORT).show();
			}
		});
		return show(context, view);
	}

	private Dialog show(Context context, View view) {
		Dialog dialog = new Dialog(context, android.R.style.Theme_Light_NoTitleBar);
		dialog.setContentView(view);
		dialog.show();
		return dialog;
	}
}