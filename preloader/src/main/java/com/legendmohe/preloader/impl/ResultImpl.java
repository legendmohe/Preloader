package com.legendmohe.preloader.impl;

import android.os.Handler;

import com.legendmohe.preloader.PreloadException;
import com.legendmohe.preloader.Preloader;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

class ResultImpl<T> implements Preloader.Result<T> {

    private CountDownLatch mLatch = new CountDownLatch(1);

    private T mResult;

    /*
    判断result是否设置过。由于可以设置result为null，所以要另外设置标记来表示result的状态
     */
    private boolean mResultHasSet;

    private MultiResultListener<T> mResultListener;

    private GetResultListener<T> mGetResultListener;

    private PreloadException mException;

    private Handler mListenerHandler;

    ResultImpl(Handler handler, long getResultTimeout) {
        mListenerHandler = handler;
        mResultListener = new MultiResultListener<>(
                handler,
                getResultTimeout
        );
    }

    @Override
    public synchronized void set(T result) {
        // 可能已经设置了exception或者result
        if (!mResultHasSet && mException == null) {
            mResult = result;
            // 这句放在mResult赋值语句下面
            mResultHasSet = true;
            mResultListener.onSetResult(result);
            mLatch.countDown();

            invokeResultCallback(mResultListener, mResult, null);
        }
    }

    @Override
    public synchronized void error(int code, Exception ex) {
        if (code == 0) {
            code = Preloader.ERROR_CODE_EXCEPTION;
        }
        setException(new PreloadException(
                code,
                "user set error code=" + code + " ex=" + ex,
                ex
        ));
    }

    @Override
    public T get() throws PreloadException {
        // 不要传Long.MAX_VALUE，否则可能会溢出
        return get(Integer.MAX_VALUE);
    }

    @Override
    public T get(long timeout) throws PreloadException {
        try {
            if (!mLatch.await(timeout, TimeUnit.MILLISECONDS)) {
                PreloadException preloadException = new PreloadException(
                        Preloader.ERROR_CODE_TIMEOUT,
                        "get result timeout"
                );
                throw preloadException;
            }
        } catch (InterruptedException e) {
            // 可能是因为cancel引起的InterruptedException
            if (mException == null) {
                PreloadException preloadException = new PreloadException(
                        Preloader.ERROR_CODE_INTERRUPT,
                        "task thread interrupted"
                );
                throw preloadException;
            }
        }
        if (mException != null) {
            throw mException;
        }
        if (mResultHasSet) {
            notifyGetInvokeWithResult();
        }
        return mResult;
    }

    @Override
    public synchronized void get(Preloader.ResultListener<T> resultListener) {
        if (mResultHasSet || mException != null) {
            invokeResultCallback(resultListener, mResult, mException);
        } else {
            mResultListener.addResultListener(resultListener);
        }
    }

    synchronized void setException(PreloadException ex) {
        // 可能外围已经设置了result
        if (!mResultHasSet) {
            mResult = null;
            mException = ex;
            mResultListener.onSetException(ex);
            mLatch.countDown();

            invokeResultCallback(mResultListener, mResult, mException);
        }
    }

    @Override
    public boolean hasSet() {
        return mResultHasSet;
    }

    private void invokeResultCallback(final Preloader.ResultListener<T> listener, final T result, final PreloadException ex) {
        if (mListenerHandler != null) {
            mListenerHandler.post(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) {
                                listener.onResult(result, ex);
                            }
                        }
                    }
            );
        } else {
            if (listener != null) {
                listener.onResult(result, ex);
            }
        }
        if (mResultHasSet) {
            notifyGetInvokeWithResult();
        }
    }

    private void notifyGetInvokeWithResult() {
        if (mGetResultListener != null) {
            mGetResultListener.onGetResult(this);
        }
    }

    public void setGetResultListener(GetResultListener<T> getResultListener) {
        mGetResultListener = getResultListener;
    }

    //////////////////////////////////////////////////////////////////////


    /**
     * 用于auto clear
     *
     * @param <T>
     */
    interface GetResultListener<T> {

        void onGetResult(Preloader.Result<T> result);
    }
}
