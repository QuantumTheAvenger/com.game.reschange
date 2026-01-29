package com.game.reschange;

import android.view.View;
import android.view.WindowManager;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XposedInit implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {
        // Sistem uygulamalarını ve modülün kendi arayüzünü geçiyoruz
        if (lpparam.packageName.equals("android") || 
            lpparam.packageName.equals("com.android.systemui") || 
            lpparam.packageName.equals("com.game.reschange")) {
            return;
        }

        // MainActivity'nin kaydettiği ayarları oku (XSharedPreferences Android 11'de stabildir)
        final XSharedPreferences prefs = new XSharedPreferences("com.game.reschange", "scale_prefs");
        prefs.makeWorldReadable();
        
        // Kayıtlı ölçeği al (varsayılan 1.0f yani %100)
        float scale = prefs.getFloat(lpparam.packageName, 1.0f);

        // Eğer ayar %100 ise veya geçersizse hiçbir şey yapma
        if (scale >= 1.0f || scale <= 0.1f) {
            return;
        }

        final float finalScale = scale;

        // Android 11 (API 30) için Çözünürlük Ölçekleme Motoru
        // Oyunun grafik dosyalarına dokunmaz, sadece sistemin çizim alanını küçültür.
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
                        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.args[1];

                        // Pencereyi fiziksel olarak küçültüyoruz (wm size kullanmadan!)
                        if (lp.width > 0) lp.width = (int) (lp.width * finalScale);
                        if (lp.height > 0) lp.height = (int) (lp.height * finalScale);
                    }
                }
        );
    }
}
