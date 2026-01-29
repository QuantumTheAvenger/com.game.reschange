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
        // Sistem uygulamalarını ve modülün kendisini geç
        if (lpparam.packageName.equals("android") || 
            lpparam.packageName.equals("com.android.systemui") ||
            lpparam.packageName.equals("com.game.reschange")) return;

        // 🛡️ AYARLARI OKUMA: Dosyayı hem CE hem DE alanında arar
        final XSharedPreferences prefs = new XSharedPreferences("com.game.reschange", "scale_prefs");
        
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
                        // Dosyayı her seferinde yeniden oku
                        prefs.reload();
                        float scale = prefs.getFloat(lpparam.packageName, 1.0f);

                        if (scale < 1.0f && scale > 0.1f) {
                            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) param.args[1];
                            if (lp.width > 0) lp.width = (int) (lp.width * scale);
                            if (lp.height > 0) lp.height = (int) (lp.height * scale);
                            
                            // Log basıyoruz ki LSPosed Logunda çalıştığını görelim
                            XposedBridge.log("GameResChange Uygulandı: " + lpparam.packageName + " Scale: " + scale);
                        }
                    }
                }
        );
    }
    }
        
