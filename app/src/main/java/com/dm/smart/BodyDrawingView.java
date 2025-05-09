package com.dm.smart;

import static com.dm.smart.ui.elements.CustomToasts.showToast;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.dm.smart.items.Brush;
import com.dm.smart.items.SerializablePath;
import com.dm.smart.items.Step;

import java.util.ArrayList;
import java.util.List;


public class BodyDrawingView extends View {

    final Rect mBGRect = new Rect();
    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleDetector;
    private final Paint snapshotPaint = new Paint();
    public Bitmap snapshot;
    List<Step> steps = new ArrayList<>();
    Canvas drawImageCanvas;
    Bitmap backgroundImage = null;
    Toast showedToast = null;
    Rect mBGZoomedRect;
    Matrix mZoomingMatrix;
    Matrix mInvertMatrix;
    private Point mBGSizeZoomed;
    private float mZoomingScale = 1.0f, minZoomingScale = 1.0f, maxZoomingScale = 7.0f;
    private Bitmap freshSnapshot;
    private SerializablePath freshPath = null;
    private Paint freshPaint = null;
    private Bitmap maskImage = null;
    private int intensity = -1;
    private Brush brush = null;
    private boolean allowOutsideDrawing;
    private int intensity_mark = 0;
    private OnDrawingChangeListener onDrawingChangeListener;

