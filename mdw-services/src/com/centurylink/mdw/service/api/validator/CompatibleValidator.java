/*
 * Copyright (C) 2017 CenturyLink, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.centurylink.mdw.service.api.validator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import io.swagger.converter.ModelConverters;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;

/**
 * Compatibility for pre-limberest validations.
 */
class CompatibleValidator {
    private List<Validator> validators = new ArrayList<Validator>();
    List<Validator> getValidators() { return validators; }

    public CompatibleValidator() {
        addDefaultValidators();
    }

    private void addDefaultValidators() {
        validators.add(new ArrayValidator());
        validators.add(new StringValidator());
    }

    void addValidator(Validator newValidator) {
        Validator matchedValidator = validators.stream().filter((validator) -> validator
                .getClass().getName().equals(newValidator.getClass().getName())).findFirst()
                .orElse(null);
        if (matchedValidator == null) {
            getValidators().add(newValidator);
        }
    }

    ValidationResult validateModel(JSONObject json, Class<?> type, boolean strict)
            throws ValidationException, JSONException {

        ModelConverters modelConverters = ModelConverters.getInstance();
        ValidationResult result = new ValidationResult();
        Map<String,Model> models = modelConverters.readAll(type);
        Map<String,Model> mainModels = modelConverters.read(type);
        for (String modelName : mainModels.keySet()) {
            Model mainModel = mainModels.get(modelName);
            result.addValidationMessages(validateFields(json, mainModel, models));
            if (strict)
                result.addValidationMessages(validateFieldsInModel(json, mainModel, models));
        }
        return result;
    }

    private ValidationResult validateFieldsInModel(JSONObject json, Model mainModel,
            Map<String,Model> models) throws JSONException {
        ValidationResult validationResult = new ValidationResult();
        String[] passedInProperties = JSONObject.getNames(json);
        for (int i = 0; i < passedInProperties.length; i++) {
            String propertyKey = passedInProperties[i];

            if (mainModel.getProperties() == null
                    || !mainModel.getProperties().containsKey(propertyKey)) {
                if (mainModel.getProperties().containsKey("get" + propertyKey)) {
                    propertyKey = "get" + propertyKey;
                }
                else {
                    String msg = "'" + passedInProperties[i] + "' not expected";
                    if (mainModel instanceof ModelImpl)
                        msg += " on " + ((ModelImpl) mainModel).getName();
                    validationResult.addValidationMessage(new ValidationMessage().message(msg));
                }
            }
            Property modelProperty = mainModel.getProperties().get(propertyKey);
            // If it's a RefProperty then look at that
            if (modelProperty instanceof RefProperty) {
                String name = ((RefProperty) modelProperty).getSimpleRef();
                validationResult.addValidationMessages(
                        (validateFieldsInModel(json.getJSONObject(passedInProperties[i]),
                                models.get(name), models)));
            }
        }
        return validationResult;
    }

    private ValidationResult validateFields(JSONObject json, Model model,
            Map<String,Model> swaggerModels) throws ValidationException {
        Map<String, Property> modelFields = model.getProperties();
        ValidationResult validationResult = new ValidationResult();
        for (Map.Entry<String, Property> prop : modelFields.entrySet()) {
            try {
                String property = prop.getKey();
                Property modelProperty = prop.getValue();
                if (!json.has(property)) {
                    // Hack for 911Address which seems to return get911Address
                    // for some reason
                    if (property.startsWith("get")) {
                        property = property.substring(3);
                    }
                }
                // Check for requiredness
                if (!json.has(property) || json.get(property) == null
                        || json.isNull(property)) {
                    if (modelProperty.getRequired()) {
                        String msg = "'" + property + "' is a required property";
                        if (model instanceof ModelImpl)
                            msg += " on " + ((ModelImpl)model).getName();
                        validationResult.addValidationMessage(new ValidationMessage().message(msg));
                    }
                }
                else {
                    if (modelProperty instanceof RefProperty) {
                        // Got a class, so process this class for required
                        // fields
                        String name = ((RefProperty) modelProperty).getSimpleRef();
                        validationResult
                                .addValidationMessages((validateFields(json.getJSONObject(property),
                                        swaggerModels.get(name), swaggerModels)));
                    }
                    // Validate formats only if not a required error
                    // and there is some kind of value
                    Iterator<Validator> itr = validators.iterator();

                    if (itr.hasNext()) {
                        Validator validator = itr.next();
                        validationResult.addValidationMessages(
                                validator.validate(json, property, modelProperty, itr));
                    }
                }
            }
            catch (Exception e) {
                // include e in ValidationException constructor so that root cause is not lost
                throw new ValidationException(
                        new ValidationMessage().message(e.getMessage()), e);
            }
        }
        return validationResult;
    }
}
