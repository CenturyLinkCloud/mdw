/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.web.servlet;


import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Method;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.centurylink.mdw.common.utilities.logger.LoggerUtil;
import com.centurylink.mdw.common.utilities.logger.StandardLogger;

public class MDWDesignerImageServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
	private static StandardLogger logger = LoggerUtil.getStandardLogger();
    private static final String IMAGE_TYPE = "IMAGE_TYPE";
    private static final String IMAGE_TYPE_TASK_INSTANCE = "TASK_INSTANCE";
    private static final String IMAGE_TYPE_SECONDARY_OWNER = "SECONDARY_OWNER";
    private static final String IMAGE_TYPE_PROCESS_INSTANCE = "PROCESS_INSTANCE";
    private static final String IMAGE_TYPE_MILESTONE_INSTANCE = "MILESTONE_INSTANCE";

    private static final String PARAM_TASK_INSTANCE_ID = "TaskInstanceId";
    private static final String PARAM_PROCESS_INSTANCE_ID = "ProcessInstanceId";
    private static final String PARAM_SECONDARY_OWNER_ID = "SecondaryOwnerId";
    private static final String PARAM_SECONDARY_OWNER = "SecondaryOwner";
    private static final String PARAM_REPORT_NAME = "ReportName";
    private static final String PARAM_MASTER_REQUEST_ID = "MasterRequestId";

	public void doGet(HttpServletRequest request, HttpServletResponse response)
	  throws ServletException, IOException {
        try {
        	String imgType = request.getParameter(IMAGE_TYPE);
            if(IMAGE_TYPE_TASK_INSTANCE.equals(imgType)){
            	// process image by task instance ID
                String instId = request.getParameter(PARAM_TASK_INSTANCE_ID);
                Long taskInstId = new Long(instId);

                Class<?> imageHelperRuntimeClass = Class.forName("com.centurylink.mdw.designer.display.ImageServletHelper");
                Method imageHelperMethod = imageHelperRuntimeClass.getMethod("generateImageForTaskInstance", new Class[]{Long.class});
                BufferedImage image = (BufferedImage)imageHelperMethod.invoke(imageHelperRuntimeClass.newInstance(), new Object[]{taskInstId});

                response.setContentType("image/jpeg");
            	ImageIO.write(image, "jpeg", response.getOutputStream());
            }else if (IMAGE_TYPE_SECONDARY_OWNER.equals(imgType)){
            	// process image by activity instance ID
                String ownerId = request.getParameter(PARAM_SECONDARY_OWNER_ID);
                String owner = request.getParameter(PARAM_SECONDARY_OWNER);
                Long secOwnerId = new Long(ownerId);

                Class<?> imageHelperRuntimeClass = Class.forName("com.centurylink.mdw.designer.display.ImageServletHelper");
                Method imageHelperMethod = imageHelperRuntimeClass.getMethod("generateImageForSecondaryOwner", new Class[]{String.class, Long.class});
                BufferedImage image = (BufferedImage)imageHelperMethod.invoke(imageHelperRuntimeClass.newInstance(), new Object[]{owner, secOwnerId});

                response.setContentType("image/jpeg");
            	ImageIO.write(image, "jpeg", response.getOutputStream());
            }else if (IMAGE_TYPE_PROCESS_INSTANCE.equals(imgType)){
            	// process image by process instance ID
                String ownerId = request.getParameter(PARAM_PROCESS_INSTANCE_ID);
                Long procInstId = new Long(ownerId);

                Class<?> imageHelperRuntimeClass = Class.forName("com.centurylink.mdw.designer.display.ImageServletHelper");
                Method imageHelperMethod = imageHelperRuntimeClass.getMethod("generateImageForProcessInstance", new Class[]{Long.class});
                BufferedImage image = (BufferedImage)imageHelperMethod.invoke(imageHelperRuntimeClass.newInstance(), new Object[]{procInstId});

                response.setContentType("image/jpeg");
            	ImageIO.write(image, "jpeg", response.getOutputStream());
            }else if (IMAGE_TYPE_MILESTONE_INSTANCE.equals(imgType)){
            	// milestone process instance image by master request ID
                String reportName = request.getParameter(PARAM_REPORT_NAME);
                String masterRequestId = request.getParameter(PARAM_MASTER_REQUEST_ID);

                Class<?> imageHelperRuntimeClass = Class.forName("com.centurylink.mdw.designer.display.ImageServletHelper");
                Method imageHelperMethod = imageHelperRuntimeClass.getMethod("generateImageForMilestoneInstance", new Class[]{String.class, String.class});
                BufferedImage image = (BufferedImage)imageHelperMethod.invoke(imageHelperRuntimeClass.newInstance(), new Object[]{reportName, masterRequestId});

                response.setContentType("image/jpeg");
            	ImageIO.write(image, "jpeg", response.getOutputStream());
            }
        } catch (Exception ex) {
            logger.severeException(ex.getMessage(), ex);
            throw new ServletException("Cannot create DesignerDataAccess", ex);
        }
		response.getOutputStream().close();
	}



}