package com.ss.Misty_Screen_Recoder_lite;

import android.app.Activity;
import android.content.Context;
import com.ss.Misty_Screen_Recoder_lite.LogUtils;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

public class AdMobHelper {
    private static final String TAG = "AdMobHelper";
    
    // Ad unit ID (use test ID for development, real ID for production)
    // private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // Test ad unit ID for development
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3318833775820712/8413516873"; // Real ad unit ID for production
    
    private InterstitialAd interstitialAd;
    private boolean isLoadingInterstitial = false;
    private boolean isSdkInitialized = false;
    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 3;
    
    // Callback for lazy loading
    private AdLoadCallback interstitialLoadCallback;
    
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
                // Auto-load first ad after SDK initialization
                loadInterstitialAd(context, null);
            });
        }
    }
    
    /**
     * Load interstitial ad with retry logic
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
                        retryCount = 0; // Reset retry count on success
                        LogUtils.d(TAG, "Interstitial ad loaded successfully");
                        
                        // Set full screen content callback
                        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                interstitialAd = null;
                                LogUtils.d(TAG, "Interstitial ad dismissed");
                                // Load next ad automatically after a short delay
                                loadInterstitialAd(context, null);
                            }
                            
                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                interstitialAd = null;
                                LogUtils.e(TAG, "Interstitial ad failed to show: " + adError.getMessage());
                                // Retry loading after failure
                                loadInterstitialAd(context, null);
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
                        
                        // Retry logic for failed loads
                        if (retryCount < MAX_RETRY_COUNT) {
                            retryCount++;
                            LogUtils.d(TAG, "Retrying ad load, attempt " + retryCount + "/" + MAX_RETRY_COUNT);
                            // Retry after a short delay
                            new android.os.Handler().postDelayed(() -> {
                                loadInterstitialAd(context, interstitialLoadCallback);
                            }, 2000); // 2 second delay
                        } else {
                            retryCount = 0; // Reset for next time
                            if (interstitialLoadCallback != null) {
                                interstitialLoadCallback.onAdFailedToLoad(loadAdError.getMessage());
                                interstitialLoadCallback = null;
                            }
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
                    LogUtils.e(TAG, "Failed to load ad for showing: " + error);
                    if (callback != null) {
                        callback.onAdFailedToLoad(error);
                    }
                }
            });
        }
    }
    
    /**
     * Check if interstitial ad is ready to show
     */
    public boolean isInterstitialAdReady() {
        return interstitialAd != null;
    }
    
    /**
     * Preload ads for better user experience
     */
    public void preloadAds(Context context) {
        // Load interstitial ad in background
        if (interstitialAd == null && !isLoadingInterstitial) {
            loadInterstitialAd(context, null);
        }
    }
    
    /**
     * Callback interface for ad loading
     */
    public interface AdLoadCallback {
        void onAdLoaded();
        void onAdFailedToLoad(String error);
    }
} 