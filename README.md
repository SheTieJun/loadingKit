## LoadingDialog

1. 耗时任务完成后结束
```kotlin
 LoadingDialog.showWithAction(this){
                repeat(100){
                    delay(50)
                    //do anything
                }
}
```

2. 类似Toast的功能
```kotlin
LoadingDialog.showTip(this,"这是一个INFO信息",Tip.INFO)
```


3. 超时2秒后自动关闭
```kotlin
LoadingDialog.showWithTimeoutAction(this,2000){
    repeat(100){
        delay(50)
        //do anything
    }
}
```