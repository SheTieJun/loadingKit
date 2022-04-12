package me.shetj.loadingkit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.coroutines.delay
import me.shetj.loading.LoadingDialog
import me.shetj.loading.Tip

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.test_loading).setOnClickListener {
            LoadingDialog.showWithAction(this){
                repeat(100){
                    delay(50)
                }
            }
        }

        findViewById<View>(R.id.test_loading_info).setOnClickListener {
            LoadingDialog.showTip(this,"这是一个INFO信息",Tip.INFO)
        }

        findViewById<View>(R.id.test_loading_timeout).setOnClickListener {
            LoadingDialog.showWithTimeoutAction(this,2000){
                repeat(100){
                    delay(50)
                }
            }
        }
    }
}