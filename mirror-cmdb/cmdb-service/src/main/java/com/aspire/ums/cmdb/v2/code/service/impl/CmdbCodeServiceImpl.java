package com.aspire.ums.cmdb.v2.code.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.aspire.ums.cmdb.code.payload.*;
import com.aspire.ums.cmdb.collectApproval.payload.CmdbCollectApproval;
import com.aspire.ums.cmdb.common.Constants;
import com.aspire.ums.cmdb.common.Result;
import com.aspire.ums.cmdb.common.throwable.CmdbRuntimeException;
import com.aspire.ums.cmdb.dict.payload.ConfigDict;
import com.aspire.ums.cmdb.dict.service.ConfigDictService;
import com.aspire.ums.cmdb.helper.JDBCHelper;
import com.aspire.ums.cmdb.module.payload.Module;
import com.aspire.ums.cmdb.schema.mapper.SchemaMapper;
import com.aspire.ums.cmdb.sqlManage.CmdbSqlManage;
import com.aspire.ums.cmdb.util.ResultUtils;
import com.aspire.ums.cmdb.util.StringUtils;
import com.aspire.ums.cmdb.util.UUIDUtil;
import com.aspire.ums.cmdb.v2.code.mapper.CmdbCodeMapper;
import com.aspire.ums.cmdb.v2.code.service.ICmdbCodeService;
import com.aspire.ums.cmdb.v2.collect.service.CmdbCollectApprovalService;
import com.aspire.ums.cmdb.v2.module.mapper.ModuleMapper;
import com.aspire.ums.cmdb.v2.module.service.ModuleService;
import com.aspire.ums.cmdb.v3.approve.AbstractApproveFactory;
import com.aspire.ums.cmdb.v3.code.payload.CmdbV3CodeApprove;
import com.aspire.ums.cmdb.v3.code.payload.CmdbV3CodeBindSource;
import com.aspire.ums.cmdb.v3.code.payload.CmdbV3CodeCollect;
import com.aspire.ums.cmdb.v3.code.payload.CmdbV3CodeValidate;
import com.aspire.ums.cmdb.v3.code.service.*;
import com.aspire.ums.cmdb.v3.config.payload.CmdbConfig;
import com.aspire.ums.cmdb.v3.config.service.ICmdbConfigService;
import com.aspire.ums.cmdb.v3.module.mapper.CmdbV3ModuleCodeSettingMapper;
import com.aspire.ums.cmdb.v3.module.payload.CmdbV3ModuleCatalog;
import com.aspire.ums.cmdb.v3.module.payload.CmdbV3ModuleCodeSetting;
import com.aspire.ums.cmdb.v3.module.service.ICmdbV3ModuleCatalogService;
import com.aspire.ums.cmdb.v3.redis.service.IRedisService;
import com.aspire.ums.cmdb.v3.validator.AbstractValidFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.util.*;

/**
* ?????????
* @author
* @date 2019-05-13 18:39:38
*/
@Service
@Slf4j
public class CmdbCodeServiceImpl implements ICmdbCodeService {
    @Autowired
    private CmdbCodeMapper mapper;
    @Autowired
    private CmdbV3ModuleCodeSettingMapper codeSettingMapper;
    @Autowired
    private ModuleMapper moduleMapper;
    @Autowired
    private SchemaMapper schemaMapper;
    @Autowired
    private ICmdbV3ModuleCatalogService catalogService;
    @Autowired
    private ICmdbV3CodeBindSourceService bindSourceService;
    @Autowired
    private ICmdbV3CodeApproveService approveService;
    @Autowired
    private ICmdbV3CodeCollectService collectService;
    @Autowired
    private ICmdbV3CodeValidateService validateService;
    @Autowired
    private IRedisService redisService;
    @Autowired
    private ICmdbV3CodeCascadeService codeCascadeService;
    @Autowired
    private ICmdbV3CodeTableService codeTableService;
    @Autowired
    private CmdbCollectApprovalService approvalService;
    @Autowired
    private JDBCHelper jdbcHelper;
    @Autowired
    private ConfigDictService configDictService;
    @Autowired
    private ModuleService moduleService;
    @Autowired
    private ICmdbConfigService configService;


    // ??????????????????
    private static Map<String, AbstractValidFactory> VALID_FACTORY_MAP = new HashMap<>();

    // ??????????????????
    private static Map<String, AbstractApproveFactory> APPROVE_FACTORY_MAP = new HashMap<>();

