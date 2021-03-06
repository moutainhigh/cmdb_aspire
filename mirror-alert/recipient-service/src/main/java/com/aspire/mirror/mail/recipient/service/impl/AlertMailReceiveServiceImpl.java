package com.aspire.mirror.mail.recipient.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.aspire.mirror.mail.recipient.dao.AlertMailRecipientDao;
import com.aspire.mirror.mail.recipient.po.AlertMailFailed;
import com.aspire.mirror.mail.recipient.po.AlertMailRecipient;
import com.aspire.mirror.mail.recipient.po.AlertMailSubstance;
import com.aspire.mirror.mail.recipient.service.AlertMailReceiveService;
import com.aspire.mirror.mail.recipient.util.CharsetUtil;
import com.google.common.collect.Lists;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.pop3.POP3Folder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import javax.activation.DataSource;
import javax.annotation.PostConstruct;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Service
public class AlertMailReceiveServiceImpl implements AlertMailReceiveService {

    private static final Logger logger = LoggerFactory.getLogger(AlertMailReceiveServiceImpl.class);
    @Autowired
    private AlertMailRecipientDao alertMailRecipientDao;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${kafka.topic.alert_mail_substance:ALERT_MAIL_SUBSTANCE}")
    private String topic;
    private BlockingQueue<List<AlertMailSubstance>> substanceQueue = null;
    @Value("${kafka.submit.queueSize:20000}")
    private int SUBSTANCE_QUEUE_SIZE;
    @Value("${kafka.submit.timeout:5}")
    private int SUBSTANCE_QUEUE_TIMEOUT;
    @Value("${kafka.submit.perSize:20}")
    private int MAIL_SUBSTANCE_PER_SIZE;

    @PostConstruct
    private void substanceConsume() {
        if (substanceQueue == null) {
            substanceQueue = new LinkedBlockingQueue<>(SUBSTANCE_QUEUE_SIZE);
        }
        Executors.newSingleThreadExecutor().execute(new AlertMailRecipientConsumer(substanceQueue));
    }

    @Override
    public List<AlertMailSubstance> pop(AlertMailRecipient recipient) {
        List<AlertMailSubstance> list = Lists.newArrayList();
        Folder folder = null;
        Store store = null;
        String server = recipient.getMailServer();
        String account = recipient.getReceiver();
        try {
            // ????????????????????????????????????Properties??????
            Properties prop = new Properties();
//            prop.setProperty("mail.debug", "true");
            prop.setProperty("mail.store.protocol", "pop3");
            prop.setProperty("mail.pop3.host", server);
            prop.setProperty("mail.pop3.port", Integer.toString(recipient.getReceivePort()));
            // 1?????????session
            Session session = Session.getInstance(prop);
            session.setDebug(false);
            // 2?????????session??????Store??????
            store = session.getStore();
            // 3????????????????????????
            store.connect(account, recipient.getPassword());
            // 4??????????????????????????????
            folder = store.getFolder("inbox");
            if (!folder.isOpen()) {
                folder.open(Folder.READ_WRITE);
            }
            int messageCount = folder.getMessageCount();
            logger.info("#===> ??????: {}, ????????????: {}",  account, messageCount);
            for (;messageCount > 0; messageCount--) {
                Message msg = folder.getMessage(messageCount);
//            }
//            // ???????????????Folder??????????????????Message??????
//            Message[] messages = folder.getMessages();
//            for (Message message : messages) {
//                MimeMessage msg = (MimeMessage) message;
                AlertMailFailed failed = null;
                String from = "";
                String uid = "";
                try {
//                    if (!folder.isOpen()) {
//                        folder.open(Folder.READ_WRITE);
//                    }
                    uid = ((POP3Folder) folder).getUID(msg);
                    int count = alertMailRecipientDao.countSubstance(account, uid);
                    if (count > 0) {
                        logger.info("#====> ??????:{} ???????????? uid: {} ???????????????, ????????????????????????!", account, uid);
//                        break;
                    }
                    from = InternetAddress.toString(msg.getFrom());
                    if (StringUtils.isEmpty(from)) {
                        continue;
                    }
                    AlertMailSubstance substance = new AlertMailSubstance();
                    String subject = MimeUtility.decodeText(CharsetUtil.transMailCode(msg.getSubject().replace("???", "")));
                    Date sendTime = msg.getSentDate();
                    if (sendTime == null) {
                        sendTime = new Date();
                    }
                    substance.setUid(uid);
                    substance.setReceiver(account);
                    substance.setSender(from);
                    substance.setSubject(subject);
                    substance.setSendTime(sendTime);
                    // ??????????????????
                    StringBuilder contentBuilder = new StringBuilder();
                    getMailContent(msg, contentBuilder);
//                    String content = CharsetUtil.transMailCode(contentBuilder.toString());
                    String content = contentBuilder.toString();
                    substance.setContent(content);
//                    list.add(substance);
//                    submitMailSubstance(substance); //????????????????????????
                } catch (Exception e) {
                    failed = parseAlertMailFailed(account, "pop3", from, uid, parseException(e));
                } finally {
                    if (failed != null) {
                        logger.error("????????????????????????, {}", JSON.toJSONString(failed));
                        alertMailRecipientDao.insertFailedRecord(failed);
                    }
                }
            }
        } catch (MessagingException e) {
            logger.error("#====> ??????????????????: {}", JSON.toJSONString(recipient), e);
        } finally {
            try {
                if (folder != null && folder.isOpen()) {
                    folder.close(false);
                }
                if (store != null) {
                    store.close();
                }
            } catch (MessagingException e) {
                logger.error("#===> ?????? pop3 ??????????????????!", e);
            }
        }
        return list;
    }

