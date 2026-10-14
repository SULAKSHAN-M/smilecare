# Smile Care Android | Patient + Doctor Edition

Native Android app built with Kotlin and Jetpack Compose.

## What is included

### Patient experience
- Patient sign in and local account registration
- Separate patient dashboard and navigation
- Book an appointment by service, date and available time
- Reschedule or cancel an appointment
- Pending / confirmed / completed / cancelled appointment states
- Doctor profile and clinic information
- Treatment history, clinical notes and prescriptions
- Notifications generated from booking and treatment actions
- Two-way patient ↔ doctor chat
- Editable patient profile

### Doctor experience
- Separate doctor dashboard and navigation
- Review pending booking requests
- Accept or decline requests
- View confirmed schedule
- Complete a treatment and add clinical notes / aftercare / prescription
- Patient record and visit history
- Manage bookable time slots; changes immediately affect patient booking
- Two-way patient ↔ doctor chat
- Editable doctor profile

## Persistent demo data
Appointments, chat messages, availability, profile data and notifications are stored locally with Android SharedPreferences/JSON, so changes remain after restarting the app on the same device.

This is a fully interactive single-device prototype. For real accounts shared across multiple phones, live clinic synchronization, push notifications, payments and secure medical-record storage, connect the same UI/workflow to a backend such as Firebase, Supabase or a REST API.

## Demo accounts

Patient:
- Email: `patient@smilecare.app`
- Password: `123456`

Doctor:
- Email: `doctor@smilecare.app`
- Password: `123456`

The login screen also has a **Fill demo account** button.

## HD imagery
The UI uses high-resolution remote images with Coil and keeps the original packaged images as offline/error fallbacks. Internet permission is included in the manifest.

HD stock images are loaded from Pexels:
- Dentist portrait: photo 31043313
- Dental treatment: photo 3881221
- Patient portrait: photo 20603310
- Smile banner: photo 7479536

## Requirements
- JDK 17
- Android SDK Platform 35
- Gradle 8.9
- Android Gradle Plugin 8.7.3

## Build from VS Code / PowerShell

From the project folder:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
```

If your Gradle wrapper is working:

```powershell
.\gradlew.bat clean
.\gradlew.bat assembleDebug
```

If you installed/extracted Gradle 8.9 manually at `C:\Gradle\gradle-8.9`:

```powershell
& "C:\Gradle\gradle-8.9\bin\gradle.bat" clean
& "C:\Gradle\gradle-8.9\bin\gradle.bat" assembleDebug
```

APK output:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Install to a connected emulator/device:

```powershell
adb devices
.\gradlew.bat installDebug
adb shell am start -n com.smilecare.app/.MainActivity
```
