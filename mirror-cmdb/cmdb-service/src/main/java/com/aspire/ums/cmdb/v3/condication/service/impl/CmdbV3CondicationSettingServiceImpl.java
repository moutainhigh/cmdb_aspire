package com.aspire.ums.cmdb.v3.condication.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.aspire.ums.cmdb.code.payload.CmdbCode;
import com.aspire.ums.cmdb.common.Constants;
import com.aspire.ums.cmdb.common.Result;
import com.aspire.ums.cmdb.dict.service.ConfigDictService;
import com.aspire.ums.cmdb.module.payload.Module;
import com.aspire.ums.cmdb.util.StringUtils;
import com.aspire.ums.cmdb.util.UUIDUtil;
import com.aspire.ums.cmdb.v2.code.service.ICmdbCodeService;
import com.aspire.ums.cmdb.v2.module.service.ModuleService;
import com.aspire.ums.cmdb.v3.condication.mapper.CmdbV3CondicationReturnRelationMapper;
import com.aspire.ums.cmdb.v3.condication.mapper.CmdbV3CondicationSettingMapper;
import com.aspire.ums.cmdb.v3.condication.mapper.CmdbV3CondicationSettingRelationMapper;
import com.aspire.ums.cmdb.v3.condication.mapper.CmdbV3CondicationSortRelationMapper;
import com.aspire.ums.cmdb.v3.condication.payload.CmdbV3AccessUser;
import com.aspire.ums.cmdb.v3.condication.payload.CmdbV3CondicationReturnRelation;
import com.aspire.ums.cmdb.v3.condication.payload.CmdbV3CondicationSetting;
import com.aspire.ums.cmdb.v3.condication.payload.CmdbV3CondicationSettingQuery;
import com.aspire.ums.cmdb.v3.condication.payload.CmdbV3CondicationSettingRelation;
import com.aspire.ums.cmdb.v3.condication.payload.CmdbV3CondicationSortRelation;
import com.aspire.ums.cmdb.v3.condication.service.ICmdbV3AccessUserService;
import com.aspire.ums.cmdb.v3.condication.service.ICmdbV3CondicationSettingService;
import com.aspire.ums.cmdb.v3.redis.service.IRedisService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
* ?????????
* @author
* @date 2020-01-09 14:33:20
*/
@Service
@Slf4j
public class CmdbV3CondicationSettingServiceImpl implements ICmdbV3CondicationSettingService {

    @Autowired
    private CmdbV3CondicationSettingMapper mapper;
    @Autowired
    private CmdbV3CondicationSettingRelationMapper relationMapper;
    @Autowired
    private CmdbV3CondicationReturnRelationMapper returnRelationMapper;
    @Autowired
    private CmdbV3CondicationSortRelationMapper sortRelationMapper;
    @Autowired
    private ICmdbV3AccessUserService userService;
    @Autowired
    private ModuleService moduleService;
    @Autowired
    private ConfigDictService dictService;
    @Autowired
    private IRedisService redisService;
    @Autowired
    private ICmdbCodeService codeService;
    @Value("${cmdb.access.inner}")
    private String innerUserId;

    /**
     * ??????????????????
     * @return ????????????????????????
     */
    @Override
    public List<CmdbV3CondicationSetting> list() {
        return mapper.list();
    }

    /**
     * ????????????ID ??????????????????
     * @param entity ????????????
     * @return ????????????ID???????????????
     */
    @Override
    public CmdbV3CondicationSetting get(CmdbV3CondicationSetting entity) {
         return mapper.get(entity);
    }

