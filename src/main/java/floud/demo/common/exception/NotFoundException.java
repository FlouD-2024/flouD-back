package floud.demo.common.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@ResponseStatus(NOT_FOUND)
public abstract class NotFoundException extends ApiException {
    public NotFoundException(final String message) {
        super(NOT_FOUND, message);
    }
}