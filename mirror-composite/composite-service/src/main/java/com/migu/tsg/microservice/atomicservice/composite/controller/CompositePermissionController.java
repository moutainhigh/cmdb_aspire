package com.migu.tsg.microservice.atomicservice.composite.controller;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.migu.tsg.microservice.atomicservice.composite.Constants;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.cmdb.CmdbInstanceSearchClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.cmdb.CmdbServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.AuthServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.payload.response.CompAuthFilterResponse;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.PayloadParseUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.ResourceAuthHelper;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.ICompPermissionService;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.BulkActionsRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.BulkActionsResponse;
import com.migu.tsg.microservice.atomicservice.composite.vo.rbac.RbacResource;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.UserViewAction;
import com.migu.tsg.microservice.atomicservice.rbac.dto.ListActionsResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ??????????????????????????????????????????????????????
 * Project Name:composite-service
 * File Name:CompositePermissionController.java
 * Package Name:com.migu.tsg.microservice.atomicservice.composite.controller
 * ClassName: CompositePermissionController <br/>
 * date: 2017???8???23??? ??????9:30:28 <br/>
 * ??????????????????????????????????????????????????????
 *
 * @author pengguihua
 * @since JDK 1.6
 */
@RestController
public class CompositePermissionController extends CommonResourceController implements ICompPermissionService {

    @Autowired
    private ResourceAuthHelper resAuthHelper;

    @Autowired
    private AuthServiceClient authServiceClient;

    @Autowired
    private CmdbServiceClient cmdbService;

    @Autowired
    private CmdbInstanceSearchClient instanceSearchClient;

    /**
     * ?????????yangshilei
     * ???????????????view?????????????????????
     *
     * @param namespace
     * @param username
     * @return
     */
    @Override
    public List<String> listUserActions(@PathVariable("namespace") String namespace,
                                        @PathVariable("username") String username,
                                        @RequestParam(value = "is_admin", defaultValue = "false") final Boolean
                                                    isAdmin) {

        List<String> userActions = new ArrayList<String>();
        List<ListActionsResponse> listActions = authServiceClient.listActions(username, namespace, isAdmin);
        if (null != listActions) {
            // ?????????????????????????????????UserViewAction
            List<UserViewAction> listUserViewActions = listActions.stream().map(item -> {
                UserViewAction userViewAction = new UserViewAction();
                userViewAction.setActions(item.getActions());
                userViewAction.setResource(item.getResource());
                return userViewAction;
            }).collect(Collectors.toList());
            // ????????????????????????view?????????????????????
            List<String> actions;
            for (UserViewAction userPermission : listUserViewActions) {
                actions = userPermission.getActions();
                if (actions.contains("*:view")) {
                    userActions.add("*");
                    break;
                } else {
                    for (String item : actions) {
                        if (item != null && !listActions.contains(item)) {
                            userActions.add(item);
                        }
                    }
                }
            }
            if (userActions.contains("*")) {
                List<String> list = new ArrayList<String>();
                list.add("*");
                return list;
            } else {
                return userActions;
            }
        } else {
            return userActions;
        }

    }

    /**
     * ?????????????????????????????????????????????.
     *
     * @see com.migu.tsg.microservice.atomicservice.composite.service.rbac.ICompPermissionService#actions
     * (java.lang.String, java.lang.String)
     */
    //???List<String>???????????????PermissionResponse
    @Override
    public List<String> actions(@PathVariable("namespace") String namespace,
                                @PathVariable("resource_type") String resourceType) {
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        return resAuthHelper.resourceActions(authCtx.getUser(), resourceType, authCtx.getFlattenConstraints(), null);
    }

