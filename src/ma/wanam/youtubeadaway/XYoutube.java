package ma.wanam.youtubeadaway;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Parcelable;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class XYoutube {

	private ClassLoader classLoader;
	private XSharedPreferences xPrefs;

	public XYoutube(ClassLoader classLoader, XSharedPreferences xPrefs) {
		this.classLoader = classLoader;
		this.xPrefs = xPrefs;
	}

	public void doHook(String version, String moduleVersion, String packageName) {

		try {
			xPrefs.reload();
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

	private static class BFAsync extends AsyncTask<Params, Void, Boolean> {
		ClassLoader classLoader;
		XSharedPreferences xPrefs;
		boolean isAdWrapperHooked = false;
		boolean isDBHooked = false;

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

		@Override
		protected Boolean doInBackground(Params... params) {
			classLoader = params[0].getClassLoader();
			xPrefs = params[0].getxPrefs();
			boolean found = false;
			for (char a1 = 'z'; a1 >= 'a'; a1--) {
				for (char a2 = 'z'; a2 >= 'a'; a2--) {
					for (char a3 = 'z'; a3 >= 'a'; a3--) {
						found = findAndHookYouTubeAds(a1, a2, a3);
						if (found) {
							return true;
						}
					}
				}
			}

			return isAdWrapperHooked;
		}

		/**
		 * @param a1
		 * @param a2
		 * @param a3
		 * @return true if a hook was found
		 */
		private boolean findAndHookYouTubeAds(char a1, char a2, char a3) {
			Class<?> classObj;
			Class<?> paramObj;
			final String lCRegex = "[a-z]+";
			Pattern lCPatern = null;

			try {
				classObj = XposedHelpers.findClass(new StringBuffer().append(a1).append(a2).append(a3).toString(),
						classLoader);
			} catch (Throwable e1) {
				return false;
			}

			try {
				if (!isDBHooked
						&& XposedHelpers.findFirstFieldByExactType(classObj, String[].class).getName().equals("b")
						&& XposedHelpers.findMethodExact(classObj, "a").getReturnType().equals(List.class)) {
					isDBHooked = true;

					XposedBridge.hookAllConstructors(classObj, new XC_MethodHook() {

						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							Object dataProvider = XposedHelpers.getObjectField(param.thisObject, "a");
							if (dataProvider != null) {
								Object sQLiteDatabase = (SQLiteDatabase) XposedHelpers.callMethod(dataProvider,
										"getWritableDatabase");
								if (sQLiteDatabase != null) {
									XposedHelpers.callMethod(sQLiteDatabase, "delete",
											new Object[] { "ads", null, null });
								}
							}
						};
					});

					log("YouTube AdAway: Successfully hooked ads DB: " + classObj.getName());
				}
			} catch (Throwable e) {
			}

			try {
				if (!isAdWrapperHooked
						&& XposedHelpers.findFirstFieldByExactType(classObj, Parcelable.Creator.class).getName()
								.equals("CREATOR")
						&& XposedHelpers.findMethodExact(classObj, "A").getReturnType().equals(List.class)) {
					isAdWrapperHooked = true;
					try {
						XposedBridge.hookAllConstructors(classObj, new XC_MethodHook() {
							@Override
							protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
								if (param.args != null && param.args.length > 1 && (param.args[0] instanceof List)) {
									param.args[0] = Collections.EMPTY_LIST; // impressionUris
									param.args[1] = null; // adVideoId
								}
							}
						});

						log("YouTube AdAway: Successfully hooked " + classObj.getName() + " constructor!");
					} catch (Throwable e) {
						log("YouTube AdAway: Failed to hook " + classObj.getName() + " constructor!");
						log(e);
					}

					try {
						lCPatern = Pattern.compile(lCRegex);
						Method[] methods = classObj.getDeclaredMethods();
						for (Method m : methods) {
							if (m.getName().equals("b") && m.getReturnType().equals(boolean.class)
									&& m.getParameterTypes().length == 1) {
								paramObj = m.getParameterTypes()[0];

								if (lCPatern.matcher(paramObj.getName()).matches()) {

									Method mClass = XposedHelpers.findMethodExact(classObj, "a", paramObj);

									if (mClass.getReturnType().equals(boolean.class)) {
										try {
											XposedBridge.hookMethod(mClass,
													XC_MethodReplacement.returnConstant(Boolean.TRUE));
											XposedBridge.hookMethod(m,
													XC_MethodReplacement.returnConstant(Boolean.FALSE));

											XposedBridge.log("YouTube AdAway: Successfully hooked "
													+ classObj.getName() + " param=" + paramObj.getName());
											break;
										} catch (Throwable e) {
											XposedBridge.log("YouTube AdAway: Failed to hook " + classObj.getName()
													+ " param=" + paramObj.getName() + " error: " + e);
										}
									}
								}
							}
						}

					} catch (Throwable e) {
						log("YouTube AdAway: Failed to hook " + classObj.getName() + " methods!");
						log(e);
					}

				}

			} catch (Throwable e) {
			}
			return isAdWrapperHooked && isDBHooked;
		}

		@Override
		protected void onPostExecute(Boolean found) {

			if (!found) {
				log("YouTube AdAway: brute force failed! Class/Param sequence not found");
			}
		}

	}

}
