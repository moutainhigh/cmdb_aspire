package com.migu.tsg.microservice.atomicservice.composite.controller.template;

import static com.migu.tsg.microservice.atomicservice.composite.controller.util.PayloadParseUtil.jacksonBaseParse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aspire.mirror.common.constant.SystemConstant;
import com.aspire.mirror.common.entity.PageResponse;
import com.aspire.mirror.composite.service.template.ICompTemplateService;
import com.aspire.mirror.composite.service.template.payload.CompItemsCreateRequest;
import com.aspire.mirror.composite.service.template.payload.CompItemsDetailResponse;
import com.aspire.mirror.composite.service.template.payload.CompMonitorApiServerConfig;
import com.aspire.mirror.composite.service.template.payload.CompTemplateCreateRequest;
import com.aspire.mirror.composite.service.template.payload.CompTemplateCreateResponse;
import com.aspire.mirror.composite.service.template.payload.CompTemplateDetailResponse;
import com.aspire.mirror.composite.service.template.payload.CompTemplatePageRequest;
import com.aspire.mirror.composite.service.template.payload.CompTemplatePageResponse;
import com.aspire.mirror.composite.service.template.payload.CompTemplateUpdateRequest;
import com.aspire.mirror.composite.service.template.payload.CompTemplateUpdateResponse;
import com.aspire.mirror.composite.service.template.payload.CompTriggerCreateRequest;
import com.aspire.mirror.composite.service.template.payload.ZabbixTemplateDetailResponse;
import com.aspire.mirror.ops.api.domain.OpsScript;
import com.aspire.mirror.template.api.dto.ApiServerConfigDetailResponse;
import com.aspire.mirror.template.api.dto.ItemsBatchCreateRequest;
import com.aspire.mirror.template.api.dto.ItemsCreateRequest;
import com.aspire.mirror.template.api.dto.ItemsCreateResponse;
import com.aspire.mirror.template.api.dto.ItemsDetailResponse;
import com.aspire.mirror.template.api.dto.ItemsUpdateRequest;
import com.aspire.mirror.template.api.dto.TemplateCreateRequest;
import com.aspire.mirror.template.api.dto.TemplateCreateResponse;
import com.aspire.mirror.template.api.dto.TemplateDetailResponse;
import com.aspire.mirror.template.api.dto.TemplateObjectBatchCreateRequest;
import com.aspire.mirror.template.api.dto.TemplateObjectCreateRequest;
import com.aspire.mirror.template.api.dto.TemplateObjectDetailResponse;
import com.aspire.mirror.template.api.dto.TemplatePageRequest;
import com.aspire.mirror.template.api.dto.TemplateUpdateRequest;
import com.aspire.mirror.template.api.dto.TemplateUpdateResponse;
import com.aspire.mirror.template.api.dto.TriggersBatchCreateRequest;
import com.aspire.mirror.template.api.dto.TriggersCreateRequest;
import com.aspire.mirror.template.api.dto.TriggersDetailResponse;
import com.aspire.mirror.template.api.dto.model.ItemExt;
import com.aspire.ums.cmdb.instance.payload.CmdbInstance;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.migu.tsg.microservice.atomicservice.composite.Constants;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.cmdb.CmdbServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.cmdb.restful.common.ICommonRestfulClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.inspection.TaskObjectServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.opsmanage.OpsManageClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.template.ApiServerConfigServiceCient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.template.ItemDatasCollectServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.template.ItemsServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.template.TemplateObjectServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.template.TemplateServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.template.TriggersServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.controller.CommonResourceController;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.ResAction;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LastLogCodeEnum;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LogCodeDefine;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.EasyPoiUtil;
import com.migu.tsg.microservice.atomicservice.composite.exception.BaseException;
import com.migu.tsg.microservice.atomicservice.composite.exception.ResultErrorEnum;
import com.migu.tsg.microservice.atomicservice.composite.helper.CmdbHelper;
import com.migu.tsg.microservice.atomicservice.composite.service.common.payload.BaseResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RbacResource;

/**
 * ????????????????????????
 * <p>
 * ????????????:  mirror??????
 * ???:        com.migu.tsg.microservice.atomicservice.composite.controller.template
 * ?????????:    CompTemplateController.java
 * ?????????:    ????????????????????????
 * ?????????:    JinSu
 * ????????????:  2018/8/2 15:37
 * ??????:      v1.0
 */
@RestController
@LogCodeDefine("1050811")
public class CompTemplateController extends CommonResourceController implements ICompTemplateService {

    private final Logger logger = LoggerFactory.getLogger(CompTemplateController.class);

    @Autowired
    private TemplateServiceClient templateService;

    @Autowired
    private ItemsServiceClient itemsService;

    @Autowired
    private TriggersServiceClient triggersService;

    @Autowired
    private TaskObjectServiceClient taskDeviceServiceClient;

//    @Autowired
//    private CompositeResourceDao compositeResDao;

    @Autowired
    private ApiServerConfigServiceCient apiServerConfigService;

    @Autowired
    private ItemDatasCollectServiceClient itemDatasCollectService;

    @Autowired
    private TemplateObjectServiceClient templateObjectService;

    @Autowired
    private CmdbServiceClient cmdbService;

    @Autowired
    private OpsManageClient opsManage;

    @Autowired
    private CmdbHelper cmdbHelper;

    @Value("${systemType:simple}")
    private String systemType;

    @Autowired
    private ICommonRestfulClient cmdbCommonService;

