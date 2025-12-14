package ru.extreames.unleashedtiktok.xposed.features;

import android.app.AlertDialog;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class BlockCloud {
    public static void initialize(final XC_LoadPackage.LoadPackageParam lpParam) {
        XposedHelpers.findAndHookMethod(
                AlertDialog.Builder.class,
                "show",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        AlertDialog.Builder thiz = (AlertDialog.Builder) param.thisObject;
                        if (thiz == null)
                            return;
                        if (!isCloudCall())
                            return;

                        AlertDialog dialog = thiz.create();
                        dialog.dismiss(); // cancel this dialog!

                        param.setResult(dialog);
                    }
                }
        );
    }
    private static boolean isCloudCall() {
        Thread currentThread = Thread.currentThread();
        StackTraceElement[] stackTrace = currentThread.getStackTrace();

        for (StackTraceElement element : stackTrace) {
            String string = element.toString();
            if (string.startsWith("me.tigrik")) { // example: me.tigrik.f.a.a(Native Method)
                return true;
            }
        }

        return false;
    }
}
