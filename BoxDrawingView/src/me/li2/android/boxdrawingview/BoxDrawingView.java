package me.li2.android.boxdrawingview;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;


public class BoxDrawingView extends android.view.View {
    public static final String TAG = "BoxDrawingView";
    
    private Box mCurrentBox;
    private ArrayList<Box> mBoxes = new ArrayList<Box>();
    private Paint mBoxPaint;
    private Paint mBackgroundPaint;
    
    public BoxDrawingView(Context context) {
        this(context, null);
    }    
    
    // 视图可从代码或者布局文件实例化。
    // 从布局文件中实例化的视图可收到一个AttributeSet实例，该实例包含了XML布局文件中指定的XML属性。
    // 即使不打算使用构造方法，按习惯做法，也应添加它们。
    public BoxDrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        // Paint the boxes a nice semitransparent red(ARGB)
        mBoxPaint = new Paint();
        mBoxPaint.setColor(0x22ff0000);
        
        // Paint the background off-white
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setColor(0xfff8efe0);
    }
    
    // 应用启动时，所有视图都处于无效状态 invalid，视图未被绘制到屏幕上，
    // Android通过调用顶级View视图的draw()方法解决这个问题，
    // 这将引起自上而下的链式调用反应，视图完成自我绘制，然后是子视图的自我绘制，直至继承结构的末端，
    // 为参与这种绘制，可覆盖View.onDraw()方法。
    // Canvas和Paint是Android系统的两大绘制类：
    // Canvas类具有我们需要的所有绘制操作。其方法可决定绘制的位置及图形，例如线条、圆形、字词、矩形等。
    // Paint类决定如何进行绘制操作。其方法可指定绘制图形的特征，例如是否填充图形、使用什么字体绘制、线条是什么颜色等。
    @Override
    protected void onDraw(Canvas canvas) {
        // Fill the background
        canvas.drawPaint(mBackgroundPaint);
        
        // 绘制所有的矩形框
        for (Box box : mBoxes) {
            float left = Math.min(box.getOrigin().x, box.getCurrent().x);
            float right = Math.max(box.getOrigin().x, box.getCurrent().x);
            float top = Math.min(box.getOrigin().y, box.getCurrent().y);
            float bottom = Math.max(box.getOrigin().y, box.getCurrent().y);
            
            canvas.drawRect(left, top, right, bottom, mBoxPaint);
        }
        
        super.onDraw(canvas);
    }
    
    // 监听触摸事件的一种方式是 setOnTouchListener(View.OnTouchListener l);
    // 然而，由于是View的子类，因此可以直接覆写View.onTouchEvent()方法。
    // 该方法接收一个MotionEvent实例，它用来描述包括位置和动作的触摸事件。
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        PointF curr = new PointF(event.getX(), event.getY());
        
        Log.i(TAG, "Received event at x=" + curr.x + ", y=" + curr.y + ":");
        switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
            Log.i(TAG, "  ACTION_DOWN");
            // Reset drawing state
            // 用户触摸视图时，创建新的Box对象 mCurrentBox，并存储到矩形框数组中；
            mCurrentBox = new Box(curr);
            mBoxes.add(mCurrentBox);
            break;
        case MotionEvent.ACTION_MOVE:
            Log.i(TAG, "  ACTION_MOVE");
            if (mCurrentBox != null) {
                // 用户在屏幕上移动手指时，当前的矩形框就发生了变化，更新数据到 mCurrentBox.
                mCurrentBox.setCurrent(curr);
                // 强制BoxDrawingView重绘自己，也就是我们看到的实时绘制的矩形。
                invalidate();
            }
            break;
        case MotionEvent.ACTION_UP:
            Log.i(TAG, "  ACTION_UP");
            // 用户手指离开了屏幕时，就完成了一次绘制，清空 mCurrentBox，这个矩形框就被保存下来，不会被改动。
            mCurrentBox = null;
            break;
        case MotionEvent.ACTION_CANCEL:
            Log.i(TAG, "  ACTION_CANCEL");
            mCurrentBox = null;
            break;
        }
        
        return true;
    }
}
