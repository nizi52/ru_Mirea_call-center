"""End-to-end acceptance check. Run only against a disposable KR3 demo database.
Creates uniquely named records/users; never deletes existing records.
Reads local .env without printing secrets. Output includes only test names and status.
"""
import json, urllib.request, urllib.error, uuid, os, csv, io
from pathlib import Path
root=Path(__file__).resolve().parents[1]
env=dict(line.split('=',1) for line in (root/'.env').read_text().splitlines() if line and not line.startswith('#'))
base=os.getenv('KR3_BASE_URL','http://localhost:'+env.get('APP_PORT','8080'))
checks=[]
def req(method,path,data=None,token=None,expect=200,headers=None):
 h={'Content-Type':'application/json',**(headers or {})}
 if token:h['Authorization']='Bearer '+token
 raw=data if isinstance(data,bytes) else None if data is None else json.dumps(data).encode()
 request=urllib.request.Request(base+path,data=raw,headers=h,method=method)
 try:
  with urllib.request.urlopen(request,timeout=40) as response:status=response.status;body=response.read();ct=response.headers.get('Content-Type','')
 except urllib.error.HTTPError as e:status=e.code;body=e.read();ct=e.headers.get('Content-Type','')
 assert status==expect,(method,path,status,body[:1000])
 return json.loads(body) if body and 'json' in ct else body

def check(name):checks.append(name);print('PASS',name,flush=True)
def login(email,password):return req('POST','/api/auth/login',{'email':email,'password':password,'clientType':'NATIVE'})
req('GET','/actuator/health');check('Docker backend health')
req('GET','/api/appeals',expect=401);check('401 without authentication')
a=login(env['BOOTSTRAP_ADMIN_EMAIL'],(root/'.env.verify').read_text() if (root/'.env.verify').exists() else env['BOOTSTRAP_ADMIN_PASSWORD']);token=a['accessToken']
password='Verify!'+uuid.uuid4().hex+'1aA'
if a['user']['passwordChangeRequired']:
 req('GET','/api/appeals',token=token,expect=403)
 req('POST','/api/auth/change-initial-password',{'newPassword':password},token,204)
 # Save only local test credential for reruns; no console output and ignored by git.
 local=root/'.env.verify';local.write_text(password);local.chmod(0o600)
 a=login(env['BOOTSTRAP_ADMIN_EMAIL'],password);token=a['accessToken']
check('initial password enforcement and change')
req('GET','/api/auth/me',token=token)
refresh=req('POST','/api/auth/refresh',{'refreshToken':a['refreshToken']});token=refresh['accessToken'];check('refresh rotation')
suffix=uuid.uuid4().hex[:8]
viewPass='Reader!'+uuid.uuid4().hex+'1aA';editPass='Editor!'+uuid.uuid4().hex+'1aA'
users={}
for role,pw in [('VIEWER',viewPass),('EDITOR',editPass)]:
 email=role.lower()+suffix+'@example.org'
 u=req('POST','/api/admin/users',{'email':email,'displayName':role,'roles':[role],'temporaryPassword':pw},token,201)
 session=login(email,pw);nextpw=pw+'x';req('POST','/api/auth/change-initial-password',{'newPassword':nextpw},session['accessToken'],204)
 users[role]=(login(email,nextpw)['accessToken'],u,email,nextpw)
viewer=users['VIEWER'][0];editor=users['EDITOR'][0]
req('GET','/api/appeals',token=viewer);req('GET','/api/admin/users',token=viewer,expect=403)
req('POST','/api/centers',{'code':'DENIED','name':'Denied','slaHours':24,'active':True},editor,403);check('VIEWER / EDITOR / ADMIN separation')
d=req('POST','/api/centers',{'code':'TEST-'+suffix,'name':'Проверка','slaHours':24,'active':True},token,201)
# Directory CRUD and role/conflict/reference checks.
cp={'fullName':'Directory '+suffix,'phone':'+7'+str(int(suffix,16)).zfill(10),'email':'directory@example.org'}
req('POST','/api/directory/clients',cp,viewer,403)
c=req('POST','/api/directory/clients',cp,editor,201)
req('POST','/api/directory/clients',cp,editor,409)
formatted_phone=cp['phone'][:2]+' ('+cp['phone'][2:5]+') '+cp['phone'][5:]
req('POST','/api/directory/clients',{**cp,'phone':formatted_phone},editor,409)
phone_variants=[cp['phone'],formatted_phone,'8'+cp['phone'][2:]]
for i,variant in enumerate(phone_variants):
 probe=req('POST','/api/appeals',{'externalId':'PHONE-'+suffix+'-'+str(i),'topic':'Phone normalization regression','clientName':cp['fullName'],'category':'TECH','email':cp['email'],'phone':variant,'centerId':d['id'],'status':'NEW','impactScore':10,'urgent':False,'tags':[]},editor,201)
 assert probe['phone']==cp['phone'],probe['phone']
 req('DELETE','/api/appeals/'+probe['id']+'/permanent',token=token,expect=204,headers={'X-Confirm-Delete':'PERMANENTLY_DELETE'})
