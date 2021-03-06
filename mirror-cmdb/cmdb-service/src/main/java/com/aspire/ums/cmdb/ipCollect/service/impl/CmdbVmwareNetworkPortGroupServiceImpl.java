package com.aspire.ums.cmdb.ipCollect.service.impl;

import com.aspire.ums.cmdb.automate.exception.AutomateException;
import com.aspire.ums.cmdb.ipCollect.mapper.CmdbVmwareNetworkPortGroupMapper;
import com.aspire.ums.cmdb.ipCollect.payload.entity.CmdbVmwareNetworkPortGroup;
import com.aspire.ums.cmdb.ipCollect.payload.vmware.BaseVmwareDTO;
import com.aspire.ums.cmdb.ipCollect.payload.vmware.EasyOpsDataDTO;
import com.aspire.ums.cmdb.ipCollect.payload.vmware.EasyOpsResult;
import com.aspire.ums.cmdb.ipCollect.payload.vmware.VmwareNetworkPortGroupDto;
import com.aspire.ums.cmdb.ipCollect.service.CmdbVmwareNetworkPortGroupService;
import com.aspire.ums.cmdb.ipCollect.utils.VmwareInstanceQueryHelper;
import com.aspire.ums.cmdb.ipCollect.utils.VmwareQuantityQueryHelper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author fanwenhui
 * @date 2020-12-25 15:11
 * @description
 */
@Slf4j
@Service
public class CmdbVmwareNetworkPortGroupServiceImpl implements CmdbVmwareNetworkPortGroupService {

    private static final Integer PAGE_SIZE = 2000;

    private static final Integer OPS_QUERY_COUNT = 100;

    @Autowired
    private CmdbVmwareNetworkPortGroupMapper mapper;

    @Override
    public CmdbVmwareNetworkPortGroup findById(String id) {
        return mapper.findById(id);
    }

    @Override
    public CmdbVmwareNetworkPortGroup findByInstanceId(String instanceId) {
        return mapper.findByInstanceId(instanceId);
    }

    @Override
    public List<CmdbVmwareNetworkPortGroup> findByInstanceIdList(List<String> instanceIdList) {
        return mapper.findByInstanceIdList(instanceIdList);
    }

    @Override
    public void batchAdd(List<CmdbVmwareNetworkPortGroup> entityList) {
        mapper.batchInsert(entityList);
    }

    @Override
    public void add(CmdbVmwareNetworkPortGroup entity) {
        mapper.insert(entity);
    }

    @Override
    public void modify(CmdbVmwareNetworkPortGroup entity) {
        mapper.update(entity);
    }

    @Override
    public void updateByInstanceId(Map<String, Object> params) {
        mapper.updateByInstanceId(params);
    }

    @Override
    public void delete(String id) {
        mapper.delete(id);
    }

    @Override
    public void deleteByInstanceId(String instanceId) {
        mapper.deleteByInstanceId(instanceId);
    }

    @Override
    public List<String> getAllInstanceId() {
        return mapper.getAllInstanceId();
    }

    @Override
    public void batchDeleteByInstanceId(List<String> instanceIdList) {
        mapper.deleteByInstanceIdList(instanceIdList);
    }

    @Override
    public void buildAndCreateInstance(String eventId, String instanceId) {
        List<CmdbVmwareNetworkPortGroup> entityList = Lists.newArrayList();
        EasyOpsResult<VmwareNetworkPortGroupDto> result = VmwareInstanceQueryHelper.queryNetPortGroup(Collections.singletonList(instanceId));
        if (null == result) {
            log.info("instanceId:[{}]?????????????????????,???????????????-????????????????????????",instanceId);
            return;
        }
        List<VmwareNetworkPortGroupDto> dataList = result.getData().getDataList();
        if (dataList.isEmpty()) {
            return;
        }
        for (VmwareNetworkPortGroupDto dto : dataList) {
            CmdbVmwareNetworkPortGroup entity = new CmdbVmwareNetworkPortGroup();
            VmwareInstanceQueryHelper.fillNetworkPortGroup(eventId,dto,entity);
            entityList.add(entity);
        }
        if (!entityList.isEmpty()) {
            batchAdd(entityList);
        }
    }

