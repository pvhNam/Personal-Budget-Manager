package vn.sotay.thuchi;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.StateListAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/** Small native animations that respect the phone's animation setting. */
final class Motion {
    static boolean enabled(){return ValueAnimator.areAnimatorsEnabled();}
    static void enter(View view,Runnable finished) {
        if(!enabled()){finished.run();return;}
        view.setAlpha(0);view.setTranslationY(12*view.getResources().getDisplayMetrics().density);
        view.animate().alpha(1).translationY(0).setDuration(240)
                .setInterpolator(new DecelerateInterpolator()).withEndAction(finished).start();
    }
    static void press(View view) {
        if(!enabled()){view.setStateListAnimator(null);return;}
        StateListAnimator states=new StateListAnimator();
        ObjectAnimator down=ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X,.96f),PropertyValuesHolder.ofFloat(View.SCALE_Y,.96f));
        down.setDuration(90);
        ObjectAnimator up=ObjectAnimator.ofPropertyValuesHolder(view,
                PropertyValuesHolder.ofFloat(View.SCALE_X,1),PropertyValuesHolder.ofFloat(View.SCALE_Y,1));
        up.setDuration(160);up.setInterpolator(new DecelerateInterpolator());
        states.addState(new int[]{android.R.attr.state_pressed,android.R.attr.state_enabled},down);
        states.addState(new int[]{},up);view.setStateListAnimator(states);
    }
}
