# 🚀 Quick Start - API de Pagamentos

Guia rápido para iniciar o desenvolvimento local.

## ⚡ TL;DR

```powershell
# 1. Subir infraestrutura
docker-compose up -d

# 2. Aguardar serviços ficarem prontos (~30 segundos)
docker-compose ps

# 3. Compilar projeto
.\mvnw clean install -DskipTests

# 4. Executar aplicação
.\mvnw spring-boot:run

# 5. Testar
curl http://localhost:8080/atuador/saude
```

## 📋 Checklist de Primeira Execução

- [ ] Docker Desktop instalado e rodando
- [ ] Java 17+ instalado (`java -version`)
- [ ] Maven 3.9+ instalado ou usar `mvnw` incluído
- [ ] Porta 8080 livre (aplicação)
- [ ] Portas 5432, 6379, 9092 livres (infraestrutura)

## 🐳 Infraestrutura Docker

### Iniciar Tudo
```powershell
docker-compose up -d
```

### Verificar Status
```powershell
docker-compose ps
```

Todos os serviços devem mostrar `Up (healthy)`:
- ✅ toolschallenge-postgres
- ✅ toolschallenge-redis
- ✅ toolschallenge-zookeeper
- ✅ toolschallenge-kafka
- ✅ toolschallenge-kafka-ui
- ✅ toolschallenge-prometheus
- ✅ toolschallenge-grafana
- ✅ toolschallenge-jaeger

### Parar Tudo
```powershell
# Parar mantendo dados
docker-compose down

# Parar E apagar dados (cuidado!)
docker-compose down -v
```

## 🔧 Desenvolvimento

### Build
```powershell
# Com Maven local
mvn clean install -DskipTests

# Com Maven wrapper (recomendado)
.\mvnw clean install -DskipTests
```

### Run
```powershell
# Modo desenvolvimento (hot reload via devtools)
.\mvnw spring-boot:run

# Executar JAR compilado
java -jar target\toolschallenge-0.0.1-SNAPSHOT.jar
```

### Acessar Aplicação
- **API Base**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/atuador/saude
- **Métricas**: http://localhost:8080/atuador/prometheus

## 🔍 Interfaces de Monitoramento

| Serviço | URL | Credenciais |
|---------|-----|-------------|
| Kafka UI | http://localhost:8081 | - |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin/admin123 |
| Jaeger | http://localhost:16686 | - |

## 🧪 Testes de Conectividade

### PostgreSQL
```powershell
docker-compose exec postgres psql -U postgres -d pagamentos -c "SELECT version();"
```

### Redis
```powershell
docker-compose exec redis redis-cli -a redis123 ping
# Deve retornar: PONG
```

### Kafka
```powershell
# Listar tópicos (deve estar vazio inicialmente)
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9093 --list

# Criar tópico de teste
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9093 --create --topic teste --partitions 3 --replication-factor 1
```

## 📊 Verificar Métricas

### Via Actuator (JSON)
```powershell
curl http://localhost:8080/atuador/metricas
```

### Via Prometheus
1. Acesse http://localhost:9090
2. Execute query: `http_server_requests_seconds_count`
3. Verifique targets: Status > Targets

### Via Grafana
1. Acesse http://localhost:3000 (admin/admin123)
2. Datasource Prometheus já está configurado
3. Explore métricas ou crie dashboard

## 🆘 Troubleshooting

### Aplicação não conecta no PostgreSQL
```powershell
# Verificar se PostgreSQL está rodando
docker-compose ps postgres

# Ver logs do PostgreSQL
docker-compose logs postgres

# Reiniciar PostgreSQL
docker-compose restart postgres
```

### Kafka não sobe
```powershell
# Kafka depende do Zookeeper - verificar ordem
docker-compose logs zookeeper
docker-compose logs kafka

# Aguardar mais tempo (~30 segundos após zookeeper)
# Reiniciar Kafka
docker-compose restart kafka
```

### Porta 8080 já está em uso
```powershell
# Windows: Descobrir processo usando a porta
netstat -ano | findstr :8080

# Matar processo (substitua <PID>)
taskkill /F /PID <PID>
```

### Redis connection refused
```powershell
# Verificar se Redis está rodando
docker-compose ps redis

# Testar conexão
docker-compose exec redis redis-cli -a redis123 ping
```

### Limpar tudo e recomeçar
```powershell
# Parar aplicação: Ctrl+C

# Parar e limpar Docker
docker-compose down -v

# Limpar build Maven
mvn clean

# Recomeçar do zero
docker-compose up -d
.\mvnw clean install -DskipTests
.\mvnw spring-boot:run
```

## 📚 Documentação Completa

- **README.md** - Documentação completa do projeto
- **docker/README.md** - Guia detalhado do Docker Compose
- **.env.example** - Variáveis de ambiente disponíveis

## 🎯 Próximos Passos

Após a infraestrutura estar rodando:

1. **Fase 1** - Implementar Flyway migrations
2. **Fase 2** - Criar entidades JPA (Pagamento, Estorno)
3. **Fase 3** - Implementar repositories
4. **Fase 4** - Implementar services com regras de negócio
5. **Fase 5** - Criar controllers REST
6. **Fase 6** - Configurar Kafka producers/consumers
7. **Fase 7** - Implementar testes de integração

---

**Status**: ✅ Infraestrutura configurada e pronta para desenvolvimento

**Última atualização**: Outubro 2025
