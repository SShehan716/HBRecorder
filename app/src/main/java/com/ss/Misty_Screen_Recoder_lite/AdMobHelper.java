package com.ss.Misty_Screen_Recoder_lite;

import android.app.Activity;
import android.content.Context;
import com.ss.Misty_Screen_Recoder_lite.LogUtils;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;

public class AdMobHelper {
    private static final String TAG = "AdMobHelper";
    
    // Ad unit ID (use test ID for development, real ID for production)
    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"; // Test rewarded ad unit ID for development
    // private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3318833775820712/2507463355"; // Real ad unit ID for production
    
    private RewardedAd rewardedAd;
    private boolean isLoadingRewarded = false;
    private boolean isSdkInitialized = false;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 1;
    
    // Callback for rewarded ad loading
    private CustomRewardedAdLoadCallback rewardedLoadCallback;
    
    public AdMobHelper() {
        // Don't initialize ads automatically - lazy loading
    }
    
    /**
     * Initialize AdMob SDK (called when first ad is needed)
     */
    private void initializeSdk(Context context) {
        if (!isSdkInitialized) {
            // SDK already initialized in MyApplication, just mark as ready
            isSdkInitialized = true;
            LogUtils.d(TAG, "AdMob SDK already initialized in Application");
        }
    }
    
    /**
     * Load rewarded ad with retry logic
     */
    public void loadRewardedAd(Context context, CustomRewardedAdLoadCallback callback) {
        if (isLoadingRewarded) {
            // Already loading, store callback
            rewardedLoadCallback = callback;
            return;
        }
        
        if (rewardedAd != null) {
            // Ad already loaded
            if (callback != null) {
                callback.onAdLoaded();
            }
            return;
        }
        
        initializeSdk(context);
        isLoadingRewarded = true;
        rewardedLoadCallback = callback;
        
        AdRequest adRequest = ConsentManager.getInstance(context).buildAdRequest(context);
        
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                        isLoadingRewarded = false;
                        retryCount = 0; // Reset retry count on success
                        LogUtils.d(TAG, "Rewarded ad loaded successfully");
                        
                        // Set full screen content callback
                        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                rewardedAd = null;
                                LogUtils.d(TAG, "Rewarded ad dismissed");
                                // Don't auto-load next ad to prevent invalid traffic
                            }
                            
                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                rewardedAd = null;
                                LogUtils.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                                // Don't auto-retry to prevent invalid traffic
                            }
                            
                            @Override
                            public void onAdShowedFullScreenContent() {
                                LogUtils.d(TAG, "Rewarded ad showed full screen content");
                            }
                        });
                        
                        if (rewardedLoadCallback != null) {
                            rewardedLoadCallback.onAdLoaded();
                            rewardedLoadCallback = null;
                        }
                    }
                    
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        rewardedAd = null;
                        isLoadingRewarded = false;
                        LogUtils.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                        
                        // Reduced retry logic to prevent invalid traffic
                        if (retryCount < 1) { // Only retry once instead of 3 times
                            retryCount++;
                            LogUtils.d(TAG, "Retrying ad load, attempt " + retryCount + "/1");
                            // Retry after a longer delay
                            new android.os.Handler().postDelayed(() -> {
                                loadRewardedAd(context, rewardedLoadCallback);
                            }, 5000); // 5 second delay instead of 2
                        } else {
                            retryCount = 0; // Reset for next time
                            if (rewardedLoadCallback != null) {
                                rewardedLoadCallback.onAdFailedToLoad(loadAdError.getMessage());
                                rewardedLoadCallback = null;
                            }
                        }
                    }
                });
    }
    
    /**
     * Show rewarded ad if available, otherwise load and show when ready
     */
    public void showRewardedAd(Activity activity, OnUserEarnedRewardListener rewardListener, Runnable onFailure) {
        // Ensure consent has been handled
        if (!ConsentManager.getInstance(activity).isConsentReady(activity)) {
            ConsentManager.getInstance(activity).requestConsentIfNeeded(activity, () -> showRewardedAd(activity, rewardListener, onFailure));
            return;
        }
        
        if (rewardedAd != null) {
            // Ad is ready, show it immediately
            rewardedAd.show(activity, rewardListener);
        } else {
            LogUtils.d(TAG, "Rewarded ad not ready, loading...");
            loadRewardedAd(activity, new CustomRewardedAdLoadCallback() {
                @Override
                public void onAdLoaded() {
                    if (rewardedAd != null) {
                        rewardedAd.show(activity, rewardListener);
                    } else {
                        LogUtils.e(TAG, "Ad loaded but rewardedAd is null");
                        if (onFailure != null) {
                            onFailure.run();
                        }
                    }
                }
                
                @Override
                public void onAdFailedToLoad(String error) {
                    LogUtils.e(TAG, "Rewarded ad failed to load: " + error);
                    if (onFailure != null) {
                        onFailure.run();
                    }
                }
            });
        }
    }
    
    /**
     * Show rewarded ad if available, otherwise load and show when ready (backward compatibility)
     */
    public void showRewardedAd(Activity activity, OnUserEarnedRewardListener rewardListener) {
        showRewardedAd(activity, rewardListener, null);
    }
    
    /**
     * Check if rewarded ad is ready to show
     */
    public boolean isRewardedAdReady() {
        return rewardedAd != null;
    }
    
    /**
     * Preload rewarded ads for better user experience
     */
    public void preloadAds(Context context) {
        if (rewardedAd == null && !isLoadingRewarded) {
            loadRewardedAd(context, null);
        }
    }
    
    /**
     * Clean up resources to prevent memory leaks
     */
    public void cleanup() {
        LogUtils.d(TAG, "Cleaning up AdMobHelper resources");
        
        // Clear rewarded ad
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(null);
            rewardedAd = null;
        }
        
        // Clear callbacks
        rewardedLoadCallback = null;
        
        // Reset state
        isLoadingRewarded = false;
        retryCount = 0;
        
        LogUtils.d(TAG, "AdMobHelper cleanup completed");
    }
    
    /**
     * Custom callback interface for rewarded ad loading
     */
    public interface CustomRewardedAdLoadCallback {
        void onAdLoaded();
        void onAdFailedToLoad(String error);
    }
} 