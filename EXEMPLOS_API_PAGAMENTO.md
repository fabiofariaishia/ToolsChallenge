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

## ❌ Validações - Exemplos de Erros

### 1. Valor inválido (negativo)
```powershell
$body = @{
    valor = -10.00
    moeda = "BRL"
    estabelecimento = "Teste"
    tipoPagamento = "AVISTA"
    parcelas = 1
    cartaoMascarado = "4111********1111"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method POST -Body $body -ContentType "application/json"
```
**Esperado**: 400 Bad Request - "Valor mínimo é R$ 0,01"

### 2. Parcelas inválidas para À Vista
```powershell
$body = @{
    valor = 100.00
    moeda = "BRL"
    estabelecimento = "Teste"
    tipoPagamento = "AVISTA"
    parcelas = 3
    cartaoMascarado = "4111********1111"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method POST -Body $body -ContentType "application/json"
```
**Esperado**: 400 Bad Request - "Pagamento à vista deve ter apenas 1 parcela"

### 3. Cartão mascarado inválido
```powershell
$body = @{
    valor = 100.00
    moeda = "BRL"
    estabelecimento = "Teste"
    tipoPagamento = "AVISTA"
    parcelas = 1
    cartaoMascarado = "4111-1111-1111-1111"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method POST -Body $body -ContentType "application/json"
```
**Esperado**: 400 Bad Request - "Cartão mascarado deve estar no formato: 4111********1111"

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
