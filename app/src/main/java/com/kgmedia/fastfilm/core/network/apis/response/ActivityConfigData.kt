package com.kgmedia.fastfilm.core.network.apis.response

/**
 *  {
 *         "popupList": [
 *             {
 *                 "id": "窗口ID",
 *                 "popupLocationCode": "弹窗位置编号（1-大厅主页、2-个人主页、3-我的作品、）",
 *                 "businessType": "业务类型（1-课程）",
 *                 "showFrequency": "展示频次（1-首次启动弹出、2-每天弹出、3-常驻展示）",
 *                 "maxShowCountPerDay": "每日最大展示次数（show_frequency=2时有效）",
 *                 "jumpType": "跳转类型（1-站内H5、2-站外H5）",
 *                 "jumpUri": "跳转URL",
 *                 "popupStatus": "弹窗状态（0-禁用、1-启用）"
 *             }
 *         ],
 *         "floatWindowList": [
 *             {
 *                 "id": "浮窗ID",
 *                 "floatLocationCode": "浮窗位置编号（1-大厅主页、2-个人主页、3-我的作品、）",
 *                 "businessType": "业务类型（1-课程）",
 *                 "showFrequency": "展示频次（1-首次启动展示、2-每天展示、3-常驻展示）",
 *                 "maxShowCountPerDay": "每日最大展示次数（show_frequency=2时有效）",
 *                 "jumpType": "跳转类型（1-站内H5、2-站外H5）",
 *                 "jumpUrl": "跳转URL",
 *                 "floatWindowStatus": "弹窗状态（0-禁用、1-启用）"
 *             }
 *         ]
 *     }
 */
data class ActivityConfigData(
    val floatWindowList: List<FloatWindowInfo>? = null,
    val popupList: List<PopupInfo>? = null
)

data class FloatWindowInfo(
    val businessType: String? = null,
    val floatLocationCode: String? = null,
    val floatWindowStatus: String? = null,
    val id: String? = null,
    val jumpType: String? = null,
    val jumpUrl: String? = null,
    val maxShowCountPerDay: String? = null,
    val showFrequency: String? = null
)

data class PopupInfo(
    val businessType: String? = null,
    val id: String? = null,
    val jumpType: String? = null,
    val jumpUri: String? = null,
    val maxShowCountPerDay: String? = null,
    val popupLocationCode: String? = null,
    val popupStatus: String? = null,
    val showFrequency: String? = null
)