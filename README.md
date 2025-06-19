# SMS Forwarder Android App

A minimal-UI Android app that monitors SMS and notifications in the background, forwarding matching messages to user-defined HTTP APIs based on customizable rules.

## Features

🔄 **Background SMS & Notification Processing** - Silently captures incoming SMS and app notifications  
📋 **Pattern-Based Rules** - Regex or substring matching against message content  
🌐 **HTTP API Forwarding** - POST/GET/PUT/PATCH to any HTTP endpoint  
📊 **Custom Headers** - Configurable headers for each forwarding rule  
📈 **History Tracking** - Paginated logs with filtering and search capabilities  
🔄 **Auto-Start** - Automatically starts monitoring on device boot  
⚡ **Battery Optimized** - Uses foreground service with minimal notifications  
🎯 **Smart Filtering** - Filter by app, search content, or use pattern matching  
📱 **Modern UI** - Clean Material 3 design with swipe gestures  

## Architecture

- **Clean Architecture** (MVVM + Repository Pattern)
- **Jetpack Compose** for modern UI
- **Room Database** for local persistence with migrations
- **Retrofit + OkHttp** for HTTP networking
- **Hilt** for dependency injection
- **WorkManager** for background tasks
- **Coroutines + Flow** for reactive programming

## Technical Requirements

- **Target SDK**: 34 (Android 14)
- **Minimum SDK**: 29 (Android 10)
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL

## Permissions

### Required Permissions
- `RECEIVE_SMS` - Receive incoming SMS messages
- `READ_SMS` - Read SMS content for processing
- `INTERNET` - Send HTTP requests to configured endpoints
- `RECEIVE_BOOT_COMPLETED` - Auto-start on device boot
- `FOREGROUND_SERVICE` - Run background service reliably
- `WAKE_LOCK` - Keep device awake during processing

### Protected Permissions
- `BIND_NOTIFICATION_LISTENER_SERVICE` - Monitor app notifications
- `PACKAGE_USAGE_STATS` - Access app usage for smart app picker

## Project Structure

```
app/src/main/java/com/zerodev/smsforwarder/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # HTTP client and API services
│   ├── repository/     # Repository implementations
│   ├── receiver/       # BroadcastReceivers (SMS, boot)
│   ├── service/        # Foreground services
│   └── worker/         # WorkManager workers
├── domain/
│   ├── model/          # Domain models
│   └── usecase/        # Business logic
├── di/                 # Dependency injection modules
└── ui/                 # Jetpack Compose UI components
```

## Key Components

### Message Processing Flow
1. **SmsReceiver** / **NotifRouterService** - Intercept SMS/notifications
2. **SmsForwardingService** - Process messages in foreground service
3. **SmsForwardingUseCase** - Rule matching and HTTP forwarding
4. **HttpClient** - HTTP requests with retry logic
5. **HistoryRepository** - Log all forwarding attempts

### Database Schema
- **Rules**: ID, name, source type, pattern, endpoint, method, headers
- **History**: ID, ruleId, message data, request/response, status, timestamps

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 34
- Kotlin 1.9+

### Build Instructions
```bash
# Clone repository
git clone <repository-url>
cd SMSForwarder

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Run lint checks
./gradlew lint
```

## CI/CD Workflows

### Automated Build & Release
- **Triggers**: Push to main/develop, PRs, tags, commits starting with "release:"
- **Features**: Builds APKs, runs tests, security scans, creates releases
- **Outputs**: Debug/Release APKs, lint reports, security analysis

### Creating Releases
```bash
# Option 1: Release commit (recommended)
git commit -m "release: Add notification support and UI improvements"
git push origin main

# Option 2: Git tag
git tag v1.0.0 && git push origin v1.0.0
```

## Development Features

### Debugging Tools
- **Comprehensive History**: All received messages logged
- **Visual Indicators**: Success/failure status for each forwarding attempt
- **Search & Filtering**: Find specific messages or apps quickly
- **Statistics Dashboard**: Success rates and processing metrics
- **Swipe Actions**: Intuitive gesture-based deletion

### Performance Optimizations
- **Pagination**: Load history in chunks (15 items per page)
- **Efficient Queries**: Optimized database queries with proper indexing
- **Background Processing**: WorkManager for reliable background execution
- **Memory Management**: Proper lifecycle management and resource cleanup

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Security & Support

- **Security**: Regular vulnerability scans, unsigned APKs in releases
- **Documentation**: Comprehensive development logs in `/development_logs/`
- **Issues**: Report bugs and feature requests on GitHub
- **Development**: Check debug instructions in the Settings tab 