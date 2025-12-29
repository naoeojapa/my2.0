package com.store.BACK.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // 1. Configuração de Arquivos (A que você já tinha)
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:src/main/resources/static/uploads/", "classpath:/static/uploads/")
                .setCachePeriod(3600);

        registry.addResourceHandler("/FRONT/**")
                .addResourceLocations("file:./FRONT/")
                .setCachePeriod(3600);
    }

    // 2. Configuração de Segurança CORS (A QUE FALTAVA!)
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Aplica a todas as rotas da API
                .allowedOriginPatterns("*") // Aceita qualquer origem (Frontend)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // <--- O "DELETE" ESTÁ AQUI
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}