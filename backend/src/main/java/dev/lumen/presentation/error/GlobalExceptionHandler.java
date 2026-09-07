package dev.lumen.presentation.error;

import dev.lumen.application.auth.InvalidCredentialsException;
import dev.lumen.application.auth.InvalidRefreshTokenException;
import dev.lumen.domain.assessment.AssessmentNotFoundException;
import dev.lumen.domain.assessment.AssessmentTooSoonException;
import dev.lumen.domain.assessment.InvalidAssessmentSubmissionException;
import dev.lumen.domain.crisis.InvalidRiskEventTransitionException;
import dev.lumen.domain.crisis.RiskEventNotFoundException;
import dev.lumen.domain.exercise.ExerciseNotFoundException;
import dev.lumen.domain.user.ConsentRequiredException;
import dev.lumen.domain.user.EmailAlreadyRegisteredException;
import dev.lumen.domain.user.UnderageRegistrationException;
import dev.lumen.domain.user.UserNotFoundException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserNotFoundException.class)
    public ProblemDetail handleUserNotFound(UserNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({
        AssessmentNotFoundException.class,
        RiskEventNotFoundException.class,
        ExerciseNotFoundException.class
    })
    public ProblemDetail handleNotFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AssessmentTooSoonException.class)
    public ProblemDetail handleAssessmentTooSoon(AssessmentTooSoonException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(InvalidAssessmentSubmissionException.class)
    public ProblemDetail handleInvalidAssessmentSubmission(InvalidAssessmentSubmissionException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidRiskEventTransitionException.class)
    public ProblemDetail handleInvalidRiskEventTransition(InvalidRiskEventTransitionException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ProblemDetail handleEmailAlreadyRegistered(EmailAlreadyRegisteredException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(UnderageRegistrationException.class)
    public ProblemDetail handleUnderageRegistration(UnderageRegistrationException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidRefreshTokenException.class})
    public ProblemDetail handleAuthenticationFailure(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    /**
     * A refresh request with no refresh cookie is an expired or absent session, not a
     * server fault. Without this it fell through to handleUnexpected and answered 500 —
     * with a stack trace logged at ERROR — on the most ordinary path there is: a signed-out
     * visitor opening the app, whose silent refresh has nothing to send.
     */
    @ExceptionHandler(MissingRequestCookieException.class)
    public ProblemDetail handleMissingAuthCookie() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Not authenticated");
    }

    @ExceptionHandler(ConsentRequiredException.class)
    public ProblemDetail handleConsentRequired(ConsentRequiredException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, exception.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    /**
     * A URL that does not exist is a 404, and without this it was not: the catch-all below
     * turned every unmatched path into a 500 with a stack trace logged at ERROR. A typo in
     * a client's URL is not a server fault, and an error log full of them hides real ones.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "No such resource");
    }

    /**
     * The same failure as the one above wearing a different name: a request Spring MVC
     * could not dispatch is a client mistake, and both of these were reaching the
     * catch-all and coming back as 500s.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed for this resource");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleUnsupportedMediaType() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported content type");
    }

    /**
     * A body Jackson cannot read is a malformed request, not a broken server. The detail is
     * deliberately generic: the parser's own message quotes the offending input back, and
     * the offending input here is a person's questionnaire answers or chat message.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception) {
        LOG.error("Unexpected error", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }
}
