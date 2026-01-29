package com.game.reschange;

import android.view.View;
import android.view.WindowManager;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedInit implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // Sistem uygulamalarını geç
        if (lpparam.packageName.equals("android") || lpparam.packageName.equals("com.android.systemui")) return;

        // AYARLARI OKUMA (ANDROID 11 FIX)
        final XSharedPreferences prefs = new XSharedPreferences("com.game.reschange", "scale_prefs");
        prefs.makeWorldReadable(); // Bu satır önemli
        
        // Kancayı atıyoruz
        XposedHelpers.findAndHookMethod(
                "android.view.ViewRootImpl",
                lpparam.classLoader,
                "setView",
                View.class,
                WindowManager.LayoutParams.class,
                View.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        // Her seferinde güncel ayarı çekmek için reload yapıyoruz
                        prefs.reload();
                        float scale = prefs.getFloat(lpparam.packageName, 1.0f);

                        if (scale < 1.0f && scale > 0.1f) {
                            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.args[1];
                            if (lp.width > 0) lp.width = (int) (lp.width * scale);
                            if (lp.height > 0) lp.height = (int) (lp.height * scale);
                            XposedBridge.log("GameResChange: " + lpparam.packageName + " için ölçek uygulandı: " + scale);
                        }
                    }
                }
        );
    }
}
