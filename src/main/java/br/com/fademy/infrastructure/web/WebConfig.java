package br.com.fademy.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * CORS para o frontend (SPEC 5.1) e autenticação nas rotas protegidas (SPEC 5.2).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Rotas públicas da SPEC 5.2. GET /api/invites/{token} é a única rota com um segmento
    // depois de /api/invites; /api/media/{pasta}/{arquivo} serve as imagens (RN49).
    private static final String[] ROTAS_PUBLICAS = {
            "/api/auth/login", "/api/auth/register", "/api/media/upload", "/api/invites/*", "/api/media/*/*"
    };

    private final String origemDoFrontend;
    private final Autenticacao autenticacao;

    public WebConfig(@Value("${fademy.frontend-origin}") String origemDoFrontend, Autenticacao autenticacao) {
        this.origemDoFrontend = origemDoFrontend;
        this.autenticacao = autenticacao;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origemDoFrontend)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(autenticacao).addPathPatterns("/api/**").excludePathPatterns(ROTAS_PUBLICAS);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(autenticacao);
    }
}