    /**
     * ?????????????????????????????????????????????????????????
     * @param indicationCode ????????????
     * @param accessUserId ????????????ID
     * @return
     */
    @Override
    public Map<String, Object> getSettingByCodeAndAccessUserId(String indicationCode, String accessUserId) {
        String key = indicationCode + "_" + accessUserId;
        Object object = redisService.get(String.format(Constants.REDIS_CONDITION_SETTING_DETAIL, key));
        if (object == null) {
            CmdbV3CondicationSetting querySetting = new CmdbV3CondicationSetting();
            querySetting.setAccessUserId(accessUserId);
            querySetting.setCondicationCode(indicationCode);
            CmdbV3CondicationSetting setting = get(querySetting);
            if (setting == null) {
                throw new RuntimeException("Can't find condication record. indicationCode -> " + indicationCode +
                        " accessUserId -> " + accessUserId);
            }
            // ??????SETTING
            Map<String, Object> returnMap = new HashMap<>();
            returnMap.put("moduleId", setting.getModuleId());
            // ??????????????????????????????
            List<CmdbV3CondicationSettingRelation> relationList = setting.getSettingRelationList();
            List<Map<String, Object>> paramsList = new LinkedList<>();
//            String defaultDeviceType = ""; // ???????????????????????????
            if (relationList != null && relationList.size() > 0) {
                for (CmdbV3CondicationSettingRelation relation : relationList) {
                    if (relation.getCmdbCode() == null) {
                        log.warn("Code id -> {} has been deleted.", relation.getCodeId());
                        continue;
                    }
                    Map<String, Object> paramMap = new HashMap<>();
                    paramMap.put("filed", relation.getCmdbCode().getFiledCode());
                    paramMap.put("filed_type", relation.getCmdbCode().getControlType().getControlCode());
                    paramMap.put("operator", relation.getOperateType());
                    Object value = null;
                    if (StringUtils.isNotEmpty(relation.getDefaultValue())) { // ???????????????
                        if (relation.getOperateType().equalsIgnoreCase("in")
                                || relation.getOperateType().equalsIgnoreCase("not in")
                                || relation.getOperateType().equalsIgnoreCase("between")) {
                            String[] temArray = String.valueOf(relation.getDefaultValue()).split("\\,");
                            value = Arrays.asList(temArray);
                        } else {
                            value = relation.getDefaultValue();
                        }
//                        // ??????????????????
//                        if (relation.getCmdbCode().getFiledCode().equalsIgnoreCase("device_type")) {
//                            defaultDeviceType = relation.getDefaultValue();
//                        }
                    }
                    paramMap.put("value", value);
                    if ("0".equals(relation.getIsRequire())) {
                        paramMap.put("require", true);
                    } else {
                        paramMap.put("require", false);
                    }
                    if (relation.getOperateType().equalsIgnoreCase("contain")) {
                        String codeId = relation.getCodeId(); // ?????????codeId
                        String containCodeId = relation.getContainCodeId();
                        // ??????????????????????????????????????????????????????????????????????????????????????????
                        if (!containCodeId.contains(codeId)) {
                            containCodeId += "," + codeId;
                        }
                        paramMap.put("containFiledId", containCodeId);
                        String[] split = containCodeId.split(",");
                        List<String> containFiledIdList = Arrays.asList(split);
                        String cmdbCodes = relationMapper.findCmdbCodeByIdList(containFiledIdList);
                        paramMap.put("containFiled", cmdbCodes);
                    }
                    paramsList.add(paramMap);
                }
            }
            returnMap.put("query", paramsList);
            String index , type;
            // ???????????????????????????????????????????????????
            Module module = moduleService.getModuleDetail(setting.getModuleId());
            index = module.getModuleCatalog().getCatalogCode();
            type = module.getCode();
            // ??????????????????
            returnMap.put("index", index);
            returnMap.put("type", type);
            // ???????????????
            List<CmdbV3CondicationReturnRelation> returnRelationList = setting.getReturnRelationList();
            if (returnRelationList != null && returnRelationList.size() > 0) {
                Map<String, String> resultMap = new HashMap<>();
                resultMap.put("id", "text");
                for (CmdbV3CondicationReturnRelation returnRelation : returnRelationList) {
                    if (returnRelation.getCmdbCode() != null) {
                        resultMap.put(returnRelation.getCmdbCode().getFiledCode(), returnRelation.getCmdbCode().getControlType().getControlCode());
                    }
                }
                returnMap.put("result", resultMap);
            }
            // ???????????????????????????
            List<CmdbCode> moduleCodeList = codeService.getCodeListByModuleId(module.getId());
            if (moduleCodeList != null && moduleCodeList.size() > 0) {
                Map<String, String> codeMap = new HashMap();
                for (CmdbCode cmdbCode : moduleCodeList) {
                    codeMap.put(cmdbCode.getFiledCode(), cmdbCode.getControlType().getControlCode());
                }
                returnMap.put("filed_list", codeMap);
            }
            // ??????????????????
            List<CmdbV3CondicationSortRelation> sortRelationList = setting.getSortRelationList();
            if (sortRelationList !=null && sortRelationList.size() > 0) {
                List<Map<String, String>> returnList = new LinkedList<>();
                for (CmdbV3CondicationSortRelation sortRelation : sortRelationList) {
                    if (sortRelation.getCmdbCode() != null) {
                        Map<String, String> sortMap = new HashMap<>();
                        sortMap.put("filed", sortRelation.getCmdbCode().getFiledCode());
                        sortMap.put("type", sortRelation.getSortType());
                        returnList.add(sortMap);
                    }
                }
                returnMap.put("sort", returnList);
            }
            return returnMap;
        }
        return (new ObjectMapper()).convertValue(object, new TypeReference<Map>(){});
    }

