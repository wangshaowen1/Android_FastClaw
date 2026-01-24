package com.kgmedia.fastfilm.widget;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.PopupWindow;

import com.blankj.utilcode.util.SizeUtils;
import com.kgmedia.fastfilm.databinding.PopupVoiceTypeBinding;
import com.kgmedia.fastfilm.R;


public class PopupVoiceDialog extends PopupWindow implements View.OnClickListener {

    public View.OnClickListener onPopViewClickedListener;
    private PopupVoiceTypeBinding popupVoiceTypeBinding;

    public PopupVoiceDialog(Context context) {
        super(context);
        popupVoiceTypeBinding = PopupVoiceTypeBinding.inflate(LayoutInflater.from(context));
        popupVoiceTypeBinding.manVoice.setOnClickListener(this);
        popupVoiceTypeBinding.womanVoice.setOnClickListener(this);
        popupVoiceTypeBinding.randomVoice.setOnClickListener(this);
        setContentView(popupVoiceTypeBinding.getRoot());
        setAnimationStyle(R.style.NoDimAnimTopGrow);
        setDefault();
    }


    public void show(View v) {
        int[] location = new int[2];
        v.getLocationOnScreen(location);
        int offsetLeft = location[0];
        int offsetTop = location[1];
        int x = (int) (offsetLeft + v.getMeasuredWidth() / 2f - getMeasureWidth() / 2f);
        int y = offsetTop + offsetY();

        setOutsideTouchable(true);
        setBackgroundDrawable(new BitmapDrawable());
        super.showAtLocation(v, Gravity.LEFT | Gravity.TOP, x, y);
    }

    private int getMeasureHeight() {
        return SizeUtils.dp2px(122);
    }

    private int getMeasureWidth() {
        return SizeUtils.dp2px(122);
    }

    private int offsetY() {
        return SizeUtils.dp2px(24);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.womanVoice:
                popupVoiceTypeBinding.womanVoice.setBackgroundResource(R.drawable.bg_10_ffffff_br12);
                popupVoiceTypeBinding.manVoice.setBackground(null);
                popupVoiceTypeBinding.randomVoice.setBackground(null);
                break;
            case R.id.manVoice:
                popupVoiceTypeBinding.randomVoice.setBackground(null);
                popupVoiceTypeBinding.womanVoice.setBackground(null);
                popupVoiceTypeBinding.manVoice.setBackgroundResource(R.color.color_10_ffffff);
                break;

            case R.id.randomVoice:
                setDefault();
                break;
        }
        if (onPopViewClickedListener != null)
            onPopViewClickedListener.onClick(v);
        dismiss();
    }

    private void setDefault() {
        popupVoiceTypeBinding.manVoice.setBackground(null);
        popupVoiceTypeBinding.womanVoice.setBackground(null);
        popupVoiceTypeBinding.randomVoice.setBackgroundResource(R.drawable.bg_10_ffffff_tr12);
    }

    public void setOnPopViewClickedListener(View.OnClickListener onPopViewClickedListener) {
        this.onPopViewClickedListener = onPopViewClickedListener;
    }

}
