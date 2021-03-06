package com.aspire.mirror.alert.server.biz.model.impl;

import com.aspire.mirror.alert.server.dao.operateLog.AlertOperateLogMapper;
import com.aspire.mirror.alert.server.dao.operateLog.po.AlertOperateLog;
import com.aspire.mirror.alert.server.util.StringUtils;
import com.aspire.mirror.alert.server.biz.model.AlertFieldBiz;
import com.aspire.mirror.alert.server.dao.model.AlertFieldDao;
import com.aspire.mirror.alert.server.dao.model.AlertModelDao;
import com.aspire.mirror.alert.server.dao.model.po.AlertField;
import com.aspire.mirror.alert.server.vo.model.AlertFieldVo;
import com.aspire.mirror.alert.server.dao.model.po.AlertModel;
import com.aspire.mirror.common.entity.PageResponse;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Service
@Slf4j
public class AlertFieldBizImpl implements AlertFieldBiz {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private AlertFieldDao alertFieldDao;
    @Autowired
    private AlertModelDao alertModelDao;
    @Autowired
    private AlertOperateLogMapper alertOperateLogMapper;

    private final ConcurrentHashMap<String, List<AlertFieldVo>> alertModelCache = new ConcurrentHashMap<>();

    /**
    *
    * @auther baiwenping
    * @Description
    * @Date 15:47 2020/2/21
    * @Param [tableName]
    * @return java.util.List<com.aspire.mirror.alert.server.vo.model.AlertFieldRequestDTO>
    **/
    public List<AlertFieldVo> getModelFromRedis (String tableName, String sort) {
        //??????sort???????????????sql????????????
        if (!StringUtils.isEmpty(sort) && !sort.matches("[a-zA-Z0-9\\.\\_\\ ,]+")) {
            sort = null;
        }
        // ?????????????????????????????????????????????????????????redis
//        List<AlertFieldRequestDTO> list = Lists.newArrayList();
//        if (redisTemplate.hasKey(tableName)) {
//            List<Object> values = redisTemplate.opsForHash().values(tableName);
//            list.addAll(PayloadParseUtil.jacksonBaseParse(AlertFieldRequestDTO.class, values));
//        } else {
            // ??????redis???????????????????????????????????????????????????redis
            List<AlertFieldVo> alertFieldList = alertFieldDao.getAlertFieldListByTableName(tableName, sort);
//            for (AlertFieldRequestDTO alertFieldDetailDTO:alertFieldList) {
//                redisTemplate.opsForHash().put(tableName, alertFieldDetailDTO.getId(), alertFieldDetailDTO);
//                redisTemplate.expire(tableName, 24, TimeUnit.HOURS);
//            }
//            list.addAll(alertFieldList);
//        }

        return  alertFieldList;
    }

    @Override
    @Transactional(rollbackFor= Exception.class)
    public void insertAlertModel(AlertFieldVo requestDTO) {

        // ??????id
        requestDTO.setId( UUID.randomUUID().toString());
        requestDTO.setCreateTime(new Date());
        try {
            // ???????????????
            alertFieldDao.insertAlertModel(requestDTO);
            AlertModel alertModelDetail = alertModelDao.getAlertModelDetail(requestDTO.getModelId());
            Map<String, Object> map = Maps.newHashMap();
            map.put("tableName", alertModelDetail.getTableName());
            map.put("fieldCode", requestDTO.getFieldCode());
            map.put("jdbcType", requestDTO.getDataType());
            map.put("jdbcLength", StringUtils.isEmpty(requestDTO.getDataLength()) ? 0 : Integer.valueOf(requestDTO.getDataLength()));
            map.put("jdbcTip", requestDTO.getDataTip());
            alertFieldDao.addFieldColumn(map);
            // ??????redis
//            AlertModelRequestDTO alertModelDetail = alertModelDao.getAlertModelDetail(requestDTO.getModelId());
//            redisTemplate.opsForHash().put(alertModelDetail.getTableName(), requestDTO.getId(), requestDTO);
            // ??????????????????
            AlertOperateLog alertOperateLog = new AlertOperateLog();
            alertOperateLog.setOperateContent("??????????????????");
            alertOperateLog.setOperateModel("alert_field");
            alertOperateLog.setOperateModelDesc("??????????????????");
            alertOperateLog.setOperater(requestDTO.getCreator());
            alertOperateLog.setOperateTime(new Date());
            alertOperateLog.setOperateType("insert");
            alertOperateLog.setOperateTypeDesc("??????????????????");
            alertOperateLog.setRelationId(requestDTO.getId());
            alertOperateLogMapper.insert(alertOperateLog);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new RuntimeException(e);
        }

    }

