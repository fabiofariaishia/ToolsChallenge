# Script helper para gerenciar infraestrutura Docker
# Uso: .\docker.ps1 <comando>

param(
    [Parameter(Position=0)]
    [ValidateSet("up", "down", "logs", "ps", "restart", "clean", "db", "kafka", "redis", "minimal")]
    [string]$Command = "up"
)

$ErrorActionPreference = "Stop"

function Show-Help {
    Write-Host "🐳 Docker Compose - API de Pagamentos Sicredi" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Uso: .\docker.ps1 <comando>" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Comandos disponíveis:" -ForegroundColor Green
    Write-Host "  up        - Sobe toda a infraestrutura"
    Write-Host "  down      - Para toda a infraestrutura"
    Write-Host "  logs      - Exibe logs de todos os serviços"
    Write-Host "  ps        - Lista status dos containers"
    Write-Host "  restart   - Reinicia todos os serviços"
    Write-Host "  clean     - Remove tudo (containers + volumes)"
    Write-Host "  minimal   - Sobe apenas PostgreSQL e Redis"
    Write-Host "  db        - Sobe apenas PostgreSQL"
    Write-Host "  kafka     - Sobe Kafka + Zookeeper + UI"
    Write-Host "  redis     - Sobe apenas Redis"
    Write-Host ""
}

function Start-All {
    Write-Host "🚀 Iniciando toda a infraestrutura..." -ForegroundColor Green
    docker-compose up -d
    Write-Host ""
    Write-Host "✅ Infraestrutura iniciada!" -ForegroundColor Green
    Write-Host ""
    Show-Services
}

function Start-Minimal {
    Write-Host "🚀 Iniciando infraestrutura mínima (PostgreSQL + Redis)..." -ForegroundColor Green
    docker-compose up -d postgres redis
    Write-Host ""
    Write-Host "✅ Infraestrutura mínima iniciada!" -ForegroundColor Green
    Write-Host ""
    docker-compose ps postgres redis
}

function Start-Database {
    Write-Host "🚀 Iniciando PostgreSQL..." -ForegroundColor Green
    docker-compose up -d postgres
    Write-Host ""
    Write-Host "✅ PostgreSQL iniciado!" -ForegroundColor Green
    Write-Host ""
    docker-compose ps postgres
}

function Start-Kafka {
    Write-Host "🚀 Iniciando Kafka + Zookeeper + UI..." -ForegroundColor Green
    docker-compose up -d zookeeper kafka kafka-ui
    Write-Host ""
    Write-Host "✅ Kafka iniciado!" -ForegroundColor Green
    Write-Host "   Aguarde ~30 segundos para Kafka ficar totalmente pronto" -ForegroundColor Yellow
    Write-Host ""
    docker-compose ps zookeeper kafka kafka-ui
}

function Start-Redis {
    Write-Host "🚀 Iniciando Redis..." -ForegroundColor Green
    docker-compose up -d redis
    Write-Host ""
    Write-Host "✅ Redis iniciado!" -ForegroundColor Green
    Write-Host ""
    docker-compose ps redis
}

function Stop-All {
    Write-Host "🛑 Parando toda a infraestrutura..." -ForegroundColor Yellow
    docker-compose down
    Write-Host ""
    Write-Host "✅ Infraestrutura parada!" -ForegroundColor Green
}

function Show-Logs {
    Write-Host "📋 Exibindo logs (Ctrl+C para sair)..." -ForegroundColor Cyan
    docker-compose logs -f
}

function Show-Status {
    Write-Host "📊 Status dos containers:" -ForegroundColor Cyan
    Write-Host ""
    docker-compose ps
}

function Restart-All {
    Write-Host "🔄 Reiniciando toda a infraestrutura..." -ForegroundColor Yellow
    docker-compose restart
    Write-Host ""
    Write-Host "✅ Infraestrutura reiniciada!" -ForegroundColor Green
    Write-Host ""
    Show-Services
}

function Clean-All {
    Write-Host "⚠️  ATENÇÃO: Isso vai remover todos os containers E volumes (dados serão perdidos)!" -ForegroundColor Red
    $confirm = Read-Host "Tem certeza? Digite 'sim' para confirmar"
    
    if ($confirm -eq "sim") {
        Write-Host "🧹 Limpando tudo..." -ForegroundColor Yellow
        docker-compose down -v
        Write-Host ""
        Write-Host "✅ Limpeza completa!" -ForegroundColor Green
    } else {
        Write-Host "❌ Operação cancelada" -ForegroundColor Yellow
    }
}

function Show-Services {
    Write-Host "🌐 Serviços disponíveis:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  PostgreSQL:   " -NoNewline -ForegroundColor Green
    Write-Host "localhost:5432 (postgres/postgres)" -ForegroundColor White
    Write-Host "  Redis:        " -NoNewline -ForegroundColor Green
    Write-Host "localhost:6379 (password: redis123)" -ForegroundColor White
    Write-Host "  Kafka:        " -NoNewline -ForegroundColor Green
    Write-Host "localhost:9092" -ForegroundColor White
    Write-Host "  Kafka UI:     " -NoNewline -ForegroundColor Green
    Write-Host "http://localhost:8081" -ForegroundColor White
    Write-Host "  Prometheus:   " -NoNewline -ForegroundColor Green
    Write-Host "http://localhost:9090" -ForegroundColor White
    Write-Host "  Grafana:      " -NoNewline -ForegroundColor Green
    Write-Host "http://localhost:3000 (admin/admin123)" -ForegroundColor White
    Write-Host "  Jaeger:       " -NoNewline -ForegroundColor Green
    Write-Host "http://localhost:16686" -ForegroundColor White
    Write-Host ""
}

# Main
switch ($Command) {
    "up" { Start-All }
    "down" { Stop-All }
    "logs" { Show-Logs }
    "ps" { Show-Status }
    "restart" { Restart-All }
    "clean" { Clean-All }
    "minimal" { Start-Minimal }
    "db" { Start-Database }
    "kafka" { Start-Kafka }
    "redis" { Start-Redis }
    default { Show-Help }
}
