package ma.wanam.youtubeadaway.utils;

import java.util.List;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.callbacks.XC_LayoutInflated.LayoutInflatedParam;

public class Utils {

	public static PackageInfo pInfo;

	public static boolean isPackageInstalled(Context context, String targetPackage) {
		List<ApplicationInfo> packages;
		PackageManager pm;
		pm = context.getPackageManager();
		packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
		for (ApplicationInfo packageInfo : packages) {
			if (packageInfo.packageName.equals(targetPackage))
				return true;
		}
		return false;
	}

	public static void hideLayoutContent(LayoutInflatedParam layout) {
		try {

			ViewGroup vg = ((ViewGroup) layout.view);
			int childCount = vg.getChildCount();
			if (childCount > 0) {
				for (int i = 0; i < childCount; i++) {
					try {
						View v = vg.getChildAt(i);
						v.setVisibility(View.GONE);
					} catch (Throwable e) {
					}
				}
			}
		} catch (Throwable e) {
		} finally {
			layout.view.setVisibility(View.GONE);
		}
	}

	public static boolean isTvPackage(String targetPackage) {
		return targetPackage.equals(Constants.GOOGLE_YOUTUBE_TV1_PACKAGE)
				|| targetPackage.equals(Constants.GOOGLE_YOUTUBE_TV2_PACKAGE);
	}

}
