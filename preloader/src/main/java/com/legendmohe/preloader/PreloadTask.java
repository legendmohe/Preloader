package com.legendmohe.preloader;

import android.os.Handler;

/**
 * 表示一个预加载任务
 *
 * @param <T>
 */
public abstract class PreloadTask<T> {

    /**
     * 异步get result的时候，listener回调所在的handler
     */
    private Handler mResultListenerHandler;

    /**
     * 异步get result的时候，listener回调超时时间。
     * 默认为30秒
     * 超时返回PreloadException，其中errorCode=Preloader.ERROR_CODE_TIMEOUT
     */
    private long mGetResultTimeout = Preloader.GET_RESULT_LISTENER_TIMEOUT;

    /**
     * 表示当task设置了result后，当get（同步&异步）被调用后，是否把当前task从preloader中移除
     */
    private boolean mAutoClear = true;

    public void setResultListenerHandler(Handler resultListenerHandler) {
        mResultListenerHandler = resultListenerHandler;
    }

    public void setGetResultTimeout(long getResultTimeout) {
        mGetResultTimeout = getResultTimeout;
    }

    public Handler getResultListenerHandler() {
        return mResultListenerHandler;
    }

    public long getGetResultTimeout() {
        return mGetResultTimeout;
    }

    public boolean isClearWhenGetResult() {
        return mAutoClear;
    }

    public void setClearWhenGetResult(boolean autoClear) {
        mAutoClear = autoClear;
    }

    //////////////////////////////////////////////////////////////////////

    /**
     * 异步执行预加载。
     *
     * 上层业务逻辑执行完毕后，一定要设置result（result.set()）或者抛出异常。
     * 否则result.get操作会一直等待直到超时。
     *
     * @param result
     * @throws Exception
     */
    public abstract void run(Preloader.Result<T> result) throws Exception;

    /**
     * 当该task尚未设置result，被cancel了，回调这个方法。
     */
    public abstract void onCancel();
}
