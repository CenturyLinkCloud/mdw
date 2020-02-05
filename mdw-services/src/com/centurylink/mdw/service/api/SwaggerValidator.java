package com.centurylink.mdw.service.api;

import io.limberest.service.http.Status;
import io.limberest.validate.Result;
import io.limberest.validate.ValidationException;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import org.json.JSONObject;

/**
 * Validate swagger requests programmatically based on method and path.
 */
public class SwaggerValidator {

    private HttpMethod method;
    private String path;
    private SwaggerModelValidator validator;

    public SwaggerValidator(HttpMethod method, String path) throws ValidationException {
        this.method = method;
        this.path = path.startsWith("/") ? path : "/" + path;
        Swagger swagger = MdwSwaggerCache.getSwagger(this.path);
        if (swagger == null)
            throw new ValidationException(Status.NOT_FOUND.getCode(), "Swagger not found for path: " + this.path);
        io.swagger.models.Path swaggerPath = swagger.getPath(this.path);
        if (swaggerPath == null)
            throw new ValidationException(Status.NOT_FOUND.getCode(), "Swagger path not found: " + this.path);
        Operation swaggerOp = swaggerPath.getOperationMap().get(this.method);
        if (swaggerOp == null)
            throw new ValidationException(Status.NOT_FOUND.getCode(), "Swagger operation not found: " + method + " " + this.path);
        validator = new SwaggerModelValidator(method.toString(), this.path, swagger);
    }

    public Result validateBody(JSONObject payload) throws ValidationException {
        Result result = new Result();
        result.also(validator.validateBody(payload, true));
        return result;
    }
}
