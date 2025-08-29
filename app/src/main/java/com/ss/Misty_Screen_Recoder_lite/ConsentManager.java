package com.ss.Misty_Screen_Recoder_lite;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.UserMessagingPlatform;

/**
 * Handles UMP consent and builds consent-aware AdRequest
 */
public class ConsentManager {
    private static final String KEY_CONSENT_READY = "consent_ready";
    private static final String KEY_NPA = "npa";

    private static ConsentManager instance;
    private ConsentInformation consentInformation;
    private boolean requesting = false;

    public static synchronized ConsentManager getInstance(Context context) {
        if (instance == null) {
            instance = new ConsentManager();
            instance.consentInformation = UserMessagingPlatform.getConsentInformation(context.getApplicationContext());
        }
        return instance;
    }

    public boolean isConsentReady(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getBoolean(KEY_CONSENT_READY, false);
    }

    @MainThread
    public void requestConsentIfNeeded(Activity activity, @Nullable Runnable onFinished) {
        if (requesting) {
            if (onFinished != null) onFinished.run();
            return;
        }

        requesting = true;
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, loadAndShowError -> {
                    // After form flow, store readiness
                    storeConsentState(activity.getApplicationContext());
                    requesting = false;
                    if (onFinished != null) onFinished.run();
                }),
                formError -> {
                    // On error still proceed; ads can be limited
                    storeConsentState(activity.getApplicationContext());
                    requesting = false;
                    if (onFinished != null) onFinished.run();
                }
        );
    }

    private void storeConsentState(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean consentObtained = consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.OBTAINED
                || consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.NOT_REQUIRED
                || consentInformation.canRequestAds();
        // If consented to personalized ads, clear NPA; otherwise set NPA=1
        boolean useNpa = consentInformation.getConsentStatus() == ConsentInformation.ConsentStatus.REQUIRED
                && !consentInformation.canRequestAds();
        prefs.edit()
                .putBoolean(KEY_CONSENT_READY, consentObtained || consentInformation.canRequestAds())
                .putBoolean(KEY_NPA, useNpa)
                .apply();
    }

    public AdRequest buildAdRequest(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        boolean npa = prefs.getBoolean(KEY_NPA, false);
        AdRequest.Builder builder = new AdRequest.Builder();
        if (npa) {
            android.os.Bundle extras = new android.os.Bundle();
            extras.putString("npa", "1");
            builder.addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter.class, extras);
        }
        return builder.build();
    }
}