assert req('GET','/api/directory/clients?q='+suffix,token=viewer)['totalElements']==1
check('phone formats reuse one client in appeals and reject duplicate directory entries')
cp={**cp,'fullName':'Updated '+suffix,'version':c['version']}
c=req('PUT',f'/api/directory/clients/{c["id"]}',cp,editor)
req('PUT',f'/api/directory/clients/{c["id"]}',cp,editor,409)
assert req('GET','/api/directory/clients?q='+suffix,token=viewer)['totalElements']==1
req('GET','/api/directory/clients?size=101',token=viewer,expect=400)
req('DELETE',f'/api/directory/clients/{c["id"]}?version={c["version"]}',token=editor,expect=403)
req('DELETE',f'/api/directory/clients/{c["id"]}?version={c["version"]}',token=token,expect=204)
op={'fullName':'Operator '+suffix,'login':'op_'+suffix,'centerId':d['id'],'active':True}
req('POST','/api/directory/operators',op,editor,403)
o=req('POST','/api/directory/operators',op,token,201)
req('POST','/api/directory/operators',op,token,409)
op={**op,'active':False,'version':o['version']}
o=req('PUT',f'/api/directory/operators/{o["id"]}',op,token)
req('PUT',f'/api/directory/operators/{o["id"]}',op,token,409)
assert req('GET','/api/directory/operators?q='+suffix,token=viewer)['content'][0]['active']==False
req('DELETE',f'/api/directory/operators/{o["id"]}?version={o["version"]}',token=token,expect=204)
for kind in ['clients','operators']:
 rows=req('GET','/api/directory/'+kind+'?size=100',token=token)['content']
 legacy=next(x for x in rows if x['id']==1)
 req('DELETE',f'/api/directory/{kind}/1?version={legacy["version"]}',token=token,expect=409)
check('client/operator CRUD, roles, versions, duplicates, search and protected history')
payload={'externalId':'TEST-'+suffix,'topic':'Проверка сети','clientName':'Тестовый клиент','category':'TECH','email':'client@example.org','phone':'+79991112233','centerId':d['id'],'status':'NEW','impactScore':85,'urgent':True,'tags':['сеть'],'version':None,'operatorId':1}
req('POST','/api/appeals',payload,viewer,403)
req('POST','/api/appeals',{},editor,400)
appeal=req('POST','/api/appeals',payload,editor,201);aid=appeal['id'];assert appeal['priorityLevel']=='CRITICAL';check('create + validation + computed priority')
changed={**payload,'topic':'Изменено','version':appeal['version']}
updated=req('PUT','/api/appeals/'+aid,changed,editor);assert updated['version']>appeal['version']
req('PUT','/api/appeals/'+aid,changed,editor,409);check('optimistic version conflict 409')
req('PUT','/api/appeals/'+aid,{**changed,'status':'CLOSED','version':updated['version']},editor,400)
for status in ['IN_PROGRESS','ESCALATED','RESOLVED','CLOSED']:
 updated=req('PUT','/api/appeals/'+aid,{**changed,'status':status,'version':updated['version']},editor)
check('status chain and rejected jump')
from urllib.parse import quote
for q in ['TEST-'+suffix,'center:TEST-'+suffix,'status:CLOSED','urgency:true','priority:not_applicable','tag:сеть','impact>=80','archived:all']:
 page=req('GET','/api/appeals?q='+quote(q)+'&size=2&sort=impact,desc',token=viewer);assert 'content' in page