    /**
     * ??????????????????
     * @return ????????????????????????
     */
    @Override
    public Result<CmdbCode> list(CmdbCodeQuery query) {
        Result<CmdbCode> result = new Result<>();
        List<CmdbCode> codeList = mapper.list(query);
        result.setData(codeList);
        result.setTotalSize(mapper.listCount(query));
        return result;
    }

    /**
     * ????????????ID ??????????????????
     * @param entity ????????????
     * @return ????????????ID???????????????
     */
    @Override
    public CmdbCode get(CmdbCode entity) {
        if (StringUtils.isEmpty(entity.getCodeId())) {
            if (StringUtils.isEmpty(entity.getFiledCode()) || StringUtils.isEmpty(entity.getModuleCatalogId())) {
                throw new RuntimeException("??????codeId ???????????? moduleCatalogId ??? filedCode");
            }
            return mapper.get(entity);
        }
        Object object = redisService.get(String.format(Constants.REDIS_CODE_DETAIL, entity.getCodeId()));
        if (object == null) {
            return mapper.get(entity);
        }
        return (new ObjectMapper()).convertValue(object, new TypeReference<CmdbCode>(){});
    }

    @Override
    public CmdbSimpleCode getByEntity(CmdbCode entity) {
        return mapper.getByEntity(entity);
    }

    @Override
    public CmdbSimpleCode getSimpleCodeById(String codeId) {
        return mapper.getSimpleCodeById(codeId);
    }

    /**
     * ????????????IDs?????? ??????????????????
     * @param ids ??????ids??????????????????
     * @return ?????????????????????????????????
     */
    @Override
    public List<CmdbCode> listByIds(List<String> ids) {
        List<CmdbCode> codeList = new ArrayList<>();
        if (ids != null && ids.size() > 0) {
            ids.forEach((id) -> {
                CmdbCode queryCode = new CmdbCode();
                queryCode.setCodeId(id);
                codeList.add(this.get(queryCode));
            });
        }
        return codeList;
    }

    /**
     * ????????????
     * @param entity ????????????
     * @return
     */
    @Override
    @Transactional(rollbackFor = { Exception.class, RuntimeException.class })
    public Map<String, String> insert(CmdbCode entity) {
        Map<String, String> returnMap = new HashMap<>();
        try {
            // ???????????????????????????????????????
            CmdbCode codeQuery = new CmdbCode();
            codeQuery.setFiledCode(entity.getFiledCode());
            codeQuery.setModuleCatalogId(entity.getModuleCatalogId());
            CmdbCode codeEntity = mapper.get(codeQuery);
            if (codeEntity != null) {
                returnMap.put("flag", "error");
                returnMap.put("msg", "?????????????????????????????????????????????.");
                return returnMap;
            }
//            CmdbCode nameQuery = new CmdbCode();
//            nameQuery.setFiledName(entity.getFiledName());
//            nameQuery.setModuleCatalogId(entity.getModuleCatalogId());
//            CmdbCode nameEntity = mapper.get(nameQuery);
//            if (nameEntity != null) {
//                returnMap.put("flag", "error");
//                returnMap.put("msg", "?????????????????????????????????????????????.");
//                return returnMap;
//            }
            String codeId = UUIDUtil.getUUID();
            // ?????????????????????
            if (Constants.YES.equals(entity.getIsBindSource())) {
                CmdbV3CodeBindSource bindSource = entity.getCodeBindSource();
                bindSource.setId(UUIDUtil.getUUID());
                bindSource.setCodeId(codeId);
                bindSourceService.insert(bindSource);
            }
            // ??????????????????
            if (Constants.YES.equals(entity.getIsApprove())) {
                CmdbV3CodeApprove approve = entity.getApprove();
                approve.setId(UUIDUtil.getUUID());
                approve.setCodeId(codeId);
                approveService.insert(approve);
            }
            // ??????????????????
            if (Constants.YES.equals(entity.getIsCollect())) {
                CmdbV3CodeCollect collect = entity.getCodeCollect();
                collect.setId(UUIDUtil.getUUID());
                collect.setCodeId(codeId);
                collectService.insert(collect);
            }
            // ??????????????????
            if (Constants.YES.equals(entity.getIsValidate())) {
                List<CmdbV3CodeValidate> validate = entity.getValidates();
                if (validate != null && validate.size() > 0) {
                    validate.forEach((v) -> {
                        v.setId(UUIDUtil.getUUID());
                        v.setCodeId(codeId);
                    });
                }
                validateService.insertByBatch(validate);
            }
            // ????????????????????????
            codeCascadeService.deleteByCodeId(entity.getCodeId());
            List<CmdbV3CodeCascade> cascadeList = entity.getCascadeList();
            if (cascadeList != null && cascadeList.size() > 0) {
                cascadeList.forEach((v) -> {
                    v.setId(UUIDUtil.getUUID());
                    v.setCodeId(codeId);
                });
                codeCascadeService.insertByBatch(cascadeList);
            }
            // ????????????????????????
            codeTableService.deleteByCodeId(entity.getCodeId());
            List<CmdbV3CodeTable> tableList = entity.getTableColList();
            if (tableList != null && tableList.size() > 0) {
                tableList.forEach((v) -> {
                    v.setId(UUIDUtil.getUUID());
                    v.setCodeId(codeId);
                });
                codeTableService.insertByBatch(tableList);
            }
            entity.setCodeId(codeId);
            mapper.insert(entity);
            returnMap.put("flag", "success");
            // ??????redis??????
            this.redisService.syncRefresh(Constants.REDIS_TYPE_CODE, codeId);
        } catch (Exception e) {
            log.error("??????????????????. {}", e.getMessage(), e);
            returnMap.put("flag", "error");
            returnMap.put("msg", "??????????????????:" + e.getMessage());
        }
        return returnMap;
    }

