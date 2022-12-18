package es.unizar.urlshortener.infrastructure.delivery

import es.unizar.urlshortener.core.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@ControllerAdvice
class RestResponseEntityExceptionHandler : ResponseEntityExceptionHandler() {

    @ResponseBody
    @ExceptionHandler(value = [InvalidUrlException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun invalidUrls(ex: InvalidUrlException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [RedirectionNotFound::class])
    @ResponseStatus(HttpStatus.NOT_FOUND)
    protected fun redirectionNotFound(ex: RedirectionNotFound) = ErrorMessage(HttpStatus.NOT_FOUND.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [UrlNotSafeException::class])
    @ResponseStatus(HttpStatus.FORBIDDEN)
    protected fun urlNotSafe(ex: UrlNotSafeException) = ErrorMessage    (HttpStatus.FORBIDDEN.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [UrlNotReachableException::class])
    protected fun urlNotReachable(ex: UrlNotReachableException) = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-after", "10000")
        .body(ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message))

    @ResponseBody
    @ExceptionHandler(value = [NotValidatedYetException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun urlNotReachable(ex: NotValidatedYetException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [HashUsedException::class])
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected fun hashUsed(ex: HashUsedException) = ErrorMessage(HttpStatus.BAD_REQUEST.value(), ex.message)

    @ResponseBody
    @ExceptionHandler(value = [TooManyRequestsException::class])
    protected fun tooManyRequests(ex: TooManyRequestsException) = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
        .header("Retry-after", "10000")
        .body(ErrorMessage(HttpStatus.TOO_MANY_REQUESTS.value(), ex.message))
}

data class ErrorMessage(
    val statusCode: Int,
    val message: String?,
    val timestamp: String = DateTimeFormatter.ISO_DATE_TIME.format(OffsetDateTime.now())
)