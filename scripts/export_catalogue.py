#!/usr/bin/env python3
"""Deterministically export all Android recipes and matching colour vectors; never edit Android code."""
from pathlib import Path
import argparse,json,re
ROOT=Path(__file__).resolve().parents[1]
MODEL=ROOT/'app/src/main/java/com/mobilefilmlab/camera/data/model'
DEFAULTS=dict(style=1,sat=0,con=0,sharp=0,wbMode=1,kelvin=0,ab=0,gm=0,pe=0,ev=0,dro=6,sub=0,puckTone=0.0,puckColor=0.0,isFavourite=False,isCustom=False,version=1,notes='')
def split_outside(text,delimiter=','):
 out=[];start=0;quoted=False;escaped=False
 for i,c in enumerate(text):
  if quoted:
   if escaped:escaped=False
   elif c=='\\':escaped=True
   elif c=='"':quoted=False
  elif c=='"':quoted=True
  elif c==delimiter:out.append(text[start:i]);start=i+1
 out.append(text[start:]);return out

def catalogue():
 model=(MODEL/'Recipe.kt').read_text();constants={}
 for name,body in re.findall(r'object (\w+) \{(.*?)(?=\n\})',model,re.S):
  for k,v in re.findall(r'const val (\w+) = (-?\d+)',body):constants[f'{name}.{k}']=int(v)
 source=(MODEL/'RecipeCatalog.kt').read_text().split('val ALL_RECIPES:')[1].split('fun getById')[0]
 recipes=[]
 for m in re.finditer(r'\bRecipe\(',source):
  start=m.end();quoted=False;escaped=False;end=None
  for i in range(start,len(source)):
   c=source[i]
   if quoted:
    if escaped:escaped=False
    elif c=='\\':escaped=True
    elif c=='"':quoted=False
   elif c=='"':quoted=True
   elif c==')':end=i;break
  assert end is not None
  values=dict(DEFAULTS)
  for part in split_outside(source[start:end]):
   part=part.strip()
   if not part:continue
   key,val=map(str.strip,part.split('=',1))
   if val.startswith('"'):v=json.loads(val)
   elif val in ('true','false'):v=val=='true'
   elif val.startswith('RecipeGroup.'):v=val.split('.')[1]
   elif val in constants:v=constants[val]
   else:v=float(val[:-1]) if val.endswith('f') else int(val)
   assert key in DEFAULTS or key in ('id','name','group'),key
   values[key]=v
  recipes.append(values)
 assert len(recipes)==80 and len({r['id'] for r in recipes})==80
 groups=[dict(id=n,name=label,order=int(order)) for n,label,order in re.findall(r'(\w+)\("([^"]+)", (-?\d+)\)',(MODEL/'RecipeGroup.kt').read_text())]
 return {'type':'mobilefilmlab.catalogue','version':1,'upstreamCount':77,'baselineCount':3,'groups':groups,'recipes':recipes}

def identity():return [1.,0,0,0,0,0,1.,0,0,0,0,0,1.,0,0,0,0,0,1.,0]
def after(a,b):
 return [sum(b[y*5+k]*a[k*5+x] for k in range(4))+(b[y*5+4] if x==4 else 0) for y in range(4) for x in range(5)]
def matrix(r):
 m=identity();mono=r['style']==6 or r['pe'] in (7,10)
 if mono:
  w= [(.70,.20,.10),(.50,.40,.10),(.25,.65,.10)][['Acros +R (red filter)','Acros +Ye (yellow filter)','Acros +G (green filter)'].index(r['name'])] if r['name'] in ['Acros +R (red filter)','Acros +Ye (yellow filter)','Acros +G (green filter)'] else ((.70,.20,.10) if r['name']=='GR Hi-Contrast B&W' else (.213,.715,.072))
  monoM=[*w,0,0,*w,0,0,*w,0,0,0,0,0,1,0];m=after(m,monoM)
 else:
  sat=max(.1,min(2,({2:1.16,3:.94,4:.96,5:1.1,8:1.08}.get(r['style'],1)+r['sat']*.12)*(1+r['puckColor']*.003)))
  q=1-sat;R=.213*q;G=.715*q;B=.072*q
  m=after(m,[R+sat,G,B,0,0,R,G+sat,B,0,0,R,G,B+sat,0,0,0,0,0,1,0])
 con=max(.5,min(1.8,({3:.96,9:.92,8:1.1}.get(r['style'],1)+r['con']*.10)*(1+r['puckTone']*.003)))
 bias=(1-con)*128;m=after(m,[con,0,0,0,bias,0,con,0,0,bias,0,0,con,0,bias,0,0,0,1,0])
 red=green=blue=1.
 if r['wbMode']==14 and r['kelvin']>0:
  d=(r['kelvin']-5600)/1000
  if d<0:red+=-d*.08;blue-=-d*.08
  else:blue+=d*.07;red-=d*.07
 ab=r['ab']+r['puckColor']*.05;red+=ab*.025;blue-=ab*.025
 green+=r['gm']*.025;red-=r['gm']*.012;blue-=r['gm']*.012
 red,green,blue=(max(.4,min(1.8,c)) for c in (red,green,blue))
 m=after(m,[red,0,0,0,0,0,green,0,0,0,0,0,blue,0,0,0,0,0,1,0])
 if r['ev']:
  e=r['ev']/3*16;m=after(m,[1,0,0,0,e,0,1,0,0,e,0,0,1,0,e,0,0,0,1,0])
 if r['style']==14:m=after(m,[.393,.769,.189,0,0,.349,.686,.168,0,0,.272,.534,.131,0,0,0,0,0,1,0])
 if r['pe']==4:m=after(m,[.95,0,0,0,12,0,.91,0,0,10,0,0,.82,0,8,0,0,0,1,0])
 elif r['pe']==5:m=after(m,[.82,0,0,0,38,0,.84,0,0,38,0,0,.88,0,40,0,0,0,1,0])
 return m

def outputs():
 data=catalogue();encoded=json.dumps(data,ensure_ascii=False,indent=2)+'\n'
 vectors=[]
 for r in data['recipes']:
  for tone,color in [(0.,0.),(-35.,45.)]:
   sample=dict(r,puckTone=tone,puckColor=color);vectors.append({'id':r['id'],'tone':tone,'color':color,'matrix':matrix(sample)})
 return {'shared/catalogue.json':encoded,'ios/Packages/FilmLabCore/Sources/FilmLabCore/Resources/catalogue.json':encoded,'shared/colour-vectors.json':json.dumps(vectors,indent=2)+'\n','ios/Packages/FilmLabCore/Tests/FilmLabCoreTests/colour-vectors.json':json.dumps(vectors,indent=2)+'\n'}
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');args=parser.parse_args()
 for path,text in outputs().items():
  p=ROOT/path
  if args.check:assert p.is_file() and p.read_text()==text,f'Catalogue drift: run python3 scripts/export_catalogue.py ({path})'
  else:p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
 print('Complete 80-recipe catalogue, 14 groups and 160 cross-platform matrix vectors verified' if args.check else 'Exported full catalogue and colour vectors without editing Android')