    @Override
    public List<AlertMailSubstance> imap(AlertMailRecipient recipient) {
        List<AlertMailSubstance> list = Lists.newArrayList();
        Folder folder = null;
        Store store = null;
        String server = recipient.getMailServer();
        String account = recipient.getReceiver();
        try {
            Properties props = new Properties();
//            props.setProperty("mail.debug", "true");
            props.setProperty("mail.store.protocol", "imap");
            props.setProperty("mail.imap.host", server);
            props.setProperty("mail.imap.port", Integer.toString(recipient.getReceivePort()));
            Session session = Session.getInstance(props);
            session.setDebug(false);
            store = session.getStore("imap");
            store.connect(account, recipient.getPassword());
            folder = store.getFolder("INBOX");
            folder.open(Folder.READ_ONLY);
            int messageCount = folder.getMessageCount();
            logger.info("#===> ??????: {}, ????????????: {}",  account, messageCount);
            for (;messageCount > 0; messageCount--) {
                Message message = folder.getMessage(messageCount);

//            Message[] messages = folder.getMessages();
//            for (Message message : messages) {
                AlertMailFailed failed = null;
                String from = "";
                String uid = "";
                try {
                    uid = Long.toString(((IMAPFolder) folder).getUID(message));
//                    logger.info("IMAP ?????????:{}, ??????UID: {}", account, uid);
                    int count = alertMailRecipientDao.countSubstance(account, uid);
                    if (count > 0) {
                        logger.info("#====> ??????:{} ???????????? uid: {} ???????????????, ????????????????????????!", account, uid);
                        break;
                    }
                    from = InternetAddress.toString(message.getFrom());
                    if (StringUtils.isEmpty(from)) {
                        continue;
                    }
                    AlertMailSubstance substance = new AlertMailSubstance();
                    Date sendTime = message.getSentDate();
                    if (sendTime == null) {
                        sendTime = new Date();
                    }
                    IMAPMessage msg = (IMAPMessage) message;
                    String subject = MimeUtility.decodeText(CharsetUtil.transMailCode(msg.getSubject()));
                    substance.setUid(uid);
                    substance.setReceiver(account);
                    substance.setSender(from);
                    substance.setSendTime(sendTime);
                    substance.setSubject(subject);
                    // ??????????????????
                    StringBuilder contentBuilder = new StringBuilder();
                    getMailContent(msg, contentBuilder);
//                    String content = CharsetUtil.transMailCode(contentBuilder.toString());
                    String content = contentBuilder.toString();
                    substance.setContent(content);
//                    list.add(substance);
                    submitMailSubstance(substance); //????????????????????????
                } catch (AddressException e) {
                    failed = parseAlertMailFailed(account, "imap", from, uid, parseException(e));
                } catch (MessagingException e) {
                    failed = parseAlertMailFailed(account, "imap", from, uid, parseException(e));
                } catch (IOException e) {
                    failed = parseAlertMailFailed(account, "imap", from, uid, parseException(e));
                } finally {
                    if (failed != null) {
                        logger.error("????????????????????????, {}", JSON.toJSONString(failed));
                        alertMailRecipientDao.insertFailedRecord(failed);
                    }
                }
            }
        } catch (MessagingException e) {
            logger.error("#====> ??????????????????: {}", JSON.toJSONString(recipient), e);
        } finally {
            try {
                if (folder != null && folder.isOpen()) {
                    folder.close(false);
                }
                if (store != null) {
                    store.close();
                }
            } catch (MessagingException e) {
                logger.error("#===> ?????? imap ??????????????????!", e);
            }
        }
        return list;
    }

