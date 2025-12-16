package com.example.wagemanager.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwt = "JWT";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwt);
        Components components = new Components()
                .addSecuritySchemes(jwt, new SecurityScheme()
                        .name(jwt)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"));

        return new OpenAPI()
                .info(new Info()
                        .title("WageManager API")
                        .description("근로자 급여 관리 시스템 API 문서")
                        .version("v1.0"))
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    /**
     * 모든 API에 공통 에러 응답 추가
     * 각 HTTP 상태 코드에 맞는 예시를 표시
     */
    @Bean
    public OperationCustomizer globalErrorResponseCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            // 400 Bad Request
            responses.addApiResponse("400", new ApiResponse()
                    .description("잘못된 요청")
                    .content(createErrorContent("BAD_REQUEST", "잘못된 요청입니다.")));

            // 401 Unauthorized
            responses.addApiResponse("401", new ApiResponse()
                    .description("인증 실패")
                    .content(createErrorContent("UNAUTHORIZED", "인증에 실패했습니다.")));

            // 404 Not Found
            responses.addApiResponse("404", new ApiResponse()
                    .description("리소스를 찾을 수 없음")
                    .content(createErrorContent("NOT_FOUND", "리소스를 찾을 수 없습니다.")));

            // 500 Internal Server Error
            responses.addApiResponse("500", new ApiResponse()
                    .description("서버 오류")
                    .content(createErrorContent("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.")));

            return operation;
        };
    }

    /**
     * 에러 응답용 Content 생성 (상태 코드별 예시 포함)
     */
    @SuppressWarnings("rawtypes")
    private Content createErrorContent(String errorCode, String errorMessage) {
        Schema errorDetailSchema = new Schema<>()
                .type("object")
                .addProperty("code", new Schema<>().type("string").example(errorCode))
                .addProperty("message", new Schema<>().type("string").example(errorMessage));

        Schema errorResponseSchema = new Schema<>()
                .type("object")
                .addProperty("success", new Schema<>().type("boolean").example(false))
                .addProperty("data", new Schema<>().type("object").nullable(true).example(null))
                .addProperty("error", errorDetailSchema);

        return new Content()
                .addMediaType("application/json", new MediaType()
                        .schema(errorResponseSchema));
    }
}
