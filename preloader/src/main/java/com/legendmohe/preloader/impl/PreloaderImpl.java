package com.legendmohe.preloader.impl;

import android.os.Handler;
import android.os.Looper;

import com.legendmohe.preloader.IPreloader;
import com.legendmohe.preloader.PreloadException;
import com.legendmohe.preloader.PreloadTask;
import com.legendmohe.preloader.Preloader;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认预加载实现
 */
public class PreloaderImpl implements IPreloader {

    /**
     * 执行预加载任务的Executor
     */
    private ExecutorService mTaskExecutor = Executors.newFixedThreadPool(
            4,
            new ThreadFactory() {
                private volatile int index = 0;

                @Override
                public Thread newThread(Runnable runnable) {
                    return new Thread(runnable, "PreloaderImpl thread-poor-" + index++);
                }
            }
    );

    /**
     * 维护start了的任务列表
     */
    private List<TaskSession<?>> mTasks = new LinkedList<>();

    /**
     * 自增任务id
     */
    private static volatile AtomicInteger sIncreaseId = new AtomicInteger(0);

    //////////////////////////////////////////////////////////////////////

    @Override
    public synchronized <T> int start(final PreloadTask<T> task) {
        TaskSession<T> taskSession = new TaskSession<>(
                task
        );
        // 先设置taskId
        taskSession.taskId = createTaskId();
        // 然后加到队列中
        mTasks.add(taskSession);
        // 再submit
        taskSession.future = mTaskExecutor.submit(taskSession);

        return taskSession.taskId;
    }

    @Override
    public synchronized <T> Preloader.Result<T> getResult(int preloadTaskId) {
        for (TaskSession<?> task : mTasks) {
            if (task.taskId == preloadTaskId) {
                return (Preloader.Result<T>) task.getResult();
            }
        }
        return null;
    }

    @Override
    public synchronized void cancel(int preloadTaskId) {
        TaskSession<?> target = null;
        for (TaskSession<?> task : mTasks) {
            if (task.taskId == preloadTaskId) {
                target = task;
                break;
            }
        }
        if (target != null) {
            // 先设置cancel exception
            target.result.setException(
                    new PreloadException(
                            Preloader.ERROR_CODE_USER_CANCEL,
                            "user cancel this preload task"
                    )
            );
            // 再interrupt
            target.cancel();
        }
    }

    @Override
    public synchronized void cancelAll() {
        for (TaskSession<?> task : mTasks) {
            task.result.setException(
                    new PreloadException(
                            Preloader.ERROR_CODE_USER_CANCEL,
                            "user cancel this preload task"
                    )
            );
            task.cancel();
        }
    }

    @Override
    public synchronized void clear(int preloadTaskId) {
        // cancel this task
        cancel(preloadTaskId);
        // then remove it
        removeInnerTask(preloadTaskId);
    }

    @Override
    public synchronized void clearAll() {
        cancelAll();
        mTasks.clear();
    }

    //////////////////////////////////////////////////////////////////////

    private int createTaskId() {
        return sIncreaseId.incrementAndGet();
    }

    private void removeInnerTask(int preloadTaskId) {
        TaskSession<?> target = null;
        for (TaskSession<?> task : mTasks) {
            if (task.taskId == preloadTaskId) {
                target = task;
                break;
            }
        }
        if (target != null) {
            mTasks.remove(target);
        }
    }

    //////////////////////////////////////////////////////////////////////

    /**
     * 预加载任务的Runnable wrapper
     *
     * @param <T>
     */
    class TaskSession<T> implements Runnable {

        private ResultImpl<T> result;

        private PreloadTask<T> task;

        /**
         * 用于取消任务
         */
        private Future<?> future;

        private int taskId;

        TaskSession(PreloadTask<T> task) {
            this.task = task;

            if (this.task.getResultListenerHandler() == null) {
                this.result = new ResultImpl<>(
                        new Handler(Looper.getMainLooper()),
                        this.task.getGetResultTimeout()
                );
            } else {
                this.result = new ResultImpl<>(
                        this.task.getResultListenerHandler(),
                        this.task.getGetResultTimeout()
                );
            }

            this.result.setGetResultListener(new ResultImpl.GetResultListener<T>() {
                @Override
                public void onGetResult(Preloader.Result<T> result) {
                    handleUserGetResult(result);
                }
            });
        }

        @Override
        public void run() {
            if (task != null) {
                try {
                    // wrap一下，以防在PreloadTask里面调用get
                    task.run(new Preloader.Result<T>() {
                        @Override
                        public void set(T result) {
                            TaskSession.this.result.set(result);
                        }

                        @Override
                        public T get(long timeout) throws PreloadException {
                            throw new RuntimeException("don't get result in PreloadTask");
                        }

                        @Override
                        public void get(Preloader.ResultListener<T> resultListener) {
                            throw new RuntimeException("don't get result in PreloadTask");
                        }

                        @Override
                        public boolean hasSet() {
                            return TaskSession.this.result.hasSet();
                        }
                    });
                } catch (Exception ex) {
                    result.setException(
                            new PreloadException(Preloader.ERROR_CODE_EXCEPTION, "exception in task", ex)
                    );
                }
            }
        }

        private void handleUserGetResult(Preloader.Result<T> result) {
            // 如果设置了自动清理，则不再维护当前任务
            if (task.isClearWhenGetResult()) {
                removeInnerTask(this.taskId);
            }
        }

        private ResultImpl<T> getResult() {
            return result;
        }

        /**
         * 利用future的cancel取消当前任务（interrupt）
         */
        private void cancel() {
            this.task.onCancel();
            if (this.future != null) {
                this.future.cancel(true);
            }
        }
    }
}
