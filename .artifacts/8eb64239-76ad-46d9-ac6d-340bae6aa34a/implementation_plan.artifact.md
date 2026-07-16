# Implementation Plan - Resolve Advertising ID Declaration Conflict

The user is experiencing an error in the Google Play Console regarding the "Advertising ID" declaration. Although the user set the declaration to "No", the Play Console indicates an issue. Research reveals that the project's **Privacy Policy** specifically mentions Google AdMob and the collection of Advertising IDs, creating a conflict with the "No ads" declaration.

## User Review Required

> [!IMPORTANT]
> The current **Privacy Policy** (`privacy_policy.html`) explicitly states that the app uses **Google AdMob** and collects **Advertising IDs** (Sections 2.2, 2.3, 3.3, and 4.1).
>
> If you intend to release the app **without ads**, we must remove these mentions from the Privacy Policy. If you intended to **have ads**, we need to add the missing SDKs and permissions to the code.

## Proposed Changes

### Documentation & Compliance

#### [MODIFY] [privacy_policy.html](file:///Users/sachinshehan/Documents/Projects/Android Studio- Projects/Misty-Screen-Recoder(lite)/privacy_policy.html)
- Remove all references to **Google AdMob**.
- Remove mentions of **Advertising ID** collection.
- Remove permissions related to ads (INTERNET, ACCESS_NETWORK_STATE) if they are listed as "for ads".
- Update Section 12.2 (Data Safety) to explicitly state that no data is shared for advertising.

#### [MODIFY] [strings.xml](file:///Users/sachinshehan/Documents/Projects/Android Studio- Projects/Misty-Screen-Recoder(lite)/app/src/main/res/values/strings.xml)
- Check if any string used in the Store Listing (like app description) mentions ads and remove it.

## Verification Plan

### Manual Verification
1. Review the updated `privacy_policy.html` to ensure all ad-related terminology is removed.
2. The user will need to re-upload the updated Privacy Policy to their hosting/Play Console and then "Fix" the issue in the Play Console by re-submitting the declaration.
