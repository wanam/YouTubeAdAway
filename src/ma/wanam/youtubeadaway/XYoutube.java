package ma.wanam.youtubeadaway;

import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class XYoutube {

	private ClassLoader classLoader;
	private XSharedPreferences xPrefs;

	public XYoutube(ClassLoader classLoader, XSharedPreferences xPrefs) {
		this.classLoader = classLoader;
		this.xPrefs = xPrefs;
	}

	public void doHook(String version, String moduleVersion, String packageName) {

		try {
			if (VERSION.SDK_INT < VERSION_CODES.N) {
				//don't do that on N+ builds
				//cannot access zygote service from forked threads
				xPrefs.reload();
			}
			log("YouTube: " + packageName + " " + version + " loaded!");
			log("YouTube AdAway " + moduleVersion + ": Trying brute force way...");
			new BFAsync().execute(new Params(classLoader, xPrefs));
		} catch (Throwable t) {
			log(t);
		}
	}

	private void log(Throwable e) {
		if (xPrefs.getBoolean("enableLogs", true)) {
			XposedBridge.log(e);
		}
	}

	private void log(String e) {
		if (xPrefs.getBoolean("enableLogs", true)) {
			XposedBridge.log(e);
		}
	}

}
