package ma.wanam.youtubeadaway;

import android.os.AsyncTask;
import android.os.Parcelable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class BFAsync extends AsyncTask<ClassLoader, Void, Boolean> {
    private static ClassLoader cl;
    private static boolean found = false;

    private void debug(String msg) {
        if (BuildConfig.DEBUG) {
            XposedBridge.log(msg);
        }
    }

    private void debug(Throwable msg) {
        if (BuildConfig.DEBUG) {
            XposedBridge.log(msg);
        }
    }

    @Override
    protected Boolean doInBackground(ClassLoader... params) {
        cl = params[0];

        for (char a1 = 'z'; a1 >= 'a'; a1--) {
            for (char a2 = 'z'; a2 >= 'a'; a2--) {
                for (char a3 = 'z'; a3 >= 'a'; a3--) {
                    findAndHookYouTubeAds(a1, a2, a3);
                }
            }
        }

        return found;
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
            if (XposedHelpers.findFirstFieldByExactType(classObj, Parcelable.Creator.class).getName()
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
                                        XposedBridge.hookAllConstructors(classObj, new XC_MethodHook() {
                                            @Override
                                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                                Field[] fields = param.thisObject.getClass().getDeclaredFields();
                                                for (Field f : fields) {
                                                    if (f.getType().equals(long.class)) {
                                                        long timestamp = Calendar.getInstance().getTimeInMillis();
                                                        debug("class: " + param.thisObject.getClass().getName());
                                                        debug("set expiry timestamp: " + f.getName() + " = " + timestamp);
                                                        XposedHelpers.setLongField(param.thisObject, f.getName(), timestamp);
                                                        break;
                                                    }
                                                }
                                            }
                                        });

                                        found = true;

                                        debug("YouTube AdAway: Successfully hooked ads wrapper " + classObj.getName()
                                                + " param=" + paramObj.getName());
                                    } catch (Throwable e) {
                                        debug("YouTube AdAway: Failed to hook " + classObj.getName()
                                                + " param=" + paramObj.getName() + " error: " + e);
                                    }
                                }
                            }
                        }
                    }

                } catch (Throwable e) {
                    debug("YouTube AdAway: Failed to hook " + classObj.getName() + " methods!");
                    debug(e);
                }
            }
        } catch (Throwable e) {
        }
    }

    @Override
    protected void onPostExecute(Boolean found) {

        if (!found) {
            XposedBridge.log("YouTube AdAway: brute force failed!");
        }
    }

}
