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
package com.centurylink.mdw.email;

import com.centurylink.mdw.cache.asset.AssetCache;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.workflow.RuntimeContext;
import com.centurylink.mdw.util.ExpressionUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

/**
 * Sends an e-mail via JavaMail/SMTP based on a template asset.
 * Parameterization is performed through the specified RuntimeContext.
 */
public class TemplatedEmail {

    private static String defaultMailHost;
    public static void setDefaultMailHost(String host) { defaultMailHost = host; }

    public String getMailHost() throws MessagingException {
        String mailHost = getProperty(PropertyNames.MDW_MAIL_SMTP_HOST);
        if (mailHost == null) {
            mailHost = getProperty("MDWFramework.JavaMail/smtpHost");
        }
        if (mailHost == null)
            mailHost = defaultMailHost;
        if (mailHost == null)
            throw new MessagingException("Missing property: " + PropertyNames.MDW_MAIL_SMTP_HOST);
        return mailHost;
    }

    private String getProperty(String propname) throws MessagingException {
        return PropertyManager.getProperty(propname);
    }

    public String getSmtpPort() throws MessagingException {
        String port = getProperty(PropertyNames.MDW_MAIL_SMTP_PORT);
        if (port == null)
            port = getProperty("MDWFramework.JavaMail/smtpPort");
        return port;
    }

    /**
     * Milliseconds
     */
    public String getConnectionTimeout() throws MessagingException {
        String timeout = getProperty(PropertyNames.MDW_MAIL_CONNECTION_TIMEOUT);
        if (timeout == null)
            timeout = getProperty("MDWFramework.JavaMail/connectionTimeout");
        return timeout;
    }

    /**
     * Milliseconds
     */
    public String getSmtpTimeout() throws MessagingException {
        String timeout = getProperty(PropertyNames.MDW_MAIL_SMTP_TIMEOUT);
        if (timeout == null)
            timeout = getProperty("MDWFramework.JavaMail/smtpTimeout");
        return timeout;
     }
    /**
     * Smtp Mail User
     */
    public String getSmtpUser() throws MessagingException {
        return getProperty(PropertyNames.MDW_MAIL_SMTP_USER);
    }
    /**
     * Smtp Mail Pass
     */
    public String getSmtpPass() throws MessagingException {
        return getProperty(PropertyNames.MDW_MAIL_SMTP_PASS);
    }

    private String protocol = "smtp";
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    private String subject;
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    private String fromAddress;
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    private Address[] recipients;
    public Address[] getRecipients() { return recipients; }
    public void setRecipients(Address[] recipients) { this.recipients = recipients; }

    private Address[] ccRecipients;
    public Address[] getCcRecipients() { return ccRecipients; }
    public void setCcRecipients(Address[] ccRecipients) { this.ccRecipients = ccRecipients; }

    private String priority;
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    private String getPriorityValue() {
        if (priority == null || "Normal".equalsIgnoreCase(priority))
            return "3";  // Normal
        else if ("High".equalsIgnoreCase(priority))
            return "1";
        else
            return "5";
    }

    private boolean html;
    public boolean isHtml() { return html; }
    public void setHtml(boolean html) { this.html = html; }

    private Map<String,String> images = new HashMap<>();
    public void addImage(String id, String file) { images.put(id, file); }
    public void removeImage(String id) { images.remove(id); }

    private AssetVersionSpec templateAssetVerSpec;
    public AssetVersionSpec getTemplateAssetVerSpec() { return templateAssetVerSpec; }
    public void setTemplateAssetVerSpec(AssetVersionSpec templateAssetVerSpec) { this.templateAssetVerSpec = templateAssetVerSpec; }

    private Map<String,File> attachments = new HashMap<>();
    public Map<String,File> getAttachments() { return attachments; }
    public void setAttachments(Map<String,File> attachments) { this.attachments = attachments; }
    public void addAttachment(String name, File file) { attachments.put(name, file); }
    public void removeAttachment(String name) { attachments.remove(name); }

    private RuntimeContext runtimeContext;
    public RuntimeContext getRuntimeContext() {
        return runtimeContext;
    }
    public void setRuntimeContext(RuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
    }

