package ma.wanam.youtubeadaway;

import ma.wanam.youtubeadaway.utils.Constants;
import ma.wanam.youtubeadaway.utils.Utils;
import android.content.Context;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Xposed implements IXposedHookLoadPackage, IXposedHookZygoteInit, IXposedHookInitPackageResources {
	private static String MODULE_PATH = null;
	private static Context context = null;
	private static XSharedPreferences prefs;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		MODULE_PATH = startupParam.modulePath;

		try {
			prefs = new XSharedPreferences(Constants.GOOGLE_YOUTUBE_XPOSED, MainActivity.class.getSimpleName());
		} catch (Throwable e) {
			XposedBridge.log(e);
		}
	}

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {

		if (lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_PACKAGE)
				|| lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_KIDS_PACKAGE)
				|| lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_GAMING)
				|| lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_MUSIC)
				|| lpparam.packageName.equals(Constants.GOOGLE_YOUTUBE_MANGO) || Utils.isTvPackage(lpparam.packageName)) {
			try {
				if (context == null) {
					Object activityThread = XposedHelpers.callStaticMethod(
							XposedHelpers.findClass("android.app.ActivityThread", null), "currentActivityThread");
					context = (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
				}
				String versionCode = context.getPackageManager().getPackageInfo(lpparam.packageName, 0).versionName;
				String moduleVersionCode = context.getPackageManager().getPackageInfo(Constants.GOOGLE_YOUTUBE_XPOSED,
						0).versionName;

				XYoutube.doHook(lpparam.classLoader, versionCode, moduleVersionCode, lpparam.packageName, prefs);
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

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {

		if (resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_PACKAGE)
				|| resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_KIDS_PACKAGE)
				|| resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_GAMING)
				|| resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_MUSIC)
				|| resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_MANGO)
				|| Utils.isTvPackage(resparam.packageName)) {
			try {
				final android.content.res.XModuleResources moduleResources = android.content.res.XModuleResources
						.createInstance(MODULE_PATH, resparam.res);
				XYouTubeLayouts.doHook(resparam, moduleResources);
			} catch (Throwable e) {
				XposedBridge.log(e);
			}
		}

	}
}
