package ma.wanam.youtubeadaway;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;

import ma.wanam.youtubeadaway.utils.Constants;

/**
 * Created by M.karami on 07/08/2017.
 */

public class MyPreferenceProvider extends RemotePreferenceProvider {
    public MyPreferenceProvider() {
        super(Constants.GOOGLE_YOUTUBE_XPOSED, new String[]{Constants.MAIN_PREFS});
    }
}
