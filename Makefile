# Makefile para API de Pagamentos - Sicredi ToolsChallenge
# Compatível com Windows (usando docker-compose direto)

.PHONY: help up down logs ps restart clean minimal db kafka redis build run test

help: ## Mostrar esta ajuda
	@echo "Comandos disponíveis:"
	@echo "  make up        - Sobe toda a infraestrutura"
	@echo "  make down      - Para toda a infraestrutura"
	@echo "  make logs      - Exibe logs dos containers"
	@echo "  make ps        - Lista status dos containers"
	@echo "  make restart   - Reinicia todos os serviços"
	@echo "  make clean     - Remove containers e volumes"
	@echo "  make minimal   - Sobe apenas PostgreSQL + Redis"
	@echo "  make db        - Sobe apenas PostgreSQL"
	@echo "  make kafka     - Sobe Kafka + Zookeeper + UI"
	@echo "  make redis     - Sobe apenas Redis"
	@echo "  make build     - Compila o projeto (Maven)"
	@echo "  make run       - Executa a aplicação"
	@echo "  make test      - Executa os testes"

up: ## Sobe toda a infraestrutura
	docker-compose up -d
	@echo "Infraestrutura iniciada!"
	@docker-compose ps

down: ## Para toda a infraestrutura
	docker-compose down
	@echo "Infraestrutura parada!"

logs: ## Exibe logs dos containers
	docker-compose logs -f

ps: ## Lista status dos containers
	docker-compose ps

restart: ## Reinicia todos os serviços
	docker-compose restart
	@echo "Infraestrutura reiniciada!"

clean: ## Remove containers e volumes
	docker-compose down -v
	@echo "Limpeza completa!"

minimal: ## Sobe apenas PostgreSQL + Redis
	docker-compose up -d postgres redis
	@echo "Infraestrutura mínima iniciada!"
	@docker-compose ps postgres redis

db: ## Sobe apenas PostgreSQL
	docker-compose up -d postgres
	@echo "PostgreSQL iniciado!"
	@docker-compose ps postgres

kafka: ## Sobe Kafka + Zookeeper + UI
	docker-compose up -d zookeeper kafka kafka-ui
	@echo "Kafka iniciado! Aguarde ~30 segundos para ficar totalmente pronto"
	@docker-compose ps zookeeper kafka kafka-ui

redis: ## Sobe apenas Redis
	docker-compose up -d redis
	@echo "Redis iniciado!"
	@docker-compose ps redis

build: ## Compila o projeto
	./mvnw clean install -DskipTests

run: ## Executa a aplicação
	./mvnw spring-boot:run

test: ## Executa os testes
	./mvnw test
