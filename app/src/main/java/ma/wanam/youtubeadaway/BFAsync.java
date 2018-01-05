package ma.wanam.youtubeadaway;

import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.reflect.Field;
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
    boolean isAdBuilderHooked = false;
    boolean isAdPropsHooked = false;

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

    private void debug(String e) {
        if (BuildConfig.DEBUG) {
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
            debug("YouTube AdAway: Successfully hooked ads fetchers" + adFetchers.toString());
        }
        return isAdWrapperHooked && isAdFetcherHooked && isAdDBHooked && isAdPropsHooked && isAdBuilderHooked;
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
    private void findAndHookYouTubeAds(char a1, char a2, char a3) {
        Class<?> classObj;
        Class<?> paramObj;
        final String lCRegex = "[a-z]+";
        final Pattern lCPatern = Pattern.compile(lCRegex);

        try {
            classObj = XposedHelpers.findClass(new StringBuffer().append(a1).append(a2).append(a3).toString(), cl);
        } catch (Throwable e1) {
            return;
        }

        try {
            if (!isAdBuilderHooked
                    && XposedHelpers.findFirstFieldByExactType(classObj, Parcelable.Creator.class).getName()
                    .equals("CREATOR")) {
                try {
                    XposedHelpers.findMethodExact(classObj, "A");
                } catch (Throwable t) {
                    XposedHelpers.findConstructorExact(classObj, Parcel.class);
                    XposedHelpers.findMethodExact(classObj, "a", Parcel.class);
                    XposedHelpers.findMethodExact(classObj, "b", Parcel.class);
                    Method m = XposedHelpers.findMethodExact(classObj, "a", List.class);
                    try {
                        XposedBridge.hookAllConstructors(classObj, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                if (param.args != null && param.args.length > 1 && lCPatern.matcher(param.args[0].getClass().getName()).matches()) {
                                    if (param.args.length > 3) {
                                        param.args[1] = false; //isLinearAdAllowed
                                        param.args[2] = false; //isNonlinearAdAllowed
                                        param.args[3] = false; //isDisplayAdAllowed
                                    }
                                }
                            }
                        });

                        XposedBridge.hookMethod(m, XC_MethodReplacement.returnConstant(Collections.EMPTY_LIST));
                        isAdBuilderHooked = true;
                        debug("YouTube AdAway: Successfully hooked ads builder " + classObj.getName());
                    } catch (Throwable e) {
                        logString("YouTube AdAway: Failed to hook " + classObj.getName() + " constructor!");
                        logThrowable(e);
                    }
                }
            }
        } catch (Throwable e) {
        }


        try {
            if (!isAdWrapperHooked
                    && XposedHelpers.findFirstFieldByExactType(classObj, Parcelable.Creator.class).getName()
                    .equals("CREATOR")
                    && XposedHelpers.findMethodExact(classObj, "A").getReturnType().equals(List.class)) {
                try {

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

                                        isAdWrapperHooked = true;
                                        debug("YouTube AdAway: Successfully hooked ads wrapper " + classObj.getName()
                                                + " param=" + paramObj.getName());
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
                debug("YouTube AdAway: Successfully hooked ads DB " + classObj.getName());
            } catch (Throwable ignored) {
            }
        }

        if (!isAdPropsHooked) {
            try {
                Class<?> cType = XposedHelpers.findMethodExact(classObj, "a").getReturnType();
                if (XposedHelpers.findFirstFieldByExactType(classObj, String.class).getName().equals("a")
                        && XposedHelpers.findMethodExact(classObj, "a", long.class).getReturnType() == cType
                        && XposedHelpers.findMethodExact(classObj, "a", boolean.class).getReturnType() == cType) {

                    Method[] methods = classObj.getDeclaredMethods();
                    final Field[] fields = classObj.getDeclaredFields();
                    for (Method m : methods) {
                        if (m.getParameterTypes().length == 0) {
                            XposedBridge.hookMethod(m, new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    for (Field f : fields) {
                                        if (f.getType() == Boolean.class) {
                                            XposedHelpers.setBooleanField(param.thisObject, f.getName(), Boolean.TRUE);
                                        }
                                    }
                                }
                            });
                        }
                    }
                    isAdPropsHooked = true;
                    debug("YouTube AdAway: Successfully hooked ads props " + classObj.getName());
                }
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