    /**
     * ??????????????????
     *
     * @param createRequest ????????????????????????
     * @return CompTemplateCreateResponse ??????
     */
    @Override
    @ResAction(action = "create", resType = "template")
    public CompTemplateCreateResponse createdTemplate(@RequestBody @Validated final CompTemplateCreateRequest
                                                              createRequest) {
        logger.info("method[createdTemplate] body is {}.", createRequest);
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        logger.info("[createdTemplate]Username is {} namespace{}.", authCtx.getUser().getUsername(), authCtx.getUser
                ().getNamespace());
        RbacResource rbacData = jacksonBaseParse(RbacResource.class, createRequest);
        rbacData.setResTypeAction(authCtx.getResAction());
        logger.info("[createdTemplate]The rbacResource is {}.", rbacData);
        // ??????
        resAuthHelper.resourceActionVerify(
                authCtx.getUser(), rbacData, authCtx.getResAction(), authCtx.getFlattenConstraints());
        logger.info("user {} is trying to create template with name {}.",
                authCtx.getUser().getUsername(), createRequest.getName());
        //  ??????????????????????????????
//        CompositeResource existProjectRes = compositeResDao.queryResourceByName(authCtx.getUser().getNamespace(),
//                authCtx.getResType(), createRequest.getName());
//        if (existProjectRes != null) {
//            String tipMsg = "The config with name '{}' for namespace {} is already exists.";
//            throw new BaseException(tipMsg, LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum
// .BIZ_RESOURCE_ALREADY_EXIST,
//                    createRequest.getName(), authCtx.getUser().getNamespace());
//        }
        TemplateCreateRequest templateCreateRequest = jacksonBaseParse(TemplateCreateRequest.class, createRequest);
        // ?????????????????????????????????
//        templateCreateRequest.setFunType(Constants.Template.FUN_TYPE_INSP);
        // ???????????????????????????
//        templateCreateRequest.setType(Constants.Template.TYPE_HARD);
        // ????????????
        if (templateCreateRequest.getDescription() == null) {
            templateCreateRequest.setDescription("");
        }
        // ?????????????????????
        if (StringUtils.isEmpty(templateCreateRequest.getStatus())) {
            templateCreateRequest.setStatus(Constants.Template.TEMP_STATUS);
        }
        // ???????????????????????????
//        templateCreateRequest.setMonType(Constants.Template.MON_TYPE_SYS);
        // ????????????????????????
//        templateCreateRequest.setSysType(Constants.Template.SYSTYPE_ZABBIX);
        TemplateCreateResponse templateResponse = templateService.createdTemplate(templateCreateRequest);
        String templateId = templateResponse.getTemplateId();
        // ????????????
        if (!CollectionUtils.isEmpty(createRequest.getListItem())) {
            Map<String, String> itemKeyExpressionMap = Maps.newHashMap();
            ItemsBatchCreateRequest createItemRequest = new ItemsBatchCreateRequest();
            List<ItemsCreateRequest> itemsCreateRequestList = Lists.newArrayList();
            for (CompItemsCreateRequest compItemsCreateRequest : createRequest.getListItem()) {
                if (StringUtils.isEmpty(compItemsCreateRequest.getStatus())) {
                    compItemsCreateRequest.setStatus(Constants.Template.STATUS);
                }
                if (StringUtils.isEmpty(compItemsCreateRequest.getType())) {
                    compItemsCreateRequest.setType(Constants.Item.TYPE);
                }
                if (StringUtils.isEmpty(compItemsCreateRequest.getSysType())) {
                    TemplateDetailResponse templateDetailResponse = templateService.findByPrimaryKey
                            (compItemsCreateRequest.getTemplateId());
                    compItemsCreateRequest.setSysType(templateDetailResponse.getSysType());
                }
                if (null == compItemsCreateRequest.getUnits()) {
                    compItemsCreateRequest.setUnits("");
                }
                ItemsCreateRequest itemsCreateRequest = new ItemsCreateRequest();
                BeanUtils.copyProperties(compItemsCreateRequest, itemsCreateRequest);
                Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
                if (compItemsCreateRequest.getDelay() != null && pattern.matcher(compItemsCreateRequest.getDelay())
                        .matches()) {
                    itemsCreateRequest.setDelay(compItemsCreateRequest.getDelay());
                }
                itemsCreateRequest.setTemplateId(templateId);
                ItemExt itemExt = jacksonBaseParse(ItemExt.class, compItemsCreateRequest.getItemExt());
                itemsCreateRequest.setItemExt(itemExt);
                itemsCreateRequestList.add(itemsCreateRequest);
                if (!StringUtils.isEmpty(compItemsCreateRequest.getExpression()) && !StringUtils.isEmpty
                        (compItemsCreateRequest.getMatch())) {
                    itemKeyExpressionMap.put(compItemsCreateRequest.getKey(), compItemsCreateRequest.getExpression()
                            + compItemsCreateRequest.getMatch());
                }
            }

            createItemRequest.setListItem(itemsCreateRequestList);
            List<ItemsCreateResponse> listItemResponse = itemsService.batchCreateItems(createItemRequest);
            // ???????????????
            addTriggerList(itemKeyExpressionMap, listItemResponse);
        }
        if (!StringUtils.isEmpty(createRequest.getObjectIds())) {

            String[] objectList = createRequest.getObjectIds().split(",");
            List<TemplateObjectCreateRequest> templateDeviceList = Lists.newArrayList();
            for (String object : objectList) {
                TemplateObjectCreateRequest templateObject = new TemplateObjectCreateRequest();
                templateObject.setTemplateId(templateId);
                templateObject.setObjectId(object);
                // ???????????? 1????????? 2???????????????
                if (StringUtils.isEmpty(createRequest.getMonType())) {
                    templateObject.setObjectType(Constants.Template.MON_TYPE_SYS);
                } else {
                    templateObject.setObjectType(createRequest.getMonType());
                }
                templateDeviceList.add(templateObject);
            }
            TemplateObjectBatchCreateRequest createDeviceRequest = new TemplateObjectBatchCreateRequest();
            createDeviceRequest.setTemplateObjectList(templateDeviceList);
            templateObjectService.batchCreate(createDeviceRequest);
        }
        // composite??????????????????????????????
//        CompositeResource compTemplate = new CompositeResource();
//        compTemplate.setUuid(templateResponse.getTemplateId());
//        compTemplate.setName(templateResponse.getName());
//        compTemplate.setType(Constants.Resource.RES_TEMPLATE);
//        compTemplate.setNamespace(authCtx.getUser().getNamespace());
//        compTemplate.setCreatedBy(authCtx.getUser().getUsername());
//        this.compositeResDao.insertCompositeResource(compTemplate);

        // ????????????????????????
//        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, Constants.Resource.RES_TEMPLATE,
//                compTemplate.getUuid(), compTemplate.getName(), "create", 0, "generic", null);
//        LogEventUtil.popupDetail(logEvent, "template", compTemplate.getName());
//        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
//        logClient.saveEventsLogInfo(jacksonJson);
        return jacksonBaseParse(CompTemplateCreateResponse.class, templateResponse);
    }

