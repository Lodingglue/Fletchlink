# FletchLink

**FletchLink** is an Android application built with Jetpack Compose and Kotlin to manage Minecraft Bedrock Edition Realms and account information.  
It provides a user-friendly interface for authenticating with a Microsoft account, viewing and joining Realms, and accessing account details.

---

## Features

- **Microsoft Authentication**: Securely sign in using Microsoft's device code authentication flow.  
- **Realms Management**: View, join, and leave Minecraft Realms, with details like server status, player count, and world type.  
- **Account Information**: Display user details such as display name, Xbox User ID (XUID), and public ID.  
- **Secure Session Handling**: Persist and refresh authentication sessions for seamless user experience.  
- **Modern UI**: Built with Jetpack Compose, featuring a clean, responsive design with Material 3 components.

---

## Getting Started

### Prerequisites

- Android Studio (latest stable version recommended)  
- Android device or emulator running Android 7.0 (API 24) or higher  
- Minecraft Bedrock Edition account with Microsoft authentication  
- Internet connection for authentication and Realms access  

---

### Installation

#### Clone the Repository:
```bash
git clone https://github.com/yourusername/fletchlink.git
```

#### Open in Android Studio:
- Open Android Studio and select **Open an existing project**.  
- Navigate to the cloned `fletchlink` directory.  

#### Build and Run:
- Sync the project with Gradle.  
- Connect an Android device or start an emulator.  
- Click **Run** to build and install the app.

---

## Usage

### Sign In:
- Launch the app and click **"Continue with Microsoft"**.  
- Copy the provided user code and visit the verification URL in a browser.  
- Enter the code to authenticate your Minecraft account.  

### Home Screen:
- View your account details (display name, XUID, public ID).  
- See the number of Realms you're part of.  
- Log out if needed.  

### Realms Screen:
- Browse your Realms with details like name, owner, status, and version.  
- Join a Realm, copy its server address, or leave it.  
- Join new Realms using an invite code.  
- View detailed Realm information in a dialog.

---

## Project Structure

```
application/       -> Core app logic (AuthActivity.kt, SessionUtils.kt, MainActivity.kt)
ui/
  └── screens/     -> Composable screens (HomeScreen.kt, RealmScreen.kt)
  └── components/  -> Reusable components (RealmCard.kt, etc.)
  └── theme/       -> Material 3 theme config
```

---

## Dependencies

- **Jetpack Compose**: For modern Android UI development.  
- **MinecraftAuth**: Library for Minecraft Bedrock authentication and Realms API integration.  
- **Kotlin Coroutines**: For asynchronous operations like network requests.  
- **Gson**: For JSON serialization/deserialization of session data.  

---

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.  
2. Create a feature branch:  
   ```bash
   git checkout -b feature/your-feature
   ```
3. Commit your changes:  
   ```bash
   git commit -m "Add your feature"
   ```
4. Push to the branch:  
   ```bash
   git push origin feature/your-feature
   ```
5. Open a pull request.

---

## License

This project is licensed under the **MIT License**. See the `LICENSE` file for details.

---

## Acknowledgments

- **MinecraftAuth by RaphiMC** for authentication and Realms API support.  
- **Jetpack Compose** for the UI framework.  
- **Tabler Icons** for icon resources.
