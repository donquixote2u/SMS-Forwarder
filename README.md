# SMS Forwarder Android App

A minimal-UI Android app that runs as a background SMS listener and forwards matching messages to user-defined HTTP APIs.

## Features

🔄 **Background SMS Processing** - Silently captures incoming SMS while app is in background  
📋 **Pattern-Based Rules** - Regex or substring matching against SMS content  
🌐 **HTTP API Forwarding** - POST/GET/PUT/PATCH to any HTTP endpoint  
📊 **Custom Headers** - Configurable headers for each forwarding rule  
📈 **History Tracking** - Paginated logs of all forwarding attempts  
🔄 **Auto-Start** - Automatically starts monitoring on device boot  
⚡ **Battery Optimized** - Uses foreground service with minimal notifications  

## Architecture

- **Clean Architecture** (MVVM + Repository Pattern)
- **Jetpack Compose** for modern UI
- **Room Database** for local persistence
- **Retrofit + OkHttp** for HTTP networking
- **Hilt** for dependency injection
- **WorkManager** for background tasks
- **Coroutines + Flow** for reactive programming

## Permissions

The app requires the following permissions:

- `RECEIVE_SMS` - Receive incoming SMS messages
- `READ_SMS` - Read SMS content for processing
- `INTERNET` - Send HTTP requests to configured endpoints
- `RECEIVE_BOOT_COMPLETED` - Auto-start on device boot
- `FOREGROUND_SERVICE` - Run background service reliably
- `WAKE_LOCK` - Keep device awake during processing

## Technical Requirements

- **Target SDK**: 34 (Android 14)
- **Minimum SDK**: 29 (Android 10)
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL

## Project Structure

```
app/src/main/java/com/zerodev/smsforwarder/
├── data/
│   ├── local/
│   │   ├── dao/           # Room DAOs
│   │   ├── database/      # Room database
│   │   ├── entity/        # Room entities
│   │   └── converter/     # Type converters
│   ├── remote/
│   │   ├── api/           # Retrofit API services
│   │   └── client/        # HTTP client implementation
│   ├── repository/        # Repository implementations
│   ├── receiver/          # BroadcastReceivers
│   ├── service/           # Foreground services
│   ├── worker/            # WorkManager workers
│   └── mapper/            # Entity mappers
├── domain/
│   ├── model/             # Domain models
│   └── usecase/           # Business logic
├── di/                    # Dependency injection modules
└── ui/                    # UI components (Compose)
```

## Key Components

### SMS Processing Flow

1. **SmsReceiver** - Intercepts incoming SMS broadcasts
2. **SmsForwardingService** - Processes SMS in foreground service
3. **SmsForwardingUseCase** - Orchestrates rule matching and HTTP forwarding
4. **HttpClient** - Handles HTTP requests with retry logic
5. **HistoryRepository** - Logs all forwarding attempts

### Database Schema

**Rules Table**
- ID, name, pattern, isRegex, endpoint, method, headers, isActive, timestamps

**History Table**
- ID, ruleId, SMS data, request/response data, status, timestamps

### Background Services

- **Foreground Service** - Ensures reliable SMS processing
- **WorkManager** - Handles boot startup and periodic health checks
- **BroadcastReceiver** - Captures SMS and boot events

## Build Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd SMSForwarder
   ```

2. **Open in Android Studio**
   - Import the project
   - Let Gradle sync complete

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run on device/emulator**
   ```bash
   ./gradlew installDebug
   ```

## Permission Rationale

### SMS Permissions (RECEIVE_SMS, READ_SMS)
Required to intercept and read incoming SMS messages for pattern matching and forwarding. The app processes SMS content to determine which rules match and what data to forward to configured APIs.

### Network Permission (INTERNET)
Essential for forwarding SMS data to external HTTP APIs as configured by the user.

### Boot Permission (RECEIVE_BOOT_COMPLETED)
Allows automatic startup of SMS monitoring after device reboot, ensuring continuous operation without manual intervention.

### Service Permissions (FOREGROUND_SERVICE, WAKE_LOCK)
Enables reliable background processing of SMS messages even when the app is not actively used, with proper battery optimization practices.

## Architecture Decisions

### Why Clean Architecture?
- **Separation of Concerns** - Clear boundaries between UI, business logic, and data
- **Testability** - Easy to unit test business logic independently
- **Maintainability** - Changes in one layer don't affect others

### Why Room Database?
- **Type Safety** - Compile-time SQL verification
- **Reactive Queries** - Flow-based reactive updates
- **Migration Support** - Structured database schema evolution

### Why Foreground Service?
- **Reliability** - Survives system memory pressure
- **User Transparency** - Shows ongoing background activity
- **Battery Compliance** - Follows Android best practices

### Why WorkManager?
- **Guaranteed Execution** - Survives app kills and reboots
- **Battery Optimization** - Respects Doze mode and App Standby
- **Constraint-based** - Runs only when conditions are met

## Development Status

✅ **Completed**: Core architecture, SMS processing, HTTP forwarding, database, background services  
🔄 **In Progress**: UI screens, permissions handling  
📋 **TODO**: Testing framework, final documentation  

## License

MIT License - see [LICENSE](LICENSE) file for details. 