    /**
     * ????????????
     * @param entity ????????????
     * @return
     */
    @Override
    public Map<String, String> insert(CmdbV3CondicationSetting entity) {
        Map<String, String> returnMap = new HashMap<>();
        // ????????????????????????
        CmdbV3CondicationSetting queryEntity = new CmdbV3CondicationSetting();
        queryEntity.setCondicationName(entity.getCondicationCode());
        CmdbV3CondicationSetting dbEntity = mapper.get(queryEntity);
        if (dbEntity != null) {
            returnMap.put("flag", "error");
            returnMap.put("msg", "??????????????????????????????, ????????????.");
            return returnMap;
        }
        try {
            entity.setId(UUIDUtil.getUUID());
            this.batchInsertSetting(entity);
            this.batchInsertReturnSetting(entity);
            this.batchInsertSortSetting(entity);
            mapper.insert(entity);
            redisService.asyncRefresh(Constants.REDIS_TYPE_CONDITION_SETTING, entity);
        } catch (Exception e) {
            returnMap.put("flag", "error");
            returnMap.put("msg", "????????????????????????." + e.getMessage());
            return returnMap;
        }
        returnMap.put("flag", "success");
        return returnMap;
    }

    /**
     * ??????????????????????????????
     * @param entity
     */
    private void batchInsertSetting(CmdbV3CondicationSetting entity) {
        // ?????????????????????????????????
        relationMapper.deleteByCondicationSettingId(entity.getId());
        // ??????????????????
        List<CmdbV3CondicationSettingRelation> relationList = entity.getSettingRelationList();
        if (relationList != null && relationList.size() > 0) {
            relationList.forEach((relation) -> {
                relation.setId(UUIDUtil.getUUID());
                relation.setCondicationSettingId(entity.getId());
                relationMapper.insert(relation);
            });
        }
    }

    /**
     * ?????????????????????????????????
     * @param entity
     */
    private void batchInsertReturnSetting(CmdbV3CondicationSetting entity) {
        // ??????????????????????????????
        returnRelationMapper.deleteByCondicationSettingId(entity.getId());
        List<CmdbV3CondicationReturnRelation> returnRelationList = entity.getReturnRelationList();
        if (returnRelationList != null && returnRelationList.size() > 0) {
            returnRelationList.forEach((relation) -> {
                relation.setId(UUIDUtil.getUUID());
                relation.setCondicationSettingId(entity.getId());
                returnRelationMapper.insert(relation);
            });
        }
    }

