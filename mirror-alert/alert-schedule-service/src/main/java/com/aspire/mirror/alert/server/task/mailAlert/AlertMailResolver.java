package com.aspire.mirror.alert.server.task.mailAlert;

import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aspire.mirror.alert.server.util.Md5Utils;
import com.aspire.mirror.alert.server.biz.model.AlertFieldBiz;
import com.aspire.mirror.alert.server.biz.helper.AlertsHandleV2Helper;
import com.aspire.mirror.alert.server.vo.alert.AlertsV2Vo;
import org.apache.commons.lang.StringUtils;
import org.htmlparser.Parser;
import org.htmlparser.util.ParserException;
import org.htmlparser.visitors.TextExtractingVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.aspire.mirror.alert.server.dao.mailAlert.AlertMailFilterDao;
import com.aspire.mirror.alert.server.dao.mailAlert.AlertMailFilterStrategyDao;
import com.aspire.mirror.alert.server.dao.mailAlert.AlertMailResolveRecordDao;
import com.aspire.mirror.alert.server.dao.mailAlert.po.AlertMailFilter;
import com.aspire.mirror.alert.server.dao.mailAlert.po.AlertMailFilterStrategy;
import com.aspire.mirror.alert.server.dao.mailAlert.po.AlertMailResolveRecord;
import com.aspire.mirror.alert.server.dao.mailAlert.po.AlertMailSubstance;

@Component
@ConditionalOnExpression("${middleware.configuration.switch.mailKafka:false}")
public class AlertMailResolver {

    private static final Logger logger = LoggerFactory.getLogger(AlertMailResolver.class);
    @Autowired
    private AlertMailFilterDao alertMailFilterDao;
    @Autowired
    private AlertMailResolveRecordDao resolveRecordDao;
    @Autowired
    private AlertMailFilterStrategyDao strategyDao;
    @Autowired
    private AlertFieldBiz alertFieldBiz;
    @Autowired
    private AlertsHandleV2Helper alertsHandleV2Helper;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final int STRATEGY_MAIL_FIELD_SUBJECT = 0;
    private static final int STRATEGY_MAIL_FIELD_CONTENT = 1;
    private static final int STRATEGY_MAIL_FIELD_SENDER = 2;
    private static final int STRATEGY_MAIL_FIELD_SENDTIME = 3;

    private Validation valid(AlertMailSubstance substance, AlertMailFilter filter) {
        logger.info("#-- ????????????: {} ????????????-----> ?????? ------------", substance.getReceiver());
        boolean valid = true;
        String message = "";
        Validation validation = new Validation();
        //?????????????????????
        String filterSender = StringUtils.trim(filter.getSender());
        //????????????:?????????????????????
        String filterTitleInclude = StringUtils.trim(filter.getTitleInclude());
        //????????????:?????????????????????
        String filterContentInclude = StringUtils.trim(filter.getContentInclude());
        if (StringUtils.isNotEmpty(substance.getSender())) {
            if (StringUtils.isNotEmpty(filterSender)) {
                //????????????????????????
                valid = substance.getSender().contains(filterSender);
                if (!valid) {
                    message += MessageFormat.format("#--> ????????? {0} ?????????????????? {1};", substance.getSender(), filterSender);
                }
            }
            if (StringUtils.isNotEmpty(filterTitleInclude) && valid) {
                //?????????????????????????????????
                String subject = StringUtils.isEmpty(substance.getSubject()) ? "" : substance.getSubject();
                valid = subject.contains(filterTitleInclude);
                if (!valid) {
                    message += MessageFormat.format("#--> ?????? {0} ?????????????????? {1}; ", substance.getSubject(), filterTitleInclude);
                }
            }
            if (StringUtils.isNotEmpty(filterContentInclude) && valid) {
                //?????????????????????????????????
                String content = StringUtils.isEmpty(substance.getContent()) ? "" : substance.getContent();
                valid = content.contains(filterContentInclude);
                if (!valid) {
                    message += MessageFormat.format("#--> ?????? ?????????????????? {0}; ", filterTitleInclude);
                }
            }
        } else {
            valid = false;
        }
        logger.info(message);
        logger.info("#-- ????????????: {} ????????????-----> ?????? -- {} --", substance.getReceiver(), valid ? "??????" : "?????????");
        validation.setValid(valid);
        validation.setMessage(message);
        return validation;
    }