    /**
     * ?????????????????????
     *
     * @param templateIds ???????????????????????????
     * @return
     */
    @Override
    @ResAction(action = "delete", resType = "template")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public BaseResponse deleteByPrimaryKeyArrays(@PathVariable("template_ids") final String templateIds) {
        logger.info("method[deleteByPrimaryKeyArrays] templateIds is {}.", templateIds);
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        logger.info("[deleteByPrimaryKeyArrays]Username is {} namespace is {}.", authCtx.getUser().getUsername(),
                authCtx.getUser().getNamespace());
//        List<String> templateIdList = Arrays.asList(templateIds.split(","));
//        List<CompositeResource> resources = this.compositeResDao.queryResourceByUuidlist(templateIdList);
//        logger.info("[deleteByPrimaryKeyArrays] CompositeResource result is {}", resources);
        // ??????????????????
//        RbacResource rbacData = new RbacResource();
//        //????????????
//        rbacData.setResTypeAction(authCtx.getResAction());
//        logger.info("[list]The rbacResource is {}.", rbacData);
//        resAuthHelper.resourceActionVerify(
//                authCtx.getUser(), rbacData, authCtx.getResAction(), authCtx.getFlattenConstraints());
//        KeyValue<List<CompositeResource>, List<CompAuthFilterResponse>> authResult = resAuthHelper.filterResourceList
//                (authCtx.getUser(), authCtx.getResAction(), resources, authCtx.getFlattenConstraints());
//        List<CompAuthFilterResponse> authList = authResult.getValue();
//        if (authList.size() < resources.size()) {
//            throw new ResourceActionAuthException();
//        }
        List<TemplateDetailResponse> listTemplate = templateService.listByPrimaryKeyArrays(templateIds);
        List<String> inspectionTemplateIdList = Lists.newArrayList();
        for (TemplateDetailResponse template : listTemplate) {
            if (template.getFunType().equals(Constants.Template.FUN_TYPE_INSP)) {
                inspectionTemplateIdList.add(template.getTemplateId());
            }
        }
        //??????????????????
        for (String templateId : inspectionTemplateIdList) {
            int taskNum = taskDeviceServiceClient.findTaskCountByTemplateId(templateId);
            if (taskNum != 0) {
                String tipMsg = "????????????????????????????????????????????????";
                throw new BaseException(tipMsg, LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum
                        .TEMPLATE_DELETE_UNSUCCESSFULLY,
                        templateId, authCtx.getUser().getNamespace());
            }
        }
        //???????????????????????????
        templateObjectService.deleteByTemplateIds(templateIds);
        templateService.deleteByPrimaryKeyArrays(templateIds);
//        //??????????????????
//        compositeResDao.removeCompositeList(authCtx.getUser().getNamespace(), Constants.Resource.RES_TEMPLATE,
//                templateIdList);

        // TODO ??????????????????????????????
//        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, Constants.Resource.RES_REGION,
//                regionRes.getUuid(), regionRes.getName(), "delete", 1, "generic", regionRes);
//        LogEventUtil.popupDetail(logEvent, "region", regionRes.getName());
//        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
//        logClient.saveEventsLogInfo(jacksonJson);

        return new BaseResponse();
    }

