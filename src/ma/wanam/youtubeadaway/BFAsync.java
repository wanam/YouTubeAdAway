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

public class BFAsync extends AsyncTask<Params, Void, Boolean> {
	ClassLoader cl;
	XSharedPreferences prefs;
	boolean isAdWrapperHooked = false;
	boolean isDBHooked = false;

	private void logThrowable(Throwable e) {
		if (prefs.getBoolean("enableLogs", true)) {
			XposedBridge.log(e);
		}
	}

	private void logString(String e) {
		if (prefs.getBoolean("enableLogs", true)) {
			XposedBridge.log(e);
		}
	}

	@Override
	protected Boolean doInBackground(Params... params) {
		cl = params[0].getClassLoader();
		prefs = params[0].getxPrefs();
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
			classObj = XposedHelpers.findClass(new StringBuffer().append(a1).append(a2).append(a3).toString(), cl);
		} catch (Throwable e1) {
			return false;
		}

		try {
			if (!isDBHooked && XposedHelpers.findFirstFieldByExactType(classObj, String[].class).getName().equals("b")
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
								XposedHelpers.callMethod(sQLiteDatabase, "delete", new Object[] { "ads", null, null });
							}
						}
					};
				});

				logString("YouTube AdAway: Successfully hooked ads DB: " + classObj.getName());
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

				} catch (Throwable e) {
					logString("YouTube AdAway: Failed to hook " + classObj.getName() + " constructor!");
					logThrowable(e);
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
										XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(Boolean.FALSE));

										XposedBridge.log("YouTube AdAway: Successfully hooked " + classObj.getName()
												+ " param=" + paramObj.getName());
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
					logString("YouTube AdAway: Failed to hook " + classObj.getName() + " methods!");
					logThrowable(e);
				}

			}

		} catch (Throwable e) {
		}
		return isAdWrapperHooked && isDBHooked;
	}

	@Override
	protected void onPostExecute(Boolean found) {

		if (!found) {
			logString("YouTube AdAway: brute force failed! Class/Param sequence not found");
		}
	}

}
