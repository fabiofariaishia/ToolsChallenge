# API de Estornos - Exemplos de Requisições

## 📋 Informações
- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **Porta**: 8080

---

## ✅ 1. Criar Estorno

**Pré-requisito**: Ter um pagamento AUTORIZADO criado nos últimos 24h.

**PowerShell:**
```powershell
# Primeiro, criar um pagamento e guardar o ID
$pagamento = @{
    valor = 250.00
    moeda = "BRL"
    estabelecimento = "Loja Exemplo"
    tipoPagamento = "AVISTA"
    parcelas = 1
    cartaoMascarado = "4111********1111"
} | ConvertTo-Json

$responsePagamento = Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method POST -Body $pagamento -ContentType "application/json"
$idTransacao = $responsePagamento.idTransacao
Write-Host "Pagamento criado: $idTransacao - Valor: R$ $($responsePagamento.valor)"

# Agora criar o estorno
$estorno = @{
    idTransacao = $idTransacao
    valor = $responsePagamento.valor
    motivo = "Cliente solicitou cancelamento"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/estornos" -Method POST -Body $estorno -ContentType "application/json"
```

---

## 🔍 2. Consultar Estorno por ID

```powershell
$idEstorno = "SEU_UUID_AQUI"
Invoke-RestMethod -Uri "http://localhost:8080/estornos/$idEstorno" -Method GET
```

---

## 📋 3. Listar Estornos de um Pagamento

```powershell
$idTransacao = "UUID_DO_PAGAMENTO"
Invoke-RestMethod -Uri "http://localhost:8080/estornos/pagamento/$idTransacao" -Method GET
```

---

## 📋 4. Listar Todos os Estornos

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/estornos" -Method GET
```

---

## 🎯 5. Listar Estornos por Status

### Cancelados (Aprovados)
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/estornos/status/CANCELADO" -Method GET
```

### Negados
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/estornos/status/NEGADO" -Method GET
```

### Pendentes
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/estornos/status/PENDENTE" -Method GET
```

---

## ❌ Validações e Tratamento de Erros

### 1. Recurso não encontrado (404)

**Estorno inexistente:**
```powershell
$response = try {
    Invoke-WebRequest -Uri "http://localhost:8080/estornos/99999999-9999-9999-9999-999999999999" -Method GET -ErrorAction Stop
} catch {
    $_.Exception.Response
}

$reader = [System.IO.StreamReader]::new($response.GetResponseStream())
$body = $reader.ReadToEnd() | ConvertFrom-Json
$body | Format-List
```

**Resposta esperada:**
```json
{
  "timestamp": "2025-10-29T21:30:00",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Estorno não encontrado(a) com identificador: 99999999-9999-9999-9999-999999999999",
  "caminho": "/estornos/99999999-9999-9999-9999-999999999999",
  "traceId": "f7e8d9c0"
}
```

**Pagamento inexistente:**
```powershell
$estorno = @{
    idTransacao = "99999999-9999-9999-9999-999999999999"
    valor = 100.00
    motivo = "Teste"
} | ConvertTo-Json

$response = try {
    Invoke-WebRequest -Uri "http://localhost:8080/estornos" -Method POST -Body $estorno -ContentType "application/json" -ErrorAction Stop
} catch {
    $_.Exception.Response
}

$reader = [System.IO.StreamReader]::new($response.GetResponseStream())
$body = $reader.ReadToEnd() | ConvertFrom-Json
Write-Host "Mensagem: $($body.mensagem)" -ForegroundColor White
```

**Resposta esperada:**
```json
{
  "timestamp": "2025-10-29T21:31:00",
  "status": 404,
  "erro": "Not Found",
  "mensagem": "Pagamento não encontrado(a) com identificador: 99999999-9999-9999-9999-999999999999",
  "caminho": "/estornos",
  "traceId": "a1b2c3d4"
}
```

---

### 2. Regras de negócio (400)

