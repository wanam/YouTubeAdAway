package ma.wanam.youtubeadaway;

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
import ma.wanam.youtubeadaway.utils.Class3C;

public class BFAsync extends AsyncTask<XC_LoadPackage.LoadPackageParam, Void, Boolean> {
    private boolean DEBUG = BuildConfig.DEBUG;
    private volatile Method emptyComponentMethod = null;
    private volatile Method fingerprintMethod = null;
    private volatile Optional<Field> pathBuilderField = Optional.empty();
    private XC_MethodHook.Unhook unhookFilterMethod;

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
            "post_base_wrapper",
            "community_guidelines",
            "sponsorships_comments_upsell",
            "member_recognition_shelf",
            "compact_banner",
            "in_feed_survey",
            "medical_panel",
            "paid_content_overlay",
            "product_carousel",
            "publisher_transparency_panel",
            "single_item_information_panel",
            "horizontal_video_shelf",
            "post_shelf",
            "channel_guidelines_entry_banner",
            "official_card",
            "cta_shelf_card",
            "expandable_metadata",
            "cell_divider"
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

        XposedHelpers.findAndHookMethod("com.google.android.apps.youtube.app.watchwhile.WatchWhileActivity", cl, "onBackPressed", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                XposedHelpers.callMethod(param.thisObject, "finish");
            }
        });

        return bruteForceAds(cl);
    }

    private boolean bruteForceAds(ClassLoader cl) {
        Instant start = Instant.now();
        boolean foundBGClass = false, foundInVideoAds = false, foundCardAds = false, skip;

        Class3C heapPermutation = new Class3C();
        while (heapPermutation.hasNext()) {
            String clsName = heapPermutation.next();
            skip = false;

            if (foundInVideoAds && foundCardAds && foundBGClass) return true;

            if (!skip && !foundInVideoAds) {
                foundInVideoAds = findAndHookInvideoAds(new StringBuilder().append('a').append(clsName).toString(), cl);
                if (foundInVideoAds) {
                    skip = true;
                    XposedBridge.log("In-Video ads hooks applied in " + Duration.between(start, Instant.now()).getSeconds() + " seconds!");
                }
            }

            if (!skip && !foundBGClass) {
                foundBGClass = findAndHookVideoBGP4C(new StringBuilder().append('a').append(clsName).toString(), cl);
                if (foundBGClass) {
                    skip = true;
                    XposedBridge.log("Video BG playback hooks applied in " + Duration.between(start, Instant.now()).getSeconds() + " seconds!");
                }
            }

            if (!skip && !foundCardAds) {
                foundCardAds = findAdCardsMethods(new StringBuilder().append(clsName).toString(), cl);
                if (foundCardAds) {
                    hookAdCardsMethods(fingerprintMethod, emptyComponentMethod);
                    XposedBridge.log("Ad cards hooks applied in " + Duration.between(start, Instant.now()).getSeconds() + " seconds!");
                }
            }
        }
        return false;
    }

    private boolean findAndHookInvideoAds(String clsName, ClassLoader cl) {
        Class<?> aClass;
        Field[] fields;
        Method[] methods;

        try {
            aClass = XposedHelpers.findClass(clsName, cl);
            fields = aClass.getDeclaredFields();
            methods = aClass.getDeclaredMethods();
        } catch (Throwable e1) {
            return false;
        }

        try {
            boolean sigAdFound = fields.length < 10 && (int) Arrays.stream(fields).parallel().filter(field -> field.getType().equals(Executor.class)
                    || field.getType().equals(LinkedBlockingQueue.class)
                    || field.getType().equals(Runnable.class)).count() == 3;

            if (sigAdFound) {
                Optional<Method> fMethod = Arrays.stream(methods).parallel().filter(method -> method.getParameterTypes().length == 1
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
                    XposedBridge.log("Found ad class: " + aClass.getName() + "." + fMethod.get().getName());
                    return true;
                }
            }

        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook in-video ads class: " + aClass.getName());
            XposedBridge.log(e);
        }
        return false;
    }

    private boolean findAndHookVideoBGP4C(String clsName, ClassLoader cl) {
        Class<?> aClass;
        Method[] methods;

        try {
            aClass = XposedHelpers.findClass(clsName, cl);
            methods = aClass.getDeclaredMethods();
        } catch (Throwable e1) {
            return false;
        }

        try {
            List<Method> fMethods = Arrays.stream(methods).parallel().filter(method -> method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].getName().length() == 4
                    && method.getReturnType().equals(boolean.class)
                    && method.getName().equals(method.getName().toLowerCase())
                    && java.lang.reflect.Modifier.isStatic(method.getModifiers())
                    && java.lang.reflect.Modifier.isPublic(method.getModifiers())
            ).collect(Collectors.toList());

            if (fMethods.size() > 5) {
                XposedBridge.hookMethod(fMethods.get(0), XC_MethodReplacement.returnConstant(true));
                XposedBridge.log("Found bg class: " + aClass.getName() + "." + fMethods.get(0).getName());
                return true;
            }

        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook video bg playback class: " + aClass.getName());
            XposedBridge.log(e);
        }
        return false;
    }

    private boolean findAdCardsMethods(String clsName, ClassLoader cl) {
        Class<?> aClass;
        Method[] methods;

        try {
            aClass = XposedHelpers.findClass(clsName, cl);
            methods = aClass.getDeclaredMethods();
        } catch (Throwable e1) {
            return false;
        }

        try {
            if (fingerprintMethod == null) {
                List<Method> fMethods = Arrays.stream(methods).parallel().filter(method -> method.getParameterTypes().length == 7
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
                    return fingerprintMethod != null && emptyComponentMethod != null;
                }
            }
        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook ad cards class: " + aClass.getName());
            XposedBridge.log(e);
        }

        try {
            if (emptyComponentMethod == null) {
                List<Method> fMethods = Arrays.stream(methods).parallel().filter(method ->
                        Modifier.isPublic(method.getModifiers())
                                && Modifier.isStatic(method.getModifiers())
                                && method.getParameterTypes().length == 1
                ).collect(Collectors.toList());

                List<Method> fMethods2 = Arrays.stream(methods).parallel().filter(method -> Modifier.isProtected(method.getModifiers())
                        && Modifier.isFinal(method.getModifiers())
                        && method.getParameterTypes().length == 1
                ).collect(Collectors.toList());

                if (fMethods.size() == 1 && fMethods2.size() == 1 && methods.length == 2) {
                    emptyComponentMethod = fMethods.get(0);
                    XposedBridge.log("Found emptyComponent class: " + aClass.getName() + "." + fMethods.get(0).getName());
                    return fingerprintMethod != null && emptyComponentMethod != null;
                }
            }
        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook EmptyElement class: " + aClass.getName());
            XposedBridge.log(e);
        }
        return false;
    }

    private void hookAdCardsMethods(Method fingerprintMethod, final Method emptyComponentMethod) {

        try {
            unhookFilterMethod = XposedBridge.hookMethod(fingerprintMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    if (!pathBuilderField.isPresent()) {
                        pathBuilderField = Arrays.stream(param.args[1].getClass().getDeclaredFields()).parallel().filter(field ->
                                field.getType().equals(StringBuilder.class)
                                        && Modifier.isFinal(field.getModifiers())
                                        && Modifier.isPublic(field.getModifiers())
                        ).findAny();
                    }

                    if (pathBuilderField.isPresent()) {
                        // Get template path
                        String pathBuilder = XposedHelpers.getObjectField(param.args[1], pathBuilderField.get().getName()).toString();
                        if (!TextUtils.isEmpty(pathBuilder) && !pathBuilder.matches(filterIgnore) && pathBuilder.matches(filterAds)) {
                            // Create emptyComponent from current componentContext
                            Object x = emptyComponentMethod.invoke(null, param.args[0]);
                            // Get created emptyComponent
                            Object y = XposedHelpers.getObjectField(x, "a");
                            param.setResult(y);
                        }
                    } else {
                        XposedBridge.log("Unable to find template's pathBuilder");
                        unhookFilterMethod.unhook();
                    }
                }
            });
        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Error hooking AdCards!");
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