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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.centurylink.mdw.cache.CachingException;
import com.centurylink.mdw.cache.impl.AssetCache;
import com.centurylink.mdw.cache.impl.PackageCache;
import com.centurylink.mdw.common.MdwException;
import com.centurylink.mdw.config.PropertyManager;
import com.centurylink.mdw.constant.PropertyNames;
import com.centurylink.mdw.model.JsonObject;
import com.centurylink.mdw.model.asset.Asset;
import com.centurylink.mdw.model.asset.AssetVersionSpec;
import com.centurylink.mdw.model.workflow.Package;
import com.centurylink.mdw.model.workflow.ProcessRuntimeContext;
import com.centurylink.mdw.util.ExpressionUtil;

/**
 * Sends an e-mail based on a template stored on the file system.
 * Parameterization is performed using javabean access on the specified model object.
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
        if (getTemplate().getId() == 0)
            return PropertyManager.getProperty(propname);
        try {
            Package pkg = PackageCache.getAssetPackage(getTemplate().getId());
            if (pkg == null)
                return PropertyManager.getProperty(propname);
            else
                return pkg.getProperty(propname);
        }
        catch (CachingException ex) {
            throw new MessagingException(ex.getMessage(), ex);
        }
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

    private boolean html;
    public boolean isHtml() { return html; }
    public void setHtml(boolean html) { this.html = html; }

    private Map<String,String> images = new HashMap<String,String>();
    public void addImage(String id, String file) { images.put(id, file); }
    public void removeImage(String id) { images.remove(id); }

    private String templateName;
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateFile) { this.templateName = templateFile; }

    private AssetVersionSpec templateAssetVerSpec;
    public AssetVersionSpec getTemplateAssetVerSpec() { return templateAssetVerSpec; }
    public void setTemplateAssetVerSpec(AssetVersionSpec templateAssetVerSpec) { this.templateAssetVerSpec = templateAssetVerSpec; }

    private Model model;
    public Model getModel() { return model; }
    public void setModel(Model model) { this.model = model; }

    private Map<String,File> attachments = new HashMap<String,File>();
    public Map<String,File> getAttachments() { return attachments; }
    public void setAttachments(Map<String,File> attachments) { this.attachments = attachments; }
    public void addAttachment(String name, File file) { attachments.put(name, file); }
    public void removeAttachment(String name) { attachments.remove(name); }

    private ProcessRuntimeContext runtimeContext;

    public ProcessRuntimeContext getRuntimeContext() {
        return runtimeContext;
    }
    public void setRuntimeContext(ProcessRuntimeContext runtimeContext) {
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

    public void sendEmail() throws MessagingException {
        sendEmail((String)null);
    }

    public void sendEmail(JSONObject json) throws MessagingException, JSONException {
        setFromAddress(json.getString("FROM"));
        setSubject(json.getString("SUBJECT"));
        setHtml(json.getBoolean("ISHTML"));
        if (json.has("IMAGES")) {
            JSONObject jsonImages = json.getJSONObject("IMAGES");
            for (Iterator<?> iter = jsonImages.keys(); iter.hasNext(); ) {
                String imageId = (String) iter.next();
                images.put(imageId, jsonImages.getString(imageId));
            }
        }
        if (json.has("RECIPIENTS")) {
            JSONArray jsonArray = json.getJSONArray("RECIPIENTS");
            Address[] addresses = new Address[jsonArray.length()];
            for (int i = 0; i < addresses.length; i++) {
                addresses[i] = new InternetAddress(jsonArray.getString(i));
            }
            setRecipients(addresses);
        }
        if (json.has("CC")) {
            JSONArray jsonArray = json.getJSONArray("CC");
            Address[] addresses = new Address[jsonArray.length()];
            for (int i = 0; i < addresses.length; i++) {
                addresses[i] = new InternetAddress(jsonArray.getString(i));
            }
            setCcRecipients(addresses);
        }

        sendEmail(json.getString("CONTENT"));
    }

    public void sendEmail(String messageBody) throws MessagingException {

        Message message = buildMessage(messageBody);
        messagingException = null;

        try {
            message.setRecipients(Message.RecipientType.TO, recipients);
            if (ccRecipients != null) {
                message.setRecipients(Message.RecipientType.CC, ccRecipients);
            }
            Transport.send(message);
        }
        catch (SendFailedException ex) {
            addMessagingException(ex);
            // try to resend without bad recipients
            if (ex.getValidUnsentAddresses() != null) {
                List<Address> newRecips = new ArrayList<Address>();
                List<Address> newCCs = new ArrayList<Address>();
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
     * @param session the JavaMail session
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
                    final Asset imageAsset = AssetCache.getAsset(imageFile, "IMAGE_" + imageFile.substring(imageFile.lastIndexOf('.') + 1).toUpperCase());
                    if (imageAsset == null)
                        throw new MessagingException("Image not found: " + imageFile);
                    imageDataSource = new DataSource(){
                        public String getContentType() {
                            return imageAsset.getContentType();
                        }
                        public InputStream getInputStream() throws IOException {
                            byte[] bytes = imageAsset.getContent();
                            return new ByteArrayInputStream(bytes);
                        }
                        public String getName() {
                            return imageAsset.getName();
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
        if (templateName == null) {
            if (templateAssetVerSpec != null && templateAssetVerSpec.getName() != null) {
                Asset template = AssetCache.getAsset(templateAssetVerSpec);
                if (template != null)
                    return template;
                else
                    throw new MessagingException("Unable to load e-mail template: " + templateAssetVerSpec);
            } else {
                throw new MessagingException("No template specified for e-mail message: " + getSubject());
            }
        }
        try {
            return AssetCache.getAsset(templateName);
        }
        catch (CachingException ex) {
            throw new MessagingException(ex.getMessage(), ex);
        }
    }

    public String getTemplateBody() throws MessagingException {
        Asset template = getTemplate();
        return template.getStringContent();
    }

    public String getBody() throws MessagingException {
        Asset template = getTemplate();
        String body = template.getStringContent();
        body = substitute(body); // calling just to populate images
        ProcessRuntimeContext runTime = this.getRuntimeContext();
        if (runTime != null) {
            body = runTime.evaluateToString(body);
        }
        return body;
    }

    protected String substitute(String input) throws MessagingException {
        try {
            return ExpressionUtil.substitute(input, model, images, true);
        }
        catch (MdwException ex) {
            throw new MessagingException(ex.getMessage(), ex);
        }
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

    public interface Model {
        public Map<String,String> getKeyParameters();
    }


}
