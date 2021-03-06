package com.aspire.ums.cmdb.v2.restful.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.aspire.ums.cmdb.client.IRbacDepartmentClient;
import com.aspire.ums.cmdb.client.IRbacUserClient;
import com.aspire.ums.cmdb.cmic.util.EventThreadUtils;
import com.aspire.ums.cmdb.common.Constants;
import com.aspire.ums.cmdb.common.Result;
import com.aspire.ums.cmdb.helper.JDBCHelper;
import com.aspire.ums.cmdb.report.service.ICmdb31ProvinceReportService;
import com.aspire.ums.cmdb.sqlManage.CmdbSqlManage;
import com.aspire.ums.cmdb.util.StringUtils;
import com.aspire.ums.cmdb.util.UUIDUtil;
import com.aspire.ums.cmdb.v2.instance.service.ICmdbInstanceService;
import com.aspire.ums.cmdb.v2.module.service.ModuleService;
import com.aspire.ums.cmdb.v2.restful.mapper.BPMRestfulMapper;
import com.aspire.ums.cmdb.v2.restful.service.IBPMRestfulService;
import com.aspire.ums.cmdb.v3.config.payload.CmdbConfig;
import com.aspire.ums.cmdb.v3.config.service.ICmdbConfigService;
import com.aspire.ums.cmdb.v3.dictMapper.payload.CmdbDictMapperEntity;
import com.aspire.ums.cmdb.v3.dictMapper.payload.CmdbSyncFiledMapperEntity;
import com.aspire.ums.cmdb.v3.dictMapper.service.ICmdbDictMapperService;
import com.aspire.ums.cmdb.v3.dictMapper.service.ICmdbSyncFiledMapperService;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.DepartmentDTO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

@Service
@Slf4j
public class BPMRestfulServiceImpl implements IBPMRestfulService {

