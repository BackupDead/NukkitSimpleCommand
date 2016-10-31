package com.rmpi.nukkit.simplecommand;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CommandClassCorruptedException extends Exception {
    private String message;

    CommandClassCorruptedException() {
        super();
    }

    CommandClassCorruptedException(String message) {
        super(message);
    }

    CommandClassCorruptedException(String message, Throwable cause) {
        super(message, cause);
    }

    CommandClassCorruptedException(Throwable cause) {
        super(cause);
    }

    CommandClassCorruptedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    @Override
    public String getMessage() {
        if (message != null)
            return message;
        else
            return super.getMessage();
    }

    static CommandClassCorruptedException factory(String methodSignature, String error, Object... args) {
        try {
            CommandClassCorruptedException exception = CommandClassCorruptedException.class.getConstructor(
                    Arrays.stream(args)
                            .map(Object::getClass)
                            .collect(Collectors.toList())
                            .toArray(new Class[0])
            ).newInstance(args);
            exception.message = methodSignature + "does not have proper parameters. " + error;
            return exception;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            return new CommandClassCorruptedException(methodSignature + "does not have proper parameters.");
        }
    }
}
