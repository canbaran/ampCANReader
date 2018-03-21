package com.github.pires.obd.reader.io;

import android.util.Log;

import static android.content.ContentValues.TAG;

/**
 * Created by canbaran on 3/21/18.
 */

public class emailRunnable implements Runnable {
    private volatile boolean sent;
    private String[] attachments;
    private String currentDateTimeString;
    private String currentSubject;
    private StringBuilder sb;

    public emailRunnable(String[] incomingAttachments, String incomingCurrentDateTimeString, String incomingCurrentSubject, StringBuilder incomingSb) {
        attachments = incomingAttachments;
        currentDateTimeString = incomingCurrentDateTimeString;
        currentSubject = incomingCurrentSubject;
        sb = incomingSb;
    }

    @Override
    public void run() {
        try {
            sent = sendEmail("can@automotivepower.com", "ampcanreader@gmail.com", currentSubject, sb.toString(), attachments);
            Log.d(TAG, "email Sent: " + sent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean getValue() {
        return sent;
    }


    private static boolean sendEmail(String to, String from, String subject,
                                    String message,String[] attachments) throws Exception {
        Mail mail = new Mail();
        if (subject != null && subject.length() > 0) {
            mail.setSubject(subject);
        } else {
            mail.setSubject("Subject");
        }

        if (message != null && message.length() > 0) {
            mail.setBody(message);
        } else {
            mail.setBody("Message");
        }

        mail.setFrom(from);
        mail.setTo(new String[] {to});

        if (attachments != null) {
            for (String attachement : attachments) {
                mail.addAttachment(attachement);
            }
        }
        return mail.send();
    }
}