    /**
     * ????????????
     * @param entity ????????????
     * @return
     */
    @Override
    @Transactional(rollbackFor = { Exception.class, RuntimeException.class })
    public Map<String, String> update(CmdbCode entity) {
        Map<String, String> returnMap = new HashMap<>();
        try {
//            CmdbCode nameQuery = new CmdbCode();
//            nameQuery.setFiledName(entity.getFiledName());
//            nameQuery.setModuleCatalogId(entity.getModuleCatalogId());
//            CmdbCode nameEntity = mapper.get(nameQuery);
//            if (nameEntity != null && !nameEntity.getCodeId().equals(entity.getCodeId())) {
//                returnMap.put("flag", "error");
//                returnMap.put("msg", "???????????????????????????.");
//                return returnMap;
//            }
            CmdbV3ModuleCodeSetting codeSetting = new CmdbV3ModuleCodeSetting();
            List<String> columnSqls = new ArrayList<>();
            CmdbCode queryCode = new CmdbCode();
            queryCode.setCodeId(entity.getCodeId());
            CmdbCode oldCode = mapper.get(queryCode);
            if (null == oldCode.getCodeLength()) {
                oldCode.setCodeLength(40);
            }
            if (entity.getCodeLength() > oldCode.getCodeLength()) {
                // todo ?????????
//                codeRelation.setCodeId(entity.getCodeId());
//                List<CmdbModuleCodeRelation> relations = codeRelationMapper.listByEntity(codeRelation);
//                for (CmdbModuleCodeRelation relation : relations) {
//                    Module module = moduleMapper.getModuleDetail(relation.getModuleId());
//                    String tableName = CmdbConst.getModuleTable(module.getModuleCatalog().getCatalogCode()) + module.getCode();
//                    String columnSql = entity.getFiledCode() + " VARCHAR(" + entity.getCodeLength() + ")";
//                    columnSqls.add(columnSql);
//                    tableMapper.alterModifyTable(tableName, columnSqls);
//                }
            }
            // ?????????????????????
            bindSourceService.deleteByCodeId(entity.getCodeId());
            CmdbV3CodeBindSource bindSource = entity.getCodeBindSource();
            if (bindSource != null && Constants.YES.equals(entity.getIsBindSource())) {
                bindSource.setId(UUIDUtil.getUUID());
                bindSource.setCodeId(entity.getCodeId());
                bindSourceService.insert(bindSource);
            }
            // ??????????????????
            approveService.deleteByCodeId(entity.getCodeId());
            CmdbV3CodeApprove approve = entity.getApprove();
            if (approve != null && Constants.YES.equals(entity.getIsApprove())) {
                approve.setId(UUIDUtil.getUUID());
                approve.setCodeId(entity.getCodeId());
                approveService.insert(approve);
            }
            // ??????????????????
            collectService.deleteByCodeId(entity.getCodeId());
            CmdbV3CodeCollect collect = entity.getCodeCollect();
            if (collect != null && Constants.YES.equals(entity.getIsCollect())) {
                collect.setId(UUIDUtil.getUUID());
                collect.setCodeId(entity.getCodeId());
                collectService.insert(collect);
            }
            // ??????????????????
            validateService.deleteByCodeId(entity.getCodeId());
            List<CmdbV3CodeValidate> validate = entity.getValidates();
            if (validate != null && validate.size() > 0 && Constants.YES.equals(entity.getIsValidate())) {
                validate.forEach((v) -> {
                    v.setId(UUIDUtil.getUUID());
                    v.setCodeId(entity.getCodeId());
                });
                validateService.insertByBatch(validate);
            }
            // ????????????????????????
            codeCascadeService.deleteByCodeId(entity.getCodeId());
            List<CmdbV3CodeCascade> cascadeList = entity.getCascadeList();
            if (cascadeList != null && cascadeList.size() > 0) {
                cascadeList.forEach((v) -> {
                    v.setId(UUIDUtil.getUUID());
                    v.setCodeId(entity.getCodeId());
                });
                codeCascadeService.insertByBatch(cascadeList);
            }
            // ????????????????????????
            codeTableService.deleteByCodeId(entity.getCodeId());
            List<CmdbV3CodeTable> tableList = entity.getTableColList();
            if (tableList != null && tableList.size() > 0) {
                tableList.forEach((v) -> {
                    v.setId(UUIDUtil.getUUID());
                    v.setCodeId(entity.getCodeId());
                });
                codeTableService.insertByBatch(tableList);
            }
            mapper.update(entity);
            returnMap.put("flag", "success");
            // ??????redis??????
            redisService.asyncRefresh(Constants.REDIS_TYPE_CODE, entity.getCodeId());
        } catch (Exception e) {
            log.error("??????????????????. {}", e.getMessage(), e);
            returnMap.put("flag", "error");
            returnMap.put("msg", "??????????????????:" + e.getMessage());
        }
        return returnMap;
    }

