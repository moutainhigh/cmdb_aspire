package com.aspire.ums.cmdb.v2.instance.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.aspire.ums.cmdb.cmic.service.ICmdbModuleEventService;
import com.aspire.ums.cmdb.code.payload.CmdbCode;
import com.aspire.ums.cmdb.code.payload.CmdbControlType;
import com.aspire.ums.cmdb.code.payload.CmdbModuleCodeGroup;
import com.aspire.ums.cmdb.code.payload.CmdbSimpleCode;
import com.aspire.ums.cmdb.collectApproval.payload.CmdbCollectApproval;
import com.aspire.ums.cmdb.common.Constants;
import com.aspire.ums.cmdb.common.Result;
import com.aspire.ums.cmdb.dict.payload.ConfigDict;
import com.aspire.ums.cmdb.dict.service.ConfigDictService;
import com.aspire.ums.cmdb.helper.JDBCHelper;
import com.aspire.ums.cmdb.instance.payload.*;
import com.aspire.ums.cmdb.module.payload.Module;
import com.aspire.ums.cmdb.module.payload.SimpleModule;
import com.aspire.ums.cmdb.schema.service.SchemaService;
import com.aspire.ums.cmdb.sqlManage.CmdbSqlManage;
import com.aspire.ums.cmdb.sync.payload.CmdbOptType;
import com.aspire.ums.cmdb.sync.service.producer.CmdbModuleProducerServiceImpl;
import com.aspire.ums.cmdb.util.StringUtils;
import com.aspire.ums.cmdb.util.UUIDUtil;
import com.aspire.ums.cmdb.v2.code.service.ICmdbCodeService;
import com.aspire.ums.cmdb.v2.code.service.ICmdbControlTypeService;
import com.aspire.ums.cmdb.v2.collect.service.CmdbCollectApprovalService;
import com.aspire.ums.cmdb.v2.idc.entity.CmdbIdcManager;
import com.aspire.ums.cmdb.v2.idc.service.ICmdbIdcManagerService;
import com.aspire.ums.cmdb.v2.instance.handler.AbstractInstanceInsertFactory;
import com.aspire.ums.cmdb.v2.instance.mapper.CmdbInstanceMapper;
import com.aspire.ums.cmdb.v2.instance.service.ICmdbInstanceIpManagerService;
import com.aspire.ums.cmdb.v2.instance.service.ICmdbInstanceService;
import com.aspire.ums.cmdb.v2.module.CmdbConst;
import com.aspire.ums.cmdb.v2.module.service.ModuleService;
import com.aspire.ums.cmdb.v2.room.entity.CmdbRoomManager;
import com.aspire.ums.cmdb.v2.room.service.ICmdbRoomManagerService;
import com.aspire.ums.cmdb.v3.code.payload.CmdbV3CodeApprove;
import com.aspire.ums.cmdb.v3.code.payload.CmdbV3CodeBindSource;
import com.aspire.ums.cmdb.v3.code.service.ICmdbV3CodeApproveService;
import com.aspire.ums.cmdb.v3.condication.service.ICmdbV3CondicationSettingService;
import com.aspire.ums.cmdb.v3.config.payload.CmdbConfig;
import com.aspire.ums.cmdb.v3.config.service.ICmdbConfigService;
import com.aspire.ums.cmdb.v3.es.service.ICmdbESService;
import com.aspire.ums.cmdb.v3.module.event.EventConst;
import com.aspire.ums.cmdb.v3.module.payload.CmdbV3ModuleCatalog;
import com.aspire.ums.cmdb.v3.module.service.ICmdbV3ModuleCatalogService;
import com.aspire.ums.cmdb.v3.module.service.ICmdbV3ModuleCodeSettingService;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ?????????
 * 
 * @author
 * @date 2019-05-20 20:56:07
 */
@Service
@Slf4j
public class CmdbInstanceServiceImpl implements ICmdbInstanceService {

    @Autowired
    private CmdbInstanceMapper mapper;

    @Autowired
    private ICmdbInstanceIpManagerService ipManagerService;

    @Autowired
    private ModuleService moduleService;

    @Autowired
    private ICmdbIdcManagerService idcManagerService;

    @Autowired
    private CmdbCollectApprovalService approvalService;

    @Autowired
    private ICmdbRoomManagerService roomManagerService;

    @Autowired
    private ConfigDictService configDictService;

    @Autowired
    private ICmdbV3CondicationSettingService condctService;

    @Autowired
    private ICmdbESService cmdbESService;

    @Autowired
    private ICmdbCodeService codeService;

    @Autowired
    private ICmdbV3CodeApproveService codeApproveService;

    @Autowired
    private ICmdbControlTypeService controlTypeService;

    @Autowired
    private ICmdbV3ModuleCodeSettingService codeSettingService;

    @Autowired
    private ICmdbV3ModuleCatalogService catalogService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private ICmdbModuleEventService moduleEventService;

    @Autowired
    private JDBCHelper jdbcHelper;

    @Autowired
    private ICmdbConfigService cmdbConfigService;

    @Value("${cmdb.enable.modifyApproval: true}")
    private String modifyApproval;

    @Value("${cmdb.access.inner}")
    private String innerUserId;

    @Value("${cmdb.query.db:mysql}")
    private String queryDbType;

    @Autowired
    private CmdbModuleProducerServiceImpl cmdbModuleProducerService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * ??????????????????
     *
     * @return ????????????????????????
     */
    @Override
    public List<CmdbInstance> list() {
        return mapper.list();
    }

    @Override
    public List<CmdbInstance> listByEntity(CmdbInstance query) {
        return mapper.listByEntity(query);
    }

    /**
     * ????????????ID ??????????????????
     * 
     * @param id
     *            ????????????
     * @return ????????????ID???????????????
     */
    @Override
    public List<Map<String, Object>> getById(String id) {
        return mapper.getByInsId(id);
    }

    /**
     * ????????????ID ??????????????????
     * 
     * @param entity
     *            ????????????
     * @return ????????????ID???????????????
     */
    @Override
    public CmdbInstance get(CmdbInstance entity) {
        return mapper.get(entity);
    }

    /**
     * ????????????????????????
     *
     * @param params
     *            ????????????
     * @return ????????????ID???????????????
     */
    @Override
    public Map<String, Object> getByParams(String moduleId, Map<String, Object> params) {
        String sql = moduleService.getModuleQuerySQL(moduleId);
        // String querySQL = "select * from (" + sql + ") res where res.suyan_uuid=#{instanceId}";
        StringBuilder whereSql = new StringBuilder("where 1=1");
        for (String filed : params.keySet()) {
            whereSql.append(" and ").append(filed).append(" = #{").append(filed).append("}");
        }
        CmdbSqlManage sqlManage = new CmdbSqlManage(sql + whereSql.toString(), moduleId, Constants.INSTANCE_AUTH_MODULE,
                Constants.NEED_AUTH);
        List<Map<String, Object>> resultList = jdbcHelper.getQueryList(sqlManage, null, null, null, params);
        if (resultList.size() > 0) {
            return resultList.get(0);
        } else {
            return new HashMap<>();
        }

    }

    @Override
    public Map<String, Object> getInstanceDetail(String moduleId, String instanceId) {
        Module module = moduleService.getModuleDetail(moduleId);
        if (module == null) {
            throw new RuntimeException("Can't find module record. module id -> " + moduleId);
        }
        if (queryDbType != null && queryDbType.toLowerCase(Locale.ENGLISH).equals("es")) {
            return cmdbESService.getById(instanceId, module.getModuleCatalog().getCatalogCode(), module.getCode());
        }
        return getInstanceById(moduleId, instanceId);
    }

    private Map<String, Object> getInstanceById(String moduleId, String instanceId) {
        String querySQL = moduleService.getModuleQuerySQL(moduleId);
        querySQL = "select * from (" + querySQL + ") res where res.id=#{instanceId}";
        Map<String, Object> params = new HashMap<>();
        params.put("instanceId", instanceId);
        CmdbSqlManage cmdbSqlManage = new CmdbSqlManage(querySQL, moduleId, Constants.INSTANCE_AUTH_MODULE, Constants.NEED_AUTH);
        List<Map<String, Object>> returnList = jdbcHelper.getQueryList(cmdbSqlManage, null, null, null, params);
        if (returnList != null && returnList.size() > 0) {
            return returnList.get(0);
        }
        return new HashMap<>();
    }

    /**
     * ??????????????????????????????
     * 
     * @param approvalList
     *            ????????????
     * @return
     */
    private void addToApproval(List<CmdbCollectApproval> approvalList) {
        approvalService.insertByBatch(approvalList);
    }

    /**
     * ????????????????????????
     * 
     * @param moduleId
     *            ????????????
     * @param instanceData
     *            ????????????
     */
    private void checkUnique(String moduleId, Map<String, Object> instanceData) {
        // ????????????????????????
        Map<String, Object> dbData = moduleService.getModuleDataByPrimarys(moduleId, instanceData);
        if (dbData != null) {
            throw new RuntimeException("Input data is already exists, Please check it.");
        }
    }