    @Autowired
    private ICmdbInstanceService instanceService;
    @Autowired
    private ICmdbConfigService configService;
    @Autowired
    private BPMRestfulMapper bpmRestfulMapper;
    @Autowired
    private ICmdbDictMapperService dictMapperService;
    @Autowired
    private ModuleService moduleService;
    @Autowired
    private JDBCHelper jdbcHelper;
    @Autowired
    private ICmdbSyncFiledMapperService filedMapperService;
    @Autowired
    private IRbacUserClient rbacUserClient;
    @Autowired
    private IRbacDepartmentClient departmentClient;
    @Autowired
    private ICmdb31ProvinceReportService provinceReportService;
    /**
     * ??????BPM??????????????????
     * @param resourceInfo ??????????????????
     *  {
     *    "bizSystem": "????????????",
     *    "idcType": "???????????????",
     *    "request_type": "request/release",
     *    "pod": "POD??????",
     *    "data": [{
     *          "type": "????????????",
     *          "num": "????????????",
     *          "cpu": "?????????CPU??????",
     *          "memory": "?????????????????????"
     *       }]
     *  }
     */
    @Override
    public Map<String, Object> resourceRequestProcess(Map<String, Object> resourceInfo) {
        // ????????????
        Map<String, Object> returnMap = new HashMap<>();
        if (resourceInfo.isEmpty()) {
            returnMap.put("flag", "error");
            returnMap.put("msg", "????????????????????????.");
            return returnMap;
        }
        if (!resourceInfo.containsKey("bizSystem") || !StringUtils.isNotEmpty(resourceInfo.get("bizSystem"))) {
            returnMap.put("flag", "error");
            returnMap.put("msg", "???????????????????????????????????????.");
            return returnMap;
        }
        if (!resourceInfo.containsKey("idcType") || !StringUtils.isNotEmpty(resourceInfo.get("idcType"))) {
            returnMap.put("flag", "error");
            returnMap.put("msg", "????????????????????????????????????.");
            return returnMap;
        }
        if (!resourceInfo.containsKey("data")) {
            returnMap.put("flag", "error");
            returnMap.put("msg", "???????????????????????????????????????.");
            return returnMap;
        }
        if (!resourceInfo.containsKey("request_type")) {
            returnMap.put("flag", "error");
            returnMap.put("msg", "?????????????????????????????????.");
            return returnMap;
        }
        String bizSystem = resourceInfo.get("bizSystem").toString();
        String idcType = resourceInfo.get("idcType").toString();
        String podName = null;
        if (resourceInfo.containsKey("pod") && !StringUtils.isNotEmpty(resourceInfo.get("pod"))) {
            podName = resourceInfo.get("pod").toString();
        }
        String requestType = resourceInfo.get("request_type").toString().toLowerCase(Locale.ENGLISH);
        Map<String, Object> params = new HashMap<>();
        params.put("idcType", idcType);
        params.put("bizSystem", bizSystem);
        if (podName != null) {
            params.put("podName", podName);
        }
        List<Map<String, Object>> quoteList = bpmRestfulMapper.queryBizSystemQuote(params);
        Map<String, Object> quoteMap = null;
        if (quoteList != null && quoteList.size() > 0) {
            quoteMap = quoteList.get(0);
        }
        CmdbDictMapperEntity queryEntity = new CmdbDictMapperEntity();
        queryEntity.setMapperSource("BPM");
        queryEntity.setMapperDictType("BPM??????????????????");
        List<CmdbDictMapperEntity> entityList = dictMapperService.listByEntity(queryEntity);
        if (entityList == null || entityList.size() == 0) {
            returnMap.put("flag", "error");
            returnMap.put("msg", "????????????cmdb_dict_mapper???????????????????????????.");
            return returnMap;
        }
        Map<String, Object> requestData = new HashMap<>();
        // ????????????
        List<Map<String, Object>> dataList = (List<Map<String, Object>>) resourceInfo.get("data");
        for (Map<String, Object> data : dataList) {
            String resourceType = data.get("type").toString();
            String number = data.get("num").toString();
            String cpu = null, memory = null;
            if (data.containsKey("cpu") && StringUtils.isNotEmpty(data.get("cpu"))) {
                cpu = data.get("cpu").toString();
            }
            if (data.containsKey("memory") && StringUtils.isNotEmpty(data.get("memory"))) {
                memory = data.get("memory").toString();
            }
            // ??????????????????
            for (CmdbDictMapperEntity dictEntity : entityList) {
                if (dictEntity.getMapperDictCode().equals(resourceType)) {
                    if (StringUtils.isEmpty(dictEntity.getUmsDictCode())) {
                        log.error("cmdb_dict_mapper?????????ums_dict_code, ????????????.");
                        continue;
                    }
                    requestData.put(dictEntity.getUmsDictCode(), number);
                    if (resourceType.equals("?????????")) {
                        if (cpu != null) {
                            requestData.put("yzj_vcpu_allocation_amount", cpu);
                        }
                        if (memory != null) {
                            requestData.put("yzj_memory_allocation_amount", memory);
                        }
                    }
                }
            }
        }
        // ??????
        if (quoteMap == null) {
            if (requestType.equals("request")) {
                // ?????????????????????????????????POD???ID
                String idcId = moduleService.getIDByCNName("idcType", idcType);
                if (StringUtils.isEmpty(idcId)) {
                    returnMap.put("flag", "error");
                    returnMap.put("msg", "????????????????????????" + idcType + ", ???????????????????????????CMDB?????????.");
                    return returnMap;
                }
                String bizSystemId = moduleService.getIDByCNName("bizSystem", bizSystem);
                if (StringUtils.isEmpty(bizSystemId)) {
                    returnMap.put("flag", "error");
                    returnMap.put("msg", "???????????????????????????" + bizSystem + ", ??????????????????????????????CMDB?????????.");
                    return returnMap;
                }
                if (!StringUtils.isEmpty(podName)) {
                    String podId = moduleService.getIDByCNName("pod_name", podName);
                    if (StringUtils.isEmpty(podId)) {
                        returnMap.put("flag", "error");
                        returnMap.put("msg", "?????????POD??????" + podName + ", ?????????POD?????????CMDB?????????.");
                        return returnMap;
                    }
                    requestData.put("pod", podId);
                }
                // ??????????????????
                CmdbConfig cmdbConfig = configService.getConfigByCode("business_quote_module_id", "38de00ee103b4bafb82489b3a0dc3311");
                String moduleId = cmdbConfig.getConfigValue();
                requestData.put("id", UUIDUtil.getUUID());
                requestData.put("module_id", moduleId);
                requestData.put("idcType", idcId);
                requestData.put("owner_biz_system", bizSystemId);
                instanceService.addInstance("BPM??????", requestData, "????????????");
            }
        } else {
            for (String key : requestData.keySet()) {
                Object oldValue = quoteMap.get(key);
                if (requestType.equals("request")) {
                    quoteMap.put(key, plusValue(oldValue, requestData.get(key)));
                } else if (requestType.equals("release")) {
                    quoteMap.put(key, subtractValue(oldValue, requestData.get(key)));
                } else {
                    returnMap.put("flag", "error");
                    returnMap.put("msg", "????????????????????????" + requestType + ", ???????????????.");
                    return returnMap;
                }
            }
            instanceService.updateInstance(quoteMap.get("id").toString(),"BPM??????", quoteMap, "????????????");
        }
        returnMap.put("flag", "success");
        return returnMap;
    }

