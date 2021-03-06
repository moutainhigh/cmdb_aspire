package com.aspire.mirror.alert.server.biz.alert.impl;

import com.alibaba.fastjson.JSON;
import com.aspire.mirror.alert.server.annotation.DataToKafka;
import com.aspire.mirror.alert.server.biz.bpm.IBpmTaskService;
import com.aspire.mirror.alert.server.biz.helper.AlertScheduleIndexHelper;
import com.aspire.mirror.alert.server.dao.alert.*;
import com.aspire.mirror.alert.server.dao.alert.po.AlertsDetail;
import com.aspire.mirror.alert.server.dao.alert.po.AlertsRecord;
import com.aspire.mirror.alert.server.dao.alert.po.AlertsTransfer;
import com.aspire.mirror.alert.server.dao.bpm.AlertBpmTuningRecordDao;
import com.aspire.mirror.alert.server.dao.bpm.po.AlertTuningRecord;
import com.aspire.mirror.alert.server.dao.notify.AlertsNotifyDao;
import com.aspire.mirror.alert.server.dao.notify.po.AlertsNotify;
import com.aspire.mirror.alert.server.vo.bpm.AlertBpmCallBack;
import com.aspire.mirror.alert.server.vo.bpm.AlertBpmStartCallBack;
import com.aspire.mirror.alert.server.config.properties.AlertPingStatusProperties;
import com.aspire.mirror.alert.server.constant.AlertCommonConstant;
import com.aspire.mirror.alert.server.constant.Constants;
import com.aspire.mirror.alert.server.vo.alert.AlertsOperationRequestVo;
import com.aspire.mirror.alert.server.vo.alert.AutoConfirmClearVo;
import com.aspire.mirror.alert.server.vo.alert.AlertMonitorObjectVo;
import com.aspire.mirror.alert.server.util.TransformUtils;
import com.aspire.mirror.alert.server.biz.model.AlertFieldBiz;
import com.aspire.mirror.alert.server.biz.alert.AlertsBizV2;
import com.aspire.mirror.alert.server.biz.alert.AlertsHisBizV2;
import com.aspire.mirror.alert.server.biz.derive.IAlertDeriveAlertsBizV2;
import com.aspire.mirror.alert.server.constant.AlertConfigConstants;
import com.aspire.mirror.alert.server.dao.isolate.AlertIsolateAlertsV2Mapper;
import com.aspire.mirror.alert.server.vo.model.AlertFieldVo;
import com.aspire.mirror.alert.server.dao.alert.po.AlertsV2;
import com.aspire.mirror.alert.server.vo.alert.AlertsV2Vo;
import com.aspire.mirror.alert.server.util.AlertModelCommonUtil;
import com.aspire.mirror.alert.server.util.Criteria;
import com.aspire.mirror.common.constant.Constant;
import com.aspire.mirror.common.entity.PageResponse;
import com.aspire.mirror.common.util.DateUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @BelongsProject: mirror-alert
 * @BelongsPackage: com.aspire.mirror.alert.server.v2.biz.impl
 * @Author: baiwenping
 * @CreateTime: 2020-02-25 15:22
 * @Description: ${Description}
 */
@Slf4j
@Service
public class AlertsBizV2Impl implements AlertsBizV2 {
    @Autowired
    private AlertsV2Dao alertsV2Dao;
    @Autowired
    private AlertsHisBizV2 alertsHisBizV2;
    @Autowired
    private AlertsDetailDao alertsDetailDao;

    @Autowired
    private IBpmTaskService iBpmTaskService;

    @Autowired
    private AutoConfirmClearDao autoConfirmClearDao;

    @Autowired
    private AlertsRecordDao alertsRecordDao;

    @Autowired
    private AlertsTransferDao alertsTransferDao;

    @Autowired
    private AlertBpmTuningRecordDao alertBpmTuningRecordDao;

    @Autowired
    private AlertFieldBiz alertFieldBiz;

    @Autowired
    private AlertsNotifyDao alertsNotifyDao;

    @Autowired
    private AlertScheduleIndexHelper alertScheduleIndexHelper;

    @Autowired
    private AlertIsolateAlertsV2Mapper alertIsolateAlertsV2Mapper;

    @Autowired
    private IAlertDeriveAlertsBizV2 alertDeriveAlertsBizV2;

