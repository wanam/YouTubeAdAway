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

    private static void debug(String msg) {
        if (BuildConfig.DEBUG) {
            XposedBridge.log(msg);
        }
    }

    private static void debug(Throwable msg) {
        if (BuildConfig.DEBUG) {
            XposedBridge.log(msg);
        }
    }

    @Override
    protected Boolean doInBackground(LoadPackageParam... params) {
        ArrayList<String> cls=findHooks(params[0]);
        hookClasses(params[0].classLoader,cls);
        return !cls.isEmpty();
    }

    public static void hookClasses(ClassLoader cl,ArrayList<String> cls) {
        for (String clName:cls) {
            hookClass(cl,clName);
        }
    }

    public static ArrayList<String> findHooks(LoadPackageParam param) {
        ArrayList<String> res=new ArrayList<String>();
        String[] allCl=getAllClasses(param.appInfo);

        for (int i=0;i<allCl.length;i++) {
            String clName=allCl[i];
            if(clName.length()>6) continue;
            //if(clName.length()>3) continue; //currently 3 letters are sufficient
            //res|=findAndHookYouTubeAds(param.classLoader,clName);
            if(checkYouTubeAds(param.classLoader,clName)!=null) {
                res.add(clName);
            }
        }
        return res;
    }

    public static void fixedHooks(LoadPackageParam param) {
        hookClass(param.classLoader,"svh");
        hookClass(param.classLoader,"xbx");
        hookClass(param.classLoader,"xfm");
    }


    private static boolean checkAndHookYouTubeAds(ClassLoader cl,String clName) {
        Class<?> classObj=checkYouTubeAds(cl,clName);
        if(classObj!=null) {
            hookClass(classObj);
            return true;
        }
        return false;
    }

    private static Class<?> checkYouTubeAds(ClassLoader cl,String clName) {
        Class<?> classObj=null;
        Class<?> paramObj=null;

        try {
            classObj = XposedHelpers.findClass(clName, cl);
        } catch (Throwable e) {
            return null;
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
                    if(sMethList==null||paramObj==null) return null;

                    int numLong=0;
                    Field[] fields = classObj.getDeclaredFields();
                    for (Field f : fields) {
                        if (f.getType().equals(long.class)) {
                            numLong++;
                        }
                    }
                    if(numLong<=0) return null;

                    XposedBridge.log(">>>>>>>>>> class: " + clName + " methList:"+sMethList+" paramobj:"+sMethBool+" "+sTypeBool+" numlong:"+numLong);

                    return classObj;
                } catch (Throwable e) {
                    debug("YouTube AdAway: Failed to hook " + classObj.getName() + " methods!");
                    debug(e);
                }
            }
        } catch (Throwable e) {
        }

        return null;
    }

    private static void hookClass(ClassLoader cl,String clName)
    {
        Class<?> classObj=null;

        try {
            classObj = XposedHelpers.findClass(clName, cl);
        } catch (Throwable e) {
            return;
        }

        hookClass(classObj);
    }


    private static void hookClass(Class<?> classObj)
    {
        try {
            XposedBridge.hookAllConstructors(classObj, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    for(int i=0;param.args!=null&&i<param.args.length;i++)
                    {
                        Object arg=param.args[i];
                        if(arg!=null&&arg.getClass()==Long.class) {
                            long val=(Long)arg;
                            //looks like a duration
                            long maxdur=100000000;
                            if(val>0&&val<maxdur) param.args[i]=-1;
                            long iNow=Calendar.getInstance().getTimeInMillis();
                            //future timestamp=expiration
                            if(val>iNow-maxdur&&val<iNow+maxdur) param.args[i]=iNow-maxdur;
                            XposedBridge.log("class: " + param.thisObject.getClass().getName()+" param: " + i + " = " + arg.toString() + " -> " + param.args[i].toString());
                        }
                    }
                }
            });

            //XposedBridge.log("YouTube AdAway: Successfully hooked ads wrapper " + classObj.getName() + " param=" + paramObj.getName());
        } catch (Throwable e) {
            XposedBridge.log("YouTube AdAway: Failed to hook " + classObj.getName()  + " error: " + e);
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
