package com.a7clk.wall_e_android.ui;

/**
 * Created by Gotze on 12/4/2016.
 */
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import com.a7clk.wall_e_android.R;

public class JoystickView extends View implements Runnable {
    // Constants
    private final double RAD = 57.2957795;
    public final static long DEFAULT_LOOP_INTERVAL = 100; // 100 ms
    public final static int FRONT = 3;
    public final static int FRONT_RIGHT = 2;
    public final static int RIGHT = 1;
    public final static int RIGHT_BOTTOM = 8;
    public final static int BOTTOM = 7;
    public final static int BOTTOM_LEFT = 6;
    public final static int LEFT = 5;
    public final static int LEFT_FRONT = 4;

    private static final int PRESSED_COLOR_LIGHTUP = 255 / 25;
    private static final int PRESSED_RING_ALPHA = 75;
    private static final int DEFAULT_PRESSED_RING_WIDTH_DIP = 6;
    private static final int ANIMATION_TIME_ID = android.R.integer.config_shortAnimTime;
    // Variables
    private OnJoystickMoveListener onJoystickMoveListener; // Listener
    private Thread thread = new Thread(this);
    private long loopInterval = DEFAULT_LOOP_INTERVAL;
    private int xPosition = 0; // Touch x position
    private int yPosition = 0; // Touch y position
    private double centerX = 0; // Center view x position
    private double centerY = 0; // Center view y position
    private Paint mainCircle;
    private Paint secondaryCircle;
    private Paint arrow;
    private Path up;
    private Path down;
    private Path left;
    private Path right;

    private Paint button;
    private Paint focusPaint;
    private float animationProgress;
    private int pressedRingWidth;
    private int defaultColor = Color.BLACK;
    private int pressedColor;
    private ObjectAnimator pressedAnimator;
    private int pressedRingRadius;


    private Paint horizontalLine;
    private Paint verticalLine;
    private int joystickRadius;
    private int buttonRadius;
    private int lastAngle = 0;
    private int lastPower = 0;

    private AttributeSet attrs;
    private Context context;

    public JoystickView(Context context) {
        super(context);
    }

