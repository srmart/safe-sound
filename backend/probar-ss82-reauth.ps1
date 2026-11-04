
# ==========================================
# SS-82 - PRUEBA DE FUERZA BRUTA EN REAUTENTICACION
# Compatible con Windows PowerShell 5.1
# ==========================================

$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"

$email = "ss82.reauth.$([guid]::NewGuid().ToString('N').Substring(0,8))@test.com"
$username = "ss82_reauth_$([guid]::NewGuid().ToString('N').Substring(0,8))"

$passwordCorrecta = "PruebaSegura123!"
$passwordIncorrecta = "PasswordIncorrecta123!"

# Ejecuta una peticion y devuelve el codigo HTTP,
# incluso cuando el servidor responde 4xx o 5xx.
function Invoke-Api {
    param(
        [string]$Uri,
        [string]$Body,
        [string]$Token
    )

    try {
        $headers = @{}

        if ($Token) {
            $headers["Authorization"] = "Bearer $Token"
        }

        $response = Invoke-WebRequest `
            -Uri $Uri `
            -Method POST `
            -ContentType "application/json" `
            -Headers $headers `
            -Body $Body `
            -UseBasicParsing `
            -ErrorAction Stop

        return [int]$response.StatusCode
    }
    catch {
        if ($null -ne $_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }

        Write-Host "Error de conexion: $($_.Exception.Message)" -ForegroundColor Red
        return 0
    }
}

Write-Host "`n==========================================" -ForegroundColor Cyan
Write-Host " SS-82 - PRUEBA DE REAUTENTICACION" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# ==========================================
# 1. REGISTRAR CUENTA
# ==========================================

Write-Host "`n========== 1. REGISTRO ==========" -ForegroundColor Cyan

$registro = @{
    email    = $email
    username = $username
    password = $passwordCorrecta
} | ConvertTo-Json

$statusRegistro = Invoke-Api `
    -Uri "$baseUrl/auth/register" `
    -Body $registro

Write-Host "Registro: HTTP $statusRegistro"

if (($statusRegistro -ne 201) -and ($statusRegistro -ne 200)) {
    Write-Host "No se pudo registrar la cuenta. Prueba cancelada." -ForegroundColor Red
    exit 1
}

# ==========================================
# 2. LOGIN PARA OBTENER JWT
# ==========================================

Write-Host "`n========== 2. OBTENER TOKEN JWT ==========" -ForegroundColor Cyan

$login = @{
    email    = $email
    password = $passwordCorrecta
} | ConvertTo-Json

try {
    $respuestaLogin = Invoke-RestMethod `
        -Uri "$baseUrl/auth/login" `
        -Method POST `
        -ContentType "application/json" `
        -Body $login `
        -ErrorAction Stop

    $token = $respuestaLogin.accessToken
}
catch {
    Write-Host "No se pudo iniciar sesion: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

if ([string]::IsNullOrWhiteSpace($token)) {
    Write-Host "El login no devolvio un accessToken." -ForegroundColor Red
    exit 1
}

Write-Host "JWT obtenido correctamente." -ForegroundColor Green

# ==========================================
# 3. CINCO INTENTOS FALLIDOS DE REAUTENTICACION
# ==========================================

Write-Host "`n========== 3. INTENTOS FALLIDOS ==========" -ForegroundColor Cyan

$bodyIncorrecto = @{
    password = $passwordIncorrecta
} | ConvertTo-Json

$resultados = @()

for ($i = 1; $i -le 6; $i++) {

    $status = Invoke-Api `
        -Uri "$baseUrl/auth/reautenticar" `
        -Body $bodyIncorrecto `
        -Token $token

    $resultados += $status

    if ($status -eq 401) {
        $color = "Yellow"
    }
    elseif ($status -eq 429) {
        $color = "Green"
    }
    else {
        $color = "Red"
    }

    Write-Host "Intento ${i}: HTTP $status" -ForegroundColor $color
}

# ==========================================
# 4. PASSWORD CORRECTA DURANTE EL BLOQUEO
# ==========================================

Write-Host "`n========== 4. PASSWORD CORRECTA ==========" -ForegroundColor Cyan

$bodyCorrecto = @{
    password = $passwordCorrecta
} | ConvertTo-Json

$statusCorrecto = Invoke-Api `
    -Uri "$baseUrl/auth/reautenticar" `
    -Body $bodyCorrecto `
    -Token $token

Write-Host "Reautenticacion correcta durante bloqueo: HTTP $statusCorrecto"

# ==========================================
# 5. RESULTADO
# ==========================================

Write-Host "`n========== 5. RESULTADO ==========" -ForegroundColor Cyan

$primerosCincoCorrectos = (
    ($resultados | Select-Object -First 5 | Where-Object { $_ -eq 401 }).Count -eq 5
)

$sextoBloqueado = (
    $resultados.Count -eq 6 -and $resultados[5] -eq 429
)

$passwordCorrectaBloqueada = ($statusCorrecto -eq 429)

if ($primerosCincoCorrectos -and $sextoBloqueado -and $passwordCorrectaBloqueada) {

    Write-Host "PASS: Los primeros cinco intentos reciben HTTP 401." -ForegroundColor Green
    Write-Host "PASS: El sexto intento recibe HTTP 429." -ForegroundColor Green
    Write-Host "PASS: La contraseña correcta tampoco supera el bloqueo." -ForegroundColor Green

    Write-Host "`nSS-82: PRUEBA DE REAUTENTICACION SUPERADA." -ForegroundColor Green
}
else {

    Write-Host "FAIL: Los resultados no coinciden con lo esperado." -ForegroundColor Red

    Write-Host "Esperado: 401, 401, 401, 401, 401, 429 y luego 429."
    Write-Host "Obtenido: $($resultados -join ', ') y luego $statusCorrecto."
}

Write-Host "`nCuenta de prueba: $email"
Write-Host "Usuario: $username"
Write-Host "La cuenta queda guardada en la base de datos."
