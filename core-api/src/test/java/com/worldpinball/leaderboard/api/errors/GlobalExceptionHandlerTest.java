package com.worldpinball.leaderboard.api.errors;

import com.worldpinball.leaderboard.api.dtos.ApiErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler sut;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        sut = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
    }

    @Test
    @DisplayName("Returns 409 CONFLICT when IllegalStateException occurs")
    void handleConflict_shouldReturn409() {
        IllegalStateException ex = new IllegalStateException("Conflict state");

        ResponseEntity<ApiErrorResponse> response = sut.handleConflict(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Conflict state");
    }

    @Test
    @DisplayName("Returns 400 BAD_REQUEST when IllegalArgumentException occurs")
    void handleBadRequest_shouldReturn400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        ResponseEntity<ApiErrorResponse> response = sut.handleBadRequest(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid argument");
    }

    @Test
    @DisplayName("Returns 500 INTERNAL_SERVER_ERROR when unexpected Exception occurs")
    void handleGeneralException_shouldReturn500() {
        Exception ex = new Exception("Database timeout");

        ResponseEntity<ApiErrorResponse> response = sut.handleGeneralException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(500);
    }
}