    @Override
    public Map<String, Object> syncOrgSystem(Map<String, Object> orgManagerData) {
        log.info("start to sync org system.");
        Map<String, Object> resultMap = new HashMap<>();
        String msg = "????????????";
        boolean success = true;
        try {
            // ??????????????????????????????????????????is_delete???????????????
            List<Map> insertOrg = JSONArray.parseArray(JSON.toJSONString(orgManagerData.get("insertOrg")), Map.class);
            List<Map> updateOrg = JSONArray.parseArray(JSON.toJSONString(orgManagerData.get("updateOrg")), Map.class);
            List<Map> deleteOrg = new ArrayList<>();
            // ????????????????????????????????????
            updateOrg.forEach(item -> {
                boolean isDelete = Boolean.parseBoolean(item.get("deleted").toString());
                if(isDelete) {
                    deleteOrg.add(item);
                }
            });
            // ????????????????????????????????????
            updateOrg.removeAll(deleteOrg);
            // ????????????????????????????????????0????????????????????????????????????
            insertOrg.addAll(updateOrg);
            // ????????????????????????id??????
            CmdbConfig config = configService.getConfigByCode("org_manager_module_id");
            if (null == config) {
                success = false;
                throw new RuntimeException("????????????????????????????????????????????????[org_manager_module_id]");
            }
            String orgModuleId = config.getConfigValue();
            // ??????BPM??????????????????????????????
            CmdbSyncFiledMapperEntity queryEntity = new CmdbSyncFiledMapperEntity();
            queryEntity.setMapperType("????????????");
            queryEntity.setSource("BPM");
            Map<String, String> filedMap = new HashMap<>();
            List<CmdbSyncFiledMapperEntity> filedMapList = filedMapperService.listByEntity(queryEntity);
            filedMapList.forEach(item -> {
                filedMap.put(item.getOtherFiledCode(), item.getUmsFiledCode());
            });
            // ????????????????????????sql
            String baseSql = moduleService.getModuleQuerySQL(orgModuleId);
            // ??????????????????????????????
            for (Map org : insertOrg) {
                EventThreadUtils.NORMAL_POOL.execute(()->{
                    try{
                        handleAddOrUpdate(orgModuleId, org, baseSql, filedMap);
                    } catch (Exception e) {
                        log.error("??????/?????????????????????data: {}???error:{}", org, e.getMessage());
                    }
                });
            }
            // ????????????????????????
            List<Map<String,Object>> deleteList = new ArrayList<>();
            for (Map org : deleteOrg) {
                if (!org.containsKey("uuid") || !StringUtils.isNotEmpty(org.get("uuid"))) {
                    log.error("???????????????BPM???????????????UUID?????????{}",org.toString());
                    continue;
                }
                Map<String,Object> instanceData = new HashMap<>();
                String uuid = org.get("uuid").toString();
                Map<String, Object> params = new HashMap<>();
                params.put(filedMap.get("uuid"), uuid);
                String whereString = " and source_id = #{source_id} ";
                List<Map<String, Object>> exsitOrg= jdbcHelper.getQueryList(new CmdbSqlManage(baseSql, null ,Constants.UN_NEED_AUTH),whereString, null, null,params );
                if (exsitOrg != null && exsitOrg.size() > 0) {
                    instanceData.put("module_id", orgModuleId);
                    instanceData.put("id", exsitOrg.get(0).get("id"));
                    deleteList.add(instanceData);
                }
            }
            instanceService.deleteInstance("???????????????", deleteList, "??????BPM??????");
        } catch (Exception e) {
            msg = e.getMessage();
        }

        resultMap.put("success", success);
        resultMap.put("msg", msg);
        return resultMap;
    }

    /**
     * ??????????????????????????????????????????
     * 1. ?????????????????????????????????, ????????????????????????????????????
     * 2. ????????????????????????????????????, ?????????????????????????????????????????????????????????????????????????????????
     *
     * @param account ????????????
     * @return
     */
    @Override
    public Result<Map<String, Object>> getBizSystemListByAccount(String account, String bizSystem, int currentPage,int pageSize) {
        // ????????????????????????
        int bizCount = bpmRestfulMapper.getBizSystemListByAccountCount(account, bizSystem);
        List<Map<String, Object>> bizList = new LinkedList<>();
        if (bizCount > 0) {
            bizList = bpmRestfulMapper.getBizSystemListByAccount(account, bizSystem, currentPage, pageSize);
            return new Result<>(bizCount, bizList);
        }
        // ???????????????????????????, ??????????????????????????????, ????????????
        UserVO user = rbacUserClient.findByLdapId(account);
        List<DepartmentDTO> deptList = user.getDeptList();
        // ??????????????????
        if (deptList == null || deptList.size() == 0) {
            return new Result<>(0, new LinkedList<>());
        }
        // ????????????????????????????????????
        List<String> deptIdSet = new LinkedList<>();
        getDeptIdSet(deptIdSet, deptList);
        // ?????????????????????
        bizCount = bpmRestfulMapper.getBizSystemListByOrgDepartmentIdsCount(deptIdSet, bizSystem);
        if (bizCount > 0) {
            bizList = bpmRestfulMapper.getBizSystemListByOrgDepartmentIds(deptIdSet, bizSystem, currentPage, pageSize);
        }
        return new Result<>(bizCount, bizList);
    }

