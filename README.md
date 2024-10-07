# Weaver: Manage Local Hotspots on Android (API level 33+)

This library provides an interface (Weaver) and implementation (WeaverImpl) for controlling local hotspots on Android devices running API level 33 or higher. It requires the NEARBY_WIFI_DEVICES permission.

## Features
Start a local hotspot with customizable configuration (SSID, password, security type, band, etc.)

Stop the currently active local hotspot.

Monitor the current state of the local hotspot (active/inactive) through a state flow.

## Usage
### 1. Add the dependency:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "your.package.name:weaver:<version>"
}
```
### 2. Obtain an instance of Weaver:

```kotlin
val context = requireContext()
val weaver = Weaver.create(context)
```
### 3. Start a local hotspot:

```kotlin
val ssid = "My Hotspot"
val password = "secret123"
val securityType = Weaver.SECURITY_TYPE_WPA2_PSK

weaver.startTethering(
    ssid = ssid,
    password = password,
    securityType = securityType,
    autoShutdownEnabled = true
)
```
### 4. Stop the local hotspot:

```kotlin
weaver.stopTethering()
```
### 5. Monitor the hotspot state:

```kotlin
val tetherStateFlow = weaver.tetherState

tetherStateFlow.onEach { isActive ->
    if (isActive) {
        // Hotspot is active
    } else {
        // Hotspot is inactive
    }
}.launchIn(lifecycleScope) // Collect updates in your lifecycle scope
```
## Additional Notes

This library uses reflection to access private APIs. While this allows for greater control on API level 33+, it might not be future-proof and could break with changes in the Android framework. Consider using official APIs when they become available.

Remember to request the **NEARBY_WIFI_DEVICES** permission before using this library.

This README provides a basic understanding of the Weaver library. Refer to the source code for a complete picture of the functionalities and limitations.

## Contributing

Pull requests are welcome. For major changes, please open an issue first
to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License

[MIT](https://choosealicense.com/licenses/mit/)
