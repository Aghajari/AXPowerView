package com.aghajari.powerview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * @author AmirHosseinAghajari
 * @version 1.00
 * @see <a href="https://github.com/Aghajari/AXPowerView">https://github.com/Aghajari/AXPowerView</a>
 */
public class AXPowerView extends View {

    // VIEW SIZE
    protected int size = 0;
    protected RectF bounds, innerBounds;
    protected final List<Animator> animators = new ArrayList<>();

    // CIRCLE DATA
    protected float start = 0.0f, end = 0.0f;
    protected float startV, endV;
    protected boolean focusOnEnd = false;

    // INNER VIEW
    boolean innerView = true;
    protected float innerStart = 0.0f, innerEnd = 0.0f;
    protected Line innerLine1 = null;
    protected Line innerLine2 = null;
    protected Line successLine1 = null;
    protected Line successLine2 = null;

    // ANIMATIONS DURATION
    long indeterminateDuration = 600;
    long succeedDuration = 400;
    long showDuration = 400;
    long delay = 80;

    // UI
    Paint paint, innerPaint;
    float thickness = 4, innerThickness = 3;
    int color = Color.rgb(85, 164, 241), innerColor = Color.BLACK;

    // VIEW STATES
    boolean isRunning = false;
    boolean autoStart = true;
    boolean firstAnimation = true;

    State state = State.HIDDEN;
    State nextState = null;
    InnerState innerState = InnerState.POWER;
    InnerState nextInnerState = null;

    AnimatorListener listener = null;

    // LOADING LOOP
    private Runnable indeterminate = new Runnable() {
        @Override
        public void run() {
            if (state == State.LOADING) {
                isRunning = true;
                startLoading(indeterminateDuration);
            } else if (state == State.POWER) {
                isRunning = false;
                stopAnimators();
            }
        }
    };

    public enum State {
        HIDDEN, POWER, LOADING, SUCCEED, RELOADING
    }

    protected enum InnerState {
        POWER, SUCCESS
    }

    public AXPowerView(Context context) {
        super(context);
        init(null, 0, 0);
    }

