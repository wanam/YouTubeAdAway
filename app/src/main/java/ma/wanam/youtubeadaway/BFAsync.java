package ma.wanam.youtubeadaway;

import android.content.Context;
import android.os.AsyncTask;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class BFAsync extends AsyncTask<XC_LoadPackage.LoadPackageParam, Void, Boolean> {
    private boolean DEBUG = BuildConfig.DEBUG;
    private volatile boolean sigAdFound = false;
    private volatile boolean sigBgFound = false;

    @Override
    protected Boolean doInBackground(XC_LoadPackage.LoadPackageParam... params) {
        ClassLoader cl = params[0].classLoader;

        boolean foundBGClass = bruteForceBGP(cl);
        boolean foundInVideoAds = bruteForceInVideoAds(cl);

        return foundBGClass && foundInVideoAds;
    }

    private boolean bruteForceInVideoAds(ClassLoader cl) {
        Instant start = Instant.now();
        for (char a1 = 'a'; a1 <= 'z'; a1++) {
            for (char a2 = 'a'; a2 <= 'z'; a2++) {
                for (char a3 = 'a'; a3 <= 'z'; a3++) {
                    findAndHookInvideoAds('a', a1, a2, a3, cl);
                    if (sigAdFound) {
                        XposedBridge.log("In-Video ads hooks applied in " + Duration.between(start, Instant.now()).getSeconds() + " seconds!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean bruteForceBGP(ClassLoader cl) {
        Instant start = Instant.now();

        for (char a1 = 'a'; a1 <= 'z'; a1++) {
            for (char a2 = 'a'; a2 <= 'z'; a2++) {
                for (char a3 = 'a'; a3 <= 'z'; a3++) {
                    findAndHookVideoBGP4C('a', a1, a2, a3, cl);
                    if (sigBgFound) {
                        XposedBridge.log("Video BG playback hooks applied in " + Duration.between(start, Instant.now()).getSeconds() + " seconds!");
                        return true;
                    }
                }
            }
        }

        for (char a1 = 'z'; a1 >= 'a'; a1--) {
            for (char a2 = 'z'; a2 >= 'a'; a2--) {
                for (char a3 = 'z'; a3 >= 'a'; a3--) {
                    findAndHookVideoBGP3C(a1, a2, a3, cl);
                    if (sigBgFound) {
                        XposedBridge.log("Video BG playback hooks applied in " + Duration.between(start, Instant.now()).getSeconds() + " seconds!");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void findAndHookInvideoAds(char a1, char a2, char a3, char a4, ClassLoader cl) {
        Class<?> aClass;
        Field[] fields;
        Method[] methods;

        try {
            aClass = XposedHelpers.findClass(new StringBuffer().append(a1).append(a2).append(a3).append(a4).toString(), cl);
            fields = aClass.getDeclaredFields();
            methods = aClass.getDeclaredMethods();
        } catch (Throwable e1) {
            return;
        }

        try {
            if (!sigAdFound) {
                sigAdFound = fields.length < 10 && (int) Arrays.asList(fields).parallelStream().filter(field -> field.getType().equals(Executor.class)
                        || field.getType().equals(LinkedBlockingQueue.class)
                        || field.getType().equals(Runnable.class)).count() == 3;

                if (sigAdFound) {
                    Optional<Method> fMethod = Arrays.asList(methods).parallelStream().filter(method -> method.getParameterTypes().length == 1
                            && method.getParameterTypes()[0].equals(boolean.class)
                            && method.getReturnType().equals(void.class)
                            && java.lang.reflect.Modifier.isFinal(method.getModifiers())
                    ).findAny();

                    sigAdFound = sigAdFound && fMethod.isPresent();
                    if (sigAdFound) {
                        XposedBridge.hookMethod(fMethod.get(), new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                param.args[0] = false;
                            }
                        });
                        XposedBridge.log("Hooked ad class: " + aClass.getName() + "." + fMethod.get().getName());
                    }
                }
            }

        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook in-video ads class: " + aClass.getName());
            XposedBridge.log(e);
        }
    }

    // for YT 17.42+
    private void findAndHookVideoBGP4C(char a0, char a1, char a2, char a3, ClassLoader cl) {
        Class<?> aClass;
        Method[] methods;

        try {
            aClass = XposedHelpers.findClass(new StringBuffer().append(a0).append(a1).append(a2).append(a3).toString(), cl);
            methods = aClass.getDeclaredMethods();
        } catch (Throwable e1) {
            return;
        }

        try {
            if (!sigBgFound) {
                List<Method> fMethods = Arrays.asList(methods).parallelStream().filter(method -> method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0].getName().length() == 4
                        && method.getReturnType().equals(boolean.class)
                        && method.getName().equals(method.getName().toLowerCase())
                        && java.lang.reflect.Modifier.isStatic(method.getModifiers())
                        && java.lang.reflect.Modifier.isPublic(method.getModifiers())
                ).collect(Collectors.toList());

                sigBgFound = fMethods.size() > 5;
                if (sigBgFound) {
                    XposedBridge.hookMethod(fMethods.get(0), XC_MethodReplacement.returnConstant(true));
                    XposedBridge.log("Hooked bg class: " + aClass.getName() + "." + fMethods.get(0).getName());
                }
            }
        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook video bg playback class: " + aClass.getName());
            XposedBridge.log(e);
        }
    }

    // for YT 17.41-
    private void findAndHookVideoBGP3C(char a1, char a2, char a3, ClassLoader cl) {
        Class<?> aClass;
        Method[] methods;

        try {
            aClass = XposedHelpers.findClass(new StringBuffer().append(a1).append(a2).append(a3).toString(), cl);
            methods = aClass.getDeclaredMethods();
        } catch (Throwable e1) {
            return;
        }

        try {
            if (!sigBgFound) {
                Class cListenableFuture = XposedHelpers.findClass("com.google.common.util.concurrent.ListenableFuture", cl);
                Optional<Method> fMethod = Arrays.asList(methods).parallelStream().filter(method -> method.getParameterTypes().length == 2
                        && method.getParameterTypes()[0].equals(Context.class)
                        && method.getParameterTypes()[1].equals(Executor.class)
                        && method.getReturnType().equals(cListenableFuture)
                        && java.lang.reflect.Modifier.isStatic(method.getModifiers())
                ).findAny();

                sigBgFound = fMethod.isPresent();

                if (sigBgFound) {
                    fMethod = Arrays.asList(methods).parallelStream().filter(method -> method.getParameterTypes().length == 1
                            && method.getParameterTypes()[0].getName().length() == 4
                            && method.getReturnType().equals(boolean.class)
                            && method.getName().equals(method.getName().toLowerCase())
                            && java.lang.reflect.Modifier.isStatic(method.getModifiers())
                            && java.lang.reflect.Modifier.isPublic(method.getModifiers())
                    ).findFirst();

                    sigBgFound = sigBgFound && fMethod.isPresent();
                    if (sigBgFound) {
                        XposedBridge.hookMethod(fMethod.get(), XC_MethodReplacement.returnConstant(true));
                        XposedBridge.log("Hooked bg class: " + aClass.getName() + "." + fMethod.get().getName());
                    }
                }
            }
        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook video bg playback class: " + aClass.getName());
            XposedBridge.log(e);
        }
    }

    @Override
    protected void onPostExecute(Boolean found) {

        if (!found) {
            XposedBridge.log("YouTube AdAway: brute force failed!");
        }
    }

}