    /**
     * ??????????????????????????????
     * 
     * @param instanceData
     *            ????????????
     * @return
     */
    @Override
    public CmdbCollectApproval handleAddApproval(String username, Map<String, Object> instanceData, String operatorType) {
        long startTime = System.currentTimeMillis();
        toCheckUnique(instanceData.get("module_id").toString(), instanceData);
        log.info(">>>>>> 2.1 ??????????????? ??????:{}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        Module module = moduleService.getModuleDetail(instanceData.get("module_id").toString());
        Map<String, List<Map<String, String>>> moduleCodeMap = new HashMap<>();
        StringBuilder currValue = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        for (String key : instanceData.keySet()) {
            params.put(key, instanceData.get(key));
        }
        params.put("customerType", "approve");
        String instanceId = UUIDUtil.getUUID();
        params.put("id", instanceId);
        String querySQL = moduleService.getModuleQuerySQL(module.getId());
        querySQL = "select * from (" + querySQL + ") res where res.id=#{id}";
        CmdbSqlManage cmdbSqlManage = new CmdbSqlManage(querySQL, Constants.INSTANCE_AUTH_MODULE, Constants.UN_NEED_AUTH);
        List<Map<String, Object>> returnList = jdbcHelper.getQueryList(cmdbSqlManage, null, null, null, params);
        if (returnList == null || returnList.size() == 0) {
            throw new RuntimeException("??????????????????????????????????????????");
        }
        log.info(">>>>>> 2.2 ???????????????????????????????????? ??????:{}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        // ??????????????????
        Map<String, Object> instanceDetail = returnList.get(0);
        Map<String, Map<String, String>> columnInfo = moduleService.getModuleColumns(module.getId());
        log.info(">>>>>> 2.3 ????????????????????? ??????:{}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        log.info(">>>>>> 2.4 ??????????????????????????? ??????:{}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();

        for (CmdbModuleCodeGroup group : module.getGroupList()) {
            for (CmdbCode code : group.getCodeList()) {
                String key = code.getCodeSetting().getOwnerModuleId();
                if (!moduleCodeMap.containsKey(key)) {
                    moduleCodeMap.put(key, new ArrayList<>());
                }
                if (StringUtils.isNotEmpty(instanceData.get(code.getFiledCode()))) {
                    String codeValue = "";
                    if (columnInfo.containsKey(code.getFiledCode())
                            && "ref".equals(columnInfo.get(code.getFiledCode()).get("type"))) {
                        log.debug("code.getFiledCode()=={}", code.getFiledCode());
                        log.debug("columnInfo.get(code.getFiledCode()).get(\"ref_name\")=={}",
                                columnInfo.get(code.getFiledCode()).get("ref_name"));
                        Object codeObj = instanceDetail.get(columnInfo.get(code.getFiledCode()).get("ref_name"));
                        if (codeObj != null) {
                            codeValue = codeObj.toString();
                        }

                    } else {
                        codeValue = instanceDetail.get(code.getFiledCode()).toString();
                    }
                    validCode(code, codeValue);
                    currValue.append(code.getFiledName());
                    currValue.append("->");
                    currValue.append(codeValue != null ? codeValue.trim() : codeValue);
                    currValue.append(",");
                } else {
                    continue;
                }
            }
        }
        log.info(">>>>>> 2.5 ???????????? ??????:{}", System.currentTimeMillis() - startTime);
        // ???????????????
        CmdbCollectApproval approval = new CmdbCollectApproval();
        approval.setInstanceId(instanceId);
        approval.setId(UUIDUtil.getUUID());
        approval.setOperatorTime(new Date());
        approval.setOperator(username);
        approval.setApprovalType("add");
        approval.setOperaterType(StringUtils.isNotEmpty(operatorType) ? operatorType : "????????????");
        approval.setApprovalStatus(0);
        approval.setModuleId(module.getId());
        approval.setResourceData(JSONObject.toJSONString(instanceData));
        approval.setCurrValue(currValue.deleteCharAt(currValue.length() - 1).toString());
        return approval;
    }


    private void toCheckUnique(String module_id, Map<String, Object> instanceData) {
        CmdbConfig config = cmdbConfigService.getConfigByCode("exclude_check_primary_module");
        boolean needCheckUnique = true;
        if (config != null) {
            List<String> excludeCheckModule = Arrays.asList(config.getConfigValue().split(","));
            if (excludeCheckModule.contains(module_id)) {
                needCheckUnique = false;
            }
        }
        if (needCheckUnique) {
            checkUnique(module_id, instanceData);
        }
    }

    @Override
    public void validCode(CmdbCode code, String codeValue) {
        long startTime = System.currentTimeMillis();
        // ??????????????????
        Integer codeLength = StringUtils.isNotEmpty(code.getCodeLength()) ? code.getCodeLength() : Constants.CODE_DEFAULT_LENGTH;
        if (codeValue.length() > codeLength) {
            throw new RuntimeException(
                    "the " + code.getFiledName() + " length is to small. You must change code length >= " + codeValue.length());
        }
        startTime = System.currentTimeMillis();
        // ????????????????????????
        Map<String, Object> codeParam = new HashMap<>();
        codeParam.put(code.getCodeId(), codeValue);
        Map<String, Map<String, String>> validResults = codeService.validCodeValue(codeParam);
        if (validResults.keySet().contains(code.getCodeId())) {
            Map<String, String> validResult = validResults.get(code.getCodeId());
            if ("error".equals(validResult.get("flag"))) {
                throw new RuntimeException("??????[" + code.getFiledName() + "]????????????????????? " + validResult.get("msg"));
            }
        }
        log.info(">>>>>> ---- !!! ???????????????????????? ?????????{}", System.currentTimeMillis() - startTime);
    }

    @Override
    public Map<String, Object> queryInstanceDetail(Map<String, Object> params, String moduleType) {
        if (!params.containsKey("token")) {
            params.put("token", innerUserId);
        }
        if (!params.containsKey("condicationCode")) {
            params.put("condicationCode", "instance_detail");
        }
        if (!params.containsKey("module_id") && !params.containsKey("device_type")) {
            Module module = moduleService.getDefaultModule(moduleType);
            params.put("module_id", module.getId());
        }
        Map<String, Object> queryParams = condctService.parseQuery(params);
        if (queryDbType != null && queryDbType.toLowerCase(Locale.ENGLISH).equals("es")) {
            return cmdbESService.queryDetail(queryParams);
        }
        Object[] formatQuerys = this.formatStatementParams(queryParams);
        String moduleId = queryParams.get("query_module_id").toString();
        String querySQL = moduleService.getModuleQuerySQL(moduleId);
        CmdbSqlManage sqlManage = new CmdbSqlManage(querySQL, moduleId, Constants.INSTANCE_AUTH_MODULE, Constants.NEED_AUTH);
        return jdbcHelper.getQueryMap(sqlManage, (String) formatQuerys[0], (Map<String, Object>) formatQuerys[1]);
    }

    /**
     * ??????CI????????????
     * 
     * @param params
     *            ???????????? ??????: { "params": [{ // ?????????????????? "operator": "?????????", "filed": "????????????", "value": "?????????", "filed_type": "??????????????????" }],
     *            "query_module_id": "???????????????ID", "result": [] //???????????? "index": "", //???????????? "type": "" //?????? }
     * @return ??????????????????
     */
    @Override
    public Map<String, Object> getInstanceByPrimaryKey(Map<String, Object> params) {
        if (queryDbType != null && queryDbType.toLowerCase(Locale.ENGLISH).equals("es")) {
            com.aspire.mirror.elasticsearch.api.dto.cmdb.Result<Map<String, Object>> returnResult = cmdbESService.list(params);
            if (returnResult == null) {
                return new HashMap<>();
            } else if (returnResult.getCount() > 1) {
                throw new RuntimeException("Too many result to be find. Except one. But find " + returnResult.getCount());
            } else if (returnResult.getCount() == 0) {
                return new HashMap<>();
            }
            return returnResult.getData().get(0);
        } else {
            Object[] formatQuerys = this.formatStatementParams(params);
            String moduleId = params.get("query_module_id").toString();
            String querySQL = moduleService.getModuleQuerySQL(moduleId);
            CmdbSqlManage sqlManage = new CmdbSqlManage(querySQL, moduleId, Constants.INSTANCE_AUTH_MODULE, Constants.NEED_AUTH);
            return jdbcHelper.getQueryMap(sqlManage, (String) formatQuerys[0], (Map<String, Object>) formatQuerys[1]);
        }
    }

    @Override
    public Map<String, Object> queryDeviceByIdcTypeAndIP(Map<String, Object> params) {
        if (!params.containsKey("token")) {
            params.put("token", innerUserId);
        }
        if (!params.containsKey("condicationCode")) {
            params.put("condicationCode", "instance_by_idc_ip");
        }
        if (!params.containsKey("ip")) {
            throw new RuntimeException("??????ip");
        }
        if (params.containsKey("idcType") && params.containsKey("is_cn") && (boolean) params.get("is_cn")) {
            Map<String, String> idc = configDictService.getIdcTypeByName(params.get("idcType").toString());
            if (idc == null) {
                throw new RuntimeException("??????????????????" + params.get("idcType").toString());
            }
            params.put("idcType", idc.get("id"));
        }
        Map<String, Object> returnMap = new HashMap<>();
        if (queryDbType != null && queryDbType.toLowerCase(Locale.ENGLISH).equals("es")) {
            List<Map<String, Object>> result = cmdbESService.queryDeviceByIpAndIdcType(condctService.parseQuery(params));
            if (result != null && result.size() > 0) {
                returnMap = result.get(0);
            }
        } else {
            Map<String, Object> queryParams = condctService.parseQuery(params);
            // String ip = params.get("ip").toString();
            // String idcType = StringUtils.isNotEmpty(params.get("idcType")) ? params.get("idcType").toString() : "";
            Result<Map<String, Object>> mapResult = getInstanceList(queryParams);
            if (mapResult.getTotalSize() != 1) {
                log.error("Get result error. Find {} record. Query params -> {}", mapResult.getTotalSize(), params);
                return null;
            }
            return mapResult.getData().get(0);
        }
        return returnMap;
    }

    /**
     * ???????????????????????????
     * 
     * @param moduleId
     *            ??????ID
     * @param moduleType
     *            ????????????
     * @return
     */
    @Override
    public List<CmdbSimpleCode> getInstanceHeader(String moduleId, String moduleType) {
        Module module;
        if (StringUtils.isNotEmpty(moduleId)) {
            module = moduleService.getModuleDetail(moduleId);
        } else {
            module = moduleService.getDefaultModule(moduleType);

        }
        if (module == null) {
            throw new RuntimeException("Can't find module record. moduleId -> " + moduleId + " moduleType -> " + moduleType);
        }
        return codeService.getSimpleCodeListByModuleId(module.getId());
    }

    @Override
    public List<Map<String, Object>> exportInstanceList(Map<String, Object> params, String moduleType) {
        if (!params.containsKey("token")) {
            params.put("token", innerUserId);
        }
        if (!params.containsKey("condicationCode")) {
            params.put("condicationCode", "instance_list");
        }
        Module module = new Module();
        if (!params.containsKey("module_id") && !params.containsKey("device_type")) {
            module = moduleService.getDefaultModule(moduleType);
            params.put("module_id", module.getId());
        } else {
            module = moduleService.getModuleDetail(params.get("module_id").toString());
        }
        List<Map<String, Object>> returnList = new LinkedList<>();
        Integer currentPage = 0, pageSize = 100;
        Map<String, Object> queryParams = condctService.parseQuery(params);
        queryParams.put("pageSize", null);
        queryParams.put("currentPage", null);
        Result<Map<String, Object>> pageResult = this.getInstanceList(params, moduleType);

        // ??????es??????
        // log.info("Request es params -> {}", queryParams);
        // com.aspire.mirror.elasticsearch.api.dto.cmdb.Result<Map<String, Object>> temp = cmdbESService.list(queryParams);
        // Integer totalCount = temp.getCount();
        // Integer totalPageSize = totalCount % pageSize == 0 ? (totalCount / pageSize) : (totalCount / pageSize + 1);
        // while (totalCount > 0 && currentPage <= totalPageSize) {
        // currentPage++;
        // queryParams.put("currentPage", currentPage);
        // queryParams.put("pageSize", pageSize);
        // log.info("Request es params -> {}", queryParams);
        // com.aspire.mirror.elasticsearch.api.dto.cmdb.Result<Map<String, Object>> temp1 = cmdbESService.list(queryParams);
        // if (temp1.getData() != null) {
        // returnList.addAll(temp1.getData());
        // }
        // }
        return returnList;
    }

    @Override
    public String addInstance(String userName, Map<String, Object> instanceData, String operateType) {
        long startTime = System.currentTimeMillis();
        String msg = "";
        SimpleModule module = validModule(instanceData);
        // ?????????????????????????????????, ???????????????????????????
        CmdbConfig specialInsert = cmdbConfigService.getConfigByCode("module_insert_handler:" + module.getId());
        if (specialInsert != null) {
            Map<String, Object> returnMap = specialHandler(specialInsert.getConfigValue(), userName, instanceData, operateType);
            return returnMap.get("msg").toString();
        }
        log.info(">>>>>> 1.????????????????????????????????? ?????????{}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        log.info(">>>>>> 2.???????????????????????????????????????");
        CmdbCollectApproval approval = handleAddApproval(userName, instanceData, operateType);
        log.info(">>>>>> 2.????????????????????????????????? ????????????{}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        // ????????????????????????????????????????????????????????????
        if (module.getEnableApprove() != null && module.getEnableApprove() == 0) {
            // ????????????
            approval.setApprovalUser(userName);
            log.info(">>>>>> 3.??????????????????");
            msg = this.insert(userName, approval);
            log.info(">>>>>> 3.?????????????????? ????????????{}", System.currentTimeMillis() - startTime);
        } else {
            // ????????????????????????
            List<CmdbCollectApproval> approvals = new ArrayList<>();
            approvals.add(approval);
            approvalService.insertByBatch(approvals);
            msg = "????????????????????????????????????????????????????????????";
            log.info(">>>>>> 4.???????????? ?????????{}", System.currentTimeMillis() - startTime);
        }
        return msg;
    }

    /**
     * ?????????????????????????????????
     * 
     * @param handlerClass
     *            ????????? ????????????
     * @param userName
     *            ????????????
     * @param instanceData
     *            ????????????
     * @param operateType
     *            ????????????
     * @return ??????????????????
     */
    private Map<String, Object> specialHandler(String handlerClass, String userName, Map<String, Object> instanceData,
            String operateType) {
        Map<String, Object> returnMap = new HashMap<>();
        try {
            Class clz = Class.forName(handlerClass);
            Constructor constructor = clz.getConstructor();
            AbstractInstanceInsertFactory factory = (AbstractInstanceInsertFactory) constructor.newInstance();
            return factory.execute(userName, instanceData, operateType);
        } catch (ClassNotFoundException e) {
            log.error("????????????????????? -> {}", handlerClass);
            returnMap.put("flag", "error");
            returnMap.put("msg", "?????????????????????");
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
            log.error("??????????????????.", e);
            returnMap.put("flag", "error");
            returnMap.put("msg", "?????????????????????");
        }
        return returnMap;
    }

    @Override
    public String addInstanceNoApprove(String userName, Map<String, Object> instanceData, String operateType) {
        String msg = "";
        validModule(instanceData);
        CmdbCollectApproval approval = handleAddApproval(userName, instanceData, operateType);
        approval.setApprovalUser(userName);
        msg = this.insert(userName, approval);
        return msg;
    }

    private SimpleModule validModule(Map<String, Object> instanceData) {
        if (!instanceData.containsKey("module_id")) {
            throw new RuntimeException("Params[module_id] is require.");
        }
        String moduleId = instanceData.get("module_id").toString();
        SimpleModule module = moduleService.getSimpleModuleDetail(moduleId);
        if (module == null) {
            throw new RuntimeException("can't find module record. module_id: " + moduleId);
        }
        return module;
    }

    @Override
    public String updateInstance(String id, String userName, Map<String, Object> instanceData, String operateType) {
        String msg = "";
        SimpleModule module = validModule(instanceData);
        // ???ID?????????????????????
        instanceData.put("id", id);
        List<CmdbCollectApproval> approvalList = handleUpdateApproval(userName, instanceData, operateType);
        if (approvalList.size() == 0) {
            msg = "?????????????????????????????????.";
        }
        // ????????????????????????????????????????????????????????????
        else if (module.getEnableApprove() != null && module.getEnableApprove() == 0) {
            // ????????????
            msg = this.update(userName, approvalList);
        } else {
            msg = this.updateToApproval(approvalList);
        }
        return msg;
    }

    private List<CmdbCollectApproval> handleUpdateApproval(String userName, Map<String, Object> instanceData, String operateType) {
        List<CmdbCollectApproval> approvals = new ArrayList<>();
        String moduleId = instanceData.get("module_id").toString();
        String instanceId = instanceData.get("id").toString();
        Map<String, Map<String, String>> columnInfo = moduleService.getModuleColumns(moduleId);
        Map<String, Object> instanceDataMap = Maps.newHashMap();
        for (Map.Entry<String, Object> entry : instanceData.entrySet()) {
            if (ImmutableList.of("module_id", "id").contains(entry.getKey())) {
                continue;
            }
            instanceDataMap.put(entry.getKey(), entry.getValue());
        }
        Map<String, Object> oldInstanceData = getInstanceDetail(moduleId, instanceId);
        oldInstanceData.put("module_id", moduleId);
        Map<String, Object> params = new HashMap<>();
        params.put("customerType", "approve");
        params.put("id", instanceId);
        for (String key : instanceDataMap.keySet()) {
            params.put(key, instanceDataMap.get(key));
        }
        String querySQL = moduleService.getModuleQuerySQL(moduleId);
        querySQL = "select * from (" + querySQL + ") res where res.id=#{id}";
        CmdbSqlManage cmdbSqlManage = new CmdbSqlManage(querySQL, Constants.INSTANCE_AUTH_MODULE, Constants.UN_NEED_AUTH);
        List<Map<String, Object>> returnList = jdbcHelper.getQueryList(cmdbSqlManage, null, null, null, params);
        if (returnList == null || returnList.size() == 0) {
            throw new RuntimeException("??????????????????????????????????????????");
        }
        Map<String, Object> newInstanceData = returnList.get(0);
        Module module = moduleService.getModuleDetail(moduleId);
        for (CmdbModuleCodeGroup group : module.getGroupList()) {
            for (CmdbCode code : group.getCodeList()) {
                // ???????????????????????????????????????????????????????????????????????????????????????
                if (!instanceDataMap.containsKey(code.getFiledCode())) {
                    continue;
                }
                // ????????????????????????????????????????????????
//                if (null != code.getUpdateReadOnly() && 1 == code.getUpdateReadOnly()) {
//                    continue;
//                }
                String oldValue = "";
                String curValue = "";
                if (columnInfo.containsKey(code.getFiledCode()) && "ref".equals(columnInfo.get(code.getFiledCode()).get("type"))) {
                    String field = columnInfo.get(code.getFiledCode()).get("ref_name");
                    oldValue = oldInstanceData.get(field) == null ? "" : oldInstanceData.get(field).toString();
                    curValue = newInstanceData.get(field) == null ? "" : newInstanceData.get(field).toString();
                } else {
                    oldValue = StringUtils.isNotEmpty(oldInstanceData.get(code.getFiledCode()))
                            ? oldInstanceData.get(code.getFiledCode()).toString()
                            : "";
                    curValue = StringUtils.isNotEmpty(instanceDataMap.get(code.getFiledCode()))
                            ? instanceDataMap.get(code.getFiledCode()).toString()
                            : "";
                }
                CmdbCollectApproval approval = new CmdbCollectApproval();
                if (instanceDataMap.containsKey(code.getFiledCode()) && !oldValue.equals(curValue)) {
                    approval.setModuleId(oldInstanceData.get("module_id").toString());
                    approval.setOwnerModuleId(code.getCodeSetting().getOwnerModuleId());
                    approval.setCodeId(code.getCodeId());
                    approval.setOperaterType(operateType);
                    approval.setApprovalType("update");
                    approval.setInstanceId(instanceId);
                    approval.setOldValue(oldValue.trim());
                    approval.setCurrValue(curValue.trim());
                    approval.setOperator(userName);
                    approval.setOperatorTime(new Date());
                    Map<String, Object> resourceMap = new HashMap<>();
                    resourceMap.put(code.getFiledCode(), newInstanceData.get(code.getFiledCode()));
                    approval.setResourceData(JSONObject.toJSONString(resourceMap));
                    approvals.add(approval);
                }
            }
        }
        return approvals;
    }

    private CmdbCollectApproval handleDeleteApproval(String userName, Map<String, Object> instanceData, String operateType) {
        List<CmdbCollectApproval> approvals = new ArrayList<>();
        String moduleId = instanceData.get("module_id").toString();
        String instanceId = instanceData.get("id").toString();
        Map<String, Object> instanceDetail = getInstanceDetail(moduleId, instanceId);
        Module module = moduleService.getModuleDetail(moduleId);
        Map<String, CmdbV3CodeBindSource> refCodes = new HashMap<>();
        Map<String, Object> oldValue = new HashMap<>();
        Map<String, Map<String, String>> columnInfo = moduleService.getModuleColumns(moduleId);
        for (CmdbModuleCodeGroup group : module.getGroupList()) {
            for (CmdbCode code : group.getCodeList()) {
                if (columnInfo.containsKey(code.getFiledCode()) && "ref".equals(columnInfo.get(code.getFiledCode()).get("type"))) {
                    oldValue.put(code.getFiledCode(), instanceDetail.get(columnInfo.get(code.getFiledCode()).get("ref_name")));
                } else {
                    oldValue.put(code.getFiledCode(), instanceDetail.get(code.getFiledCode()));
                }
            }
        }
        CmdbCollectApproval approval = new CmdbCollectApproval();
        approval.setModuleId(moduleId);
        approval.setOwnerModuleId(moduleId);
        approval.setOperaterType(operateType);
        approval.setInstanceId(instanceId);
        approval.setApprovalType("delete");
        approval.setOldValue(oldValue.toString());
        approval.setOperator(userName);
        approval.setApprovalUser(userName);
        approval.setOperatorTime(new Date());
        approvals.add(approval);
        return approval;
    }

    /**
     * ????????????
     *
     * @param approval
     *            ????????????
     * @return
     */
    @Override
    @Transactional(rollbackFor = { RuntimeException.class, Exception.class, SQLException.class })
    public String insert(String userName, CmdbCollectApproval approval) {
        Map<String, Object> instanceData = (Map<String, Object>) JSONObject.parse(approval.getResourceData());
        if (!instanceData.containsKey("module_id")) {
            throw new RuntimeException("Params[module_id] is require.");
        }
        String moduleId = instanceData.get("module_id").toString();
        // ??????UUID?????????id??????
        String instanceId = approval.getInstanceId();
        // ???????????????id??????????????????id
        Optional<Object> instance_id = Optional.ofNullable(instanceData.get("instance_id"));
        if (instance_id.isPresent()) {
            instanceId = instance_id.get().toString();
        }
        // ??????????????????????????????
        instanceData.put("insert_person", approval.getOperator());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        instanceData.put("insert_time", dateFormat.format(new Date()));
        // ??????????????????????????????????????????
        instanceData.put("update_person", approval.getOperator());
        instanceData.put("update_time", dateFormat.format(new Date()));
        instanceData.put("id", instanceId);
        // ????????????
        long startTime = System.currentTimeMillis();
        this.insertCiByNoValid(moduleId, instanceData);
        log.info(">>>>>> 3.1?????????????????? ??????:{}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        // ????????????????????????
        Map<String, Object> handleData = new HashMap<>();
        List<CmdbCollectApproval> approvals = new ArrayList<>();
        approval.setApprovalStatus(1);
        approvals.add(approval);
        handleData.put("username", userName);
        handleData.put("approvals", approvals);
        handleData.put("operateType", EventConst.EVENT_TYPE_DATA_INSERT);
        moduleEventService.handlerModuleDataEvent(moduleId, instanceId, null, handleData, EventConst.EVENT_TYPE_DATA_INSERT);
        log.info(">>>>>> 3.2???????????????????????? ??????:{}", System.currentTimeMillis() - startTime);
        return "??????????????????";
    }

    /**
     * ????????????, ???IP????????????????????????. ?????????????????????????????????insert??????. ???????????????????????????????????????????????????
     *
     * @param
     *
     */
    private void insertCiByNoValid(String moduleId, Map<String, Object> instanceData) {
        // ?????????????????????????????????????????????????????????????????????
        long startTime = System.currentTimeMillis();
        toCheckUnique(moduleId, instanceData);
        log.info(">>>>>> ??????????????? ??????:{}", System.currentTimeMillis() - startTime);
        String instanceId = instanceData.get("id").toString();
        // ???????????????id??????????????????id
        Optional<Object> instance_id = Optional.ofNullable(instanceData.get("instance_id"));
        if (instance_id.isPresent()) {
            instanceId = instance_id.get().toString();
        }
        startTime = System.currentTimeMillis();
        Map<String, Object> handelResult = handleModuleData(instanceId, moduleId, instanceData);
        log.info(">>>>>> ?????????????????? ??????:{}", System.currentTimeMillis() - startTime);
        Map<String, Map<String, Object>> moduleData = (Map<String, Map<String, Object>>) handelResult.get("moduleData");
        if (!moduleData.containsKey(moduleId)) {
            moduleData.put(moduleId, new HashMap<>());
        }
        List<CmdbInstanceIpManager> ipManagerList = (List<CmdbInstanceIpManager>) handelResult.get("ipManagerList");
        // ??????IP????????????
        if (ipManagerList.size() > 0) {
            ipManagerService.insertByBatch(instanceId, ipManagerList);
        }
        // ??????????????????
        startTime = System.currentTimeMillis();

        // ???????????????????????????
        DefaultTransactionDefinition transDefinition = new DefaultTransactionDefinition();
        // ???????????????????????????
        transDefinition.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRES_NEW);
        // ?????????????????????????????????????????????????????????????????????????????????????????????
        TransactionStatus transStatus = transactionManager.getTransaction(transDefinition);
        Map<String, CmdbV3ModuleCatalog> catalogMap = Maps.newHashMap();
        try {
            // ??????????????????
            for (String refModuleId : moduleData.keySet()) {
                // ?????????????????????ci???id
                moduleData.get(refModuleId).put("id", instanceId);
                CmdbV3ModuleCatalog catalog = catalogService.getByModuleId(refModuleId);
                if (catalog == null) {
                    throw new RuntimeException("?????????????????????Id[" + refModuleId + "]????????????");
                }
                catalogMap.put(refModuleId, catalog);
                // ?????????????????????
                if (moduleData.get(refModuleId).keySet().size() > 0) {
                    schemaService.insertCi(catalog.getCatalogCode(), moduleData.get(refModuleId));
                }
            }
            transactionManager.commit(transStatus);
        } catch (RuntimeException e) {
            transactionManager.rollback(transStatus);
            throw new RuntimeException(e);
        }

        // ?????????ES
        if (queryDbType != null && queryDbType.toLowerCase(Locale.ENGLISH).equals("es")) {
            for (Map.Entry<String, CmdbV3ModuleCatalog> entry : catalogMap.entrySet()) {
                String refModuleId = entry.getKey();
                CmdbV3ModuleCatalog catalog = entry.getValue();
                List<Map<String, Object>> esData = new ArrayList<>();
                esData.add(moduleData.get(refModuleId));
                SimpleModule refModule = moduleService.getSimpleModuleDetail(refModuleId);
                if (refModule == null) {
                    throw new RuntimeException("?????????????????????Id[" + refModuleId + "]??????");
                }
                cmdbESService.insert(esData, catalog.getCatalogCode(), refModule.getCode());
            }
        }
        log.info(">>>>>> ?????????????????? ??????:{}", System.currentTimeMillis() - startTime);

        // ????????????????????????????????????????????????????????????????????????.
        Object noSyncFlag = instanceData.get("noSyncFlag");
        if (noSyncFlag == null || BooleanUtils.toBoolean(noSyncFlag.toString()) == false) {
            // ????????????????????????????????????????????????????????????kafka
            cmdbModuleProducerService.saveEventLogAndSendMsg(CmdbOptType.OPT_ADD, moduleId, instanceId);
        }
    }

    @Override
    public List<CmdbInstanceIpManager> handleIpManagerList(String curValue, String codeId, String instanceId) {
        List<CmdbInstanceIpManager> ipManagerList = new ArrayList<>();
        String ipValue = curValue.replace("???", ",");
        // ???????????????
        CmdbInstanceIpManager deleteEntity = new CmdbInstanceIpManager();
        deleteEntity.setInstanceId(instanceId);
        deleteEntity.setCodeId(codeId);
        ipManagerService.delete(deleteEntity);
        if (StringUtils.isNotEmpty(ipValue)) {
            String[] ips = ipValue.split(",");
            for (String ip : ips) {
                if (StringUtils.isEmpty(ip.trim())) {
                    continue;
                }
                CmdbInstanceIpManager ipManager = new CmdbInstanceIpManager(instanceId, codeId, ip.trim(), null);
                ipManagerList.add(ipManager);
            }

        }
        return ipManagerList;
    }

    /**
     * ??????????????????
     */
    @Override
    public String update(String userName, List<CmdbCollectApproval> approvals) {
        if (approvals == null || approvals.size() == 0) {
            return "?????????????????????";
        }
        // ??????CI????????????
        Map<String, Map<String, Object>> moduleData = new HashMap<>();
        Map<String, String> tableMap = new HashMap<>();
        // ????????????????????????????????????????????????CI
        // ??????????????????CI
        long startTime = System.currentTimeMillis();
        String instanceId = approvals.get(0).getInstanceId();
        String moduleId = approvals.get(0).getModuleId();
        boolean isAloneModule = moduleService.isAloneModule(moduleId);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Module topModule = null;
        // ???????????????, ????????????
        if (!isAloneModule) {
            // ???????????????????????????????????????
            List<Module> moduleList = moduleService.getCurRefModule(moduleId);
            for (Module module : moduleList) {
                tableMap.put(module.getId(), module.getModuleCatalog().getCatalogCode());
                moduleData.put(module.getModuleCatalog().getCatalogCode(), new HashMap<>());
                // ?????????????????????
                if ("0".equals(module.getModuleCatalog().getParentCatalogId())) {
                    topModule = module;
                }
            }
            if (topModule == null) {
                throw new RuntimeException("????????????????????????.");
            }
        } else {
            topModule = moduleService.getModuleDetail(moduleId);
            if (topModule == null) {
                throw new RuntimeException("??????????????????moduleId[" + moduleId + "].");
            }
            tableMap.put(topModule.getId(), topModule.getModuleCatalog().getCatalogCode());
            moduleData.put(topModule.getModuleCatalog().getCatalogCode(), new HashMap<>());
        }
        // ?????????update_person/update_time ?????????????????????
        moduleData.get(topModule.getModuleCatalog().getCatalogCode()).put("update_person", approvals.get(0).getOperator());
        moduleData.get(topModule.getModuleCatalog().getCatalogCode()).put("update_time", dateFormat.format(new Date()));

        log.info(">>>>>> ??????CI ??????????????????????????? ?????????{}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        List<CmdbInstanceIpManager> ipManagerList = new ArrayList<>();
        // ??????????????????
        for (CmdbCollectApproval approval : approvals) {
            if (!approval.getInstanceId().equals(instanceId)) {
                throw new RuntimeException("????????????????????????????????????CI");
            }
            approval.setApprovalUser(userName);
            approval.setApprovalTime(new Date());
            approval.setApprovalStatus(1);
            // ??????id???????????????????????????????????????????????????
            Map<String, Object> instanceData = (Map<String, Object>) JSONObject.parse(approval.getResourceData());
            // moduleData.put(, new HashMap<>());
            // CmdbV3ModuleCatalog ownerCatalog = catalogService.getByModuleId(approval.getOwnerModuleId());
            //
            // String tableName = ownerCatalog.getCatalogCode();
            // if (!moduleData.containsKey(tableName)) {
            // moduleData.put(tableName, new HashMap<>());
            // }
            CmdbSimpleCode code = codeService.getSimpleCodeById(approval.getCodeId());
            CmdbControlType type = controlTypeService.getById(code.getControlTypeId());
            String currentValue = StringUtils.isNotEmpty(instanceData.get(code.getFiledCode()))
                    ? instanceData.get(code.getFiledCode()).toString()
                    : "";
            moduleData.get(tableMap.get(approval.getOwnerModuleId())).put(code.getFiledCode(), currentValue);
            // ???????????????ip???ip?????????
            if (Constants.CODE_CONTROL_TYPE_IP.equals(type.getControlCode())) {
                ipManagerList.addAll(handleIpManagerList(currentValue, code.getCodeId(), instanceId));
            }
            //
        }
        log.info(">>>>>> ??????CI ???????????????????????? ?????????{}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        // ????????????????????????CI
        for (String tableName : moduleData.keySet()) {
            // mapper.update(tableName, instanceId, moduleData.get(tableName));
            schemaService.updateCi(tableName, instanceId, moduleData.get(tableName));
        }
        log.info(">>>>>> ??????CI ????????? ?????????{}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        // ip????????????
        if (ipManagerList.size() > 0) {
            ipManagerService.insertByBatch(instanceId, ipManagerList);
        }
        if (queryDbType != null && queryDbType.toLowerCase(Locale.ENGLISH).equals("es")) {
            // ??????CI????????????ES??????
            cmdbESService.asyncRefresh(moduleId, instanceId);
        }
        // ????????????????????????
        Map<String, Object> handleData = new HashMap<>();
        handleData.put("username", userName);
        handleData.put("approvals", approvals);
        handleData.put("operateType", EventConst.EVENT_TYPE_DATA_UPDATE);
        moduleEventService.handlerModuleDataEvent(moduleId, instanceId, null, handleData, EventConst.EVENT_TYPE_DATA_UPDATE);
        log.info(">>>>>> ??????CI ???????????? ?????????{}", System.currentTimeMillis() - startTime);
        startTime = System.currentTimeMillis();
        // ????????????????????????????????????????????????????????????kafka
        cmdbModuleProducerService.saveEventLogAndSendMsg(CmdbOptType.OPT_MODIFY, moduleId, instanceId);
        log.info(">>>>>> ??????CI ???????????????kafka ?????????{}", System.currentTimeMillis() - startTime);
        return "??????????????????";
    }

    @Override
    public Map<String, Object> handleModuleData(String instanceId, String moduleId, Map<String, Object> instanceData) {

        Map<String, Object> handleResult = new HashMap<>();
        Map<String, Map<String, Object>> moduleData = new HashMap<>();
        Module module = moduleService.getModuleDetail(moduleId);
        for (Module refModule : module.getRefModules()) {
            moduleData.put(refModule.getId(), new HashMap<>());
        }
        List<CmdbInstanceIpManager> ipManagerList = new ArrayList<>();
        for (CmdbModuleCodeGroup group : module.getGroupList()) {
            for (CmdbCode code : group.getCodeList()) {
                if (StringUtils.isNotEmpty(instanceData.get(code.getFiledCode()))) {
                    String ownerModuleId = code.getCodeSetting().getOwnerModuleId();
                    String filedCode = code.getFiledCode();
                    String curValue = instanceData.get(code.getFiledCode()).toString();
                    validCode(code, curValue);
                    // ??????????????????
                    if (code.getControlType().getControlCode().equals(Constants.CODE_CONTROL_TYPE_IP)) {
                        ipManagerList.addAll(handleIpManagerList(curValue, code.getCodeId(), instanceId));
                    }
                    if (!moduleData.containsKey(ownerModuleId)) {
                        moduleData.put(ownerModuleId, new HashMap<>());
                    }
                    moduleData.get(ownerModuleId).put(filedCode, curValue);
                }
            }
        }

        handleResult.put("moduleData", moduleData);
        handleResult.put("ipManagerList", ipManagerList);
        return handleResult;
    }

    @Override
    public void updateInstance(Map<String, Object> instanceData) {
        log.debug("instanceData=={}", instanceData);
        Object instanceIdObj = instanceData.get("instance_id");
        Object moduleIdObj = instanceData.get("module_id");
        Preconditions.checkArgument(instanceIdObj != null, "instanceId ????????????!");
        Preconditions.checkArgument(moduleIdObj != null, "moduleId ????????????!");
        String instanceId = instanceIdObj.toString();
        String moduleId = moduleIdObj.toString();
        if (org.apache.commons.lang3.StringUtils.isBlank(instanceId)) {
            return;
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        instanceData.put("update_person", "??????");
        instanceData.put("update_time", dateFormat.format(new Date()));
        // ??????CI????????????
        Map<String, List<Map<String, String>>> moduleData = new HashMap<>();
        Module module = moduleService.getModuleDetail(moduleId);
        String ownerTable = module.getModuleCatalog().getCatalogCode();

        List<CmdbCode> ownerCodeList = codeService.getSelfCodeListByModuleId(module.getId());
        // ????????????????????????????????????
        if (!moduleData.containsKey(ownerTable) && CollectionUtils.isNotEmpty(ownerCodeList)) {
            moduleData.put(ownerTable, new ArrayList<>());
        }
        for (CmdbCode cmdbCode : ownerCodeList) {
            Object fieldValue = instanceData.get(cmdbCode.getFiledCode());
            Map<String, String> filedInfo = new HashMap<>();
            // ???????????????????????????null
            // if (fieldValue != null) {
            filedInfo.put("filedCode", cmdbCode.getFiledCode());
            if (fieldValue != null && org.apache.commons.lang3.StringUtils.isNotBlank(fieldValue.toString())) {
                filedInfo.put("value", fieldValue.toString());
            } else {
                filedInfo.put("value", null);
            }
            // log.debug("filedInfo=={}", filedInfo);
            moduleData.get(ownerTable).add(filedInfo);
            // }
        }

        List<Module> refModules = module.getRefModules();
        for (Module module1 : refModules) {
            String refTable = module1.getModuleCatalog().getCatalogCode();
            if (!moduleData.containsKey(refTable)) {
                moduleData.put(refTable, new ArrayList<>());
            }
            List<CmdbCode> codes = codeService.getSelfCodeListByModuleId(module1.getId());

            for (CmdbCode cmdbCode : codes) {
                Map<String, String> filedInfo1 = new HashMap<>();
                Object fieldValue = instanceData.get(cmdbCode.getFiledCode());
                // ???????????????????????????null
                // if (fieldValue != null) {
                filedInfo1.put("filedCode", cmdbCode.getFiledCode());
                if (fieldValue != null && org.apache.commons.lang3.StringUtils.isNotBlank(fieldValue.toString())) {
                    filedInfo1.put("value", fieldValue.toString());
                } else {
                    filedInfo1.put("value", null);
                }
                moduleData.get(refTable).add(filedInfo1);
                // if (fieldValue != null) {
                // filedInfo1.put("filedCode", cmdbCode.getFiledCode());
                // filedInfo1.put("value", fieldValue.toString());
                // moduleData.get(refTable).add(filedInfo1);
                // break;
                // }
            }

        }
        log.debug("????????????moduleData=={}", moduleData);
        // ????????????????????????CI
        for (String tableName : moduleData.keySet()) {
            mapper.update(tableName, instanceId, moduleData.get(tableName));
        }
        // TODO:??????IP??????????????????????????????ipmanager???
        // // ip????????????
        // if (ipManagerList.size() > 0) {
        // ipManagerService.insertByBatch(instanceId, ipManagerList);
        // }

        if (queryDbType != null && queryDbType.toLowerCase(Locale.ENGLISH).equals("es")) {
            // ??????CI????????????ES??????
            cmdbESService.asyncRefresh(moduleId, instanceId);
        }
    }

    /**
     * ????????????????????????
     *
     * @param approvalList
     *            ????????????
     * @return
     */
    @Transactional(rollbackFor = { RuntimeException.class, Exception.class, SQLException.class })
    public String updateToApproval(List<CmdbCollectApproval> approvalList) {
        List<CmdbCollectApproval> insertApprovals = new ArrayList<>();
        List<CmdbCollectApproval> autoPass = new ArrayList<>();
        List<CmdbCollectApproval> autoRefuse = new ArrayList<>();
        String instanceId = approvalList.get(0).getInstanceId();
        List<CmdbInstanceIpManager> ipManagerList = new ArrayList<>();
        for (CmdbCollectApproval approval : approvalList) {
            String codeId = approval.getCodeId();
            CmdbSimpleCode code = codeService.getSimpleCodeById(codeId);
            CmdbControlType controlType = controlTypeService.getById(code.getControlTypeId());
            CmdbV3CodeApprove approve = codeApproveService.getByCodeId(codeId);
            // ??????????????????????????????
            if (approve != null) {
                if ("????????????".equals(approve.getApproveType())) {
                    autoPass.add(approval);
                    // ??????ip????????????
                    if (controlType.getControlCode().equals(Constants.CODE_CONTROL_TYPE_IP)) {
                        ipManagerList.addAll(handleIpManagerList(approval.getCurrValue(), codeId, approval.getInstanceId()));
                    }
                } else if ("????????????".equals(approve.getApproveType())) {
                    autoRefuse.add(approval);
                }
            } else {
                insertApprovals.add(approval);
            }
        }
        if (ipManagerList.size() > 0) {
            ipManagerService.insertByBatch(instanceId, ipManagerList);
        }
        if (insertApprovals.size() > 0) {
            approvalService.insertByBatch(insertApprovals);
        }
        if (autoRefuse.size() > 0) {
        }
        if (autoPass.size() > 0) {
            this.update(autoPass.get(0).getOperator(), autoPass);
        }
        return "????????????????????????" + insertApprovals.size() + "???, ????????????" + autoPass.size() + "????????????" + autoRefuse.size() + "???";
    }

    @Override
    public Integer batchUpdateCount(String moduleId, Map<String, Object> batchUpdate) {
        Module module = moduleService.getModuleDetail(moduleId);
        if (module == null) {
            throw new RuntimeException("???????????????????????????");
        }
        String sql = startToQuery(module, batchUpdate, "count");
        // ???????????????????????????????????? ?????????????????????????????????, ?????????????????????????????????
        CmdbSqlManage cmdbSqlManage = new CmdbSqlManage(sql, Constants.INSTANCE_AUTH_MODULE, Constants.UN_NEED_AUTH);
        return jdbcHelper.getInt(cmdbSqlManage, null, null);
    }

    /**
     * ????????????????????????????????????CI???????????????
     */
    @Override
    public List<String> getBatchUpdateApprovals(String username, String moduleId, Map<String, Object> batchUpdate) {
        Module module = moduleService.getModuleDetail(moduleId);
        if (module == null) {
            throw new RuntimeException("???????????????????????????");
        }
        String sql = "select id from (" + startToQuery(module, batchUpdate, "list") + ") upRes";
        // ???????????????????????????????????? ?????????????????????????????????, ?????????????????????????????????
        CmdbSqlManage cmdbSqlManage = new CmdbSqlManage(sql, Constants.INSTANCE_AUTH_MODULE, Constants.UN_NEED_AUTH);
        List<String> queryRes = jdbcHelper.getQueryFiled(cmdbSqlManage, null, null, null, null);
        return queryRes;
    }

    private String startToQuery(Module module, Map<String, Object> batchUpdate, String type) {
        List<Map<String, String>> updateFileds = (List<Map<String, String>>) batchUpdate.get("update");
        List<Map<String, String>> querys = (List<Map<String, String>>) batchUpdate.get("querys");
        validUpdateRequest(querys, updateFileds);
        String condition = CmdbConst.getConditionSql(querys);
        String notEqualsCurrValue = getIfNull(updateFileds);
        // ????????????????????????sql
        String moduleSql = moduleService.getModuleQuerySQL(module.getId());
        if (StringUtils.isEmpty(moduleSql)) {
            throw new RuntimeException("?????????????????????sql");
        }
        return "select * from (" + moduleSql + ")res where 1=1 " + condition + notEqualsCurrValue;
    }

    private String getIfNull(List<Map<String, String>> updateFileds) {
        StringBuilder ifNull = new StringBuilder("and (");
        for (Map<String, String> filed : updateFileds) {
            ifNull.append(" IFNULL(").append(filed.get("field")).append(", '') != '").append(filed.get("value")).append("' OR ");
        }
        ifNull.deleteCharAt(ifNull.length() - 3);
        ifNull = new StringBuilder(ifNull.substring(0, ifNull.length() - 3));
        ifNull.append(")");
        return ifNull.toString();
    }

    private void validUpdateRequest(List<Map<String, String>> querys, List<Map<String, String>> update) {
        if (querys == null || querys.size() == 0) {
            throw new RuntimeException("????????????????????????");
        }
        for (Map<String, String> query : querys) {
            if (StringUtils.isEmpty(query.get("field"))) {
                throw new RuntimeException("????????????????????????????????????");
            }
            if (StringUtils.isEmpty(query.get("operator"))) {
                throw new RuntimeException("????????????????????????");
            }
        }
        if (update == null || update.size() == 0) {
            throw new RuntimeException("????????????????????????");
        }
        for (Map<String, String> up : update) {
            if (StringUtils.isEmpty(up.get("codeId"))) {
                throw new RuntimeException("????????????id????????????");
            }
        }
    }

    /**
     * ????????????
     * 
     * @param entity
     *            ????????????
     * @return
     */
    @Override
    public void delete(CmdbInstance entity) {
        mapper.delete(entity);
    }

    @Override
    public Result<Map<String, Object>> getInstanceList(Map<String, Object> params, String moduleType) {
        if (!params.containsKey("token")) {
            params.put("token", innerUserId);
        }
        if (!params.containsKey("condicationCode")) {
            params.put("condicationCode", "instance_list");
        }
        if (StringUtils.isNotEmpty(moduleType)) {
            params.put("moduleType", moduleType);
        }
        Map<String, Object> queryParams = condctService.parseQuery(params);
        queryParams.put("condicationCode4Tab", params.get("condicationCode"));
        queryParams.put("exportTabType", params.get("exportTabType"));
        if (null == queryParams.get("query_module_id")) {
            return new Result<>(0, new ArrayList<>());
        }
        if (queryDbType != null && queryDbType.toLowerCase(Locale.ENGLISH).equals("es")) {
            // ??????es??????
            log.info("Request es params -> {}", net.sf.json.JSONObject.fromObject(queryParams).toString());
            com.aspire.mirror.elasticsearch.api.dto.cmdb.Result result = cmdbESService.list(queryParams);
            return new Result<>(result.getCount(), result.getData());
        }
        long start = new Date().getTime();
        Result<Map<String, Object>> result = getInstanceList(queryParams);
        log.info("-------??????: {}", (new Date().getTime() - start));
        return result;
    }

    @Override
    public List<Map<String, Object>> getInstanceListData(Map<String, Object> params, String moduleType) {
        if (!params.containsKey("token")) {
            params.put("token", innerUserId);
        }
        if (!params.containsKey("condicationCode")) {
            params.put("condicationCode", "instance_list");
        }
        if (StringUtils.isNotEmpty(moduleType)) {
            params.put("moduleType", moduleType);
        }
        Map<String, Object> queryParams = condctService.parseQuery(params);
        queryParams.put("condicationCode4Tab", params.get("condicationCode"));
        queryParams.put("exportTabType", params.get("exportTabType"));
        if (null == queryParams.get("query_module_id")) {
            return new ArrayList<>();
        }
        if (queryDbType != null && queryDbType.toLowerCase(Locale.ENGLISH).equals("es")) {
            // ??????es??????
            log.info("Request es params -> {}", net.sf.json.JSONObject.fromObject(queryParams).toString());
            com.aspire.mirror.elasticsearch.api.dto.cmdb.Result result = cmdbESService.list(queryParams);
            return new ArrayList<>();
        }
        Object[] formatQuerys = this.formatStatementParams(queryParams);
        StringBuilder queryBuilder = new StringBuilder((String) formatQuerys[0]);
        Map<String, Object> statementParams = (Map<String, Object>) formatQuerys[1];
        StringBuilder sortBuilderSQL = new StringBuilder((String) formatQuerys[2]);
        Integer currentPage = null, pageSize = null;
        if (queryParams.containsKey("currentPage") && queryParams.get("currentPage") != null) {
            currentPage = (Integer) queryParams.get("currentPage");
        }
        if (queryParams.containsKey("pageSize") && queryParams.get("pageSize") != null) {
            pageSize = (Integer) queryParams.get("pageSize");
        }
        String moduleId = queryParams.get("query_module_id").toString();
        String querySQL = "";
        List<String> tabParamList = new ArrayList<>(statementParams.keySet());
        CmdbConfig cmdbConfig = cmdbConfigService.getConfigByCode("instance_tab_list");
        if (null != cmdbConfig && "instance_list".equals(queryParams.get("condicationCode4Tab").toString())) {
            String exportTabType = Constants.MODULE_TAB_LIST;
            if (null != queryParams.get("exportTabType")) {
                exportTabType = queryParams.get("exportTabType").toString();
            }
            querySQL = moduleService.getModuleQuerySQL4Tab(moduleId, tabParamList, exportTabType);
        } else {
            querySQL = moduleService.getModuleQuerySQL(moduleId);
        }
        // ????????????
        String orderBy = null;
        if (!("").equals(sortBuilderSQL.toString())) {
            orderBy = " order by " + sortBuilderSQL.toString();
        }
        String limitString = null;
        if (currentPage != null && pageSize != null) {
            limitString = " limit " + ((currentPage - 1) * pageSize) + ", " + pageSize;
        }
        // ?????????IP????????????????????????????????????
        CmdbSqlManage sqlManage = new CmdbSqlManage(querySQL, moduleId, Constants.INSTANCE_AUTH_MODULE, Constants.NEED_AUTH);
        CmdbV3ModuleCatalog catalog = catalogService.getByModuleId(moduleId);
        CmdbV3ModuleCatalog topCatalog = catalogService.getTopCatalog(catalog.getId());
        if (topCatalog !=null && Constants.MODULE_IP_REPOSITORY.equals(topCatalog.getCatalogCode())) {
            sqlManage = new CmdbSqlManage(querySQL, moduleId, Constants.INSTANCE_AUTH_MODULE, Constants.UN_NEED_AUTH);
        }
        return jdbcHelper.getQueryList(sqlManage, queryBuilder.toString(), orderBy, limitString, statementParams);
    }

    /**
     * ????????????????????????
     * 
     * @return
     */
    @Override
    public String getDownloadType() {
        List<ConfigDict> configDictList = configDictService.selectDictsByType("export_type", null, null, null);
        // ??????ftp??????
        String ftpType = "ftp";
        if (configDictList != null && configDictList.size() > 0) {
            ftpType = configDictList.get(0).getValue();
        }
        return ftpType;
    }

    private void assessKey(Map<String, Object> map, String key, String msg) {
        if (!map.containsKey(key)) {
            throw new RuntimeException(msg);
        }
    }

    /**
     * ??????????????????
     * 
     * @param queryParams
     * @return
     */
    private Object[] formatStatementParams(Map<String, Object> queryParams) {
        List<Map<String, Object>> paramsList = (List<Map<String, Object>>) queryParams.get("params");
        StringBuilder queryBuilder = new StringBuilder();
        Map<String, Object> statementParams = new LinkedMap();
        for (Map<String, Object> params : paramsList) {
            this.assessKey(params, "filed",
                    "Format params error. querySetting.params every member must has filed???operator???value properties.");
            this.assessKey(params, "operator",
                    "Format params error. querySetting.params every member must has filed???operator???value properties.");
            this.assessKey(params, "value",
                    "Format params error. querySetting.params every member must has filed???operator???value properties.");
            String operator = params.get("operator").toString().trim();
            String filed = params.get("filed").toString().trim();
            Object value = params.get("value");
            // ?????????????????????IP??????????????????, ????????????IP??????????????????module sql????????????, ????????????????????????where??????
            if (("cmdb_instance_ip_manager_ip").equals(filed.toLowerCase(Locale.ENGLISH))) {
                statementParams.put(filed, value);
                continue;
            }
            if (StringUtils.isNotEmpty(value)) {
                switch (operator.toLowerCase(Locale.ENGLISH)) {
                    case "like":
                        queryBuilder.append(jdbcHelper.likeSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case "<":
                    case "<=":
                        queryBuilder.append(jdbcHelper.lteSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case ">":
                    case ">=":
                        queryBuilder.append(jdbcHelper.gteSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case "in":
                        queryBuilder.append(jdbcHelper.inSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case "=":
                        queryBuilder.append(jdbcHelper.eqSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case "not in":
                        queryBuilder.append(jdbcHelper.notInSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case "between":
                        List<String> valueList = (List<String>) value;
                        queryBuilder.append(jdbcHelper.betweenSql(filed, true));
                        statementParams.put(filed, value);
                        statementParams.put(filed + "_start", valueList.get(0));
                        statementParams.put(filed + "_end", valueList.get(1));
                        break;
                    case "contain":
                        Object containFiled = params.get("containFiled");
                        if (null != containFiled) {
                            String s = containFiled.toString();
                            String[] split = s.split(",");
                            queryBuilder.append(jdbcHelper.containSql(filed,split, true));
                            statementParams.put(filed, value);
                        } else {
                            queryBuilder.append(jdbcHelper.likeSql(filed, true));
                            statementParams.put(filed, value);
                        }
                        break;
                    default:
                        throw new RuntimeException("Don't support operator type [" + operator + "]");
                }
            }
        }
        // ????????????
        StringBuilder sortBuilderSQL = new StringBuilder();
        if (queryParams.containsKey("sort")) {
            List<Map<String, Object>> sortList = (List<Map<String, Object>>) queryParams.get("sort");
            for (Map<String, Object> sortMap : sortList) {
                if (sortMap.get("filed") != null) {
                    String sortFiled = sortMap.get("filed").toString();
                    if (sortMap.get("type") != null) {
                        if (!("").equals(sortBuilderSQL.toString())) {
                            sortBuilderSQL.append(",");
                        }
                        String sortType = sortMap.get("type").toString();
                        // ????????????????????????????????? modify by fanwenhui 20200730
                        boolean index = checkInstanceListSortField(queryParams.get("index").toString(), sortFiled);
                        sortBuilderSQL.append(" IFNULL(").append(sortFiled).append(",'') = '' , ");
                        if (index) {
                            sortBuilderSQL.append(" INET_ATON(").append(sortFiled).append(") ");
                        } else {
                            sortBuilderSQL.append(sortFiled).append(" ");
                        }
                        sortBuilderSQL.append(sortType).append(" ");
                    }
                }
            }
        }
        return new Object[] { queryBuilder.toString(), statementParams, sortBuilderSQL.toString() };
    }

    private Result<Map<String, Object>> getInstanceList(Map<String, Object> queryParams) {
        Object[] formatQuerys = this.formatStatementParams(queryParams);
        StringBuilder queryBuilder = new StringBuilder((String) formatQuerys[0]);
        Map<String, Object> statementParams = (Map<String, Object>) formatQuerys[1];
        StringBuilder sortBuilderSQL = new StringBuilder((String) formatQuerys[2]);
        Integer currentPage = null, pageSize = null;
        if (queryParams.containsKey("currentPage") && queryParams.get("currentPage") != null) {
            currentPage = (Integer) queryParams.get("currentPage");
        }
        if (queryParams.containsKey("pageSize") && queryParams.get("pageSize") != null) {
            pageSize = (Integer) queryParams.get("pageSize");
        }
        //???????????????moduleId
        String moduleId = queryParams.get("query_module_id").toString();
        String querySQL = "";
        List<String> tabParamList = new ArrayList<>(statementParams.keySet());
        CmdbConfig cmdbConfig = cmdbConfigService.getConfigByCode("instance_tab_list");
        if (null != cmdbConfig && "instance_list".equals(queryParams.get("condicationCode4Tab").toString())) {
            String exportTabType = Constants.MODULE_TAB_LIST;
            if (null != queryParams.get("exportTabType")) {
                exportTabType = queryParams.get("exportTabType").toString();
            }
            querySQL = moduleService.getModuleQuerySQL4Tab(moduleId, tabParamList, exportTabType);
        } else {
            querySQL = moduleService.getModuleQuerySQL(moduleId);
        }
        // ????????????
        String orderBy = null;
        if (!("").equals(sortBuilderSQL.toString())) {
            orderBy = " order by " + sortBuilderSQL.toString();
        }
        String limitString = null;
        if (currentPage != null && pageSize != null) {
            limitString = " limit " + ((currentPage - 1) * pageSize) + ", " + pageSize;
        }
        // ?????????IP????????????????????????????????????
        CmdbSqlManage sqlManage = new CmdbSqlManage(querySQL, moduleId, Constants.INSTANCE_AUTH_MODULE, Constants.NEED_AUTH);
        CmdbV3ModuleCatalog catalog = catalogService.getByModuleId(moduleId);
        CmdbV3ModuleCatalog topCatalog = catalogService.getTopCatalog(catalog.getId());
        if (topCatalog !=null && Constants.MODULE_IP_REPOSITORY.equals(topCatalog.getCatalogCode())) {
            sqlManage = new CmdbSqlManage(querySQL, moduleId, Constants.INSTANCE_AUTH_MODULE, Constants.UN_NEED_AUTH);
        }
        Result<Map<String, Object>> resultPage = new Result<>();
        List<Map<String, Object>> list = jdbcHelper.getQueryList(sqlManage, queryBuilder.toString(), orderBy, limitString,
                statementParams);
        String queryCountSql = "";
        if (null != cmdbConfig && "instance_list".equals(queryParams.get("condicationCode4Tab").toString())) {
            queryCountSql = moduleService.getModuleQueryCountSQL4Tab(moduleId, tabParamList);
        } else {
            queryCountSql = moduleService.getModuleQueryCountSQL(moduleId);
        }
        CmdbSqlManage countManage = new CmdbSqlManage(queryCountSql, moduleId, Constants.INSTANCE_AUTH_MODULE, Constants.NEED_AUTH);
        Integer count = jdbcHelper.getInt(countManage, queryBuilder.toString(), statementParams);
        resultPage.setData(list);
        resultPage.setTotalSize(count);
        resultPage.setColumns(moduleService.getModuleColumns(moduleId));
        return resultPage;
    }

    @Override
    public List<CmdbInstance> getInstanceByIp(Map<String, Object> param) {
        return mapper.getInstanceByIp(param);
    }

    @Override
    @Transactional(rollbackFor = { RuntimeException.class, Exception.class, SQLException.class })
    public String deleteInstance(String userName, List<Map<String, Object>> instanceList, String operateType) {
        String msg = "";
        Integer deleteCount = 0;
        Integer approvalCount = 0;
        for (Map<String, Object> instanceData : instanceList) {
            SimpleModule module = validModule(instanceData);
            CmdbCollectApproval approval = handleDeleteApproval(userName, instanceData, operateType);
            // ????????????????????????????????????????????????????????????
            if (module.getEnableApprove() != null && module.getEnableApprove() == 0) {
                // ????????????
                this.delete(userName, approval);
                deleteCount++;
            } else {
                List<CmdbCollectApproval> approvals = new ArrayList<>();
                approvals.add(approval);
                approvalService.insertByBatch(approvals);
                approvalCount++;
            }
        }
        if (deleteCount > 0 && approvalCount > 0) {
            return "?????????????????????" + deleteCount + ",??????????????????" + approvalCount + "???";
        } else if (deleteCount > 0) {
            return "?????????????????????" + deleteCount + "???";
        } else {
            return "??????????????????" + approvalCount + "???";
        }
    }

    /**
     * ??????CI????????????,?????????????????????ci??????
     * 
     * @param userName
     *            ????????????
     * @param approval
     *            ??????????????????
     */
    @Override
    public void delete(String userName, CmdbCollectApproval approval) {
        String instanceId = approval.getInstanceId();
        String moduleId = approval.getModuleId();
        Module module = moduleService.getModuleDetail(moduleId);
        CmdbV3ModuleCatalog catalog = catalogService.getByModuleId(moduleId);
        List<String> deleteTableName = new ArrayList<>();
        deleteTableName.add(catalog.getCatalogCode());
        // ??????????????????
        boolean isAloneModule = moduleService.isAloneModule(moduleId);
        CmdbV3ModuleCatalog topCatalog = null;
        if (!isAloneModule) {
            topCatalog = catalogService.getTopCatalog(catalog.getId());
            List<Module> refModules = module.getRefModules();
            for (Module m : refModules) {
                CmdbV3ModuleCatalog refCatalog = catalogService.getByModuleId(m.getId());
                deleteTableName.add(refCatalog.getCatalogCode());
            }
        }
        Map<String, Object> instanceDetail = getInstanceDetail(moduleId, instanceId);
        // ??????????????????
        for (String tableName : deleteTableName) {
            Map<String, Object> instanceData = new HashMap<>();
            if (isAloneModule || tableName.equals(topCatalog.getCatalogCode())) {
                instanceData.put("update_person", userName);
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                instanceData.put("update_time", dateFormat.format(new Date()));
            }
            instanceData.put("is_delete", 1);
            schemaService.deleteCi(tableName, approval.getInstanceId(), instanceData);
        }
        // ???????????????????????????
        cmdbModuleProducerService.saveEventLogAndSendMsg(CmdbOptType.OPT_DEL, moduleId, approval.getInstanceId());
        // ????????????????????????
        Map<String, Object> handleData = new HashMap<>();
        List<CmdbCollectApproval> approvals = new ArrayList<>();
        approval.setApprovalStatus(1);
        approvals.add(approval);
        handleData.put("username", userName);
        handleData.put("approvals", approvals);
        handleData.put("instanceDetail", instanceDetail);
        handleData.put("operateType", EventConst.EVENT_TYPE_DATA_DELETE);
        moduleEventService.handlerModuleDataEvent(moduleId, approval.getInstanceId(), null, handleData,
                EventConst.EVENT_TYPE_DATA_DELETE);
    }

    @Override
    public void deletePhysic(CmdbCollectApproval approval) {
        String moduleId = approval.getModuleId();
        Module module = moduleService.getModuleDetail(moduleId);
        CmdbV3ModuleCatalog catalog = catalogService.getByModuleId(moduleId);
        // CmdbV3ModuleCatalog topCatalog = catalogService.getTopCatalog(catalog.getId());
        List<String> deleteTableName = new ArrayList<>();
        // ??????????????????
        deleteTableName.add(catalog.getCatalogCode());
        List<Module> refModules = module.getRefModules();
        for (Module m : refModules) {
            CmdbV3ModuleCatalog refCatalog = catalogService.getByModuleId(m.getId());
            deleteTableName.add(refCatalog.getCatalogCode());
        }
        // ??????????????????
        for (String tableName : deleteTableName) {
            schemaService.delete(tableName, approval.getInstanceId());
        }
    }

    @Override
    public List<Map> getIdcTree() {
        // ????????? -> ????????????
        List<CmdbIdcManager> idcManagerList = idcManagerService.list();
        final List<Map> list = new LinkedList<>();
        idcManagerList.stream().forEach((idc) -> {
            Map<String, Object> idcMap = new HashMap<>();
            idcMap.put("uuid", idc.getId());
            idcMap.put("name", idc.getIdcName());
            idcMap.put("type", "idcType");
            // ??????ROOM
            CmdbRoomManager queryRoom = new CmdbRoomManager();
            queryRoom.setIdcId(idc.getId());
            List<CmdbRoomManager> roomList = roomManagerService.listByEntity(queryRoom);
            List<Map> roomMapList = new LinkedList<>();
            if (roomList != null && roomList.size() > 0) {
                roomList.stream().forEach((room) -> {
                    Map<String, Object> roomMap = new HashMap<>();
                    roomMap.put("uuid", room.getId());
                    roomMap.put("name", room.getRoomName());
                    roomMap.put("type", "room");
                    roomMap.put("subList", new ArrayList<>());
                    roomMapList.add(roomMap);
                });
            }
            idcMap.put("subList", roomMapList);
            list.add(idcMap);
        });
        return list;
    }

    @Override
    public List<Map> getDeviceClassTree() {
        final List<Map> list = new LinkedList<>();
        List<ConfigDict> deviceClassList = configDictService.selectDictsByType("device_class", null, null, null);
        if (deviceClassList != null && deviceClassList.size() > 0) {
            deviceClassList.stream().forEach((deviceClass) -> {
                Map<String, Object> deviceMap = new HashMap<>();
                deviceMap.put("uuid", deviceClass.getId());
                deviceMap.put("name", deviceClass.getName());
                deviceMap.put("type", "device_class");
                List<ConfigDict> deviceTypeList = configDictService.selectDictsByType("device_type",
                        String.valueOf(deviceClass.getId()), deviceClass.getValue(), "device_class");
                List<Map> typeMapList = new LinkedList<>();
                if (deviceTypeList != null && deviceTypeList.size() > 0) {
                    deviceTypeList.stream().forEach((type) -> {
                        Map<String, Object> typeMap = new HashMap<>();
                        typeMap.put("uuid", type.getId());
                        typeMap.put("name", type.getName());
                        typeMap.put("type", "device_type");
                        typeMap.put("subList", new ArrayList<>());
                        typeMapList.add(typeMap);
                    });
                }
                deviceMap.put("subList", typeMapList);
                list.add(deviceMap);
            });
        }
        return list;
    }

    @Override
    public List<String> getDepartmentsByIDC(String idcType) {
        return mapper.getDepartmentsByIDC(idcType);
    }

    @Override
    public List<Map<String, String>> getIdcByIds(String ids) {
        String[] idcIds = ids.split(",");
        List<Map<String, String>> idcList = new LinkedList<>();
        for (String id : idcIds) {
            Map<String, String> idcMap = mapper.getIdcById(id);
            if (idcMap != null && idcMap.size() > 0) {
                idcList.add(idcMap);
            }
        }
        return idcList;
    }

    @Override
    public List<Map<String, String>> getPodByIds(String ids) {
        String[] podIds = ids.split(",");
        List<Map<String, String>> podList = new LinkedList<>();
        for (String id : podIds) {
            Map<String, String> podMap = mapper.getPodById(id);
            if (podMap != null && podMap.size() > 0) {
                podList.add(podMap);
            }
        }
        return podList;
    }

    @Override
    public List<Map<String, String>> getRoomByIds(String ids) {
        String[] roomIds = ids.split(",");
        List<Map<String, String>> roomList = new LinkedList<>();
        for (String id : roomIds) {
            Map<String, String> roomMap = mapper.getRoomById(id);
            if (roomMap != null && roomMap.size() > 0) {
                roomList.add(roomMap);
            }
        }
        return roomList;
    }

    @Override
    public List<CmdbDeviceTypeCount> queryServiceCount(String bizSystem) {
        List<String> bizSystemList = Lists.newArrayList();
        if (!StringUtils.isEmpty(bizSystem)) {
            String[] bizSystemArray = bizSystem.split(",");
            bizSystemList = Arrays.asList(bizSystemArray);
        }
        return mapper.queryServiceCount(bizSystemList);
    }

    @Override
    public List<CmdbDeviceTypeCount> queryServiceCountForKG() {
        return mapper.queryServiceCountForKG();
    }

    @Override
    public List<Map<String, Object>> getNetworkAndSafetyDeivce(CmdbQueryInstance cmdbQueryInstance) {
        return mapper.getNetworkAndSafetyDeivce(cmdbQueryInstance);
    }

    @Override
    public List<Map<String, String>> getProjectNameByIdcType(String idcType) {
        return mapper.getProjectNameByIdcType(idcType);
    }

    @Override
    public List<CmdbDeviceTypeByConditonCount> queryDeviceCountByIdctype(String idcType, String deviceType, String startTime,
            String endTime) {
        return mapper.queryDeviceCountByIdctype(idcType, deviceType, startTime, endTime);
    }

    @Override
    public List<CmdbDeviceTypeByConditonCount> queryDeviceCountByBizsystem(String bizSystem, String idcType, String deviceType,
            String startTime, String endTime, String sourceType) {
        List<String> bizSystemList = Lists.newArrayList();
        if (!StringUtils.isEmpty(bizSystem)) {
            String[] bizSystemArray = bizSystem.split(",");
            bizSystemList = Arrays.asList(bizSystemArray);
        }
        return mapper.queryDeviceCountByBizsystem(bizSystemList, idcType, deviceType, startTime, endTime, sourceType);
    }

    @Override
    public Map<String, Map<String, Map<String, Integer>>> filterEmptyCiItem(String ciItem) {
        // ????????????????????????
        Map<String, Map<String, Map<String, Integer>>> returnMap = new LinkedHashMap<>();
        List<ConfigDict> idcList = configDictService.selectDictsByType("idcType", null, null, null);
        for (ConfigDict configDict : idcList) {
            if (!returnMap.containsKey(configDict.getName())) {
                returnMap.put(configDict.getName(), new HashMap<>());
            }
            Map<String, Map<String, Integer>> deviceMap = returnMap.get(configDict.getName());
            // ??????????????????
            List<Module> moduleList = moduleService.getModuleTree(null, null); // ?????????
            for (Module parentModule : moduleList) {
                if (parentModule.getChildModules() != null && parentModule.getChildModules().size() > 0) {
                    // ????????? -> ???????????? -> ??????????????????
                    // ???????????????
                    for (Module childModule : parentModule.getChildModules()) {
                        if (!deviceMap.containsKey(childModule.getName())) {
                            deviceMap.put(childModule.getName(), new HashMap<>());
                        }
                        Map<String, Integer> filedMap = deviceMap.get(childModule.getName());
                        // ?????????????????????????????????
                        List<CmdbCode> codeList = codeService.getCodeListByModuleId(childModule.getId());
                        for (CmdbCode cmdbCode : codeList) {
                            for (String ciCode : ciItem.split(",")) {
                                if (ciCode.equals(cmdbCode.getFiledName())) {
                                    // ??????????????????????????????
                                    int count = mapper.queryEmptyCiItemCount("cmdb_instance_" + childModule.getCode(),
                                            cmdbCode.getFiledCode(), configDict.getName(), childModule.getName());
                                    if (!filedMap.containsKey(ciCode)) {
                                        filedMap.put(ciCode, count);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return returnMap;
    }

    @Override
    public Map<String, Object> getInstanceByIPMI(String ipmiIp, String idcType) {
        Map<String, Object> params = new HashMap<>();
        params.put("ipmi_ip", ipmiIp);
        Map<String, Object> detail = queryInstanceDetail(params, null);
        return detail;
    }

    @Override
    public void updateAllPool(List<String> monitorPools, String flag) {
        mapper.updateAllPool(monitorPools, flag);
    }

    @Override
    public void updateZbxMonitorStatus(String instanceId, String flag) {
        mapper.updateZbxMonitorStatus(instanceId, flag);
    }

    @Override
    public void updateProMonitorStatus(String instanceId, String flag) {
        mapper.updateProMonitorStatus(instanceId, flag);
    }

    @Override
    public List<Map<String, Object>> getInstanceBaseInfo(Map<String, Object> param) {
        return mapper.getInstanceBaseInfo(param);
    }

    @Override
    public Result<Map<String, Object>> getAllIPInstance(Map<String, Object> params) {
        Module module = moduleService.getDefaultModule("host");
        String querySql = moduleService.getModuleQuerySQL(module.getId());
        querySql = querySql.replace(Constants.INNER_LIMIT_STRING, "");
        // ??????SQL
        String finalSql = "select m.ip cmdb_instance_manager_ip, c.* from cmdb_instance_ip_manager m ";
        finalSql += "inner join (" + querySql + ") c on m.instance_id = c.id ";
        finalSql += "where IFNULL(m.ip, '') != '' " + Constants.INNER_LIMIT_STRING;
        String whereString = "";
        Map<String, Object> queryParams = new HashMap<>();
        if (params.containsKey("idcType") && StringUtils.isNotEmpty(params.get("idcType"))) {
            whereString += jdbcHelper.eqSql("idcType", true);
            queryParams.put("idcType", params.get("idcType"));
        }
        if (params.containsKey("update_time") && StringUtils.isNotEmpty(params.get("update_time"))) {
            whereString += jdbcHelper.gteSql("update_time", true);
            queryParams.put("update_time", params.get("update_time"));
        }
        Integer pageSize = 10000, currentPage = 1;
        try {
            pageSize = Integer.parseInt(params.get("pageSize").toString());
            currentPage = Integer.parseInt(params.get("currentPage").toString());
        } catch (Exception e) {
            log.error("Get paged error, use default size instanceof.");
        }
        String limitString = " limit " + ((currentPage - 1) * pageSize) + ", " + pageSize;
        long start1 = new Date().getTime();
        CmdbSqlManage sqlManage = new CmdbSqlManage(finalSql, Constants.INSTANCE_AUTH_MODULE, Constants.UN_NEED_AUTH);
        List<Map<String, Object>> dataList = jdbcHelper.getQueryList(sqlManage, whereString, null, limitString, queryParams);
        log.info("??????1===>?????? {} ms", (new Date().getTime() - start1));
        // ??????SQL
        String finalCountSql = "select 1 from cmdb_instance_ip_manager m ";
        finalCountSql += "inner join cmdb_instance c on m.instance_id = c.id ";
        finalCountSql += "where IFNULL(m.ip, '') != '' and c.is_delete='0'";
        start1 = new Date().getTime();
        CmdbSqlManage countSqlManager = new CmdbSqlManage(finalCountSql, Constants.INSTANCE_AUTH_MODULE, Constants.UN_NEED_AUTH);
        int count = jdbcHelper.getInt(countSqlManager, null, queryParams);
        log.info("??????2===>?????? {} ms", (new Date().getTime() - start1));

        Result<Map<String, Object>> pagedResult = new Result();
        pagedResult.setData(dataList);
        pagedResult.setTotalSize(count);
        pagedResult.setColumns(moduleService.getModuleColumns(module.getId()));
        return pagedResult;
    }

    /**
     * ?????????????????? ???????????????????????? ?????????????????????API????????????
     * 
     * @param params
     *            ????????????
     * @return
     */
    @Override
    public Result<Map<String, Object>> getInstanceListForCommon(Map<String, Object> params) {
        Map<String, Object> queryParams = condctService.parseQuery(params);
        if (queryDbType != null && queryDbType.toLowerCase(Locale.ENGLISH).equals("es")) {
            // ??????es??????
            log.info("Request es params -> {}", net.sf.json.JSONObject.fromObject(queryParams).toString());
            com.aspire.mirror.elasticsearch.api.dto.cmdb.Result result = cmdbESService.list(queryParams);
            return new Result<>(result.getCount(), result.getData());
        }
        return getInstanceList(queryParams);
    }

    @Override
    public List<Map<String, Object>> deviceCountByDeviceClass(String deviceClass) {
        return mapper.deviceCountByDeviceClass(deviceClass);
    }

    @Override
    public Map<String, Object> deviceCountByDeviceType(String deviceClass, String deviceType) {
        return mapper.deviceCountByDeviceType(deviceClass, deviceType);
    }

    @Override
    public Object getBlockSize() {
        Map<String, Object> blockSize = mapper.getBlockSize();
        return null != blockSize && null != blockSize.get("block_size") ? blockSize.get("block_size") : 0;
    }

    @Override
    public void countSegmentIp(String segmentTableName, String segmentAddress, String ipTableName, String ipSegment) {
        mapper.countSegmentIp(segmentTableName, segmentAddress, ipTableName, ipSegment);
    }

    @Override
    public void countSegmentIp4Segment(String segmentTableName, String segmentAddress, String ipTableName, String ipSegment,
            String segmentAddressValue) {
        mapper.countSegmentIp4Segment(segmentTableName, segmentAddress, ipTableName, ipSegment, segmentAddressValue);
    }

    @Override
    public void syncIpBussiness(String ipCode, String segmentTableName, String segmentAddress, String ipTableName,
            String ipSegment) {
        mapper.syncIpBussiness(ipCode, segmentTableName, segmentAddress, ipTableName, ipSegment);
    }

    @Override
    public Integer listV3Count(Map<String, Object> params, String moduleType) {
        if (!params.containsKey("token")) {
            params.put("token", innerUserId);
        }
        if (!params.containsKey("condicationCode")) {
            params.put("condicationCode", "instance_list");
        }
        String moduleId = "";
        if (!params.containsKey("module_id") && !params.containsKey("device_type")) {
            Module module = moduleService.getDefaultModule(moduleType);
            params.put("module_id", module.getId());
            moduleId = module.getId();
        }
        Map<String, Object> queryParams = condctService.parseQuery(params);
        if (StringUtils.isEmpty(moduleId)) {
            moduleId = queryParams.get("query_module_id").toString();
        }
        queryParams.put("module_id", moduleId);
        if (queryDbType != null && queryDbType.toLowerCase(Locale.ENGLISH).equals("es")) {
            // ??????es??????
            log.info("Request es params -> {}", net.sf.json.JSONObject.fromObject(queryParams).toString());
            com.aspire.mirror.elasticsearch.api.dto.cmdb.Result result = cmdbESService.list(queryParams);
            return 0;
        }
        return getInstanceCount(queryParams);
    }

    private Integer getInstanceCount(Map<String, Object> queryParams) {
        Object[] formatQuerys = this.formatStatementParams(queryParams);
        StringBuilder queryBuilder = new StringBuilder((String) formatQuerys[0]);
        Map<String, Object> statementParams = (Map<String, Object>) formatQuerys[1];
        String moduleId = queryParams.get("query_module_id").toString();
        List<String> tabParamList = new ArrayList<>(statementParams.keySet());
        CmdbConfig cmdbConfig = cmdbConfigService.getConfigByCode("instance_tab_list");
        String queryCountSql = "";
        if (null != cmdbConfig && queryParams.containsKey("condicationCode4Tab")
                && "instance_list".equals(queryParams.get("condicationCode4Tab").toString())) {
            queryCountSql = moduleService.getModuleQueryCountSQL4Tab(moduleId, tabParamList);
        } else {
            queryCountSql = moduleService.getModuleQueryCountSQL(moduleId);
        }
        CmdbSqlManage countManage = new CmdbSqlManage(queryCountSql, moduleId, Constants.INSTANCE_AUTH_MODULE, Constants.NEED_AUTH);
        // ?????????IP????????????????????????????????????
        CmdbV3ModuleCatalog catalog = catalogService.getByModuleId(moduleId);
        CmdbV3ModuleCatalog topCatalog = catalogService.getTopCatalog(catalog.getId());
        if (Constants.MODULE_IP_REPOSITORY.equals(topCatalog.getCatalogCode())) {
            countManage = new CmdbSqlManage(queryCountSql, moduleId, Constants.INSTANCE_AUTH_MODULE, Constants.UN_NEED_AUTH);
        }
        Integer count = jdbcHelper.getInt(countManage, queryBuilder.toString(), statementParams);
        return count;
    }

    private Integer getInstanceList4Count(Map<String, Object> queryParams) {
        List<Map<String, Object>> paramsList = (List<Map<String, Object>>) queryParams.get("params");
        StringBuilder queryBuilder = new StringBuilder();
        Map<String, Object> statementParams = new LinkedMap();
        for (Map<String, Object> params : paramsList) {
            this.assessKey(params, "filed",
                    "Format params error. querySetting.params every member must has filed???operator???value properties.");
            this.assessKey(params, "operator",
                    "Format params error. querySetting.params every member must has filed???operator???value properties.");
            this.assessKey(params, "value",
                    "Format params error. querySetting.params every member must has filed???operator???value properties.");
            String operator = params.get("operator").toString().trim();
            String filed = params.get("filed").toString().trim();
            Object value = params.get("value");
            if (StringUtils.isNotEmpty(value)) {
                switch (operator.toLowerCase(Locale.ENGLISH)) {
                    case "like":
                        queryBuilder.append(jdbcHelper.likeSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case "<":
                    case "<=":
                        queryBuilder.append(jdbcHelper.lteSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case ">":
                    case ">=":
                        queryBuilder.append(jdbcHelper.gteSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case "in":
                        queryBuilder.append(jdbcHelper.inSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case "=":
                        queryBuilder.append(jdbcHelper.eqSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case "not in":
                        queryBuilder.append(jdbcHelper.notInSql(filed, true));
                        statementParams.put(filed, value);
                        break;
                    case "between":
                        List<String> valueList = (List<String>) value;
                        queryBuilder.append(jdbcHelper.betweenSql(filed, true));
                        statementParams.put(filed, value);
                        statementParams.put(filed + "_start", valueList.get(0));
                        statementParams.put(filed + "_end", valueList.get(1));
                        break;
                    default:
                        throw new RuntimeException("Don't support operator type [" + operator + "]");
                }
            }
        }
        // ????????????
        StringBuilder sortBuilderSQL = new StringBuilder();
        if (queryParams.containsKey("sort")) {
            List<Map<String, Object>> sortList = (List<Map<String, Object>>) queryParams.get("sort");
            for (Map<String, Object> sortMap : sortList) {
                if (sortMap.get("filed") != null) {
                    String sortFiled = sortMap.get("filed").toString();
                    if (sortMap.get("type") != null) {
                        if (!("").equals(sortBuilderSQL.toString())) {
                            sortBuilderSQL.append(",");
                        }
                        String sortType = sortMap.get("type").toString();
                        // ????????????????????????????????? modify by fanwenhui 20200730
                        boolean index = checkInstanceListSortField(queryParams.get("index").toString(), sortFiled);
                        sortBuilderSQL.append(" IFNULL(").append(sortFiled).append(",'') = '' , ");
                        if (index) {
                            sortBuilderSQL.append(" INET_ATON(").append(sortFiled).append(") ");
                        } else {
                            sortBuilderSQL.append(sortFiled).append(" ");
                        }
                        sortBuilderSQL.append(sortType).append(" ");
                    }
                }
            }
        }
        String moduleId = queryParams.get("module_id").toString();
        String querySQL = moduleService.getModuleQuerySQL(moduleId);
        // ??????????????????
        if (!("").equals(queryBuilder.toString())) {
            querySQL += queryBuilder.toString();
        }
        // ????????????
        if (!("").equals(sortBuilderSQL.toString())) {
            querySQL += " order by " + sortBuilderSQL.toString();
        }
        CmdbSqlManage sqlManage = new CmdbSqlManage(querySQL, moduleId, Constants.INSTANCE_AUTH_MODULE, Constants.NEED_AUTH);
        return jdbcHelper.getInt(sqlManage, null, statementParams);
    }

    /**
     * ??????????????????????????????????????????????????????IP???????????? INET_ATON()
     * 
     * @param tableName
     *            ????????????
     * @param sortField
     *            ????????????
     * @return true - ????????????????????????
     */
    private boolean checkInstanceListSortField(String tableName, String sortField) {
        // ????????????????????????????????????,instance_search_sort, create by fanwenhui 20200730
        CmdbConfig cmdbConfig = cmdbConfigService.getConfigByCode("instance_search_sort");
        if (null == cmdbConfig) {
            return false;
        }
        Map<String, String> configFiledMap = (Map<String, String>) JSONObject.parse(cmdbConfig.getConfigValue());
        if (null == configFiledMap) { // ?????????????????????
            return false;
        }
        String configField = configFiledMap.get(tableName); // ???????????????????????????
        if (StringUtils.isEmpty(configField)) { // ???????????????????????????????????????
            return false;
        }
        if (!configField.contains(sortField)) {
            return false;
        }
        return true;
    }

    @Data
    class QueryStatement {

        private String partSql;

        private Map<String, Object> params;
    }

    @Override
    @Transactional(rollbackFor = { RuntimeException.class, Exception.class, SQLException.class })
    public String deleteInstanceNoApprove(String userName, List<Map<String, Object>> instanceList, String operateType) {
        String msg = "";
        Integer deleteCount = 0;
        for (Map<String, Object> instanceData : instanceList) {
            SimpleModule module = validModule(instanceData);
            CmdbCollectApproval approval = handleDeleteApproval(userName, instanceData, operateType);
            // ????????????
            this.delete(userName, approval);
            deleteCount++;
        }
        if (deleteCount > 0) {
            msg = "??????????????????:" + deleteCount + "???";
        }
        return msg;
    }

    @Override
    @Transactional(rollbackFor = { RuntimeException.class, Exception.class, SQLException.class })
    public String deleteInstancePysicalNoApprove(String userName, List<Map<String, Object>> instanceList, String operateType) {
        String msg = "";
        Integer deleteCount = 0;
        for (Map<String, Object> instanceData : instanceList) {
            // SimpleModule module = validModule(instanceData);
            CmdbCollectApproval approval = handleDeleteApproval(userName, instanceData, operateType);
            // ????????????
            this.deletePhysic(approval);
            deleteCount++;
        }
        if (deleteCount > 0) {
            msg = "??????????????????:" + deleteCount + "???";
        }
        return msg;
    }
}
