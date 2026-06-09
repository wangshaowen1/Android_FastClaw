package com.shiyuan.fastclaw.core.manager

/**
 * @desc：MsgActionManager
 * @author：CaoWei
 * @date：2026/6/9 16:47
 */
class MsgActionManager private constructor() {

    var mOnMsgActionListener: OnMsgActionListener? = null

    companion object {
        val instance: MsgActionManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            MsgActionManager()
        }
    }

    fun setListener(onMsgActionListener: OnMsgActionListener?) {
        mOnMsgActionListener = onMsgActionListener
    }

    fun getListener(): OnMsgActionListener? {
        return mOnMsgActionListener
    }

    interface OnMsgActionListener {
        fun onMsgAction(msgGson: String)
    }
}
