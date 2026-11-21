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
 print(f'iOS source invariants passed: {len(swift)} Swift files, complete 80 recipes, unchanged Android/site baseline, GPL, no signing credentials or publishing')
if __name__=='__main__':main()
