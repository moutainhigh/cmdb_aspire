package com.aspire.mirror.alert.server.biz.notify.impl;

import com.aspire.mirror.alert.server.biz.notify.AlertNotifyConfigBiz;
import com.aspire.mirror.alert.server.biz.helper.EmailSendHelper;
import com.aspire.mirror.alert.server.biz.helper.SmSendHelper;
import com.aspire.mirror.alert.server.constant.AlertCommonConstant;
import com.aspire.mirror.alert.server.dao.notify.AlertNotifyConfigDao;
import com.aspire.mirror.alert.server.dao.common.AlertScheduleIndexDao;
import com.aspire.mirror.alert.server.dao.notify.AlertSubscribeRulesDao;
import com.aspire.mirror.alert.server.dao.alert.AlertsStatisticDao;
import com.aspire.mirror.alert.server.dao.alert.po.Alerts;
import com.aspire.mirror.alert.server.dao.notify.po.*;
import com.aspire.mirror.alert.server.vo.common.AlertScheduleIndexVo;
import com.aspire.mirror.alert.server.util.AlertFilterCommonUtil;
import com.aspire.mirror.alert.server.util.TransformUtils;
import com.aspire.mirror.alert.server.biz.alert.AlertsBizV2;
import com.aspire.mirror.alert.server.biz.alert.AlertsHisBizV2;
import com.aspire.mirror.alert.server.biz.notify.IAlertNotifyTemplateBiz;
import com.aspire.mirror.alert.server.constant.AlertConfigConstants;
import com.aspire.mirror.common.entity.Page;
import com.aspire.mirror.common.util.DateUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class AlertNotifyConfigBizImpl implements AlertNotifyConfigBiz {

    @Autowired
    private AlertNotifyConfigDao alertNotifyConfigDao;
    @Autowired
    private AlertsStatisticDao alertsStatisticDao;
    @Autowired
    private EmailSendHelper mailHelper;
    @Autowired
    private SmSendHelper smsSendHelper;
    @Autowired
    private IAlertNotifyTemplateBiz alertNotifyTemplateBiz;

    @Autowired
    private AlertsBizV2 alertsBizV2;
    @Autowired
    private AlertsHisBizV2 alertsHisBizV2;
    @Autowired
    private AlertScheduleIndexDao alertScheduleIndexDao;

    @Autowired
    private AlertSubscribeRulesDao alertSubscribeRulesDao;

    @Override
    public Object getAlertNotifyConfigList(String name,
                                           String isOpen,
                                           String notifyType,
                                           String alertFilter,
                                           String notifyObj,
                                           String isRecurrenceInterval,
                                           String sendTimeStart,
                                           String sendTimeEnd,
                                           int pageNum,
                                           int pageSize) {
        Map<String, Object> response = Maps.newHashMap();
        try {
            Map<String, Object> mapRequest = Maps.newHashMap();
            // ????????????
            mapRequest.put("name", name);
            mapRequest.put("isOpen", Integer.parseInt(isOpen));
            mapRequest.put("alertFilter", StringUtils.isBlank(alertFilter) ? 0 : Integer.parseInt(alertFilter));
            mapRequest.put("isRecurrenceInterval", Integer.parseInt(isRecurrenceInterval));
            if (StringUtils.isNotBlank(sendTimeStart) && StringUtils.isNotBlank(sendTimeEnd)) {
                mapRequest.put("sendTime", 1);
                mapRequest.put("sendTimeStart", sendTimeStart);
                mapRequest.put("sendTimeEnd", sendTimeEnd);
            } else {
                mapRequest.put("sendTime", -1);
            }
            mapRequest.put("isPage", 1);
            mapRequest.put("begin", (pageNum - 1) * pageSize);
            mapRequest.put("pageSize", pageSize);
            // ??????????????????
            if (!"-1".equals(notifyType) || StringUtils.isNotBlank(notifyObj)) {
                mapRequest.put("notifyObjInfo", 1);
                mapRequest.put("notifyType", "-1".equals(notifyType) ? "" : notifyType);
                mapRequest.put("notifyObj", notifyObj);
            }
            log.info("Searching Alert Notify Config List Request is {}", mapRequest);
            List<Map<String, Object>> alertNotifyConfigList = alertNotifyConfigDao.getAlertNotifyConfigList(mapRequest);
            response.put("count", alertNotifyConfigList.isEmpty() ? 0 : alertNotifyConfigList.size());
            response.put("result", alertNotifyConfigList);
        } catch (Exception e) {
            log.error("Searching Alert Notify Config List is error " + e);
        }
        return response;
    }

    @Override
    public String createAlertNotifyConfig(AlertNotifyConfigReqDTO request) {
        try {
            String uuid = UUID.randomUUID().toString();
            request.setUuid(uuid);
            List<Map<String, String>> notifyObjInfo = request.getNotifyObjInfo();
//            List<String> notifyType = Lists.newArrayList();
            if (!notifyObjInfo.isEmpty()) {
                for (Map<String, String> map : notifyObjInfo) {
                    map.put("uuid", UUID.randomUUID().toString());
                    map.put("alertNotifyConfigId", uuid);
                    if ("??????".equals(map.get("notifyObjType"))) {
                        map.put("notifyObjType", "2");
                    }
                }
//                request.setNotifyType(notifyType.toString().substring(1, notifyType.toString().length() - 1));
                alertNotifyConfigDao.createAlertNotifyConfig(TransformUtils.transform(AlertNotifyConfigReqDTO.class, request));
                alertNotifyConfigDao.createAlertNotifyConfigReceiver(notifyObjInfo);
            }
            return "success";
        } catch (Exception e) {
            log.error("Creating Alert Notify Config is error" + e);
            return "error";
        }
    }

    @Override
    public String updateAlertNotifyConfig(AlertNotifyConfigReqDTO request) {
        try {
            List<String> id = Lists.newArrayList();
            id.add(request.getUuid());
            alertNotifyConfigDao.deleteAlertNotifyConfig(id);
            alertNotifyConfigDao.deleteAlertNotifyConfigReceiver(id);

            List<Map<String, String>> notifyObjInfo = request.getNotifyObjInfo();
//            List<String> notifyType = Lists.newArrayList();
            if (!notifyObjInfo.isEmpty()) {
                for (Map<String, String> map : notifyObjInfo) {
                    map.put("uuid", UUID.randomUUID().toString());
                    map.put("alertNotifyConfigId", request.getUuid());
                    if ("??????".equals(map.get("notifyObjType"))) {
                        map.put("notifyObjType", "2");
                    }
                }
//                request.setNotifyType(notifyType.toString().substring(1, notifyType.toString().length() - 1));
                alertNotifyConfigDao.createAlertNotifyConfig(TransformUtils.transform(AlertNotifyConfigReqDTO.class, request));
                alertNotifyConfigDao.createAlertNotifyConfigReceiver(notifyObjInfo);
            }
            return "success";
        } catch (Exception e) {
            log.error("Creating Alert Notify Config is error" + e);
            return "error";
        }
    }

    @Override
    public Object getAlertNotifyConfigDetail(String uuid) {
        AlertNotifyConfigDetail alertNotifyConfigDetail = null;
        try {
            alertNotifyConfigDetail = alertNotifyConfigDao.getAlertNotifyConfigDetail(uuid);
            List<AlertNotifyConfigReceiverDetail> notifyObjInfo = alertNotifyConfigDetail.getNotifyObjInfo();
            String objList = "";
            if (!notifyObjInfo.isEmpty()) {
                for (AlertNotifyConfigReceiverDetail detail : notifyObjInfo) {
                    objList = objList + "," + detail.getNotifyObjInfo().split("-")[0];
                    if ("??????".equals(detail.getNotifyObjType())) {
                        alertNotifyConfigDetail.setPersonalNotifyObjFlag(Boolean.TRUE);
                    }
                }
                alertNotifyConfigDetail.setPersonalNotifyObjLists(objList.substring(1, objList.length()));
            }
        } catch (Exception e) {
            log.error("Getting Alert Notify Config Detail is error" + e);
        }
        return alertNotifyConfigDetail;
    }

    @Override
    public String deleteAlertNotifyConfig(List<String> uuidList) {
        try {
            alertNotifyConfigDao.deleteAlertNotifyConfigReceiver(uuidList);
            alertNotifyConfigDao.deleteAlertNotifyConfig(uuidList);
            return "success";
        } catch (Exception e) {
            log.error("Deleting Alert Notify Config is error" + e);
            return "error";
        }
    }

    @Override
    public String openAlertNotifyConfig(List<String> uuidList) {
        Map<String, Object> open = Maps.newHashMap();
        open.put("isOpen", 1);
        open.put("uuidList", uuidList);
        try {
            alertNotifyConfigDao.updateAlertNotifyConfigForOpen(open);
            return "success";
        } catch (Exception e) {
            log.error("Opening Alert Notify Config is error" + e);
            return "error";
        }
    }

    @Override
    public String closeAlertNotifyConfig(List<String> uuidList) {
        Map<String, Object> close = Maps.newHashMap();
        close.put("isOpen", 0);
        close.put("uuidList", uuidList);
        try {
            alertNotifyConfigDao.updateAlertNotifyConfigForOpen(close);
            return "success";
        } catch (Exception e) {
            log.error("Closing Alert Notify Config is error" + e);
            return "error";
        }
    }

    @Override
    public String copyAlertNotifyConfig(String uuid) {
        try {
            AlertNotifyConfigDetail alertNotifyConfigDetail = alertNotifyConfigDao.getAlertNotifyConfigDetail(uuid);
            AlertNotifyConfigReqDTO request = new AlertNotifyConfigReqDTO();
            String newUuid = UUID.randomUUID().toString();
            request.setUuid(newUuid);
            String name = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            request.setName(alertNotifyConfigDetail.getName() + name);
            request.setIsOpen(alertNotifyConfigDetail.getIsOpen());
            request.setAlertFilterId(alertNotifyConfigDetail.getAlertFilterId());
            request.setAlertFilterSceneId(alertNotifyConfigDetail.getAlertFilterSceneId());
//            request.setNotifyType(alertNotifyConfigDetail.getNotifyType());
            request.setNotifyAlertType(alertNotifyConfigDetail.getNotifyAlertType());
            request.setIsRecurrenceInterval(alertNotifyConfigDetail.getIsRecurrenceInterval());
            request.setRecurrenceInterval(alertNotifyConfigDetail.getRecurrenceInterval());
            request.setRecurrenceIntervalUtil(alertNotifyConfigDetail.getRecurrenceIntervalUtil());
            request.setEmailType(alertNotifyConfigDetail.getEmailType());
            request.setCreator(alertNotifyConfigDetail.getCreator());
            request.setPeriod(alertNotifyConfigDetail.getPeriod());
            request.setStartPeriod(alertNotifyConfigDetail.getStartPeriod());
            request.setEndPeriod(alertNotifyConfigDetail.getEndPeriod());
            List<Map<String, String>> notifyObjInfo = Lists.newArrayList();
            List<AlertNotifyConfigReceiverDetail> notifyObjInfo1 = alertNotifyConfigDetail.getNotifyObjInfo();
            for (AlertNotifyConfigReceiverDetail detail : notifyObjInfo1) {
                Map<String, String> map = Maps.newHashMap();
                map.put("uuid", UUID.randomUUID().toString());
                map.put("alertNotifyConfigId", newUuid);
                if ("??????".equals(detail.getNotifyObjType())) {
                    map.put("notifyObjType", "2");
                }
                map.put("notifyObjInfo", detail.getNotifyObjInfo());
                map.put("notifyType", detail.getNotifyType());
                notifyObjInfo.add(map);
            }
//            request.setNotifyType(notifyType.toString().substring(1, notifyType.toString().length() - 1));
            alertNotifyConfigDao.createAlertNotifyConfig(TransformUtils.transform(AlertNotifyConfigReqDTO.class, request));
            alertNotifyConfigDao.createAlertNotifyConfigReceiver(notifyObjInfo);
            return "success";
        } catch (Exception e) {
            log.error("Copying Alert Notify Config is error is {}", e);
            return "error";
        }
    }

    private Map<String, String> sendSmsByTemplate(Map<String, Object> alert, List<String> mobileList) {
        if (alert != null && !alert.isEmpty()) {
            return alertNotifyTemplateBiz.sendSms(AlertConfigConstants.MESSAGE_TEMPLATE_ALERT_TEMPLATE,
                    mobileList, alert);
        } else {
            Map<String, String> map = Maps.newHashMap();
            map.put("code", "9998");
            map.put("desc", "alert is not exist!");
            return map;
        }
    }

    private Map<String, String> sendEmailByTemplate(List<Map<String, Object>> alertList, List<String> emailList) {
        String subject = "??????????????????";
        return alertNotifyTemplateBiz.sendEmail(AlertConfigConstants.MESSAGE_TEMPLATE_ALERT_TEMPLATE,
                subject, false, emailList, null, alertList);
    }

    // ????????????
    private boolean SMSSend(String message, List<String> mobileList) {
        return smsSendHelper.send(mobileList, message);
    }

    // ????????????
    private boolean emailSend(String message, List<String> emailList) {
        String[] strings = emailList.toArray(new String[emailList.size()]);
        String subject = "??????????????????";
        try {
            mailHelper.sendMail(subject, message, false, strings);
            return true;
        } catch (Exception e) {
            log.error("send email is error >>>", e);
            return false;
        }
    }

    //????????????????????????
    private List<Map<String, Object>> getNewAlerts(List<Map<String, Object>> alertsFromOperationRecord, List<Alerts> listAlerts,
                                                   Date startTime, Date end) {
        Calendar beginTime = Calendar.getInstance();
        beginTime.setTime(startTime);
        Calendar endTime = Calendar.getInstance();
        endTime.setTime(end);
        List<String> list = Lists.newArrayList();
        for (Alerts alerts : listAlerts) {
            Calendar alertTime = Calendar.getInstance();
            alertTime.setTime(alerts.getCreateTime());
            boolean startT = alertTime.after(beginTime);
            boolean endT = alertTime.before(endTime);
            boolean orderStatus = alerts.getOrderStatus().equals("1");
            boolean operationStatus = alerts.getOperateStatus() == 0;
            if (startT && endT && orderStatus && operationStatus) {
                Map<String, Object> map = Maps.newHashMap();
                map.put("alertNotifyType", "-1");
                map.put("alertId", alerts.getAlertId());
                alertsFromOperationRecord.add(map);
            }
        }
        return alertsFromOperationRecord;
    }


    @Override
    public Map<String, String> getAlertNotifyConfigRule() {
        Map<String, String> response = Maps.newHashMap();
        List<Map<String, String>> alertNotifyConfigRule = alertNotifyConfigDao.getAlertNotifyConfigRule();
        for (Map<String, String> map : alertNotifyConfigRule) {
            response.put(map.get("ruleName"), map.get("ruleCron"));
        }
        return response;
    }

    @Override
    public String updateAlertNotifyConfigRule(Map<String, String> req) {
        try {
            req.remove("namespace");
            Map<String, String> m = Maps.newHashMap();
            m.put("ruleName", "send");
            m.put("ruleCron", req.get("send"));
            alertNotifyConfigDao.updateAlertNotifyConfigRule(m);
//            autoAlertNotifyConfigSchedule.resetTriggerTask(req);
            return "success";
        } catch (Exception e) {
            log.error("[ update Alert Notify Config Rule is error ] >>> {}", e);
            return "error";
        }
    }

    private final ConcurrentHashMap<String, Date> localLockMap = new ConcurrentHashMap<>(2);

    /**
     * ????????????????????????
     */
    @Override
    public void sendAlertNotifyConfigNew() {
        Date date = new Date(System.currentTimeMillis() / 1000 * 1000);
        // ????????????
        Date dateLock = localLockMap.get(AlertConfigConstants.REDIS_ALERT_AUTO_NOTIFY_LOCK);
        if (dateLock != null && dateLock.getTime() >= (date.getTime() - 1800 * 1000)) {
            log.info("Last alert order task not finished!");
            return;
        }
        localLockMap.put(AlertConfigConstants.REDIS_ALERT_AUTO_NOTIFY_LOCK, date);
        Map<String, String> date1 = getDate();
        String start = date1.get(AlertConfigConstants.START);
        String end = date1.get(AlertConfigConstants.END);
        List<String> sendOperation = Lists.newArrayList();
        try {
            // ??????????????????????????????????????????
            Map<String, String> param = Maps.newHashMap();
            param.put("send", "");
            param.put("resend", "");
            final List<AlertNotifyConfigDetail> alertNotifyConfig = alertNotifyConfigDao.getAlertNotifyConfig(param);
            Map<String, List<Map<String, String>>> dEmailData = Maps.newHashMap();
            List<AlertNotifyConfigLog> alertNotifyConfigLogs = Lists.newArrayList();
            Map<String, Map<String, Object>> alertMap = Maps.newHashMap();
            String hour = new SimpleDateFormat("HH").format(date);
            if (alertNotifyConfig != null && alertNotifyConfig.size() > 0) {
                for (AlertNotifyConfigDetail alertNotifyConfigDetail : alertNotifyConfig) {
                    try {
                        if ("1".equals(alertNotifyConfigDetail.getPeriod()) && !(Integer.parseInt(alertNotifyConfigDetail.getStartPeriod()) <= Integer.parseInt(hour)
                                && Integer.parseInt(alertNotifyConfigDetail.getEndPeriod()) > Integer.parseInt(hour)))
                            continue;
                        // ???????????????????????????????????????????????????
                        Map<String, Object> alertFilterSceneInfo = alertNotifyConfigDetail.getAlertFilterSceneInfo();//?????????
                        if (alertFilterSceneInfo == null || alertFilterSceneInfo.get("optionCondition") == null) {
                            continue;
                        }
                        String optionCondition = AlertFilterCommonUtil.getCondition(String.valueOf(alertFilterSceneInfo.get("optionCondition")));
                        Set<String> alertIds = Sets.newHashSet();
                        // ????????????????????????
                        String notifyAlertType = alertNotifyConfigDetail.getNotifyAlertType();
                        if (StringUtils.isEmpty(notifyAlertType)) {
                            continue;
                        }
                        String[] split1 = notifyAlertType.split(",");
                        ArrayList<String> typeList = new ArrayList<String>(Arrays.asList(split1));
                        //????????????
                        if (typeList.contains("-1")) {
                            Map<String, Object> condition = Maps.newHashMap();
                            condition.put("condition", optionCondition);
                            condition.put("startTime", start);
                            condition.put("endTime", end);
                            Page page = new Page();
                            page.setBegin(null);
                            page.setPageSize(null);
                            page.setParams(condition);
                            // ???????????????????????????????????????
                            List<Map<String, Object>> hisAlerts = alertsStatisticDao.filterMapList(page);//????????????????????????
                            for (Map<String, Object> hisMap : hisAlerts) {
                                String alertId = MapUtils.getString(hisMap, "alert_id");
                                alertIds.add(alertId);
                                alertMap.put(alertId, hisMap);
                            }
                        }
                        //????????????
                        if (typeList.contains("3")) {
                            // ????????????????????????????????????????????????????????????????????????
                            Map<String, Object> req = Maps.newHashMap();
                            req.put("startTime", start);
                            req.put("endTime", end);
                            req.put("typeList", typeList);
                            req.put("condition", optionCondition);
                            //?????????????????????????????????
                            List<Map<String, Object>> hisAlerts = alertNotifyConfigDao.getHisAlerts(req);
                            for (Map<String, Object> hisMap : hisAlerts) {
                                String alertId = MapUtils.getString(hisMap, "alert_id");
                                alertIds.add(alertId);
                                alertMap.put(alertId, hisMap);
                            }
                        }
                        // ???????????????????????????????????????????????????
                        if (CollectionUtils.isEmpty(alertIds)) {
                            continue;
                        }

                        // ????????????????????????
                        // ?????????????????????
                        List<AlertNotifyConfigReceiverDetail> notifyObjInfo = alertNotifyConfigDetail.getNotifyObjInfo();
                        Map<String, Set<String>> mobileAndEmailMap = Maps.newHashMap();
                        for (AlertNotifyConfigReceiverDetail ancrd : notifyObjInfo) {
                            String[] notifyTypeArray = ancrd.getNotifyObjInfo().split("-");
                            String type = ancrd.getNotifyType();
                            for (int i = 0; i < notifyTypeArray.length; i++) {
                                if (i == 0) {
                                    continue;
                                }
                                String s = notifyTypeArray[i];
                                Set<String> NotifyTypeSet = mobileAndEmailMap.get(s);
                                if (NotifyTypeSet == null) {
                                    NotifyTypeSet = Sets.newHashSet();
                                }
                                NotifyTypeSet.add(type);
                                mobileAndEmailMap.put(s, NotifyTypeSet);
                            }
                        }

                        for (String alertId : alertIds) {
                            // ????????????????????????????????????
                            for (Map.Entry<String, Set<String>> entry : mobileAndEmailMap.entrySet()) {
                                String key = entry.getKey();
                                Set<String> value = entry.getValue();
                                if (StringUtils.isNumeric(key)) {
                                    // ????????????
                                    if (value.contains("0") || value.contains("2")) {
                                        sendMobileMessage(alertMap.get(alertId), key, alertNotifyConfigDetail.getUuid(), alertNotifyConfigLogs, sendOperation, date, null);
                                    }
                                } else {
                                    // ????????????
                                    if (value.contains("0") || value.contains("1")) {
                                        if (alertNotifyConfigDetail.getEmailType() == 2) { //2-??????;1-????????????
                                            // ??????????????????
                                            sendEmailMessage(alertMap.get(alertId), key, alertNotifyConfigDetail.getUuid(), alertNotifyConfigLogs, sendOperation, date, null);
                                        } else {
                                            getEmailMerge(alertId, key, alertNotifyConfigDetail.getUuid(), dEmailData, sendOperation);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.error("???????????????????????????????????????: {},{}", alertNotifyConfigDetail.getName(), e);
                    }

                }
            }

            // ????????????(??????)
            if (!CollectionUtils.isEmpty(dEmailData)) {
                for (Map.Entry<String, List<Map<String, String>>> entry : dEmailData.entrySet()) {
                    String str = entry.getKey();
                    List<Map<String, String>> mapList = entry.getValue();
                    List<String> newList = Lists.newArrayList();
                    List<Map<String, Object>> alertList = Lists.newArrayList();
                    for (Map<String, String> map : mapList) {
                        String alertId = map.get("alertId");
                        if (!newList.contains(alertId)) {
                            newList.add(alertId);
                            alertList.add(alertMap.get(alertId));
                        }
                    }

                    List<String> emailList = Lists.newArrayList();
                    emailList.add(str);
                    Map<String, String> resultMap = sendEmailByTemplate(alertList, emailList);
                    boolean emailSend = true;
                    if (!"0000".equals(resultMap.get("code"))) {
                        emailSend = false;
                    }
                    String message = resultMap.get("content");
                    if (StringUtils.isEmpty(message)) {
                        message = resultMap.get("desc");
                    }
                    Date date2 = new Date();
                    for (Map<String, String> map : mapList) {
                        AlertNotifyConfigLog alertNotifyConfigLog = new AlertNotifyConfigLog();
                        alertNotifyConfigLog.setAlertNotifyConfigId(map.get("alertNotifyConfigId"));
                        alertNotifyConfigLog.setReceiver(str);
                        alertNotifyConfigLog.setSendStatus(emailSend ? "1" : "0");
                        alertNotifyConfigLog.setSendContent(message);
                        alertNotifyConfigLog.setSendType("3");
                        alertNotifyConfigLog.setSendTime(date2);
                        alertNotifyConfigLog.setSendAlertId(map.get("alertId"));
                        alertNotifyConfigLogs.add(alertNotifyConfigLog);
                    }
                }
            }
            // ?????????????????????????????????
            log.info("alertNotifyConfigLogs >>>" + alertNotifyConfigLogs);
            if (alertNotifyConfigLogs != null && alertNotifyConfigLogs.size() > 0) {
                alertNotifyConfigDao.insertAlertNotifyConfigLog(alertNotifyConfigLogs);
                List<String> alertIdList = alertNotifyConfigLogs.stream().map(AlertNotifyConfigLog::getSendAlertId).collect(Collectors.toList());
                alertsBizV2.updateNotifyStatus(alertIdList, AlertCommonConstant.NUM.ONE);
                Set<String> idSet = Sets.newHashSet();
                Map<String, Object> map = Maps.newHashMap();
                for (AlertNotifyConfigLog alertNotifyConfigLog : alertNotifyConfigLogs) {
                    String id = alertNotifyConfigLog.getAlertNotifyConfigId();
                    if (StringUtils.isEmpty(id) || idSet.contains(id)) {
                        continue;
                    }
                    idSet.add(id);
                    map.put("alertNotifyConfigId", id);
                    alertNotifyConfigDao.updateAlertNotifyConfig(map);
                }
            }
            log.info("[ send alert notify config is success ]");

        } catch (Exception e) {
            log.error("[ send alert notify config is error ] >>> {}", e);
        }
        sendAlertNotifyConfigNew(start, end, date, sendOperation);
        localLockMap.remove(AlertConfigConstants.REDIS_ALERT_AUTO_NOTIFY_LOCK);

    }


    /**
     * ????????????????????????
     */
    @Override
    public void resendAlertNotifyConfigNew() {
        Date date = new Date(System.currentTimeMillis() / 1000 * 1000);
        // ????????????
        Date dateLock = localLockMap.get(AlertConfigConstants.REDIS_ALERT_AUTO_NOTIFY_LOCK_RESEND);
        if (dateLock != null && dateLock.getTime() >= (date.getTime() - 1800 * 1000)) {
            log.info("Last alert order task not finished!");
            return;
        }
        localLockMap.put(AlertConfigConstants.REDIS_ALERT_AUTO_NOTIFY_LOCK_RESEND, date);
        try {
            // ??????????????????????????????????????????
            Map<String, String> param = Maps.newHashMap();
            param.put("resend", "resend");
            final List<AlertNotifyConfigDetail> alertNotifyConfig = alertNotifyConfigDao.getAlertNotifyConfig(param);
            Map<String, List<Map<String, String>>> dEmailData = Maps.newHashMap();
            List<AlertNotifyConfigLog> alertNotifyConfigLogs = Lists.newArrayList();
            List<String> sendOperation = Lists.newArrayList();
            Map<String, Map<String, Object>> alertMaps = Maps.newHashMap();
            String hour = new SimpleDateFormat("HH").format(date);
            if (alertNotifyConfig != null && alertNotifyConfig.size() > 0) {
                for (AlertNotifyConfigDetail alertNotifyConfigDetail : alertNotifyConfig) {
                    try {
                        if ("1".equals(alertNotifyConfigDetail.getPeriod()) && !(Integer.parseInt(alertNotifyConfigDetail.getStartPeriod()) <= Integer.parseInt(hour)
                                && Integer.parseInt(alertNotifyConfigDetail.getEndPeriod()) > Integer.parseInt(hour)))
                            continue;
                        // ???????????????????????????????????????????????????
                        Map<String, Object> alertFilterSceneInfo = alertNotifyConfigDetail.getAlertFilterSceneInfo();//?????????
                        if (alertFilterSceneInfo == null || alertFilterSceneInfo.get("optionCondition") == null) {
                            continue;
                        }
                        String optionCondition = AlertFilterCommonUtil.getCondition(String.valueOf(alertFilterSceneInfo.get("optionCondition")));
//                    Set<String> alertIds = Sets.newHashSet();
                        // ????????????????????????
                        String notifyAlertType = alertNotifyConfigDetail.getNotifyAlertType();
                        if (StringUtils.isEmpty(notifyAlertType)) {
                            continue;
                        }
                        String[] split1 = notifyAlertType.split(",");
                        ArrayList<String> typeList = new ArrayList<String>(Arrays.asList(split1));

                        // ???????????????????????????????????????????????????????????????
                        if (!typeList.contains("-1") && !typeList.contains("2")) {
                            continue;
                        }
                        Integer recurrenceCount = alertNotifyConfigDetail.getRecurrenceCount();
                        if (recurrenceCount == null) {
                            recurrenceCount = -1;
                        }
                        // ???????????????0 ????????????
                        if (recurrenceCount == 0) {
                            continue;
                        }
                        //????????????
                        Map<String, Object> condition = Maps.newHashMap();
                        condition.put("condition", optionCondition);
                        condition.put("alertNotifyConfigId", alertNotifyConfigDetail.getUuid());
                        if (!typeList.contains("-1")) {
                            condition.put("orderId", notifyAlertType);
                        }
                        if (recurrenceCount > 0) {
                            condition.put("recurrenceCount", recurrenceCount);
                        }
                        // ???????????????????????????
                        List<Map<String, Object>> mapList = alertNotifyConfigDao.getReSendAlerts(condition);

                        // ???????????????????????????????????????????????????
                        if (CollectionUtils.isEmpty(mapList)) {
                            continue;
                        }
                        // ????????????????????????
                        // ?????????????????????
                        List<AlertNotifyConfigReceiverDetail> notifyObjInfo = alertNotifyConfigDetail.getNotifyObjInfo();
                        Map<String, Set<String>> mobileAndEmailMap = Maps.newHashMap();
                        for (AlertNotifyConfigReceiverDetail ancrd : notifyObjInfo) {
                            String[] notifyTypeArray = ancrd.getNotifyObjInfo().split("-");
                            String type = ancrd.getNotifyType();
                            for (int i = 0; i < notifyTypeArray.length; i++) {
                                if (i == 0) {
                                    continue;
                                }
                                String s = notifyTypeArray[i];
                                Set<String> NotifyTypeSet = mobileAndEmailMap.get(s);
                                if (NotifyTypeSet == null) {
                                    NotifyTypeSet = Sets.newHashSet();
                                }
                                NotifyTypeSet.add(type);
                                mobileAndEmailMap.put(s, NotifyTypeSet);
                            }
                        }

                        Integer recurrenceInterval = 5;
                        String recurrenceInterval1 = alertNotifyConfigDetail.getRecurrenceInterval();
                        if (!StringUtils.isEmpty(recurrenceInterval1) && StringUtils.isNumeric(recurrenceInterval1)) {
                            recurrenceInterval = Integer.parseInt(recurrenceInterval1);
                        }
                        if ("h".equalsIgnoreCase(alertNotifyConfigDetail.getRecurrenceIntervalUtil())) {
                            recurrenceInterval = recurrenceInterval * 60;
                        }
                        for (Map<String, Object> alertMap : mapList) {
                            String alertId = MapUtils.getString(alertMap, "alert_id");
                            String receiver = MapUtils.getString(alertMap, "receiver_tmp");
                            String sendTime = MapUtils.getString(alertMap, "send_time_tmp");
                            if (StringUtils.isEmpty(alertId)) {
                                continue;
                            }
                            alertMaps.put(alertId, alertMap);
                            // ????????????????????????????????????
                            if (StringUtils.isEmpty(receiver) || StringUtils.isEmpty(sendTime)) {
                                for (Map.Entry<String, Set<String>> entry : mobileAndEmailMap.entrySet()) {
                                    String key = entry.getKey();
                                    Set<String> value = entry.getValue();
                                    if (StringUtils.isNumeric(key)) {
                                        // ????????????
                                        if (value.contains("0") || value.contains("2")) {
                                            sendMobileMessage(alertMap, key, alertNotifyConfigDetail.getUuid(), alertNotifyConfigLogs, sendOperation, date, null);
                                        }
                                    } else {
                                        // ????????????
                                        if (value.contains("0") || value.contains("1")) {
                                            if (alertNotifyConfigDetail.getEmailType() == 2) { //2-??????;1-????????????
                                                // ??????????????????
                                                sendEmailMessage(alertMap, key, alertNotifyConfigDetail.getUuid(), alertNotifyConfigLogs, sendOperation, date, null);
                                            } else {
                                                getEmailMerge(alertId, key, alertNotifyConfigDetail.getUuid(), dEmailData, sendOperation);
                                            }
                                        }
                                    }
                                }
                                continue;
                            }

                            Date sendTimes = DateUtil.getDate(sendTime, DateUtil.DATE_TIME_CH_FORMAT);
                            // ???????????????????????????
                            if (sendTimes.getTime() > (date.getTime() - recurrenceInterval * 60 * 1000)) {
                                continue;
                            }

                            Set<String> notifyTypeSet = mobileAndEmailMap.get(receiver);
                            if (CollectionUtils.isEmpty(notifyTypeSet)) {
                                continue;
                            }
                            if (StringUtils.isNumeric(receiver)) {
                                // ????????????
                                if (notifyTypeSet.contains("0") || notifyTypeSet.contains("2")) {
                                    sendMobileMessage(alertMap, receiver, alertNotifyConfigDetail.getUuid(), alertNotifyConfigLogs, sendOperation, date, null);
                                }
                            } else {
                                // ????????????
                                if (notifyTypeSet.contains("0") || notifyTypeSet.contains("1")) {
                                    if (alertNotifyConfigDetail.getEmailType() == 2) { //2-??????;1-????????????
                                        // ??????????????????
                                        sendEmailMessage(alertMap, receiver, alertNotifyConfigDetail.getUuid(), alertNotifyConfigLogs, sendOperation, date, null);
                                    } else {
                                        getEmailMerge(alertId, receiver, alertNotifyConfigDetail.getUuid(), dEmailData, sendOperation);
                                    }
                                }
                            }

                        }
                    } catch (Exception e) {
                        log.error("???????????????????????????????????????: {},{}", alertNotifyConfigDetail.getName(), e);
                    }
                }
            }

            // ????????????(??????)
            if (!CollectionUtils.isEmpty(dEmailData)) {
                for (Map.Entry<String, List<Map<String, String>>> entry : dEmailData.entrySet()) {
                    String str = entry.getKey();
                    List<Map<String, String>> mapList = entry.getValue();
                    List<String> newList = Lists.newArrayList();
                    List<Map<String, Object>> alertList = Lists.newArrayList();
                    for (Map<String, String> map : mapList) {
                        String alertId = map.get("alertId");
                        if (!newList.contains(alertId)) {
                            newList.add(alertId);
                            alertList.add(alertMaps.get(alertId));
                        }
                    }

                    List<String> emailList = Lists.newArrayList();
                    emailList.add(str);
                    Map<String, String> resultMap = sendEmailByTemplate(alertList, emailList);
                    boolean emailSend = true;
                    if (!"0000".equals(resultMap.get("code"))) {
                        emailSend = false;
                    }
                    String message = resultMap.get("content");
                    if (StringUtils.isEmpty(message)) {
                        message = resultMap.get("desc");
                    }
                    for (Map<String, String> map : mapList) {
                        AlertNotifyConfigLog alertNotifyConfigLog = new AlertNotifyConfigLog();
                        alertNotifyConfigLog.setAlertNotifyConfigId(map.get("alertNotifyConfigId"));
                        alertNotifyConfigLog.setReceiver(str);
                        alertNotifyConfigLog.setSendStatus(emailSend ? "1" : "0");
                        alertNotifyConfigLog.setSendContent(message);
                        alertNotifyConfigLog.setSendType("3");
                        alertNotifyConfigLog.setSendTime(date);
                        alertNotifyConfigLog.setSendAlertId(map.get("alertId"));
                        alertNotifyConfigLogs.add(alertNotifyConfigLog);
                    }
                }
            }
            // ?????????????????????????????????
            log.info("alertNotifyConfigLogs >>>" + alertNotifyConfigLogs);
            if (alertNotifyConfigLogs != null && alertNotifyConfigLogs.size() > 0) {
                alertNotifyConfigDao.insertAlertNotifyConfigLog(alertNotifyConfigLogs);
                List<String> alertIdList = alertNotifyConfigLogs.stream().map(AlertNotifyConfigLog::getSendAlertId
                ).collect(Collectors.toList());
                alertsBizV2.updateNotifyStatus(alertIdList, AlertCommonConstant.NUM.ONE);
                Set<String> idSet = Sets.newHashSet();
                Map<String, Object> map = Maps.newHashMap();
                for (AlertNotifyConfigLog alertNotifyConfigLog : alertNotifyConfigLogs) {
                    String id = alertNotifyConfigLog.getAlertNotifyConfigId();
                    if (StringUtils.isEmpty(id) || idSet.contains(id)) {
                        continue;
                    }
                    idSet.add(id);
                    map.put("alertNotifyConfigId", id);
                    alertNotifyConfigDao.updateAlertNotifyConfig(map);
                }
            }
            log.info("[ send alert notify config is success ]");
        } catch (Exception e) {
            log.error("[ send alert notify config is error ] >>> {}", e);
        }
        localLockMap.remove(AlertConfigConstants.REDIS_ALERT_AUTO_NOTIFY_LOCK_RESEND);

    }

    /**
     * ??????????????????
     *
     * @param alert
     * @param mobile
     * @param alertNotifyConfigId
     * @param alertNotifyConfigLogs
     * @param sendOperation
     */
    private void sendMobileMessage(Map<String, Object> alert, String mobile, String alertNotifyConfigId, List<AlertNotifyConfigLog> alertNotifyConfigLogs, List<String> sendOperation, Date date, AlertSubscribeRulesDetail subscribeRulesDetail) {
        if (alert == null || alert.isEmpty()) {
            return;
        }
        // ????????????
        AlertNotifyConfigLog alertNotifyConfigLog = new AlertNotifyConfigLog();
        alertNotifyConfigLog.setAlertNotifyConfigId(alertNotifyConfigId);
        alertNotifyConfigLog.setSendTime(date);
        List<String> mobileList = Lists.newArrayList();
        mobileList.add(mobile);
        alertNotifyConfigLog.setReceiver(mobile);
        alertNotifyConfigLog.setSendType("1");
        String alertId = MapUtils.getString(alert,"alert_id");
        Map<String, String> p = Maps.newHashMap();
        p.put("receiver", mobile);
        p.put("alertId", alertId);
        p.put("sendType", "1");
        String send = mobile + "-" + alertId + "-1";
        if (!sendOperation.contains(send)) {
            sendOperation.add(send);
            Map<String, String> resultMap;
            if (subscribeRulesDetail == null) {
                resultMap = sendSmsByTemplate(alert, mobileList);
            } else {
                resultMap = sendSubscribeSmsByTemplate(alert, mobileList, subscribeRulesDetail);
            }

            boolean sms = true;
            if (!"0000".equals(resultMap.get("code"))) {
                sms = false;
            }
            String message = resultMap.get("content");
            if (StringUtils.isEmpty(message)) {
                message = resultMap.get("desc");
            }
            alertNotifyConfigLog.setSendContent(message);
            alertNotifyConfigLog.setSendStatus(sms ? "1" : "0");
            alertNotifyConfigLog.setSendAlertId(alertId);
            alertNotifyConfigLogs.add(alertNotifyConfigLog);
        }
    }

    /**
     * ??????????????????
     *
     * @param alert
     * @param email
     * @param alertNotifyConfigId
     * @param alertNotifyConfigLogs
     * @param sendOperation
     */
    private void sendEmailMessage(Map<String, Object> alert, String email, String alertNotifyConfigId, List<AlertNotifyConfigLog> alertNotifyConfigLogs, List<String> sendOperation, Date date, AlertSubscribeRulesDetail subscribeRulesDetail) {
        if (alert == null || alert.isEmpty()) {
            return;
        }
        AlertNotifyConfigLog alertNotifyConfigLog = new AlertNotifyConfigLog();
        alertNotifyConfigLog.setAlertNotifyConfigId(alertNotifyConfigId);
        alertNotifyConfigLog.setSendTime(date);
        List<String> emailList = Lists.newArrayList();
        emailList.add(email);
        alertNotifyConfigLog.setReceiver(email);
        alertNotifyConfigLog.setSendType("2");
        String alertId = MapUtils.getString(alert,"alert_id");
        Map<String, String> p = Maps.newHashMap();
        p.put("receiver", email);
        p.put("alertId", alertId);
        p.put("sendType", "2");
        String send = email + "-" + alertId + "-2";
        if (!sendOperation.contains(send)) {
            sendOperation.add(send);
            List<Map<String, Object>> alertList = Lists.newArrayList();
            alertList.add(alert);
            Map<String, String> resultMap;
            if (subscribeRulesDetail == null) {
                resultMap = sendEmailByTemplate(alertList, emailList);
            } else {
                resultMap = sendSubscribeEmailByTemplate(alertList, emailList, subscribeRulesDetail);
            }

            boolean emailSend = true;
            if (!"0000".equals(resultMap.get("code"))) {
                emailSend = false;
            }
            String message = resultMap.get("content");
            if (StringUtils.isEmpty(message)) {
                message = resultMap.get("desc");
            }
            alertNotifyConfigLog.setSendContent(message);
            alertNotifyConfigLog.setSendStatus(emailSend ? "1" : "0");
            alertNotifyConfigLog.setSendAlertId(alertId);
            alertNotifyConfigLogs.add(alertNotifyConfigLog);
        }
    }

    /**
     * ??????????????????
     *
     * @param alertId
     * @param email
     * @param alertNotifyConfigId
     * @param dEmailData
     */
    private void getEmailMerge(String alertId, String email, String alertNotifyConfigId, Map<String, List<Map<String, String>>> dEmailData, List<String> sendOperation) {
        List<Map<String, String>> mapList = dEmailData.get(email);

        if (CollectionUtils.isEmpty(mapList)) {
            mapList = Lists.newArrayList();
        }
        String send = email + "-" + alertId + "-2";
        if (!sendOperation.contains(send)) {
            sendOperation.add(send);
            Map<String, String> map = Maps.newHashMap();
            map.put("alertId", alertId);
            map.put("alertNotifyConfigId", alertNotifyConfigId);
            mapList.add(map);
            dEmailData.put(email, mapList);
        }
    }

    /**
     * ?????????????????????????????????
     *
     * @return
     */
    private Map<String, String> getDate() {
        long l = System.currentTimeMillis();
        Date endDate = new Date(l / 1000 * 1000);
        String endStr = new SimpleDateFormat(DateUtil.DATE_TIME_CH_FORMAT).format(endDate);
        Map<String, String> map = new HashMap<>(2);
        List<AlertScheduleIndexVo> indexDTOList = alertScheduleIndexDao.getAlertScheduleIndexDetail(AlertConfigConstants.ALERT_AUTO_NOTIFY_TYPE);
        if (!CollectionUtils.isEmpty(indexDTOList)) {
            int size = indexDTOList.size();
            for (int i = 0; i < size; i++) {
                // ?????????????????????
                if (i != 0) {
                    alertScheduleIndexDao.deleteById(indexDTOList.get(i).getId());
                }
            }
            AlertScheduleIndexVo alertScheduleIndexVo = indexDTOList.get(0);
            String startStr = alertScheduleIndexVo.getIndexValue();
            if (StringUtils.isEmpty(startStr) || !DateUtil.isValid(startStr, DateUtil.DATE_TIME_CH_FORMAT)) {
                alertScheduleIndexDao.deleteById(alertScheduleIndexVo.getId());
                getDateByNullValue(map, endStr, l);
            } else {
                map.put(AlertConfigConstants.START, startStr);
                Date startDate = DateUtil.getDate(startStr, DateUtil.DATE_TIME_CH_FORMAT);
                if (startDate == null) {
                    alertScheduleIndexDao.deleteById(alertScheduleIndexVo.getId());
                    getDateByNullValue(map, endStr, l);
                } else {
                    map.put(AlertConfigConstants.END, endStr);
                    alertScheduleIndexVo.setIndexValue(endStr);
                    alertScheduleIndexDao.updateAlertScheduleIndex(alertScheduleIndexVo);
                }
            }

        } else {
            getDateByNullValue(map, endStr, l);
        }

        return map;
    }

    /**
     * ????????????????????????????????????????????????????????????
     *
     * @param map
     * @param endStr
     * @param l
     */
    private void getDateByNullValue(Map<String, String> map, String endStr, long l) {
        map.put(AlertConfigConstants.END, endStr);
        Date startDate = new Date(l / (1000 * 3600) * (1000 * 3600));
        String startStr = new SimpleDateFormat(DateUtil.DATE_TIME_CH_FORMAT).format(startDate);
        map.put(AlertConfigConstants.START, startStr);
        insertAlertScheduleIndex(endStr);
    }

    /**
     * ?????????????????????
     *
     * @param value
     */
    private void insertAlertScheduleIndex(String value) {
        AlertScheduleIndexVo alertScheduleIndexVo = new AlertScheduleIndexVo();
        alertScheduleIndexVo.setId(UUID.randomUUID().toString());
        alertScheduleIndexVo.setIndexName(AlertConfigConstants.ALERT_AUTO_NOTIFY_TYPE_DESC);
        alertScheduleIndexVo.setIndexValue(value);
        alertScheduleIndexVo.setIndexType(AlertConfigConstants.ALERT_AUTO_NOTIFY_TYPE);
        alertScheduleIndexVo.setRemark("????????????????????????");
        alertScheduleIndexDao.insertAlertScheduleIndex(alertScheduleIndexVo);
    }


    /**
     * ??????????????????????????????
     */
    private void sendAlertNotifyConfigNew(String start, String end, Date date, List<String> sendOperation) {
        try {
            Map<String, String> params = Maps.newHashMap();
            params.put("send", "");
            params.put("resend", "");
            params.put("startTime", start);
            params.put("endTime", end);
            String hour = new SimpleDateFormat("HH").format(date);
            //????????????????????????????????????????????????????????????????????????????????????
            final List<AlertSubscribeRulesDetail> alertSubscribeRulesDetail = alertSubscribeRulesDao.getAlertSubscribeRulesNotifyConfig(params);
            Map<String, List<Map<String, String>>> dEmailData = Maps.newHashMap();
            Map<String, Map<String, Object>> alertMap = Maps.newHashMap();
            List<AlertNotifyConfigLog> alertNotifyConfigLogs = Lists.newArrayList();
            for (AlertSubscribeRulesDetail subscribeRulesDetail : alertSubscribeRulesDetail) {
                try {
                    if ("1".equals(subscribeRulesDetail.getPeriod()) && !(Integer.parseInt(subscribeRulesDetail.getStartPeriod()) <= Integer.parseInt(hour)
                            && Integer.parseInt(subscribeRulesDetail.getEndPeriod()) > Integer.parseInt(hour)))
                        continue;

                    Set<String> alertIds = Sets.newHashSet();
                    //???????????????????????????????????????id
                    String id = subscribeRulesDetail.getId();
                    //??????????????????????????????
                    String notifyAlertType = subscribeRulesDetail.getNotifyAlertType();
                    params.put("id", id);
                    if (notifyAlertType.contains("0")) {
                        List<Map<String, Object>> alertlist = alertSubscribeRulesDao.querySubscribeRulesManagement(params);
                        for (Map<String, Object> hisMap : alertlist) {
                            String alertId = MapUtils.getString(hisMap, "alert_id");
                            alertIds.add(alertId);
                            alertMap.put(alertId, hisMap);
                        }
                    }
                    if (notifyAlertType.contains("1")) {
                        List<Map<String, Object>> hislist = alertSubscribeRulesDao.querySubscribeRulesManagementHis(params);
                        for (Map<String, Object> hisMap : hislist) {
                            String alertId = MapUtils.getString(hisMap, "alert_id");
                            alertIds.add(alertId);
                            alertMap.put(alertId, hisMap);
                        }
                    }

                    // ???????????????????????????????????????????????????
                    if (CollectionUtils.isEmpty(alertIds)) {
                        continue;
                    }
                    //????????????id??????????????????????????????????????????????????????
                    List<Reciver> revicerInformationByAlertSubscribeRulesId = alertSubscribeRulesDao.getRevicerInformationByAlertSubscribe_rulesId(id);
                    for (Reciver reciver : revicerInformationByAlertSubscribeRulesId) {
                        //?????????????????????????????????????????????
                        String telephone = reciver.getTelephone();
                        //??????????????????????????????????????????????????????
                        String email = reciver.getEmail();
                        for (String alertId : alertIds) {
                            // ?????????????????????????????????
                            if (subscribeRulesDetail.getNotifyType().contains("0") || subscribeRulesDetail.getNotifyType().contains("2")) {
                                sendMobileMessage(alertMap.get(alertId), telephone, id, alertNotifyConfigLogs, sendOperation, date, subscribeRulesDetail);
                            }
                            // ????????????
                            if (subscribeRulesDetail.getNotifyType().contains("1") || subscribeRulesDetail.getNotifyType().contains("0")) {
                                if (subscribeRulesDetail.getEmailType() == 2) { //2-??????;1-????????????
                                    // ??????????????????
                                    sendEmailMessage(alertMap.get(alertId), email, id, alertNotifyConfigLogs, sendOperation, date, subscribeRulesDetail);
                                } else {
                                    getEmailMerge(alertId, email, id, dEmailData, sendOperation);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("?????????????????????????????????????????????: {},{}", subscribeRulesDetail.getSubscribeRules(), e);
                }
            }
            // ????????????(??????)
            if (!CollectionUtils.isEmpty(dEmailData)) {
                for (Map.Entry<String, List<Map<String, String>>> entry : dEmailData.entrySet()) {
                    String str = entry.getKey();
                    List<Map<String, String>> mapList = entry.getValue();
                    List<String> newList = Lists.newArrayList();
                    AlertSubscribeRulesDetail subscribeRules = null;
                    List<Map<String, Object>> alertList = Lists.newArrayList();
                    for (Map<String, String> map : mapList) {
                        String alertId = map.get("alertId");
                        String alertNotifyConfigId=map.get("alertNotifyConfigId");
                        if (StringUtils.isNotEmpty(alertNotifyConfigId) && subscribeRules == null) {
                            for (AlertSubscribeRulesDetail subscribeRulesDetail : alertSubscribeRulesDetail) {
                                if (alertNotifyConfigId.equals(subscribeRulesDetail.getId())) {
                                    subscribeRules = subscribeRulesDetail;
                                }
                            }
                        }
                        if (!newList.contains(alertId)) {
                            newList.add(alertId);
                            alertList.add(alertMap.get(alertId));
                        }
                    }
                    List<String> emailList = Lists.newArrayList();
                    emailList.add(str);
                    Map<String, String> resultMap = sendSubscribeEmailByTemplate(alertList, emailList,subscribeRules);
                    boolean emailSend = true;
                    if (!"0000".equals(resultMap.get("code"))) {
                        emailSend = false;
                    }
                    String message = resultMap.get("content");
                    if (StringUtils.isEmpty(message)) {
                        message = resultMap.get("desc");
                    }
                    for (Map<String, String> map : mapList) {
                        AlertNotifyConfigLog alertNotifyConfigLog = new AlertNotifyConfigLog();
                        alertNotifyConfigLog.setAlertNotifyConfigId(map.get("alertNotifyConfigId"));
                        alertNotifyConfigLog.setReceiver(str);
                        alertNotifyConfigLog.setSendStatus(emailSend ? "1" : "0");
                        alertNotifyConfigLog.setSendContent(message);
                        alertNotifyConfigLog.setSendType("3");
                        alertNotifyConfigLog.setSendTime(date);
                        alertNotifyConfigLog.setSendAlertId(map.get("alertId"));
                        alertNotifyConfigLogs.add(alertNotifyConfigLog);
                    }
                }
            }
            // ?????????????????????????????????
            log.info("alertNotifyConfigLogs >>>" + alertNotifyConfigLogs);
            if (alertNotifyConfigLogs != null && alertNotifyConfigLogs.size() > 0) {
                alertNotifyConfigDao.insertAlertNotifyConfigLog(alertNotifyConfigLogs);
                List<String> alertIdList = alertNotifyConfigLogs.stream().map(AlertNotifyConfigLog::getSendAlertId).collect(Collectors.toList());
                alertsBizV2.updateNotifyStatus(alertIdList, AlertCommonConstant.NUM.ONE);
                Set<String> idSet = Sets.newHashSet();
                Map<String, Object> map = Maps.newHashMap();
                for (AlertNotifyConfigLog alertNotifyConfigLog : alertNotifyConfigLogs) {
                    String id = alertNotifyConfigLog.getAlertNotifyConfigId();
                    if (StringUtils.isEmpty(id) || idSet.contains(id)) {
                        continue;
                    }
                    idSet.add(id);
                    map.put("alertNotifyConfigId", id);
                    alertSubscribeRulesDao.updateAlertSubscribeules(map);

                }
            }
            log.info("[ send alert notify config is success ]");

        } catch (Exception e) {
            log.error("???????????????????????????????????????: {},{}");
        }
    }

    private Map<String, String> sendSubscribeSmsByTemplate(Map<String, Object> alert, List<String> mobileList, AlertSubscribeRulesDetail subscribeRulesDetail) {

        if (alert != null && !alert.isEmpty()) {
            //????????????id???????????????????????????????????????
            String smsContent = subscribeRulesDetail.getSmsContent();
            String smsMessages1 = getSmsMessages1(smsContent, alert);
            boolean flag = smsSendHelper.send(mobileList, smsMessages1.replaceAll("@\\{[^\\}.]*\\}", ""));
            Map<String, String> map=Maps.newHashMap();
            if (flag) {
                map.put("code", "0000");
                map.put("desc", "success");
                map.put("content",smsMessages1);
            } else {
                map.put("code", "0003");
                map.put("desc", "send sms failed!");
                map.put("content",smsMessages1);
            }
            return map;
        } else {
            Map<String, String> map = Maps.newHashMap();
            map.put("code", "9998");
            map.put("desc", "alert is not exist!");
            return map;
        }
    }

    private Map<String, String> sendSubscribeEmailByTemplate(List<Map<String, Object>> alertList, List<String> emailList, AlertSubscribeRulesDetail subscribeRulesDetail) {
        Map<String, String> map=Maps.newHashMap();
        if (subscribeRulesDetail == null) {
            return map;
        }
        String emialSubject = subscribeRulesDetail.getEmialSubject();
        String emialContent = subscribeRulesDetail.getEmialContent();
        //??????????????????????????????
        String emailMessages = getEmailMessages(emialContent, alertList);
        String subject = getSubject(emialSubject, alertList);

        map.put("content",emailMessages);
        try {
            mailHelper.sendMail(subject.replaceAll("@\\{[^\\}.]*\\}", ""), emailMessages.replaceAll("@\\{[^\\}.]*\\}", ""), false, emailList.toArray(new String[0]));
        } catch (Exception e) {
            log.error("send email failed ", e);
            map.put("code", "0003");
            map.put("desc", "send sms failed!");
            return map;
        }
        map.put("code", "0000");
        map.put("desc", "success");
        return map;
    }
    private String getSmsMessages1(String mergeTemplate, Map<String, Object> alert) {
        StringBuilder sb = new StringBuilder();
        if (org.springframework.util.StringUtils.isEmpty(mergeTemplate)) {
            return sb.toString();
        }
        String tempTmeplate = mergeTemplate;
        for (String key : alert.keySet()) {
            Object value = alert.get(key);;
            if (value == null) {
                continue;
            }
            if (value instanceof String) {
                tempTmeplate = tempTmeplate.replaceAll("@\\{" + key + "\\}", (String) value);
            } else if (value instanceof Integer || value instanceof Double || value instanceof Float
                    || value instanceof Long || value instanceof Boolean) {
                tempTmeplate = tempTmeplate.replaceAll("@\\{" + key + "\\}", value + "");
            } else if (value instanceof Date) {
                tempTmeplate = tempTmeplate.replaceAll("@\\{" + key + "\\}", DateUtil.format((Date) value, "yyyy-MM-dd HH:mm:ss"));
            }

        }
        sb.append(tempTmeplate);


        return sb.toString();
    }
    private String getEmailMessages(String mergeTemplate, List<Map<String, Object>> mergeList) {
        StringBuilder sb = new StringBuilder();
        if (org.springframework.util.StringUtils.isEmpty(mergeTemplate)) {
            return sb.toString();
        }
        if (!CollectionUtils.isEmpty(mergeList)) {
            for (Map<String, Object> merge : mergeList) {
                String tempTmeplate = mergeTemplate;
                for (Map.Entry<String, Object> entry : merge.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (value == null) {
                        continue;
                    }
                    if (value instanceof String) {
                        tempTmeplate = tempTmeplate.replaceAll("@\\{" + key + "\\}", (String) value);
                    } else if (value instanceof Integer || value instanceof Double || value instanceof Float
                            || value instanceof Long || value instanceof Boolean) {
                        tempTmeplate = tempTmeplate.replaceAll("@\\{" + key + "\\}", value + "");
                    } else if (value instanceof Date) {
                        tempTmeplate = tempTmeplate.replaceAll("@\\{" + key + "\\}", DateUtil.format((Date) value, "yyyy-MM-dd HH:mm:ss"));
                    }

                }
                sb.append(tempTmeplate);
            }
        }
        return sb.toString();
    }

    private String getSubject(String mergeTemplate, List<Map<String, Object>> mergeList) {
        StringBuilder sb = new StringBuilder();
        if (org.springframework.util.StringUtils.isEmpty(mergeTemplate)) {
            return sb.toString();
        }
        if (!CollectionUtils.isEmpty(mergeList)) {
//            for (int i = 0; i < mergeList.size(); i++) {
            Map<String, Object> stringObjectMap = mergeList.get(0);
            String tempTmeplate = mergeTemplate;
            for (Map.Entry<String, Object> entry : stringObjectMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                if (value instanceof String) {
                    tempTmeplate = tempTmeplate.replaceAll("@\\{" + key + "\\}", (String) value);
                } else if (value instanceof Integer || value instanceof Double || value instanceof Float
                        || value instanceof Long || value instanceof Boolean) {
                    tempTmeplate = tempTmeplate.replaceAll("@\\{" + key + "\\}", value + "");
                } else if (value instanceof Date) {
                    tempTmeplate = tempTmeplate.replaceAll("@\\{" + key + "\\}", DateUtil.format((Date) value, "yyyy-MM-dd HH:mm:ss"));
                }

            }
            sb.append(tempTmeplate);
//            }
        }
        return sb.toString();
    }
    /**
     * ??????????????????????????????
     */
    public void resendAlertNotifyConfig() {
        Date date = new Date(System.currentTimeMillis() / 1000 * 1000);
        // ????????????
        Date dateLock = localLockMap.get(AlertConfigConstants.REDIS_ALERT_AUTO_SUBSCRIBE_LOCK_RESEND);
        if (dateLock != null && dateLock.getTime() >= (date.getTime() - 1800 * 1000)) {
            log.info("Last alert order task not finished!");
            return;
        }
        localLockMap.put(AlertConfigConstants.REDIS_ALERT_AUTO_SUBSCRIBE_LOCK_RESEND, date);
        try {
            // ??????????????????????????????????????????
            Map<String, String> params = Maps.newHashMap();
            params.put("resend", "resend");
            String hour = new SimpleDateFormat("HH").format(date);
            Set<String> alertIds = Sets.newHashSet();
            //1.????????????????????????????????????????????????????????????????????????????????????
            final List<AlertSubscribeRulesDetail> alertSubscribeRulesDetail = alertSubscribeRulesDao.getAlertSubscribeRulesNotifyConfig(params);
            Map<String, List<Map<String, String>>> dEmailData = Maps.newHashMap();
            List<AlertNotifyConfigLog> alertNotifyConfigLogs = Lists.newArrayList();
            List<String> sendOperation = Lists.newArrayList();
            Map<String, Map<String, Object>> alertMaps = Maps.newHashMap();
            for (AlertSubscribeRulesDetail subscribeRulesDetail : alertSubscribeRulesDetail) {
                try {
                    if ("1".equals(subscribeRulesDetail.getPeriod()) && !(Integer.parseInt(subscribeRulesDetail.getStartPeriod()) <= Integer.parseInt(hour)
                            && Integer.parseInt(subscribeRulesDetail.getEndPeriod()) > Integer.parseInt(hour)))
                        continue;
                    //2.???????????????????????????????????????id
                    String id = subscribeRulesDetail.getId();
                    //??????????????????????????????
                    String notifyAlertType = subscribeRulesDetail.getNotifyAlertType();
                    params.put("id", id);
                    // ?????????????????????????????????????????????
                    if (!notifyAlertType.contains("0")) {
                        continue;
                    }
                    //6.??????????????????????????????
                    Integer recurrenceCount = subscribeRulesDetail.getRecurrenceCount();
                    if (recurrenceCount == null) {
                        recurrenceCount = -1;
                    }
                    //7???????????????0 ????????????
                    if (recurrenceCount == 0) {
                        continue;
                    }

                    List<Map<String, Object>> mapList = alertSubscribeRulesDao.getReSendAlerts(id, recurrenceCount);

                    //10.???????????????????????????????????????
                    Integer recurrenceInterval = 5;
                    String recurrenceInterval1 = subscribeRulesDetail.getRecurrenceInterval();
                    if (!StringUtils.isEmpty(recurrenceInterval1) && StringUtils.isNumeric(recurrenceInterval1)) {
                        recurrenceInterval = Integer.parseInt(recurrenceInterval1);
                    }
                    if ("h".equalsIgnoreCase(subscribeRulesDetail.getRecurrenceIntervalUtil())) {
                        recurrenceInterval = recurrenceInterval * 60;
                    }
                    // ???????????????????????????????????????????????????
                    if (CollectionUtils.isEmpty(mapList)) {
                        continue;
                    }
                    if (null != mapList && mapList.size() > 0) {
                        for (Map<String, Object> alertMap : mapList) {
                            String alertId = MapUtils.getString(alertMap, "alert_id");
                            String receiver = MapUtils.getString(alertMap, "receiver_tmp");
                            String sendTime = MapUtils.getString(alertMap, "send_time_tmp");
                            // ???????????????????????????????????????????????????
                            if (StringUtils.isEmpty(alertId)) {
                                continue;
                            }
                            alertMaps.put(alertId, alertMap);
                            //11.????????????id??????????????????????????????????????????????????????
                            List<Reciver> revicerInformationByAlertSubscribeRulesId = alertSubscribeRulesDao.getRevicerInformationByAlertSubscribe_rulesId(id);
                            for (Reciver reciver : revicerInformationByAlertSubscribeRulesId) {
                                //12?????????????????????????????????
//                                String notifyObjInfo = reciver.getNotifyObjInfo();
                                //13?????????????????????????????????????????????
                                String telephone = reciver.getTelephone();
                                //14??????????????????????????????????????????????????????
                                String email = reciver.getEmail();
                                if (StringUtils.isEmpty(receiver) || StringUtils.isEmpty(sendTime)) {

                                    if (subscribeRulesDetail.getNotifyType().contains("0") || subscribeRulesDetail.getNotifyType().contains("2")) {
                                        sendMobileMessage(alertMaps.get(alertId), telephone, id, alertNotifyConfigLogs, sendOperation, date, subscribeRulesDetail);
                                    }
                                    // ????????????
                                    if (subscribeRulesDetail.getNotifyType().contains("1") || subscribeRulesDetail.getNotifyType().contains("0")) {
                                        if (subscribeRulesDetail.getEmailType() == 2) { //2-??????;1-????????????
                                            // ??????????????????
                                            sendEmailMessage(alertMaps.get(alertId), email, id, alertNotifyConfigLogs, sendOperation, date, subscribeRulesDetail);
                                        } else {
                                            getEmailMerge(alertId, email, id, dEmailData, sendOperation);
                                        }
                                    }

                                    continue;
                                }
                                Date sendTimes = DateUtil.getDate(sendTime, DateUtil.DATE_TIME_CH_FORMAT);
                                // ???????????????????????????
                                if (sendTimes.getTime() > (date.getTime() - recurrenceInterval * 60 * 1000)) {
                                    continue;
                                }
                                if (subscribeRulesDetail.getNotifyType().contains("0") || subscribeRulesDetail.getNotifyType().contains("2")) {
                                    sendMobileMessage(alertMaps.get(alertId), telephone, id, alertNotifyConfigLogs, sendOperation, date, subscribeRulesDetail);
                                }
                                // ????????????
                                if (subscribeRulesDetail.getNotifyType().contains("1") || subscribeRulesDetail.getNotifyType().contains("0")) {
                                    if (subscribeRulesDetail.getEmailType() == 2) { //2-??????;1-????????????
                                        // ??????????????????
                                        sendEmailMessage(alertMaps.get(alertId), email, id, alertNotifyConfigLogs, sendOperation, date, subscribeRulesDetail);
                                    } else {
                                        getEmailMerge(alertId, email, id, dEmailData, sendOperation);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("?????????????????????????????????????????????: {},{}", subscribeRulesDetail.getSubscribeRules(), e);
                }
            }
            // ????????????(??????)
            if (!CollectionUtils.isEmpty(dEmailData)) {
                for (Map.Entry<String, List<Map<String, String>>> entry : dEmailData.entrySet()) {
                    String str = entry.getKey();
                    List<Map<String, String>> mapList = entry.getValue();
                    List<String> newList = Lists.newArrayList();
                    AlertSubscribeRulesDetail subscribeRules = null;
                    List<Map<String, Object>> alertList = Lists.newArrayList();
                    for (Map<String, String> map : mapList) {
                        String alertId = map.get("alertId");
                        String alertNotifyConfigId=map.get("alertNotifyConfigId");
                        if (StringUtils.isNotEmpty(alertNotifyConfigId) && subscribeRules == null) {
                            for (AlertSubscribeRulesDetail subscribeRulesDetail : alertSubscribeRulesDetail) {
                                if (alertNotifyConfigId.equals(subscribeRulesDetail.getId())) {
                                    subscribeRules = subscribeRulesDetail;
                                }
                            }
                        }
                        if (!newList.contains(alertId)) {
                            newList.add(alertId);
                            alertList.add(alertMaps.get(alertId));
                        }
                    }
                    List<String> emailList = Lists.newArrayList();
                    emailList.add(str);
                    Map<String, String> resultMap = sendSubscribeEmailByTemplate(alertList, emailList,subscribeRules);
                    boolean emailSend = true;
                    if (!"0000".equals(resultMap.get("code"))) {
                        emailSend = false;
                    }
                    String message = resultMap.get("content");
                    if (StringUtils.isEmpty(message)) {
                        message = resultMap.get("desc");
                    }
                    for (Map<String, String> map : mapList) {
                        AlertNotifyConfigLog alertNotifyConfigLog = new AlertNotifyConfigLog();
                        alertNotifyConfigLog.setAlertNotifyConfigId(map.get("alertNotifyConfigId"));
                        alertNotifyConfigLog.setReceiver(str);
                        alertNotifyConfigLog.setSendStatus(emailSend ? "1" : "0");
                        alertNotifyConfigLog.setSendContent(message);
                        alertNotifyConfigLog.setSendType("3");
                        alertNotifyConfigLog.setSendTime(date);
                        alertNotifyConfigLog.setSendAlertId(map.get("alertId"));
                        alertNotifyConfigLogs.add(alertNotifyConfigLog);
                    }
                }
            }

            // 17.?????????????????????????????????
            log.info("alertNotifyConfigLogs >>>" + alertNotifyConfigLogs);
            if (alertNotifyConfigLogs != null && alertNotifyConfigLogs.size() > 0) {
                alertNotifyConfigDao.insertAlertNotifyConfigLog(alertNotifyConfigLogs);
                List<String> alertIdList = alertNotifyConfigLogs.stream().map(AlertNotifyConfigLog::getSendAlertId
                ).collect(Collectors.toList());
                alertsBizV2.updateNotifyStatus(alertIdList, AlertCommonConstant.NUM.ONE);
                Set<String> idSet = Sets.newHashSet();
                Map<String, Object> map = Maps.newHashMap();
                for (AlertNotifyConfigLog alertNotifyConfigLog : alertNotifyConfigLogs) {
                    String id = alertNotifyConfigLog.getAlertNotifyConfigId();
                    if (StringUtils.isEmpty(id) || idSet.contains(id)) {
                        continue;
                    }
                    idSet.add(id);
                    map.put("alertNotifyConfigId", id);
                    //18.??????????????????????????????????????????
                    alertSubscribeRulesDao.updateAlertSubscribeules(map);
                }
            }
            log.info("[ send alert notify config is success ]");
        } catch (Exception e) {
            log.error("???????????????????????????????????????: {},{}");
        }
        localLockMap.remove(AlertConfigConstants.REDIS_ALERT_AUTO_SUBSCRIBE_LOCK_RESEND);
    }
}