    public JoystickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        attrs = this.attrs;
        context= this.context;
        initJoystickView();
    }

    public JoystickView(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        attrs = this.attrs;
        context= this.context;
        initJoystickView();
    }

    protected void initJoystickView() {
        this.setFocusable(true);
        setClickable(true);
        mainCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        mainCircle.setColor(Color.BLACK);
        mainCircle.setAlpha(8);
        mainCircle.setStyle(Paint.Style.FILL_AND_STROKE);

        secondaryCircle = new Paint();
        secondaryCircle.setColor(Color.GREEN);
        secondaryCircle.setStyle(Paint.Style.STROKE);

        verticalLine = new Paint();
        verticalLine.setStrokeWidth(5);
        verticalLine.setColor(Color.RED);

        horizontalLine = new Paint();
        horizontalLine.setStrokeWidth(2);
        horizontalLine.setColor(Color.BLACK);

        arrow= new Paint();
        arrow.setStrokeWidth(2);
        arrow.setColor(Color.GRAY);
        arrow.setAlpha(20);

//        int width= getWidth();
//        int height= getHeight();
//        int xCenter = (int) (width / 2 * 0.75);
//        int yCenter = (int) (height / 2 * 0.75);
//        int arrowWidth = 15;
//        int arrowHeight = 50;
//
//        up = new Path();
//        up.moveTo(xCenter, 0);
//        up.lineTo(xCenter+arrowWidth, arrowHeight);
//        up.lineTo(xCenter-arrowWidth, arrowHeight);
//        up.close();
//
//        down = new Path();
//        down.moveTo(xCenter, height);
//        down.lineTo(xCenter+arrowWidth, height - arrowHeight);
//        down.lineTo(xCenter-arrowWidth, height - arrowHeight);
//        down.close();
//
//        right=new Path();
//        right.moveTo(width, yCenter);
//        right.lineTo(width-arrowHeight, yCenter+arrowWidth);
//        right.lineTo(width-arrowHeight, yCenter-arrowWidth);
//        right.close();
//
//        left = new Path();
//        left.moveTo(0, yCenter);
//        left.lineTo(arrowHeight, yCenter+arrowWidth);
//        left.lineTo(arrowHeight, yCenter-arrowWidth);
//        left.close();

        button = new Paint(Paint.ANTI_ALIAS_FLAG);
        button.setAlpha(40);
        button.setStyle(Paint.Style.FILL);

        focusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        focusPaint.setStyle(Paint.Style.STROKE);

        pressedRingWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_PRESSED_RING_WIDTH_DIP, getResources()
                .getDisplayMetrics());

        int color = Color.LTGRAY;
        if (attrs != null) {
            final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircleButton);
            color = a.getColor(R.styleable.CircleButton_cb_color, color);
            pressedRingWidth = (int) a.getDimension(R.styleable.CircleButton_cb_pressedRingWidth, pressedRingWidth);
            a.recycle();
        }

        setColor(color);

        focusPaint.setStrokeWidth(pressedRingWidth);
        final int pressedAnimationTime = getResources().getInteger(ANIMATION_TIME_ID);
        pressedAnimator = ObjectAnimator.ofFloat(this, "animationProgress", 0f, 0f);
        pressedAnimator.setDuration(pressedAnimationTime);
    }

    public void setColor(int color) {
        this.defaultColor = color;
        this.pressedColor = getHighlightColor(color, PRESSED_COLOR_LIGHTUP);

        button.setColor(defaultColor);
        focusPaint.setColor(defaultColor);
        focusPaint.setAlpha(PRESSED_RING_ALPHA);

        this.invalidate();
    }

    private int getHighlightColor(int color, int amount) {
        return Color.argb(Math.min(255, Color.alpha(color)), Math.min(255, Color.red(color) + amount),
                Math.min(255, Color.green(color) + amount), Math.min(255, Color.blue(color) + amount));
    }

    @Override
    protected void onFinishInflate() {
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld) {
        super.onSizeChanged(xNew, yNew, xOld, yOld);
        // before measure, get the center of view
        xPosition = (int) getWidth() / 2;
        yPosition = (int) getHeight() / 2;
        int d = Math.min(xNew, yNew);
        buttonRadius = (int) (d / 2 * 0.25);
        joystickRadius = (int) (d / 2 * 0.75);

        pressedRingRadius = buttonRadius - pressedRingWidth - pressedRingWidth / 2;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // setting the measured values to resize the view to a certain width and
        // height
        int d = Math.min(measure(widthMeasureSpec), measure(heightMeasureSpec));

        setMeasuredDimension(d, d);

    }

    private int measure(int measureSpec) {
        int result = 0;

        // Decode the measurement specifications.
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.UNSPECIFIED) {
            // Return a default size of 200 if no bounds are specified.
            result = 200;
        } else {
            // As you want to fill the available space
            // always return the full available bounds.
            result = specSize;
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // super.onDraw(canvas);
        centerX = (getWidth()) / 2;
        centerY = (getHeight()) / 2;

        int xCenter = (int) (centerX);
        int yCenter = (int) (centerY);
        int arrowWidth = 40;
        int arrowHeight = 50;

        up = new Path();
        up.moveTo(xCenter, yCenter-joystickRadius);
        up.lineTo(xCenter+arrowWidth, yCenter-joystickRadius + arrowHeight);
        up.lineTo(xCenter-arrowWidth, yCenter-joystickRadius + arrowHeight);
        up.close();

        down = new Path();
        down.moveTo(xCenter, yCenter+joystickRadius);
        down.lineTo(xCenter+arrowWidth, yCenter+joystickRadius - arrowHeight);
        down.lineTo(xCenter-arrowWidth, yCenter+joystickRadius - arrowHeight);
        down.close();
//
        right=new Path();
        right.moveTo(xCenter+joystickRadius, yCenter);
        right.lineTo(xCenter+joystickRadius-arrowHeight, yCenter+arrowWidth);
        right.lineTo(xCenter+joystickRadius-arrowHeight, yCenter-arrowWidth);
        right.close();
//
        left = new Path();
        left.moveTo(xCenter-joystickRadius, yCenter);
        left.lineTo(xCenter-joystickRadius+arrowHeight, yCenter+arrowWidth);
        left.lineTo(xCenter-joystickRadius+arrowHeight, yCenter-arrowWidth);
        left.close();

        // painting the main circle
        canvas.drawCircle((int) centerX, (int) centerY, joystickRadius,
                mainCircle);
        // painting the secondary circle
//        canvas.drawCircle((int) centerX, (int) centerY, joystickRadius / 2,
//                secondaryCircle);
        // paint lines
//        canvas.drawLine((float) centerX, (float) centerY, (float) centerX,
//                (float) (centerY - joystickRadius), verticalLine);
//        canvas.drawLine((float) (centerX - joystickRadius), (float) centerY,
//                (float) (centerX + joystickRadius), (float) centerY,
//                horizontalLine);
//        canvas.drawLine((float) centerX, (float) (centerY + joystickRadius),
//                (float) centerX, (float) centerY, horizontalLine);

        canvas.drawPath(up, arrow);
        canvas.drawPath(down, arrow);
        canvas.drawPath(right, arrow);
        canvas.drawPath(left, arrow);
        // painting the move button
        canvas.drawCircle(xPosition, yPosition, buttonRadius - pressedRingWidth, button);
        canvas.drawCircle(xPosition, yPosition, pressedRingRadius + animationProgress, focusPaint);

    }

    public float getAnimationProgress() {
        return animationProgress;
    }

    public void setAnimationProgress(float animationProgress) {
        this.animationProgress = animationProgress;
        this.invalidate();
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);

        Log.i("JoystickView","Pressed");
        if (button != null) {
            button.setColor(pressed ? pressedColor : defaultColor);
        }

        if (pressed) {
            showPressedRing();
            Log.i("JoystickView","showPressedRing");
        } else {
            hidePressedRing();
        }
    }

    private void hidePressedRing() {
        pressedAnimator.setFloatValues(pressedRingWidth, 0f);
        pressedAnimator.start();
    }

    private void showPressedRing() {
        pressedAnimator.setFloatValues(animationProgress, pressedRingWidth);
        pressedAnimator.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        xPosition = (int) event.getX();
        yPosition = (int) event.getY();

        double abs = Math.sqrt((xPosition - centerX) * (xPosition - centerX)
                + (yPosition - centerY) * (yPosition - centerY));
        if (abs > joystickRadius) {
            xPosition = (int) ((xPosition - centerX) * joystickRadius / abs + centerX);
            yPosition = (int) ((yPosition - centerY) * joystickRadius / abs + centerY);
        }
        invalidate();
        if (event.getAction() == MotionEvent.ACTION_UP) {
            xPosition = (int) centerX;
            yPosition = (int) centerY;
            thread.interrupt();
            if (onJoystickMoveListener != null)
                onJoystickMoveListener.onValueChanged(getAngle(), getPower(),
                        getDirection());
            if (button != null) {
                button.setColor(defaultColor);
            }
            hidePressedRing();
        }
        if (onJoystickMoveListener != null
                && event.getAction() == MotionEvent.ACTION_DOWN) {
            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }
            thread = new Thread(this);
            thread.start();
            if (onJoystickMoveListener != null)
                onJoystickMoveListener.onValueChanged(getAngle(), getPower(),
                        getDirection());
            if (button != null) {
                button.setColor(pressedColor);
            }
            showPressedRing();
        }
        return true;
    }

    private int getAngle() {
        if (xPosition > centerX) {
            if (yPosition < centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY)
                        / (xPosition - centerX))
                        * RAD + 90);
            } else if (yPosition > centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY)
                        / (xPosition - centerX)) * RAD) + 90;
            } else {
                return lastAngle = 90;
            }
        } else if (xPosition < centerX) {
            if (yPosition < centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY)
                        / (xPosition - centerX))
                        * RAD - 90);
            } else if (yPosition > centerY) {
                return lastAngle = (int) (Math.atan((yPosition - centerY)
                        / (xPosition - centerX)) * RAD) - 90;
            } else {
                return lastAngle = -90;
            }
        } else {
            if (yPosition <= centerY) {
                return lastAngle = 0;
            } else {
                if (lastAngle < 0) {
                    return lastAngle = -180;
                } else {
                    return lastAngle = 180;
                }
            }
        }
    }

    private int getPower() {
        return (int) (100 * Math.sqrt((xPosition - centerX)
                * (xPosition - centerX) + (yPosition - centerY)
                * (yPosition - centerY)) / joystickRadius);
    }

    private int getDirection() {
        if (lastPower == 0 && lastAngle == 0) {
            return 0;
        }
        int a = 0;
        if (lastAngle <= 0) {
            a = (lastAngle * -1) + 90;
        } else if (lastAngle > 0) {
            if (lastAngle <= 90) {
                a = 90 - lastAngle;
            } else {
                a = 360 - (lastAngle - 90);
            }
        }

        int direction = (int) (((a + 22) / 45) + 1);

        if (direction > 8) {
            direction = 1;
        }
        return direction;
    }

    public void setOnJoystickMoveListener(OnJoystickMoveListener listener,
                                          long repeatInterval) {
        this.onJoystickMoveListener = listener;
        this.loopInterval = repeatInterval;
    }

    public interface OnJoystickMoveListener {
        public void onValueChanged(int angle, int power, int direction);
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            post(new Runnable() {
                public void run() {
                    if (onJoystickMoveListener != null)
                        onJoystickMoveListener.onValueChanged(getAngle(),
                                getPower(), getDirection());
                }
            });
            try {
                Thread.sleep(loopInterval);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}

