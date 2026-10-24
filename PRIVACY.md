# Privacy Policy

**LibreCellar for Android**
Effective date: 23 October 2026

LibreCellar is an open source app for managing a wine cellar. It works with a [LibreCellar server](https://github.com/sim0nx/LibreCellar_backend)
that **you** run on your own hardware. There is no LibreCellar cloud service: the developer does
not operate any server for the app and does not receive, collect or store any of your data.

## Summary

- The app has no analytics, advertising, crash reporting or tracking.
- Your cellar data is stored on your own server, not by the developer.
- Photos and barcode scans are processed on your phone.
- Google ML Kit, which the app uses for scanning and label reading, sends some technical usage
  data to Google (see below).

## Data stored on your server

When you use the app, it sends the following to the server address you enter, and only to that
address:

- your login (username and password, used once to obtain a login token)
- your cellar data: wines, quantities, prices, ratings, notes, cellar layout and merchants
- bottle and label photos you add

This data is controlled by whoever runs the server, usually you or your household. The developer
has no access to it. To delete your data or your account, delete it on your server, for example
in the server's web app or by asking its administrator.

Use an `https://` server address whenever possible. With a plain `http://` address, data between
the app and your server is not encrypted, which matters especially outside your home network.

## Data stored on your phone

- **Settings:** server address, username, display and filter settings and your grape list.
- **Login token:** encrypted with a key held in the Android Keystore. Your password is not stored.
- **Photo cache:** copies of bottle and label photos, so they don't need to be downloaded again.

All of this is deleted when you uninstall the app or clear its storage.

**Android backup:** if Google backup is turned on for your phone, Android may include the app's
settings in your backup. Bottle and label photos are excluded. The login token can't be decrypted
on another device, so you will have to log in again after a restore.

## Permissions

- **Camera** (optional): to scan barcodes and take photos of bottles and labels. Without it,
  barcodes can be typed in and photos picked from the gallery.
- **Internet and local network access:** to connect to your server.

## Google ML Kit

Barcode scanning and label reading use Google ML Kit with models included in the app. The images
and the text read from them are processed on your phone and are not sent to Google.

According to Google, ML Kit sends technical information to Google, such as device and app
information, API performance metrics and error codes, and may download model updates. Google uses
this to maintain the service and detect misuse. This is described in the
[ML Kit terms](https://developers.google.com/ml-kit/terms) and the
[ML Kit data disclosure](https://developers.google.com/ml-kit/android-data-disclosure), and is
governed by the [Google Privacy Policy](https://policies.google.com/privacy).

## Google Play

If you install the app from Google Play, Google handles the download and updates under its own
terms and privacy policy. Through the Google Play Console, the developer sees what Google Play
reports for every app: aggregated statistics such as install counts, and crash and performance
reports if you allow your phone to share usage and diagnostics with Google. These don't identify
you to the developer.

## Sharing and selling

The developer does not collect your data, so it is never shared with or sold to anyone.

## Children

The app is not directed at children.

## Changes

Changes to this policy are published in this file. Its history is visible in the
[git log](https://github.com/sim0nx/LibreCellar/commits/main/PRIVACY.md).

## Contact

Questions about this policy: open an issue at
[github.com/sim0nx/LibreCellar](https://github.com/sim0nx/LibreCellar/issues) or email
android-market@trypill.org.
