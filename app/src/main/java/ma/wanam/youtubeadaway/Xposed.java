package ma.wanam.youtubeadaway;

import android.content.Context;
import android.view.View;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import ma.wanam.youtubeadaway.utils.Constants;

public class Xposed implements IXposedHookLoadPackage {
    private static final String SKIP_AD = "skip_ad";
    private static Context context = null;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_PACKAGE)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_KIDS_PACKAGE)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_GAMING)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_MUSIC)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_MANGO)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_TV1_PACKAGE)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_TV2_PACKAGE)) {
            try {

                if (context == null) {
                    Object activityThread = XposedHelpers.callStaticMethod(
                            XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread");
                    context = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
                }

                String versionCode = context.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionName;
                String moduleVersionCode = context.getPackageManager().getPackageInfo(Constants.GOOGLE_YOUTUBE_XPOSED,
                        0).versionName;

                hookViews(lpparam);

                new BFAsync().execute(lpparam.classLoader);

                XposedBridge.log("YouTube: " + lpparam.packageName + " " + versionCode + " loaded with module version " + moduleVersionCode);
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }

        if (lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_XPOSED)) {
            try {
                XposedHelpers.findAndHookMethod(Constants.GOOGLE_YOUTUBE_XPOSED + ".XChecker", lpparam.classLoader,
                        "isEnabled", XC_MethodReplacement.returnConstant(Boolean.TRUE));
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }

    private void hookViews(LoadPackageParam lpparam) {

        final Class<?> mView = XposedHelpers.findClass("android.view.View", lpparam.classLoader);
        XposedHelpers.findAndHookMethod(mView, "setVisibility", int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                checkAndHideVisibleAd(param);
            }

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                performClick(param);
            }
        });

    }

    private void checkAndHideVisibleAd(XC_MethodHook.MethodHookParam param) {
        int visibility = (int) param.args[0];
        View view = (View) param.thisObject;

        try {
            String key = view.getResources().getResourceEntryName(view.getId());
            if (key.startsWith(SKIP_AD)) {
                debug(key + " visibility: visible");
                param.args[0] = View.VISIBLE;
            } else if (visibility != View.GONE && isAd(key)) {
                debug("visible ad view: " + key);
                param.args[0] = View.GONE;
            }

        } catch (Throwable ignored) {
        }
    }

    private void performClick(XC_MethodHook.MethodHookParam param) throws Exception {

        View view = (View) param.thisObject;
        try {
            String key = view.getResources().getResourceEntryName(view.getId());
            if (key.startsWith(SKIP_AD)) {
                debug("perform click: " + key);
                view.bringToFront();
                view.setEnabled(true);
                view.setClickable(true);
                view.performClick();
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean isAd(String key) {
        return key.equals("ad") || key.equals("ads") || key.startsWith("ad_") || key.startsWith("ads_")
                || key.contains("_cta") || key.contains("shopping") || key.contains("teaser")
                || key.contains("companion") || key.contains("_ad_") || key.contains("_ads_")
                || key.contains("promo") || key.endsWith("_ad") || key.endsWith("_ads");
    }

    private void debug(String msg) {
        if (BuildConfig.DEBUG) {
            XposedBridge.log(msg);
        }
    }

}
