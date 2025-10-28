# Docker - Infraestrutura Local

Este diretório contém as configurações Docker para executar toda a infraestrutura necessária localmente.

## 🐳 Serviços Disponíveis

### Banco de Dados
- **PostgreSQL 15** - `localhost:5432`
  - Database: `pagamentos`
  - User: `postgres`
  - Password: `postgres`

### Cache e Locks Distribuídos
- **Redis 7** - `localhost:6379`
  - Password: `redis123`

### Mensageria
- **Kafka** - `localhost:9092` (client) / `localhost:9093` (internal)
- **Zookeeper** - `localhost:2181`
- **Kafka UI** - http://localhost:8081

### Observabilidade
- **Prometheus** - http://localhost:9090
- **Grafana** - http://localhost:3000
  - User: `admin`
  - Password: `admin123`
- **Jaeger** - http://localhost:16686

## 🚀 Como Usar

### Iniciar Toda a Infraestrutura
```powershell
# Subir todos os serviços
docker-compose up -d

# Ver logs de todos os serviços
docker-compose logs -f

# Ver logs de um serviço específico
docker-compose logs -f postgres
docker-compose logs -f kafka
```

### Iniciar Apenas Alguns Serviços
```powershell
# Apenas PostgreSQL e Redis (mínimo para desenvolvimento)
docker-compose up -d postgres redis

# Adicionar Kafka depois
docker-compose up -d zookeeper kafka

# Adicionar observabilidade
docker-compose up -d prometheus grafana
```

### Parar Serviços
```powershell
# Parar todos os serviços
docker-compose down

# Parar e remover volumes (ATENÇÃO: apaga dados!)
docker-compose down -v

# Parar um serviço específico
docker-compose stop postgres
```

### Verificar Status
```powershell
# Listar containers rodando
docker-compose ps

# Ver uso de recursos
docker stats
```

## 🔧 Healthchecks

Todos os serviços possuem healthchecks configurados:

```powershell
# Ver status de saúde
docker-compose ps

# Aguardar que todos os serviços estejam saudáveis
docker-compose up -d --wait
```

## 📊 Acessar Interfaces

### Kafka UI
http://localhost:8081
- Visualizar tópicos, mensagens, consumers
- Criar/deletar tópicos

### Prometheus
http://localhost:9090
- Queries: `http_server_requests_seconds_count`
- Status > Targets: ver se aplicação está sendo monitorada

### Grafana
http://localhost:3000
- Login: admin/admin123
- Datasource Prometheus já configurado
- Criar dashboards para métricas da aplicação

### Jaeger (Tracing)
http://localhost:16686
- Visualizar traces distribuídos
- Analisar latência entre serviços

## 🗄️ Volumes Persistentes

Os dados são persistidos em volumes Docker:
- `postgres_data` - Dados do PostgreSQL
- `redis_data` - Dados do Redis (AOF)
- `kafka_data` - Logs e partições do Kafka
- `zookeeper_data` - Dados do Zookeeper
- `prometheus_data` - Métricas históricas
- `grafana_data` - Dashboards e configurações

```powershell
# Listar volumes
docker volume ls | findstr toolschallenge

# Remover volumes específicos (ATENÇÃO: apaga dados!)
docker volume rm toolschallenge_postgres_data
```

## 🔍 Troubleshooting

### PostgreSQL não conecta
```powershell
# Verificar se está rodando
docker-compose ps postgres

# Ver logs
docker-compose logs postgres

# Testar conexão
docker-compose exec postgres psql -U postgres -d pagamentos -c "SELECT version();"
```

### Redis não conecta
```powershell
# Verificar se está rodando
docker-compose ps redis

# Testar conexão (com senha)
docker-compose exec redis redis-cli -a redis123 ping
```

### Kafka não conecta
```powershell
# Verificar se Zookeeper e Kafka estão rodando
docker-compose ps zookeeper kafka

# Listar tópicos
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9093 --list

# Criar tópico de teste
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9093 --create --topic teste --partitions 3 --replication-factor 1
```

### Aplicação não consegue conectar
```powershell
# Verificar se todos os serviços estão saudáveis
docker-compose ps

# No Windows, verificar se host.docker.internal está resolvendo
ping host.docker.internal

# Se necessário, adicionar no C:\Windows\System32\drivers\etc\hosts:
# 127.0.0.1 host.docker.internal
```

## 🧹 Limpeza Completa

```powershell
# Parar tudo e remover volumes
docker-compose down -v

# Remover imagens não utilizadas
docker image prune -a

# Remover tudo (containers, volumes, networks)
docker system prune -a --volumes
```

## 📝 Notas

1. **Primeira execução**: Pode levar alguns minutos para baixar todas as imagens
2. **Kafka**: Aguarde ~30 segundos após `docker-compose up` para Kafka ficar pronto
3. **Grafana**: Na primeira execução, aguarde o provisionamento do datasource
4. **Recursos**: Recomendado 4GB RAM mínimo para Docker Desktop
5. **Porta 8080**: Certifique-se que não está em uso antes de subir a aplicação

## 🔗 Configuração da Aplicação

O arquivo `application.yml` já está configurado para usar estes serviços:
- PostgreSQL: `jdbc:postgresql://localhost:5432/pagamentos`
- Redis: `localhost:6379` (password: redis123)
- Kafka: `localhost:9092`
- Prometheus: scrape em `/atuador/prometheus`

Para ambiente de produção, sobrescreva com variáveis de ambiente.
