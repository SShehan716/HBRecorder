package com.ss.Misty_Screen_Recoder_lite;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import com.ss.Misty_Screen_Recoder_lite.LogUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdMobHelper {
    private static final String TAG = "AdMobHelper";
    
    // Test ad unit IDs (replace with your real ad unit IDs for production)
    private static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
    
    private InterstitialAd interstitialAd;
    private RewardedAd rewardedAd;
    private boolean isLoadingInterstitial = false;
    private boolean isLoadingRewarded = false;
    private boolean isSdkInitialized = false;
    
    // Callbacks for lazy loading
    private AdLoadCallback interstitialLoadCallback;
    private AdLoadCallback rewardedLoadCallback;
    
    public AdMobHelper() {
        // Don't initialize ads automatically - lazy loading
    }
    
    /**
     * Initialize AdMob SDK (called when first ad is needed)
     */
    private void initializeSdk(Context context) {
        if (!isSdkInitialized) {
            MobileAds.initialize(context, initializationStatus -> {
                isSdkInitialized = true;
                LogUtils.d(TAG, "AdMob SDK initialized");
            });
        }
    }
    
    /**
     * Load banner ad (immediate loading for UI)
     */
    public void loadBannerAd(Context context, ViewGroup adContainer) {
        initializeSdk(context);
        
        AdView adView = new AdView(context);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId(BANNER_AD_UNIT_ID);
        
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);
        
        // Add ad view to container
        if (adContainer instanceof LinearLayout) {
            ((LinearLayout) adContainer).addView(adView);
        } else {
            adContainer.addView(adView);
        }
    }
    
    /**
     * Load interstitial ad lazily (only when needed)
     */
    public void loadInterstitialAd(Context context, AdLoadCallback callback) {
        if (isLoadingInterstitial) {
            // Already loading, store callback
            interstitialLoadCallback = callback;
            return;
        }
        
        if (interstitialAd != null) {
            // Ad already loaded
            if (callback != null) {
                callback.onAdLoaded();
            }
            return;
        }
        
        initializeSdk(context);
        isLoadingInterstitial = true;
        interstitialLoadCallback = callback;
        
        AdRequest adRequest = new AdRequest.Builder().build();
        
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                        isLoadingInterstitial = false;
                        LogUtils.d(TAG, "Interstitial ad loaded");
                        
                        // Set full screen content callback
                        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                interstitialAd = null;
                                LogUtils.d(TAG, "Interstitial ad dismissed");
                                // Load next ad automatically
                                loadInterstitialAd(context, null);
                            }
                            
                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                interstitialAd = null;
                                LogUtils.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                            }
                            
                            @Override
                            public void onAdShowedFullScreenContent() {
                                LogUtils.d(TAG, "Interstitial ad showed full screen content");
                            }
                        });
                        
                        if (interstitialLoadCallback != null) {
                            interstitialLoadCallback.onAdLoaded();
                            interstitialLoadCallback = null;
                        }
                    }
                    
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        interstitialAd = null;
                        isLoadingInterstitial = false;
                        LogUtils.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                        
                        if (interstitialLoadCallback != null) {
                            interstitialLoadCallback.onAdFailedToLoad(loadAdError.getMessage());
                            interstitialLoadCallback = null;
                        }
                    }
                });
    }
    
    /**
     * Show interstitial ad if available, otherwise load and show when ready
     */
    public void showInterstitialAd(Activity activity, AdLoadCallback callback) {
        if (interstitialAd != null) {
            interstitialAd.show(activity);
            if (callback != null) {
                callback.onAdLoaded();
            }
        } else {
            LogUtils.d(TAG, "Interstitial ad not ready, loading...");
            loadInterstitialAd(activity, new AdLoadCallback() {
                @Override
                public void onAdLoaded() {
                    if (interstitialAd != null) {
                        interstitialAd.show(activity);
                    }
                    if (callback != null) {
                        callback.onAdLoaded();
                    }
                }
                
                @Override
                public void onAdFailedToLoad(String error) {
                    if (callback != null) {
                        callback.onAdFailedToLoad(error);
                    }
                }
            });
        }
    }
    
    /**
     * Load rewarded ad lazily (only when needed)
     */
    public void loadRewardedAd(Context context, AdLoadCallback callback) {
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
        
        AdRequest adRequest = new AdRequest.Builder().build();
        
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                        isLoadingRewarded = false;
                        LogUtils.d(TAG, "Rewarded ad loaded");
                        
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
                        
                        if (rewardedLoadCallback != null) {
                            rewardedLoadCallback.onAdFailedToLoad(loadAdError.getMessage());
                            rewardedLoadCallback = null;
                        }
                    }
                });
    }
    
    /**
     * Show rewarded ad if available, otherwise load and show when ready
     */
    public void showRewardedAd(Activity activity, RewardedAdCallback callback) {
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                                            public void onAdDismissedFullScreenContent() {
                                rewardedAd = null;
                                LogUtils.d(TAG, "Rewarded ad dismissed");
                                // Load next ad automatically
                                loadRewardedAd(activity, null);
                            }
                
                @Override
                                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                rewardedAd = null;
                                LogUtils.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                            }
                
                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad showed full screen content");
                }
            });
            
            rewardedAd.show(activity, rewardItem -> {
                LogUtils.d(TAG, "User earned reward: " + rewardItem.getAmount());
                if (callback != null) {
                    callback.onRewarded();
                }
            });
        } else {
            LogUtils.d(TAG, "Rewarded ad not ready, loading...");
            loadRewardedAd(activity, new AdLoadCallback() {
                @Override
                public void onAdLoaded() {
                    if (rewardedAd != null) {
                        rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                rewardedAd = null;
                                LogUtils.d(TAG, "Rewarded ad dismissed");
                                loadRewardedAd(activity, null);
                            }
                            
                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                rewardedAd = null;
                                LogUtils.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                            }
                            
                            @Override
                            public void onAdShowedFullScreenContent() {
                                LogUtils.d(TAG, "Rewarded ad showed full screen content");
                            }
                        });
                        
                        rewardedAd.show(activity, rewardItem -> {
                            Log.d(TAG, "User earned reward: " + rewardItem.getAmount());
                            if (callback != null) {
                                callback.onRewarded();
                            }
                        });
                    } else {
                        if (callback != null) {
                            callback.onAdNotAvailable();
                        }
                    }
                }
                
                @Override
                public void onAdFailedToLoad(String error) {
                    if (callback != null) {
                        callback.onAdNotAvailable();
                    }
                }
            });
        }
    }
    
    /**
     * Check if interstitial ad is ready
     */
    public boolean isInterstitialAdReady() {
        return interstitialAd != null;
    }
    
    /**
     * Check if rewarded ad is ready
     */
    public boolean isRewardedAdReady() {
        return rewardedAd != null;
    }
    
    /**
     * Preload ads for better user experience (call this when app is idle)
     */
    public void preloadAds(Context context) {
        if (!isSdkInitialized) {
            initializeSdk(context);
        }
        
        // Preload interstitial ad
        if (!isLoadingInterstitial && interstitialAd == null) {
            loadInterstitialAd(context, null);
        }
        
        // Preload rewarded ad
        if (!isLoadingRewarded && rewardedAd == null) {
            loadRewardedAd(context, null);
        }
    }
    
    /**
     * Callback interface for ad loading
     */
    public interface AdLoadCallback {
        void onAdLoaded();
        void onAdFailedToLoad(String error);
    }
    
    /**
     * Callback interface for rewarded ads
     */
    public interface RewardedAdCallback {
        void onRewarded();
        void onAdNotAvailable();
    }
} 