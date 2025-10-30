# API de Pagamentos - Exemplos de Requisições

## 📋 Informações
- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **Porta**: 8080

---

## ✅ 1. Criar Pagamento - À Vista

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -d '{
    "valor": 150.00,
    "moeda": "BRL",
    "estabelecimento": "Supermercado Sicredi",
    "tipoPagamento": "AVISTA",
    "parcelas": 1,
    "cartaoMascarado": "4111********1111"
  }'
```

**PowerShell:**
```powershell
$body = @{
    valor = 150.00
    moeda = "BRL"
    estabelecimento = "Supermercado Sicredi"
    tipoPagamento = "AVISTA"
    parcelas = 1
    cartaoMascarado = "4111********1111"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method POST -Body $body -ContentType "application/json"
```

---

## ✅ 2. Criar Pagamento - Parcelado (Loja)

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -d '{
    "valor": 1200.00,
    "moeda": "BRL",
    "estabelecimento": "Magazine Luiza",
    "tipoPagamento": "PARCELADO_LOJA",
    "parcelas": 6,
    "cartaoMascarado": "5555********4444"
  }'
```

**PowerShell:**
```powershell
$body = @{
    valor = 1200.00
    moeda = "BRL"
    estabelecimento = "Magazine Luiza"
    tipoPagamento = "PARCELADO_LOJA"
    parcelas = 6
    cartaoMascarado = "5555********4444"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method POST -Body $body -ContentType "application/json"
```

---

## ✅ 3. Criar Pagamento - Parcelado (Emissor)

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -d '{
    "valor": 2500.50,
    "moeda": "BRL",
    "estabelecimento": "Apple Store",
    "tipoPagamento": "PARCELADO_EMISSOR",
    "parcelas": 12,
    "cartaoMascarado": "3782********1007"
  }'
