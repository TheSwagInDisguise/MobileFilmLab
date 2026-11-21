import importlib.util,json,struct,unittest,zlib,base64
from pathlib import Path
from export_catalogue import catalogue,matrix
ROOT=Path(__file__).resolve().parents[1]
class IOSInterchangeTest(unittest.TestCase):
 def test_catalogue_contains_all_upstream_and_baseline_ids(self):
  c=catalogue();self.assertEqual(len(c['recipes']),80);self.assertEqual(len(c['groups']),14);self.assertEqual(sum(r['group']!='OPPO' for r in c['recipes']),77)
 def test_natural_is_identity(self):self.assertEqual(matrix(catalogue()['recipes'][0]),[1.,0,0,0,0,0,1.,0,0,0,0,0,1.,0,0,0,0,0,1.,0])
 def test_legacy_jvm_qr_fixture_contains_full_valid_recipe(self):
  value=json.loads((ROOT/'shared/interchange-fixtures.json').read_text());payload=value['androidDeflaterQR'].split(':',2)[2];data=zlib.decompress(base64.urlsafe_b64decode(payload+'='*(-len(payload)%4)));self.assertEqual(json.loads(data),value['document']);self.assertEqual(value['document']['recipes'][0],catalogue()['recipes'][3])
 def test_ipa_validator_rejects_simulator_and_encryption(self):
  spec=importlib.util.spec_from_file_location('ipa',ROOT/'ios/scripts/validate_ipa.py');m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
  h=struct.pack('<8I',0xfeedfacf,0x0100000c,0,2,2,48,0,0)
  def binary(platform,crypt):return h+struct.pack('<6I',0x32,24,platform,18<<16,0,0)+struct.pack('<6I',0x2c,24,0,0,crypt,0)
  self.assertEqual(m.macho(binary(2,0))['cpu'],'arm64')
  for platform,crypt in [(7,0),(2,1)]:
   with self.assertRaises(AssertionError):m.macho(binary(platform,crypt))
if __name__=='__main__':unittest.main()
