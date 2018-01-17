package ma.wanam.youtubeadaway;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import ma.wanam.youtubeadaway.utils.Constants;

public class Xposed implements IXposedHookLoadPackage {
    private Context context = null;

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

                final Class<?> mView = XposedHelpers.findClass("android.view.View", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(mView, "setVisibility", int.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                        int visibility = (int) param.args[0];
                        View view = (View) param.thisObject;
                        if (visibility == View.VISIBLE) {
                            try {
                                String key = view.getResources().getResourceEntryName(view.getId());
                                if (isAd(key)) {
                                    debug("detected visible ad: " + key);
                                    param.args[0] = View.GONE;
                                } else {
                                    debug("ignored visible view: " + key);
                                }
                            } catch (Resources.NotFoundException ignored) {
                            }
                        }
                    }
                });

                final Class<?> mLayoutInflater = XposedHelpers.findClass("android.view.LayoutInflater", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(mLayoutInflater, "inflate", int.class, ViewGroup.class, boolean.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        hideInflatedAd(param);
                    }
                });

                XposedHelpers.findAndHookMethod(mLayoutInflater, "inflate", int.class, ViewGroup.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        hideInflatedAd(param);
                    }
                });

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

    private void hideInflatedAd(XC_MethodHook.MethodHookParam param) {
        View view = (View) param.getResult();
        try {
            String key = view.getResources().getResourceEntryName(view.getId());
            if (isAd(key)) {
                debug("detected inflated ad: " + key);
                XposedHelpers.callMethod(view, "setVisibility", View.GONE);
            } else {
                debug("ignored inflated view: " + key);
            }
        } catch (Resources.NotFoundException ignored) {
        }
    }

    private boolean isAd(String key) {
        return key.startsWith("ad_") || key.startsWith("ads_") || key.contains("promo") || key.contains("mdx")
                || key.contains("shopping") || key.contains("teaser") || key.contains("companion") || key.contains("invideo")
                || key.contains("_ad_") || key.contains("_ads_") || key.endsWith("_ad") || key.endsWith("_ads") || key.contains("gads");
    }

    private void debug(String msg) {
        if (BuildConfig.DEBUG) {
            XposedBridge.log(msg);
        }
    }

}