    @Autowired
    private AlertPingStatusProperties alertPingStatusConfig;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * ??????????????????
     * @param alertQuery
     * @return
     */
    public List<AlertsV2Vo> select(AlertsV2 alertQuery) {
        List<AlertsV2> list = alertsV2Dao.select(alertQuery);
        return TransformUtils.transform(AlertsV2Vo.class, list);
    }

    /**
     * ??????????????????
     *
     * @param map ??????????????????
     * @return ??????????????????
     */
    public int updateByPrimaryKey (Map<String, Object> map) {
        return alertsV2Dao.updateByPrimaryKey(map);
    }

    /**
     * ??????????????????
     *
     * @param map ??????????????????
     * @return ??????????????????
     */
    @DataToKafka(index = "alert_alerts")
    public String insert (Map<String, Object> map) {
        String alertId = MapUtils.getString(map, AlertConfigConstants.ALERT_ID);
        if (StringUtils.isEmpty(alertId)) {
            alertId = UUID.randomUUID().toString();
            map.put(AlertConfigConstants.ALERT_ID, alertId);
        }
        alertsV2Dao.insert(map);
        return alertId;
    }

    /**
     * @param alertIdArrays ??????ID??????
     * @return
     */
    @Override
    public int deleteByPrimaryKeyArrays(String[] alertIdArrays) {
        if (ArrayUtils.isEmpty(alertIdArrays)) {
            log.error("method[deleteByPrimaryKeyArrays] param[alertIdArrays] is null");
            throw new RuntimeException("param[alertIdArrays] is null");
        }
        return alertsV2Dao.deleteByPrimaryKeyArrays(alertIdArrays);
    }

    /**
     * ????????????id??????????????????
     *
     * @param alertId
     */
    public void deleteAlertsDetail(String alertId) {
        alertsDetailDao.deleteByAlertId(alertId);
    }

    /**
     * ????????????????????????
     *
     * @param alertDetail
     */
    public void insertAlertsDetail(AlertsDetail alertDetail) {
        alertsDetailDao.insert(alertDetail);
    }

    /**
     * ??????????????????alert
     *
     * @param alertId ??????ID
     * @return AlertsDTO ????????????
     */
    public AlertsV2Vo selectAlertByPrimaryKey(String alertId) {
        if (StringUtils.isEmpty(alertId)) {
            log.warn("method[selectByPrimaryKey] param[alertId] is null");
            return null;
        }
        AlertsV2 alerts = alertsV2Dao.selectByPrimaryKey(alertId);

        if (alerts == null) {
            return null;
        }
        AlertsV2Vo alertsDTO = TransformUtils.transform(AlertsV2Vo.class, alerts);
        return alertsDTO;
    }

