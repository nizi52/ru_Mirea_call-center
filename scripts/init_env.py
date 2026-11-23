from pathlib import Path
import secrets, base64
root=Path(__file__).resolve().parents[1]
p=root/'.env'
if p.exists():
    raise SystemExit('.env already exists; kept unchanged')
p.write_text('POSTGRES_DB=call_center\nPOSTGRES_USER=call_center_app\nPOSTGRES_PASSWORD='+secrets.token_urlsafe(32)+'\nJWT_SECRET_BASE64='+base64.b64encode(secrets.token_bytes(48)).decode()+'\nBOOTSTRAP_ADMIN_EMAIL=admin@example.org\nBOOTSTRAP_ADMIN_PASSWORD=Adm1!'+secrets.token_hex(16)+'\nAPP_PORT=8080\nPOSTGRES_PORT=55432\n')
p.chmod(0o600)
print('Created .env. Administrator credentials are in this local file.')
