#!/usr/bin/env python3
"""Fail closed on release identity and exact-tag non-alpha publication approval."""
from pathlib import Path
import os
import re
import subprocess


def verify_approval(tag: str, approved_tag: str | None) -> None:
    if '-alpha.' not in tag:
        assert approved_tag == tag, 'Non-alpha publication blocked: approval must name this exact tag'


def main() -> None:
    text = Path('app/build.gradle.kts').read_text()
    version = re.search(r'versionName\s*=\s*"([^"]+)"', text).group(1)
    code = int(re.search(r'versionCode\s*=\s*(\d+)', text).group(1))
    package = re.search(r'applicationId\s*=\s*"([^"]+)"', text).group(1)
    assert package == 'com.mobilefilmlab.camera', 'Release package identity changed'
    assert re.fullmatch(r'\d+\.\d+\.\d+(?:-(?:alpha|beta|rc)\.\d+)?', version), 'Invalid version'
    assert code > 0
    assert Path(f'.github/release-notes/{version}.md').is_file(), 'Missing version-specific release notes'
    tag = os.environ.get('RELEASE_TAG')
    if tag:
        assert tag == f'v{version}', 'Tag does not match source version'
        verify_approval(tag, os.environ.get('APPROVED_RELEASE_TAG'))
        tagged = subprocess.check_output(['git', 'rev-parse', f'refs/tags/{tag}^{{commit}}'], text=True).strip()
        head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
        assert tagged == head, 'Release source is not the tagged commit'
    print(f'Verified {package} {version} ({code})' + (f' / {tag}' if tag else ' — source check only'))


if __name__ == '__main__':
    main()
