package br.com.fademy.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Libera o frontend React (FRONTEND_ORIGIN) a chamar a API (SPEC seção 5.1).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String origemDoFrontend;

    public CorsConfig(@Value("${fademy.frontend-origin}") String origemDoFrontend) {
        this.origemDoFrontend = origemDoFrontend;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(origemDoFrontend)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE");
    }
}
