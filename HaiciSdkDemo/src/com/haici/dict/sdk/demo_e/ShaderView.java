package com.haici.dict.sdk.demo_e;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.view.View;

public class ShaderView extends View{
	
	public static final String TAG = "ShaderView";
	
	/**
	 * 放大倍数
	 */
	private static final float FACTOR = 1.2f;
	
	/**
	 * 原始图片
	 */
	public Bitmap sourceBitmap;
	
	/**
	 * 放大镜边框
	 */
	private Bitmap magnifierStrokeBitmap;
	
	/**
	 * 放大镜外边框左边到内容区间距
	 */
	private int frameLeft;
	
	/**
	 * 放大镜外边框上边到内容区间距
	 */
	private int frameTop;
	
	/**
	 * 放大镜外边框右边到内容区间距
	 */
	private int frameRight;
	
	/**
	 * 放大镜外边框下边到内容区间距
	 */
	private int frameBottom;
	
	/**
	 * 放大镜
	 */
	private ShapeDrawable magnifierShapeDrawable;
	
	/**
	 * 放大镜宽度
	 */
	private int magnifier_width;
	
	/**
	 * 放大镜宽度
	 */
	private int magnifier_height;
	
	/**
	 * 放大镜位置转换器
	 */
	private Matrix matrix = new Matrix();
	
	/**
	 * 边框.9图片资源id
	 */
	private int frameResId;
	
	/**
	 * 蒙板画笔
	 */
	private Paint mPaint = null;
	
	/**
	 * 实际放大倍数
	 */
	private float realityFactor;
	
	/**
	 * 当前view的宽度
	 */
	private int view_width;
	
	public ShaderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public ShaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ShaderView(Context context) {
		super(context);
		init();
	}

	private void init(){
		magnifier_height = dip2px(getContext(), 60);
		//更换边框.9图片，需修改以下四个值
		frameLeft = 14;
		frameTop = 14;
		frameRight = 14;
		frameBottom = 35;
		frameResId = R.drawable.magnifier_bg;
	}
	
	public void updateBitmap(Bitmap bitmap,int x,int y,int view_width,int realX,int realY){
		if(bitmap == null && magnifierShapeDrawable == null){
			return;
		}
		if(getVisibility() != View.VISIBLE){
			setVisibility(View.VISIBLE);
		}
		if(this.view_width != view_width){
			magnifierStrokeBitmap = null;
			this.view_width = view_width;
		}
		magnifier_width = view_width*3/5;
		if(bitmap != null){
			realityFactor = FACTOR;
			this.sourceBitmap = bitmap;
			//放大图片
			Bitmap scaleBitmap = scaleBitmap(sourceBitmap, realityFactor);
			if(scaleBitmap == null){
				scaleBitmap = sourceBitmap;
				realityFactor = 1.0f;
			}
			//创建BitmapShader
			BitmapShader shader = new BitmapShader(scaleBitmap, TileMode.CLAMP,TileMode.CLAMP);
			//创建圆角矩形Drawable
			if(magnifierShapeDrawable == null){
				float[] outerRadii = new float[]{0,0,0,0,0,0,0,0};
				magnifierShapeDrawable = new ShapeDrawable(new RoundRectShape(outerRadii, null, null));
			}
			magnifierShapeDrawable.getPaint().setShader(shader);
		}
		
		int rx = magnifier_width/2;
		int ry = magnifier_height/2;
		//设置shader的起始位置
		float dx = rx - realX*realityFactor;
		float dy = ry - realY*realityFactor;
		matrix.setTranslate(dx, dy);
		magnifierShapeDrawable.getPaint().getShader().setLocalMatrix(matrix);
		//设置shape外边框
		int x1 = x;
		int y1 = y - ry*3;
		magnifierShapeDrawable.setBounds(x1-rx, y1-ry, x1+rx, y1+ry);
		//重绘
		invalidate();
	}
	
