package com.faceid.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    private ApiExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/users/42");
    }

    @Test
    void shouldMapNotFoundExceptionTo404() {
        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new ResourceNotFoundException("Usuario nao encontrado com ID: 42"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Usuario nao encontrado com ID: 42");
        assertThat(response.getBody().path()).isEqualTo("/api/users/42");
    }

    @Test
    void shouldMapConflictExceptionTo409() {
        ResponseEntity<ApiErrorResponse> response = handler.handleConflict(
                new ConflictException("Ja existe um usuario com esse nome."),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("Conflict");
        assertThat(response.getBody().message()).isEqualTo("Ja existe um usuario com esse nome.");
    }
}
