# 简介

Preloader是一个用于异步加载数据的工具。它提供同步或者异步的获取结果回调。
适当地使用可让业务层获取数据的时间点提前。

# 使用例子

以简单activity跳转的例子为例，从MainActivity跳转到SubActivity。其中SubActivity的完整
显示需要发起一次请求，获取到结果后显示在中央的textView。

利用Preloader，我们可以在MainActivity发起跳转的时候开始请求，在SubActivity需要的时候将结果取出。
这样子可以把获取结果的时间点提前，起到加速显示SubActivity的作用。

代码如下所示：

* MainActivity：

        PreloadTask<String> task = new PreloadTask<String>() {

            @Override
            public void run(final Preloader.Result<String> result) throws Exception {
                FakeRestfulApi.request("hello world", new ResponseListener() {
                    @Override
                    public void onSuccess(String response) {
                        result.set(response);
                    }

                    @Override
                    public void onFail(int resCode) {
                        result.error(resCode, null);
                    }
                });
            }

            @Override
            public void onCancel() {
                Log.d(TAG, "onCancel() called");
                FakeRestfulApi.cancel();
            }
        };

        // 启动task，获取taskId。
        int taskId = Preloader.start(task);
        // 传递taskId，用于获取结果。
        Intent intent = new Intent(this, SubActivity.class);
        intent.putExtra("preload_task_id", taskId);
        startActivity(intent);

* SubActivity：

        mTextView = findViewById(R.id.textView);

        // get result
        int preloadTaskId = getIntent().getIntExtra("preload_task_id", -1);
        if (preloadTaskId > 0) {
            // 使用taskId取出Result对象
            Preloader.Result<String> preloadResult = Preloader.getResult(preloadTaskId);
            if (preloadResult != null) {
                // 同步获取结果
                try {
                    // 取出结果，设置超时时间，单位毫秒
                    String content = preloadResult.get(1000);
                    mTextView.setText(content);
                } catch (PreloadException e) {
                    mTextView.setText("e=" + e.getMessage());
                }
                // 异步获取结果
                preloadResult.get(new Preloader.ResultListener<String>() {
                    @Override
                    public void onResult(String result, PreloadException e) {
                        if (e == null) {
                            mTextView.setText(result);
                        } else {
                            mTextView.setText("e=" + e.getMessage());
                        }
                    }
                });
            }
            // 取消预加载任务，所有正在等待的get方法都会返回timeout exception
            // Preloader.cancel(preloadTaskId);
            // Preloader.cancelAll();
        }

# 注意事项

* 可通过PreloadTask.setGetResultTimeout设置异步获取结果的超时时间，默认为30秒。
* 如果PreloadTask.isClearWhenGetResult()==false，那么需要使用者自己调用Preloader.clear方法来释放预加载task，
  否则预加载task将在第一次成功获取结果后被释放（无法再次通过Preloader.getResult取结果）
* 对于同一个preloadResult对象，可多次调用get()，互不影响。