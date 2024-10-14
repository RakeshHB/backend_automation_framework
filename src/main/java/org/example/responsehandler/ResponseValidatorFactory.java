package org.example.responsehandler;

public class ResponseValidatorFactory {

    public static AbstractResponseValidator getInstance(boolean isSchemaValidation) {
        return isSchemaValidation ? new SchemaValidator() : new JsonResponseValidator();
    }


}
