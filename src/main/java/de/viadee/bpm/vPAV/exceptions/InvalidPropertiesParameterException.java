package de.viadee.bpm.vPAV.exceptions;

public class InvalidPropertiesParameterException extends RuntimeException {

    public InvalidPropertiesParameterException(final String message, Throwable e) {
        super(message, e);
    }

    public InvalidPropertiesParameterException(final String message) {
        super(message);
    }

}
