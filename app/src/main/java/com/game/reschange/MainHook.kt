package com.game.reschange

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam
import android.util.DisplayMetrics
import android.view.View

class MainHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: LoadPackageParam) {
        // Kendi uygulamamızı ve sistem arayüzünü ellemiyoruz
        if (lpparam.packageName == "com.game.reschange" || lpparam.packageName == "android") return

        // ViewRootImpl üzerinden ekranı manipüle ediyoruz (Surface Scaling)
        XposedHelpers.findAndHookMethod(
            "android.view.ViewRootImpl",
            lpparam.classLoader,
            "setView",
            View::class.java,
            android.view.WindowManager.LayoutParams::class.java,
            View::class.java,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    // Buraya ileride MainActivity'den gelen ölçeği bağlayacağız. 
                    // Test için şimdilik tüm uygulamaları %60 render yapar:
                    val scale = 0.6f 
                    
                    val lp = param.args[1] as android.view.WindowManager.LayoutParams
                    // Sadece oyunun/uygulamanın içini küçültür, sistemi bozmaz!
                    lp.width = (lp.width * scale).toInt()
                    lp.height = (lp.height * scale).toInt()
                }
            }
        )
    }
}
