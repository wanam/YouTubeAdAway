package ma.wanam.youtubeadaway;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
    private volatile boolean generalAdsFound = false;
    private volatile Method emptyComponentMethod = null;
    private volatile Method fingerprintMethod = null;

    private static final String filterAds = new StringBuffer().append(".*(").append(String.join("|", new String[]{
            "ads_video_with_context",
            "banner_text_icon",
            "square_image_layout",
            "watch_metadata_app_promo",
            "video_display_full_layout",
            "browsy_bar",
            "compact_movie",
            "horizontal_movie_shelf",
            "movie_and_show_upsell_card",
            "compact_tvfilm_item",
            "video_display_full_buttoned_layout",
            "full_width_square_image_layout",
            "_ad_with",
            "landscape_image_wide_button_layout",
            "carousel_ad",
            "in_feed_survey",
            "compact_banner"
    })).append(").*").toString();

    private static final String filterIgnore = new StringBuffer().append(".*(").append(String.join("|", new String[]{
            "home_video_with_context",
            "related_video_with_context",
            "comment_thread",
            "comment\\.",
            "download_",
            "library_recent_shelf",
            "playlist_add_to_option_wrapper"
    })).append(").*").toString();


    @Override
    protected Boolean doInBackground(XC_LoadPackage.LoadPackageParam... params) {
        ClassLoader cl = params[0].classLoader;

        boolean foundBGClass = bruteForceBGP(cl);
        boolean foundInVideoAds = bruteForceInVideoAds(cl);
        boolean foundGNADS = bruteForceGADS(cl);

        return foundBGClass && foundInVideoAds && foundGNADS;
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
                Class<?> cListenableFuture = XposedHelpers.findClass("com.google.common.util.concurrent.ListenableFuture", cl);
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

    private boolean bruteForceGADS(ClassLoader cl) {
        Instant start = Instant.now();
        start:
        for (char a1 = 'a'; a1 <= 'z'; a1++) {
            for (char a2 = 'a'; a2 <= 'z'; a2++) {
                for (char a3 = 'a'; a3 <= 'z'; a3++) {
                    findAdCardsMethods(a1, a2, a3, cl);
                    if (emptyComponentMethod != null && fingerprintMethod != null)
                        break start;
                }
            }
        }

        if (emptyComponentMethod == null || fingerprintMethod == null)
            return false;

        hookAdCardsMethods(fingerprintMethod, emptyComponentMethod);
        XposedBridge.log("Ad cards hooks applied in " + Duration.between(start, Instant.now()).getSeconds() + " seconds!");
        generalAdsFound = true;
        return true;
    }

    private void findAdCardsMethods(char a1, char a2, char a3, ClassLoader cl) {
        Class<?> aClass;
        Method[] methods;

        try {
            aClass = XposedHelpers.findClass(new StringBuffer().append(a1).append(a2).append(a3).toString(), cl);
            methods = aClass.getDeclaredMethods();
        } catch (Throwable e1) {
            return;
        }

        try {
            if (fingerprintMethod == null) {
                List<Method> fMethods = Arrays.asList(methods).parallelStream().filter(method -> method.getParameterTypes().length == 7
                        && method.getParameterTypes()[0].getName().length() == 3
                        && method.getParameterTypes()[6].equals(boolean.class)
                        && method.getParameterTypes()[5].equals(int.class)
                        && method.getName().equals(method.getName().toLowerCase())
                        && Modifier.isFinal(method.getModifiers())
                        && Modifier.isPublic(method.getModifiers())
                ).collect(Collectors.toList());

                if (fMethods.size() == 1) {
                    fingerprintMethod = fMethods.size() == 1 ? fMethods.get(0) : null;
                    XposedBridge.log("Found ad cards class: " + aClass.getName() + "." + fMethods.get(0).getName());
                    return;
                }
            }
        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook ad cards class: " + aClass.getName());
            XposedBridge.log(e);
        }

        try {
            if (emptyComponentMethod == null) {
                List<Method> fMethods = Arrays.asList(methods).parallelStream().filter(method ->
                        Modifier.isPublic(method.getModifiers())
                                && Modifier.isStatic(method.getModifiers())
                                && method.getParameterTypes().length == 1
                ).collect(Collectors.toList());

                List<Method> fMethods2 = Arrays.asList(methods).parallelStream().filter(method -> Modifier.isProtected(method.getModifiers())
                        && Modifier.isFinal(method.getModifiers())
                        && method.getParameterTypes().length == 1
                ).collect(Collectors.toList());

                if (fMethods.size() == 1 && fMethods2.size() == 1 && methods.length == 2) {
                    emptyComponentMethod = fMethods.get(0);
                    XposedBridge.log("Found emptyComponent class: " + aClass.getName() + "." + fMethods.get(0).getName());
                }
            }
        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook EmptyElement class: " + aClass.getName());
            XposedBridge.log(e);
        }
    }

    private void hookAdCardsMethods(Method cMethod, final Method emptyMethod) {
        final Optional<Method> filterMethod = Arrays.asList(cMethod.getParameterTypes()[1].getDeclaredMethods()).parallelStream().filter(method ->
                Modifier.isPublic(method.getModifiers())
                        && Modifier.isFinal(method.getModifiers())
                        && method.getName().length() == 1
                        && method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0].equals(String.class)
        ).findFirst();

        if (!filterMethod.isPresent()) {
            XposedBridge.log("YouTube AdAway: Failed to find filter method!");
            return;
        }

        XposedBridge.hookMethod(cMethod, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                String val = (String) filterMethod.get().invoke(param.args[1], "");
                if (!TextUtils.isEmpty(val) && !val.matches(filterIgnore) && val.matches(filterAds)) {
                    Object x = emptyMethod.invoke(null, param.args[0]);
                    Object y = XposedHelpers.getObjectField(x, "a");
                    param.setResult(y);
                }
            }
        });
    }

    @Override
    protected void onPostExecute(Boolean found) {
        if (!found) {
            XposedBridge.log("YouTube AdAway: brute force failed!");
        }
    }

}
