package com.haici.dict.sdk.demo_e;

import com.haici.dict.sdk.tool.HaiciCustomCallBack;
import com.haici.dict.sdk.tool.HaiciManager;
import com.haici.dict.sdk.tool.HaiciSplitResult;
import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

public class TestActivity extends Activity{
	
	/**
	 * 是否全屏显示标识
	 */
	public static final boolean FULL_SCREEN = true;
	
	/**
	 * 标题
	 */
	private TextView titleTV;
	
	/**
	 * 自定义ScrollView
	 */
	private MyScrollView myScrollView;
	
	/**
	 * 自定义EditText
	 */
	private HaiciCustomEditText haiciEditText;
	
	/**
	 * 放大镜
	 */
	private ShaderView myShaderView;
	
	/**
	 * 海词查词接口管理类对象
	 */
	private HaiciManager manager = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		if(FULL_SCREEN){
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		}
		setContentView(R.layout.main);
		initData();
		initView();
	}
	
	/**
	 * 初始化全局变量
	 */
	private void initData(){
		//得到
		String haici_sdk_secret = "c685441980325f0596463f96dce98f1f";
		String haici_sdk_id = "sdk1";
		manager = HaiciManager.getInstance(this, haici_sdk_secret, haici_sdk_id);
	}
	
	/**
	 * 初始化界面控件
	 */
	private void initView(){
		titleTV = (TextView)findViewById(R.id.titleTV);
		myScrollView = (MyScrollView)findViewById(R.id.myScrollView);
		haiciEditText = (HaiciCustomEditText)findViewById(R.id.haiciEditText);
		myShaderView = (ShaderView)findViewById(R.id.myShaderView);
		
		haiciEditText.setCallback(callBack);
		haiciEditText.shaderView = myShaderView;
		haiciEditText.manager = manager;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		/**
		 * 关闭查词用到的词库包
		 */
		HaiciManager.closeCiku();
	}

	private HaiciCustomCallBack callBack = new HaiciCustomCallBack(){

		@Override
		public void splitWordResponse(HaiciSplitResult result) {
			if(manager == null){
				return;
			}
			int scroll_y = myScrollView.getScrollY();
			result.click_y -= scroll_y;
			result.click_up_y -= scroll_y;
			result.click_down_y -= scroll_y;
			int height = titleTV.getHeight();
			result.click_y += height;
			result.click_up_y += height;
			result.click_down_y += height;
			manager.searchWord(TestActivity.this, result.key, result.relatedKeys, result.click_x, result.click_y, result.click_up_y, result.click_down_y, FULL_SCREEN);
			
			//manager.searchWord(TestActivity.this, result.key, result.relatedKeys, FULL_SCREEN);
		}
	};
}
