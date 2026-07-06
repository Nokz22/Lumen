package dev.lumen.presentation.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnGenericMessageForUnexpectedExceptionsWithoutLeakingDetails() {
        RuntimeException internalError = new RuntimeException("connection string: postgres://secret");

        ProblemDetail problem = handler.handleUnexpected(internalError);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(problem.getDetail()).doesNotContain("secret");
    }
}