    @Override
    public void synAllNetworkPortGroup() {
        int pageCount;
        try {
            List<String> opsInstanceIdList = Lists.newArrayList();
            List<String> addIdList = Lists.newArrayList();
            List<String> editIdList = Lists.newArrayList();
            List<String> delIdList = Lists.newArrayList();

            // ??????????????????InstanceId
            List<String> localIdList = mapper.getAllInstanceId();
            log.info("??????????????????NetworkPortGroup??????:[{}]", localIdList.size());
            // 1?????????????????????
            EasyOpsResult<VmwareNetworkPortGroupDto> result = VmwareQuantityQueryHelper.queryNetworkPortGroup(null,1, 1, true);
            int totalCount = result.getData().getTotalCount();
            // 2??????????????????
            pageCount = (totalCount / PAGE_SIZE) + 1;
            log.info("???????????????????????????[Template]??????:[{}]", totalCount);
            // 3????????????????????? InstanceId
            for (int page = 1; page <= pageCount; page++) {
                EasyOpsResult<VmwareNetworkPortGroupDto> networkPortGroup = VmwareQuantityQueryHelper.queryNetworkPortGroup(null,page, PAGE_SIZE, true);
                EasyOpsDataDTO<VmwareNetworkPortGroupDto> dto = networkPortGroup.getData();
                List<VmwareNetworkPortGroupDto> dataList = dto.getDataList();
                if (CollectionUtils.isNotEmpty(dataList)) {
                    List<String> collect = dataList.stream().map(BaseVmwareDTO::getInstanceId).collect(Collectors.toList());
                    opsInstanceIdList.addAll(collect);
                }
            }
            // ????????????ID??????,???????????????????????????????????????????????????
            List<String> reduceIdList = localIdList.stream().filter(item -> !opsInstanceIdList.contains(item))
                    .collect(Collectors.toList());
            delIdList.addAll(reduceIdList);
            // ?????????
            List<String> newIdList = opsInstanceIdList.stream().filter(item -> !localIdList.contains(item))
                    .collect(Collectors.toList());
            addIdList.addAll(newIdList);
            // ????????? ??????
            List<String> updateIdList = opsInstanceIdList.stream().filter(localIdList::contains)
                    .collect(Collectors.toList());
            editIdList.addAll(updateIdList);
            log.info("[NetworkPortGroup]???????????????:[{}],???????????????:[{}],???????????????:[{}]", reduceIdList.size(), newIdList.size(), updateIdList.size());

            // ??????instance??????
            if (CollectionUtils.isNotEmpty(delIdList)) {
                mapper.deleteByInstanceIdList(delIdList);
                log.info("[NetworkPortGroup]??????????????????:[{}]", delIdList.size());
            }

            // ??????instance??????
            saveOrUpdateNetworkPortGroup(addIdList,false);
            // ??????instance??????
            saveOrUpdateNetworkPortGroup(editIdList,true);

        } catch (Exception e) {
            log.error("[NetworkPortGroup]??????????????????", e);
            throw new AutomateException(e);
        }
    }

    /**
     * ????????????????????????-?????????
     * @param idList ??????ID??????
     * @param isUpdate ???????????????true-?????????false-??????
     */
    private void saveOrUpdateNetworkPortGroup(List<String> idList, boolean isUpdate) {
        if (CollectionUtils.isEmpty(idList)) {
            return;
        }
        List<CmdbVmwareNetworkPortGroup> networkPortGroupList = Lists.newArrayList();
        List<List<String>> partitionList = Lists.partition(idList, OPS_QUERY_COUNT);
        for (List<String> subList : partitionList) {
            EasyOpsResult<VmwareNetworkPortGroupDto> easyOpsResult = VmwareInstanceQueryHelper.queryNetPortGroup(subList, 1, OPS_QUERY_COUNT);
            EasyOpsDataDTO<VmwareNetworkPortGroupDto> dto = easyOpsResult.getData();
            if (null == dto) {
                continue;
            }
            List<VmwareNetworkPortGroupDto> dataList = dto.getDataList();
            if (CollectionUtils.isEmpty(dataList)) {
                continue;
            }
            List<String> instanceIdList = dataList.stream().map(BaseVmwareDTO::getInstanceId).collect(Collectors.toList());
            List<CmdbVmwareNetworkPortGroup> entityList = Lists.newArrayList();
            if (isUpdate) {
                entityList = mapper.findByInstanceIdList(instanceIdList);
            }
            List<CmdbVmwareNetworkPortGroup> updateList = VmwareInstanceQueryHelper.getFillNetworkPortGroup(isUpdate, dataList, entityList);
            if (CollectionUtils.isNotEmpty(updateList)) {
                networkPortGroupList.addAll(updateList);
            }
        }

        if (isUpdate) {
            networkPortGroupList.forEach(e -> mapper.update(e));
            log.info("[NetworkPortGroup]??????????????????:[{}]", networkPortGroupList.size());
        } else {
            mapper.batchInsert(networkPortGroupList);
            log.info("[NetworkPortGroup]??????????????????:[{}]", networkPortGroupList.size());
        }
    }
}