    /**
     * ??????????????????
     *
     * @param templateId    ??????ID
     * @param updateRequest ????????????????????????
     * @return CompTemplateUpdateResponse ????????????
     */
    @Override
    @ResAction(action = "update", resType = "template")
    public CompTemplateUpdateResponse modifyByPrimaryKey(@PathVariable("template_id") final String templateId,
                                                         @RequestBody @Validated CompTemplateUpdateRequest
                                                                 updateRequest) {
        logger.info("method[modifyByPrimaryKey] template_id is {},  updateRequest body is {}", templateId,
                updateRequest);
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        logger.info("[modifyByPrimaryKey]Username is {} namesapce is {}.", authCtx.getUser().getUsername(), authCtx
                .getUser().getNamespace());
//        CompositeResource compositeResource = new CompositeResource();
//        compositeResource.setUuid(templateId);
//        compositeResource.setNamespace(authCtx.getUser().getNamespace());
//        CompositeResource resource = this.compositeResDao.queryByUuidAndNamespace(templateId, authCtx.getUser()
//                .getNamespace());
//        resource.setName(updateRequest.getName());
//        logger.info("[modifyByPrimaryKey] CompositeResource result is {}", resource);
        // ??????????????????
//        resAuthHelper.resourceActionVerify(authCtx.getUser(), resource, authCtx.getResAction(),
//                authCtx.getFlattenConstraints());
//        CompositeResource existProjectRes = compositeResDao.queryResourceByName(authCtx.getUser().getNamespace(),
//                authCtx.getResType(), updateRequest.getName());
//        if (existProjectRes != null && !existProjectRes.getUuid().equals(templateId)) {
//            String tipMsg = "The config with name '{}' for namespace {} is already exists.";
//            throw new BaseException(tipMsg, LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum
// .BIZ_RESOURCE_ALREADY_EXIST,
//                    updateRequest.getName(), authCtx.getUser().getNamespace());
//        }
        //???????????????????????????

        //???????????????????????????
        updateRequest.setStatus(Constants.Template.FORMAL_STATUS);
        TemplateUpdateRequest request =
                jacksonBaseParse(TemplateUpdateRequest.class, updateRequest);
        TemplateUpdateResponse templateUpdateResponse = templateService.modifyByPrimaryKey(templateId, request);

        // ?????????????????????
        if (!CollectionUtils.isEmpty(updateRequest.getListItem())) {
            Map<String, String> itemKeyExpressionMap = Maps.newHashMap();
            ItemsBatchCreateRequest createItemRequest = new ItemsBatchCreateRequest();

            List<ItemsCreateRequest> itemsCreateRequestList = Lists.newArrayList();
            Set<String> updateItemIdSet = Sets.newHashSet();
            for (CompItemsCreateRequest compItemsCreateRequest : updateRequest.getListItem()) {
                if (!StringUtils.isEmpty(compItemsCreateRequest.getExpression()) && !StringUtils.isEmpty
                        (compItemsCreateRequest.getMatch())) {
                    itemKeyExpressionMap.put(compItemsCreateRequest.getKey(), compItemsCreateRequest.getExpression()
                            + compItemsCreateRequest.getMatch());
                }
                //??????????????????
                if (!StringUtils.isEmpty(compItemsCreateRequest.getItemId())) {
                    ItemsUpdateRequest itemsUpdateRequest = new ItemsUpdateRequest();
                    BeanUtils.copyProperties(compItemsCreateRequest, itemsUpdateRequest);
                    ItemExt itemExt = new ItemExt();
                    BeanUtils.copyProperties(compItemsCreateRequest.getItemExt(), itemExt);
                    itemsUpdateRequest.setItemExt(itemExt);
                    itemsService.modifyByPrimaryKey(compItemsCreateRequest.getItemId(), itemsUpdateRequest);

//                    TriggersUpdateRequest triggersUpdateRequest = new TriggersUpdateRequest();
//                    triggersUpdateRequest.setExpression(itemKeyExpressionMap.get(compItemsCreateRequest.getKey()));
//                    triggersUpdateRequest.setItemId(compItemsCreateRequest.getItemId());
//                    triggersService.updateExpressionByItemId(triggersUpdateRequest);
                    triggersService.deleteByItemIdArrays(compItemsCreateRequest.getItemId());
                    List<TriggersCreateRequest> createtriggerList = Lists.newArrayList();
                    handlerTrigger(itemKeyExpressionMap, createtriggerList, itemsUpdateRequest, compItemsCreateRequest.getItemId());
                    TriggersBatchCreateRequest triggersBatchCreateRequest = new TriggersBatchCreateRequest();
                    triggersBatchCreateRequest.setTriggerList(createtriggerList);
                    triggersService.batchCreatedTriggers(triggersBatchCreateRequest);
//                    TriggersCreateRequest triggersCreateRequest = new TriggersCreateRequest();
//                    triggersCreateRequest.setItemId(compItemsCreateRequest.getItemId());
//                    triggersCreateRequest.setExpression(itemKeyExpressionMap.get(compItemsCreateRequest.getKey()));
                    updateItemIdSet.add(compItemsCreateRequest.getItemId());
                } else { //????????????

                    if (StringUtils.isEmpty(compItemsCreateRequest.getStatus())) {
                        compItemsCreateRequest.setStatus(Constants.Template.STATUS);
                    }
                    if (StringUtils.isEmpty(compItemsCreateRequest.getType())) {
                        compItemsCreateRequest.setType(Constants.Item.TYPE);
                    }
                    if (StringUtils.isEmpty(compItemsCreateRequest.getSysType())) {
                        TemplateDetailResponse templateDetailResponse = templateService.findByPrimaryKey
                                (compItemsCreateRequest.getTemplateId());
                        compItemsCreateRequest.setSysType(templateDetailResponse.getSysType());
                    }
                    if (null == compItemsCreateRequest.getUnits()) {
                        compItemsCreateRequest.setUnits("");
                    }
                    ItemsCreateRequest itemsCreateRequest = new ItemsCreateRequest();
                    BeanUtils.copyProperties(compItemsCreateRequest, itemsCreateRequest);
                    Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
                    if (compItemsCreateRequest.getDelay() != null && pattern.matcher(compItemsCreateRequest.getDelay())
                            .matches()) {
                        itemsCreateRequest.setDelay(compItemsCreateRequest.getDelay());
                    }
                    itemsCreateRequest.setTemplateId(templateId);
                    ItemExt itemExt = jacksonBaseParse(ItemExt.class, compItemsCreateRequest.getItemExt());
                    itemsCreateRequest.setItemExt(itemExt);
                    itemsCreateRequestList.add(itemsCreateRequest);
                }
//                Set<String> itemRespIdSet = listItemResponse.stream().map(ItemsCreateResponse::getItemId).collect(Collectors.toSet());
            }
            // ??????????????????
            List<ItemsDetailResponse> itemDetailList = itemsService.listItemsByTemplateIds(templateId);
            if (!CollectionUtils.isEmpty(itemDetailList)) {
                Set<String> itemIdSet = itemDetailList.stream().filter(item -> !updateItemIdSet.contains(item
                        .getItemId())).map(ItemsDetailResponse::getItemId).collect
                        (Collectors.toSet());
                if (!CollectionUtils.isEmpty(itemIdSet)) {
                    itemsService.deleteByPrimaryKeyArrays(Joiner.on(",").join(itemIdSet));
                    triggersService.deleteByItemIdArrays(Joiner.on(",").join(itemIdSet));
                }
            }
            if (!CollectionUtils.isEmpty(itemsCreateRequestList)) {
                createItemRequest.setListItem(itemsCreateRequestList);
                List<ItemsCreateResponse> listItemResponse = itemsService.batchCreateItems(createItemRequest);
                // ???????????????
                addTriggerList(itemKeyExpressionMap, listItemResponse);
            }
        }
        // ???????????????
        if (updateRequest.getTriggerList() != null) {
            List<ItemsDetailResponse> list = itemsService.listItemsByTemplateIds(templateId);
            //?????????????????????
            List<String> itemIdArrays = Lists.newArrayList();
            for (ItemsDetailResponse itemsDetailResponse : list) {
                itemIdArrays.add(itemsDetailResponse.getItemId());
            }
            String itemIds = Joiner.on(",").join(itemIdArrays);
            triggersService.deleteByItemIdArrays(itemIds);
            //?????????????????????
            TriggersBatchCreateRequest batchCreateRequest = this.transferTriggers(templateId, updateRequest
                    .getTriggerList());
            if (!CollectionUtils.isEmpty(batchCreateRequest.getTriggerList())) {
                triggersService.batchCreatedTriggers(batchCreateRequest);
            }

        }
        if (!StringUtils.isEmpty(updateRequest.getObjectIds())) {
            templateObjectService.deleteByTemplateIds(templateId);

            String[] objectList = updateRequest.getObjectIds().split(",");
            List<TemplateObjectCreateRequest> templateDeviceList = Lists.newArrayList();
            for (String object : objectList) {
                TemplateObjectCreateRequest templateObject = new TemplateObjectCreateRequest();
                templateObject.setTemplateId(templateId);
                templateObject.setObjectId(object);
                // ???????????? 1????????? 2???????????????
                if (StringUtils.isEmpty(updateRequest.getMonType())) {
                    templateObject.setObjectType(Constants.Template.MON_TYPE_SYS);
                } else {
                    templateObject.setObjectType(updateRequest.getMonType());
                }
                templateDeviceList.add(templateObject);
            }
            TemplateObjectBatchCreateRequest createDeviceRequest = new TemplateObjectBatchCreateRequest();
            createDeviceRequest.setTemplateObjectList(templateDeviceList);
            templateObjectService.batchCreate(createDeviceRequest);
        }

        //??????????????????
//        compositeResDao.updateResourceNameById(resource.getId(), resource.getName());
        CompTemplateUpdateResponse response = new CompTemplateUpdateResponse();
        BeanUtils.copyProperties(templateUpdateResponse, response);

        // TODO ??????????????????????????????
//        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, Constants.Resource.RES_REGION,
//                resource.getUuid(), resource.getName(), "update", 1, "generic", resource);
//        LogEventUtil.popupDetail(logEvent, "region", resource.getName());
//        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
//        logger.info("[modifyByPrimaryKey]log is {}.",jacksonJson);
//        logClient.saveEventsLogInfo(jacksonJson);
        return response;
    }