    /**
     * ??????????????????????????????
     * @param entity
     */
    private void batchInsertSortSetting(CmdbV3CondicationSetting entity) {
        sortRelationMapper.deleteByCondicationSettingId(entity.getId());
        List<CmdbV3CondicationSortRelation> sortRelationList = entity.getSortRelationList();
        if (sortRelationList != null && sortRelationList.size() > 0) {
            sortRelationList.forEach((relation) -> {
                relation.setId(UUIDUtil.getUUID());
                relation.setCondicationSettingId(entity.getId());
                sortRelationMapper.insert(relation);
            });
        }
    }
    /**
     * ????????????
     * @param entity ????????????
     * @return
     */
    @Override
    public Map<String, String> update(CmdbV3CondicationSetting entity) {
        Map<String, String> returnMap = new HashMap<>();
        // ????????????????????????
        CmdbV3CondicationSetting queryEntity = new CmdbV3CondicationSetting();
        queryEntity.setCondicationName(entity.getCondicationCode());
        CmdbV3CondicationSetting dbEntity = mapper.get(queryEntity);
        if (dbEntity != null && !dbEntity.getId().equals(entity.getId())) {
            returnMap.put("flag", "error");
            returnMap.put("msg", "??????????????????????????????, ????????????.");
            return returnMap;
        }
        try {
            this.batchInsertSetting(entity);
            this.batchInsertReturnSetting(entity);
            this.batchInsertSortSetting(entity);
            mapper.update(entity);
            redisService.asyncRefresh(Constants.REDIS_TYPE_CONDITION_SETTING, entity);
            // ????????????tab???????????????????????????????????????
            if("instance_list".equals(entity.getCondicationCode())) {
                redisService.asyncRefresh(Constants.REDIS_TYPE_MODULE_TAB, entity);
            }
        } catch (Exception e) {
            returnMap.put("flag", "error");
            returnMap.put("msg", "????????????????????????." + e.getMessage());
            return returnMap;
        }
        returnMap.put("flag", "success");
        return returnMap;
    }

    /**
     * ????????????
     * @param entity ????????????
     * @return
     */
    @Override
    public Map<String, String> delete(CmdbV3CondicationSetting entity) {
        Map<String, String> returnMap = new HashMap<>();
        try {
            // ?????????????????????????????????
            relationMapper.deleteByCondicationSettingId(entity.getId());
            returnRelationMapper.deleteByCondicationSettingId(entity.getId());
            sortRelationMapper.deleteByCondicationSettingId(entity.getId());
            mapper.delete(entity);
            redisService.asyncRefresh(Constants.REDIS_TYPE_CONDITION_SETTING, entity);
            returnMap.put("flag", "success");
        } catch (Exception e) {
            returnMap.put("flag", "error");
            returnMap.put("msg", "????????????????????????:" + e.getMessage());
        }
        return returnMap;
    }

    @Override
    public Result<CmdbV3CondicationSetting> getCondicationSettingList(CmdbV3CondicationSettingQuery settingQuery) {
        Integer count = mapper.getCondicationSettingListCount(settingQuery);
        List<CmdbV3CondicationSetting> list = mapper.getCondicationSettingList(settingQuery);
        return new Result<>(count, list);
    }




//    @Override
//    public List<Map<String, Object>> validCondication(CmdbV3CondicationSetting cdt, List<Map<String,Object>> params) {
//        cdt = getCdtInfo(cdt);
//        List<Map<String, Object>> filterParams = new ArrayList<>();
//        List<CmdbV3CondicationSettingRelation> relations = cdt.getSettingRelationList();
//        if (params == null) {
//            if (relations != null && relations.size() > 0) {
//                throw new RuntimeException("??????????????????");
//            }
//            return filterParams;
//        }
//        // ?????????????????????{filed:value}
//        Map<String, Object> filedMap = new HashMap<>();
//        // ????????????????????????{filedcode:operator}
//        Map<String, Object> filedOperatorMap = new HashMap<>();
//        for (Map<String, Object> p : params) {
//            filedMap.put(p.get("filed").toString(), p.get("value"));
//            if (StringUtils.isNotEmpty(p.get("operator"))) {
//                filedOperatorMap.put(p.get("filed").toString(), p.get("operator"));
//            }
//        }
//        for (CmdbV3CondicationSettingRelation relation : relations) {
//            Map<String, Object> tempMap = new HashMap<>();
//            String cdtKey = relation.getCmdbCode().getFiledCode();
//            CmdbSimpleCode code = relation.getCmdbCode();
//            // ????????????
//            if (relation.getIsRequire().equals("0")) {
//                // ??????????????????????????????
//                if(!filedMap.containsKey(cdtKey) && StringUtils.isEmpty(relation.getDefaultValue())) {
//                    throw new RuntimeException("???????????????" + code.getFiledName() + "!");
//                }
//                // ???????????????????????????
//                if (!filedMap.containsKey(cdtKey) && StringUtils.isNotEmpty(relation.getDefaultValue())){
//                    tempMap = new HashMap<>();
//                    tempMap.put("filed", cdtKey);
//                    tempMap.put("value", relation.getDefaultValue());
//                    tempMap.put("operator", "=");
//                    filterParams.add(tempMap);
//                }
//            } else if (filedMap.containsKey(cdtKey)){
//                // ?????????????????????????????????
//                tempMap = new HashMap<>();
//                tempMap.put("filed", cdtKey);
//                tempMap.put("value", filedMap.get(cdtKey));
//                tempMap.put("operator", StringUtils.isNotEmpty(filedOperatorMap.get(cdtKey)) ? filedOperatorMap.get(cdtKey) :"=");
//                filterParams.add(tempMap);
//            }
//        }
//        return filterParams;
//    }

