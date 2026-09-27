package br.com.fademy.infrastructure.temporeal;

import io.socket.engineio.server.EngineIoServer;
import io.socket.engineio.server.EngineIoServerOptions;
import io.socket.socketio.server.SocketIoServer;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * Servidor Socket.IO (v4) dentro do próprio Tomcat, na mesma porta da API, como o frontend
 * espera (SPEC RN43): polling por um servlet assíncrono e WebSocket por um endpoint JSR-356,
 * os dois em /socket.io/.
 */
@Configuration
public class SocketIoConfig {

    private static final String CAMINHO = "/socket.io/";

    @Bean
    public EngineIoServer engineIoServer(@Value("${fademy.frontend-origin}") String origemDoFrontend) {
        EngineIoServerOptions opcoes = EngineIoServerOptions.newFromDefault();
        opcoes.setAllowedCorsOrigins(new String[]{origemDoFrontend});
        return new EngineIoServer(opcoes);
    }

    @Bean
    public SocketIoServer socketIoServer(EngineIoServer engineIoServer) {
        return new SocketIoServer(engineIoServer);
    }

    @Bean
    public ServletRegistrationBean<HttpServlet> socketIoPolling(EngineIoServer engineIoServer) {
        HttpServlet servlet = new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
                engineIoServer.handleRequest(request, response);
            }
        };
        ServletRegistrationBean<HttpServlet> registro = new ServletRegistrationBean<>(servlet, CAMINHO + "*");
        registro.setAsyncSupported(true);
        return registro;
    }

    // O Tomcat publica o ServerContainer num listener de contexto, que roda depois dos
    // inicializadores do Spring; por isso o endpoint é registrado num listener também. Nos testes
    // com MockMvc não há ServerContainer e o WebSocket fica de fora (o polling continua valendo).
    @Bean
    public ServletContextInitializer socketIoWebSocket(EngineIoServer engineIoServer) {
        return contexto -> contexto.addListener(new ServletContextListener() {
            @Override
            public void contextInitialized(ServletContextEvent evento) {
                if (evento.getServletContext().getAttribute(ServerContainer.class.getName())
                        instanceof ServerContainer container) {
                    registrar(container, engineIoServer);
                }
            }
        });
    }

    private static void registrar(ServerContainer container, EngineIoServer engineIoServer) {
        try {
            container.addEndpoint(ServerEndpointConfig.Builder.create(EndpointWebSocket.class, CAMINHO)
                    .configurator(new EndpointWebSocket.Configurador(engineIoServer))
                    .build());
        } catch (DeploymentException e) {
            throw new IllegalStateException("Não foi possível registrar o WebSocket do Socket.IO", e);
        }
    }
}