```

**PowerShell:**
```powershell
$body = @{
    valor = 2500.50
    moeda = "BRL"
    estabelecimento = "Apple Store"
    tipoPagamento = "PARCELADO_EMISSOR"
    parcelas = 12
    cartaoMascarado = "3782********1007"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method POST -Body $body -ContentType "application/json"
```

---

## 🔍 4. Consultar Pagamento por ID

**Substitua `{idTransacao}` pelo UUID retornado na criação**

```bash
curl -X GET http://localhost:8080/pagamentos/{idTransacao}
```

**PowerShell:**
```powershell
$idTransacao = "SEU_UUID_AQUI"
Invoke-RestMethod -Uri "http://localhost:8080/pagamentos/$idTransacao" -Method GET
```

---

## 📋 5. Listar Todos os Pagamentos

```bash
curl -X GET http://localhost:8080/pagamentos
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method GET
```

---

## 🎯 6. Listar Pagamentos por Status

### Autorizados
```bash
curl -X GET http://localhost:8080/pagamentos/status/AUTORIZADO
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/pagamentos/status/AUTORIZADO" -Method GET
```

### Negados
```bash
curl -X GET http://localhost:8080/pagamentos/status/NEGADO
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/pagamentos/status/NEGADO" -Method GET
```

### Pendentes
```bash
curl -X GET http://localhost:8080/pagamentos/status/PENDENTE
```

**PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/pagamentos/status/PENDENTE" -Method GET
```

---

## ❌ Validações e Tratamento de Erros

### 1. Recurso não encontrado (404)

**Pagamento inexistente:**
```powershell
# Tentar buscar pagamento com ID inválido
$response = try {
    Invoke-WebRequest -Uri "http://localhost:8080/pagamentos/99999999-9999-9999-9999-999999999999" -Method GET -ErrorAction Stop
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
  "mensagem": "Pagamento não encontrado(a) com identificador: 99999999-9999-9999-9999-999999999999",
  "caminho": "/pagamentos/99999999-9999-9999-9999-999999999999",
  "traceId": "c90a8a91"
}
```

---

### 2. Validação de campos (400)

**Valor negativo:**
```powershell
$body = @{
    valor = -100.00
    moeda = "BRL"
    estabelecimento = "Teste"
    tipoPagamento = "AVISTA"
    parcelas = 1
    cartaoMascarado = "4111********1111"
} | ConvertTo-Json

$response = try {
    Invoke-WebRequest -Uri "http://localhost:8080/pagamentos" -Method POST -Body $body -ContentType "application/json" -ErrorAction Stop
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
  "timestamp": "2025-10-29T21:31:00",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Dados inválidos na requisição",
  "caminho": "/pagamentos",
  "errosValidacao": [
    {
      "campo": "valor",
      "valorRejeitado": -100.0,
      "mensagem": "Valor mínimo é R$ 0,01"
    }
  ],
  "traceId": "3121e118"
}
```

**Parcelas inválidas para À Vista:**
```powershell
$body = @{
    valor = 100.00
    moeda = "BRL"
    estabelecimento = "Teste"
    tipoPagamento = "AVISTA"
    parcelas = 3
    cartaoMascarado = "4111********1111"
} | ConvertTo-Json

$response = try {
    Invoke-WebRequest -Uri "http://localhost:8080/pagamentos" -Method POST -Body $body -ContentType "application/json" -ErrorAction Stop
} catch {
    $_.Exception.Response
}

$reader = [System.IO.StreamReader]::new($response.GetResponseStream())
$json = $reader.ReadToEnd() | ConvertFrom-Json
$json.errosValidacao | Format-Table campo, mensagem -AutoSize
```

**Resposta esperada:** 400 Bad Request - "Pagamento à vista deve ter apenas 1 parcela"

**Cartão mascarado inválido:**
```powershell
$body = @{
    valor = 100.00
    moeda = "BRL"
    estabelecimento = "Teste"
    tipoPagamento = "AVISTA"
    parcelas = 1
    cartaoMascarado = "123"  # Muito curto
} | ConvertTo-Json

$response = try {
    Invoke-WebRequest -Uri "http://localhost:8080/pagamentos" -Method POST -Body $body -ContentType "application/json" -ErrorAction Stop
} catch {
    $_.Exception.Response
}

$reader = [System.IO.StreamReader]::new($response.GetResponseStream())
$json = $reader.ReadToEnd() | ConvertFrom-Json
$json.errosValidacao | Format-Table campo, mensagem -AutoSize
```

**Resposta esperada:** 400 Bad Request - "Cartão mascarado deve estar no formato: 4111********1111"

---

### 3. Tipo de argumento inválido (400)

**Status com valor inválido:**
```powershell
# Passar valor inválido para enum
$response = try {
    Invoke-WebRequest -Uri "http://localhost:8080/pagamentos/status/INVALIDO" -Method GET -ErrorAction Stop
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
  "mensagem": "Parâmetro 'status' com valor 'INVALIDO' não pode ser convertido para o tipo 'StatusPagamento'",
  "caminho": "/pagamentos/status/INVALIDO",
  "traceId": "a1b2c3d4"
}
```

---

## 📊 Estrutura de Resposta de Erro

Todas as respostas de erro seguem o padrão do Global Exception Handler:

```typescript
{
  timestamp: string;        // ISO 8601 format
  status: number;           // HTTP status code
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
  "idTransacao": "123e4567-e89b-12d3-a456-426614174000",
  "status": "AUTORIZADO",
  "valor": 150.00,
  "moeda": "BRL",
  "dataHora": "2025-10-29T20:35:00.123-03:00",
  "estabelecimento": "Supermercado Sicredi",
  "tipoPagamento": "AVISTA",
  "parcelas": 1,
  "nsu": "0123456789",
  "codigoAutorizacao": "123456",
  "cartaoMascarado": "4111********1111",
  "criadoEm": "2025-10-29T20:35:00.123-03:00",
  "atualizadoEm": "2025-10-29T20:35:00.456-03:00"
}
```

---

## 🔗 Endpoints Disponíveis

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/pagamentos` | Criar novo pagamento |
| GET | `/pagamentos/{id}` | Buscar pagamento por ID |
| GET | `/pagamentos` | Listar todos os pagamentos |
| GET | `/pagamentos/status/{status}` | Listar por status |