    /**
     * ????????????
     * @param cmdbCode ????????????
     * @return
     */
    @Override
    @Transactional(rollbackFor = { Exception.class, RuntimeException.class })
    public Map<String, String> delete(CmdbCode cmdbCode) {
        Map<String, String> returnMap = new HashMap<>();
        try {
            CmdbV3ModuleCodeSetting codeRelation = new CmdbV3ModuleCodeSetting();
            cmdbCode.setIsDelete(1);
            codeRelation.setCodeId(cmdbCode.getCodeId());
            List<Module> modules = moduleMapper.getModuleByCodeId(cmdbCode.getCodeId());
            // ???????????????????????????, ??????????????????
            if (modules != null && modules.size() > 0) {
                StringBuilder moduleBuf = new StringBuilder();
                for (Module module : modules) {
                    moduleBuf.append(module.getName());
                    moduleBuf.append("???");
                }
                moduleBuf.deleteCharAt(moduleBuf.length()-1);
                returnMap.put("flag", "error");
                returnMap.put("msg", "?????????????????????["+moduleBuf.toString()+"]??????, ????????????????????????.");
                return returnMap;
            }
            // ?????????????????????
            bindSourceService.deleteByCodeId(cmdbCode.getCodeId());
            // ??????????????????
            approveService.deleteByCodeId(cmdbCode.getCodeId());
            // ??????????????????
            collectService.deleteByCodeId(cmdbCode.getCodeId());
            // ??????????????????
            validateService.deleteByCodeId(cmdbCode.getCodeId());
            // ????????????????????????
            codeCascadeService.deleteByCodeId(cmdbCode.getCodeId());
            // ????????????????????????
            codeTableService.deleteByCodeId(cmdbCode.getCodeId());
            // ??????????????????????????????????????????
            CmdbCollectApproval approval = new CmdbCollectApproval();
            approval.setCodeId(cmdbCode.getCodeId());
            approvalService.delete(approval);
            // ????????????
            mapper.delete(cmdbCode);
            // todo ?????????????????????
            // ??????redis??????
            redisService.syncRefresh(Constants.REDIS_TYPE_CODE, cmdbCode.getCodeId());
            returnMap.put("flag", "success");
        } catch (Exception e) {
            log.error("??????????????????. {}", e.getMessage(), e);
            returnMap.put("flag", "error");
            returnMap.put("msg", "??????????????????:" + e.getMessage());
        }
        return returnMap;
    }

