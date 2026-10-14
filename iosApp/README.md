# TransitGO iOS checkpoint

Open `TransitGO.xcodeproj` in Xcode, select the `TransitGO` scheme and an installed iPhone simulator, then press Run. The deployment target is iOS 15.

## Available features

- Planner: choose stations, search/filter by line, swap endpoints, and request route options with fares and directions.
- Map: pinch and pan the shared transit map, use zoom/reset controls, tap stations to set endpoints or see station details, and view route overlays and directions.
- Nearby stations: choose a station, tap Use my location, and allow location access while using the app. Suggestions use valid GPS coordinates, list up to five stations within 2 km, and exclude stale or imprecise fixes.
- Trips: save a route, view recent trips, restore a trip, and remove saved/recent entries.
- Settings: app information, credits, version, and a persisted dark-mode preference.

The Planner, map, station details, station picker, trip screen, transit state/controller, API parsing, and storage logic live in `shared/src/commonMain`. Android's ViewModel retains its existing lifecycle and Koin integration. Each platform provides a Ktor HTTP engine, a local preferences adapter, and a native location flow.

The map image, bundled station data, and line-logo vectors live in `shared/src/commonMain/composeResources`. Stations use the existing seven-day local cache plus bundled fallback and background refresh; route results have a five-minute in-memory cache. Recent trips remain limited to eight, and the shared trip serializer reads the existing Android preference format.

## Checks from the repository root

```sh
./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :app:assembleDebug
```

For the installed simulator on this Mac:

```sh
xcodebuild -project iosApp/TransitGO.xcodeproj -scheme TransitGO -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17 Pro,OS=26.5' CODE_SIGNING_ALLOWED=NO build
```

Choose a concrete simulator destination for resource integration.

## iOS location checkpoint

The iOS picker uses a one-shot Core Location request after When In Use permission is granted. Requests stop when the app leaves the foreground or the picker closes. Location is not persisted or sent to the API. Denied permission, disabled Location Services, approximate location, timeout, and no nearby station each show an explanation with search still available.

To check nearby suggestions in Simulator, choose **Features → Location → Custom Location** and enter latitude `13.7456`, longitude `100.5347` (Siam). Open a station picker and allow location access. Check permission and Precise Location on a physical iPhone as well.

## Remaining release work

Physical-device interaction and permission checks, app icon, signing, privacy information, and App Store distribution are still required before release.
