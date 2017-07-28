package ma.wanam.youtubeadaway;

import android.content.res.XModuleResources;
import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;

public class XYouTubeLayouts {

	private static final String[] blockedPromotedLayers = new String[] { "app_promo_ad_cta_overlay",
			"app_promotion_card", "background_promo", "compact_promoted_item", "compact_promoted_video_item",
			"default_promo_panel", "generic_promo_banner", "generic_promo_card", "interstitial_promo_view",
			"music_key_promo_feature_item", "music_key_promo_feature_item_text", "music_key_promo_small_feature_item",
			"promoted_15_click_pt_text_ctd_watch", "promoted_15_click_pt_text_watch",
			"promoted_15_click_text_ctd_watch", "promoted_15_click_text_watch", "promoted_app_install_new_line_layout",
			"promoted_app_install_right_align_layout", "promoted_app_install_wrapper", "promoted_native_text_ctd_home",
			"promoted_native_text_ctd_watch", "promoted_native_text_home", "promoted_native_text_watch",
			"promoted_short_descriptive_banner", "promoted_tall_descriptive_banner", "promoted_text_banner_layout_one",
			"promoted_text_banner_layout_three", "promoted_text_banner_layout_two", "promoted_text_banner_wrapper",
			"promoted_video_item", "promoted_video_item_consistent_attribution",
			"promoted_video_item_consistent_attribution_top_aligned", "promoted_video_item_land",
			"promoted_video_item_top_aligned", "share_panel_promo", "sign_in_promo", "watch_metadata_cards",
			"inline_ad_overlay", "default_ad_overlay", "embedded_ad_overlay", "mdx_ad_overlay", "info_card_shopping",
			"info_card_shopping_container_watch_next", "shopping_companion_product_view", "shopping_companion_card",
			"shopping_companion_card_white", "shopping_companion_card_grey", "info_card_shopping_watch_next",
			"invideo_programming_overlay", "info_cards_teaser_overlay", "custom_debug_ad_break",
			"debug_offline_ad_video_entry", "debug_online_ads", "debug_offline_ad_layout", "debug_offline_ad_entry" };

	public static void doHook(InitPackageResourcesParam resparam, XModuleResources moduleResources) {

		try {

			for (final String layer : blockedPromotedLayers) {
				try {
					resparam.res.hookLayout(resparam.packageName, "layout", layer, new XC_LayoutInflated() {

						@Override
						public void handleLayoutInflated(LayoutInflatedParam layoutInflatedParam) throws Throwable {
							try {

								ViewGroup vg = ((ViewGroup) layoutInflatedParam.view);
								int childCount = vg.getChildCount();
								if (childCount > 0) {
									for (int i = 0; i < childCount; i++) {
										try {
											vg.getChildAt(i).setVisibility(View.GONE);
										} catch (Throwable e) {
											XposedBridge.log(e);
										}
									}
								}
							} catch (Throwable e) {
								XposedBridge.log(e);
							} finally {
								layoutInflatedParam.view.setVisibility(View.GONE);
							}
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