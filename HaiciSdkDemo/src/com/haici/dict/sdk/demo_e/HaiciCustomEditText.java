package com.haici.dict.sdk.demo_e;

import com.haici.dict.sdk.tool.HaiciCustomCallBack;
import com.haici.dict.sdk.tool.HaiciManager;
import com.haici.dict.sdk.tool.HaiciSplitResult;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.Editable;
import android.text.Layout;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.ImageSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

public class HaiciCustomEditText extends EditText {

	
	public static final String TAG = "HaiciCustomEditText";
	
	//长按响应时间
	private static final long LONG_CLICK_RESPONSE_TIME = 300;
	
	private static final int LONG_CLICK = 1;
	
	private HaiciCustomCallBack callback = null;//回调接口
	
	private Spanned spannableString = null;//特殊字符
	
	private HaiciSplitResult hsResult = null;//分词结果
	
	public boolean has_long_click = false;//是否触发了长按事件
	
	private int action_move_times = 0;//ACTION_MOVE事件触发的次数
	
	private int click_x = 0;//ACTION_MOVE事件触发点的x坐标
	
	private int click_y = 0;//ACTION_MOVE事件触发点的y坐标
	
	private int curSelectStart = -1;//当前选中文本的起始位置
	
	private int curSelectEnd = -1;//当前选中文本的结束位置
	
	public ShaderView shaderView = null;
	
	private MyScrollView parentView = null;//editext的父view
	
	public int parent_scroll_y = 0;//父view的滚动条位置
	
	public HaiciManager manager = null;
	
	public HaiciCustomEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public HaiciCustomEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public HaiciCustomEditText(Context context) {
		super(context);
		init();
	}
	
	private void init(){
		setBackgroundDrawable(null);
		setCursorVisible(false);
		setGravity(Gravity.TOP|Gravity.LEFT);
	}
	
	/**
	 * 设置TextView的显示文本
	 * @param plainText
	 */
	public void setPlainText(String plainText){
		setText(plainText);
		this.spannableString = null;
	}
	
	/**
	 * 设置TextView的显示文本
	 * @param spannableString
	 */
	public void setSpannableString(Spanned spannableString){
		setText(spannableString);
		this.spannableString = spannableString;
	}

	/**
	 * 设置分词回调接口
	 * @param callback
	 */
	public void setCallback(HaiciCustomCallBack callback) {
		this.callback = callback;
	}
	
