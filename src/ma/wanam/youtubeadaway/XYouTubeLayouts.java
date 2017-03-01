package ma.wanam.youtubeadaway;

import java.util.ArrayList;

import ma.wanam.youtubeadaway.utils.Utils;
import android.content.res.XModuleResources;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

public class XYouTubeLayouts {

	private static ArrayList<String> blockedPromotedLayers = null;

	public static void doHook(InitPackageResourcesParam resparam, XModuleResources moduleResources) {

		try {
			blockedPromotedLayers = new ArrayList<String>();

			blockedPromotedLayers.add("watch_metadata_cards");
			blockedPromotedLayers.add("inline_ad_overlay");
			blockedPromotedLayers.add("default_ad_overlay");
			blockedPromotedLayers.add("embedded_ad_overlay");
			blockedPromotedLayers.add("mdx_ad_overlay");

			blockedPromotedLayers.add("promoted_video_wrapper");
			blockedPromotedLayers.add("promoted_app_install_wrapper");
			blockedPromotedLayers.add("promoted_text_banner_wrapper");
			
			blockedPromotedLayers.add("info_card_shopping");
			blockedPromotedLayers.add("shopping_companion_product_view");
			blockedPromotedLayers.add("shopping_companion_card_white");
			blockedPromotedLayers.add("shopping_companion_card_grey");
			blockedPromotedLayers.add("info_card_shopping_watch_next");

			blockedPromotedLayers.add("promoted_video_item");
			blockedPromotedLayers.add("q_promoted_video_item");
			blockedPromotedLayers.add("compact_promoted_video_item");
			blockedPromotedLayers.add("generic_promo_banner");
			blockedPromotedLayers.add("promoted_app_install");
			blockedPromotedLayers.add("compact_promoted_item");
			blockedPromotedLayers.add("interstitial_promo_view");
			blockedPromotedLayers.add("promoted_tall_descriptive_banner");
			blockedPromotedLayers.add("promoted_short_descriptive_banner");
			blockedPromotedLayers.add("grid_promoted_banner");
			blockedPromotedLayers.add("music_key_promo_banner");
			blockedPromotedLayers.add("music_key_promo_feature_item");
			blockedPromotedLayers.add("music_key_promo_small_feature_item");

			blockedPromotedLayers.add("promoted_app_install_right_align_layout");
			blockedPromotedLayers.add("sign_in_promo");
			blockedPromotedLayers.add("promoted_text_banner_layout_one");
			blockedPromotedLayers.add("promoted_text_banner_layout_two");
			blockedPromotedLayers.add("promoted_text_banner_layout_three");
			blockedPromotedLayers.add("music_key_promo_feature_item_text");
			blockedPromotedLayers.add("generic_promo_card");
			blockedPromotedLayers.add("background_promo");
			blockedPromotedLayers.add("promoted_app_install_new_line_layout");

			blockedPromotedLayers.add("invideo_programming_overlay");
			blockedPromotedLayers.add("info_cards_teaser_overlay");
			
			blockedPromotedLayers.add("custom_debug_ad_break");
			blockedPromotedLayers.add("debug_offline_ad_video_entry");
			blockedPromotedLayers.add("debug_online_ads");
			blockedPromotedLayers.add("debug_offline_ad_layout");
			blockedPromotedLayers.add("debug_offline_ad_entry");

			for (String layer : blockedPromotedLayers) {
				try {
					resparam.res.hookLayout(resparam.packageName, "layout", layer, new XC_LayoutInflated() {

						@Override
						public void handleLayoutInflated(LayoutInflatedParam layoutInflatedParam) throws Throwable {
							Utils.hideLayoutContent(layoutInflatedParam);
						}
					});
				} catch (Throwable e) {

				}
			}
		} catch (Throwable e1) {
		}

		try {
			resparam.res.setReplacement(resparam.packageName, "bool", "show_startup_promo", false);
		} catch (Throwable e) {

		}

		try {
			resparam.res.setReplacement(resparam.packageName, "bool", "supports_rtl", true);
		} catch (Throwable e) {

		}

		try {
			resparam.res.setReplacement(resparam.packageName, "bool", "enable_channel_layer_banner", false);
		} catch (Throwable e) {

		}

		try {
			resparam.res.setReplacement(resparam.packageName, "bool", "generic_promo_banner_view", false);
		} catch (Throwable e) {

		}

	}

}