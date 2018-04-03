package com.legendmohe.preloader;

/**
 * 预加载接口
 */
public interface IPreloader {

    /**
     * 启动一个预加载任务
     *
     * @param task
     * @param <T>
     * @return 该任务的id
     */
    <T> int start(PreloadTask<T> task);

    /**
     * 取出该id的任务的结果
     *
     * @param preloadTaskId start返回的taskId
     * @param <T>
     * @return Result对象
     */
    <T> Preloader.Result<T> getResult(int preloadTaskId);

    /**
     * 取消该id的预加载任务。
     * 注意，取消后仍可凭taskId取出Result对象。
     *
     * @param preloadTaskId
     */
    void cancel(int preloadTaskId);

    /**
     * 取消所有预加载任务。
     * 注意，取消后仍可凭taskId取出Result对象。
     */
    void cancelAll();

    /**
     * 取消并清空Preloader里面的该id的预加载任务
     * 注意，取消后不可凭taskId取出Result对象。
     *
     * @param preloadTaskId
     */
    void clear(int preloadTaskId);

    /**
     * 取消并清空Preloader里面的所有预加载任务
     * 注意，取消后不可凭taskId取出Result对象。
     */
    void clearAll();
}
