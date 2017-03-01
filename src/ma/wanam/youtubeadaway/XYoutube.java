package ma.wanam.youtubeadaway;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

	private static ClassLoader classLoader;
	private static XSharedPreferences xPrefs;
	private static boolean isAdResponseHooked = false;
	private static boolean isAdWrapperHooked = false;
	private static boolean isDBHooked = false;

	public static void doHook(ClassLoader classLoader, String version, String moduleVersion, String packageName,
			XSharedPreferences prefs) {

		XYoutube.classLoader = classLoader;
		XYoutube.xPrefs = prefs;

		try {
			XYoutube.xPrefs.reload();
			logError("YouTube: " + packageName + " " + version + " loaded!");
			Class<?> mVastAd = XposedHelpers.findClass(
					"com.google.android.libraries.youtube.innertube.model.ads.VastAd", classLoader);
			Class<?> mClock = XposedHelpers.findClass("com.google.android.libraries.youtube.common.util.Clock",
					classLoader);

			XposedHelpers
					.findAndHookMethod(mVastAd, "shouldPlayAd", mClock, XC_MethodReplacement.returnConstant(false));

			XposedHelpers.findAndHookMethod(mVastAd, "hasExpired", mClock, XC_MethodReplacement.returnConstant(true));

			logError("YouTube AdAway: Non proguarded successful hook!");
		} catch (Throwable e) {
			try {
				logError("YouTube AdAway " + moduleVersion + ": Trying brute force way...");
				isAdResponseHooked = false;
				isAdWrapperHooked = false;
				isDBHooked = false;
				new BFAsync().execute();
			} catch (Throwable t) {
				logError(t);
			}
		}

	}

	private static void disableYouTubeAds(ClassLoader classLoader) {

		Class<?> vastAd = null, vmapAdBreak = null;

		try {

			vastAd = XposedHelpers.findClass("com.google.android.apps.youtube.datalib.ads.model.VastAd", classLoader);
			XposedBridge.hookAllMethods(vastAd, "shouldPlayAd", XC_MethodReplacement.returnConstant(Boolean.FALSE));

			XposedBridge.hookAllMethods(vastAd, "hasExpired", XC_MethodReplacement.returnConstant(Boolean.TRUE));

		} catch (Throwable t) {
			logError(t);
			try {

				vastAd = XposedHelpers.findClass("com.google.android.apps.youtube.datalib.legacy.model.VastAd",
						classLoader);
				XposedBridge.hookAllMethods(vastAd, "shouldPlayAd", XC_MethodReplacement.returnConstant(Boolean.FALSE));

				XposedBridge.hookAllMethods(vastAd, "hasExpired", XC_MethodReplacement.returnConstant(Boolean.TRUE));

			} catch (Throwable e1) {
				logError(e1);
				try {
					vastAd = XposedHelpers.findClass("com.google.android.apps.youtube.core.model.VastAd", classLoader);

					XposedBridge.hookAllMethods(vastAd, "shouldPlayAd",
							XC_MethodReplacement.returnConstant(Boolean.FALSE));

					XposedBridge
							.hookAllMethods(vastAd, "hasExpired", XC_MethodReplacement.returnConstant(Boolean.TRUE));
				} catch (Throwable e2) {
					logError(e2);
				}
			}
		}

		try {

			vmapAdBreak = XposedHelpers.findClass("com.google.android.apps.youtube.datalib.ads.model.VmapAdBreak",
					classLoader);
			XposedHelpers.findAndHookMethod(vmapAdBreak, "isDisplayAdAllowed",
					XC_MethodReplacement.returnConstant(Boolean.FALSE));

			XposedHelpers.findAndHookMethod(vmapAdBreak, "getAds", new XC_MethodReplacement() {

				@Override
				protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
					return Collections.EMPTY_LIST;
				}
			});

		} catch (Throwable e1) {
			logError(e1);
			try {
				vmapAdBreak = XposedHelpers.findClass(
						"com.google.android.apps.youtube.datalib.legacy.model.VmapAdBreak", classLoader);
				XposedHelpers.findAndHookMethod(vmapAdBreak, "isDisplayAdAllowed",
						XC_MethodReplacement.returnConstant(Boolean.FALSE));

				XposedHelpers.findAndHookMethod(vmapAdBreak, "getAds", new XC_MethodReplacement() {

					@Override
					protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
						return Collections.EMPTY_LIST;
					}
				});

			} catch (Throwable e2) {
				logError(e2);
				try {
					vmapAdBreak = XposedHelpers.findClass("com.google.android.apps.youtube.core.model.VmapAdBreak",
							classLoader);
					XposedBridge.hookAllConstructors(vmapAdBreak, new XC_MethodHook() {
						@Override
						protected void afterHookedMethod(MethodHookParam param) throws Throwable {
							XposedHelpers.setBooleanField(param.thisObject, "isDisplayAdAllowed", Boolean.FALSE);
						}
					});

					XposedHelpers.findAndHookMethod(vmapAdBreak, "getAds", new XC_MethodReplacement() {

						@Override
						protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
							return Collections.EMPTY_LIST;
						}
					});

				} catch (Throwable e3) {
					logError(e3);
				}
			}

		}

	}

	private static void logError(Throwable e) {
		if (XYoutube.xPrefs.getBoolean("enableLogs", true)) {
			XposedBridge.log(e);
		}
	}

	private static void logError(String e) {
		if (XYoutube.xPrefs.getBoolean("enableLogs", true)) {
			XposedBridge.log(e);
		}
	}

	private static class BFAsync extends AsyncTask<Void, Void, Boolean> {

		@Override
		protected Boolean doInBackground(Void... params) {

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

			XYoutube.xPrefs.reload();
			try {
				classObj = XposedHelpers.findClass(new StringBuffer().append(a1).append(a2).append(a3).toString(),
						XYoutube.classLoader);
			} catch (Throwable e1) {
				return false;
			}

			try {
				if (!isAdResponseHooked
						&& XposedHelpers.findFirstFieldByExactType(classObj, Pattern.class).getName().equals("a")
						&& XposedHelpers.findFirstFieldByExactType(classObj, int.class).getName().equals("d")) {

					Method f = XposedHelpers.findMethodExact(classObj, "f");
					if (f.getReturnType().equals(boolean.class)) {
						XposedBridge.hookMethod(f, XC_MethodReplacement.returnConstant(Boolean.TRUE));
						isAdResponseHooked = true;
						logError("YouTube AdAway: Successfully hooked ad response: " + classObj.getName());
					}
				}
			} catch (Throwable e) {
			}

			try {
				if (!isDBHooked
						&& XposedHelpers.findFirstFieldByExactType(classObj, String[].class).getName().equals("b")
						&& XposedHelpers.findMethodExact(classObj, "a").getReturnType().equals(List.class)) {
					isDBHooked = true;

					Method a = XposedHelpers.findMethodExact(classObj, "a", String.class);
					if (a.getReturnType().equals(Set.class)) {
						XposedBridge.hookMethod(a, XC_MethodReplacement.returnConstant(Collections.emptySet()));
					}

					Method aNoParams = XposedHelpers.findMethodExact(classObj, "a");
					if (aNoParams.getReturnType().equals(List.class)) {
						XposedBridge.hookMethod(a, XC_MethodReplacement.returnConstant(Collections.emptySet()));
					}

					Method b = XposedHelpers.findMethodExact(classObj, "b", String.class);
					if (b.getReturnType().equals(int.class)) {
						XposedBridge.hookMethod(a, XC_MethodReplacement.returnConstant(0));
					}

					Method a2P = XposedHelpers.findMethodExact(classObj, "a", String.class);
					XposedBridge.hookMethod(a2P, new XC_MethodHook() {
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							Object dataProvider = XposedHelpers.getObjectField(param.thisObject, "a");
							Object sQLiteDatabase = (SQLiteDatabase) XposedHelpers.callMethod(dataProvider,
									"getWritableDatabase");
							XposedHelpers.callMethod(sQLiteDatabase, "delete", new Object[] { "ads", null, null });
						};
					});

					Method b2P = XposedHelpers.findMethodExact(classObj, "a", String.class, String.class);
					XposedBridge.hookMethod(b2P, new XC_MethodHook() {
						protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
							Object dataProvider = XposedHelpers.getObjectField(param.thisObject, "a");
							Object sQLiteDatabase = (SQLiteDatabase) XposedHelpers.callMethod(dataProvider,
									"getWritableDatabase");
							XposedHelpers.callMethod(sQLiteDatabase, "delete", new Object[] { "ads", null, null });
						};
					});

					logError("YouTube AdAway: Successfully hooked ads DB: " + classObj.getName());
				}
			} catch (Throwable e) {
			}

			try {
				if (!isAdWrapperHooked
						&& XposedHelpers.findFirstFieldByExactType(classObj, Parcelable.Creator.class).getName()
								.equals("CREATOR")
						&& XposedHelpers.findFirstFieldByExactType(classObj, List.class).getName().equals("A")) {
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

						logError("YouTube AdAway: Successfully hooked " + classObj.getName() + " constructor!");
					} catch (Throwable e) {
						logError("YouTube AdAway: Failed to hook " + classObj.getName() + " constructor!");
						logError(e);
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
						logError("YouTube AdAway: Failed to hook " + classObj.getName() + " methods!");
						logError(e);
					}

				}

			} catch (Throwable e) {
			}
			return isAdWrapperHooked && isAdResponseHooked && isDBHooked;
		}

		@Override
		protected void onPostExecute(Boolean found) {

			if (!found) {
				logError("YouTube AdAway: brute force failed! Class/Param sequence not found");
				disableYouTubeAds(XYoutube.classLoader);
			}
		}

	}

}