    /**
     * ????????????
     *
     * @param regex ???????????????
     * @param text  ??????????????????
     * @return
     */
    private String regExtract(String regex, String text) {
        if (StringUtils.isEmpty(regex)) {
            return text;
        }
        if (StringUtils.isEmpty(text)) {
            return null;
        }
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        try {
            while (matcher.find()) {
                return matcher.group();
            }
        } catch (Exception e) {
            logger.error("#==???????????????????????????: {}, ??????: {}", regex, text, e);
        }
        return null;
    }

    /**
     * ????????????????????????????????????
     *
     * @param text
     * @return
     * @throws ParserException
     */
    private String getMailRawContent(String text) {
        String result = "";
        try {
            Parser parser = new Parser(text);
            TextExtractingVisitor visitor = new TextExtractingVisitor();
            parser.visitAllNodesWith(visitor);
            String content = visitor.getExtractedText();
            String filterBodyRegex = "body\\s\\{[\\s\\S]*?\\}";
            Pattern filterBodyPattern = Pattern.compile(filterBodyRegex, Pattern.CASE_INSENSITIVE);
            Matcher filterBodyMatcher = filterBodyPattern.matcher(content);
            result = filterBodyMatcher.replaceAll("");
        } catch (ParserException e) {
//            logger.error("????????????????????????!", e);
        } finally {
            if (StringUtils.isEmpty(result)) {
                result = text;
            }
        }
        return result;
    }

    /**
     * ????????????????????????
     *
     * @param substance
     * @param strategy
     * @return
     */
    private String parseAlertField(AlertMailSubstance substance, AlertMailFilterStrategy strategy) throws ParseException, ParserException {
        String value = null;
        String matchVal = strategy.getFieldMatchValue();
        if (strategy.getMailField() == -1) { //?????????, ??????????????????, ??????????????????
            return matchVal;
        } else { // ????????????
            //FieldMatchReg==????????????????????????
            String regex = strategy.getFieldMatchReg();
//            if (StringUtils.isNotEmpty(regex)) {
            //MailField==??????????????????, 0: ??????, 1: ??????, 2:?????????, 3:????????????
            switch (strategy.getMailField()) {
                case STRATEGY_MAIL_FIELD_SUBJECT: //??????
                    //??????????????????, 0: ?????????, 1:??????
                    value = strategy.getUseReg() == 1 ? regExtract(regex, substance.getSubject()) : substance.getSubject();
                    break;
                case STRATEGY_MAIL_FIELD_CONTENT: //??????
                    value = strategy.getUseReg() == 1 ? regExtract(regex, substance.getContent()) : substance.getContent();
                    break;
                case STRATEGY_MAIL_FIELD_SENDER: //?????????
                    value = strategy.getUseReg() == 1 ? regExtract(regex, substance.getSender()) : substance.getSender();
                    break;
//                case STRATEGY_MAIL_FIELD_SENDTIME: //????????????
//                    value = strategy.getUseReg() == 1 ? substance.getSendTime() : sdf.parse(strategy.getFieldMatchValue());
//                    break;
                default:
                    break;
            }
//            }
        }
        return value;
    }