    @Override
    public boolean submitMailSubstance(List<AlertMailSubstance> list) {
        boolean success = true;
        if (CollectionUtils.isEmpty(list)) {
            logger.error("====> ?????????????????????????????????????????????????????????!");
            return success;
        }
        final int size = list.size();
        for (int start = 0; start < size; start+=MAIL_SUBSTANCE_PER_SIZE) {
            int end = start + MAIL_SUBSTANCE_PER_SIZE;
            List<AlertMailSubstance> subList = list.subList(start, end > size ? size : end);
            try {
                if (!substanceQueue.offer(subList, SUBSTANCE_QUEUE_TIMEOUT, TimeUnit.SECONDS)) {
                    success = false;
                    logger.error("====> ?????????????????????Kafka??????, ??????: {} ???.", subList.size());
                } else {
                    logger.info("#====> ???????????? {} ?????????????????? Kafka.", subList.size());
                }
            } catch (InterruptedException e) {
                logger.error("====> ?????????????????????Kafka??????, ??????: {} ???", subList.size(), e);
            }
        }
        return success;
    }

    /**
     * ????????????????????????
     * @param substance
     */
    private void submitMailSubstance(AlertMailSubstance substance) {
        List<AlertMailSubstance> list = Lists.newArrayList();
        list.add(substance);
        try {
            if (!substanceQueue.offer(list, SUBSTANCE_QUEUE_TIMEOUT, TimeUnit.SECONDS)) {
                logger.error("====> ?????????????????????Kafka??????!");
            }
        } catch (InterruptedException e) {
            logger.error("====> ?????????????????????Kafka??????!", e);
        }
    }

    private String getPartContent(Part part) throws IOException, MessagingException {
        String content = "";
        String contentType = part.getContentType();
        if (part.getContent() instanceof InputStream) {
            InputStream is = (InputStream)part.getContent();
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            if (StringUtils.isNotEmpty(contentType) && contentType.toLowerCase(Locale.getDefault()).indexOf("utf") > -1) {
                content = new String(bytes, "UTF-8");
            } else if (StringUtils.isNotEmpty(contentType) && contentType.toLowerCase(Locale.getDefault()).indexOf("gbk") > -1) {
                content = new String(bytes, "GBK");
            } else {
                content = new String(bytes, CharsetUtil.MAIL_CONTENT_PART_CHARSET);
                content = CharsetUtil.transMailCode(content);
            }
        }
        if (part.getContent() instanceof String) {
            content = (String) part.getContent();
            if (StringUtils.isNotEmpty(contentType) && contentType.toLowerCase(Locale.getDefault()).indexOf("utf") > -1) {
                content = new String(content.getBytes(), "UTF-8");
            } else if (StringUtils.isNotEmpty(contentType) && contentType.toLowerCase(Locale.getDefault()).indexOf("gbk") > -1) {
                content = new String(content.getBytes(), "GBK");
            } else {
                content = new String(content.getBytes(), CharsetUtil.MAIL_CONTENT_PART_CHARSET);
                content = CharsetUtil.transMailCode(content);
            }
        }
        return content;
    }

