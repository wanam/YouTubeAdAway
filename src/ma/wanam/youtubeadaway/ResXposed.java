package ma.wanam.youtubeadaway;

import ma.wanam.youtubeadaway.utils.Constants;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;

public class ResXposed implements IXposedHookZygoteInit, IXposedHookInitPackageResources {
	private static String MODULE_PATH = null;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		MODULE_PATH = startupParam.modulePath;
	}

	@Override
	public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
		if (resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_PACKAGE)
				|| resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_KIDS_PACKAGE)
				|| resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_GAMING)
				|| resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_MUSIC)
				|| resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_MANGO)
				|| resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_TV1_PACKAGE)
				|| resparam.packageName.equals(Constants.GOOGLE_YOUTUBE_TV2_PACKAGE)) {
			try {
				final android.content.res.XModuleResources moduleResources = android.content.res.XModuleResources
						.createInstance(MODULE_PATH, resparam.res);
				XYouTubeLayouts.doHook(resparam, moduleResources);
			} catch (Throwable e) {
				XposedBridge.log(e);
			}
		}

	}
}
