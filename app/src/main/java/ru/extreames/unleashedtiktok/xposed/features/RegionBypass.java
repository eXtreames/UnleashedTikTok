package ru.extreames.unleashedtiktok.xposed.features;

import android.telephony.TelephonyManager;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import ru.extreames.unleashedtiktok.xposed.utils.Utils;

public class RegionBypass {
    private static final String REGION_COUNTRY_ISO = "BY";
    private static final String REGION_OPERATOR_NUM = "25704";
    private static final String REGION_OPERATOR_NAME = "life:)";

    public static void initialize(final XC_LoadPackage.LoadPackageParam lpParam) {
        Utils.retConst(TelephonyManager.class, "getSimOperatorName", REGION_OPERATOR_NAME);
        Utils.retConst(TelephonyManager.class, "getSimCountryIso", REGION_COUNTRY_ISO);
        Utils.retConst(TelephonyManager.class, "getSimOperator", REGION_OPERATOR_NUM);

        Utils.retConst(TelephonyManager.class, "getNetworkOperatorName", REGION_OPERATOR_NAME);
        Utils.retConst(TelephonyManager.class, "getNetworkCountryIso", REGION_COUNTRY_ISO);
        Utils.retConst(TelephonyManager.class, "getNetworkOperator", REGION_OPERATOR_NUM);

        Utils.retConst(TelephonyManager.class, "getDataNetworkType", TelephonyManager.NETWORK_TYPE_LTE);
        Utils.retConst(TelephonyManager.class, "getNetworkType", TelephonyManager.NETWORK_TYPE_LTE);
        Utils.retConst(TelephonyManager.class, "getSimState", TelephonyManager.SIM_STATE_READY);
    }
}
