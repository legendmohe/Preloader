package com.legendmohe.preloader.impl;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.legendmohe.preloader.PreloadException;
import com.legendmohe.preloader.Preloader;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * 实现多个异步get同时进行。维护超时状态及最终结果。
 *
 * @param <T>
 */
class MultiResultListener<T> implements Preloader.ResultListener<T> {

    private static final int MSG_GET_RESULT_TIMEOUT = 100;

    private List<Preloader.ResultListener<T>> mResultListeners = new ArrayList<>();

    private H<T> mHandler;

    private long mGetResultTimeout;

    MultiResultListener(Handler listenerHandler, long getResultTimeout) {
        mGetResultTimeout = getResultTimeout;
        mHandler = new H<>(listenerHandler.getLooper());
    }

    @Override
    public void onResult(T result, PreloadException e) {
        // 通知子listener，然后不再维护它
        ListIterator<Preloader.ResultListener<T>> iterator =
                mResultListeners.listIterator();
        while (iterator.hasNext()) {
            Preloader.ResultListener<T> listener = iterator.next();
            listener.onResult(result, e);
            iterator.remove();
        }
    }

    public void onSetResult(T result) {
        mHandler.removeMessages(MSG_GET_RESULT_TIMEOUT);
    }

    public void onSetException(PreloadException ex) {
        mHandler.removeMessages(MSG_GET_RESULT_TIMEOUT);
    }

    //////////////////////////////////////////////////////////////////////

    void addResultListener(Preloader.ResultListener<T> listener) {
        if (!mResultListeners.contains(listener)) {
            mResultListeners.add(listener);

            Message msg = Message.obtain();
            msg.what = MSG_GET_RESULT_TIMEOUT;
            msg.obj = listener;
            mHandler.sendMessageDelayed(msg, mGetResultTimeout);
        }
    }

    void removeResultListener(Preloader.ResultListener<T> listener) {
        mResultListeners.remove(listener);
    }

    //////////////////////////////////////////////////////////////////////

    private class H<T> extends Handler {

        H(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_GET_RESULT_TIMEOUT: {
                    if (msg.obj == null) {
                        return;
                    }
                    Preloader.ResultListener<T> listener = (Preloader.ResultListener<T>) msg.obj;
                    listener.onResult(
                            null,
                            new PreloadException(
                                    Preloader.ERROR_CODE_TIMEOUT,
                                    "get result timeout"
                            )
                    );
                    // 回调超时，然后remove掉
                    mResultListeners.remove(listener);
                }
            }
        }
    }
}
