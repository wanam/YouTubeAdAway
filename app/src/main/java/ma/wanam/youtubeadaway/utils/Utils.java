package ma.wanam.youtubeadaway.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.File;

import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Utils {

    public static String getPackageVersion(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            File apkPath = new File(lpparam.appInfo.sourceDir);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Class<?> pkgParserClass = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
                Object packageLite = XposedHelpers.callStaticMethod(pkgParserClass, "parsePackageLite", apkPath, 0);
                return String.valueOf(XposedHelpers.getIntField(packageLite, "versionCode"));
            } else {
                Class<?> parserCls = XposedHelpers.findClass("android.content.pm.PackageParser", lpparam.classLoader);
                Object pkg = XposedHelpers.callMethod(parserCls.newInstance(), "parsePackage", apkPath, 0);
                return  String.valueOf(XposedHelpers.getIntField(pkg, "mVersionCode"));
            }
        } catch (Throwable e) {
            return null;
        }
    }
}