**Estorno de pagamento não autorizado:**
```powershell
$estorno = @{
    idTransacao = "TXN-002-2025-PARCELADO-LOJA"  # Pagamento NEGADO nos dados seed
    valor = 299.99
    motivo = "Teste validação"
} | ConvertTo-Json

$response = try {
    Invoke-WebRequest -Uri "http://localhost:8080/estornos" -Method POST -Body $estorno -ContentType "application/json" -ErrorAction Stop
} catch {
    $_.Exception.Response
}

$reader = [System.IO.StreamReader]::new($response.GetResponseStream())
$body = $reader.ReadToEnd() | ConvertFrom-Json
Write-Host "Tipo: $($body.erro)" -ForegroundColor Cyan
Write-Host "Mensagem: $($body.mensagem)" -ForegroundColor White
Write-Host "TraceId: $($body.traceId)" -ForegroundColor Gray
```

**Resposta esperada:**
```json
{
  "timestamp": "2025-10-29T21:32:00",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Apenas pagamentos AUTORIZADOS podem ser estornados. Status atual: NEGADO",
  "caminho": "/estornos",
  "traceId": "i9j0k1l2"
}
```

**Estorno parcial (não permitido):**
```powershell
# Primeiro criar um pagamento AUTORIZADO
$pagamento = @{
    valor = 500.00
    moeda = "BRL"
    estabelecimento = "Loja Teste"
    tipoPagamento = "AVISTA"
    parcelas = 1
    cartaoMascarado = "4111********1111"
} | ConvertTo-Json

$respPagamento = Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method POST -Body $pagamento -ContentType "application/json"

# Tentar estornar apenas metade do valor
$estorno = @{
    idTransacao = $respPagamento.idTransacao
    valor = 250.00  # Metade do valor
    motivo = "Estorno parcial"
} | ConvertTo-Json

$response = try {
    Invoke-WebRequest -Uri "http://localhost:8080/estornos" -Method POST -Body $estorno -ContentType "application/json" -ErrorAction Stop
} catch {
    $_.Exception.Response
}

$reader = [System.IO.StreamReader]::new($response.GetResponseStream())
$body = $reader.ReadToEnd() | ConvertFrom-Json
Write-Host "Mensagem: $($body.mensagem)" -ForegroundColor White
```

**Resposta esperada:**
```json
{
  "timestamp": "2025-10-29T21:33:00",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Estorno parcial não permitido. Valor do pagamento: R$ 500,00",
  "caminho": "/estornos",
  "traceId": "e5f6g7h8"
}
```

**Estorno duplicado:**
```powershell
# Criar pagamento
$pagamento = @{
    valor = 300.00
    moeda = "BRL"
    estabelecimento = "Loja Teste"
    tipoPagamento = "AVISTA"
    parcelas = 1
    cartaoMascarado = "4111********1111"
} | ConvertTo-Json

$respPagamento = Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method POST -Body $pagamento -ContentType "application/json"

# Primeiro estorno (deve funcionar)
$estorno = @{
    idTransacao = $respPagamento.idTransacao
    valor = 300.00
    motivo = "Primeiro estorno"
} | ConvertTo-Json

$respEstorno = Invoke-RestMethod -Uri "http://localhost:8080/estornos" -Method POST -Body $estorno -ContentType "application/json"
Write-Host "Primeiro estorno: $($respEstorno.status)" -ForegroundColor Green

# Segundo estorno (deve falhar se o primeiro foi CANCELADO)
if ($respEstorno.status -eq "CANCELADO") {
    Start-Sleep -Seconds 1
    $estorno2 = @{
        idTransacao = $respPagamento.idTransacao
        valor = 300.00
        motivo = "Tentativa duplicada"
    } | ConvertTo-Json

    $response = try {
        Invoke-WebRequest -Uri "http://localhost:8080/estornos" -Method POST -Body $estorno2 -ContentType "application/json" -ErrorAction Stop
    } catch {
        $_.Exception.Response
    }

    $reader = [System.IO.StreamReader]::new($response.GetResponseStream())
    $body = $reader.ReadToEnd() | ConvertFrom-Json
    Write-Host "Mensagem: $($body.mensagem)" -ForegroundColor Yellow
}
```

