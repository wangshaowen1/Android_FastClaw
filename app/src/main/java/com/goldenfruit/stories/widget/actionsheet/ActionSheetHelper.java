package com.kgmedia.fastfilm.widget.actionsheet;


public class ActionSheetHelper {
    public static ActionSheetDialog createDialog(String[] items, OnItemClickListener onItemClickListener) {
        ActionSheetDialog.Builder builder = new ActionSheetDialog.Builder();
        builder.setOthers(items);
        builder.setCancelText("取消");
        builder.setOnItemClickListener(onItemClickListener);
        return builder.build();
    }
}
