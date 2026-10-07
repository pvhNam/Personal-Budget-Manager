package vn.sotay.thuchi;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.DecelerateInterpolator;
import java.util.*;
import java.util.function.Consumer;

/** Exact proportional segments with small rounded gaps and matching, clickable legend rows. */
public final class DonutChart extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path segment=new Path();
    private final RectF outer=new RectF(),inner=new RectF();
    private final float density;
    private List<SummaryData.Category> categories=Collections.emptyList();
    private long total;
    private float reveal=1,downX,downY;
    private int selected=-1,pressed=-1;
    private Consumer<SummaryData.Category> onCategory;
    private ValueAnimator animation;
    public DonutChart(Context context,AttributeSet attrs){
        super(context,attrs);density=getResources().getDisplayMetrics().density;setClickable(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void setData(SummaryData data,Consumer<SummaryData.Category> listener){
        categories=data.categories;total=data.total;onCategory=listener;selected=-1;
        if(animation!=null)animation.cancel();reveal=1;
        if(Motion.enabled()&&total>0){
            animation=ValueAnimator.ofFloat(0,1);animation.setDuration(550);
            animation.setInterpolator(new DecelerateInterpolator());
            animation.addUpdateListener(value->{reveal=(float)value.getAnimatedValue();invalidate();});
            animation.start();
        }
        invalidate();
    }
    private float radius(){return Math.min(getWidth(),getHeight())*.5f-12*density;}
    int categoryAt(float x,float y){
        if(total==0)return -1;
        float dx=x-getWidth()*.5f,dy=y-getHeight()*.5f;
        double distance=Math.hypot(dx,dy);float radius=radius();
        if(distance<radius-31*density||distance>radius+8*density)return -1;
        double angle=(Math.toDegrees(Math.atan2(dy,dx))+450)%360,cursor=0;
        for(int i=0;i<categories.size();i++){
            cursor+=categories.get(i).amount*360.0/total;if(angle<cursor)return i;
        }
        return categories.size()-1;
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float cx=getWidth()*.5f,cy=getHeight()*.5f,r=radius();
        paint.setPathEffect(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(19*density);
        paint.setColor(0xff25314a);canvas.drawCircle(cx,cy,r-9.5f*density,paint);
        if(total==0)return;
        paint.setStyle(Paint.Style.FILL);paint.setPathEffect(new CornerPathEffect(3*density));
        float angle=-90;
        for(int i=0;i<categories.size();i++){
            SummaryData.Category category=categories.get(i);
            float sweep=(float)(category.amount*360.0/total);
            float visible=Math.max(0,Math.min(sweep,360*reveal-(angle+90)));
            float gap=categories.size()==1?0:Math.min(3.5f,sweep*.22f);
            if(visible>gap){
                float offset=i==selected?3*density:0;
                outer.set(cx-r-offset,cy-r-offset,cx+r+offset,cy+r+offset);
                float inside=r-19*density-(i==selected?2*density:0);
                inner.set(cx-inside,cy-inside,cx+inside,cy+inside);
                segment.reset();segment.arcTo(outer,angle+gap*.5f,visible-gap);
                segment.arcTo(inner,angle+visible-gap*.5f,-(visible-gap));segment.close();
                paint.setColor(category.color);canvas.drawPath(segment,paint);
            }
            angle+=sweep;
        }
        paint.setPathEffect(null);
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                pressed=categoryAt(event.getX(),event.getY());downX=event.getX();downY=event.getY();return pressed>=0;
            case MotionEvent.ACTION_MOVE:
                if(Math.hypot(event.getX()-downX,event.getY()-downY)>ViewConfiguration.get(getContext()).getScaledTouchSlop())pressed=-1;
                return true;
            case MotionEvent.ACTION_UP:
                if(pressed>=0&&pressed==categoryAt(event.getX(),event.getY())){
                    selected=pressed;invalidate();performClick();
                    if(onCategory!=null)onCategory.accept(categories.get(pressed));
                }
                pressed=-1;return true;
            case MotionEvent.ACTION_CANCEL:pressed=-1;return true;
            default:return super.onTouchEvent(event);
        }
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override protected void onDetachedFromWindow(){if(animation!=null)animation.cancel();super.onDetachedFromWindow();}
}