    public BodyDrawingView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //noinspection NullableProblems
        mGestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent motionEvent) {
                return false;
            }

            @Override
            public void onShowPress(@NonNull MotionEvent motionEvent) {
            }

            @Override
            public boolean onSingleTapUp(@NonNull MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onScroll(@NonNull MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
                moveXY(-v, -v1);
                return true;
            }

            @Override
            public void onLongPress(@NonNull MotionEvent motionEvent) {
            }

            @Override
            public boolean onFling(@NonNull MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
                return false;
            }
        });

        mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public void onScaleEnd(@NonNull ScaleGestureDetector detector) {
            }

            @Override
            public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public boolean onScale(@NonNull ScaleGestureDetector detector) {
                float tmpZoomingScale = mZoomingScale * detector.getScaleFactor();
                if (tmpZoomingScale >= minZoomingScale && tmpZoomingScale <= maxZoomingScale) {
                    mZoomingMatrix.postScale(detector.getScaleFactor(), detector.getScaleFactor(),
                            detector.getFocusX(), detector.getFocusY());
                }
                setBGImageZoomingLocation();
                invalidate();
                return true;
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int old_w, int old_h) {
        if (w > 0 & h > 0) {
            setBGImageZooming(this.backgroundImage);
            setZoomingBounds();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {

        // Draw background
        if (backgroundImage != null) {
            canvas.drawBitmap(backgroundImage, mBGRect, mBGZoomedRect, null);
        }

        if (snapshot != null) {
            canvas.drawBitmap(snapshot, mBGRect, mBGZoomedRect, snapshotPaint);
        }

        // draw freshSnapshot and path currently being drawn
        if (freshSnapshot != null) {
            canvas.drawBitmap(freshSnapshot, mBGRect, mBGZoomedRect, null);
        }
        if (freshPath != null) {
            canvas.drawPath(freshPath, freshPaint);
        }
    }

    // After lifting the pen this method draws the step to the snapshot
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    void drawStep(Step step, int width, int height) {
        if (freshSnapshot == null) {
            if (width != 0 && height != 0) {
                freshSnapshot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            } else {
                freshSnapshot = Bitmap.createBitmap(mBGRect.width(), mBGRect.height(),
                        Bitmap.Config.ARGB_8888);
            }
            drawImageCanvas = new Canvas(freshSnapshot);
        }
        Bitmap currentSnapshot;
        if (width != 0 && height != 0) {
            currentSnapshot = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        } else {
            currentSnapshot = Bitmap.createBitmap(mBGRect.width(), mBGRect.height(),
                    Bitmap.Config.ARGB_8888);
        }
        Canvas tempCanvas = new Canvas(currentSnapshot);
        new Paint();
        Paint paint;
        if (step.brush.paint == null) {
            paint = new Paint();
        } else {
            paint = new Paint(step.brush.paint);
        }
        paint.setStrokeWidth(step.brush.thickness);
        boolean drawOutside = step.brush.drawOutside;
        if (step.brush.type.equals("erase")) {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            drawImageCanvas.drawPath(step.path, paint);
        } else {
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
            tempCanvas.drawPath(step.path, paint);
            if (!drawOutside) {
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
                if (maskImage != null) {
                    tempCanvas.drawBitmap(maskImage, 0, 0, paint);
                }
            }
        }
        // Log the size of the current snapshot
        Log.d("Snapshot size", "Width: " + currentSnapshot.getWidth() + " Height: " + currentSnapshot.getHeight());
        drawImageCanvas.drawBitmap(currentSnapshot, 0, 0, null);
        snapshot = freshSnapshot;
    }

    public void setBGImage(Bitmap backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public void setMaskImage(Bitmap maskImage) {
        this.maskImage = maskImage;
    }

    public void setIntensity(int progress, int intensity) {
        this.intensity_mark = progress;
        this.intensity = intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public void setBrush(Brush brush) {
        this.brush = brush;
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void undoLastStep() {
        if (!steps.isEmpty()) {
            steps.remove(steps.size() - 1);
            if (freshSnapshot != null) {
                freshSnapshot.recycle();
                freshSnapshot = null;
            }
            if (!steps.isEmpty()) {
                for (Step step : steps) {
                    drawStep(step, 0, 0);
                }
            } else {
                freshSnapshot = Bitmap.createBitmap(mBGRect.width(), mBGRect.height(),
                        Bitmap.Config.ARGB_8888);
                drawImageCanvas = new Canvas(freshSnapshot);
                drawImageCanvas.drawBitmap(freshSnapshot, 0, 0, null);
                snapshot = freshSnapshot;
            }
            // After a new drawing is added, notify the listener
            if (onDrawingChangeListener != null) {
                onDrawingChangeListener.onDrawingChange();
            }
            invalidate();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    void redrawAllSavedSteps() {
        if (!steps.isEmpty()) {
            int width = backgroundImage.getWidth();
            int height = backgroundImage.getHeight();
            for (Step step : steps) {
                drawStep(step, width, height);
            }
            invalidate();
        }
    }

    private void setBGImageZooming(Bitmap backgroundImage) {
        int imgW = backgroundImage.getWidth();
        int imgH = backgroundImage.getHeight();
        RectF imgRect = new RectF(0, 0, imgW, imgH);
        int viewW = this.getWidth();
        int viewH = this.getHeight();
        RectF viewRect = new RectF(0, 0, viewW, viewH);
        imgRect.round(mBGRect);
        Matrix matrix = new Matrix();
        matrix.setRectToRect(imgRect, viewRect, Matrix.ScaleToFit.CENTER);
        mZoomingMatrix = matrix;
        setBGImageZoomingLocation();
        if (mBGSizeZoomed.x > 0 & mBGSizeZoomed.y > 0) {
            invalidate();
        }
    }

    private void setBGImageZoomingLocation() {
        Matrix matrix = mZoomingMatrix;
        mInvertMatrix = new Matrix();
        matrix.invert(mInvertMatrix);
        int imgW = backgroundImage.getWidth();
        int imgH = backgroundImage.getHeight();
        mZoomingScale = mZoomingMatrix.mapRadius(1f) - mZoomingMatrix.mapRadius(0f);
        mBGSizeZoomed = new Point((int) (imgW * mZoomingScale), (int) (imgH * mZoomingScale));
        RectF BGZoomedRect = new RectF();
        matrix.mapRect(BGZoomedRect, new RectF(mBGRect));
        mBGZoomedRect = new Rect();
        BGZoomedRect.round(mBGZoomedRect);
    }

    private void setZoomingBounds() {
        int imgW = backgroundImage.getWidth();
        int imgH = backgroundImage.getHeight();
        RectF imgRect = new RectF(0, 0, imgW, imgH);
        int viewW = this.getWidth();
        int viewH = this.getHeight();
        RectF viewRect = new RectF(0, 0, viewW, viewH);
        Matrix matrix = new Matrix();
        matrix.setRectToRect(imgRect, viewRect, Matrix.ScaleToFit.CENTER);
        minZoomingScale = matrix.mapRadius(1f) - matrix.mapRadius(0f);
        maxZoomingScale = 7 * minZoomingScale;
    }

    private void moveXY(float transX, float transY) {
        float newX = mBGZoomedRect.left + transX;
        float newY = mBGZoomedRect.top + transY;

        boolean widthBiggerThanScreen = mBGZoomedRect.width() > getWidth();
        boolean heightBiggerThanScreen = mBGZoomedRect.height() > getHeight();

        boolean passLeftEdge = (newX > 0 && widthBiggerThanScreen) || (newX < 0 && !widthBiggerThanScreen);
        boolean passRightEdge = (newX + mBGZoomedRect.width() < getWidth() && widthBiggerThanScreen) ||
                (newX + mBGZoomedRect.width() > getWidth() && !widthBiggerThanScreen);
        boolean passTopEdge = (newY > 0 && heightBiggerThanScreen) || (newY < 0 && !heightBiggerThanScreen);
        boolean passBottomEdge = (newY + mBGZoomedRect.height() < getHeight() && heightBiggerThanScreen) ||
                (newY + mBGZoomedRect.height() > getHeight() && !heightBiggerThanScreen);

        if (passLeftEdge && passRightEdge) {
            transX = 0;
        } else if (passLeftEdge) {
            transX = -mBGZoomedRect.left;
        } else if (passRightEdge) {
            transX = getWidth() - (mBGZoomedRect.left + mBGZoomedRect.width());
        }

        if (passTopEdge && passBottomEdge) {
            transY = 0;
        } else if (passTopEdge) {
            transY = -mBGZoomedRect.top;
        } else if (passBottomEdge) {
            transY = getHeight() - (mBGZoomedRect.top + mBGZoomedRect.height());
        }

        mZoomingMatrix.postTranslate(transX, transY);
        setBGImageZoomingLocation();
        invalidate();
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        boolean isPen = event.getToolType(0) == MotionEvent.TOOL_TYPE_STYLUS;

        if (!isPen) {
            mScaleDetector.onTouchEvent(event);
            if (!mScaleDetector.isInProgress()) {
                mGestureDetector.onTouchEvent(event);
            }
            return true;
        }
        if (intensity == -1) {
            if (showedToast != null) {
                showedToast.cancel();
            }
            showedToast = showToast(this.getContext(), getResources().getString(R.string.toast_select_intensity));
            showedToast.show();
            return true;
        }

        if (event.getAction() != MotionEvent.ACTION_DOWN &&
                event.getAction() != MotionEvent.ACTION_MOVE &&
                event.getAction() != MotionEvent.ACTION_UP) {
            freshPath = null;
            invalidate();
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        if (brush.drawByMove) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                freshPath = new SerializablePath();
                freshPath.moveTo(x, y);
                freshPath.lineTo(x, y);

                freshPaint = new Paint(brush.paint);
                freshPaint.setStyle(Paint.Style.STROKE);
                freshPaint.setStrokeWidth(mZoomingScale * brush.thickness);
                freshPaint.setColor(brush.type.equals("erase") ? Color.WHITE : intensity);

            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                // Get all points from event.getHistoricalX/Y for a smoother draw;
                int histPointsAmount = event.getHistorySize();
                for (int i = 0; i < histPointsAmount; i++) {
                    freshPath.lineTo(event.getHistoricalX(i), event.getHistoricalY(i));
                }
                freshPath.lineTo(x, y);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                freshPath.lineTo(x, y);

                Step step = new Step();
                step.brush = new Brush(brush);
                step.brush.paint.setColor(intensity);
                step.brush.drawOutside = allowOutsideDrawing;
                step.intensity_mark = intensity_mark;
                step.path = new SerializablePath();
                step.path.addPath(freshPath);
                freshPath = null;
                step.path.transform(mInvertMatrix);
                steps.add(step);
                drawStep(step, 0, 0);

                // After a new drawing is added, notify the listener
                if (onDrawingChangeListener != null) {
                    onDrawingChangeListener.onDrawingChange();
                }
            }
        } else {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {

                float[] pts = new float[]{x, y};
                mInvertMatrix.mapPoints(pts);

                Step step;
                step = new Step();
                step.brush = brush;
                step.intensity_mark = intensity_mark;
            }
        }
        invalidate();
        return true;
    }

    public void setAllowOutsideDrawing(boolean allowOutsideDrawing) {
        this.allowOutsideDrawing = allowOutsideDrawing;
    }

    public void setOnDrawingChangeListener(OnDrawingChangeListener listener) {
        this.onDrawingChangeListener = listener;
    }

    public interface OnDrawingChangeListener {
        void onDrawingChange();
    }

}