    public AXPowerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, 0);
    }

    public AXPowerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AXPowerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    private void init(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        thickness *= getContext().getResources().getDisplayMetrics().density;
        innerThickness *= getContext().getResources().getDisplayMetrics().density;

        boolean firstAnimation = true;

        if (attrs!=null) {
            final TypedArray a = getContext().obtainStyledAttributes(
                    attrs, R.styleable.AXPowerView, defStyleAttr, defStyleRes);
            color = a.getColor(R.styleable.AXPowerView_color, color);
            innerColor = a.getColor(R.styleable.AXPowerView_innerColor, innerColor);
            thickness = a.getDimension(R.styleable.AXPowerView_thickness, thickness);
            innerThickness = a.getDimension(R.styleable.AXPowerView_innerThickness, innerThickness);
            delay = a.getInteger(R.styleable.AXPowerView_delay, (int) delay);
            showDuration = a.getInteger(R.styleable.AXPowerView_showDuration, (int) showDuration);
            succeedDuration = a.getInteger(R.styleable.AXPowerView_succeedDuration, (int) succeedDuration);
            indeterminateDuration = a.getInteger(R.styleable.AXPowerView_indeterminateDuration, (int) indeterminateDuration);
            autoStart = a.getBoolean(R.styleable.AXPowerView_autoStart, autoStart);
            innerView = a.getBoolean(R.styleable.AXPowerView_innerViewEnabled, innerView);
            firstAnimation = a.getBoolean(R.styleable.AXPowerView_firstAnimation, firstAnimation);

            if (a.hasValue(R.styleable.AXPowerView_state)) {
                int stateMode = a.getInt(R.styleable.AXPowerView_state, 0);
                switch (stateMode) {
                    case 0:
                        state = State.HIDDEN;
                        break;
                    case 1:
                        state = State.POWER;
                        innerState = InnerState.POWER;
                        break;
                    case 2:
                        state = State.LOADING;
                        innerState = InnerState.POWER;
                        break;
                    case 3:
                        state = State.SUCCEED;
                        innerState = InnerState.SUCCESS;
                        break;
                }
            }
            a.recycle();
        }

        bounds = new RectF();
        innerBounds = new RectF();

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setPathEffect(new CornerPathEffect(100));
        paint.setAntiAlias(true);
        paint.setDither(true);

        innerPaint = new Paint();
        innerPaint.setStyle(Paint.Style.STROKE);
        innerPaint.setStrokeJoin(Paint.Join.ROUND);
        innerPaint.setStrokeCap(Paint.Cap.ROUND);
        innerPaint.setPathEffect(new CornerPathEffect(100));
        innerPaint.setAntiAlias(true);
        innerPaint.setDither(true);

        updatePaint();

        if (state == State.LOADING || !firstAnimation) {
            setState(state, false);
        } else {
            state = State.HIDDEN;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int xPadding = getPaddingLeft() + getPaddingRight();
        int yPadding = getPaddingTop() + getPaddingBottom();
        int width = getMeasuredWidth() - xPadding;
        int height = getMeasuredHeight() - yPadding;
        size = Math.min(width, height);
        setMeasuredDimension(size + xPadding, size + yPadding);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        size = Math.min(w, h);
        updateBounds();
    }

    /** update view bounds */
    protected void updateBounds() {
        bounds.set(getPaddingLeft() + thickness,
                getPaddingTop() + thickness,
                size - getPaddingRight() - thickness,
                size - getPaddingBottom() - thickness);


        float innerSize = (size - calculateInnerSize(size)) / 2;
        innerBounds.set(bounds.left + innerSize,
                getPaddingTop() + innerThickness + innerSize,
                size - getPaddingRight() - innerThickness - innerSize,
                size - getPaddingBottom() - innerThickness - innerSize);

        updateSuccessLines();
        if (!isRunning) setState(state, false);
    }

    /** calculate and return innerView size */
    protected float calculateInnerSize(int size) {
        return (size / 3f) + (getContext().getResources().getDisplayMetrics().density * (innerThickness / 2));
    }

    /** create lines for the success state  */
    protected void updateSuccessLines() {
        successLine1 = new Line();
        successLine1.stopY = innerBounds.bottom - (innerBounds.height() / 5);
        successLine1.stopX = innerBounds.centerX() - (innerBounds.width() / 6);
        successLine1.startX = innerBounds.left;
        successLine1.startY = successLine1.stopY - (successLine1.stopX - successLine1.startX);

        successLine2 = new Line();
        successLine2.startY = successLine1.stopY;
        successLine2.startX = successLine1.stopX;
        successLine2.stopX = innerBounds.right;
        successLine2.stopY = successLine2.startY + (successLine2.startX - successLine2.stopX);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (autoStart && state == State.HIDDEN)
            setState(findState(innerState), firstAnimation);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        clearAnimation();
    }

    @Override
    public void setVisibility(int visibility) {
        int currentVisibility = getVisibility();
        super.setVisibility(visibility);
        if (visibility != currentVisibility) {
            stopAnimators();
            if (visibility == GONE || visibility == INVISIBLE) {
                setState(state, false);
            } else if (visibility == VISIBLE) {
                setState(state, true);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // DESIGNER
        if (isInEditMode()) {
            float from = -90;
            float circle = 360 - start;
            canvas.drawArc(bounds, from, circle, false, paint);

            if (innerView) {
                if (innerState == InnerState.POWER) {
                    canvas.drawArc(innerBounds, -45, 270, false, innerPaint);
                    initPowerLine();
                    canvas.drawLine(innerLine1.startX, innerLine1.startY, innerLine1.stopX, innerLine1.stopY, innerPaint);
                } else {
                    if (successLine1 == null || successLine2 == null) updateSuccessLines();
                    canvas.drawLine(successLine1.startX, successLine1.startY, successLine1.stopX, successLine1.stopY, innerPaint);
                    canvas.drawLine(successLine2.startX, successLine2.startY, successLine2.stopX, successLine2.stopY, innerPaint);
                }
            }
            return;
        }

        drawArcShadow(canvas, 3);
        drawArcShadow(canvas, 2);
        drawArcShadow(canvas, 1);

        paint.setStrokeWidth(thickness);
        paint.setAlpha(255);
        float from = start - 90;
        float circle = end - start;
        canvas.drawArc(bounds, from, circle, false, paint);

        if (innerView) {
            canvas.drawArc(innerBounds, innerStart - 90, innerEnd - innerStart, false, innerPaint);

            if (innerLine1 != null) {
                canvas.drawLine(innerLine1.startX, innerLine1.startY, innerLine1.stopX, innerLine1.stopY, innerPaint);
            }
            if (innerLine2 != null) {
                canvas.drawLine(innerLine2.startX, innerLine2.startY, innerLine2.stopX, innerLine2.stopY, innerPaint);
            }
        }
    }

    /** draw shadow of the main circular depends on the velocity */
    protected void drawArcShadow(Canvas canvas, int step) {
        float start = this.start;
        float end = this.end;
        paint.setAlpha(180 / step);
        paint.setStrokeWidth(thickness / (step + 1));

        if (startV > 0) {
            float deltaStart = getArcShadowLength(true, step);
            start -= deltaStart;
        }

        if (endV > 0) {
            float deltaEnd = getArcShadowLength(false, step);
            end += deltaEnd;
        }

        float from = start - 90;
        float circle = end - start;
        canvas.drawArc(bounds, from, circle, false, paint);
    }

    /** calculate shadow size of the main circular depends on the velocity */
    protected float getArcShadowLength(boolean isStart, int step) {
        if (isStart) {
            if (!focusOnEnd) {
                return 15.0f * (float) Math.pow(2, step - 1) * startV;
            } else {
                return 5.0f * (float) Math.pow(2, step - 1) * startV;
            }
        } else {
            if (focusOnEnd) {
                return 15.0f * (float) Math.pow(2, step - 1) * endV;
            } else {
                return 5.0f * (float) Math.pow(2, step - 1) * endV;
            }
        }
    }

    // PRIVATE METHODS

    /** show view */
    private void show(boolean animation) {
        if (animation) {
            showWithAnimation();
        } else {
            stopAnimators();
            start = 0.0f;
            end = 360.0f;
            showInnerState();
            invalidate();
        }
    }

    /** start showing animation */
    private void showWithAnimation() {
        final float firstPlace = -90.0f;
        final float length = 10.0f;
        start = firstPlace;
        end = firstPlace + length;
        stopAnimators();
        isRunning = true;
        focusOnEnd = false;

        final long d1 = 80;
        final long d2 = 80;
        long duration = showDuration - d1 - d2;

        final float v = (10.0f) / d1;
        final ValueAnimator firstAnimator = ValueAnimator.ofFloat(0, d1);
        firstAnimator.setDuration(d1);
        firstAnimator.setInterpolator(new DecelerateInterpolator());
        firstAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float delta = v * (float) valueAnimator.getAnimatedValue();
                startV = valueAnimator.getCurrentPlayTime() * v / d1;
                endV = startV;
                start = firstPlace + delta;
                end = firstPlace + length + delta;
                invalidate();
            }
        });

        final float v2 = (Math.abs(firstPlace) - 10.0f) / d2;
        final ValueAnimator secondAnimator = ValueAnimator.ofFloat(0, d2);
        secondAnimator.setDuration(d2);
        secondAnimator.setInterpolator(new DecelerateInterpolator());
        secondAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float delta = v2 * (float) valueAnimator.getAnimatedValue();
                startV = valueAnimator.getCurrentPlayTime() * v2 / d2;
                endV = startV;
                start = firstPlace + 10.0f + delta;
                end = firstPlace + length + 10.0f + delta;
                invalidate();
            }
        });

        duration = duration / 2;

        final float deltaX = (360.0f - length) / 2;
        final float a = (deltaX * 2) / (float) Math.pow(duration, 2);

        final ValueAnimator startAnimator1 = ValueAnimator.ofFloat(0, duration);
        startAnimator1.setDuration(duration);
        startAnimator1.setInterpolator(new LinearInterpolator());
        startAnimator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                end = (float) (0.5 * a * Math.pow(valueAnimator.getCurrentPlayTime(), 2));
                end += length;
                invalidate();
            }
        });

        final float v0 = a * duration;
        final float a2 = (float) -Math.pow(v0, 2) / (2 * deltaX);
        final long duration2 = (long) Math.sqrt((deltaX * 2) / -a2);

        final ValueAnimator startAnimator2 = ValueAnimator.ofFloat(0, duration2);
        startAnimator2.setDuration(duration2);
        startAnimator2.setInterpolator(new LinearInterpolator());
        startAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                end = (float) (0.5 * a2 * Math.pow(valueAnimator.getCurrentPlayTime(), 2));
                end += v0 * valueAnimator.getCurrentPlayTime();
                end += length + deltaX;
                invalidate();
            }
        });

        animators.add(firstAnimator);
        animators.add(secondAnimator);
        animators.add(startAnimator1);
        animators.add(startAnimator2);

        firstAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                secondAnimator.start();
            }
        });

        secondAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                startV = 0;
                endV = 0;
                focusOnEnd = false;
                startAnimator1.start();
                showInnerStateWithAnim(startAnimator1.getDuration() + startAnimator2.getDuration(), true);
            }
        });
        startAnimator1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                startAnimator2.start();
            }
        });
        startAnimator2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                show(false);
                isRunning = false;
                animationFinished();
            }
        });
        firstAnimator.start();
    }

    /** create line for the power state */
    private void initPowerLine() {
        innerLine1 = new Line();
        innerLine1.startX = innerBounds.centerX();
        innerLine1.stopX = innerBounds.centerX();
        innerLine1.startY = innerBounds.top - (getContext().getResources().getDisplayMetrics().density * 4);
        innerLine1.stopY = innerBounds.centerY() + (getContext().getResources().getDisplayMetrics().density * 2);
    }

    /** load innerView data */
    private void showInnerState() {
        if (!innerView) return;

        if (innerState == InnerState.POWER) {
            innerStart = 45.0f;
            innerEnd = 360.0f - innerStart;

            innerLine2 = null;
            initPowerLine();
        } else if (innerState == InnerState.SUCCESS) {
            innerStart = 0;
            innerEnd = 0;

            if (successLine1 == null || successLine2 == null)
                updateSuccessLines();
            innerLine1 = new Line(successLine1);
            innerLine2 = new Line(successLine2);
        }
    }

    /** start showing innerView animation */
    private void showInnerStateWithAnim(long duration, boolean alpha) {
        if (!innerView) return;
        innerLine1 = null;
        innerLine2 = null;

        if (alpha) {
            final ValueAnimator alphaAnimator = ValueAnimator.ofInt(0, 255);
            alphaAnimator.setDuration(duration);
            alphaAnimator.setInterpolator(new LinearInterpolator());
            alphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    innerPaint.setAlpha((Integer) valueAnimator.getAnimatedValue());
                    invalidate();
                }
            });
            alphaAnimator.start();
            animators.add(alphaAnimator);
        }

        if (innerState == InnerState.POWER) {
            innerStart = 45.0f;
            innerEnd = innerStart;

            final float v = (360.0f - (2 * innerStart)) / duration;
            final ValueAnimator startAnimator1 = ValueAnimator.ofFloat(0, duration);
            startAnimator1.setDuration(duration);
            startAnimator1.setInterpolator(new LinearInterpolator());
            startAnimator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    innerEnd = v * valueAnimator.getCurrentPlayTime();
                    innerEnd += innerStart;
                    innerEnd = Math.min(360.0f - innerStart, innerEnd);
                    invalidate();
                }
            });
            startAnimator1.start();

            innerLine1 = new Line();
            innerLine1.startX = innerBounds.centerX();
            innerLine1.stopX = innerBounds.centerX();

            final Line targetLine = new Line();
            targetLine.startY = innerBounds.top - (getContext().getResources().getDisplayMetrics().density * 4);
            targetLine.stopY = innerBounds.centerY() + (getContext().getResources().getDisplayMetrics().density * 2);
            innerLine1.startY = targetLine.startY;

            final float v2 = (targetLine.stopY - targetLine.startY) / duration;
            final ValueAnimator startAnimator2 = ValueAnimator.ofFloat(0, duration);
            startAnimator2.setDuration(duration);
            startAnimator2.setInterpolator(new LinearInterpolator());
            startAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    if (innerLine1 == null) return;
                    float delta = v2 * valueAnimator.getCurrentPlayTime();
                    innerLine1.stopY = innerLine1.startY + delta;
                    innerLine1.stopY = Math.min(innerLine1.stopY, targetLine.stopY);
                    invalidate();
                }
            });
            startAnimator2.start();

            animators.add(startAnimator1);
            animators.add(startAnimator2);
        } else if (innerState == InnerState.SUCCESS) {
            innerStart = 0;
            innerEnd = 0;

            if (successLine1 == null || successLine2 == null)
                updateSuccessLines();
            innerLine1 = new Line(successLine1);
            innerLine2 = new Line(successLine2);


            final float v = Math.abs(innerLine1.startY - innerLine1.stopY) / (duration / 2f);
            innerLine1.stopY = innerLine1.startY;
            innerLine1.stopX = innerLine1.startX;

            final ValueAnimator startAnimator1 = ValueAnimator.ofFloat(0, duration / 2f);
            startAnimator1.setDuration(duration / 2);
            startAnimator1.setInterpolator(new LinearInterpolator());
            startAnimator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float delta = v * valueAnimator.getCurrentPlayTime();
                    innerLine1.stopX = innerLine1.startX + delta;
                    innerLine1.stopY = innerLine1.startY + delta;
                    invalidate();
                }
            });

            final float v2 = Math.abs(innerLine2.stopX - innerLine2.startX) / (duration / 2f);
            innerLine2 = null;

            final ValueAnimator startAnimator2 = ValueAnimator.ofFloat(0, duration / 2f);
            startAnimator2.setDuration(duration / 2);
            startAnimator2.setInterpolator(new LinearInterpolator());
            startAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    float delta = v2 * valueAnimator.getCurrentPlayTime();
                    innerLine2.stopX = innerLine2.startX + delta;
                    innerLine2.stopY = innerLine2.startY - delta;
                    invalidate();
                }
            });
            startAnimator2.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    innerLine1 = new Line(successLine1);
                    innerLine2 = new Line(successLine2);
                    invalidate();
                }
            });

            startAnimator1.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    innerLine1 = new Line(successLine1);
                    innerLine2 = new Line(successLine2);
                    innerLine2.stopX = innerLine2.startX;
                    innerLine2.stopY = innerLine2.startY;
                    startAnimator2.start();
                }
            });
            startAnimator1.start();

            animators.add(startAnimator1);
            animators.add(startAnimator2);
        }
    }

    private float last_start, last_end;

    /** start loading animation */
    private void startLoadingNow() {
        state = State.LOADING;
        startLoading(indeterminateDuration);
        hidePowerLineAnim();
    }

    /** start hiding power's line animation */
    private void hidePowerLineAnim() {
        if (!innerView) return;
        innerState = InnerState.POWER;
        showInnerState();

        final long duration = 140;
        final ValueAnimator lineAnimator = ValueAnimator.ofFloat(innerLine1.stopY - innerLine1.startY, 0);
        lineAnimator.setDuration(duration);
        lineAnimator.setInterpolator(new LinearInterpolator());
        lineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (innerLine1 == null) return;
                innerLine1.stopY = innerLine1.startY + (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        lineAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                innerLine1 = null;
            }
        });
        lineAnimator.start();
        animators.add(lineAnimator);
    }

    /** start loading (main circular) animation */
    private void startLoading(final long duration) {
        start = 0.0f;
        end = 360.0f;
        isRunning = true;
        focusOnEnd = false;

        final float a = ((360) * 2) / (float) Math.pow(duration, 2);

        ValueAnimator startAnimator1 = ValueAnimator.ofFloat(0, duration);
        startAnimator1.setDuration(duration);
        startAnimator1.setInterpolator(new LinearInterpolator());
        startAnimator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                start = (float) (0.5 * a * Math.pow(valueAnimator.getCurrentPlayTime(), 2));
                startV = a * valueAnimator.getCurrentPlayTime();
                invalidate();
            }
        });

        final float v0 = a * duration;
        final float a2 = (float) -Math.pow(v0, 2) / (2 * 110);
        final long duration2 = (long) Math.sqrt((110 * 2) / -a2);

        final ValueAnimator startAnimator2 = ValueAnimator.ofFloat(0, duration2);
        startAnimator2.setDuration(duration2);
        startAnimator2.setInterpolator(new LinearInterpolator());
        startAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                start = 360.0f;
                start += (float) (0.5 * a2 * Math.pow(valueAnimator.getCurrentPlayTime(), 2));
                start += v0 * valueAnimator.getCurrentPlayTime();
                startV = a2 * valueAnimator.getCurrentPlayTime() + v0;
                invalidate();
            }
        });

        final long end_duration = duration + duration2;
        final float a_end = (160 * 2) / (float) Math.pow(end_duration, 2);

        final ValueAnimator endAnimator = ValueAnimator.ofFloat(0, end_duration);
        endAnimator.setDuration(end_duration);
        endAnimator.setInterpolator(new LinearInterpolator());
        endAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                end = 360.0f;
                end += (float) (0.5 * a_end * Math.pow(valueAnimator.getCurrentPlayTime(), 2));
                endV = a_end * valueAnimator.getCurrentPlayTime();
                invalidate();
            }
        });

        //NextRound
        final long duration3 = duration + duration2;
        final float v = (float) (360 - 110) / duration3;
        final ValueAnimator startAnimator3 = ValueAnimator.ofFloat(0, duration3);
        startAnimator3.setDuration(duration3);
        startAnimator3.setInterpolator(new LinearInterpolator());
        startAnimator3.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float delta = v * (float) valueAnimator.getAnimatedValue();
                startV = v;
                start = last_start + delta;
                invalidate();
            }
        });

        float deltaX = (360 + (360 - 160));
        deltaX = deltaX * 3 / 4;
        final long duration4 = duration3 * 3 / 4;
        final float a_end2 = ((deltaX - (v * duration4)) * 2) / (float) Math.pow(duration4, 2);

        final ValueAnimator endAnimator2 = ValueAnimator.ofFloat(0, duration4);
        endAnimator2.setDuration(duration4);
        endAnimator2.setInterpolator(new LinearInterpolator());
        endAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                end = (float) (0.5 * a_end2 * Math.pow(valueAnimator.getCurrentPlayTime(), 2));
                end += v * valueAnimator.getCurrentPlayTime();
                end += last_end;
                endV = a_end2 * valueAnimator.getCurrentPlayTime() + v;
                invalidate();
            }
        });

        final float v02 = a_end2 * duration4;
        final float a_end3 = (float) -Math.pow(v02, 2) / (2 * deltaX / 3);
        final long duration5 = (long) Math.sqrt((2 * deltaX / 3) / -a_end3);

        final ValueAnimator endAnimator3 = ValueAnimator.ofFloat(0, duration5);
        endAnimator3.setDuration(duration5);
        endAnimator3.setInterpolator(new LinearInterpolator());
        endAnimator3.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                end = last_end;
                end += (float) (0.5 * a_end3 * Math.pow(valueAnimator.getCurrentPlayTime(), 2));
                end += v02 * valueAnimator.getCurrentPlayTime();
                endV = a_end3 * valueAnimator.getCurrentPlayTime() + v02;
                invalidate();
            }
        });

        startAnimator1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                startAnimator2.start();
            }
        });
        endAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                last_start = start;
                last_end = end;
                startAnimator3.start();
                endAnimator2.start();
            }
        });

        endAnimator.start();
        startAnimator1.start();

        endAnimator2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                last_end = end;
                endAnimator3.start();
            }
        });
        endAnimator3.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animationFinished();
            }
        });

        animators.add(endAnimator);
        animators.add(endAnimator2);
        animators.add(endAnimator3);
        animators.add(startAnimator1);
        animators.add(startAnimator2);
        animators.add(startAnimator3);

        final long maxDuration = endAnimator.getDuration() + endAnimator2.getDuration() + endAnimator3.getDuration();

        if (innerView) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    startInnerLoading(maxDuration, duration2);
                }
            }, 140);
        }
    }

    /** start loading (innerView) animation */
    private void startInnerLoading(final long maxDuration, final long duration2) {
        if (!innerView) return;

        InnerState nextState = getNextInnerState();
        innerState = nextState;
        nextInnerState = null;

        if (nextState == InnerState.POWER) {
            startInnerPowerLoading(maxDuration, duration2);
        } else if (innerState == InnerState.SUCCESS) {
            startInnerSuccessLoading(maxDuration, duration2);
        }
    }

    /** start loading (power) animation */
    private void startInnerPowerLoading(final long maxDuration, final long duration2) {
        innerStart = 45.0f;
        innerEnd = 360.0f - innerStart;
        innerLine2 = null;

        final float firstPlaceOfStart = innerStart;
        final float firstPlaceOfEnd = innerEnd;

        final long duration = (maxDuration - (delay * 4)) / 2;
        final float v = (innerEnd) / duration;

        final float aD = 200;
        final long deltaT = (long) (aD / v);
        final long secondDuration = duration - deltaT;

        final float deltaD = 360 - (aD - 45);
        final float a = (float) ((deltaD - (v * secondDuration)) * 2 / Math.pow(secondDuration, 2));

        final ValueAnimator firstAnimator = ValueAnimator.ofFloat(0, duration);
        firstAnimator.setDuration(duration);
        firstAnimator.setInterpolator(new LinearInterpolator());
        firstAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float delta = v * valueAnimator.getCurrentPlayTime();
                innerEnd = firstPlaceOfEnd - delta;
                if (delta <= aD) {
                    innerStart = firstPlaceOfStart - delta;
                } else {
                    long time = valueAnimator.getCurrentPlayTime() - deltaT;

                    innerStart = (float) (0.5 * a * Math.pow(time, 2));
                    innerStart += v * time;
                    innerStart += aD;
                    innerStart = firstPlaceOfStart - innerStart;
                }
                invalidate();
            }
        });

        //Next round

        final ValueAnimator animator = ValueAnimator.ofFloat(0, 315);
        animator.setDuration(duration);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                innerStart = -(float) valueAnimator.getAnimatedValue();
            }
        });

        final float deltaX = 405;
        final float ratio = getInnerLoadingRatio();
        System.out.println(ratio);
        final float deltaX_back = deltaX * ratio;
        final long duration_back = (long) (duration * ratio);
        final float deltaX_back2 = deltaX - deltaX_back;
        final long duration_back2 = duration - duration_back;
        final float a_back1 = (float) ((deltaX_back) * 2 / Math.pow(duration_back, 2));
        final float max_v_back = a_back1 * duration_back;
        final float a_back2 = (float) ((deltaX_back2 - (max_v_back * duration_back2)) * 2 / Math.pow(duration_back2, 2));

        final ValueAnimator animator2 = ValueAnimator.ofFloat(0, 315);
        animator2.setDuration(animator.getDuration());
        animator2.setInterpolator(new LinearInterpolator());
        animator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                long time = valueAnimator.getCurrentPlayTime();
                if (time > duration_back) {
                    time -= duration_back;
                    innerEnd = (float) (0.5 * a_back2 * Math.pow(time, 2));
                    innerEnd += max_v_back * time;
                    innerEnd += deltaX_back;
                    innerEnd = 360 - innerEnd;
                } else {
                    innerEnd = 360 - (float) (0.5 * a_back1 * Math.pow(time, 2));
                }
            }
        });

        firstAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        animator.start();
                        animator2.start();
                    }
                }, delay);
            }
        });
        firstAnimator.start();
        animators.add(firstAnimator);
        animators.add(animator);
        animators.add(animator2);
    }

    /** return power loading acceleration ratio */
    protected float getInnerLoadingRatio() {
        final long deltaT = (700 - indeterminateDuration)/100;
        return (float) Math.max((0.1f - (deltaT*0.02)),0.005);
    }

    /** start changing state to the success */
    private void startInnerSuccessLoading(final long maxDuration, final long duration2) {
        innerStart = 45.0f;
        innerEnd = 360.0f - innerStart;

        final float firstPlaceOfStart = innerStart;
        final float firstPlaceOfEnd = innerEnd;

        final long duration = maxDuration / 3;
        final float target = -90;
        final float vStart = (Math.abs(target) + Math.abs(innerStart)) / duration;
        final float endDeltaX = Math.abs(innerEnd) + Math.abs(target);
        final float aEnd = endDeltaX * 2 / (float) Math.pow(duration, 2);

        final ValueAnimator firstAnimator = ValueAnimator.ofFloat(0, duration);
        firstAnimator.setDuration(duration);
        firstAnimator.setInterpolator(new LinearInterpolator());
        firstAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                innerStart = firstPlaceOfStart - (vStart * valueAnimator.getCurrentPlayTime());
                innerEnd = (float) (firstPlaceOfEnd - (0.5 * aEnd * Math.pow(valueAnimator.getCurrentPlayTime(), 2)));
                innerStart = Math.max(innerStart, target);
                innerEnd = Math.max(innerEnd, target);
                invalidate();
            }
        });

        firstAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                showInnerStateWithAnim(maxDuration / 3, false);
            }
        });
        firstAnimator.start();
        animators.add(firstAnimator);
    }

    /** start changing state to the power from the loading */
    private void backToShowingFromLoading() {
        start = 0.0f;
        end = 360.0f;

        if (!innerView) return;
        innerState = InnerState.POWER;
        showInnerState();

        final long duration = 140;
        final ValueAnimator lineAnimator = ValueAnimator.ofFloat(0, innerLine1.stopY - innerLine1.startY);
        lineAnimator.setDuration(duration);
        lineAnimator.setInterpolator(new LinearInterpolator());
        lineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (innerLine1 == null) return;
                innerLine1.startY = innerLine1.stopY - (float) valueAnimator.getAnimatedValue();
                invalidate();
            }
        });
        lineAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                animationFinished();
            }
        });
        lineAnimator.start();
        animators.add(lineAnimator);
    }

    /** start changing state to the power from the success */
    private void backToShowingFromSuccess() {
        startReloadAnimation(indeterminateDuration);

        if (!innerView) return;
        innerStart = 0;
        innerEnd = 0;
        innerState = InnerState.SUCCESS;
        showInnerState();
        innerState = InnerState.POWER;

        final long duration = indeterminateDuration * 2;
        final long lineDuration = duration / 8;

        final Line targetInnerLine1 = new Line(successLine1);
        innerLine1 = new Line(successLine1);
        final Line targetInnerLine2 = new Line(successLine2);
        innerLine2 = new Line(successLine2);

        final float v = Math.abs(targetInnerLine1.startY - targetInnerLine1.stopY) / (lineDuration);
        final ValueAnimator startAnimator1 = ValueAnimator.ofFloat(0, lineDuration);
        startAnimator1.setDuration(lineDuration);
        startAnimator1.setInterpolator(new LinearInterpolator());
        startAnimator1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float delta = v * valueAnimator.getCurrentPlayTime();
                innerLine1.stopX = targetInnerLine1.stopX - delta;
                innerLine1.stopY = targetInnerLine1.stopY - delta;
                invalidate();
            }
        });

        final float v2 = Math.abs(targetInnerLine2.stopX - targetInnerLine2.startX) / lineDuration;
        final ValueAnimator startAnimator2 = ValueAnimator.ofFloat(0, lineDuration);
        startAnimator2.setDuration(lineDuration);
        startAnimator2.setInterpolator(new LinearInterpolator());
        startAnimator2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float delta = v2 * valueAnimator.getCurrentPlayTime();
                innerLine2.stopX = targetInnerLine2.stopX - delta;
                innerLine2.stopY = targetInnerLine2.stopY + delta;
                invalidate();
            }
        });

        final long deltaT = duration - (lineDuration * 2);
        final float v3 = 135f / deltaT;
        final float deltaX = 540f / 2;
        final float a = deltaX / (float) Math.pow(deltaT / 2f, 2);
        final float maxV = a * (deltaT / 2f);

        final Line targetLine = new Line();
        targetLine.startY = innerBounds.top - (getContext().getResources().getDisplayMetrics().density * 4);
        targetLine.stopY = innerBounds.centerY() + (getContext().getResources().getDisplayMetrics().density * 2);

        final long powerLineDuration = deltaT / 4;
        final float vPowerLine = (targetLine.stopY - targetLine.startY) / powerLineDuration;
        final ValueAnimator startPowerLineAnimator = ValueAnimator.ofFloat(0, powerLineDuration);
        startPowerLineAnimator.setDuration(powerLineDuration);
        startPowerLineAnimator.setInterpolator(new LinearInterpolator());
        startPowerLineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (innerLine1 == null) return;
                float delta = vPowerLine * valueAnimator.getCurrentPlayTime();
                innerLine1.startY = targetLine.stopY - delta;
                invalidate();
            }
        });

        final ValueAnimator startAnimator3 = ValueAnimator.ofFloat(0, deltaT / 2f);
        startAnimator3.setDuration(deltaT);
        startAnimator3.setInterpolator(new LinearInterpolator());
        startAnimator3.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                innerStart = Math.min(-90 + v3 * valueAnimator.getCurrentPlayTime(), 45);
                if (valueAnimator.getCurrentPlayTime() > (valueAnimator.getDuration() / 2)) {
                    long time = valueAnimator.getCurrentPlayTime() - (valueAnimator.getDuration() / 2);
                    innerEnd = (float) (0.5 * -a * Math.pow(time, 2));
                    innerEnd += maxV * time;
                    innerEnd += deltaX;
                } else {
                    innerEnd = (float) (0.5 * a * Math.pow(valueAnimator.getCurrentPlayTime(), 2));
                }
                innerEnd = -90 + innerEnd;
                innerEnd = Math.min(315f, innerEnd);

                if (innerStart > 0 && innerLine1 == null) {
                    innerLine1 = new Line();
                    innerLine1.startX = innerBounds.centerX();
                    innerLine1.stopX = innerBounds.centerX();
                    innerLine1.stopY = targetLine.stopY;
                    if (!startPowerLineAnimator.isRunning())
                        startPowerLineAnimator.start();
                }
                invalidate();
            }
        });
        startAnimator3.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                innerStart = 45f;
                innerEnd = 315f;
            }
        });

        startAnimator1.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                innerLine2 = null;
                innerLine1 = null;
                startAnimator3.start();
            }
        });
        startAnimator2.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                innerLine2 = null;
                startAnimator1.start();
            }
        });
        startAnimator2.start();
        animators.add(startAnimator1);
        animators.add(startAnimator2);
        animators.add(startAnimator3);
        animators.add(startPowerLineAnimator);
    }

    /** start reloading animation */
    protected void startReloadAnimation(final long duration) {
        start = 0.0f;
        end = 360.0f;

        final float target1 = 250.0f;
        final float deltaX = 360.0f + target1 - 5.0f;
        final float a = deltaX / (float) Math.pow(duration / 2f, 2);
        final float maxV = a * duration / 2;
        final float maxStartV = target1 / (duration / 2f);

        final ValueAnimator firstAnimator = ValueAnimator.ofFloat(0, target1);
        firstAnimator.setDuration(duration);
        firstAnimator.setInterpolator(new LinearInterpolator());
        firstAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                start = -(float) valueAnimator.getAnimatedValue();

                if (valueAnimator.getCurrentPlayTime() > valueAnimator.getDuration() / 2) {
                    long deltaT = valueAnimator.getCurrentPlayTime() - (valueAnimator.getDuration() / 2);
                    end = (float) (0.5 * -a * Math.pow(deltaT, 2));
                    end += maxV * deltaT;
                    end += deltaX / 2;

                    startV = deltaT * maxStartV / (valueAnimator.getDuration() / 2f);
                    startV = maxStartV - startV;
                    endV = (-a) * deltaT + maxV;
                } else {
                    end = (float) (0.5 * a * Math.pow(valueAnimator.getCurrentPlayTime(), 2));

                    startV = valueAnimator.getCurrentPlayTime() * maxStartV / (valueAnimator.getDuration() / 2f);
                    endV = a * valueAnimator.getCurrentPlayTime();
                }
                end = 360.0f - end;
                invalidate();
            }
        });

        final float target2 = 360 - target1;
        final float deltaX2 = 360 + target2;
        final float a2 = deltaX2 / (float) Math.pow(duration / 2f, 2);
        final float maxV2 = a2 * duration / 2;
        //final float maxEndV = target1 / (duration/2);

        final ValueAnimator secondAnimator = ValueAnimator.ofFloat(-245.0f, -366.0f);
        secondAnimator.setDuration(duration);
        secondAnimator.setInterpolator(new LinearInterpolator());
        secondAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                end = Math.max(-360f, (float) valueAnimator.getAnimatedValue());

                if (valueAnimator.getCurrentPlayTime() > valueAnimator.getDuration() / 2) {
                    long deltaT = valueAnimator.getCurrentPlayTime() - (valueAnimator.getDuration() / 2);
                    start = (float) (0.5 * -a2 * Math.pow(deltaT, 2));
                    start += maxV2 * deltaT;
                    start += deltaX2 / 2;
                } else {
                    start = (float) (0.5 * a2 * Math.pow(valueAnimator.getCurrentPlayTime(), 2));
                }
                start = -250 - start;
                invalidate();
            }
        });

        secondAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                start = 0;
                end = 360;

                changeState(State.RELOADING, State.POWER);
                if (listener != null)
                    listener.onAnimationEnded(State.RELOADING, state);
            }
        });

        firstAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                startV = 0;
                endV = 0;
                secondAnimator.start();
            }
        });
        firstAnimator.start();
        animators.add(firstAnimator);
        animators.add(secondAnimator);
    }

    /** stop all animators */
    private void stopAnimators() {
        try {
            synchronized (animators) {
                for (Animator animator : animators) {
                    animator.cancel();
                }
                animators.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        startV = 0;
        endV = 0;
        removeCallbacks(indeterminate);
        isRunning = false;
        updatePaint();
    }

    /** update ui */
    protected void updatePaint() {
        paint.setColor(color);
        innerPaint.setColor(innerColor);
        innerPaint.setAlpha(255);
        paint.setAlpha(255);
        innerPaint.setStrokeWidth(innerThickness);
        paint.setStrokeWidth(thickness);
    }

    /** go to the next state */
    protected void startNextState() {
        State currentState = state;
        State nextState = getNextState();
        if (currentState == nextState) return;
        changeState(currentState, nextState);
    }

    /** change state */
    protected void changeState(State currentState, State nextState) {
        stopAnimators();
        state = nextState;
        this.nextState = null;

        if ((currentState == State.HIDDEN && nextState == State.POWER) ||
                (currentState == State.HIDDEN && nextState == State.RELOADING)) {
            isRunning = true;
            show(true);

        } else if (currentState == State.HIDDEN && nextState == State.SUCCEED) {
            isRunning = true;
            innerState = InnerState.SUCCESS;
            show(true);

        } else if (nextState == State.RELOADING) {
            isRunning = true;
            startReloadAnimation(indeterminateDuration);

        } else if (currentState == State.POWER && nextState == State.LOADING) {
            isRunning = true;
            startLoadingNow();

        } else if (currentState == State.POWER && nextState == State.SUCCEED) {
            isRunning = true;
            hidePowerLineAnim();
            innerLine2 = null;
            nextInnerState = InnerState.SUCCESS;
            startLoading(succeedDuration);

        } else if (currentState == State.LOADING && nextState == State.LOADING) {
            postDelayed(indeterminate, delay);

        } else if (currentState == State.LOADING && nextState == State.POWER) {
            isRunning = true;
            backToShowingFromLoading();

        } else if (currentState == State.LOADING && nextState == State.SUCCEED) {
            isRunning = true;
            innerLine1 = null;
            innerLine2 = null;
            nextInnerState = InnerState.SUCCESS;
            startLoading(succeedDuration);

        } else if (currentState == State.SUCCEED && nextState == State.POWER) {
            isRunning = true;
            innerLine1 = null;
            innerLine2 = null;
            backToShowingFromSuccess();

        } else if (nextState == State.HIDDEN) {
            // TODO : support hidden animation
            setState(State.HIDDEN, false);

        } else {
            if (nextState == State.POWER || nextState == State.SUCCEED) {
                setState(nextState, false);
            } else {
                setState(State.POWER, false);
            }
        }

        if (currentState != nextState && listener != null)
            listener.onStateChanged(currentState, nextState, true);
    }

    /** last animation has been finished, call the listener and go to the next state */
    protected void animationFinished() {
        State currentState = state;
        State nextState = getNextState();
        changeState(currentState, nextState);

        if (listener != null)
            listener.onAnimationEnded(currentState, nextState);
    }

    /** find innerState by the state */
    protected @NonNull InnerState findInnerState(State state) {
        return (state == State.SUCCEED) ? InnerState.SUCCESS : InnerState.POWER;
    }

    /** find state by the innerState */
    protected @NonNull State findState(InnerState state) {
        return (state == InnerState.POWER) ? State.POWER : State.SUCCEED;
    }

    /** find next innerState */
    protected @NonNull InnerState getNextInnerState() {
        return nextInnerState != null ? nextInnerState : innerState;
    }

    /** innerView lines data */
    protected static class Line {
        float startX, startY, stopX, stopY;

        public Line() {
        }

        public Line(@NonNull Line line) {
            this.startY = line.startY;
            this.startX = line.startX;
            this.stopY = line.stopY;
            this.stopX = line.stopX;
        }
    }

    // INTERFACE

    public interface AnimatorListener {
        /** called when the last animation finished */
        void onAnimationEnded(State currentState, State nextState);
        /** called when the state changed */
        void onStateChanged(State from, State to,boolean animationLoaded);
    }

    // PUBLIC METHODS

    public void setAnimatorListener(@Nullable AnimatorListener listener) {
        this.listener = listener;
    }

    /**
     * set current AXPowerView's state
     *
     * @param state : next AXPowerView's state
     * @see AXPowerView#setState(State,boolean)
     */
    public void setState(@NonNull State state) {
        setState(state, true);
    }

    /**
     * set current AXPowerView's state
     *
     * @param state : next AXPowerView's state
     * @param animation : If you enable animation, new state will be queued and the
     *                  new animation will run after the end of the active animation.
     * @see AXPowerView.State
     */
    public void setState(@NonNull State state, boolean animation) {
        if (animation) {
            State currentState = this.state;
            State nextState = this.nextState;
            if (currentState == nextState) return;
            this.nextState = state;

            if (!isRunning) {
                startNextState();
            } else {
                if (listener != null)
                    listener.onStateChanged(currentState, state, false);
            }
        } else {
            if (state == State.POWER || state == State.SUCCEED) {
                stopAnimators();
                nextState = null;
                nextInnerState = null;
                innerState = findInnerState(state);
                this.state = state;
                show(false);
            } else if (state == State.HIDDEN) {
                stopAnimators();
                nextState = null;
                nextInnerState = null;
                innerState = InnerState.POWER;
                this.state = state;
                innerLine1 = null;
                innerLine2 = null;
                innerStart = 0;
                innerEnd = 0;
                start = 0;
                end = 0;
            } else if (state == State.LOADING) {
                setState(State.POWER, false);
                setState(State.LOADING, true);
            } else {
                setState(state, true);
            }
        }
    }

    /**
     * get the current AXPowerView's state
     * @return the current state.
     */
    public @NonNull State getCurrentState() {
        return state;
    }

    /**
     * get the next AXPowerView's state
     * @return the next state.
     */
    public @NonNull State getNextState() {
        return nextState != null ? nextState : state;
    }

    /**
     * set auto start enabled
     */
    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    /**
     * set color of the main circle
     */
    public void setColor(int color) {
        this.color = color;
        updatePaint();
    }

    /**
     * get color of the main circle
     * @return the main circle color
     */
    public int getColor() {
        return color;
    }

    /**
     * set color of the innerView
     */
    public void setInnerColor(int innerColor) {
        this.innerColor = innerColor;
        updatePaint();
    }

    /**
     * get color of the innerView
     * @return the innerView color
     */
    public int getInnerColor() {
        return innerColor;
    }

    /**
     * set thickness of the main circle
     */
    public void setThickness(float thickness) {
        this.thickness = thickness;
        updatePaint();
    }

    /**
     * get thickness of the main circle
     * @return the main circle thickness.
     */
    public float getThickness() {
        return thickness;
    }

    /**
     * set thickness of the innerView
     */
    public void setInnerThickness(float innerThickness) {
        this.innerThickness = innerThickness;
        updatePaint();
    }

    /**
     * get thickness of the innerView
     * @return the innerView circle thickness.
     */
    public float getInnerThickness() {
        return innerThickness;
    }

    /**
     * set showing animation duration
     */
    public void setShowDuration(long showDuration) {
        this.showDuration = showDuration;
    }

    /**
     * get showing animation duration
     * @return showing animation duration
     */
    public long getShowDuration() {
        return showDuration;
    }

    /**
     * set loading animation duration
     */
    public void setIndeterminateDuration(long indeterminateDuration) {
        this.indeterminateDuration = indeterminateDuration;
    }

    /**
     * get loading animation duration
     * @return loading animation duration
     */
    public long getIndeterminateDuration() {
        return indeterminateDuration;
    }

    /**
     * set succeed animation duration
     */
    public void setSucceedDuration(long succeedDuration) {
        this.succeedDuration = succeedDuration;
    }

    /**
     * get succeed animation duration
     * @return succeed animation duration
     */
    public long getSucceedDuration() {
        return succeedDuration;
    }

    /**
     * set loading animation delay
     */
    public void setDelay(long delay) {
        this.delay = delay;
    }

    /**
     * get loading animation delay
     * @return loading animation delay
     */
    public long getDelay() {
        return delay;
    }

    /**
     * check whether animation is running
     * @return true if animation is running
     */
    public boolean isAnimationRunning() {
        return isRunning;
    }

    /**
     * @return true if autoStart is enabled
     */
    public boolean isAutoStartEnabled() {
        return autoStart;
    }

    /**
     * check whether innerView is enabled
     * @return true if innerView is enabled
     */
    public boolean isInnerViewEnabled() {
        return innerView;
    }

    /**
     * set the innerView enabled
     */
    public void setInnerViewEnabled(boolean innerView) {
        this.innerView = innerView;
    }
}