    private void addTriggerList(Map<String, String> itemKeyExpressionMap, List<ItemsCreateResponse> listItemResponse) {
        if (!CollectionUtils.isEmpty(itemKeyExpressionMap)) {
            List<TriggersCreateRequest> triggerList = Lists.newArrayList();
            for (ItemsCreateResponse itemsCreateResponse : listItemResponse) {
                handlerTrigger(itemKeyExpressionMap, triggerList, itemsCreateResponse);
            }
            TriggersBatchCreateRequest triggersBatchCreateRequest = new TriggersBatchCreateRequest();
            triggersBatchCreateRequest.setTriggerList(triggerList);
            triggersService.batchCreatedTriggers(triggersBatchCreateRequest);
        }
    }

    private void handlerTrigger(Map<String, String> itemKeyExpressionMap, List<TriggersCreateRequest> triggerList, ItemsCreateResponse itemsCreateResponse) {
        if (itemKeyExpressionMap.get(itemsCreateResponse.getKey()) != null) {
            TriggersCreateRequest triggersCreateRequest = new TriggersCreateRequest();
            triggersCreateRequest.setName(itemsCreateResponse.getName());
            triggersCreateRequest.setStatus(Constants.Template.STATUS);
            triggersCreateRequest.setPriority(Constants.Template.PRIORITY);
            triggersCreateRequest.setUrl("");
            triggersCreateRequest.setParam("");
            triggersCreateRequest.setExpression(itemKeyExpressionMap.get(itemsCreateResponse.getKey()));
            triggersCreateRequest.setItemId(itemsCreateResponse.getItemId());
            triggerList.add(triggersCreateRequest);
        }
    }

    private void handlerTrigger(Map<String, String> itemKeyExpressionMap, List<TriggersCreateRequest> triggerList, ItemsUpdateRequest itemsUpdateRequest, String itemId) {
        if (itemKeyExpressionMap.get(itemsUpdateRequest.getKey()) != null) {
            TriggersCreateRequest triggersCreateRequest = new TriggersCreateRequest();
            triggersCreateRequest.setName(itemsUpdateRequest.getName());
            triggersCreateRequest.setStatus(Constants.Template.STATUS);
            triggersCreateRequest.setPriority(Constants.Template.PRIORITY);
            triggersCreateRequest.setUrl("");
            triggersCreateRequest.setParam("");
            triggersCreateRequest.setExpression(itemKeyExpressionMap.get(itemsUpdateRequest.getKey()));
            triggersCreateRequest.setItemId(itemId);
            triggerList.add(triggersCreateRequest);
        }
    }

