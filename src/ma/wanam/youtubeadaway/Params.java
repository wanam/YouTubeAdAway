package ma.wanam.youtubeadaway;

import de.robv.android.xposed.XSharedPreferences;

public class Params {
	private ClassLoader classLoader;
	private XSharedPreferences xPrefs;

	public Params(ClassLoader classLoader, XSharedPreferences xPrefs) {
		this.classLoader = classLoader;
		this.xPrefs = xPrefs;
	}

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public XSharedPreferences getxPrefs() {
		return xPrefs;
	}

}
