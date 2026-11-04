
# ==========================================
# SS-82 - PRUEBA DE PROTECCION CONTRA FUERZA BRUTA
# Compatible con Windows PowerShell 5.1
# ==========================================

$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"

$email = "ss82.test.$([guid]::NewGuid().ToString('N').Substring(0,8))@test.com"
$username = "ss82_$([guid]::NewGuid().ToString('N').Substring(0,8))"

$passwordCorrecta = "PruebaSegura123!"
$passwordIncorrecta = "PasswordIncorrecta123!"

# Funcion para obtener el codigo HTTP incluso ante errores 4xx/5xx.
function Invoke-Api {
    param(
        [string]$Uri,
        [string]$Body
    )

    try {
        $response = Invoke-WebRequest `
            -Uri $Uri `
            -Method POST `
            -ContentType "application/json" `
            -Body $Body `
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
Write-Host " SS-82 - PRUEBA DE FUERZA BRUTA" -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

Write-Host "Backend: $baseUrl"
Write-Host "Email de prueba: $email"

# ==========================================
# 1. REGISTRO
# ==========================================

Write-Host "`n========== 1. REGISTRO ==========" -ForegroundColor Cyan

$registro = @{
    email    = $email
    username = $username
    password = $passwordCorrecta
} | ConvertTo-Json

$statusRegistro = Invoke-Api -Uri "$baseUrl/auth/register" -Body $registro

Write-Host "Registro: HTTP $statusRegistro"

if (($statusRegistro -ne 201) -and ($statusRegistro -ne 200)) {
    Write-Host "No se pudo registrar la cuenta. Se cancela la prueba." -ForegroundColor Red
    exit 1
}

# ==========================================
# 2. SEIS INTENTOS CON PASSWORD INCORRECTA
# ==========================================

Write-Host "`n========== 2. FUERZA BRUTA ==========" -ForegroundColor Cyan

$bodyIncorrecto = @{
    email    = $email
    password = $passwordIncorrecta
} | ConvertTo-Json

$resultados = @()

for ($i = 1; $i -le 6; $i++) {

    $status = Invoke-Api -Uri "$baseUrl/auth/login" -Body $bodyIncorrecto

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
# 3. PASSWORD CORRECTA DURANTE EL BLOQUEO
# ==========================================

Write-Host "`n========== 3. PASSWORD CORRECTA ==========" -ForegroundColor Cyan

$bodyCorrecto = @{
    email    = $email
    password = $passwordCorrecta
} | ConvertTo-Json

$statusCorrecto = Invoke-Api -Uri "$baseUrl/auth/login" -Body $bodyCorrecto

Write-Host "Login con contraseña correcta: HTTP $statusCorrecto"

# ==========================================
# 4. RESULTADOS
# ==========================================

Write-Host "`n========== 4. RESULTADO ==========" -ForegroundColor Cyan

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
    Write-Host "PASS: La contraseña correcta tampoco permite entrar durante el bloqueo." -ForegroundColor Green

    Write-Host "`nSS-82: PRUEBA SUPERADA." -ForegroundColor Green
}
else {

    Write-Host "FAIL: Los resultados no coinciden con lo esperado." -ForegroundColor Red

    Write-Host "Esperado: 401, 401, 401, 401, 401, 429 y luego 429."
    Write-Host "Obtenido: $($resultados -join ', ') y luego $statusCorrecto."
}

Write-Host "`nCuenta de prueba: $email"
Write-Host "Usuario: $username"
Write-Host "La cuenta queda guardada en la base de datos."
