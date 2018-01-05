package ma.wanam.youtubeadaway;

import android.content.SharedPreferences;

import de.robv.android.xposed.XposedBridge;

public class XYoutube {

    private ClassLoader classLoader;
    private SharedPreferences xPrefs;

    public XYoutube(ClassLoader classLoader, SharedPreferences xPrefs) {
        this.classLoader = classLoader;
        this.xPrefs = xPrefs;
    }

    public void doHook(String version, String moduleVersion, String packageName) {

        try {
            log("YouTube: " + packageName + " " + version + " loaded with module version " + moduleVersion);
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