    private String parseAlertLevel(AlertMailSubstance substance, AlertMailFilterStrategy strategy) {
        String value = null;
        String matchVal = strategy.getFieldMatchValue();
        if (strategy.getMailField() == -1) { //?????????, ??????????????????, ??????????????????
            if (Pattern.matches("\\d+", matchVal)) {
                return matchVal;
            } else {
                logger.error("#===> alert-level ????????????! ?????????????????????, ????????????: {}", matchVal);
            }
        } else { // ????????????
            //FieldMatchTarget==????????????????????????
            String targetValue = strategy.getFieldMatchTarget();
            //MailField==??????????????????, 0: ??????, 1: ??????, 2:?????????, 3:????????????
            switch (strategy.getMailField()) {
                case STRATEGY_MAIL_FIELD_SUBJECT: //??????
                    if (substance.getSubject().contains(matchVal)) {
                        value = targetValue;
                    }
                    break;
                case STRATEGY_MAIL_FIELD_CONTENT: //??????
                    if (substance.getContent().contains(matchVal)) {
                        value = targetValue;
                    }
                    break;
                case STRATEGY_MAIL_FIELD_SENDER: //?????????
                    if (substance.getSender().contains(matchVal)) {
                        value = targetValue;
                    }
                    break;
                default:
                    logger.error("#===> ?????????????????????: {}", strategy.getMailField());
                    break;
            }
        }
        return value;
    }

    private Date parseAlertMoniTime(AlertMailSubstance substance, AlertMailFilterStrategy strategy) throws ParseException, ParserException {
        Date value = substance.getSendTime();
        try {
            //??????????????????
            String matchVal = strategy.getFieldMatchValue();
            if (strategy.getMailField() == -1) { //?????????, ??????????????????, ??????????????????
                if (StringUtils.isNotEmpty(matchVal) && Pattern.matches("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}$", matchVal)) {
                    return sdf.parse(matchVal);
                }
            } else {
                //????????????????????????
                String regex = strategy.getFieldMatchReg();
                String temp = "";
                //??????????????????, 0: ??????, 1: ??????, 2:?????????, 3:????????????
                switch (strategy.getMailField()) {
                    case STRATEGY_MAIL_FIELD_SUBJECT: //??????
                        //UseReg==??????????????????, 0: ?????????, 1:??????
                        temp = (strategy.getUseReg() == 1 && StringUtils.isNotEmpty(regex)) ? regExtract(regex, substance.getSubject()) : substance.getSubject();
                        break;
                    case STRATEGY_MAIL_FIELD_CONTENT: //??????
                        temp = (strategy.getUseReg() == 1 && StringUtils.isNotEmpty(regex)) ? regExtract(regex, substance.getContent()) : substance.getContent();
                        break;
                    case STRATEGY_MAIL_FIELD_SENDER: //?????????
                        temp = (strategy.getUseReg() == 1 && StringUtils.isNotEmpty(regex)) ? regExtract(regex, substance.getSender()) : substance.getSender();
                        break;
                    case STRATEGY_MAIL_FIELD_SENDTIME: //????????????
                        temp = (strategy.getUseReg() == 1 && StringUtils.isNotEmpty(regex)) ? regExtract(regex, sdf.format(substance.getSendTime())) : sdf.format(substance.getSendTime());
                        break;
                    default:
                        break;
                }
                if (StringUtils.isNotEmpty(temp)) {
                    value = sdf.parse(temp);
                }
            }
        } catch (ParseException e) {
            logger.error("??????????????????????????????!", e);
        }
        return value;
    }

