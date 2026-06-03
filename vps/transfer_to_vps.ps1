# =============================================================================
#  L2J Zona Zero - Transferir proyecto al VPS desde Windows (PowerShell)
#
#  Uso:
#    .\transfer_to_vps.ps1 -VpsIP "1.2.3.4" -VpsUser "l2j"
#
#  Requiere: OpenSSH instalado en Windows (viene por defecto en Windows 10+)
# =============================================================================
param(
    [Parameter(Mandatory=$true)]
    [string]$VpsIP,

    [string]$VpsUser = "l2j",
    [string]$ProjectPath = "C:\Users\david\OneDrive\Desktop\l2j",
    [string]$RemotePath = "/opt/l2j",
    [switch]$FullTransfer,
    [switch]$GameOnly,
    [switch]$ConfigOnly
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║       L2J Zona Zero - Transferencia al VPS                   ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host "  VPS:      $VpsUser@$VpsIP"
Write-Host "  Origen:   $ProjectPath"
Write-Host "  Destino:  $RemotePath"
Write-Host ""

# ── Verificar SSH ─────────────────────────────────────────────────────────────
if (-not (Get-Command ssh -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] SSH no encontrado. Instalar OpenSSH desde Configuracion > Apps > Features Opcionales" -ForegroundColor Red
    exit 1
}

# ── Funcion de copia via SCP ──────────────────────────────────────────────────
function Copy-ToVps {
    param([string]$Local, [string]$Remote, [string]$Label)
    Write-Host "  Copiando $Label..." -ForegroundColor Yellow
    scp -r "$Local" "${VpsUser}@${VpsIP}:${Remote}"
    Write-Host "  [OK] $Label" -ForegroundColor Green
}

function Run-OnVps {
    param([string]$Command)
    ssh "${VpsUser}@${VpsIP}" $Command
}

# ── Transferencia completa ────────────────────────────────────────────────────
if ($FullTransfer -or (-not $GameOnly -and -not $ConfigOnly)) {
    Write-Host "[1/6] Copiando GameServer..." -ForegroundColor Cyan
    Copy-ToVps "$ProjectPath\game"      "$RemotePath/"     "game/"

    Write-Host "[2/6] Copiando LoginServer..." -ForegroundColor Cyan
    Copy-ToVps "$ProjectPath\login"     "$RemotePath/"     "login/"

    Write-Host "[3/6] Copiando librerias..." -ForegroundColor Cyan
    Copy-ToVps "$ProjectPath\libs"      "$RemotePath/"     "libs/"

    Write-Host "[4/6] Copiando instalador DB..." -ForegroundColor Cyan
    Copy-ToVps "$ProjectPath\db_installer" "$RemotePath/"  "db_installer/"

    Write-Host "[5/6] Copiando scripts VPS..." -ForegroundColor Cyan
    Copy-ToVps "$ProjectPath\vps\install.sh"        "$RemotePath/scripts/"  "install.sh"
    Copy-ToVps "$ProjectPath\vps\configure_l2j.sh"  "$RemotePath/scripts/"  "configure_l2j.sh"
    Copy-ToVps "$ProjectPath\vps\import_db.sh"      "$RemotePath/scripts/"  "import_db.sh"

    Write-Host "[6/6] Copiando web panel..." -ForegroundColor Cyan
    Copy-ToVps "$ProjectPath\vps\web"   "$RemotePath/"     "web/"

    Write-Host ""
    Write-Host "  Ajustando permisos en VPS..." -ForegroundColor Yellow
    Run-OnVps "sudo chown -R l2j:l2j /opt/l2j && sudo chmod +x /opt/l2j/scripts/*.sh"
}

# ── Solo game data (para updates rapidos) ────────────────────────────────────
if ($GameOnly) {
    Write-Host "[UPDATE] Copiando solo archivos del game..." -ForegroundColor Cyan
    Copy-ToVps "$ProjectPath\game\data"   "$RemotePath/game/"  "game/data/"
    Copy-ToVps "$ProjectPath\libs"        "$RemotePath/"        "libs/ (JARs)"
    Write-Host "  Recordar reiniciar GameServer en el VPS" -ForegroundColor Yellow
}

# ── Solo configs ──────────────────────────────────────────────────────────────
if ($ConfigOnly) {
    Write-Host "[CONFIG] Copiando solo configuraciones..." -ForegroundColor Cyan
    Copy-ToVps "$ProjectPath\game\config"  "$RemotePath/game/"  "game/config/"
    Copy-ToVps "$ProjectPath\login\config" "$RemotePath/login/" "login/config/"
    Run-OnVps "sudo /opt/l2j/scripts/configure_l2j.sh"
}

Write-Host ""
Write-Host "╔══════════════════════════════════════════════════════════════╗" -ForegroundColor Green
Write-Host "║  Transferencia completada                                    ║" -ForegroundColor Green
Write-Host "╠══════════════════════════════════════════════════════════════╣" -ForegroundColor Green
Write-Host "║  Proximos pasos en el VPS:                                   ║" -ForegroundColor Green
Write-Host "║    sudo /opt/l2j/scripts/configure_l2j.sh                    ║" -ForegroundColor Green
Write-Host "║    sudo /opt/l2j/scripts/import_db.sh                        ║" -ForegroundColor Green
Write-Host "║    sudo systemctl start l2j-login l2j-game                   ║" -ForegroundColor Green
Write-Host "╚══════════════════════════════════════════════════════════════╝" -ForegroundColor Green
