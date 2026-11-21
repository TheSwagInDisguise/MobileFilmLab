#!/usr/bin/env python3
"""Independently inspect IPA zip paths, plist, Mach-O CPU/encryption and included GPL/catalogue resources."""
import argparse,hashlib,json,plistlib,struct,zipfile
from pathlib import Path,PurePosixPath

def macho(data,executable=True):
 assert data[:4]==b'\xcf\xfa\xed\xfe','Expected little-endian Mach-O 64 executable'
 _,cpu,sub,filetype,ncmds,sizeofcmds,flags,reserved=struct.unpack_from('<8I',data)
 assert cpu==0x0100000c and filetype in ([2] if executable else [2,6]),'Device binary must be arm64 iOS Mach-O'
 offset=32;cryptids=[];minimum=None;signatures=[]
 for _ in range(ncmds):
  cmd,size=struct.unpack_from('<II',data,offset);assert size>=8 and offset+size<=len(data)
  if cmd in (0x21,0x2c):cryptids.append(struct.unpack_from('<I',data,offset+16)[0])
  if cmd==0x1d:
   start,length=struct.unpack_from('<II',data,offset+8)
   blob=data[start:start+length]
   assert len(blob)>=12,'Truncated code signature'
   magic,total,count=struct.unpack_from('>III',blob)
   assert magic==0xfade0cc0 and total<=len(blob),'Unexpected signing blob'
   directories=0
   for i in range(count):
    slot,where=struct.unpack_from('>II',blob,12+i*8)
    if slot==0 or 0x1000<=slot<0x1006:
     cdmagic,cdlen,version,signflags=struct.unpack_from('>IIII',blob,where)
     assert cdmagic==0xfade0c02 and signflags&2,'Developer-signed executable unexpectedly supplied'
     signatures.append('ad-hoc linker signature only');directories+=1
   assert directories,'No inspectable CodeDirectory'
  if cmd==0x32:
   platform,version=struct.unpack_from('<II',data,offset+8);assert platform==2,'Not an iOS device binary';minimum=f'{version>>16}.{(version>>8)&255}.{version&255}'
  offset+=size
 assert all(x==0 for x in cryptids),'IPA must not contain FairPlay-encrypted code'
 return dict(cpu='arm64',filetype='MH_EXECUTE' if filetype==2 else 'MH_DYLIB',signature=signatures or ['none'],encryption_ids=cryptids,encrypted=False,minimum_load_version=minimum)

def validate(path,source=None):
 p=Path(path)
 with zipfile.ZipFile(p) as z:
  assert z.testzip() is None,'CRC failure'
  names=z.namelist();assert len(names)==len(set(names)),'Duplicate zip entries'
  assert all(not PurePosixPath(n).is_absolute() and '..' not in PurePosixPath(n).parts for n in names),'Unsafe zip paths'
  apps={n.split('/')[1] for n in names if n.startswith('Payload/') and len(n.split('/'))>2};assert apps=={'MobileFilmLab.app'},apps
  base='Payload/MobileFilmLab.app/'
  info=plistlib.loads(z.read(base+'Info.plist'))
  assert info['CFBundleIdentifier']=='com.mobilefilmlab.camera'
  assert info['CFBundleDisplayName']=='MobileFilmLab'
  assert info['CFBundleShortVersionString']=='0.1.0' and info['CFBundleVersion']=='2'
  assert info.get('NSCameraUsageDescription') and info.get('NSPhotoLibraryAddUsageDescription') and info.get('NSMotionUsageDescription')
  assert not info.get('UIBackgroundModes') and not info.get('NSMicrophoneUsageDescription')
  assert float(info['MinimumOSVersion'])<=27
  if source:assert info.get('MFLSourceSHA')==source,'Wrong source commit'
  assert not any(n.endswith(('.mobileprovision','.p12','.jks','.keystore')) for n in names),'Signing material/profile unexpectedly included'
  binary=macho(z.read(base+info['CFBundleExecutable']))
  for name in names:
   if name.endswith('/') or name==base+info['CFBundleExecutable']:continue
   content=z.read(name)
   if content[:4]==b'\xcf\xfa\xed\xfe':macho(content,executable=False)
  cats=[n for n in names if n.endswith('/catalogue.json')];assert len(cats)==1,'Full catalogue missing/duplicated'
  catalogue=json.loads(z.read(cats[0]));assert len(catalogue['recipes'])==80 and catalogue['upstreamCount']==77
  assert len({r['id'] for r in catalogue['recipes']})==80
  licenses=[n for n in names if n.endswith('/GPL-3.0-or-later.txt')];assert len(licenses)==1
  assert b'GNU GENERAL PUBLIC LICENSE' in z.read(licenses[0])
  attrs=[n for n in names if n.endswith('/Attribution.txt')];assert len(attrs)==1 and b'voxivoid' in z.read(attrs[0])
  privacy=[n for n in names if n.endswith('/PrivacyInfo.xcprivacy')];assert privacy
  for name in privacy:
   manifest=plistlib.loads(z.read(name));assert not manifest.get('NSPrivacyTracking',False)
  return dict(filename=p.name,bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest(),source=info.get('MFLSourceSHA'),bundle_id=info['CFBundleIdentifier'],version=info['CFBundleShortVersionString'],build=info['CFBundleVersion'],minimum_ios=info['MinimumOSVersion'],binary=binary,catalogue_count=80,upstream_count=77,signing='No developer signature or embedded provisioning supplied; owner must re-sign',handset_tested=False,distribution='iOS test build; owner-controlled signing; GitHub release publication authorised')
if __name__=='__main__':
 a=argparse.ArgumentParser();a.add_argument('ipa');a.add_argument('--source');a.add_argument('--output');args=a.parse_args();result=validate(args.ipa,args.source);text=json.dumps(result,indent=2)+'\n';print(text)
 if args.output:Path(args.output).write_text(text)