    @Override
    public List<CmdbCodeGroup> queryCodeListFormatGroup(String catalogId) {
        Object object = redisService.get(String.format(Constants.REDIS_CODE_GROUP_PREFIX, catalogId));
        if (object == null) {
            List<CmdbCodeGroup> list = new LinkedList<>();
            CmdbCode cmdbCode = new CmdbCode();
            if (StringUtils.isEmpty(catalogId)) {
                cmdbCode.setModuleCatalogId("0");
                List<CmdbCode> codeList = mapper.listByEntity(cmdbCode);
                list.add(new CmdbCodeGroup("????????????", codeList));
                List<CmdbV3ModuleCatalog> catalogList = catalogService.getFirstLevel();
                if (catalogList != null && catalogList.size() > 0) {
                    for (CmdbV3ModuleCatalog catalog : catalogList) {
                        CmdbCode code = new CmdbCode();
                        code.setModuleCatalogId(catalog.getId());
                        List<CmdbCode> codeList2 = mapper.listByEntity(code);
                        list.add(new CmdbCodeGroup(catalog.getCatalogName(), codeList2));
                    }
                }
            } else {
                CmdbCode code = new CmdbCode();
                code.setModuleCatalogId(catalogId);
                List<CmdbCode> codeList2 = mapper.listByEntity(code);
                CmdbV3ModuleCatalog catalog = catalogService.getById(catalogId);
                list.add(new CmdbCodeGroup(catalog.getCatalogName(), codeList2));
            }
            return list;
        }
        return (new ObjectMapper()).convertValue(object, new TypeReference<List<CmdbCodeGroup>>(){});

    }

    @Override
    public List<CmdbCode> listByEntity(CmdbCode cmdbCode) {
        return mapper.listByEntity(cmdbCode);
    }

    /**
     * ?????????????????????????????????
     * @param cmdbCode ?????????
     * @return
     */
    @Override
    public List<CmdbSimpleCode> simpleCodeListByEntity(CmdbCode cmdbCode) {
        return mapper.simpleCodeListByEntity(cmdbCode);
    }

    @Override
    public List<CmdbCode> getCodeListByModuleId(String moduleId) {
        List<CmdbCode> codeList = mapper.getCodeListByModuleId(moduleId);
        // todo ??????????????????, ???????????????redis???
//        if (codeList != null && codeList.size() > 0) {
//            redisService.set(Constants.REDIS_MODULE_PREFIX + moduleId, codeList);
//        }
        return codeList;
    }

    @Override
    public CmdbSimpleCode getSimpleCodeByCodeAndModuleId(String moduleId, String filedCode) {
        return mapper.getSimpleCodeByCodeAndModuleId(moduleId, filedCode);
    }

    @Override
    public List<CmdbSimpleCode> getSimpleCodeListByModuleId(String moduleId) {
        return mapper.getSimpleCodeListByModuleId(moduleId);
    }

    @Override
    public List<CmdbCode> getSelfCodeListByModuleId(String moduleId) {
        return mapper.getSelfCodeListByModuleId(moduleId);
    }

    @Override
    public String changeCodeLength(String filedCode, Integer changeLength) {
        StringBuilder returnBuilder = new StringBuilder();
        // ????????? ??????????????????
        try {
            CmdbV3ModuleCodeSetting codeSetting = new CmdbV3ModuleCodeSetting();
            CmdbCode queryCode = new CmdbCode();
            queryCode.setFiledCode(filedCode);
            CmdbCode cmdbCode = get(queryCode);
            if (cmdbCode == null) {
                return "???????????????[" + filedCode + "]??????";
            }
            // ??????????????? ????????????cmdb_instance???
            List<String> list = new LinkedList<>();
            List<Module> modules = moduleMapper.getModuleByCodeId(codeSetting.getCodeId());
//            List<CmdbModuleCodeRelation> relations = codeSettingMapper.(codeSetting);
            if (modules != null) {
                for (Module module : modules) {
                    // ???????????????
//                    Module module = moduleMapper.getModuleDetail(relation.getModuleId());
                    if (module != null) {
                        StringBuilder builder = new StringBuilder();
                        builder.append("ALTER TABLE ").append(module.getModuleCatalog().getCatalogCode())
                                .append(" MODIFY ")
                                .append(filedCode)
                                .append(" VARCHAR(")
                                .append(changeLength)
                                .append("), ALGORITHM=INPLACE;").append("\r\n");
                        list.add(builder.toString());
                    }
                }
            }
            List<String> successList = new LinkedList<>();
            List<String> failedList = new LinkedList<>();
            list.stream().forEach((item) -> {
                try {
                    schemaMapper.alterTable(item);
                    successList.add(item);
                } catch (Exception e) {
                    failedList.add(item);
                }
            });
            returnBuilder.append("##-???????????????DDL----------------").append("\r\n");
            successList.stream().forEach((item) -> {
                returnBuilder.append(item).append("\r\n");
            });
            returnBuilder.append("##-???????????????DDL----------------").append("\r\n");
            failedList.stream().forEach((item) -> {
                returnBuilder.append(item).append("\r\n");
            });
            try {
                // ???????????????
                cmdbCode.setCodeLength(changeLength);
                mapper.update(cmdbCode);
            } catch (Exception e) {
                returnBuilder.append("##-????????????????????????----------------").append("\r\n");
                returnBuilder.append(e.getMessage()).append("\r\n");
            }
        }  catch (Exception e) {
            log.error("????????????:" + e.getMessage());
            return "????????????:" + e.getMessage();
        }
        return returnBuilder.toString();
    }

