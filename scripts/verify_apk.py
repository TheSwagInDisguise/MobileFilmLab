#!/usr/bin/env python3
"""Verify public release identity. This script never receives credentials."""
from pathlib import Path
import os
import re
import subprocess
import sys

apk=Path(sys.argv[1])
sdk=Path(os.environ.get('ANDROID_HOME') or os.environ['ANDROID_SDK_ROOT'])
tools=sdk/'build-tools'/'34.0.0'
certificate='8dcdb6c65bdac80a488d063d7026b3d45eaa5b4bc002e2d4c84acbc97351f41a'
result=subprocess.check_output([str(tools/'apksigner'),'verify','--verbose','--print-certs',str(apk)],text=True)
match=re.search(r'Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F]+)',result)
assert match and match.group(1).lower()==certificate,'Release signing certificate changed'
manifest=subprocess.check_output([str(tools/'aapt2'),'dump','badging',str(apk)],text=True)
config=Path('app/build.gradle.kts').read_text()
version=re.search(r'versionName\s*=\s*"([^"]+)"',config).group(1)
code=re.search(r'versionCode\s*=\s*(\d+)',config).group(1)
assert "name='com.mobilefilmlab.camera'" in manifest,'Wrong package'
assert f"versionCode='{code}'" in manifest and f"versionName='{version}'" in manifest,'Wrong version'
subprocess.run([str(tools/'zipalign'),'-c','4',str(apk)],check=True)
print(f'Verified release APK package/version/alignment/signature: {apk.name}')
print(f'Public signing certificate SHA-256: {certificate}')