    /**
     * ??????????????????????????????????????????????????????.
     *
     * @see com.migu.tsg.microservice.atomicservice.composite.service.rbac.ICompPermissionService#bulkActions
     * (java.util.List, java.lang.String)
     */
    @Override
    public List<BulkActionsResponse> bulkActions(@RequestBody List<BulkActionsRequest> resList,
                                                 @PathVariable("namespace") String namespace, @PathVariable
                                                             ("resource_type") String resourceType,
                                                 @RequestParam(name = "action", required = false, defaultValue =
                                                         Constants.Action.VIEW) String action) {
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();

        // ????????????action?????????????????????action
        String defaultFlatAction = resolveRawAction2FlattenAction(resourceType, action);
        for (BulkActionsRequest resItem : resList) {
            if (StringUtils.isNotBlank(resItem.getResTypeAction())) {
                continue;
            }
            resItem.setResTypeAction(defaultFlatAction);
        }

        List<RbacResource> rbacResList = PayloadParseUtil.jacksonBaseParse(RbacResource.class, resList);
        List<CompAuthFilterResponse> filterList
                = resAuthHelper.resourcesMixedFilter(authCtx.getUser(), rbacResList, authCtx.getFlattenConstraints());
        List<BulkActionsResponse> bulkRespList = new ArrayList<BulkActionsResponse>();

        for (CompAuthFilterResponse authItem : filterList) {
            RbacResource rabcRes = authItem.getResource();
            if (rabcRes == null) {
                continue;
            }
            BulkActionsResponse respItem = new BulkActionsResponse();
            respItem.setName(rabcRes.getName());
            respItem.setUuid(rabcRes.getUuid());
            //respItem.setResTypeActionList(authItem.getResTypeActionList());
            respItem.setResult(authItem.getResTypeActionList());
            bulkRespList.add(respItem);
        }
        return bulkRespList;
    }

    @Override
    @ResponseStatus(HttpStatus.OK)
    public List<Map<String, String>> getAuthDeviceData(@RequestParam(name = "area") String area, @RequestParam(name =
            "bizSystem", required = false) String
            bizSystem, @RequestParam(name = "deviceType", required = false) String deviceType) {
        String room = "", idc = "";
        Set<String> roomSet = Sets.newHashSet();
        Set<String> idcSet = Sets.newHashSet();
        if (StringUtils.isNotEmpty(area)) {
            String[] areaArray = area.split(",");
            for (String areaItem : areaArray) {
                String[] subArray = areaItem.split("_");
                if (subArray.length == 2) {
                    idcSet.add(subArray[0]);
                    roomSet.add(subArray[1]);
                }
            }
        }
        if (!CollectionUtils.isEmpty(idcSet)) {
            idc = Joiner.on(",").join(this.handleIdcWildcard(idcSet));
        }
        if (!CollectionUtils.isEmpty(roomSet)) {
            room = Joiner.on(",").join(this.handlePrecintWildcard(roomSet));
        }
        if (StringUtils.isNotEmpty(bizSystem)) {
            bizSystem =  Joiner.on(",").join(this.handleBizSysWildcard(Sets.newHashSet(Arrays.asList(bizSystem.split(",")))));
        }
        if (StringUtils.isNotEmpty(deviceType)) {
            deviceType = Joiner.on(",").join(this.handleDeviceTypeWildcard(Sets.newHashSet(Arrays.asList(deviceType.split(",")))));
        }

//        param.put("area", area);
//        param.put("bizSystem", bizSystem);
//        param.put("deviceType", deviceType);
        return instanceSearchClient.getAuthDeviceData(idc, room,
                bizSystem, deviceType);
    }


    /**
     * ???????????????????????????????????????????????????????????????. <br/>
     * TODO(???????????????????????????????????? ??? ??????).<br/>
     * TODO(??????????????????????????????????????? ??? ??????).<br/>
     * <p>
     * ????????? pengguihua
     *
     * @param resType
     * @param rawAction
     * @return
     */
    private String resolveRawAction2FlattenAction(String resType, String rawAction) {
        Map<String, String> resTypeActionMap = Constants.RESOURCE_ACTION_MAP.get(resType);
        if (resTypeActionMap == null) {
            return null;
        }
        return resTypeActionMap.get(rawAction);
    }


}