	public void updateBitmap(Bitmap bitmap,int x,int y,int view_width){
		if(bitmap == null && magnifierShapeDrawable == null){
			return;
		}
		if(getVisibility() != View.VISIBLE){
			setVisibility(View.VISIBLE);
		}
		if(this.view_width != view_width){
			magnifierStrokeBitmap = null;
			this.view_width = view_width;
		}
		magnifier_width = view_width*3/5;
		if(bitmap != null){
			realityFactor = FACTOR;
			this.sourceBitmap = bitmap;
			//放大图片
			Bitmap scaleBitmap = scaleBitmap(sourceBitmap, realityFactor);
			if(scaleBitmap == null){
				scaleBitmap = sourceBitmap;
				realityFactor = 1.0f;
			}
			//创建BitmapShader
			BitmapShader shader = new BitmapShader(scaleBitmap, TileMode.CLAMP,TileMode.CLAMP);
			//创建圆角矩形Drawable
			if(magnifierShapeDrawable == null){
				magnifierShapeDrawable = new ShapeDrawable(new RoundRectShape(null, null, null));
			}
			magnifierShapeDrawable.getPaint().setShader(shader);
		}
		
		int rx = magnifier_width/2;
		int ry = magnifier_height/2;
		//设置shader的起始位置
		float dx = rx - x*realityFactor;
		float dy = ry - y*realityFactor;
		matrix.setTranslate(dx, dy);
		magnifierShapeDrawable.getPaint().getShader().setLocalMatrix(matrix);
		//设置shape外边框
		int x1 = x;
		int y1 = y - ry*3;
		magnifierShapeDrawable.setBounds(x1-rx, y1-ry, x1+rx, y1+ry);
		//重绘
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		magnifierShapeDrawable.draw(canvas);
		Rect bounds = magnifierShapeDrawable.getBounds();
		//画蒙板
//		if(mPaint == null){
//			mPaint = new Paint();
//			mPaint.setAntiAlias(true);
//			mPaint.setColor(Color.parseColor("#10000000"));
//			mPaint.setStyle(Style.FILL);
//		}
//		canvas.drawRect(magnifierShapeDrawable.getBounds(), mPaint);
		//画外边框
		if(magnifierStrokeBitmap == null){
			int frameWidth = bounds.width() + frameLeft + frameRight;
			int frameHeight = bounds.height() + frameTop + frameBottom;
			magnifierStrokeBitmap = getNninepatch(getContext(), frameResId, frameWidth, frameHeight);
		}
		int left = bounds.left - frameLeft;
		int top = bounds.top - frameTop;
		canvas.drawBitmap(magnifierStrokeBitmap, left, top, null);
	}
	
	/**
	 * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
	 */
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}
	
	/**
	 * 获取指定大小的.9图片
	 * @param context
	 * @param resId
	 * @param x
	 * @param y
	 * @return
	 */
	public static Bitmap getNninepatch(Context context, int resId, int x, int y){
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
        byte[] chunk = bitmap.getNinePatchChunk();
        NinePatchDrawable np_drawable = new NinePatchDrawable(bitmap,chunk, new Rect(), null);
        np_drawable.setBounds(0, 0,x, y);
        
        Bitmap output_bitmap = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output_bitmap);
        np_drawable.draw(canvas);
        return output_bitmap;
    }

	/**
	 * 缩放图片
	 * @param sourceBitmap 	原始图片
	 * @param factor	  	 缩放倍数
	 * @return
	 */
	public static Bitmap scaleBitmap(Bitmap sourceBitmap, float factor){
		if(sourceBitmap == null){
			return null;
		}
		int width = sourceBitmap.getWidth();
		int height = sourceBitmap.getHeight();
		Bitmap scaleBitmap = null;
		try{
			int scaleWidth = (int)(width * factor);
			int scaleHeight = (int)(height * factor);
			scaleBitmap = Bitmap.createScaledBitmap(sourceBitmap, scaleWidth, scaleHeight, true);
		}catch(Exception e){
			e.printStackTrace();
			scaleBitmap = null;
		}
		return scaleBitmap;
	}
}
