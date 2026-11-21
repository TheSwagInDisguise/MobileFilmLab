#!/bin/bash
# Personal test build only. Never uploads to App Store, signs with a certificate or creates a release/tag.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p artifacts/screenshots
SOURCE_SHA="${MFL_SOURCE_SHA:-$(git rev-parse HEAD)}"
xcodebuild -version | tee artifacts/toolchain.log
xcodebuild -showsdks >> artifacts/toolchain.log
xcodebuild -project MobileFilmLab.xcodeproj -scheme MobileFilmLab -configuration Release -destination 'generic/platform=iOS' -derivedDataPath DerivedData CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO "MFL_SOURCE_SHA=$SOURCE_SHA" build 2>&1 | tee artifacts/device-build.log
xcodebuild -project MobileFilmLab.xcodeproj -scheme MobileFilmLab -configuration Debug -destination 'generic/platform=iOS Simulator' -derivedDataPath DerivedData CODE_SIGNING_ALLOWED=NO "MFL_SOURCE_SHA=$SOURCE_SHA" build-for-testing 2>&1 | tee artifacts/simulator-build.log
xcrun simctl list devices available -j > artifacts/simulators.json
SIM=$(python3 - <<'PY'
import json
j=json.load(open('artifacts/simulators.json'))
devices=[(runtime,d) for runtime,ds in j['devices'].items() for d in ds if d.get('isAvailable') and 'iPhone' in d['name']]
assert devices,'No installed iPhone simulator on this runner'
devices.sort(key=lambda v:('iOS' in v[0],v[0],'Pro Max' in v[1]['name'],v[1]['name']),reverse=True)
print(devices[0][1]['udid'])
PY
)
xcrun simctl boot "$SIM" 2>/dev/null || true
xcrun simctl bootstatus "$SIM" -b
xcodebuild -project MobileFilmLab.xcodeproj -scheme MobileFilmLab -configuration Debug -destination "platform=iOS Simulator,id=$SIM" -derivedDataPath DerivedData -resultBundlePath artifacts/SimulatorTests.xcresult CODE_SIGNING_ALLOWED=NO "MFL_SOURCE_SHA=$SOURCE_SHA" test-without-building 2>&1 | tee artifacts/simulator-tests.log
xcrun xcresulttool get test-results summary --path artifacts/SimulatorTests.xcresult --format json > artifacts/qa-summary.json
xcrun xcresulttool export attachments --path artifacts/SimulatorTests.xcresult --output-path artifacts/screenshots --only-failures false 2>/dev/null || xcrun xcresulttool export attachments --path artifacts/SimulatorTests.xcresult --output-path artifacts/screenshots
mkdir -p artifacts/package/Payload
ditto DerivedData/Build/Products/Release-iphoneos/MobileFilmLab.app artifacts/package/Payload/MobileFilmLab.app
(cd artifacts/package && /usr/bin/zip -qry ../MobileFilmLab-iOS-test-unsigned.ipa Payload)
python3 scripts/validate_ipa.py artifacts/MobileFilmLab-iOS-test-unsigned.ipa --source "$SOURCE_SHA" --output artifacts/BUILD-INFO.json
shasum -a 256 artifacts/MobileFilmLab-iOS-test-unsigned.ipa > artifacts/checksums.txt
cp TESTING.md artifacts/TESTING-iOS.md
