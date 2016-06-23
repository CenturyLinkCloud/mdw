/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.taskmgr.jsf.validators;

import java.text.SimpleDateFormat;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

public class FormDateValidator implements Validator
{
	public void validate(FacesContext facesContext, UIComponent uIComponent, Object pDate)
		throws ValidatorException
	{
//		System.out.println("In FormDateValidator: " + 
//			  pDate.toString() + " [" + pDate.getClass().getName() + "]");

		String pattern = (String)uIComponent.getAttributes().get("datePattern");
		if (pattern==null||pattern.length()==0) pattern = "yyyy-MM-dd";
		String value = (String)pDate;
		SimpleDateFormat fmter = new SimpleDateFormat(pattern);
		try {
			fmter.setLenient(false);
			fmter.parse(value);
		} catch (Exception e) {
			FacesMessage message = new FacesMessage();
			message.setDetail("Date format is " + pattern);
			message.setSummary("Invalid date format");
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			throw new ValidatorException(message);
		}
	}

}