/*
 * MIT License
 *
 * Copyright (c) 2019 SheTieJun
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.shetj.loading

import android.app.Dialog
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.LongDef
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 *   must  【 android:configChanges="orientation|keyboardHidden|screenSize"】
 */
open class LoadingDialog : LifecycleEventObserver {

    companion object {
        const val LOADING_LONG = 1800L

        const val LOADING_SHORT = 800L


        /**
         * 和协程一起使用
         */
        inline fun showWithAction(
            context: AppCompatActivity,
            crossinline action: suspend () -> Unit
        ): LoadingDialog {
            return LoadingDialog().showWithAction(context, action)
        }

        /**
         * 和协程一起使用
         */
        inline fun showWithTimeoutAction(
            context: AppCompatActivity,
            time: Long = LOADING_LONG,
            crossinline action: suspend () -> Unit
        ): LoadingDialog {
            return LoadingDialog().showWithTimeOutAction(context, time, action)
        }


        @JvmStatic
        @JvmOverloads
        fun showTip(
            context: AppCompatActivity,
            msg: CharSequence = "加载中...",
            tip: Tip = Tip.INFO,
            @LoadingTipsDuration time: Long = LOADING_SHORT
        ): LoadingDialog {
            val image = when (tip) {
                Tip.SUCCESS -> R.drawable.icon_tip_success
                Tip.DEFAULT -> R.drawable.icon_tip_success
                Tip.WARNING -> R.drawable.icon_tip_warn
                Tip.INFO -> R.drawable.icon_tip_success
                Tip.ERROR -> R.drawable.icon_tip_error
            }
            return LoadingDialog().showTip(context, false, msg, image, time)
        }

    }


    private val handler = CoroutineExceptionHandler { _, throwable ->
        Log.e("CoroutineException", throwable.stackTraceToString())
    }

    val ktScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + handler)

    @LongDef(LOADING_LONG, LOADING_SHORT)
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class LoadingTipsDuration

    private var weakReference: WeakReference<AppCompatActivity>? = null
    private var mLoadingDialog: AlertDialog? = null

    protected open fun createLoading(
        context: Context,
        cancelable: Boolean = false,
        msg: CharSequence = "加载中...",
        @DrawableRes image: Int? = null
    ): AlertDialog {
        val view = LayoutInflater.from(context).inflate(R.layout.base_dialog_loading, null)
        return AlertDialog.Builder(context, R.style.trans_dialog).apply {
            val tvMsg = view.findViewById<TextView>(R.id.tv_msg)
            tvMsg.text = msg
            image?.let {
                tvMsg.setDrawables(it, Gravity.TOP)
                view.findViewById<ProgressBar>(R.id.progress).isVisible = false
            }
            setView(view)
            setCancelable(cancelable)
        }.create().apply {
            setCanceledOnTouchOutside(false)
        }
    }

    fun showLoading(
        context: AppCompatActivity,
        cancelable: Boolean = true,
        msg: CharSequence = "加载中",
        @DrawableRes image: Int? = null
    ): AlertDialog {
        if (context.isFinishing) {
            return mLoadingDialog!!
        }
        initDialog(context, cancelable, msg, image)
        mLoadingDialog?.let {
            if (!mLoadingDialog!!.isShowing) {
                mLoadingDialog!!.show()
            }
        }
        return mLoadingDialog!!
    }

    open fun hideLoading() {
        ktScope.cancel()
        if (null != mLoadingDialog && mLoadingDialog!!.isShowing) {
            mLoadingDialog!!.dismiss()
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Event) {
        when (event) {
            Event.ON_DESTROY -> {
                hideLoading()
            }
            else -> {}
        }
    }

    private fun initDialog(
        context: AppCompatActivity,
        cancelable: Boolean = true,
        msg: CharSequence = "加载中",
        @DrawableRes image: Int? = null
    ) {
        if (mLoadingDialog == null || context != weakReference?.get()) {
            weakReference = WeakReference(context)
            mLoadingDialog = createLoading(context, cancelable, msg, image).apply {
                initSetting()
            }
            context.lifecycle.addObserver(this)
        }
    }

    private fun Dialog.initSetting() {
        setOnDismissListener {
            clean()
        }
        setOnCancelListener {
            clean()
        }
    }

    private fun clean() {
        weakReference?.get()?.lifecycle?.removeObserver(this)
    }

    /**
     * 协程一起使用
     * 任务结束后自定退出
     */
    inline fun showWithAction(
        context: AppCompatActivity,
        crossinline action: suspend () -> Unit
    ): LoadingDialog {
        showLoading(context)
        ktScope.launch {
            action()
            hideLoading()
        }
        return this
    }


    /**
     * 协程一起使用
     * 1. 任务结束后自定退出
     * 2. 超时也自动结束
     */
    inline fun showWithTimeOutAction(
        context: AppCompatActivity,
        time: Long = LOADING_SHORT,
        crossinline action: suspend () -> Unit
    ): LoadingDialog {
        ktScope.launch {
            try {
                withTimeout(time) {
                    showLoading(context)
                    action.invoke()
                }
            } catch (e: TimeoutCancellationException) {
                hideLoading()
            } catch (e: Exception) {
                e.printStackTrace()
                hideLoading()
            }
        }
        return this
    }


    /**
     * 展示提示
     */
    protected open fun showTip(
        context: AppCompatActivity,
        cancelable: Boolean = true,
        msg: CharSequence = "加载中",
        @DrawableRes image: Int?,
        @LoadingTipsDuration time: Long = LOADING_SHORT
    ): LoadingDialog {
        showLoading(context, cancelable, msg, image)
        ktScope.launch {
            delay(time)
            hideLoading()
        }
        return this
    }
}

enum class Tip {
    DEFAULT, INFO, ERROR, SUCCESS, WARNING
}

fun TextView.setDrawables(
    @DrawableRes resId: Int,
    gravity: Int = Gravity.TOP
) {
    ContextCompat.getDrawable(context, resId)?.apply {
        setBounds(0, 0, minimumWidth, minimumHeight)
    }?.let {
        when (gravity) {
            Gravity.START -> setCompoundDrawablesRelative(it, null, null, null)
            Gravity.TOP -> setCompoundDrawablesRelative(null, it, null, null)
            Gravity.END -> setCompoundDrawablesRelative(null, null, it, null)
            Gravity.BOTTOM -> setCompoundDrawablesRelative(null, null, null, it)
        }
    }
}
