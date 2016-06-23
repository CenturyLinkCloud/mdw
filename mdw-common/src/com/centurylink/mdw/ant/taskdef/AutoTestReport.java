/**
 * Copyright (c) 2016 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.ant.taskdef;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.optional.junit.AggregateTransformer;
import org.apache.tools.ant.taskdefs.optional.junit.XMLResultAggregator;
import org.apache.tools.ant.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AutoTestReport extends XMLResultAggregator {

    private String testOutput;
    public String getTestOutput() { return testOutput; }
    public void setTestOutput(String output) { this.testOutput = output; }

    private File testOutputFile;
    public File getTestOutputFile() { return testOutputFile; }
    public void setTestOutputFile(File file) { this.testOutputFile = file; }

    private File xslFile;
    public File getXslFile() { return xslFile; }
    public void setXslFile(File file) { this.xslFile = file; }

    private String emailRecipients;
    public String getEmailRecipients() { return emailRecipients; }
    public void setEmailRecipients(String recipients) { this.emailRecipients = recipients; }

    private String emailFromAddress = "mdw.testing@centurylink.com";
    public String getEmailFromAddress() { return emailFromAddress; }
    public void setEmailFromAddress(String fromAddr) { this.emailFromAddress = fromAddr; }

    private String emailSubject = "MDW Test Results";
    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String subject) { this.emailSubject = subject; }

    private String mailHost = "mailgate.uswc.uswest.com";
    public String getMailHost() { return mailHost; }
    public void setMailHost(String host) { this.mailHost = host; }

    private String smtpPort;
    public String getSmtpPort() { return smtpPort; }
    public void setSmtpPort(String port) { this.smtpPort = port; }

    @Override
    public void execute() throws BuildException
    {
        super.execute();
        if (emailRecipients != null && emailRecipients.trim().length() > 0) {
            ClassLoader contextClassLoader = null;
            try {
                // avoid this error:
                // javax.activation.UnsupportedDataTypeException: no object DCH for MIME type multipart/mixed
                contextClassLoader = Thread.currentThread().getContextClassLoader();
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

                String[] recips = emailRecipients.split(",");
                Address[] addresses = new Address[recips.length];
                for (int i = 0; i < recips.length; i++)
                    addresses[i] = new InternetAddress(recips[i]);

                Message message = new MimeMessage(getSession());
                message.setFrom(new InternetAddress(emailFromAddress));
                message.setSubject(emailSubject);

                Multipart multiPart = new MimeMultipart();
                // html body part
                BodyPart bodyPart = new MimeBodyPart();
                File output = new File(toDir.getAbsolutePath() + "/junit-noframes.html");
                bodyPart.setContent(new String(readFile(output)).replaceAll("font:normal 68%", "font:normal 100%"), "text/html");
                multiPart.addBodyPart(bodyPart);
                message.setContent(multiPart);
                message.setSentDate(new Date());
                message.setRecipients(Message.RecipientType.TO, addresses);
                Transport.send(message);
            }
            catch (Exception ex) {
                ex.printStackTrace();
                throw new BuildException(ex.getMessage(), ex);
            }
            finally {
                if (contextClassLoader != null)
                    Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
    }

    protected Element createDocument() {
        if (testOutputFile != null) {
            try {
                testOutput = new String(readFile(testOutputFile));
            }
            catch (IOException ex) {
                ex.printStackTrace();
                testOutput = "<testsuite/>";
            }
        }

        if (testOutput != null) {
            try {
                DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                String xml = "<testsuites>\n" + testOutput + "\n</testsuites>";
                Document doc = docBuilder.parse(new ByteArrayInputStream(xml.getBytes()));
                return doc.getDocumentElement();
            }
            catch (Exception ex) {
                log(StringUtils.getStackTrace(ex), Project.MSG_ERR);
                throw new BuildException(ex.getMessage(), ex);
            }
        }
        else {
            return super.createDocument();
        }
    }

    @SuppressWarnings("unchecked")
    public AggregateTransformer createReport() {
        AutoTestTransformer transformer = new AutoTestTransformer(this);
        if (xslFile != null) {
            try {
                transformer.setXsl(new String(readFile(xslFile)));
            }
            catch (IOException ex) {
                ex.printStackTrace();
                transformer.setXsl("<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" />");
            }
        }
        transformers.addElement(transformer);
        return transformer;
    }

    private static byte[] readFile(File file) throws IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] bytes = new byte[(int) file.length()];
            fis.read(bytes);
            return bytes;
        }
        finally {
            if (fis != null) {
                try {
                  fis.close();
                }
                catch (IOException ex) {
                }
            }
        }
    }

    public Session getSession() throws MessagingException {
        Properties props = new Properties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.host", getMailHost());
        if (getSmtpPort() != null)
            props.put("mail.smtp.port", getSmtpPort());
        return Session.getInstance(props);
    }

}