    @Override
    public List<Map<String, String>> getDistinctCodeList() {
        return mapper.getDistinctCodeList();
    }

    @Override
    public Map<String, Map<String, String>> validCodeValue(Map<String, Object> params) {
        Map<String, Map<String, String>> returnMap = new LinkedHashMap<>();
        if (params != null && params.keySet().size() > 0) {
            for (String codeId : params.keySet()) {
                if (codeId.equalsIgnoreCase("namespace")) {
                    continue;
                }
                Map<String, String> resultMap = new HashMap<>();
                CmdbCode queryCode = new CmdbCode();
                queryCode.setCodeId(codeId);
                CmdbCode cmdbCode = get(queryCode);
                if (cmdbCode == null) {
                    resultMap.put("flag", "error");
                    resultMap.put("msg", "CodeId " + codeId + " is invalid.");
                    returnMap.put(codeId, resultMap);
                    continue;
                }
                List<CmdbV3CodeValidate> validates = cmdbCode.getValidates();
                if (validates != null && validates.size() > 0) {
                    for (CmdbV3CodeValidate codeValidate : validates) {
                        Map<String, String> validMap = new HashMap<>();
                        try {
                            AbstractValidFactory validFactory;
                            if (VALID_FACTORY_MAP.containsKey(codeValidate.getHandlerClass())) {
                                validFactory = VALID_FACTORY_MAP.get(codeValidate.getHandlerClass());
                            } else {
                                Class clz = Class.forName(codeValidate.getHandlerClass());
                                Constructor constructor = clz.getConstructor();
                                validFactory = (AbstractValidFactory) constructor.newInstance();
                            }
                            Map<String, String> temMap = validFactory.valid(cmdbCode, codeValidate, params.get(codeId));
                            if (temMap.containsKey(ResultUtils.ERROR)) {
                                validMap.put("flag", ResultUtils.ERROR);
                                validMap.put("msg", temMap.get("msg"));
                            } else {
                                validMap.put("flag", ResultUtils.SUCCESS);
                            }
                        } catch (Exception e) {
                            validMap.put("flag", ResultUtils.ERROR);
                            validMap.put("msg", codeValidate.getHandlerClass() + " is invalid. Handler class must extends super class AbstractValidFactory.class.");
                        }
                        returnMap.put(codeId, validMap);
                        if (validMap.get("flag").equals(ResultUtils.ERROR)) {
                            break;
                        }
                    }
                } else {
                    resultMap.put("flag", ResultUtils.SUCCESS);
                    returnMap.put(codeId, resultMap);
                }
            }
        }
        return returnMap;
    }

    @Override
    public Map<String, Map<String, Object>> approveCodeValue(Map<String, Object> params) {
        Map<String, Map<String, Object>> returnMap = new LinkedHashMap<>();
        if (params != null && params.keySet().size() > 0) {
            for (String codeId : params.keySet()) {
                Map<String, Object> resultMap = new HashMap<>();
                CmdbCode queryCode = new CmdbCode();
                queryCode.setCodeId(codeId);
                // ??????CodeId????????????????????????????????????
                CmdbCode cmdbCode = get(queryCode);
                if (cmdbCode == null) {
                    resultMap.put("flag", "error");
                    resultMap.put("msg", "CodeId " + codeId + " is invalid.");
                    returnMap.put(codeId, resultMap);
                    continue;
                }
                CmdbV3CodeApprove approve = cmdbCode.getApprove();
                if (approve != null) {
                    Map<String, Object> validMap = new HashMap<>();
                    try {
                        AbstractApproveFactory approveFactory;
                        if (APPROVE_FACTORY_MAP.containsKey(approve.getHandlerClass())) {
                            approveFactory = APPROVE_FACTORY_MAP.get(approve.getHandlerClass());
                        } else {
                            Class clz = Class.forName(approve.getHandlerClass());
                            Constructor constructor = clz.getConstructor();
                            approveFactory = (AbstractApproveFactory) constructor.newInstance();
                        }
                        Map<String, String> temMap = approveFactory.valid(cmdbCode, approve, params.get(codeId));
                        if (temMap.containsKey(ResultUtils.SUCCESS)) {
                            validMap.put("flag", ResultUtils.SUCCESS);
                            Map<String,String> msg = new HashMap<>();
                            msg.put("status",temMap.get("status"));
                            msg.put("msg",temMap.get("msg"));
                            msg.put("reason",temMap.get("reason"));
                            validMap.put("msg",msg);
                        }
                    } catch (Exception e) {
                        validMap.put("flag", ResultUtils.ERROR);
                        Map<String,String> msg = new HashMap<>();
                        msg.put("msg", approve.getHandlerClass() + " is invalid. Handler class must extends super class AbstractValidFactory.class.");
                        msg.put("exceptionInfo",e.getMessage());
                        validMap.put("msg",msg);
                    }
                    returnMap.put(codeId, validMap);
                }
            }
        }
        return returnMap;
    }

