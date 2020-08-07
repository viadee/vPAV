package de.viadee.bpm.vPAV.exceptions;

public class InvalidPropertiesConfiguration extends RuntimeException {
    
    public InvalidPropertiesConfiguration(final String message, Throwable e) {
        super(message, e);
    }

    public InvalidPropertiesConfiguration(final String message) {
        super(message);
    }

}