req('GET','/api/appeals?sort=center,asc',token=viewer)
req('GET','/api/appeals?size=101',token=viewer,expect=400);check('search, seven filters, sorting and bounded pagination')
n=req('POST',f'/api/appeals/{aid}/notes',{'body':'Проверено','pinned':True},editor,201)
n=req('PUT',f'/api/appeals/{aid}/notes/{n["id"]}',{'body':'Исправлено','pinned':False,'version':n['version']},editor)
assert req('GET',f'/api/appeals/{aid}/notes',token=viewer)[0]['body']=='Исправлено'
req('DELETE',f'/api/appeals/{aid}/notes/{n["id"]}',token=editor,expect=204);check('notes CRUD, author and pin')
req('DELETE','/api/appeals/'+aid,token=editor)
assert req('GET','/api/appeals/'+aid,token=viewer)['archived']
req('POST','/api/appeals/'+aid+'/restore',token=editor)
bulk=req('POST','/api/appeals/bulk',{'ids':[aid,str(uuid.uuid4())],'action':'ADD_TAG','tag':'batch'},editor);assert bulk['succeeded']==1 and len(bulk['failures'])==1
check('archive, restore and partial bulk result')
headers=['externalId','clientName','topic','category','email','phone','dueDate','center','status','impactScore','urgent','tags','operatorId']
s=io.StringIO();w=csv.writer(s);w.writerow(headers);w.writerow(['CSV-'+suffix,'Клиент','CSV тема','TECH','csv@example.org','+79991112234','',d['code'],'NEW',30,'false','csv',''])
def upload(action,mode='CREATE_ONLY',content=None):
 boundary='kr3'+suffix
 raw=('--'+boundary+'\r\nContent-Disposition: form-data; name="file"; filename="test.csv"\r\nContent-Type: text/csv\r\n\r\n').encode()+(content or s.getvalue().encode('utf-8-sig'))+('\r\n--'+boundary+'--\r\n').encode()
 return req('POST','/api/appeals/import/'+action+'?mode='+mode,raw,editor,headers={'Content-Type':'multipart/form-data; boundary='+boundary})
p=upload('preview');assert p['validRows']==1 and p['invalidRows']==0,p
p=upload('apply');assert p['created']==1,p
p=upload('preview');assert p['invalidRows']==1,p
p=upload('apply','UPSERT_BY_EXTERNAL_ID');assert p['updated']==1,p
export=req('GET','/api/appeals/export?q='+quote('CSV-'+suffix),token=editor);assert ('CSV-'+suffix).encode() in export
p=upload('preview','UPSERT_BY_EXTERNAL_ID',export);assert p['validRows']==1,p
check('CSV preview/apply, two modes, BOM and export roundtrip')
summary=req('GET','/api/analytics/summary',token=viewer);assert summary['total']>=2
check('analytics summary')
req('PATCH',f'/api/centers/{d["id"]}/active?active=false&version={d["version"]}',token=token)
check('reference deactivate')
u=users['VIEWER'][1]
req('PUT','/api/admin/users/'+u['id'],{'email':u['email'],'displayName':'Disabled','enabled':False,'roles':['VIEWER'],'version':req('GET','/api/admin/users?size=100',token=token)['content'][[x['id'] for x in req('GET','/api/admin/users?size=100',token=token)['content']].index(u['id'])]['version']},token)
req('POST','/api/auth/login',{'email':users['VIEWER'][2],'password':users['VIEWER'][3]},expect=401)
check('disabled account denied')
audit=req('GET','/api/admin/audit?size=10&sort=occurred,desc',token=token);assert audit['totalElements']>0
assert req('GET','/v3/api-docs',token=token)['openapi'];check('audit + OpenAPI')
req('DELETE','/api/appeals/'+aid+'/permanent',token=editor,expect=403)
req('DELETE','/api/appeals/'+aid+'/permanent',token=token,expect=204,headers={'X-Confirm-Delete':'PERMANENTLY_DELETE'})
req('GET','/api/appeals/'+aid,token=token,expect=404);check('admin permanent delete and 404')
req('POST','/api/auth/logout',{'refreshToken':refresh['refreshToken']},expect=204)
req('POST','/api/auth/refresh',{'refreshToken':refresh['refreshToken']},expect=401);check('logout revokes refresh session')
(root/'backend/ACCEPTANCE_RESULT.txt').write_text('\n'.join('PASS '+c for c in checks)+'\n')
print('ALL',len(checks),'ACCEPTANCE GROUPS PASSED')
