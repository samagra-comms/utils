package com.uci.utils.service;


import com.uci.utils.model.EmailDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {
    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String sender;

    public void sendSimpleMail(EmailDetails details) {
        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(sender);
            mailMessage.setTo(details.getRecipient());
            mailMessage.setText(details.getMsgBody());
            mailMessage.setSubject(details.getSubject());
            javaMailSender.send(mailMessage);
            log.info("Mail Sent Successfully...");
        } catch (Exception e) {
            log.error("Error while Sending Mail" + e.getMessage());
        }
    }

    public void sendMailWithAttachment(EmailDetails details) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper;
        String tempPath = "/tmp/email/";
        File file = new File(tempPath);
        try {
            if (!file.exists()) {
                file.mkdirs();
            }
            mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom(sender);
            if (details.getRecipient().contains(",")) {
                String recipients[] = details.getRecipient().split(",");
                mimeMessageHelper.setTo(recipients);
            } else {
                mimeMessageHelper.setTo(details.getRecipient());
            }
            mimeMessageHelper.setText(details.getMsgBody());
            mimeMessageHelper.setSubject(details.getSubject());
            addAttachments(mimeMessageHelper, details, file.getPath());
            javaMailSender.send(mimeMessage);
            log.info("Mail sent Successfully...");
        } catch (Exception e) {
            log.error("Error while sending mail!!! " + e.getMessage());
        } finally {
            deleteFiles(file);
        }
    }

//    public void sendMailWithAttachment(String subject, String body, String recipient, XMessage xMessage, String attachmentFileName) {
//        EmailDetails emailDetails = new EmailDetails();
//        emailDetails.setSubject(subject);
//        emailDetails.setMsgBody(body);
//        emailDetails.setRecipient(recipient);
//        emailDetails.setAttachment(xMessage.toString());
//        emailDetails.setAttachmentFileName(attachmentFileName);
//        log.info("EmailDetails :" + emailDetails);
//        sendMailWithAttachment(emailDetails);
//    }

    private void createTempFile(String fileData, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        Files.writeString(path, fileData, StandardCharsets.UTF_8);
        log.info("Email attachment temp file is created : " + filePath);
    }

    private void addAttachments(MimeMessageHelper mimeMessageHelper, EmailDetails emailDetails, String rootPath) {
        try {
            if (emailDetails != null && emailDetails.getAttachments() != null) {
                Map<String, String> attachmentMap = emailDetails.getAttachments();
                for (String fileName : attachmentMap.keySet()) {
                    File file = new File(rootPath + File.separator + fileName + ".txt");
                    String fileData = attachmentMap.get(fileName);
                    createTempFile(fileData, file.getPath());
                    FileSystemResource fileSystemResource = new FileSystemResource(file);
                    mimeMessageHelper.addAttachment(fileSystemResource.getFilename(), file);
                }
            } else if (emailDetails != null && emailDetails.getAttachment() != null) {
                File file = new File(rootPath + File.separator + emailDetails.getAttachmentFileName() + ".txt");
                createTempFile(emailDetails.getAttachment(), file.getPath());
                FileSystemResource fileSystemResource = new FileSystemResource(file);
                mimeMessageHelper.addAttachment(fileSystemResource.getFilename(), file);
            } else {
                log.error("No Attachment in Email...");
            }
        } catch (Exception ex) {
            log.error("An error occured : " + ex.getMessage());
        }
    }

    public static void deleteFiles(File dirPath) {
        File filesList[] = dirPath.listFiles();
        for (File file : filesList) {
            if (file.isFile()) {
                log.info("Attachments deleted : " + file.getPath());
                file.delete();
            }
        }
    }
}