    @Override
    public List<Map<String, Object>> listOrderReportData(String submitMonth) {
        return provinceReportService.listOrderReportData(submitMonth);
    }


    private void getDeptIdSet(List<String> deptIdSet, List<DepartmentDTO> departments) {
        for (DepartmentDTO department : departments) {
            deptIdSet.add(department.getUuid());
            List<DepartmentDTO> subDepartemnt = departmentClient.queryByDeptId(department.getUuid());
            if (!CollectionUtils.isEmpty(subDepartemnt)) {
                getDeptIdSet(deptIdSet, subDepartemnt);
            }
        }
    }

    void handleAddOrUpdate(String orgModuleId, Map org, String baseSql, Map<String, String> filedMap) {
            Map<String,Object> instanceData = new HashMap<>();
        instanceData.put("module_id", orgModuleId);
        if (!org.containsKey("uuid") || !StringUtils.isNotEmpty(org.get("uuid"))) {
            throw new RuntimeException("???????????????BPM???????????????UUID,?????????" + org.toString());
        }
        // ????????????????????????????????????
        for (String key : filedMap.keySet()) {
            if ("parent_id".equals(filedMap.get(key))) {
                if (!StringUtils.isNotEmpty(org.get(key))) {
                    instanceData.put(filedMap.get(key),"0");
                } else {
                    // ??????parent_id????????????id
                    String parentId = org.get("parent_id").toString();
                    Map<String, Object> params = new HashMap<>();
                    params.put("source_id", parentId);
                    String whereString = " and source_id = #{source_id} ";
                    List<Map<String, Object>> exsitOrg= jdbcHelper.getQueryList(new CmdbSqlManage(baseSql, null ,Constants.UN_NEED_AUTH),whereString, null, null,params );
                    if (exsitOrg != null && exsitOrg.size() > 0) {
                        instanceData.put(filedMap.get(key), exsitOrg.get(0).get("id"));
                    } else {
                        instanceData.put(filedMap.get(key), "");
                    }

                }

            } else {
                instanceData.put(filedMap.get(key),org.get(key));
            }
        }
        String uuid = org.get("uuid").toString();
        String whereString = " and " + filedMap.get("uuid") + " = '" + uuid + "' ";
        List<Map<String, Object>> exsitOrg= jdbcHelper.getQueryList(new CmdbSqlManage(baseSql, null ,Constants.UN_NEED_AUTH),whereString, null, null,null );
        // ??????????????????
        if (exsitOrg.size() > 0) {
            Map<String, Object> oldOrg = exsitOrg.get(0);
            instanceService.updateInstance(oldOrg.get("id").toString(), "???????????????", instanceData, "??????BPM??????");
        } else {
            instanceService.addInstance("???????????????",instanceData, "??????BPM??????");
        }
    }


    private Object plusValue(Object oldValue, Object plusValue) {
        try {
            if (oldValue == null) {
                return oldValue;
            }
            Long v = Long.parseLong(oldValue.toString());
            Long pV = Long.parseLong(plusValue.toString());
            return v + pV;
        } catch (Exception e) {
            Double v = Double.parseDouble(oldValue.toString());
            Double pV = Double.parseDouble(plusValue.toString());
            return v + pV;
        }
    }

    private Object subtractValue(Object oldValue, Object plusValue) {
        try {
            if (oldValue == null) {
                return 0;
            }
            Long v = Long.parseLong(oldValue.toString());
            Long pV = Long.parseLong(plusValue.toString());
            if (v - pV >= 0) {
                return v - pV;
            }
            return 0;
        } catch (Exception e) {
            Double v = Double.parseDouble(oldValue.toString());
            Double pV = Double.parseDouble(plusValue.toString());
            if (v - pV >= 0) {
                return v - pV;
            }
            return 0;
        }
    }
}
