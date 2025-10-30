package br.com.sicredi.toolschallenge.infra.idempotencia.interceptor;

import br.com.sicredi.toolschallenge.infra.idempotencia.annotation.Idempotente;
import br.com.sicredi.toolschallenge.infra.idempotencia.service.IdempotenciaService;
import br.com.sicredi.toolschallenge.infra.idempotencia.service.IdempotenciaService.RespostaIdempotente;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Interceptor para validar e processar requisições com controle de idempotência.
 * 
 * Fluxo:
 * 1. Verifica se o método tem annotation {@link Idempotente}
 * 2. Extrai chave de idempotência do header
 * 3. Se header ausente: retorna 400 Bad Request
 * 4. Se chave já processada: retorna resposta anterior (header X-Idempotency-Replayed: true)
 * 5. Se chave nova: permite processamento e armazena atributos para o filtro
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdempotenciaInterceptor implements HandlerInterceptor {

    private final IdempotenciaService idempotenciaService;
    private final ObjectMapper objectMapper;

    public static final String CHAVE_IDEMPOTENCIA_ATTR = "chaveIdempotencia";
    public static final String ID_TRANSACAO_ATTR = "idTransacao";
    public static final String TTL_ATTR = "ttl";
    public static final String HEADER_NAME_ATTR = "headerName";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Verificar se o método tem annotation @Idempotente
        Idempotente idempotente = handlerMethod.getMethodAnnotation(Idempotente.class);
        if (idempotente == null) {
            return true;
        }

        String headerName = idempotente.headerName();
        String chave = request.getHeader(headerName);

        // Se header ausente, retornar erro 400
        if (chave == null || chave.trim().isEmpty()) {
            log.warn("Header '{}' ausente na requisição para {}", headerName, request.getRequestURI());
            enviarErro(response, HttpStatus.BAD_REQUEST, 
                String.format("Header '%s' é obrigatório para esta operação", headerName),
                request.getRequestURI());
            return false;
        }

        log.debug("Chave de idempotência recebida: {}", chave);

        // Buscar resposta anterior
        Optional<RespostaIdempotente> respostaOpt = idempotenciaService.buscarResposta(chave);

        if (respostaOpt.isPresent()) {
            // Requisição duplicada - retornar resposta anterior
            log.info("Requisição duplicada detectada para chave: {}", chave);
            RespostaIdempotente respostaAnterior = respostaOpt.get();
            enviarRespostaAnterior(response, respostaAnterior);
            return false;
        }

        // Chave nova - armazenar atributos para o filtro processar após resposta
        long ttlSeconds = idempotente.unidadeTempo().toSeconds(idempotente.ttl());
        request.setAttribute(CHAVE_IDEMPOTENCIA_ATTR, chave);
        request.setAttribute(TTL_ATTR, ttlSeconds);
        request.setAttribute(HEADER_NAME_ATTR, headerName);

        return true;
    }

    /**
     * Envia resposta de erro em JSON.
     */
    private void enviarErro(HttpServletResponse response, HttpStatus status, String mensagem, String caminho) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> erro = Map.of(
            "erro", mensagem,
            "timestamp", OffsetDateTime.now().toString(),
            "status", status.value(),
            "caminho", caminho
        );

        String json = objectMapper.writeValueAsString(erro);
        response.getWriter().write(json);
        response.getWriter().flush();
    }

    /**
     * Envia resposta anterior armazenada (requisição duplicada).
     */
    private void enviarRespostaAnterior(HttpServletResponse response, RespostaIdempotente resposta) throws IOException {
        response.setStatus(resposta.statusHttp());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("X-Idempotency-Replayed", "true");

        String json = objectMapper.writeValueAsString(resposta.corpo());
        response.getWriter().write(json);
        response.getWriter().flush();
    }
}