    /**
     * ????????????ID????????????
     *
     * @param templateId ??????ID
     * @return CompTemplateDetailResponse ??????????????????
     */
    @Override
    @ResAction(action = "view", resType = "template")
    public CompTemplateDetailResponse findByPrimaryKey(@PathVariable("template_id") final String templateId) {
        logger.info("method[findByPrimaryKey]template_id is {}", templateId);
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        logger.info("[findByPrimaryKey]Username is {}.", authCtx.getUser().getUsername());
//        CompositeResource resource = resDao.queryResourceByUuid(authCtx.getUser().getNamespace(), Constants.Resource
//                .RES_TEMPLATE, templateId);
//        if (null == resource) {
//            String tipMsg = "The template with uuid '{}' for namespace {} is not exist.";
//            throw new BaseException(tipMsg, LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST,
//                    templateId, authCtx.getUser().getNamespace());
//        }
        // ??????
//        resAuthHelper.resourceActionVerify(
//                authCtx.getUser(), resource, authCtx.getResAction(), authCtx.getFlattenConstraints());
        //????????????????????????
        TemplateDetailResponse detailResponse = templateService.findByPrimaryKey(templateId);
        List<CompItemsDetailResponse> compListItem = Lists.newArrayList();
        List<ItemsDetailResponse> listItem = detailResponse.getItemList();
        List<String> itemIdArrays = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(listItem)) {
            for (ItemsDetailResponse itemsDetailResponse : listItem) {
                itemIdArrays.add(itemsDetailResponse.getItemId());
                CompItemsDetailResponse compItemsDetailResponse = new CompItemsDetailResponse();
                BeanUtils.copyProperties(itemsDetailResponse, compItemsDetailResponse);
                com.aspire.mirror.composite.service.template.payload.ItemExt itemExt = new com.aspire.mirror.composite.service.template.payload.ItemExt();
                BeanUtils.copyProperties(itemsDetailResponse.getItemExt(), itemExt);
                compItemsDetailResponse.setItemExt(itemExt);
                if (itemsDetailResponse.getSysType().equals("SCRIPT")) {
                    OpsScript opsScript = opsManage.queryOpsScriptById(Long.parseLong(itemsDetailResponse.getKey()));
                    compItemsDetailResponse.setScriptName(opsScript.getScriptName());
                    compItemsDetailResponse.setContentType(opsScript.getContentType());
                }
                compListItem.add(compItemsDetailResponse);
            }
            String itemIds = Joiner.on(",").join(itemIdArrays);
            //?????????????????????
            List<TriggersDetailResponse> listTrigger = triggersService.listByItemIdArrays(itemIds);
            Map<String, TriggersDetailResponse> triggersMaps = Maps.newHashMap();
            if (!CollectionUtils.isEmpty(listTrigger)) {
                for (TriggersDetailResponse triggers : listTrigger) {
                    triggersMaps.put(triggers.getItemId(), triggers);
                }
            }
            //??????????????????????????????????????????
            for (CompItemsDetailResponse itemsDetailResponse : compListItem) {
                if (triggersMaps.containsKey(itemsDetailResponse.getItemId())) {
                    itemsDetailResponse.setExpression(triggersMaps.get(itemsDetailResponse.getItemId()).getExpression
                            ());
                }
            }

        }

