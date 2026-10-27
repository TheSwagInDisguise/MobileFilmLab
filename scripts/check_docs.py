#!/usr/bin/env python3
"""Check checked-in Markdown links/images without network access or dependencies."""
from pathlib import Path
import re
from urllib.parse import unquote, urlsplit

root = Path(__file__).resolve().parents[1]
# Include new untracked docs during local preparation; skip Git/build/tool caches.
files = [root / 'README.md', root / 'CONTRIBUTING.md', root / 'SUPPORT.md', root / 'KNOWN_LIMITATIONS.md',
         root / 'UPSTREAM_ATTRIBUTION.md', root / 'THIRD_PARTY_NOTICES.md', root / 'ARCHITECTURE.md',
         *sorted((root / 'docs').rglob('*.md')), root / 'design/README.md']
errors = []
count = 0
for file in files:
    if not file.is_file():
        errors.append(f'Missing documentation: {file.relative_to(root)}')
        continue
    text = file.read_text()
    links = re.findall(r'!?\[[^\]]*\]\(([^\s)]+)(?:\s+"[^"]*")?\)', text)
    links += re.findall(r'<img\b[^>]*\bsrc=["\']([^"\']+)["\']', text)
    for link in links:
        parsed = urlsplit(link)
        if parsed.scheme or parsed.netloc or not parsed.path:
            continue
        target = (file.parent / unquote(parsed.path)).resolve()
        if not target.is_relative_to(root) or not target.exists():
            errors.append(f'{file.relative_to(root)}: broken local link {link}')
        count += 1
if errors:
    raise SystemExit('\n'.join(errors))
print(f'Checked {count} local documentation links/images in {len(files)} files')