    @Override
    public Map<String, Object> parseQuery(Map<String, Object> params) {
        if (!params.containsKey("token") || params.get("token") == null || StringUtils.isEmpty(params.get("token").toString())) {
            log.error("Can't find authentication token.");
            throw new RuntimeException("Authentication failed.");
        }
        long startTime = new Date().getTime();
        String token = params.get("token").toString();
        String accessUserId;
        // ??????token??????
        if (!innerUserId.equals(token)) {
            if (!params.containsKey("condicationCode") || params.get("condicationCode") == null
                    || StringUtils.isEmpty(params.get("condicationCode").toString())) {
                log.error("Can't find query parameter[condicationCode].");
                throw new RuntimeException("Query parameter[condicationCode] is required.");
            }
            CmdbV3AccessUser accessUser = userService.getUserByToken(token);
            if (accessUser == null) {
                log.error("Invalid authentication token -> {}", token);
                throw new RuntimeException("Authentication failed.");
            }
            accessUserId = accessUser.getId();
        } else {
            accessUserId = innerUserId;
        }
        String condicationCode = params.get("condicationCode").toString();
        Map<String, Object> settingMap;
        try {
            settingMap = getSettingByCodeAndAccessUserId(condicationCode, accessUserId);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("Check token grant error. Please check it.");
        }
        Map<String, Object> returnMap = new HashMap<>();
        if (settingMap.containsKey("query")) {
            List<Map<String, Object>> settingParams = (List<Map<String, Object>>) settingMap.get("query");
            for (Map<String, Object> settingParam : settingParams) {
                String filed = settingParam.get("filed").toString();
                // ?????????????????????, ?????????????????????
                if (!accessUserId.equals(innerUserId)) {
                    // ??????
                    if ((Boolean) settingParam.get("require")) {
                        if (!params.containsKey(filed)) {
                            throw new RuntimeException("Query parameter[" + filed + "] is required.");
                        }
                    }
                }
                Object paramValue = params.get(filed);
                if (StringUtils.isNotEmpty(paramValue)) {
                    String operator = settingParam.get("operator").toString();
                    if (operator.equalsIgnoreCase("in")
                            || operator.equalsIgnoreCase("not in")
                            || operator.equalsIgnoreCase("between")) {
                        if (paramValue instanceof List) {
                            settingParam.put("value", paramValue);
                        } else {
                            String[] temArray = String.valueOf(paramValue).split("\\,");
                            settingParam.put("value", Arrays.asList(temArray));
                        }
                    } else if (operator.equalsIgnoreCase("contain")) {
                        settingParam.put("value", paramValue);
                        settingParam.put("containFiled", settingParam.get("containFiled"));
                    } else {
                        settingParam.put("value", paramValue);
                    }
                }
            }
            returnMap.put("params", settingParams);
        }
        if (!settingMap.containsKey("moduleId")) {
            throw new RuntimeException("?????????????????????????????????. ???????????????[" + condicationCode + "]");
        }
        String moduleId = settingMap.get("moduleId").toString();
        // ???????????????????????????????????????????????????
        if (params.containsKey("device_type") && StringUtils.isNotEmpty(params.get("device_type")) && params.get("device_type").toString().indexOf(",") == -1) {
            String deviceType = params.get("device_type").toString();
            Map<String, String> codeInfo = moduleService.getModuleCodeAndCatalogCodeByDeviceType(deviceType);
            if (codeInfo != null) {
                moduleId = codeInfo.get("id");
            }
        }
        // ???????????????module_id, ??????????????????module_id??????????????????.
        if (params.containsKey("module_id") && StringUtils.isNotEmpty(params.get("module_id"))) {
            moduleId = params.get("module_id").toString();
        }
        Module module = moduleService.getModuleDetail(moduleId);
        if (module == null) {
            throw new RuntimeException("Invalid module id [" + moduleId + "]");
        } else {
            returnMap.put("index", module.getModuleCatalog().getCatalogCode());
            returnMap.put("type", module.getCode());
            returnMap.put("query_module_id", moduleId);
        }
        if (settingMap.containsKey("result")) {
            returnMap.put("result", settingMap.get("result"));
        }
        if (params.containsKey("sort")) {
            try {
                List<Map<String, Object>> sortList = (List<Map<String, Object>>) params.get("sort");
                for (Map<String, Object> sort : sortList) {
                    if (!sort.containsKey("filed") && !StringUtils.isNotEmpty(sort.get("filed"))) {
                       continue;
                    }
                    if (!sort.containsKey("type") && !StringUtils.isNotEmpty(sort.get("type"))) {
                       continue;
                    }
                }
                returnMap.put("sort", params.get("sort"));
            } catch (Exception e) {
                log.error("Parse params.sort error. -> {}", e.getMessage());
            }
        } else if (settingMap.containsKey("sort")) {
            returnMap.put("sort", settingMap.get("sort"));
        }
        if (settingMap.containsKey("filed_list")) {
            returnMap.put("filed_list", settingMap.get("filed_list"));
        }
        if (params.containsKey("pageSize")) {
            try {
                returnMap.put("pageSize", params.get("pageSize"));
            } catch (Exception e) {
                throw new RuntimeException("Query parameter[pageSize] is invalid.");
            }
        } else {
            returnMap.put("pageSize", 50);
        }
        if (params.containsKey("currentPage")) {
            try {
                returnMap.put("currentPage", params.get("currentPage"));
            } catch (Exception e) {
                throw new RuntimeException("Query parameter[currentPage] is invalid.");
            }
        } else {
            returnMap.put("currentPage", 1);
        }
        log.info("=== #1.???????????????????????? {}s=====================", (new Date().getTime() - startTime) / 1000);
        return returnMap;
    }