        CompTemplateDetailResponse response = new CompTemplateDetailResponse();
        BeanUtils.copyProperties(detailResponse, response);
        //????????????????????????
        List<TemplateObjectDetailResponse> templateObjectList = templateObjectService.findByTemplateId(templateId);
        List<String> objectIds = Lists.newArrayList();
        for (TemplateObjectDetailResponse templateObject : templateObjectList) {
            objectIds.add(templateObject.getObjectId());
        }
        if (!CollectionUtils.isEmpty(objectIds)) {
            List<Map<String, String>> objectList = org.assertj.core.util.Lists.newArrayList();
            if (StringUtils.isEmpty(detailResponse.getMonType()) || detailResponse.getMonType().equals(Constants
                    .Template.DEVICE_TYPE)) {
                List<CmdbInstance> mapList;
                if (SystemConstant.BIZ_SYSTEM_BDC.equals(systemType)) {
                    mapList = null;
                } else {
                    mapList = cmdbService.getInstanceByIds(Joiner.on(",").join(objectIds));
                }
                if (!CollectionUtils.isEmpty(mapList)) {
                    for (CmdbInstance map : mapList) {
                        Map deviceDetail = Maps.newHashMap();
                        deviceDetail.put("device_id", map.getId());
                        deviceDetail.put("device_name", map.getDeviceName());
//                        deviceDetail.setDeviceId((String) map.get("id"));
//                        deviceDetail.setDeviceName((String) map.get("name"));
                        objectList.add(deviceDetail);
                    }
                }

            } else if (detailResponse.getMonType().equals(Constants
                    .Template.SYS_TYPE)) {
                for (String objectId : objectIds) {
//                    String bizSysName = cmdbHelper.getBizSysName(objectId);
                    Map<String, Object> params = Maps.newHashMap();
                    params.put("condicationCode", "business_detail");
                    params.put("token", "5245ed1b-6345-11e");
                    params.put("module_id", "9212e88a698d43cbbf9ec35b83773e2d");
                    params.put("id", objectId);
                    Map<String, Object> resultInstance = cmdbCommonService.getInstanceDetail(params);
                    if (resultInstance != null && resultInstance.get("bizSystem") != null) {
                        String bizName = (String) resultInstance.get("bizSystem");
                        Map bizSys = Maps.newHashMap();
                        bizSys.put("biz_code", objectId);
                        bizSys.put("biz_name", bizName);
                        objectList.add(bizSys);
                    }
//                    if (!StringUtils.isEmpty(bizSysName)) {
//                        Map bizSys = Maps.newHashMap();
//                        bizSys.put("biz_code", objectId);
//                        bizSys.put("biz_name", bizSysName);
////                        deviceDetail.setDeviceId((String) map.get("id"));
////                        deviceDetail.setDeviceName((String) map.get("name"));
//                        objectList.add(bizSys);
//                    }
                }

            } else {
                for (String objectId : objectIds) {
                    Map obj = Maps.newHashMap();
                    obj.put("object_id", objectId);
                    obj.put("object_name", objectId);
                    objectList.add(obj);
                }
            }
            response.setDeviceInstanceList(objectList);
        }
        response.setItemList(compListItem);
        return response;
    }

    /**
     * ??????page????????????????????????
     *
     * @param request ?????????????????????
     * @return PageResponse<CompTemplatePageResponse> ????????????page??????
     */
    @Override
    @ResAction(action = "view", resType = "template", loadResFilter = true)
    public PageResponse<CompTemplatePageResponse> list(@RequestBody CompTemplatePageRequest request) {
        logger.info("method[list] request body is {}", request);
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        logger.info("[list]Username is {}.", authCtx.getUser().getUsername());
        RbacResource rbacData = jacksonBaseParse(RbacResource.class, request);
        //????????????
        rbacData.setResTypeAction(authCtx.getResAction());
        logger.info("[list]The rbacResource is {}.", rbacData);
        resAuthHelper.resourceActionVerify(
                authCtx.getUser(), rbacData, authCtx.getResAction(), authCtx.getFlattenConstraints());
        //????????????????????????
        TemplatePageRequest templatePageRequest = new TemplatePageRequest();
        BeanUtils.copyProperties(request, templatePageRequest);
        if (templatePageRequest.getPageNo() == null) {
            templatePageRequest.setPageNo(1);
        }
        if (templatePageRequest.getPageSize() == null) {
            templatePageRequest.setPageSize(1000);
        }
        PageResponse<TemplateDetailResponse> pageResponse = templateService.pageList(templatePageRequest);
        PageResponse<CompTemplatePageResponse> response = new PageResponse<CompTemplatePageResponse>();
        response.setCount(pageResponse.getCount());
        List<CompTemplatePageResponse> templatePageResponses = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(pageResponse.getResult())) {
            for (TemplateDetailResponse templateDetailResponse : pageResponse.getResult()) {
                CompTemplatePageResponse compTemplatePageResponse = new CompTemplatePageResponse();
                BeanUtils.copyProperties(templateDetailResponse, compTemplatePageResponse);
                List<ItemsDetailResponse> itemList = templateDetailResponse.getItemList();
                List<String> itemNameList = Lists.newArrayList();
                for (ItemsDetailResponse itemsDetailResponse : itemList) {
                    itemNameList.add(itemsDetailResponse.getName());
                }
                String itemNames = Joiner.on(",").join(itemNameList);
                compTemplatePageResponse.setItems(itemNames);
                // ??????taskNum
                try {
                    int taskNum = taskDeviceServiceClient.findTaskCountByTemplateId(templateDetailResponse
                            .getTemplateId());
                    compTemplatePageResponse.setTaskNum(taskNum);
                } catch (Exception e) {
                    logger.error("??????????????????????????????");
                }

                templatePageResponses.add(compTemplatePageResponse);
            }
        }
        response.setResult(templatePageResponses);
        return response;
    }

    /**
     * ????????????
     *
     * @param templateId ??????ID
     * @return
     */
    @Override
    @ResAction(action = "copy", resType = "template")
    public CompTemplateCreateResponse copy(@PathVariable("template_id") String templateId) {
        logger.info("method[copy]template_id is {}", templateId);
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        logger.info("[copy]Username is {}.", authCtx.getUser().getUsername());
//        CompositeResource resource = resDao.queryResourceByUuid(authCtx.getUser().getNamespace(), Constants.Resource
//                .RES_TEMPLATE, templateId);
//        if (null == resource) {
//            String tipMsg = "The template with uuid '{}' for namespace {} is not exist.";
//            throw new BaseException(tipMsg, LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST,
//                    templateId, authCtx.getUser().getNamespace());
//        }
        // ??????
//        resAuthHelper.resourceActionVerify(
//                authCtx.getUser(), resource, authCtx.getResAction(), authCtx.getFlattenConstraints());
        //????????????????????????
        TemplateCreateResponse templateCreateResponse = templateService.copy(templateId);


        //????????????
//        CompositeResource compTemplate = new CompositeResource();
//        compTemplate.setUuid(templateCreateResponse.getTemplateId());
//        compTemplate.setName(templateCreateResponse.getName());
//        compTemplate.setType(Constants.Resource.RES_TEMPLATE);
//        compTemplate.setNamespace(authCtx.getUser().getNamespace());
//        compTemplate.setCreatedBy(authCtx.getUser().getUsername());
//        this.compositeResDao.insertCompositeResource(compTemplate);
        return jacksonBaseParse(CompTemplateCreateResponse.class, templateCreateResponse);
    }

    /**
     * ?????????????????????
     *
     * @param templateId  ??????ID
     * @param triggerList ???????????????
     * @return
     */
    private TriggersBatchCreateRequest transferTriggers(String templateId, List<CompTriggerCreateRequest> triggerList) {
        TriggersBatchCreateRequest request = new TriggersBatchCreateRequest();
        List<TriggersCreateRequest> listCreateTriggerReq = Lists.newArrayList();
        for (CompTriggerCreateRequest compTriggerCreateRequest : triggerList) {
            TriggersCreateRequest triggersCreateRequest = new TriggersCreateRequest();
            BeanUtils.copyProperties(compTriggerCreateRequest, triggersCreateRequest);
            if (StringUtils.isEmpty(triggersCreateRequest.getName())) {
                ItemsDetailResponse itemsDetailResponse = itemsService.findByPrimaryKey(triggersCreateRequest
                        .getItemId());
                triggersCreateRequest.setName(itemsDetailResponse.getName());
            }
            if (StringUtils.isEmpty(triggersCreateRequest.getStatus())) {
                triggersCreateRequest.setStatus(Constants.Template.STATUS);
            }
            if (StringUtils.isEmpty(triggersCreateRequest.getPriority())) {
                triggersCreateRequest.setPriority(Constants.Template.PRIORITY);
            }
            if (triggersCreateRequest.getUrl() == null) {
                triggersCreateRequest.setUrl("");
            }
            if (triggersCreateRequest.getParam() == null) {
                triggersCreateRequest.setParam("");
            }
            listCreateTriggerReq.add(triggersCreateRequest);
        }
        request.setTriggerList(listCreateTriggerReq);
        return request;
    }

    @Override
    @ResAction(action = "view", resType = "template")
    public List<ZabbixTemplateDetailResponse> zbxTemplateList(@PathVariable("room") String room) {
        if (StringUtils.isEmpty(room)) {
            logger.error("????????????????????????");
            return null;
        }
        ApiServerConfigDetailResponse apiServerConfigDetailResponse = apiServerConfigService.findByRoom(room);
        if (apiServerConfigDetailResponse == null) {
            logger.error("?????????????????????zabbix????????????");
            return null;
        }
        CompMonitorApiServerConfig config = jacksonBaseParse(CompMonitorApiServerConfig.class,
                apiServerConfigDetailResponse);
        config.setServerType(CompMonitorApiServerConfig.SERVER_TYPE_ZABBIX);
        List<ZabbixTemplateDetailResponse> list = itemDatasCollectService.loadZbxTemplateList(config);
        return list;
    }

    @Override
    @ResAction(action = "view", resType = "template")
    public CompTemplateDetailResponse findByName(@PathVariable("template_name") String templateName) {
        logger.info("method[findByName]template_name is {}", templateName);
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
//        logger.info("[findByName]Username is {}.", authCtx.getUser().getUsername());
//        CompositeResource resource = resDao.queryResourceByName(authCtx.getUser().getNamespace(), Constants.Resource
//                .RES_TEMPLATE, templateName);
//        if (null != resource) {
////            String tipMsg = "The template with uuid '{}' for namespace {} is not exist.";
////            throw new BaseException(tipMsg, LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST,
////                    templateName, authCtx.getUser().getNamespace());
//            return null;
//        }
        // ??????
//        resAuthHelper.resourceActionVerify(
//                authCtx.getUser(), resource, authCtx.getResAction(), authCtx.getFlattenConstraints());

        return jacksonBaseParse(CompTemplateDetailResponse.class, templateService.findByName(templateName));
    }

    @Override
    @ResAction(action = "view", resType = "template")
    public void exportItems(@RequestParam("template_id") String templateId, HttpServletResponse response) {
        if (StringUtils.isEmpty(templateId)) {
            logger.error("CompTemplateController[exportItems] param template_id is empty");
            return;
        }
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        //????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx
                .getFlattenConstraints());
        String fileName = "item_export";
        List<ItemsDetailResponse> dtoList = itemsService.listItemsByTemplateIds(templateId);
