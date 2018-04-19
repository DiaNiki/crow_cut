package com.diana.crowcut;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class DrawableImageView extends AppCompatImageView {
    protected final Paint mPaint;
    protected Bitmap mBitmap;
    protected final Paint mBitmapPaint;
    protected Canvas mCanvas;
    protected final Path mPath;

    protected float mX = -1;
    protected float mY = -1;
    protected static final float TOUCH_TOLERANCE = 4;

    protected Boolean isEnabled = false;

    public DrawableImageView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(35);

        mPath = new Path();
        mBitmapPaint = new Paint();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    public void resize(float w, float h, float realW, float realH)
    {
        float stretch = Math.min(realW / w, realH / h);
        int actualW = (int) (w * stretch);
        int actualH = (int) (h * stretch);
        this.getLayoutParams().width = actualW;
        this.getLayoutParams().height = actualH;
        this.requestLayout();
        this.invalidate();
    }

    public void setColor(int color)
    {
        mPaint.setColor(color);
        mBitmapPaint.setColor(color);
    }

    public void setIsEnabled(Boolean isEnabled)
    {
        this.isEnabled = isEnabled;
    }

    public void clear()
    {
        if (mBitmap != null) {
            mBitmap.eraseColor(Color.TRANSPARENT);
        }
    }

    public Bitmap getBitmap(float w, float h)
    {
        return Bitmap.createScaledBitmap(mBitmap, (int) w, (int) h, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
            canvas.drawPath(mPath, mPaint);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        if (!isEnabled)
        {
            return true;
        }

        final float x = event.getX();
        final float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
                break;
        }
        return true;
    }

    private void touchStart(float x, float y) {
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }
    private void touchUp() {
        mPath.lineTo(mX, mY);
        mCanvas.drawPath(mPath, mPaint);
        mPath.reset();
    }
}
