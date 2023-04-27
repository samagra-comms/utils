package com.uci.utils.service;


import com.uci.utils.model.EmailDetails;

public interface EmailService {
    public void sendSimpleMail(EmailDetails details);

    public void sendMailWithAttachment(EmailDetails details);
}
