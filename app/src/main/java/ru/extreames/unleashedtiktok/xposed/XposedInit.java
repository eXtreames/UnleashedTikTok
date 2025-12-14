package ru.extreames.unleashedtiktok.xposed;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import ru.extreames.unleashedtiktok.xposed.features.BlockCloud;
import ru.extreames.unleashedtiktok.xposed.features.DownloadVideo;
import ru.extreames.unleashedtiktok.xposed.features.RegionBypass;
import ru.extreames.unleashedtiktok.xposed.utils.Utils;

public class XposedInit implements IXposedHookLoadPackage {
    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpParam) {
        if (!lpParam.packageName.equals("com.zhiliaoapp.musically") && !lpParam.packageName.equals("com.ss.android.ugc.trill"))
            return;

        try {
            DownloadVideo.initialize(lpParam);
            RegionBypass.initialize(lpParam);
            BlockCloud.initialize(lpParam);
            Utils.log(Utils.DEBUG_LEVEL.INFO, "Successfully initialized");
        }
        catch (Exception e) {
            Utils.log(Utils.DEBUG_LEVEL.ERROR, "Error while initializng - " + e);
        }
    }
}