    public Session getSession() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.transport.protocol", getProtocol());
        props.put("mail.host", getMailHost());
        if (getSmtpPort() != null)
            props.put("mail.smtp.port", getSmtpPort());
        if (getSmtpTimeout() != null)
            props.put("mail.smtp.timeout", getSmtpTimeout());
        if (getConnectionTimeout() != null)
            props.put("mail.smtp.connectiontimeout", getConnectionTimeout());
        /**
         * Handle any required mail authentication
         */
        Authenticator authenticator = null;
        final String user = getSmtpUser();
        final String pass = getSmtpPass();
        if (user != null && pass != null) {
            props.put("mail.smtp.auth", "true");
            authenticator = new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }

            };
        }
        return Session.getInstance(props, authenticator);
    }

    private MessagingException messagingException;

    /**
     * Sends an email based on the substituted template.
     */
    public void sendEmail() throws MessagingException {
        sendEmail((String)null);
    }

    public void sendEmail(String messageBody) throws MessagingException {

        Message message = buildMessage(messageBody);
        messagingException = null;

        try {
            message.setRecipients(Message.RecipientType.TO, recipients);
            if (ccRecipients != null) {
                message.setRecipients(Message.RecipientType.CC, ccRecipients);
            }
            message.setHeader("X-Priority", getPriorityValue());
            Transport.send(message);
        }
        catch (SendFailedException ex) {
            addMessagingException(ex);
            // try to resend without bad recipients
            if (ex.getValidUnsentAddresses() != null) {
                List<Address> newRecips = new ArrayList<>();
                List<Address> newCCs = new ArrayList<>();
                for (Address validUnsent : ex.getValidUnsentAddresses()) {
                    for (Address recip : recipients) {
                        if (validUnsent.equals(recip)) {
                            newRecips.add(recip);
                            break;
                        }
                    }
                    if (ccRecipients != null) {
                        for (Address ccRecip : ccRecipients) {
                            if (validUnsent.equals(ccRecip)) {
                                newCCs.add(validUnsent);
                                break;
                            }
                        }
                    }
                }

                if (!newRecips.isEmpty()) {
                    message.setRecipients(Message.RecipientType.TO, newRecips.toArray(new Address[0]));
                    if (!newCCs.isEmpty()) {
                        message.setRecipients(Message.RecipientType.CC, newCCs.toArray(new Address[0]));
                    } else if (ccRecipients!=null) {
                        message.setRecipients(Message.RecipientType.CC, new Address[0]);
                    }
                    Transport.send(message);
                }
            }
        }
        catch (MessagingException ex) {
            addMessagingException(ex);
        }

        if (messagingException != null) {
            throw messagingException;
        }
    }

    private void addMessagingException(MessagingException ex) {
        if (messagingException == null)
            messagingException = ex;
        else
            messagingException.setNextException(ex);
    }

    public String formatEmailDocument() throws MessagingException {
        return buildEmailJson().toString();
    }

    public JSONObject buildEmailJson() throws MessagingException {
        JSONObject jsonobj = new JsonObject();
        try {
            jsonobj.put("FROM", fromAddress);
            jsonobj.put("SUBJECT", substitute(subject));
            jsonobj.put("ISHTML", html?"true":"false");
            jsonobj.put("CONTENT", getBody());
            if (!images.isEmpty()) {
                JSONObject imagesObj = new JsonObject();
                jsonobj.put("IMAGES", imagesObj);
                for (String imageId : images.keySet()) {
                    String imageFile = images.get(imageId);
                    imagesObj.put(imageId, imageFile);
                }
            }
            JSONArray addrs = new JSONArray();
            jsonobj.put("RECIPIENTS", addrs);
            for (Address a : recipients) {
                addrs.put(a.toString());
            }
            if (ccRecipients != null) {
                addrs = new JSONArray();
                jsonobj.put("CC", addrs);
                for (Address a : ccRecipients) {
                    addrs.put(a.toString());
                }
            }

            return jsonobj;
        } catch (JSONException e1) {
            throw new MessagingException("Failed to generate email document", e1);
        }
    }

    /**
     * Creates the mime message for the e-mail.
     * @param messageBody
     * @return the message
     */
    private Message buildMessage(String messageBody)
    throws MessagingException {
        Message message = new MimeMessage(getSession());
        message.setFrom(new InternetAddress(fromAddress));
        message.setSubject(substitute(subject));

        Multipart multiPart = new MimeMultipart();
        // html body part
        BodyPart bodyPart = new MimeBodyPart();
        if (messageBody == null)
            messageBody = getBody();
        bodyPart.setContent(messageBody, html ? "text/html" : "text/plain");
        multiPart.addBodyPart(bodyPart);
        // image body parts
        if (!images.isEmpty()) {
            for (String imageId : images.keySet()) {
                String imageFile = images.get(imageId);
                BodyPart imageBodyPart = new MimeBodyPart();
                DataSource imageDataSource = null;
                URL url = Thread.currentThread().getContextClassLoader().getResource(imageFile);
                if (url == null) {
                    Asset imageAsset = null;
                    try {
                        imageAsset = AssetCache.getAsset(imageFile);
                    } catch (IOException ex) {
                        throw new MessagingException("Error loading image: " + imageFile);
                    }
                    if (imageAsset == null)
                        throw new MessagingException("Image not found: " + imageFile);
                    final Asset finalImage = imageAsset;
                    imageDataSource = new DataSource(){
                        public String getContentType() {
                            return finalImage.getContentType();
                        }
                        public InputStream getInputStream() throws IOException {
                            byte[] bytes = finalImage.getContent();
                            return new ByteArrayInputStream(bytes);
                        }
                        public String getName() {
                            return finalImage.getName();
                        }
                        public OutputStream getOutputStream() throws IOException {
                            return null;
                        }
                    };
                }
                else {
                    // load from file
                    imageDataSource = new FileDataSource(getFilePath(url));
                }
                imageBodyPart.setDataHandler(new DataHandler(imageDataSource));
                imageBodyPart.setHeader("Content-ID","<" + imageId + ">");
                multiPart.addBodyPart(imageBodyPart);
            }
        }
        if (!attachments.isEmpty()) {
            for (String name : attachments.keySet()) {
                File file = attachments.get(name);
                BodyPart attachBodyPart = new MimeBodyPart();
                DataSource fds = new FileDataSource(file);
                attachBodyPart.setDataHandler(new DataHandler(fds));
                attachBodyPart.setFileName(name);
                multiPart.addBodyPart(attachBodyPart);
            }
        }
        message.setContent(multiPart);
        message.setSentDate(new Date());
        return message;
    }


    public Asset getTemplate() throws MessagingException {
        if (templateAssetVerSpec != null && templateAssetVerSpec.getName() != null) {
            try {
                Asset template = AssetCache.getAsset(templateAssetVerSpec);
                if (template != null)
                    return template;
                else
                    throw new MessagingException("Unable to load e-mail template: " + templateAssetVerSpec);
            } catch (IOException ex) {
                throw new MessagingException("Error loading " + templateAssetVerSpec, ex);
            }
        }
        else {
            throw new MessagingException("No template specified for e-mail message: " + getSubject());
        }
    }

    public String getTemplateBody() throws MessagingException {
        Asset template = getTemplate();
        return template.getText();
    }

    public String getBody() throws MessagingException {
        Asset template = getTemplate();
        String body = template.getText();
        body = substitute(body); // calling just to populate images
        if (runtimeContext != null) {
            body = runtimeContext.evaluateToString(body);
        }
        return body;
    }

    protected String substitute(String input) throws MessagingException {
        return ExpressionUtil.substituteImages(input, images);
    }

    private String getFilePath(URL url) throws MessagingException {
        File file = null;
        try {
            file = new File(url.toURI());
        }
        catch(URISyntaxException e) {
            file = new File(url.getPath());
        }

        if (!file.exists())
            throw new MessagingException("File not found: " + file.getName());

        return file.getAbsolutePath();
    }

}
