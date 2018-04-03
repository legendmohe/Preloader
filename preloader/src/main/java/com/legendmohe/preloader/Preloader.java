package com.legendmohe.preloader;

import com.legendmohe.preloader.impl.PreloaderImpl;

/**
 * 预加载全局入口
 */
public class Preloader {

    public static final int ERROR_CODE_USER_CANCEL = 0;
    public static final int ERROR_CODE_TIMEOUT = 1;
    public static final int ERROR_CODE_INTERRUPT = 2;
    public static final int ERROR_CODE_EXCEPTION = 3;

    public static final long GET_RESULT_LISTENER_TIMEOUT = 30*1000;

    private IPreloader mPreloaderImpl = new PreloaderImpl();

    private static class LazyHolder {
        private static final Preloader INSTANCE = new Preloader();
    }

    private static Preloader getInstance() {
        return LazyHolder.INSTANCE;
    }

    private static IPreloader getPreloaderImpl() {
        return getInstance().mPreloaderImpl;
    }

    //////////////////////////////////////////////////////////////////////

    /**
     * 启动一个预加载任务
     *
     * @param task
     * @param <T>
     * @return 该任务的任务id
     */
    public static <T> int start(PreloadTask<T> task) {
        getInstance().checkPreloaderImpl();

        return getPreloaderImpl().start(task);
    }

    /**
     * 取出该id的任务的结果
     *
     * @param preloadTaskId start返回的taskId
     * @param <T>
     * @return Result对象
     */
    public static <T> Result<T> getResult(int preloadTaskId) {
        getInstance().checkPreloaderImpl();

        return getPreloaderImpl().getResult(preloadTaskId);
    }

    /**
     * 取消该id的预加载任务。
     * 注意，取消后仍可凭taskId取出Result对象。
     *
     * @param preloadTaskId
     */
    public static void cancel(int preloadTaskId) {
        getInstance().checkPreloaderImpl();

        getPreloaderImpl().cancel(preloadTaskId);
    }

    /**
     * 取消所有预加载任务。
     * 注意，取消后仍可凭taskId取出Result对象。
     */
    public static void cancelAll() {
        getInstance().checkPreloaderImpl();

        getPreloaderImpl().cancelAll();
    }

    /**
     * 取消并清空Preloader里面的该id的预加载任务
     * 注意，取消后不可凭taskId取出Result对象。
     *
     * @param preloadTaskId
     */
    public static void clear(int preloadTaskId) {
        getInstance().checkPreloaderImpl();

        getPreloaderImpl().clear(preloadTaskId);
    }

    /**
     * 取消并清空Preloader里面的所有预加载任务
     * 注意，取消后不可凭taskId取出Result对象。
     */
    public static void clearAll() {
        getInstance().checkPreloaderImpl();

        getPreloaderImpl().clearAll();
    }

    //////////////////////////////////////////////////////////////////////

    private void checkPreloaderImpl() {
        if (mPreloaderImpl == null) {
            throw new NullPointerException("preloaderImpl is null");
        }
    }

    /**
     * 设置Preload实现。
     * 默认实现为PreloaderImpl.class
     *
     * @param preloaderImpl
     */
    public static void setPreloadImpl(IPreloader preloaderImpl) {
        getInstance().mPreloaderImpl = preloaderImpl;
    }

    //////////////////////////////////////////////////////////////////////

    /**
     * 预加载结果。用于让预加载任务实现类设置结果，和让外围逻辑取出结果
     * @param <T>
     */
    public interface Result<T> {

        /**
         * 设置该次预加载的结果
         *
         * @param result
         */
        void set(T result);

        /**
         * 同步获取结果
         *
         * @param timeout 超时时间，单位毫秒。当timeout <= 0时，表示一直等待
         * @return
         * @throws PreloadException
         */
        T get(long timeout) throws PreloadException;

        /**
         * 异步获取结果
         *
         * @param resultListener
         */
        void get(ResultListener<T> resultListener);

        /**
         * 当前Result对象是否已经被设置了result
         * @return
         */
        boolean hasSet();
    }

    /**
     * 异步get result的时候，用于接收结果回调
     * @param <T>
     */
    public interface ResultListener<T> {
        /**
         * 结果回调。如果超时或被取消或预加载任务抛出异常，e参数不为null
         *
         * @param result
         * @param e 获取结果异常，详见Preloader的error code常量
         */
        void onResult(T result, PreloadException e);
    }
}
