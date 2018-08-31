package ma.wanam.youtubeadaway;

import android.os.AsyncTask;
import android.os.Parcelable;
import android.content.pm.ApplicationInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;

import dalvik.system.DexFile;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class BFAsync extends AsyncTask<LoadPackageParam, Void, Boolean> {
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
    protected Boolean doInBackground(LoadPackageParam... params) {
        cl = params[0].classLoader;

        String[] allCl=getAllClasses(params[0].appInfo);

        final String lCRegex = "[a-z]+";
        final Pattern lCPatern = Pattern.compile(lCRegex);

        for (int i=0;i<allCl.length;i++) {
            String clName=allCl[i];
            if(clName.length()>6) continue;
            findAndHookYouTubeAds(clName,lCPatern);
        }

        return found;
    }


    /**
     * @param a1
     * @param a2
     * @param a3
     * @return true if a hook was found
     */
    private void findAndHookYouTubeAds(String clName,Pattern lCPatern) {
        Class<?> classObj=null;
        Class<?> paramObj=null;

        try {
            classObj = XposedHelpers.findClass(clName, cl);
        } catch (Throwable e) {
            return;
        }

        try {
            if (XposedHelpers.findFirstFieldByExactType(classObj, Parcelable.Creator.class).getName()
                    .equals("CREATOR")
                    /*&& XposedHelpers.findMethodExact(classObj, "A").getReturnType().equals(List.class)*/) {
                try {

                    Method[] methods = classObj.getDeclaredMethods();

                    String sMethList=null;
                    String sMethBool="";
                    String sTypeBool="";

                    for (Method m : methods) {
                        if (m.getReturnType().equals(List.class)&&m.getParameterTypes().length==0) {
                            sMethList=m.getName();
                        }

                        if (m.getReturnType().equals(boolean.class)&& m.getParameterTypes().length == 1) {
                            if(m.getName().length()<6) {
                                paramObj = m.getParameterTypes()[0];

                                if(sMethBool.length()>0) sMethBool +=",";
                                sMethBool += m.getName();

                                if(sTypeBool.length()>0) sTypeBool +=",";
                                sTypeBool += m.getParameterTypes()[0].getName();
                            }
                        }
                    }
                    if(sMethList==null||paramObj==null) return;

                    int numLong=0;
                    Field[] fields = classObj.getDeclaredFields();
                    for (Field f : fields) {
                        if (f.getType().equals(long.class)) {
                            numLong++;
                        }
                    }
                    if(numLong<=0) return;


                    XposedBridge.log(">>>>>>>>>> class: " + clName + " methList:"+sMethList+" paramobj:"+sMethBool+" "+sTypeBool+" numlong:"+numLong);

                    try {
                        XposedBridge.hookAllConstructors(classObj, new XC_MethodHook() {
                            @Override
                            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                for(int i=0;param.args!=null&&i<param.args.length;i++)
                                {
                                    Object arg=param.args[i];
                                    if(arg!=null&&arg.getClass()==Long.class) {
                                        long val=(Long)arg;
                                        XposedBridge.log("class: " + param.thisObject.getClass().getName()+" param: " + i + " = " + arg.toString());
                                        //looks like a duration
                                        long maxdur=100000000;
                                        if(val>0&&val<maxdur) param.args[i]=-1;
                                        long iNow=Calendar.getInstance().getTimeInMillis();
                                        //future timestamp=expiration
                                        if(val>iNow&&val<iNow+maxdur) param.args[i]=Calendar.getInstance().getTimeInMillis();
                                    }
                                }
                            }
                        });
                        found = true;
                        //XposedBridge.log("YouTube AdAway: Successfully hooked ads wrapper " + classObj.getName() + " param=" + paramObj.getName());
                    } catch (Throwable e) {
                        XposedBridge.log("YouTube AdAway: Failed to hook " + classObj.getName() + " param=" + paramObj.getName() + " error: " + e);
                    }

                } catch (Throwable e) {
                    debug("YouTube AdAway: Failed to hook " + classObj.getName() + " methods!");
                    debug(e);
                }
            }
        } catch (Throwable e) {
        }
    }

    private static String[] getAllClasses(ApplicationInfo ai) {
        ArrayList<String> classes = new ArrayList<String>();
        try {
            XposedBridge.log(">>>>>>>>>> sourceDir: " + ai.sourceDir);
            DexFile df = new DexFile(ai.sourceDir);
            int iCnt=0;
            for (Enumeration<String> iter = df.entries(); iter.hasMoreElements(); ) {
                String className = iter.nextElement();
                iCnt++;
                classes.add(className);
            }

            XposedBridge.log(">>>>>>>>>> count: " + classes.size()+"/"+iCnt);
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return toStringArray(classes);
    }
    private static String[] toStringArray(ArrayList<String> classes) {
        String[] array = new String[classes.size()];
        for (int i = 0; i < classes.size(); i++) {
            array[i] = classes.get(i);
        }
        return array;
    }

    @Override
    protected void onPostExecute(Boolean found) {

        if (!found) {
            XposedBridge.log("YouTube AdAway: brute force failed!");
        }
    }

}
