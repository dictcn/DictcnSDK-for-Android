package com.haici.dict.sdk.demo_e;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class MyScrollView extends ScrollView {

	public MyScrollView(Context context) {
		super(context);
	}

	public MyScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MyScrollView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		HaiciCustomEditText child = getChildEditText();
		if(child != null){
			if(child.has_long_click){
				child.parent_scroll_y = getScrollY();
				child.onTouchEvent(ev);
				return false;
			}else{
				child.cancel_long_click();
			}	
		}
		return super.onTouchEvent(ev);
	}
	
	private HaiciCustomEditText getChildEditText(){
		HaiciCustomEditText child = null;
		if(getChildCount() == 0){
			return null;
		}
		try{
			child = (HaiciCustomEditText)getChildAt(0);
		}catch(Exception e){
			e.printStackTrace();
			child = null;
		}
		return child;
	}
}
