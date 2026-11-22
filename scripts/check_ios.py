#!/usr/bin/env python3
"""Offline source/package invariants. Hardware behaviour requires an iPhone."""
import hashlib,json,plistlib,re
from pathlib import Path
from export_catalogue import outputs
ROOT=Path(__file__).resolve().parents[1]
def main():
 for name,text in outputs().items():assert (ROOT/name).read_text()==text,f'Drift: {name}'
 baseline=json.loads((ROOT/'shared/android-site-baseline.json').read_text())
 for path,digest in baseline['sha256'].items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,f'Android/public website unexpectedly changed: {path}'
 assert (ROOT/'LICENSE').read_bytes()==(ROOT/'ios/MobileFilmLab/Resources/GPL-3.0-or-later.txt').read_bytes()
 assert b'voxivoid' in (ROOT/'ios/MobileFilmLab/Resources/Attribution.txt').read_bytes()
 project=(ROOT/'ios/project.yml').read_text();assert 'CODE_SIGNING_ALLOWED: NO' in project and 'com.mobilefilmlab.camera' in project
 privacy=plistlib.loads((ROOT/'ios/MobileFilmLab/PrivacyInfo.xcprivacy').read_bytes());assert not privacy['NSPrivacyTracking'] and privacy['NSPrivacyCollectedDataTypes']==[]
 swift=list((ROOT/'ios').rglob('*.swift'))
 for p in swift:
  if any(x in p.parts for x in ['.build','DerivedData','artifacts']):continue
  source=p.read_text()
  assert not any(k in source for k in ['AVCaptureMovieFileOutput','AVAudioRecorder','import CoreML','import FoundationModels']),f'Out-of-scope capture/AI in {p}'
 workflow=(ROOT/'.github/workflows/ios-test.yml').read_text();assert 'workflow_dispatch:' in workflow and 'contents: read' in workflow
 assert not any(k in workflow for k in ['gh release create','create-release','contents: write','APPLE_ID','SIGNULOUS_PASSWORD'])
 assert (ROOT/'ios/TESTING.md').is_file()
 view=(ROOT/'ios/MobileFilmLab/CameraView.swift').read_text()
 assert 'ForEach(camera.visibleLenses)' in view and 'ForEach(camera.routes)' not in view, 'Do not mix front/virtual routes into the physical rear lens row'
 for control in ['jpegCrop','flipCamera','cameraMode','captureFormat','openSettings','openRecipes']:
  assert 'accessibilityIdentifier("'+control+'")' in view, 'Missing accessible camera control: '+control
 assert 'if pro { proPanel }' in view and 'if !store.preferences.format.writesRaw { framingMenu }' in view
 controller=(ROOT/'ios/MobileFilmLab/CameraController.swift').read_text()
 assert '#if DEBUG && targetEnvironment(simulator)' in controller and 'Sample lens metadata · no camera feed' in controller
 assert (ROOT/'ios/Packages/FilmLabCore/Tests/FilmLabCoreTests/LensPresentationTests.swift').is_file()
 # Preview performance must not be fixed by silently lowering still-photo quality or removing live looks.
 photo=json.loads((ROOT/'shared/ios-photo-baseline.json').read_text())
 for path,digest in photo['sha256'].items():assert hashlib.sha256((ROOT/path).read_bytes()).hexdigest()==digest,f'Photo pipeline changed in preview-only release: {path}'
 transaction=controller[controller.index('    func capture(_ preferences:'):controller.index('    func captureOutput(')]
 assert hashlib.sha256(transaction.encode()).hexdigest()==photo['capture_transaction_sha256'],'Still/HDR capture transaction unexpectedly changed'
 frame_path=controller[controller.index('    func captureOutput('):controller.index('    #if DEBUG && targetEnvironment(simulator)\n    var isShowingLensFixture:')]
 assert not any(x in frame_path for x in ['createCGImage','UIImage(','1/12','lastPreview']), 'Do not restore bitmap readback or the 12 FPS limiter in camera delivery'
 assert '@Published var preview:UIImage?' not in controller and 'CameraPreviewSurface(frames:' in view
 assert 'video.deliversPreviewSizedOutputBuffers = true' in controller and 'video.alwaysDiscardsLateVideoFrames=true' in controller
 renderer=(ROOT/'ios/MobileFilmLab/CameraPreviewSurface.swift').read_text()
 assert 'DispatchSemaphore(value: 2)' in renderer and 'preferredFramesPerSecond = 60' in renderer and 'command.waitUntilCompleted' not in renderer
 assert 'LatestFrameMailbox<CameraPreviewFrame>' in renderer and 'destination.isFlipped = true' in renderer
 drawer=(ROOT/'ios/MobileFilmLab/RecipeDrawer.swift').read_text()
 assert 'requestLookThumbnails(true)' in drawer and 'requestLookThumbnails(false)' in drawer and 'createCGImage' not in drawer
 print(f'iOS source invariants passed: {len(swift)} Swift files, 80 recipes, unchanged Android/site + photo-processing/capture baselines, bounded native preview, GPL, no signing credentials or release writes in build workflow')
if __name__=='__main__':main()