    /**
     * ??????id????????????????????????
     * @param alertId
     * @param curMoniTime
     */
    public void updateCurMoniTime(String alertId, Date curMoniTime) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("alertId", alertId);
        map.put("curMoniTime", curMoniTime);
        alertsV2Dao.updateCurMoniTime(map);
    }

    /**
     * ??????Id??????????????????????????????
     * @param alertIds
     * @return
     */
    public List<Map<String, Object>> selectByIds (List<String> alertIds) {
        List<Map<String, Object>> mapList = alertsV2Dao.selectByIds(alertIds);
        for (Map<String, Object> map:  mapList) {
            alertScheduleIndexHelper.pushDictAlert(map);
        }
        return mapList;
    }

    /**
     * ??????????????????????????????
     *
     * @param example
     * @return
     */
    public List<Map<String, Object>> list(Criteria example) {
        return alertsV2Dao.listByEntity(example);
    }

    /**
     * ????????????
     *
     * @param example
     * @return
     */
    public PageResponse<Map<String, Object>> findPage(Criteria example) {
        List<Map<String, Object>> pageWithResult = alertsV2Dao.findPageWithResult(example);
        Integer pageWithCount = alertsV2Dao.findPageWithCount(example);
        PageResponse<Map<String, Object>> page = new PageResponse<>();
        page.setCount(pageWithCount);
        page.setResult(pageWithResult);
        return page;
    }

    /**
     * ????????????
     * @auther baiwenping
     * @Description
     * @Date 14:58 2020/3/12
     * @Param [alertId]
     * @return java.util.Map<java.lang.String,java.lang.Object>
     **/
    public Map<String, Object> detailById(String alertId) {
        Map<String, Object> alert = alertsV2Dao.detailById(alertId);
//        alertScheduleIndexHelper.pushDictAlert(alert);
        return alert;
    }

    @Override
    public List<AlertMonitorObjectVo> getMonitObjectList() {
        return alertsV2Dao.getMonitObjectList();
    }

    /**
     * ????????????
     *
     * @param alertIds ???????????????ID??????
     */
    @Override
    @Transactional
    public void alertTransfer(String namespace, String alertIds, String userIds) {
        String[] alertIdArrays = alertIds.split(",");
        String[] userIdArrays = userIds.split(",");

        if (alertIdArrays.length > 0 && userIdArrays.length > 0) {
            for (int i = 0; i < alertIdArrays.length; i++) {
                String alertId = alertIdArrays[i];

                for (int j = 0; j < userIdArrays.length; j++) {
                    String userId = userIdArrays[j];
                    AlertsTransfer alertsTransfer = new AlertsTransfer();
                    alertsTransfer.setAlertId(alertId);
                    alertsTransfer.setUserName(namespace);
                    alertsTransfer.setConfirmUserName(userId);
                    alertsTransferDao.insert(alertsTransfer);
                }

                AlertsRecord alertsRecord = new AlertsRecord();
                alertsRecord.setAlertId(alertId);
                alertsRecord.setUserName(namespace);
                alertsRecord.setOperationType("0");
                String content = "?????????" + userIds;
                alertsRecord.setContent(content);

                AlertsV2 alerts = alertsV2Dao.selectByPrimaryKey(alertId);
                if (alerts.getOperateStatus() == 0) {
                    alertsRecord.setOperationStatus("1");
                } else {

                    alerts.setOperateStatus(0);
                    int index = alertsV2Dao.updateByPrimaryKey(AlertModelCommonUtil.generateAlerts(alerts, alertFieldBiz.getModelFromRedis(AlertConfigConstants.REDIS_MODEL_ALERT, null)));

                    if (index == 1) {
                        alertsRecord.setOperationStatus("1");
                    } else {
                        alertsRecord.setOperationStatus("0");
                    }

                }

                //????????????????????????

                alertsRecordDao.insert(alertsRecord);

            }

        }


    }

    /**
     * ????????????
     */
    @Override
    @Transactional
    public void alertConfirm(AlertsOperationRequestVo request) {
        String[] alertIdArrays = request.getAlertIds().split(",");
        if (alertIdArrays.length > 0) {
            for (String alertId : alertIdArrays) {
                AlertsV2 alerts = alertsV2Dao.selectByPrimaryKey(alertId);
                //????????????????????????
                alerts.setOperateStatus(1);
                Map<String, Object> p = Maps.newHashMap();
                p.put("alert_id",alerts.getAlertId());
                p.put("operate_status",alerts.getOperateStatus());
                int index = alertsV2Dao.updateByPrimaryKey(p);
//                int index = alertsV2Dao.updateByPrimaryKey(AlertV2CommonUtils.generateAlerts(alerts, alertFieldBiz.getModelFromRedis(AlertConfigConstants.REDIS_MODEL_ALERT, null)));
                AlertsRecord alertsRecord = new AlertsRecord();
                alertsRecord.setAlertId(alertId);
                alertsRecord.setUserName(request.getUserName());
                alertsRecord.setOperationType("1");
                alertsRecord.setContent(request.getContent());
                if (index == 1) {
                    alertsRecord.setOperationStatus("1");
                } else {
                    alertsRecord.setOperationStatus("0");
                }
                alertsRecordDao.insert(alertsRecord);
                if (request.getAutoType() != -1) {
                    AutoConfirmClearVo autoConfirmClearId = autoConfirmClearDao.getAutoConfirmClearId(
                            alerts.getDeviceIp(),
                            alerts.getIdcType(),
                            alerts.getBizSys(),
                            alerts.getAlertLevel(),
                            alerts.getSource(),
                            alerts.getItemId(),
                            request.getAutoType(),
                            null);
                    if (null == autoConfirmClearId) {
                        AutoConfirmClearVo autoConfirmClearVo = new AutoConfirmClearVo();
                        autoConfirmClearVo.setUuid(UUID.randomUUID().toString());
                        autoConfirmClearVo.setDeviceIp(alerts.getDeviceIp());
                        autoConfirmClearVo.setIdcType(alerts.getIdcType());
                        autoConfirmClearVo.setBizSys(alerts.getBizSys());
                        autoConfirmClearVo.setAlertLevel(alerts.getAlertLevel());
                        autoConfirmClearVo.setSource(alerts.getSource());
                        autoConfirmClearVo.setItemId(alerts.getItemId());
                        autoConfirmClearVo.setAutoType(request.getAutoType());
                        autoConfirmClearVo.setContent(request.getContent());
                        autoConfirmClearVo.setStartTime(request.getStartTime());
                        autoConfirmClearVo.setEndTime(request.getEndTime());
                        autoConfirmClearVo.setOperator(request.getUserName());
                        autoConfirmClearDao.insert(autoConfirmClearVo);
                    }
                }
            }
        }

    }

    @Override
    public void alertObserve(Map<String, Object> request) {
        String[] alertIdArrays = String.valueOf(request.get("alertIds")).split(",");
        if (alertIdArrays.length > 0) {
            for (String alertId : alertIdArrays) {
                AlertsV2 alerts = alertsV2Dao.selectByPrimaryKey(alertId);
                //????????????????????????
                alerts.setOperateStatus(4);
                Map<String, Object> p = Maps.newHashMap();
                p.put("alert_id",alerts.getAlertId());
                p.put("operate_status",alerts.getOperateStatus());
                int index = alertsV2Dao.updateByPrimaryKey(p);
//                int index = alertsV2Dao.updateByPrimaryKey(AlertV2CommonUtils.generateAlerts(alerts, alertFieldBiz.getModelFromRedis(AlertConfigConstants.REDIS_MODEL_ALERT, null)));
                AlertsRecord alertsRecord = new AlertsRecord();
                alertsRecord.setAlertId(alertId);
                alertsRecord.setUserName(String.valueOf(request.get("username")));
                alertsRecord.setOperationType("1");
                alertsRecord.setContent("????????????????????????");
                if (index == 1) {
                    alertsRecord.setOperationStatus("1");
                } else {
                    alertsRecord.setOperationStatus("0");
                }
                alertsRecordDao.insert(alertsRecord);
            }
        }
    }

    /**
     * ????????????
     *
     * @param namespace
     * @param alertIds
     * @param orderType
     * @return
     */
    @Override
    public AlertBpmStartCallBack switchOrder(String namespace, String alertIds, Integer orderType) {
        AlertBpmStartCallBack message = null;
        String orderTpye = orderType.toString();
        switch (orderTpye) {
            case Constants.ORDER_TUNING: // ??????????????????
                message = genTuningOder(namespace, alertIds, orderTpye);
                break;
            default: // ??????????????????
                String genMessage = genOrder(namespace, alertIds, orderType);
                message = new AlertBpmStartCallBack();
                if (genMessage.length()>8 && genMessage.substring(0,8).equals("success:")) {
                    String successNum = genMessage.substring(8,9);
                    if (StringUtils.isNotEmpty(successNum)) {
                        message.setSuccess(Integer.valueOf(successNum));
                    }
                    if (genMessage.contains("_")) {
                        message.setOrderIdList(genMessage.split("_")[1]);
                    }
                } else { // ??????
                    message.setStatus(false);
                    message.setMessage(genMessage);
                }
                break;
        }
        return message;
    }

    /**
     * ????????????-????????????
     * @param namespace
     * @param alertIds
     * @param orderType
     * @return
     */
    private AlertBpmStartCallBack genTuningOder(String namespace, String alertIds, String orderType) {
        AlertBpmStartCallBack callBack = new AlertBpmStartCallBack();
        int successNum = 0;
        String[] alertIdArrays = alertIds.split(",");
        List<String> orderIdList = Lists.newArrayList();
        for (String alertId : alertIdArrays) {
            Map param = Maps.newHashMap();
            param.put("alertIdArrays", new String[]{alertId});
            List<Map<String, Object>> list = alertsV2Dao.selectOrderParam1(param);
            if (CollectionUtils.isEmpty(list)) {
                continue;
            }
            AlertTuningRecord recordInDB = alertBpmTuningRecordDao.select(alertId);
            if (recordInDB != null) {
                log.info("??????????????????????????????????????????!");
                successNum++;
                continue;
            }
            Map<String, Object> alertMap = list.get(0);
            if ("1".equals(alertMap.get("object_type"))) { // ??????????????????
                AlertBpmCallBack tuningCallBack = iBpmTaskService.callBpmFlowStart(namespace, alertMap, orderType);
                if ("1".equals(tuningCallBack.getStatus())) { // ????????????????????????
                    successNum++;
                    String runId = tuningCallBack.getRunId(); // ????????????ID
                    orderIdList.add(runId);
                    // ???????????????????????????
                    AlertTuningRecord record = new AlertTuningRecord();
                    record.setAlertId(alertId);
                    record.setOrderId(runId); // ????????????ID
                    record.setOrderType(orderType); // ????????????
                    record.setOrderStatus(Constant.ORDER_DEALING); // ?????????
                    alertBpmTuningRecordDao.insert(record);
                    // ???????????????????????????
                    AlertsRecord alertsRecord = new AlertsRecord();
                    alertsRecord.setAlertId(alertId);
                    alertsRecord.setUserName(namespace);
                    alertsRecord.setOperationType("2"); // ????????????
                    alertsRecord.setContent(Constant.TUNING_ORDER);
                    alertsRecord.setOperationStatus("1");
                    alertsRecord.setOperationTime(new Date());
                    alertsRecordDao.insert(alertsRecord);
                } else {
                    log.error("??????????????????????????????, alertId: {}, {}! ", alertId, tuningCallBack.getMessage());
                    continue;
                }
            } else {
                log.info("??????????????????????????????????????????????????????????????????");
                continue;
            }
        }
        callBack.setTotal(alertIdArrays.length);
        callBack.setSuccess(successNum);
        if (successNum == 0) {
            callBack.setStatus(false);
            callBack.setMessage("??????????????????");
        }
        if (!CollectionUtils.isEmpty(orderIdList)) callBack.setOrderIdList(String.join(",", orderIdList));
        return callBack;
    }

    /**
     * ????????????
     *
     * @param alertIds ???????????????ID??????
     * @param orderType
     */
    @Override
    public String genOrder(String namespace, String alertIds, Integer orderType) {

        String[] alertIdArrays = alertIds.split(",");

        Map paramMap = Maps.newHashMap();
        // ????????????????????????
//        paramMap.put("orderStatus", Constant.ORDER_BEFOR);
        // ??????ID??????
        paramMap.put("alertIdArrays", alertIdArrays);
        List<Map<String, Object>> list = alertsV2Dao.selectOrderParam1(paramMap);
        List<Map<String, Object>> unSend = list.stream().filter(p->p.get("order_status").equals(Constant.ORDER_BEFOR)
                                            ||p.get("order_status").equals("4")).collect(Collectors.toList());
        //??????orderType??????????????????????????? ??????orderType=1???????????????????????????????????????
        // ??????orderType=2????????????????????????????????????????????????????????????????????????????????????
        // ??????orderType=3???????????????????????????????????????????????????????????????????????????????????????
        List<Map<String, Object>> newList = new ArrayList<>();
        newList.addAll(unSend);
        log.info("#=====> orderType: {}" , orderType);
        if (orderType.toString().equals(Constants.ORDER_HITCH)){
            List<Map<String, Object>> list1 = list.stream().filter(p->p.containsKey("order_type"))
                    .filter(p->p.get("order_type").equals(Constants.ORDER_ALERT)).collect(Collectors.toList());
            newList.addAll(list1);
        }else if (orderType.toString().equals(Constants.ORDER_MAINTENANCE)){
            List<Map<String, Object>> list2 = list.stream().filter(p->p.containsKey("order_type"))
                    .filter(p->!p.get("order_type").equals(Constants.ORDER_MAINTENANCE)).collect(Collectors.toList());
            newList.addAll(list2);
        }
        String message = iBpmTaskService.alertHandleBpmResult(newList, AlertCommonConstant.NUM.ONE, namespace,orderType);
        return message;
    }

    /**
     * ??????????????????
     * @auther baiwenping
     * @Description
     * @Date 18:52 2020/3/23
     * @Param [unserName, alertIds]
     * @return void
     **/
    public void notifyStatus(String status, List<String> alertIds) {
        if (StringUtils.isEmpty(status)) {
            return;
        }
        List<AlertsV2> alertList = alertsV2Dao.selectByPrimaryKeyArrays(alertIds.toArray(new String[0]));
        List<String> ids = Lists.newArrayList();
        for (AlertsV2 alert: alertList) {
            if (!status.equals(alert.getNotifyStatus())) {
                ids.add(alert.getAlertId());
            }
        }
        if (ids.size() > 0) {
            alertsV2Dao.updateNotifyStatus(ids, status);
            for (String alertId: ids) {
                AlertsNotify alertsNotify = new AlertsNotify();
                alertsNotify.setAlertId(alertId);
                alertsNotify.setUserName("???");
                alertsNotify.setReportType("3");     //????????????
//            alertsNotify.setDestination(dest);
                alertsNotify.setMessage("????????????");
                alertsNotify.setStatus(Constants.ISOLATE_STATUS_OPEN); //??????

                alertsNotifyDao.insert(alertsNotify);
            }
        }

    }

    /**
     * ????????????????????????id?????????. <br/>
     * <p>
     *
     */
    @Transactional
    public void manualClear(AlertsOperationRequestVo request) {
        String[] alertIdArrays = request.getAlertIds().split(",");
        if (alertIdArrays.length > 0) {
            List<AlertsV2> alertList = alertsV2Dao.selectByPrimaryKeyArrays(alertIdArrays);
            this.manualClear(alertList, request.getUserName(), request.getContent());
            for (AlertsV2 alertsDTO : alertList) {
                AlertsRecord alertsRecord = new AlertsRecord();
                alertsRecord.setAlertId(alertsDTO.getAlertId());
                alertsRecord.setUserName(request.getUserName());
                alertsRecord.setOperationType("3");
                alertsRecord.setContent(request.getContent());
                alertsRecord.setOperationStatus("1");
                alertsRecordDao.insert(alertsRecord);
                if (request.getAutoType() != null && request.getAutoType() != -1) {
                    AutoConfirmClearVo autoConfirmClearId = autoConfirmClearDao.getAutoConfirmClearId(
                            alertsDTO.getDeviceIp(),
                            alertsDTO.getIdcType(),
                            alertsDTO.getBizSys(),
                            alertsDTO.getAlertLevel(),
                            alertsDTO.getSource(),
                            alertsDTO.getItemId(),
                            request.getAutoType(),
                            null);
                    if (null == autoConfirmClearId) {
                        AutoConfirmClearVo autoConfirmClearVo = new AutoConfirmClearVo();
                        autoConfirmClearVo.setUuid(UUID.randomUUID().toString());
                        autoConfirmClearVo.setDeviceIp(alertsDTO.getDeviceIp());
                        autoConfirmClearVo.setIdcType(alertsDTO.getIdcType());
                        autoConfirmClearVo.setBizSys(alertsDTO.getBizSys());
                        autoConfirmClearVo.setAlertLevel(alertsDTO.getAlertLevel());
                        autoConfirmClearVo.setSource(alertsDTO.getSource());
                        autoConfirmClearVo.setItemId(alertsDTO.getItemId());
                        autoConfirmClearVo.setAutoType(request.getAutoType());
                        autoConfirmClearVo.setContent(request.getContent());
                        autoConfirmClearVo.setStartTime(request.getStartTime());
                        autoConfirmClearVo.setEndTime(request.getEndTime());
                        autoConfirmClearVo.setOperator(request.getUserName());
                        autoConfirmClearDao.insert(autoConfirmClearVo);
                    }
                }
            }
        }

    }

    /**
     * ??????????????????.  <br/>
     * <p>
     *
     * @param alertList
     */
    private void manualClear(final List<AlertsV2> alertList, String namespace, String content) {

        Date nowTime = new Date();
        String newStr = DateUtil.format(nowTime, "yyyy-MM-dd HH:mm:ss");
        String[] ids = new String[alertList.size()];
        List<AlertFieldVo> modelFromRedis = alertFieldBiz.getModelFromRedis(AlertConfigConstants.REDIS_MODEL_ALERT_HIS, null);
        for (int i = 0;i < alertList.size(); i++) {
            AlertsV2 alert = alertList.get( i );
            Map<String, Object> map = alertsV2Dao.detailById(alert.getAlertId());
            for (Map.Entry<String, Object> entry: map.entrySet()) {
                Object value = entry.getValue();
                if (value == null) {
                    continue;
                }
                if (value instanceof Timestamp) {
                    map.put(entry.getKey(), DateUtil.format(new Date(((Timestamp) value).getTime()), "yyyy-MM-dd HH:mm:ss"));
                }
            }
            if (StringUtils.isNotEmpty(alert.getOrderId())) {
                map.put("order_status", Constants.ORDER_END);
                if (!AlertCommonConstant.CLEAR_ALERT_CONTENT_BYORDER.equalsIgnoreCase(content)) {
                    String message = iBpmTaskService.closeBpmInstance(alert.getOrderId(), content);
                    if ("ERROR".equals(message)) {
                        map.put("order_status", Constants.ORDER_ERROR);
                    }
                }
            }

            map.put("alert_end_time", newStr);
            map.put("clear_user", namespace);
            map.put("clear_content", content);
            alertsHisBizV2.insert(AlertModelCommonUtil.generateAlerts(map, modelFromRedis));
            ids[i] = alert.getAlertId();
            if (AlertConfigConstants.SOURCE_DERIVE.equals(alert.getSource())) {
                // ??????????????????
                alertDeriveAlertsBizV2.deleteByAlertId(alert.getAlertId(), alertFieldBiz.getModelField(AlertConfigConstants.REDIS_MODEL_DERIVE_ALERT_HIS));
            }
        }
        deleteByPrimaryKeyArrays(ids);
    }

    /**
     * ????????????????????????
     * @param type
     * @param namespace
     * @param alertIds
     * @param destination
     * @param message
     * @param status
     */
    @Override
    public void recordNotifyLog(String type, String namespace, String alertIds, List<String> destination, String message, String status) {
        //????????????
        String reportType = "1";
        String contentRecord = "????????????";
        if (AlertConfigConstants.MESSAGE_TYPE_SMS.equals(type)) {
            reportType = "0"; //????????????
            contentRecord = "????????????";
        }
        String[] alertIdArrays = alertIds.split(",");

        if (alertIdArrays.length > 0 && destination.size() > 0) {
            for (int i = 0; i < alertIdArrays.length; i++) {
                String alertId = alertIdArrays[i];
                for (String dest: destination) {
                    AlertsNotify alertsNotify = new AlertsNotify();
                    alertsNotify.setAlertId(alertId);
                    alertsNotify.setUserName(namespace);
                    alertsNotify.setReportType(reportType);    //??????
                    alertsNotify.setDestination(dest);
                    alertsNotify.setMessage(message);
                    alertsNotify.setStatus(status);
                    alertsNotifyDao.insert(alertsNotify);
                }

                AlertsRecord alertsRecord = new AlertsRecord();
                alertsRecord.setAlertId(alertId);
                alertsRecord.setUserName(namespace);
                alertsRecord.setOperationType("4");
                alertsRecord.setContent(contentRecord);
                alertsRecord.setOperationStatus(status);
                alertsRecordDao.insert(alertsRecord);

            }
            alertsV2Dao.updateNotifyStatus(Arrays.asList(alertIdArrays), AlertConfigConstants.YES);
        }
    }

    public void updateNotifyStatus (List<String> alertIds, String status) {
        alertsV2Dao.updateNotifyStatus(alertIds, status);
    }

    /**
     *
     * @param deviceIds
     * @return
     */
    public List<Map<String, Object>> getDeviceNewestAlertLevelList(List<String> deviceIds) {
        if (deviceIds == null) {
            deviceIds = Lists.newArrayList();
        }
        return alertsV2Dao.getDeviceNewestAlertLevelList(deviceIds);
    }

    /**
     *
     * @param itemIds
     * @return
     */
    public List<Map<String, Object>> getItemNewestAlertLevelList(String prefix, List<String> itemIds) {
        if (itemIds == null) {
            itemIds = Lists.newArrayList();
        }
        return alertsV2Dao.getItemNewestAlertLevelList(prefix, itemIds);
    }

    /**
     * ????????????
     *
     * @param example
     * @return
     */
    public PageResponse<Map<String, Object>> queryDeviceAlertList(Criteria example) {
        List<Map<String, Object>> pageWithResult = alertsV2Dao.queryDeviceAlertList(example);
        Integer pageWithCount = alertsV2Dao.queryDeviceAlertCount(example);
        PageResponse<Map<String, Object>> page = new PageResponse<>();
        page.setCount(pageWithCount);
        page.setResult(pageWithResult);
        return page;
    }

    /**
     * ???????????????????????????????????????
     * @param example
     * @return
     */
    public List<Map<String, Object>> summaryDeviceAlertsByLevel(Criteria example) {
        return alertsV2Dao.summaryDeviceAlertsByLevel(example);
    }

    /**
     *
     * @param list
     * @return
     */
    public List<String> checkOrderStatus(List<String> list) {
        List<Map<String, Object>> alertOrderList = alertsV2Dao.checkOrderStatus(list);
        List<String> result = alertOrderList.stream().map(item -> {
            return MapUtils.getString(item, "order_id");
        }).collect(Collectors.toList());
        List<Map<String, Object>> isolateOrderList = alertIsolateAlertsV2Mapper.checkOrderStatus(list);
        result.addAll(isolateOrderList.stream().map(item -> {
            return MapUtils.getString(item, "order_id");
        }).collect(Collectors.toList()));
        return result;
    }

    /**
     * ??????ping?????????kafka?????????cmdb??????
     * @param alert
     */
    @Async
    public void putPingStatusToKafka(AlertsV2Vo alert, String deviceType) {
        if (!alertPingStatusConfig.isFlag() || alert == null) {
            return;
        }
        String keyComment = alert.getKeyComment();
        if (StringUtils.isEmpty(keyComment)) {
            return;
        }
        if (keyComment.equals(alertPingStatusConfig.getManageIpTitle())) {
            sendPingStatus("status[manageIp]", deviceType, alert);
        } else if (keyComment.equals(alertPingStatusConfig.getIpmiIpTitle())) {
            sendPingStatus("status[ipmiIp]", deviceType, alert);
        } else if (keyComment.equals(alertPingStatusConfig.getServiceIpTitle())) {
            sendPingStatus("status[serviceIp]", deviceType, alert);
        }
    }

    /**
     * ??????ping??????
     * @param key
     * @param alert
     */
    private void sendPingStatus (String key, String deviceType, AlertsV2Vo alert) {
        String alertType = alert.getAlertType();
        // ?????????????????????????????????????????????
        if (!AlertsV2Vo.ALERT_ACTIVE.equals(alertType) && !AlertsV2Vo.ALERT_REVOKE.equals(alertType)) {
            return;
        }

        Map map = Maps.newHashMap();
        if (StringUtils.isNotEmpty(deviceType)) {
            map.put("device_type", deviceType);
        }
        map.put("ip", alert.getDeviceIp());
        map.put("pool", alert.getIdcType());
        map.put("source", "zbx");
        map.put("key_", key);
        // ??????????????????
        if (AlertsV2Vo.ALERT_ACTIVE.equals(alertType)) {
            map.put("lastvalue",alertPingStatusConfig.getCmdbYes());
        } else if (AlertsV2Vo.ALERT_REVOKE.equals(alertType)) {
            map.put("lastvalue",alertPingStatusConfig.getCmdbNo());
        }
        String pingStatusString = JSON.toJSONString(map);
        kafkaTemplate.send(alertPingStatusConfig.getTopic(), pingStatusString);
        log.info("send ping status message to kafka, topic is: {}, message is : {}", alertPingStatusConfig.getTopic(), pingStatusString);
        map.put("key_", "latest_ping_time");
        Date curMoniTime = alert.getCurMoniTime();
        if (curMoniTime == null) {
            curMoniTime = new Date();
        }
        map.put("lastvalue",DateUtil.format(curMoniTime, DateUtil.DATE_TIME_CH_FORMAT));
        String lastTimeString = JSON.toJSONString(map);
        kafkaTemplate.send(alertPingStatusConfig.getTopic(), lastTimeString);
        log.info("send ping status message to kafka, topic is: {}, message is : {}", alertPingStatusConfig.getTopic(), lastTimeString);
    }

}
