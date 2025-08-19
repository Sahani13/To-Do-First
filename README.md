# 📝 MyToDo - Smart Task Management App

<div align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green.svg" alt="Platform" />
  <img src="https://img.shields.io/badge/Language-Java-orange.svg" alt="Language" />
  <img src="https://img.shields.io/badge/API-24+-brightgreen.svg" alt="API" />
  <img src="https://img.shields.io/badge/Version-1.0-blue.svg" alt="Version" />
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License" />
</div>

## 🚀 Overview

MyToDo is a comprehensive task management Android application that combines traditional to-do functionality with modern features like location-based reminders, note-taking, and sensor integration. Built with Java and Firebase, it offers a seamless user experience for managing tasks, notes, and location-based activities.

## ✨ Key Features

### 📋 Task Management
- ✅ Create, edit, and delete tasks
- 🏷️ Categorize tasks with custom labels
- ⏰ Set reminders and due dates
- ✔️ Mark tasks as complete
- 📊 Track task progress

### 📍 Location-Based Features
- 🗺️ Interactive map integration
- 📍 Location-based task reminders
- 🔔 Proximity notifications
- 💾 Save favorite locations
- 🎯 Current location detection

### 📝 Note Taking
- 📄 Create and manage personal notes
- ✏️ Rich text editing
- 🗂️ Organize notes efficiently
- 🔍 Search through notes

### 👤 User Management
- 🔐 Secure Firebase authentication
- 📧 Email verification
- 🔑 Password reset functionality
- 👥 User profile management
- 📊 Personal statistics dashboard

### 🎨 Modern UI/UX
- 🌙 Dark/Light theme support
- 📱 Material Design principles
- 🎥 Interactive tutorial with video content
- 📷 Camera integration
- 🔄 Smooth animations

### 🔔 Smart Notifications
- 📱 Real-time notifications
- 📍 Location-triggered alerts
- 🔊 Audio notifications
- ⚡ Background service integration

## 🛠️ Technical Stack

### Core Technologies
- **Language**: Java
- **Platform**: Android (API 24+)
- **IDE**: Android Studio
- **Build System**: Gradle (Kotlin DSL)

### Libraries & Dependencies
- **Firebase Authentication** - User management
- **Material Design Components** - Modern UI
- **RecyclerView** - Efficient list displays
- **Google Play Services** - Location services
- **AndroidX Libraries** - Modern Android components

### Database
- **SQLite** - Local data storage with custom DatabaseHelper

## 📱 App Architecture

```
MyToDo/
├── 🏠 MainActivity - Welcome screen with authentication
├── 👤 User Management
│   ├── LoginPage - User login
│   ├── SignupPage - User registration
│   ├── ForgotPassword - Password recovery
│   └── AccountPage - Profile management
├── 📋 Task Management
│   ├── HomePage - Dashboard
│   ├── TaskListPage - View all tasks
│   ├── AddTaskPage - Create new tasks
│   └── SimpleAddTaskPage - Quick task creation
├── 📝 Notes
│   ├── NotesPage - View all notes
│   └── AddNotePage - Create new notes
├── 🗺️ Location Features
│   ├── MapPage - Interactive map
│   └── SavedPlacePage - Manage saved locations
├── 🎓 TutorialPage - App tutorial with video
└── 🔧 Services
    ├── LocationNotificationService - Background location monitoring
    └── SensorManagerHelper - Sensor integration
```

## 🏗️ Installation & Setup

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK (API 24+)
- Firebase project setup
- Google Maps API key

### Getting Started

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/MyToDo.git
   cd MyToDo
   ```

2. **Firebase Setup**
   - Create a new Firebase project
   - Enable Authentication (Email/Password)
   - Download `google-services.json` and place it in `app/` directory

3. **Configure API Keys**
   - Add your Google Maps API key to `AndroidManifest.xml`
   - Configure Firebase authentication settings

4. **Build and Run**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

## 🎯 Usage

### Quick Start
1. **Launch the app** and complete the interactive tutorial
2. **Sign up** for a new account or **login** with existing credentials
3. **Create your first task** from the home dashboard
4. **Set location reminders** for location-based tasks
5. **Organize notes** for additional information

### Advanced Features
- **Location Tasks**: Create tasks that notify you when you reach specific locations
- **Smart Reminders**: Get contextual notifications based on your location and time
- **Data Analytics**: View your productivity statistics in the account page
- **Theme Customization**: Switch between light and dark themes

## 🔧 Permissions

The app requires the following permissions for optimal functionality:

- **📍 Location** - For location-based reminders and map features
- **📷 Camera** - For capturing images with tasks/notes
- **💾 Storage** - For saving images and data
- **🔔 Notifications** - For task reminders and alerts
- **🌐 Internet** - For Firebase authentication and services

## 🎨 Screenshots

*Add your app screenshots here to showcase the UI*

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Guidelines
- Follow Java coding conventions
- Write meaningful commit messages
- Test your changes thoroughly
- Update documentation when necessary

## 🐛 Known Issues & Fixes

- **Location Notifications**: Ensure background location permission is granted
- **Video Playback**: Videos in tutorial require proper codec support
- **Theme Switching**: May require app restart for full effect

## 📈 Roadmap

- [ ] 🔄 Task synchronization across devices
- [ ] 🏷️ Advanced tagging system
- [ ] 📊 Enhanced analytics dashboard
- [ ] 🔗 Third-party calendar integration
- [ ] 🎨 Custom theme creation
- [ ] 🗣️ Voice note recording
- [ ] 📤 Export/Import functionality

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Student ID**: s22010514

## 🙏 Acknowledgments

- Firebase team for excellent authentication services
- Google for Material Design guidelines
- Android development community for continuous support

## 📞 Support

If you encounter any issues or have questions:
- 📧 Email: [your-email@example.com]
- 🐛 Report bugs via GitHub Issues
- 💬 Join our community discussions

---

<div align="center">
  <strong>Built with ❤️ for better productivity</strong>
</div>
