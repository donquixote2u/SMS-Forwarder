# CI/CD Setup for SMS Forwarder

## Overview
This document describes the GitHub Actions workflows configured for the SMS Forwarder Android app, providing automated building, testing, and release management.

## Workflows

### 1. Build and Release Workflow
**File**: `.github/workflows/build-and-release.yml`

#### Purpose
Comprehensive CI/CD pipeline that builds, tests, and releases the Android app.

#### Triggers
- **Push Events**: `master`, `main`, `develop` branches
- **Pull Requests**: To `master` or `main` branches
- **Release Events**: When a GitHub release is created
- **Manual Dispatch**: Can be triggered manually with release option
- **Tag Push**: Automatically creates releases for version tags

#### Jobs

##### Build Job
- **Environment**: Ubuntu Latest
- **JDK**: Version 17 (Temurin distribution)
- **Android SDK**: Latest version
- **Caching**: Gradle dependencies and wrapper
- **Steps**:
  1. Checkout source code
  2. Setup Java and Android SDK
  3. Cache Gradle files
  4. Run unit tests
  5. Build debug APK
  6. Build release APK
  7. Upload artifacts (APKs and build info)

##### Release Job
- **Dependency**: Requires build job completion
- **Condition**: Only runs for releases, manual dispatch, or tag pushes
- **Steps**:
  1. Download build artifacts
  2. Extract version from `build.gradle.kts`
  3. Generate comprehensive release notes
  4. Create GitHub release
  5. Upload both debug and release APKs

##### Security Scan Job
- **Purpose**: Vulnerability scanning of dependencies
- **Tool**: OWASP Dependency Check
- **Output**: HTML security report
- **Condition**: Only on push and PR events

##### Lint Job
- **Purpose**: Code quality analysis
- **Tool**: Android Lint
- **Output**: Lint report HTML

#### Features
- **Automatic Versioning**: Extracts version from Gradle build file
- **Rich Release Notes**: Auto-generated with features, installation instructions, and build info
- **Artifact Management**: Stores APKs and reports as GitHub artifacts
- **Security Scanning**: Regular dependency vulnerability checks
- **Build Caching**: Optimized build times with Gradle caching

### 2. PR Check Workflow
**File**: `.github/workflows/pr-check.yml`

#### Purpose
Fast validation workflow for pull requests to ensure code quality before merging.

#### Triggers
- **Pull Requests**: To `master` or `main` branches

#### Features
- **Quick Validation**: Runs lint, tests, and build
- **PR Comments**: Automatically comments on PRs with build status
- **Status Updates**: Updates existing comments instead of creating new ones
- **Detailed Feedback**: Shows individual step outcomes (lint, test, build)

#### Job Steps
1. Code checkout
2. Java/Android SDK setup
3. Gradle caching
4. Lint analysis
5. Unit test execution
6. Debug APK build
7. PR status comment

## Release Process

### Automatic Release (Recommended)
```bash
# Create and push a version tag
git tag v1.0.0
git push origin v1.0.0
```

This triggers:
1. Automatic workflow execution
2. APK building (debug + release)
3. GitHub release creation
4. APK attachment to release
5. Release notes generation

### Manual Release
1. Navigate to GitHub Actions tab
2. Select "Build and Release" workflow
3. Click "Run workflow"
4. Enable "Create a new release" option
5. Execute workflow

### Release Artifacts
Each release includes:
- **Release APK**: `sms-forwarder-v{version}-release.apk`
- **Debug APK**: `sms-forwarder-v{version}-debug.apk`
- **Release Notes**: Auto-generated with features and build info
- **Build Metadata**: Commit SHA, build date, branch info

## Security Considerations

### APK Signing
- **Development**: APKs are unsigned for security
- **Production**: Users should sign with their own keystores
- **Distribution**: GitHub releases provide unsigned APKs only

### Dependency Scanning
- **Tool**: OWASP Dependency Check
- **Frequency**: Every push and PR
- **Reports**: Available as workflow artifacts
- **Format**: HTML reports with vulnerability details

### Permissions
- **GitHub Token**: Uses built-in `GITHUB_TOKEN`
- **Scope**: Limited to repository actions
- **Security**: No external secrets required

## Workflow Optimization

### Caching Strategy
```yaml
- name: Cache Gradle packages
  uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
```

### Build Performance
- **Parallel Jobs**: Multiple jobs run concurrently
- **Conditional Execution**: Jobs run only when needed
- **Artifact Reuse**: Build artifacts shared between jobs
- **Gradle Daemon**: Enabled for faster builds

## Monitoring and Maintenance

### Workflow Status
- **GitHub Actions Tab**: View all workflow runs
- **Status Badges**: Can be added to README
- **Email Notifications**: GitHub sends failure notifications
- **PR Integration**: Status checks prevent merging failed builds

### Artifact Retention
- **Build Artifacts**: Retained for 90 days (GitHub default)
- **Release Assets**: Permanent (attached to releases)
- **Log Files**: Available for 90 days
- **Cache**: Automatic cleanup after 7 days

### Troubleshooting
Common issues and solutions:

#### Build Failures
1. Check Gradle compatibility
2. Verify Android SDK versions
3. Review dependency conflicts
4. Check Java version compatibility

#### Release Creation Issues
1. Verify tag format (`v*.*.*`)
2. Check GitHub token permissions
3. Ensure version extraction works
4. Validate release notes format

#### Cache Issues
1. Clear cache manually if corrupted
2. Update cache keys for new dependencies
3. Check cache size limits

## Future Enhancements

### Planned Improvements
- **Signed APK Releases**: Add keystore signing for production
- **Play Store Deployment**: Automated Play Console uploads
- **Code Coverage**: Integration with coverage reporting tools
- **Performance Testing**: Add UI and performance tests
- **Multi-Architecture**: Build for different CPU architectures

### Monitoring Integration
- **Crash Reporting**: Firebase Crashlytics integration
- **Analytics**: Usage analytics for release tracking
- **Performance**: APK size and build time monitoring

## Best Practices

### Workflow Design
- **Fail Fast**: Quick feedback on common issues
- **Parallel Execution**: Maximize concurrent job execution
- **Conditional Logic**: Run jobs only when necessary
- **Clear Naming**: Descriptive job and step names

### Security
- **Minimal Permissions**: Use least privilege principle
- **Secret Management**: Avoid hardcoded secrets
- **Dependency Scanning**: Regular vulnerability checks
- **Unsigned Releases**: Maintain security through unsigned APKs

### Maintenance
- **Regular Updates**: Keep action versions current
- **Dependency Updates**: Monitor and update dependencies
- **Workflow Testing**: Test workflow changes in branches
- **Documentation**: Keep this documentation updated

This CI/CD setup provides a robust, secure, and efficient development workflow for the SMS Forwarder Android application. 