**Resposta esperada:**
```json
{
  "timestamp": "2025-10-29T21:34:00",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Já existe um estorno processado para este pagamento",
  "caminho": "/estornos",
  "traceId": "k1l2m3n4"
}
```

---

### 3. Validação de campos (400)

**Valor inválido:**
```powershell
$estorno = @{
    idTransacao = "TXN-001-2025-AVISTA"
    valor = -50.00  # Valor negativo
    motivo = "Teste"
} | ConvertTo-Json

$response = try {
    Invoke-WebRequest -Uri "http://localhost:8080/estornos" -Method POST -Body $estorno -ContentType "application/json" -ErrorAction Stop
} catch {
    $_.Exception.Response
}

$reader = [System.IO.StreamReader]::new($response.GetResponseStream())
$json = $reader.ReadToEnd() | ConvertFrom-Json
Write-Host "`nErros de Validação:" -ForegroundColor Cyan
$json.errosValidacao | Format-Table campo, mensagem, valorRejeitado -AutoSize
```

**Resposta esperada:**
```json
{
  "timestamp": "2025-10-29T21:35:00",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos na requisição",
  "caminho": "/estornos",
  "errosValidacao": [
    {
      "campo": "valor",
      "valorRejeitado": -50.0,
      "mensagem": "deve ser maior que 0.01"
    }
  ],
  "traceId": "o5p6q7r8"
}
```

---

## 📊 Estrutura de Resposta de Erro

Todas as respostas de erro seguem o padrão do Global Exception Handler:

```typescript
{
  timestamp: string;        // ISO 8601 format
  status: number;           // HTTP status code (400, 404, 500)
  erro: string;             // HTTP status name
  mensagem: string;         // Mensagem principal do erro
  caminho: string;          // Path da requisição
  errosValidacao?: Array<{  // Opcional: erros de validação de campos
    campo: string;
    valorRejeitado: any;
    mensagem: string;
  }>;
  traceId: string;          // ID de rastreamento (8 chars UUID)
}
```

---

## 📊 Resposta Esperada (Sucesso)

```json
{
  "idTransacao": "9932bc4c-d33b-454b-8df4-3bca383b609c",
  "idEstorno": "abc123de-f456-7890-ghij-klmnopqrstuv",
  "status": "CANCELADO",
  "valor": 250.00,
  "dataHora": "2025-10-29T21:05:00.123-03:00",
  "nsu": "0987654321",
  "codigoAutorizacao": "654321",
  "motivo": "Cliente solicitou cancelamento",
  "criadoEm": "2025-10-29T21:05:00.123-03:00",
  "atualizadoEm": "2025-10-29T21:05:00.456-03:00"
}
```

---

## 🔗 Endpoints Disponíveis

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/estornos` | Criar novo estorno |
| GET | `/estornos/{id}` | Buscar estorno por ID |
| GET | `/estornos` | Listar todos os estornos |
| GET | `/estornos/pagamento/{idTransacao}` | Listar estornos de um pagamento |
| GET | `/estornos/status/{status}` | Listar por status |

---

## 🎯 Regras de Negócio

1. ✅ Apenas pagamentos **AUTORIZADOS** podem ser estornados
2. ✅ Apenas **estorno total** é permitido (valor = valor do pagamento)
3. ✅ Janela de **24 horas** para solicitar estorno
4. ✅ Apenas **1 estorno CANCELADO** por pagamento
5. ✅ Múltiplas tentativas **NEGADAS** são permitidas
6. ✅ Taxa de aprovação: **95%** (simulação)
