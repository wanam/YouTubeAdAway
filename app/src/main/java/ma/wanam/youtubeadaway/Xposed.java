package ma.wanam.youtubeadaway;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import ma.wanam.youtubeadaway.utils.Constants;

public class Xposed implements IXposedHookLoadPackage {
    private static final String HIDE_MY_PARENT = "hide_my_parent";
    private static final String AD_BADGE = "ad_badge";
    private static Context context = null;
    private static Activity activity = null;
    private static String versionCode = "";
    private static String moduleVersionCode = "";

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam) throws Throwable {

        if (lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_PACKAGE)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_KIDS_PACKAGE)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_GAMING)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_MUSIC)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_MANGO)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_TV1_PACKAGE)
                || lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_TV2_PACKAGE)) {
            try {

                Class<?> instrumentation = XposedHelpers.findClass(
                        "android.app.Instrumentation", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(instrumentation, "newActivity",ClassLoader.class,String.class,Intent.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String clName = (String) param.args[1];
                        debug("!!!! Activity starting: " + clName);

                        final Class<?> clAct = XposedHelpers.findClass(clName, lpparam.classLoader);
                        final Class<?> clBase = clAct.getSuperclass();

                        XposedHelpers.findAndHookMethod(clBase, "onCreate", Bundle.class, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if(activity!=null) return; //already done
                                activity = (Activity) param.thisObject;
                                context = activity;
                                debug("!!!! Activity Started: " + activity.getClass().getName());

                                SharedPreferences prefs = activity.getSharedPreferences("ytaw", 0);
                                hookTimers(lpparam, prefs);
                            }
                        });
                    }
                });

                if (context == null) {
                    Object activityThread = XposedHelpers.callStaticMethod(
                            XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread");
                    context = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
                }

                versionCode = context.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionName;
                moduleVersionCode = context.getPackageManager().getPackageInfo(Constants.GOOGLE_YOUTUBE_XPOSED,0).versionName;
                //new BFAsync().execute(lpparam);

                hookViews(lpparam);
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

    private void hookTimers(final LoadPackageParam lpparam,SharedPreferences prefs) {
        String veridentExpected=versionCode+moduleVersionCode;
        String veridentStored=prefs.getString("verident","");
        String scls=prefs.getString("cls","");

        if(!veridentExpected.equals(veridentStored))
        {
            debug("!!!! Version mismatch, searching classes: "+veridentStored+" -> "+veridentExpected);

            ArrayList<String> cls=BFAsync.findHooks(lpparam);
            scls = TextUtils.join(",", cls);

            SharedPreferences.Editor pedit=prefs.edit();
            pedit.putString("verident",veridentExpected);
            pedit.putString("cls",scls);
            pedit.commit();

            debug("!!!! Saving found classes: "+scls);
        }

        ArrayList<String> cls=new ArrayList<>(Arrays.asList(scls.split(",")));
        BFAsync.hookClasses(lpparam.classLoader,cls);
    }

    private void hookViews(final LoadPackageParam lpparam) {
        final Class<?> mViewGroup = XposedHelpers.findClass("android.view.ViewGroup", lpparam.classLoader);
        XposedBridge.hookAllMethods(mViewGroup, "addView", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                checkAndHideAdViewCards(param, lpparam);
            }
        });
    }

    private void checkAndHideAdViewCards(XC_MethodHook.MethodHookParam param, LoadPackageParam lpparam) {
        try {
            View view = (View) param.args[0];
            if (view.getTag() != null && view.getTag().equals(HIDE_MY_PARENT)) {
                debug("hide ad badge grand parent");
                ViewGroup vg = (ViewGroup) param.thisObject;
                vg.setVisibility(View.GONE);
            } else if (view.getVisibility() == View.VISIBLE && view instanceof TextView) {
                int adBadge = view.getResources().getIdentifier(AD_BADGE, "string", lpparam.packageName);
                String adBadgeStr = view.getResources().getString(adBadge);
                if (((TextView) view).getText().equals(adBadgeStr)) {
                    debug("hide ad badge parent");
                    ViewGroup vg = (ViewGroup) param.thisObject;
                    vg.setVisibility(View.GONE);
                    vg.setTag(HIDE_MY_PARENT);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private void debug(String msg) {
        if (BuildConfig.DEBUG) {
            try {
                XposedBridge.log(msg);
            } catch (Exception e) {
                Log.v("youtube", "exception=" + e.toString());
                e.printStackTrace();
            }
        }
    }

}