    /**
     * ????????????????????????
     *
     * @param substance
     * @param strategyList
     * @return
     */
    private AlertsV2Vo parseAlert(AlertMailSubstance substance, List<AlertMailFilterStrategy> strategyList) {
        AlertsV2Vo alert = null;
        AlertMailSubstance innerSub = new AlertMailSubstance();
        //??????innerSub
        BeanUtils.copyProperties(substance, innerSub);
        try {
            //?????????????????????
            String content = getMailRawContent(substance.getContent());
            innerSub.setContent(content);
            alert = new AlertsV2Vo();
            alert.setAlertType(AlertsV2Vo.ALERT_ACTIVE);
            String filterId = null;
            for (AlertMailFilterStrategy strategy : strategyList) {
                if (StringUtils.isEmpty(filterId)) {
                    filterId = strategy.getFilterId();
                }
                //AlertField==????????????????????????
                switch (strategy.getAlertField().toLowerCase()) {
                    case "moni_index": //????????????
                        String moniIndex = parseAlertField(innerSub, strategy);
                        //????????????/??????
                        alert.setMoniIndex(moniIndex);
                        break;
                    case "cur_moni_time": //????????????
                        Date curMonitime = parseAlertMoniTime(innerSub, strategy);
                        alert.setCurMoniTime(curMonitime == null ? innerSub.getSendTime() : curMonitime);
                        break;
                    case "moni_object": //????????????
                        String moniObj = parseAlertField(innerSub, strategy);
                        alert.setMoniObject(moniObj);
                        alert.setItemId(Md5Utils.generateCheckCode(moniObj));
                        break;
                    case "device_ip": //??????IP
                        String deviceIp = parseAlertField(innerSub, strategy);
                        alert.setDeviceIp(deviceIp);
                        break;
                    case "alert_level": //????????????, 1-?????? 2-??? 3-??? 4-??? 5-??????
                        String alertLevel = parseAlertLevel(innerSub, strategy);
                        alert.setAlertLevel(alertLevel);
                        break;
                    case "cur_moni_value": //???????????????
                        String curMoniVal = parseAlertField(innerSub, strategy);
                        alert.setCurMoniValue(curMoniVal);
                        break;
                    case "idc_type": //?????????
                        String idcType = parseAlertField(innerSub, strategy);
                        alert.setIdcType(idcType);
                        break;
                    case "biz_sys": //????????????
                        String bizSys = parseAlertField(innerSub, strategy);
                        alert.setBizSys(bizSys);
                        break;
                    case "device_class": //????????????
                        String deviceClass = parseAlertField(innerSub, strategy);
                        alert.setDeviceClass(deviceClass);
                        break;
                    case "source_room": //??????
                        String sourceRoom = parseAlertField(innerSub, strategy);
                        alert.setSourceRoom(sourceRoom);
                        break;
                    case "object_type": //????????????, 1-?????? 2-??????
                        String objectType = parseAlertField(innerSub, strategy);
                        alert.setObjectType(objectType);
                        break;
                    default:
                        break;
                }
            }
            if (StringUtils.isEmpty(alert.getItemId())) {
                alert.setItemId(filterId);
            }
            alert.setSource("MAIL");
            alert.setAlertStartTime(innerSub.getSendTime());
            if (StringUtils.isEmpty(alert.getAlertLevel())) {
                logger.error("#===> ???????????????????????????????????????!");
                return null;
            }
            if (StringUtils.isEmpty(alert.getDeviceIp())) {
                alert.setObjectType(AlertsV2Vo.OBJECT_TYPE_BIZ);
            } else {
                alert.setObjectType(AlertsV2Vo.OBJECT_TYPE_DEVICE);
            }
//            if (StringUtils.isEmpty(alert.getMoniIndex())) { // ??????????????????
//                logger.error("#===> ???????????????????????????????????????!");
//                return null;
//            }
        } catch (ParseException e) {
            logger.error("#===> ?????????????????????????????? !", e);
        } catch (ParserException e) {
            logger.error("#===> ????????????????????????->????????????HTML???????????? !", e);
        }
        return alert;
    }

