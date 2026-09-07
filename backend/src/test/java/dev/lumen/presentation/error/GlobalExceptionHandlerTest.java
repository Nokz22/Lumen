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

    /** A signed-out visitor's silent refresh is an ordinary event, not a server fault. */
    @Test
    void shouldMapAMissingAuthCookieToUnauthorizedRatherThanServerError() {
        ProblemDetail problem = handler.handleMissingAuthCookie();

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /** An unmatched URL used to reach the catch-all and come back as a 500. */
    @Test
    void shouldMapAnUnknownUrlToNotFoundRatherThanServerError() {
        ProblemDetail problem = handler.handleNoResourceFound();

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void shouldMapAWrongHttpMethodToMethodNotAllowedRatherThanServerError() {
        ProblemDetail problem = handler.handleMethodNotSupported();

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
    }

    @Test
    void shouldMapAnUnsupportedContentTypeToUnsupportedMediaType() {
        ProblemDetail problem = handler.handleUnsupportedMediaType();

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
    }

    @Test
    void shouldMapAMalformedBodyToBadRequest() {
        ProblemDetail problem = handler.handleUnreadableBody();

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /** The parser echoes the bad input, and the bad input here is questionnaire answers. */
    @Test
    void shouldNotLeakTheRejectedBodyBackToTheCaller() {
        ProblemDetail problem = handler.handleUnreadableBody();

        assertThat(problem.getDetail()).isEqualTo("Malformed request body");
    }

    @Test
    void shouldMapAccessDeniedToForbidden() {
        ProblemDetail problem = handler.handleAccessDenied();

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }
}