    @Override
    public AlertField getAlertFieldDetailById(String id) {
        return alertFieldDao.getAlertFieldDetailById(id);
    }

    @Override
    public void deleteAlertFieldDetailById(String id, String modelId, String userName) {

        AlertModel alertModelDetail = alertModelDao.getAlertModelDetail(modelId);
        AlertField alertFieldById = alertFieldDao.getAlertFieldDetailById(id);
        // ???????????????
        alertFieldDao.deleteAlertFieldDetailById(id,modelId);
        alertFieldDao.deleteFieldColumn(alertModelDetail.getTableName(), alertFieldById.getFieldCode());
        // ??????????????????
        AlertOperateLog alertOperateLog = new AlertOperateLog();
        alertOperateLog.setOperateContent("??????????????????");
        alertOperateLog.setOperateModel("alert_field");
        alertOperateLog.setOperateModelDesc("??????????????????");
        alertOperateLog.setOperater(userName);
        alertOperateLog.setOperateTime(new Date());
        alertOperateLog.setOperateType("delete");
        alertOperateLog.setOperateTypeDesc("??????????????????");
        alertOperateLog.setRelationId(id);
        alertOperateLogMapper.insert(alertOperateLog);
        // ??????redis
//        AlertModelRequestDTO alertModelDetail = alertModelDao.getAlertModelDetail(modelId);
//        redisTemplate.opsForHash().delete(alertModelDetail.getTableName(), id);
    }

    @Override
    @Transactional(rollbackFor= Exception.class)
    public void updateAlertField(AlertFieldVo requestDTO) {
        requestDTO.setUpdateTime(new Date());
        AlertModel alertModelDetail = alertModelDao.getAlertModelDetail(requestDTO.getModelId());
        AlertField alertFieldById = alertFieldDao.getAlertFieldDetailById(requestDTO.getId());
        try {
            // ???????????????
            alertFieldDao.updateAlertField(requestDTO);
            if (!requestDTO.getDataLength().equals(alertFieldById.getDataLength()) ||
                    !requestDTO.getDataType().equals(alertFieldById.getDataType()) ||
                    !requestDTO.getDataTip().equals(alertFieldById.getDataTip())) {
                alertFieldDao.deleteFieldColumn(alertModelDetail.getTableName(),requestDTO.getFieldCode());
                Map<String, Object> map = Maps.newHashMap();
                map.put("tableName", alertModelDetail.getTableName());
                map.put("fieldCode", requestDTO.getFieldCode());
                map.put("jdbcType", requestDTO.getDataType());
                map.put("jdbcLength", StringUtils.isEmpty(requestDTO.getDataLength()) ? 0 : Integer.valueOf(requestDTO.getDataLength()));
                map.put("jdbcTip", requestDTO.getDataTip());
                alertFieldDao.addFieldColumn(map);
            }
            // ??????????????????
            AlertOperateLog alertOperateLog = new AlertOperateLog();
            alertOperateLog.setOperateContent("??????????????????");
            alertOperateLog.setOperateModel("alert_field");
            alertOperateLog.setOperateModelDesc("??????????????????");
            alertOperateLog.setOperater(requestDTO.getUpdater());
            alertOperateLog.setOperateTime(new Date());
            alertOperateLog.setOperateType("update");
            alertOperateLog.setOperateTypeDesc("??????????????????");
            alertOperateLog.setRelationId(requestDTO.getId());
            alertOperateLogMapper.insert(alertOperateLog);
            // ??????redis
//            AlertModelRequestDTO alertModelDetail = alertModelDao.getAlertModelDetail(requestDTO.getModelId());
//            redisTemplate.opsForHash().put(alertModelDetail.getTableName(), requestDTO.getId(), requestDTO);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new RuntimeException(e);
        }
    }

    @Override
    public PageResponse<AlertField> getAlertFieldListByModelId(Map<String,Object> request) {
//        Map<String, Object> param = Maps.newHashMap();
//        param.put("modelId", modelId);
//        param.put("searchText", searchText);
//        param.put("pageNum", pageNo);
//        param.put("pageSize", pageSize);
        PageResponse<AlertField> response = new PageResponse<AlertField>();
        response.setCount(alertFieldDao.getAlertFieldListCountByModelId(request));
        response.setResult(alertFieldDao.getAlertFieldListByModelId(request));
        return response;
    }

