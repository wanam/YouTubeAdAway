package ma.wanam.youtubeadaway;

import ma.wanam.youtubeadaway.utils.Constants;
import android.content.Context;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class Xposed implements IXposedHookLoadPackage, IXposedHookZygoteInit {
	private Context context = null;
	private XSharedPreferences prefs;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {

		try {
			prefs = new XSharedPreferences(Constants.GOOGLE_YOUTUBE_XPOSED, MainActivity.class.getSimpleName());
		} catch (Throwable e) {
		}
	}

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
				XYoutube xy = new XYoutube(lpparam.classLoader, prefs);
				xy.doHook(versionCode, moduleVersionCode, lpparam.packageName);
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

}
