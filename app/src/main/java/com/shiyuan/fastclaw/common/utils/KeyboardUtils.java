package com.shiyuan.fastclaw.common.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.blankj.utilcode.util.ScreenUtils;
import com.shiyuan.fastclaw.App;

public class KeyboardUtils {

    public enum ShownModel {
        Delay, AfterLayout, None
    }

    public static void showKeyboard(View view, ShownModel shownModel) {
        if (shownModel == ShownModel.None) {
            showKeyboard(view);
        } else if (shownModel == ShownModel.AfterLayout) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    showKeyboard(view);
                    view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            });
        } else if (shownModel == ShownModel.Delay) {
            App.getInstance().postDelay(() -> showKeyboard(view), 100);
        }
    }

    public static void showKeyboard(View view) {
        view.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) view.getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                view.requestFocus();
                imm.showSoftInput(view, 0);
            }
        }, 100);
    }

    public static void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static int getKeyboardHeight(Activity context) {
        Rect rect = new Rect();
        context.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int screenHeight = ScreenUtils.getAppScreenHeight();
        int keyboardHeight = screenHeight - rect.bottom; // 输入法的高度
        if (Math.abs(keyboardHeight) > screenHeight / 5) {
            return keyboardHeight;
        }
        return 0;
    }

    public static void hideKeyBoard(Window window) {
        InputMethodManager imm = (InputMethodManager) App.instance.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (isSoftInputShown(window)) {
                imm.toggleSoftInput(0, 0);
            }
        }
    }

    public static boolean isSoftInputShown(Window window) {
        View decorView = window.getDecorView();
        int screenHeight = decorView.getHeight();
        Rect rect = new Rect();
        decorView.getWindowVisibleDisplayFrame(rect);
        return screenHeight - rect.bottom - getNavigateBarHeight(window.getWindowManager()) >= 0;
    }

    /**
     * 判断当前输入法是否仍然可见。
     */
    public static boolean isImeVisible(View view) {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);
        return insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
    }

    public static int getNavigateBarHeight(WindowManager windowManager) {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int usableHeight = metrics.heightPixels;
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        int realHeight = metrics.heightPixels;
        if (realHeight > usableHeight) {
            return realHeight - usableHeight;
        } else {
            return 0;
        }
    }
}