    @Override
    public JSONObject validateCmdbCodeUnique(String filedCode, String moduleCatalogId) {
        JSONObject result = new JSONObject();
        Integer count = mapper.validateCmdbCodeUnique(filedCode,moduleCatalogId);
        if(count > 0) {
            result.put("flag",false);
            result.put("msg","??????????????????????????????,?????????");
        } else {
            result.put("flag",true);
            result.put("msg","?????????????????????????????????");
        }
        return result;
    }

//    @Override
//    public List<Map<String, Object>> getCodeDataSource(Map<String, Object> params) {
//        if (!params.containsKey("codeId")) {
//            throw new RuntimeException("Require param's codeId missing.");
//        }
//        CmdbCode cmdbCode = this.get(new CmdbCode(params.get("codeId").toString()));
//        List<Map<String, Object>> sourceDataList = Lists.newLinkedList();
//        if (("???").equals(cmdbCode.getIsBindSource()) && cmdbCode.getCodeBindSource() != null && cmdbCode.getCodeBindSource().getBindSourceType() != null) {
//            // ???????????????
//
//        }
//        return sourceDataList;
//    }

    @Override
    public LinkedList<CmdbCode> getCasParentCodes(List<String> codeIds) {
        LinkedList<CmdbCode> cmdbCodes = new LinkedList<>();
        LinkedHashMap<String, CmdbCode> codeMap = new LinkedHashMap<>();
        for (String id : codeIds) {
            // ?????????
            LinkedList<CmdbCode> codes = mapper.getCasParentCodes(id);
            codeMap.putAll(getParentCasCodes(codeMap, codes));
            codeMap.put(id, mapper.getById(id));
        }
        for (String key : codeMap.keySet()) {
            cmdbCodes.add(codeMap.get(key));
        }
        return cmdbCodes;
    }

    private LinkedHashMap<String, CmdbCode> getParentCasCodes(LinkedHashMap<String, CmdbCode> codeMap, LinkedList<CmdbCode> codes) {
        for (CmdbCode code : codes) {
            codeMap.put(code.getCodeId(), code);
            LinkedList<CmdbCode> casCodes = mapper.getCasParentCodes(code.getCodeId());
            if (casCodes != null && casCodes.size() > 0) {
                getParentCasCodes(codeMap, casCodes);
            }
        }
        return codeMap;
    }

    @Override
    public List<Map<String,String>> getCodeIdByNameAndModuleId(Map<String, Object> param) {
        return mapper.getCodeIdByNameAndModuleId(param);
    }

