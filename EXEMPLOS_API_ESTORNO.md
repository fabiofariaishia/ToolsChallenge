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

## ❌ Validações - Exemplos de Erros

### 1. Estorno de pagamento não autorizado
```powershell
$estorno = @{
    idTransacao = "ID_DE_PAGAMENTO_NEGADO"
    valor = 100.00
    motivo = "Teste"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/estornos" -Method POST -Body $estorno -ContentType "application/json"
```
**Esperado**: 409 Conflict - "Apenas pagamentos AUTORIZADOS podem ser estornados"

### 2. Valor diferente do pagamento original
```powershell
$estorno = @{
    idTransacao = "ID_VALIDO"
    valor = 50.00  # Valor parcial
    motivo = "Estorno parcial"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/estornos" -Method POST -Body $estorno -ContentType "application/json"
```
**Esperado**: 400 Bad Request - "Estorno parcial não permitido"

### 3. Pagamento não encontrado
```powershell
$estorno = @{
    idTransacao = "uuid-inexistente"
    valor = 100.00
    motivo = "Teste"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/estornos" -Method POST -Body $estorno -ContentType "application/json"
```
**Esperado**: 400 Bad Request - "Pagamento não encontrado"

### 4. Estorno duplicado
```powershell
# Tentar estornar o mesmo pagamento duas vezes
# (Primeiro estorno foi CANCELADO)
Invoke-RestMethod -Uri "http://localhost:8080/estornos" -Method POST -Body $estorno -ContentType "application/json"
```
**Esperado**: 409 Conflict - "Já existe um estorno processado para este pagamento"

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