	@Override    
    protected void onCreateContextMenu(ContextMenu menu) {    
        //不做任何处理，为了阻止长按的时候弹出上下文菜单    
    }    
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		switch(event.getAction()){
		case MotionEvent.ACTION_DOWN:
			handler_action_down(event);
			break;
		case MotionEvent.ACTION_MOVE:
			handler_action_move(event);
			break;
		case MotionEvent.ACTION_UP:
			handler_action_up(event);
			break;
		}
		return true;
	}
	
	/**
	 * 处理ACTION_DOWN事件
	 * @param event
	 */
	private void handler_action_down(MotionEvent event){
		//取消文本的选中
		removeSelect();
		//重置相关参数
		parent_scroll_y = 0;
		hsResult = null;
		has_long_click = false;
		action_move_times = 0;
		curSelectStart = -1;
		curSelectEnd = -1;
		if(parentView == null){
			parentView = (MyScrollView)getParent();
		}
		if(shaderView != null && shaderView.getVisibility() == VISIBLE){
			shaderView.setVisibility(View.VISIBLE);
		}
		//按下点的坐标
		click_x = (int)event.getX();
		click_y = (int)event.getY();
		//发送长按操作请求,在特定时间后执行
		mHandler.sendEmptyMessageDelayed(LONG_CLICK, LONG_CLICK_RESPONSE_TIME);
	}
	
	/**
	 * 处理ACTION_MOVE事件
	 * @param event
	 */
	private void handler_action_move(MotionEvent event){
		if(!has_long_click){
			return;
		}
		int x = (int)event.getX();
		int y = (int)event.getY();
		//如果移动到的点的x坐标(或y坐标)与按下点的x坐标(或y坐标)的距离大于一定值时，记为一次有效的移动事件
		if(Math.abs(x-click_x) > 10 || Math.abs(y-click_y) > 20){
			action_move_times++;
		}
		//当move事件由parent强制调用的时,修正y坐标
		y += parent_scroll_y;
		
		//当有效的移动次数小于10次
		if(action_move_times < 10){
			return;
		}
		//计算移动点对应的字符并分词
		hsResult = calculateTextOfPointer(x, y);
		if(hsResult == null){
			return;
		}
		boolean needClipBitmp = false;
		if(hsResult.start != curSelectStart || hsResult.end != curSelectEnd){
			//分词有变化
			if(hsResult.start == -1 || hsResult.end == -1){
				//没有分到词，取消当前选中文本
				removeSelect();
			}else{
				//进行了分词并且得到了新词，选中新词
				selectText(hsResult.start, hsResult.end);
			}
			needClipBitmp = true;
			curSelectStart = hsResult.start;
			curSelectEnd = hsResult.end;
		}
		showResult(needClipBitmp);
	}

	/**
	 * 处理ACTION_UP事件
	 * @param event
	 */
	private void handler_action_up(MotionEvent event){
		cancel_long_click();
		if(shaderView != null && shaderView.getVisibility() == VISIBLE){
			shaderView.setVisibility(View.GONE);
		}
		if(hsResult != null && callback != null){
			if(hsResult.start != -1 && hsResult.end != -1){
				callback.splitWordResponse(hsResult);
			}
		}
	}
	
	/**
	 * 设置选中的文本
	 * @param selectStart
	 * @param selectEnd
	 */
	private void selectText(int selectStart,int selectEnd){
		if(selectStart < 0 || selectEnd < 0){
			return;
		}
		if(selectStart >= selectEnd){
			return;
		}
		Editable editable = getEditableText();
		if(editable == null){
			return;
		}
		if(selectEnd > editable.length()){
			return;
		}
		Selection.setSelection(editable, selectStart, selectEnd);
	}
	
	/**
	 * 取消选中的文本
	 */
	private void removeSelect(){
		Editable editable = getEditableText();
		if(editable != null){
			Selection.removeSelection(getEditableText());
		}
	}
	
	/**
	 * 计算按下点对应的字符
	 * @param x
	 * @param y
	 * @return
	 */
	private HaiciSplitResult calculateTextOfPointer(int x,int y){
		HaiciSplitResult result = null;
		try{
			Layout layout = getLayout();
			if(layout == null){
				return result;
			}
			
			int scroll_y = getScrollY();
			//行高
			int lineHeight = getLineHeight();
			
			// 按下点所在行的行号
			int lineNumber = layout.getLineForVertical(scroll_y+y);
			
			//设置默认返回值
			result = new HaiciSplitResult();
			result.click_x = x;
			result.click_y = y;
			result.click_up_y = lineHeight * lineNumber + getPaddingTop();
			result.click_down_y = result.click_up_y + lineHeight;
			result.start = -1;
			result.end = -1;
			
			// 按下点所在行的内容宽度
			int lineWidth = (int) layout.getLineWidth(lineNumber);
			
			// 按下点不在行文本范围内
			if (x > lineWidth + getPaddingLeft() || x <= getPaddingLeft()) {
				return result;
			}
			
			// 行内容第一个字符在整个文本中的位置
			int lineStart = layout.getLineStart(lineNumber);
			
			// 行内容最后一个字符在整个文本中的位置的下一个位置
			int lineEnd = layout.getLineEnd(lineNumber);
			
			// 行内容文本
			String lineText = getText().toString().substring(lineStart,lineEnd);
			if (lineText == null || lineText.length() == 0) {
				return result;
			}
			
			// 按下点最接近的字符在整个文本中的位置
			int position = layout.getOffsetForHorizontal(lineNumber, x);
			position = checkPosition(layout, lineNumber, lineText, position, x);
			
			// 所点击的字符在行文本中的位置
			int lineOffset = position - lineStart - 1;
			if (lineOffset < 0) {
				lineOffset = 0;
			} else if (lineOffset >= lineText.length()) {
				lineOffset--;
			}
			
			// 得到所点击的文本
			String clickText = String.valueOf(lineText.charAt(lineOffset));
			if(clickText == null || clickText.trim().length() == 0){
				return result;
			}
			
			// 执行分词
			if(manager != null){
				result = manager.splitWord(getContext(), lineText, lineOffset, 2, null);
			}
			if(result != null){
				result.click_x = x;
				result.click_y = y;
				result.click_up_y = lineHeight * lineNumber + getPaddingTop();
				result.click_down_y = result.click_up_y + lineHeight;
				result.start = lineStart + result.start;
				result.end = lineStart + result.end;
			}
			return result;
		}catch(Exception e){
			e.printStackTrace();
			result = null;
		}
		return result;
	}  
	
	/**
	 * 校正position
	 * 
	 * @param layout
	 * @param lineNumber
	 * @param lineText
	 * @param offset
	 * @param x
	 * @return
	 */
	private int checkPosition(Layout layout, int lineNumber, String lineText, int position, int x) throws Exception{
		/**
		 * 问题：
		 * 当所点击的文本在行尾时(最后一行行尾字符除外),position表示所点击文本在整个文本中的位置(即下标,从0开始); 
		 * 当点击的文本在行的其它位置时(包括最后一行行尾字符),position表示所点击文本在整个文本中的第几位(从1开始，非下标)
		 * 例如：一串文本
		 * 第一行: ABCDEFGXYZ 
		 * 第二行: 0123456789
		 * 第三行: QWERTYUIOP
		 * 当点击Z、9字符时,offset等于9、19(即下标);
		 * 当点击其它字符时,offset等于其下标+1
		 * 
		 * 解决方案:
		 * 计算行文本最后一个字符在x轴上的范围A,如果按下点的x坐标落在A中,position++
		 */
		if (lineNumber < layout.getLineCount() - 1) {
			int length = lineText.length();
			float[] widths = new float[length];
			TextPaint paint = layout.getPaint();
			paint.getTextWidths(lineText, 0, length, widths);
			//计算行的最后一个字符的x坐标返回
			int last_min_x = getPaddingLeft();
			int last_max_x = 0;
			for (int i = 0; i < length; i++) {
				if (i == length - 1) {
					last_max_x += last_min_x;
					last_max_x += widths[i];
				} else {
					last_min_x += widths[i];
				}
			}
			if (x >= last_min_x && x <= last_max_x) {
				position++;
			}
		}
		return position;
	}
	
	/**
	 * 判断offset位置对应的span是否是需要特殊处理的span,例如：URLSpan、ImageSpan、ClickableSpan
	 * @param offset
	 * @return
	 */
	private boolean isSpecialSpan(int offset) throws Exception{
		if(spannableString == null){
			return false;
		}
		if(offset < 0 || offset > spannableString.length()){
			return false;
		}
		ImageSpan[] imageSpans = spannableString.getSpans(0, spannableString.length(), ImageSpan.class);
		if(imageSpans != null && imageSpans.length > 0){
			for (ImageSpan imageSpan : imageSpans) {
				 int start = spannableString.getSpanStart(imageSpan);
	             int end = spannableString.getSpanEnd(imageSpan);
	             if(offset >= start && offset <= end){
	            	 return true;
	             }
			}
		}
		imageSpans = null;
		
		URLSpan[] urlSpans = spannableString.getSpans(0, spannableString.length(), URLSpan.class);
		if(urlSpans != null && urlSpans.length > 0){
			for (URLSpan urlSpan : urlSpans) {
				 int start = spannableString.getSpanStart(urlSpan);
	             int end = spannableString.getSpanEnd(urlSpan);
	             if(offset >= start && offset <= end){
	            	 return true;
	             }
			}
		}
		urlSpans = null;
		
		ClickableSpan[] clickableSpans = spannableString.getSpans(0, spannableString.length(), ClickableSpan.class);
		if(clickableSpans != null && clickableSpans.length > 0){
			for (ClickableSpan clickableSpan : clickableSpans) {
				 int start = spannableString.getSpanStart(clickableSpan);
	             int end = spannableString.getSpanEnd(clickableSpan);
	             if(offset >= start && offset <= end){
	            	 return true;
	             }
			}
		}
		clickableSpans = null;
		return false;
	}
	
	/**
	 * 取消ACTION_DOWN时发送长按操作请求
	 */
	public void cancel_long_click(){
		click_x = -1;
		click_y = -1;
		mHandler.removeMessages(LONG_CLICK);
		has_long_click = false;
	}
	
	/**
	 * 处理长按事件
	 */
	private void handler_long_click(){
		has_long_click = true;
		if(click_x == -1 || click_y == -1){
			return;
		}
		hsResult = calculateTextOfPointer(click_x, click_y);
		if(hsResult == null){
			return;
		}
		selectText(hsResult.start,hsResult.end);
		showResult(true);
	}
	
	/**
	 * 显示放大镜
	 * @param needClipBitmp 是否需要截屏标志
	 */
	private void showResult(boolean needClipBitmp){
		if(hsResult == null){
			return;
		}
		if(parentView == null){
			return;
		}
		if(shaderView == null){
			return;
		}
		int parentScrollY = parentView.getScrollY();//父view滚动条的位置
		int click_y = hsResult.click_y - parentScrollY;//点击的点y轴坐标
		int click_x = hsResult.click_x;//点击的点x轴坐标
		
		//截图
		boolean all = false;
		if(all){
			//截整个view图
			Bitmap clipBm = null;
			if(needClipBitmp){
				clipBm = convertViewToBitmap(parentView, 0, 0, parentView.getWidth(), parentView.getHeight());
			}
			shaderView.updateBitmap(clipBm, click_x, click_y, parentView.getWidth());
		}else{
			//截部分view图
			int view_width = parentView.getWidth();
			int view_height = parentView.getHeight();
			//截图宽度
			int clip_width = view_width;
			//截图高度
			int clip_height = view_height/4;
			//截图起始点的x坐标
			int clipX = 0;
			//截图起始点的y坐标
			int clipY = 0;
			//点击点在截图上的对应点x坐标
			int realX = click_x;
			//点击点在截图上的对应点y坐标
			int realY = 0;
			if(click_y - clip_height/2 < 0){
				realY = click_y;
			}else if(click_y + clip_height/2 > view_height){
				realY = clip_height - (view_height - click_y);
			}else{
				realY = clip_height/2;
			}
			clipY = click_y - realY;
			Bitmap clipBm = convertViewToBitmap(parentView, clipX, clipY,clip_width, clip_height);
			shaderView.updateBitmap(clipBm, click_x, click_y, clip_width, realX, realY);
		}
	}
	
	public static Bitmap convertViewToBitmap(View view,int x,int y,int width,int height) {
		Bitmap curViewBitmap = null;
		Bitmap clipBm = null;
		try {
			view.setDrawingCacheEnabled(true);
			curViewBitmap = view.getDrawingCache();
			if(curViewBitmap != null){
				clipBm = Bitmap.createBitmap(curViewBitmap, x, y, width, height);
			}
		} catch (Exception e) {
			e.printStackTrace();
			clipBm = null;
		} catch (Error e) {
			e.printStackTrace();
			clipBm = null;
		} finally {
			curViewBitmap = null;
			view.destroyDrawingCache();
		}
		return clipBm;
	}
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch(msg.what){
			case LONG_CLICK:
				handler_long_click();
				break;
			}
		};
	};
}
