package ma.wanam.youtubeadaway;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Parcelable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class BFAsync extends AsyncTask<Params, Void, Boolean> {
    ClassLoader cl;
    SharedPreferences prefs;
    boolean isAdFetcherHooked = false;
    StringBuilder adFetchers = new StringBuilder();
    boolean isAdDBHooked = false;
    Class<?> jSONObject = null;
    boolean isAdWrapperHooked = false;

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
        jSONObject = XposedHelpers.findClass("org.json.JSONObject", cl);

        for (char a1 = 'z'; a1 >= 'a'; a1--) {
            for (char a2 = 'z'; a2 >= 'a'; a2--) {
                for (char a3 = 'z'; a3 >= 'a'; a3--) {
                    findAndHookYouTubeAds(a1, a2, a3);
                }
            }
        }

        if (adFetchers.length() > 0) {
            logString("YouTube AdAway: Successfully hooked ads fetchers" + (BuildConfig.DEBUG ? adFetchers.toString() : ""));
        }
        return isAdWrapperHooked && isAdFetcherHooked && isAdDBHooked;
    }

    private void deleteAds(MethodHookParam param) {
        try {
            if (param.thisObject == null) return;
            Object dataProvider = XposedHelpers.getObjectField(param.thisObject, "a");
            if (dataProvider != null) {
                Object sQLiteDatabase = (SQLiteDatabase) XposedHelpers.callMethod(dataProvider, "getWritableDatabase");
                if (sQLiteDatabase != null) {
                    String[] tables = new String[]{"ads", "adbreaks", "ad_videos"};
                    for (String table : tables) {
                        try {
                            XposedHelpers.callMethod(sQLiteDatabase, "delete", new Object[]{table, null, null});
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable e) {
        }
    }

    /**
     * @param a1
     * @param a2
     * @param a3
     * @return true if a hook was found
     */
    private synchronized void findAndHookYouTubeAds(char a1, char a2, char a3) {
        Class<?> classObj;
        Class<?> paramObj;
        final String lCRegex = "[a-z]+";
        Pattern lCPatern = null;

        try {
            classObj = XposedHelpers.findClass(new StringBuffer().append(a1).append(a2).append(a3).toString(), cl);
        } catch (Throwable e1) {
            return;
        }

        try {
            if (!isAdWrapperHooked
                    && XposedHelpers.findFirstFieldByExactType(classObj, Parcelable.Creator.class).getName()
                    .equals("CREATOR")
                    && XposedHelpers.findMethodExact(classObj, "A").getReturnType().equals(List.class)) {
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
                    isAdWrapperHooked = true;
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

                                        logString("YouTube AdAway: Successfully hooked ads wrapper " + (BuildConfig.DEBUG ? classObj.getName()
                                                + " param=" + paramObj.getName() : ""));
                                        break;
                                    } catch (Throwable e) {
                                        logString("YouTube AdAway: Failed to hook " + classObj.getName()
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

        if (!isAdDBHooked) {
            try {
                Method mGetReadableDatabase = XposedHelpers.findMethodExact(classObj, "getReadableDatabase");
                XposedBridge.hookMethod(mGetReadableDatabase, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        deleteAds(param);
                    }
                });

                isAdDBHooked = true;
                logString("YouTube AdAway: Successfully hooked ads DB " + classObj.getName());
            } catch (Throwable ignored) {
            }
        }

        try {
            if (XposedHelpers.findMethodExact(classObj, "a", jSONObject).getReturnType() == Void.TYPE
                    && XposedHelpers.findMethodExact(classObj, "a").getReturnType().equals(int.class)
                    && classObj.getDeclaredFields().length == 1) {

                Method[] methods = classObj.getDeclaredMethods();
                for (Method m : methods) {
                    XposedBridge.hookMethod(m, XC_MethodReplacement.DO_NOTHING);
                }

                isAdFetcherHooked = true;
                adFetchers.append(" " + classObj.getName());
            }
        } catch (Throwable ignored) {
        }

    }

    @Override
    protected void onPostExecute(Boolean found) {

        if (!found) {
            logString("YouTube AdAway: brute force failed! Class/Param sequence not found");
        }
    }

}
