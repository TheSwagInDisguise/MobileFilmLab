#!/usr/bin/env python3
"""Dependency-free validation of the static GitHub Pages site and pinned release metadata."""
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit
import json
import re

ROOT = Path(__file__).resolve().parents[1]
SITE = ROOT / 'site'


class SiteParser(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.ids = set()
        self.refs = []
        self.images = []
        self.scripts = []
        self.headings = []

    def handle_starttag(self, tag, attributes):
        attrs = dict(attributes)
        if 'id' in attrs:
            assert attrs['id'] not in self.ids, f'Duplicate id: {attrs["id"]}'
            self.ids.add(attrs['id'])
        for name in ('href', 'src', 'data-screen'):
            if attrs.get(name):
                self.refs.append((tag, name, attrs[name]))
        assert not any(name.lower().startswith('on') for name in attrs), 'Inline event handler in site'
        if tag == 'img':
            assert 'alt' in attrs, 'Image lacks an alt attribute'
            assert attrs.get('width') and attrs.get('height'), 'Image dimensions must be explicit'
            self.images.append(attrs)
        if tag == 'script':
            assert attrs.get('src'), 'No inline executable scripts allowed'
            self.scripts.append(attrs['src'])
        if tag == 'h1':
            self.headings.append(tag)


def main():
    assert SITE.is_dir(), 'Missing static site'
    html = (SITE / 'index.html').read_text()
    parser = SiteParser()
    parser.feed(html)
    assert len(parser.headings) == 1, 'Site must have one primary heading'
    assert parser.scripts == ['app.js'], 'Unexpected script/dependency'
    count = 0
    for tag, name, ref in parser.refs:
        parsed = urlsplit(ref)
        assert parsed.scheme not in ('javascript', 'http'), f'Insecure link: {ref}'
        if parsed.scheme or parsed.netloc:
            assert parsed.scheme == 'https', f'Unexpected URL scheme: {ref}'
            continue
        if not parsed.path:
            if parsed.fragment:
                assert unquote(parsed.fragment) in parser.ids, f'Broken anchor: {ref}'
            continue
        target = (SITE / unquote(parsed.path)).resolve()
        assert target.is_relative_to(SITE) and target.is_file(), f'Missing or escaping site asset: {ref}'
        count += 1
    release = json.loads((SITE / 'release.json').read_text())
    source = (ROOT / 'app/build.gradle.kts').read_text()
    assert release['version'] == re.search(r'versionName\s*=\s*"([^"]+)"', source).group(1)
    assert release['versionCode'] == int(re.search(r'versionCode\s*=\s*(\d+)', source).group(1))
    assert release['package'] == 'com.mobilefilmlab.camera'
    assert release['tag'] == 'v' + release['version']
    assert re.fullmatch(r'[a-f0-9]{64}', release['sha256']), 'Invalid APK digest'
    base = 'https://github.com/TheSwagInDisguise/MobileFilmLab/releases'
    assert release['releaseUrl'] == base + '/tag/' + release['tag']
    assert release['apkUrl'] == f"{base}/download/{release['tag']}/MobileFilmLab-{release['tag']}-signed.apk"
    downloads = [ref for _, _, ref in parser.refs if ref.endswith('.apk')]
    assert len(downloads) >= 2 and set(downloads) == {release['apkUrl']}, 'Stale download CTA'
    assert release['sha256'] in html and release['primaryDevice'] in html
    assert 'RAW_SENSOR' in html and 'RAW + JPEG' in html and 'ISP' in html and 'firmware' in html, 'Missing RAW capability/firmware disclosure'
    assert 'AI-generated demo scene' in html and 'AI-assisted development' in html, 'Missing provenance disclosure'
    for filename in ('styles.css', 'app.js', 'robots.txt', 'sitemap.xml', '.nojekyll'):
        assert (SITE / filename).is_file(), f'Missing site file: {filename}'
    forbidden = {'.apk', '.jks', '.keystore', '.pem', '.key', '.env'}
    for path in SITE.rglob('*'):
        if path.is_file():
            assert path.suffix not in forbidden, f'Private/build artifact in public site: {path.name}'
            assert 'token' not in path.name.lower(), f'Unexpected credential-like site filename: {path.name}'
    print(f'Checked static site: {count} local assets/references, {len(parser.images)} images, one local script, release {release["tag"]}')


if __name__ == '__main__':
    main()
