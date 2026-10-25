# Play Console answers for com.linkinxp.terminal (draft; check each against the form)

**App category**: Personalization. **Tags**: launcher, customization. **Contact email / website**: set in the Console.
**Privacy policy URL**: https://linkinxp.github.io/Terminal-Launcher/privacy/

## App content
- **Ads**: no. **App access**: everything works without login or special access (notification access, contacts and calendar are optional).
- **Target audience**: 18 and over (not a children's app). **Content rating questionnaire**: no violence, sexual content, language, controlled substances, gambling, user-generated content or location sharing: the result should be "Everyone".
- **Government / financial / health / news apps**: no.

## Data safety
- **Collected and shared**: nothing is sent to the developer. No data is shared with third parties.
- Optional weather: coordinates of the city the user types go to Open-Meteo to get a forecast, in transit only and not stored by the app's developer. Declare conservatively: Location > Approximate location, collected, not shared, optional, purpose "App functionality", encrypted in transit (HTTPS), not stored by the developer.
- Contacts, calendar, notifications and installed apps are read on the device and never transmitted.
- Users can delete their data by clearing app storage or uninstalling.

## Permission declarations
- `REQUEST_DELETE_PACKAGES`: the app menu has an "uninstall" entry that starts Android's uninstall dialog for the selected app (a launcher feature).
- `READ_CONTACTS`: drawer search shows matching contact names; on-device only; runtime permission requested only when the user taps "allow contacts".
- `READ_CALENDAR`: calendar panel; on-device only; runtime permission.
- `BIND_NOTIFICATION_LISTENER_SERVICE` (notification access): icon badges (counts only) and the now-playing panel; optional; the user grants it in system settings; text of notifications is never read.
- `EXPAND_STATUS_BAR`: swipe-down gestures open the notification shade and quick settings.
- `INTERNET`, `ACCESS_NETWORK_STATE`: optional weather panel and the network type in the system-stats panel.
- Home screen (launcher) app: the activity declares category HOME.
- The store build contains **no accessibility service** (the debug build has an optional screen-lock one).