    @Override
    public <T> List<Map<String, T>> getRefCodeData(String codeId) {
        Object refValue = redisService.get(String.format(Constants.REDIS_CODE_DICT_DATA_VALUE, codeId));
        CmdbConfig config = configService.getConfigByCode("ref_code_use_redis", "???");
        if (refValue == null || config.getConfigValue().equals("???")) {
            List<Map<String, T>>  returnList = new ArrayList<>();
            CmdbCode queryCode = new CmdbCode();
            queryCode.setCodeId(codeId);
            queryCode = this.get(queryCode);
            if (queryCode == null) {
                throw new CmdbRuntimeException("ID???" + codeId + "??????????????????");
            }
            if ("???".equals(queryCode.getIsBindSource())) {
                return returnList;
            }
            if (queryCode.getCodeBindSource() == null) {
                return returnList;
            }
            switch (queryCode.getCodeBindSource().getBindSourceType()) {
                case "????????????":
                    final List<Map<String, T>> dictList = Lists.newLinkedList();
                    List<ConfigDict> configDictList = configDictService.selectDictsByType(queryCode.getCodeBindSource().getDictSource(), null, null, null);
                    configDictList.forEach(dict -> {
                        Map<String, T> dictMap = new HashMap<>();
                        dictMap.put("id", (T)dict.getId());
                        dictMap.put("key", (T)dict.getName());
                        dictMap.put("value", (T)dict.getValue());
                        dictList.add(dictMap);
                    });
                    returnList = dictList;
                    break;
                case "?????????":
                    CmdbSqlManage cmdbSqlManage = new CmdbSqlManage();
                    cmdbSqlManage.setChartSql(queryCode.getCodeBindSource().getTableSql().replace("\\'?'", "?"));
                    cmdbSqlManage.setNeedAuth(Constants.UN_NEED_AUTH);
                    returnList = jdbcHelper.getQueryList(cmdbSqlManage, null, null, null, null);
                    break;
                case "????????????":
                    String refModuleCodeId =queryCode.getCodeBindSource().getShowModuleCodeId();
                    String refModuleId = queryCode.getCodeBindSource().getRefModuleId();
                    String queryString = queryCode.getCodeBindSource().getRefModuleQuery();
                    CmdbSimpleCode refCode = this.getSimpleCodeById(refModuleCodeId);
                    if (refCode == null) {
                        throw new CmdbRuntimeException("ID???" + refModuleCodeId + "??????????????????");
                    }
                    String redisKey = String.format(Constants.REDIS_MODULE_DICT_DATA, refModuleId, refCode.getFiledCode());
                    Object object = redisService.get(redisKey);
                    String executeSql = "";
                    if (object == null) {
                        String baseSql = moduleService.getModuleQuerySQL(refModuleId);
                        executeSql = "select id, " + refCode.getFiledCode() + " `key`, " + refCode.getFiledCode() + " `value` from (" + baseSql + ") res";
                    } else {
                        Map<String, String> sqlMap = (new ObjectMapper().convertValue(object, new TypeReference<Map<String, String>>(){}));
                        executeSql = sqlMap.get("sql");
                    }
                    StringBuilder whereBuilder = new StringBuilder();
                    Map<String, Object> params = new HashMap<>();
                    if (!StringUtils.isEmpty(queryString)) {
                        List<Map<String, Object>> querySourceMap = JSONObject.parseObject(queryString, List.class);
                        for (Map<String, Object> query : querySourceMap) {
                            String filed = query.get("filed").toString();
                            String operator = query.get("operator").toString();
                            String value = query.get("value").toString();
                            if (query.containsKey("isOr") &&  "true".equals(query.get("isOr").toString())) {
                                whereBuilder.append("OR (1=1 ").append(handleSql(filed,value, operator)).append(")");
                            } else {
                                whereBuilder.append(handleSql(filed,value, operator));
                            }
                        }
                    }
                    CmdbSqlManage sqlManage = new CmdbSqlManage(executeSql, refModuleId, Constants.DICT_AUTH_MODULE, Constants.NEED_AUTH);
                    returnList = jdbcHelper.getQueryList(sqlManage, whereBuilder.toString(), null, null, params);
                    break;
                default:
                    throw new RuntimeException("?????????[" + queryCode.getFiledName() + "]???????????????, ??????????????????????????????[" + queryCode.getCodeBindSource().getBindSourceType() + "]");
            }
            redisService.set(String.format(Constants.REDIS_CODE_DICT_DATA_VALUE, codeId), returnList);
            return returnList;
        }
        return (new ObjectMapper()).convertValue(refValue, new TypeReference<List<Map<String, T>>>(){});
    }

    private String handleSql(String filed, String value, String operator) {
        switch (operator) {
            case "=" :
                return "null".equals(value) ? " and " + filed + " is null " : " and " + filed + " = '" + value + "' " ;
            case "!=" :
                return "null".equals(value) ? " and " + filed + " is not null " : " and " + filed + " != '" + value + "' " ;
            case ">=" : return " and " + filed + " <![CDATA[>=]]> '" + value + "' ";
            case "<=" : return " and " + filed + " <![CDATA[<=]]> '" + value + "' ";
            case "like" : return " and " + filed + " like '%" + value + "%' ";
            case "in" : return " and " + filed + " in ('" + value.replaceAll(",", "','") + "') ";
            case "not in" : return " and " + filed + " not in ('" + value.replaceAll(",", "','") + "') ";
            case "between" : return " and " + filed + " between '" + value.split(",")[0] + "' and '" + value.split(",")[1] + "' ";
            default: return "";
        }
    }
}
