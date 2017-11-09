package ma.wanam.youtubeadaway;

import android.content.SharedPreferences;

public class Params {
    private ClassLoader classLoader;
    private SharedPreferences xPrefs;

    public Params(ClassLoader classLoader, SharedPreferences xPrefs) {
        this.classLoader = classLoader;
        this.xPrefs = xPrefs;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public SharedPreferences getxPrefs() {
        return xPrefs;
    }

}
