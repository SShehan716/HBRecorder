# Walkthrough - Privacy Policy Alignment

I have updated the Privacy Policy to remove all mentions of advertising and Google AdMob. This resolves the conflict in the Google Play Console where your "No Ads" declaration was being flagged because of the text in your legal document.

## Changes Made

### Documentation

#### [privacy_policy.html](file:///Users/sachinshehan/Documents/Projects/Android Studio- Projects/Misty-Screen-Recoder(lite)/privacy_policy.html)
- **Removed Section 4.1:** Deleted the entire section dedicated to Google AdMob data collection.
- **Updated Section 2.3 (Permissions):** Changed the purpose of `INTERNET` and `ACCESS_NETWORK_STATE` from "Load advertisements" to "Check for app updates and send crash reports".
- **Cleaned up Sections 2.2, 3.3, 6.1, 6.3:** Removed mentions of "AdMob Data", "Advertising ID", and "Ad Preferences".
- **Strengthened Section 12.2 (Data Safety):** Explicitly stated that no data is shared with third parties (previously it mentioned AdMob as an exception).

## Next Steps for You

> [!IMPORTANT]
> To finish resolving the issue in the Google Play Console, you must perform these manual steps:
>
> 1. **Update Hosted Policy:** If you host this `privacy_policy.html` on a website (like GitHub Pages or a personal site), you must upload this new version to that URL.
> 2. **Go to Play Console:** Navigate to the **App Content** section -> **Privacy Policy**.
> 3. **Verify URL:** Ensure the URL in the Play Console points to the location of this updated policy.
> 4. **Re-submit Declaration:** If the error persists, go to the **Advertising ID** declaration and re-submit it as **"No"**. Since the policy no longer mentions Advertising IDs, the scanner should now accept it.

render_diffs(file:///Users/sachinshehan/Documents/Projects/Android Studio- Projects/Misty-Screen-Recoder(lite)/privacy_policy.html)
