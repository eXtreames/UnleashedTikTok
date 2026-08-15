package ru.extreames.unleashedtiktok.xposed.utils;

import android.util.Log;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;

public class Utils {
    public enum DEBUG_LEVEL {
        INFO,
        WARNING,
        ERROR
    }

    public static void log(DEBUG_LEVEL level, String text) {
        Log.i("UnleashedTikTok", "[ UnleashedTikTok ] [ " + level + " ] " + text);
    }

    public static void retConst(Class<?> clazz, String method, Object constant) {
        XposedHelpers.findAndHookMethod(
                clazz,
                method,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(constant);
                    }
                });
    }
}
