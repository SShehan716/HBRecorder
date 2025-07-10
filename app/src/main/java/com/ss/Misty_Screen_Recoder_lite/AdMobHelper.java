package com.ss.Misty_Screen_Recoder_lite;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
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
    
    public AdMobHelper() {
        // Initialize MobileAds SDK
        MobileAds.initialize(MyApplication.getInstance(), initializationStatus -> {
            Log.d(TAG, "AdMob SDK initialized");
        });
    }
    
    /**
     * Load banner ad
     */
    public void loadBannerAd(Context context, ViewGroup adContainer) {
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
     * Load interstitial ad
     */
    public void loadInterstitialAd(Context context) {
        if (isLoadingInterstitial) return;
        
        isLoadingInterstitial = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                        isLoadingInterstitial = false;
                        Log.d(TAG, "Interstitial ad loaded");
                        
                        // Set full screen content callback
                        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                interstitialAd = null;
                                Log.d(TAG, "Interstitial ad dismissed");
                            }
                            
                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                interstitialAd = null;
                                Log.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                            }
                            
                            @Override
                            public void onAdShowedFullScreenContent() {
                                Log.d(TAG, "Interstitial ad showed full screen content");
                            }
                        });
                    }
                    
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        interstitialAd = null;
                        isLoadingInterstitial = false;
                        Log.e(TAG, "Interstitial ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }
    
    /**
     * Show interstitial ad if available
     */
    public void showInterstitialAd(Activity activity) {
        if (interstitialAd != null) {
            interstitialAd.show(activity);
        } else {
            Log.d(TAG, "Interstitial ad not ready yet");
            // Load a new ad for next time
            loadInterstitialAd(activity);
        }
    }
    
    /**
     * Load rewarded ad
     */
    public void loadRewardedAd(Context context) {
        if (isLoadingRewarded) return;
        
        isLoadingRewarded = true;
        AdRequest adRequest = new AdRequest.Builder().build();
        
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest,
                new RewardedAdLoadCallback() {
                    @Override
                    public void onAdLoaded(RewardedAd ad) {
                        rewardedAd = ad;
                        isLoadingRewarded = false;
                        Log.d(TAG, "Rewarded ad loaded");
                    }
                    
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        rewardedAd = null;
                        isLoadingRewarded = false;
                        Log.e(TAG, "Rewarded ad failed to load: " + loadAdError.getMessage());
                    }
                });
    }
    
    /**
     * Show rewarded ad if available
     */
    public void showRewardedAd(Activity activity, RewardedAdCallback callback) {
        if (rewardedAd != null) {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    rewardedAd = null;
                    Log.d(TAG, "Rewarded ad dismissed");
                }
                
                @Override
                public void onAdFailedToShowFullScreenContent(AdError adError) {
                    rewardedAd = null;
                    Log.e(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                }
                
                @Override
                public void onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded ad showed full screen content");
                }
            });
            
            rewardedAd.show(activity, rewardItem -> {
                Log.d(TAG, "User earned reward: " + rewardItem.getAmount());
                if (callback != null) {
                    callback.onRewarded();
                }
            });
        } else {
            Log.d(TAG, "Rewarded ad not ready yet");
            if (callback != null) {
                callback.onAdNotAvailable();
            }
            // Load a new ad for next time
            loadRewardedAd(activity);
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
     * Callback interface for rewarded ads
     */
    public interface RewardedAdCallback {
        void onRewarded();
        void onAdNotAvailable();
    }
} 