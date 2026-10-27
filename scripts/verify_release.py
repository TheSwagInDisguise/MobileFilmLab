#!/usr/bin/env python3
"""Fail closed before release: version/tag/package identity and explicit non-alpha approval."""
from pathlib import Path
import os
import re
import subprocess

text=Path('app/build.gradle.kts').read_text()
version=re.search(r'versionName\s*=\s*"([^"]+)"',text).group(1)
code=int(re.search(r'versionCode\s*=\s*(\d+)',text).group(1))
package=re.search(r'applicationId\s*=\s*"([^"]+)"',text).group(1)
assert package=='com.mobilefilmlab.camera','Release package identity changed'
assert re.fullmatch(r'\d+\.\d+\.\d+(?:-(?:alpha|beta|rc)\.\d+)?',version),'Invalid version'
assert code>0
assert Path(f'.github/release-notes/{version}.md').is_file(),'Missing version-specific release notes'
tag=os.environ.get('RELEASE_TAG')
if tag:
    assert tag==f'v{version}','Tag does not match source version'
    if '-alpha.' not in tag:
        assert os.environ.get('BETA_RELEASE_APPROVED')=='true','Non-alpha publication blocked: owner approval still required'
    tagged=subprocess.check_output(['git','rev-parse',f'refs/tags/{tag}^{{commit}}'],text=True).strip()
    head=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
    assert tagged==head,'Release source is not the tagged commit'
print(f'Verified {package} {version} ({code})'+(f' / {tag}' if tag else ' — source check only'))