    @Override
    @Transactional(rollbackFor= Exception.class)
    public void updateLockStatus(String id, String modelId,String isLock, String userName) {
        try {
            // ???????????????
            alertFieldDao.updateLockStatus(id,isLock);
            // ??????????????????
            AlertOperateLog alertOperateLog = new AlertOperateLog();
            alertOperateLog.setOperateContent("??????????????????????????????");
            alertOperateLog.setOperateModel("alert_field");
            alertOperateLog.setOperateModelDesc("??????????????????");
            alertOperateLog.setOperater(userName);
            alertOperateLog.setOperateTime(new Date());
            alertOperateLog.setOperateType("update");
            alertOperateLog.setOperateTypeDesc("??????????????????????????????");
            alertOperateLog.setRelationId(id);
            alertOperateLogMapper.insert(alertOperateLog);
            // ??????redis
//            AlertModelRequestDTO alertModelDetail = alertModelDao.getAlertModelDetail(modelId);
            // ?????????????????????????????????redis
            // redisTemplate.opsForHash().put(alertModelDetail.getTableName(), id, );

        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new RuntimeException(e);
        }
    }

    @Override
    @Transactional(rollbackFor= Exception.class)
    public void synchronizeField(String modelId, String userName) {
        try {
            // ?????? redis ??????
            AlertModel alertModelDetail = alertModelDao.getAlertModelDetail(modelId);
//            List<Object> values = redisTemplate.opsForHash().values(alertModelDetail.getTableName());
//            List<AlertFieldRequestDTO> alertFieldRequestDTOS = PayloadParseUtil.jacksonBaseParse(AlertFieldRequestDTO.class, values);
            List<AlertFieldVo> alertFieldVos = getModelFromRedis(alertModelDetail.getTableName(), null);
            for (AlertFieldVo alertFieldVo : alertFieldVos) {
                if (alertFieldVo.getFieldType().equals("2")) {
                    alertFieldDao.deleteAlertFieldDetailById(alertFieldVo.getId(), alertFieldVo.getModelId());
                    alertFieldDao.deleteFieldColumn(alertModelDetail.getTableName(), alertFieldVo.getFieldCode());
//                    redisTemplate.opsForHash().delete(alertModelDetail.getTableName(),alertFieldRequestDTO.getId());
                }
            }
            List<AlertFieldVo> alert_alerts = getModelFromRedis("alert_alerts", null);
            for (AlertFieldVo alertFieldVo : alert_alerts) {
                if (alertFieldVo.getFieldType().equals("1")) continue;
                alertFieldVo.setId(UUID.randomUUID().toString());
                alertFieldVo.setModelId(modelId);
                // ???????????????
                alertFieldDao.insertAlertModel(alertFieldVo);
                Map<String, Object> map = Maps.newHashMap();
                map.put("tableName", alertModelDetail.getTableName());
                map.put("fieldCode", alertFieldVo.getFieldCode());
                map.put("jdbcType", alertFieldVo.getDataType());
                map.put("jdbcLength", StringUtils.isEmpty(alertFieldVo.getDataLength()) ? 0 : Integer.valueOf(alertFieldVo.getDataLength()));
                map.put("jdbcTip", alertFieldVo.getDataTip());
                alertFieldDao.addFieldColumn(map);
                // ??????redis
//                redisTemplate.opsForHash().put(alertModelDetail.getTableName(), alertFieldRequestDTO.getId(), alertFieldRequestDTO);
            }
            // ??????????????????
            AlertOperateLog alertOperateLog = new AlertOperateLog();
            alertOperateLog.setOperateContent("??????????????????");
            alertOperateLog.setOperateModel("alert_field");
            alertOperateLog.setOperateModelDesc("??????????????????");
            alertOperateLog.setOperater(userName);
            alertOperateLog.setOperateTime(new Date());
            alertOperateLog.setOperateType("insert");
            alertOperateLog.setOperateTypeDesc("??????????????????");
            alertOperateLog.setRelationId(modelId);
            alertOperateLogMapper.insert(alertOperateLog);
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new RuntimeException(e);
        }

    }

    /**
     * ????????????????????????
     * @param tableName
     * @return
     */
    public List<AlertFieldVo> getModelField (String tableName) {
        List<AlertFieldVo> alertFieldList = alertModelCache.get(tableName);
        if (CollectionUtils.isEmpty(alertFieldList)) {
            alertFieldList = getModelFromRedis(tableName, null);
            alertModelCache.put(tableName, alertFieldList);
        }
        return alertFieldList;
    }

    @Scheduled(cron = "0 0 */1 * * ?")
    public void flushAlertModelCache () {
        alertModelCache.clear();
    }
}