    //    @Async
    void resolve(AlertMailSubstance substance) {
        logger.info("##==> ????????????: {}", JSON.toJSONString(substance));
        //????????????????????????????????????????????????????????????????????????
        List<AlertMailFilter> filterList = alertMailFilterDao.selectFilterByReceiver(substance.getReceiver());
        if (CollectionUtils.isEmpty(filterList)) {
            logger.error("#===> ????????????????????????: {} ??????????????????????????? !", substance.getReceiver());
            return;
        }
        for (AlertMailFilter filter : filterList) {
            //??????????????????????????????????????????????????????????????????:????????????????????????????????????:?????????????????????
            Validation validation = valid(substance, filter);
            if (!validation.isValid()) {
                continue;
            }
            //??????Filter??????ID????????????
            List<AlertMailFilterStrategy> strategyList = strategyDao.selectStrategiesByFilterId(filter.getId());
            if (!CollectionUtils.isEmpty(strategyList)) {
                AlertsV2Vo alert = parseAlert(substance, strategyList);
                if (alert != null) {
                    // ?????? alert ??????
//                    ObjectMapper objectMapper = new ObjectMapper();
//                    String jsonString = "{}";
//                    try {
//                        jsonString = objectMapper.writeValueAsString(alert);
//                    } catch (JsonProcessingException e) {
//                    }
//                    JSONObject alertJson = JSONObject.parseObject(jsonString);
//                    List<AlertFieldRequestDTO> alertFieldList = alertFieldBiz.getModelField(AlertConfigConstants.REDIS_MODEL_ALERT);
//                    if (com.aspire.mirror.alert.server.util.StringUtils.isNotEmpty(alert.getDeviceIp())){
//                        // ??????  ?????? + IP, ????????????
//                        cmdbHelper.queryDeviceForAlertV2(alertJson, alertFieldList,alertFieldBiz.getModelField(AlertConfigConstants.REDIS_MODEL_DEVICE_INSTANCE));
//                    }
//                    String alertId = alertsBiz.insert(AlertV2CommonUtils.generateAlerts(alertJson, alertFieldList));;
                    //???????????????alert_alerts??????
                    String alertId = alertsHandleV2Helper.handleAlert(alert);

                    logger.info("recipient alert: {}", JSON.toJSONString(alert));
                    Date resolveTime = new Date();
                    AlertMailResolveRecord resolveRecord = new AlertMailResolveRecord();
                    resolveRecord.setFilterId(filter.getId());
                    resolveRecord.setMailTitle(substance.getSubject());
                    resolveRecord.setMailContent(substance.getContent());
                    resolveRecord.setMailSender(substance.getSender());
                    resolveRecord.setMailReceiver(substance.getReceiver());
                    resolveRecord.setMailSendTime(substance.getSendTime());
                    resolveRecord.setResolveTime(resolveTime);
                    resolveRecord.setDeviceIp(alert.getDeviceIp());
                    resolveRecord.setMoniObject(alert.getMoniObject());
                    resolveRecord.setMoniIndex(alert.getMoniIndex());
                    resolveRecord.setAlertLevel(alert.getAlertLevel());
                    if (StringUtils.isEmpty(alertId)) {
                        alertId = "-1";
                    }
                    resolveRecord.setAlertId(alertId);
                    //???????????????alert_mail_resolve_record
                    resolveRecordDao.insertResolveRecords(resolveRecord);
                    break;
                } else {
                    logger.error("#===> ????????????????????????????????????????????????! substance: {}", JSON.toJSONString(substance));
                }
            } else {
                logger.info("#===> ???????????????????????????! ????????????ID: {}", filter.getId());
            }
        }
    }

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @KafkaListener(topics = {"ALERT_MAIL_SUBSTANCE"})
    private void consume(String data) {
        List<AlertMailSubstance> sbustanceList = JSON.parseArray(data, AlertMailSubstance.class);
        logger.info("kafka????????????" + sbustanceList);
        if (!CollectionUtils.isEmpty(sbustanceList)) {
            for (AlertMailSubstance substance : sbustanceList) {
                taskExecutor.execute(() -> resolve(substance));
            }
        }
    }

    private class Validation {
        private boolean valid;
        private String message;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