    @Override
    public List<CmdbV3CondicationSetting> getConditionListByCodeId(String codeId) {
        return mapper.getConditionListByCodeId(codeId);
    }

    private CmdbV3CondicationSetting getCdtInfo(CmdbV3CondicationSetting cdt) {
        if (null == cdt) {
            throw new RuntimeException("????????????????????????");
        }
        // ??????????????????????????????????????????????????????????????????????????????id???
        if (StringUtils.isNotEmpty(cdt.getId())) {
            CmdbV3CondicationSetting c = new CmdbV3CondicationSetting();
            c.setId(cdt.getId());
            cdt = mapper.get(c);
        } else {
            if (StringUtils.isEmpty(cdt.getCondicationName()) || StringUtils.isEmpty(cdt.getCondicationType())) {
                throw new RuntimeException("???????????????????????????????????????id????????????????????????????????????");
            }
            cdt = mapper.get(cdt);
        }
        if (cdt == null || StringUtils.isEmpty(cdt.getId())) {
            throw new RuntimeException("???????????????????????????!");
        }
        return cdt;
    }

    @Override
    public JSONObject validConditionUnique(String code, String name) {
        Integer count = mapper.validConditionUnique(code,name);
        JSONObject result = new JSONObject();
        if(count > 0) {
            result.put("flag","false");
            result.put("msg","??????????????????,?????????");
        } else {
            result.put("flag","true");
            result.put("msg","????????????");
        }
        return result;
    }
}
