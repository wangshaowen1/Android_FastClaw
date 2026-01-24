package com.kgmedia.fastfilm.feature.play;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import com.blankj.utilcode.util.ScreenUtils;
import com.blankj.utilcode.util.SizeUtils;
import com.kgmedia.fastfilm.R;
import com.kgmedia.fastfilm.databinding.PopupSetVoiceBinding;


public class PopupWindowSetVoice extends PopupWindow implements View.OnClickListener {
    private boolean mAllowDismissOnOutsideTouch = false; // 控制标志位

    public void setAllowDismissOnOutsideTouch(boolean allow) {
        mAllowDismissOnOutsideTouch = allow;
    }
    public View.OnClickListener onPopViewClickedListener;
    private PopupSetVoiceBinding popupVoiceTypeBinding;

    public PopupWindowSetVoice(Context context) {
        super(context);
        popupVoiceTypeBinding = PopupSetVoiceBinding.inflate(LayoutInflater.from(context));
        setContentView(popupVoiceTypeBinding.getRoot());
        setAnimationStyle(R.style.NoDimAnimTopGrow);
        setOutsideTouchable(false);
        setFocusable(false);
    }

    @Override
    public void dismiss() {
        if(mAllowDismissOnOutsideTouch){
            super.dismiss();
        }
    }

    public void show(View v) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        int offsetLeft = location[0];
        int offsetTop = location[1];
        int x = (int) (offsetLeft + v.getMeasuredWidth() / 2f - getMeasureWidth() / 2f);
        int y = getMeasureHeight() + offsetTop + offsetY();

        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        super.showAtLocation(v, Gravity.LEFT | Gravity.TOP, x, y);
    }

    private int getMeasureHeight() {
        return SizeUtils.dp2px(40);
    }

    private int getMeasureWidth() {
        return SizeUtils.dp2px(164);
    }

    private int offsetY() {
        return 0;
    }

    @Override
    public void onClick(View v) {
        if (onPopViewClickedListener != null)
            onPopViewClickedListener.onClick(v);
    }

    public void setOnPopViewClickedListener(View.OnClickListener onPopViewClickedListener) {
        this.onPopViewClickedListener = onPopViewClickedListener;
    }

}