//        List<CompItemsDetailResponse> data = PayloadParseUtil.jacksonBaseParse(CompItemsDetailResponse.class, dtoList);
        List<CompItemsDetailResponse> compListItem = Lists.newArrayList();
//        List<ItemsDetailResponse> listItem = detailResponse.getItemList();
        List<String> itemIdArrays = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(dtoList)) {
            for (ItemsDetailResponse itemsDetailResponse : dtoList) {
                itemIdArrays.add(itemsDetailResponse.getItemId());
                CompItemsDetailResponse compItemsDetailResponse = new CompItemsDetailResponse();
                BeanUtils.copyProperties(itemsDetailResponse, compItemsDetailResponse);

                if (itemsDetailResponse.getSysType().equals("SCRIPT")) {
                    OpsScript opsScript = opsManage.queryOpsScriptById(Long.parseLong(itemsDetailResponse.getKey()));
                    compItemsDetailResponse.setScriptName(opsScript.getScriptName());
                    compItemsDetailResponse.setContentType(opsScript.getContentType());
                }
                compListItem.add(compItemsDetailResponse);
            }
            String itemIds = Joiner.on(",").join(itemIdArrays);
            //?????????????????????
            List<TriggersDetailResponse> listTrigger = triggersService.listByItemIdArrays(itemIds);
            Map<String, TriggersDetailResponse> triggersMaps = Maps.newHashMap();
            if (!CollectionUtils.isEmpty(listTrigger)) {
                for (TriggersDetailResponse triggers : listTrigger) {
                    triggersMaps.put(triggers.getItemId(), triggers);
                }
            }
            //??????????????????????????????????????????
            for (CompItemsDetailResponse itemsDetailResponse : compListItem) {
                if (triggersMaps.containsKey(itemsDetailResponse.getItemId())) {
                    itemsDetailResponse.setExpression(triggersMaps.get(itemsDetailResponse.getItemId()).getExpression
                            ());
                }
            }
            EasyPoiUtil.exportExcel(compListItem, "?????????????????????", "?????????", CompItemsDetailResponse.class, fileName, true, response);
        }
    }
}