    /**
     * ??????????????????
     * @param part
     * @param bodytext
     * @throws MessagingException
     * @throws IOException
     */
    private void getMailContent(Part part, StringBuilder bodytext) throws MessagingException, IOException {
        String contentType = part.getContentType();
        int nameindex = contentType.indexOf("name");
        boolean conname = false;
        if (nameindex != -1)
            conname = true;
        logger.info("?????? ContentType: {}", contentType);
        if (part.isMimeType("text/plain") && !conname) {
            bodytext.append(getPartContent(part));
        }
        if (part.isMimeType("text/html")) {
            if (!conname) {
                bodytext.append(getPartContent(part));
            }
        }
        if (part.isMimeType("multipart/*")) {
            DataSource source = new ByteArrayDataSource(part.getInputStream(), "multipart/*");
            Multipart mp = new MimeMultipart(source);
            int counts = mp.getCount();
            for (int i = 0; i < counts; i++) {
                getMailContent(mp.getBodyPart(i), bodytext);
            }
        } else if (part.isMimeType("message/rfc822")) {
            logger.info("{},----{}", part.getContent().getClass(), part.getContent());
            getMailContent((Part) part.getContent(), bodytext);
        }
    }

    /**
     * ????????????????????????
     * @param account
     * @param protocal
     * @param from
     * @param uid
     * @param msg
     * @return
     */
    private AlertMailFailed parseAlertMailFailed(String account, String protocal, String from, String uid, String msg) {
        AlertMailFailed failed = new AlertMailFailed();
        failed.setReceiver(account);
        failed.setMethod(protocal);
        failed.setSender(from);
        failed.setUid(uid);
        failed.setMessage(msg);
        return failed;
    }

    /**
     * ??????????????????
     * @param e
     * @return
     */
    private String parseException(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return sw.toString();
    }

    private class AlertMailRecipientConsumer implements Runnable {
        private BlockingQueue<List<AlertMailSubstance>> substanceQueue;
        boolean interrupted = false;

        public AlertMailRecipientConsumer(BlockingQueue<List<AlertMailSubstance>> substanceQueue) {
            this.substanceQueue = substanceQueue;
        }

        @Override
        public void run() {
            for (;;) {
                try {
                    final List<AlertMailSubstance> substanceList = substanceQueue.take();
                    if (CollectionUtils.isNotEmpty(substanceList)) {
                        String jsonObj = JSON.toJSONString(substanceList, SerializerFeature.WriteMapNullValue);
                        ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, jsonObj);
                        List<AlertMailSubstance> finalSubstanceList = substanceList;
                        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                            @Override
                            public void onFailure(Throwable e) {
                                logger.error("#===> ?????????????????????Kafka??????, ?????? {} ???. topic: {} ", substanceList.size(), topic, e);
                                logger.info("#>>--------------------------------------------------------");
                            }

                            @Override
                            public void onSuccess(SendResult<String, Object> result) {
                                logger.info("#===> ?????????????????????Kafka??????, ?????? {} ???, ??????????????????: {} ", substanceList.size(), substanceQueue.size());
                                try {
                                    alertMailRecipientDao.batchSaveSubstance(finalSubstanceList);
                                } catch (Exception e) {
                                    logger.error("#===> ??????????????????????????????????????? !", e);
                                } finally {
                                    logger.info("#>>--------------------------------------------------------");
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    interrupted = true;
                    logger.error("????????????????????????!", e);
                } finally {
                    if (interrupted) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
