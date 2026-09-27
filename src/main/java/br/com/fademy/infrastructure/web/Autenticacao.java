package br.com.fademy.infrastructure.web;

import br.com.fademy.application.gateway.TokenGateway;
import br.com.fademy.domain.excecao.NaoAutenticadoException;
import br.com.fademy.domain.usuario.Visualizador;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Exige o token nas rotas protegidas (SPEC RN08) e entrega o {@link Visualizador} como
 * parâmetro dos controllers. As exceções lançadas aqui passam pelo {@link TratadorDeErros}.
 */
@Component
public class Autenticacao implements HandlerInterceptor, HandlerMethodArgumentResolver {

    private static final String ATRIBUTO = Visualizador.class.getName();
    private static final String PREFIXO = "Bearer ";

    private final TokenGateway tokens;

    public Autenticacao(TokenGateway tokens) {
        this.tokens = tokens;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String cabecalho = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecalho == null || !cabecalho.startsWith(PREFIXO)) {
            throw new NaoAutenticadoException("Authentication required");
        }
        request.setAttribute(ATRIBUTO, tokens.validar(cabecalho.substring(PREFIXO.length())));
        return true;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType() == Visualizador.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return webRequest.getAttribute(ATRIBUTO, RequestAttributes.SCOPE_REQUEST);
    }
}
