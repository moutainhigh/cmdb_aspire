package com.migu.tsg.microservice.atomicservice.composite.controller.rbac;

import com.aspire.mirror.common.entity.PageResult;
import com.google.common.collect.Lists;
import com.migu.tsg.microservice.atomicservice.composite.Constants;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.log.LogClientService;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.log.payload.LogEventPayload;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.ResourceSchemaServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.RoleServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.payload.RabcRoleDetailPayload;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.payload.response.CompAuthFilterResponse;
import com.migu.tsg.microservice.atomicservice.composite.common.KeyValue;
import com.migu.tsg.microservice.atomicservice.composite.common.ThreeValue;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext.RequestHeadUser;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.ResAction;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LastLogCodeEnum;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LogCodeDefine;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.LogEventUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.PaginateUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.PayloadParseUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.ResourceAuthHelper;
import com.migu.tsg.microservice.atomicservice.composite.dao.CompositeResourceDao;
import com.migu.tsg.microservice.atomicservice.composite.dao.po.CompositeResource;
import com.migu.tsg.microservice.atomicservice.composite.exception.BaseException;
import com.migu.tsg.microservice.atomicservice.composite.exception.ResourceActionAuthException;
import com.migu.tsg.microservice.atomicservice.composite.exception.ResultErrorEnum;
import com.migu.tsg.microservice.atomicservice.composite.service.common.payload.BaseResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.ICompRbacRoleService;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.CompRoleCreatePayload;
import com.migu.tsg.microservice.atomicservice.composite.vo.rbac.RbacResource;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RbacRoleListItem;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.ResourceRoleList;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RoleAddParentRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RoleDetailParentsResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RoleDetailResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RoleDetailResponsePer;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RoleListResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RolePageRequestPayload;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RolePermission;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RolePermissionRes;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RoleUserPayload;
import com.migu.tsg.microservice.atomicservice.rbac.dto.FetchResourceSchemaDetailResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.GetRoleDetailResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.InsertParentRoleRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.InsertRolePermissionsRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.InsertRolePermissionsResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.InsertRoleRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.InsertRoleResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.ListRolesAssignedResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.ListRolesResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.ModifyRoleRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.RolePageRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.RolesAssignedRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.RolesRevokeRequest;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ??????????????????Controller Project Name:composite-service File
 * Name:RbacRolesController.java Package
 * Name:com.migu.tsg.microservice.atomicservice.composite.controller.rabc
 * ClassName: RbacRolesController <br/>
 * date: 2017???8???24??? ??????2:56:56 <br/>
 * ???????????????Controller
 *
 * @author pengguihua
 * @version
 * @since JDK 1.6
 */
/**
 * <p>
 * Description:???????????????<br />
 * </p>
 * @title RbacRolesController.java
 * @package com.migu.tsg.microservice.atomicservice.composite.controller.rbac
 * @author ?????????
 * @version 0.1 2019???3???14???
 */
@RestController
@LogCodeDefine("1050221")
public class RbacRolesController implements ICompRbacRoleService {

    /**
     * ???????????????
     */
    @Autowired
    private ResourceAuthHelper resAuthHelper;

    @Autowired
    private RoleServiceClient rbacClient;

    @Autowired
    private LogClientService logClient;

    @Autowired
    private CompositeResourceDao compositeResDao;

    @Autowired
    private ResourceSchemaServiceClient resSchemaClient;

    // ????????????
    private static final Logger LOGGER = LoggerFactory.getLogger(RbacRolesController.class);

    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "view")
    @LogCodeDefine("51")
    public RoleListResponse rolesList(@PathVariable("namespace") String namespace,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "project_name", required = false) String projectName,
            @RequestParam(name = "name", required = false) String name) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();

        // resources_resource?????? RoleList
        CompositeResource param = new CompositeResource();
        param.setType(reqCtx.getResType());
        param.setNamespace(namespace);
        String orderBy = reqCtx.getParameterSingleValue("order_by");
        if (StringUtils.isNotBlank(orderBy)) {
            param.setOrderBy(orderBy);
        }

        CompositeResource projRes = null;
        if (StringUtils.isNotBlank(projectName)) {
            projRes = compositeResDao.queryResourceByName(namespace, Constants.Resource.RES_PROJECT, projectName);
        }
        if (null != projRes) {
            param.setProjectUuid(projRes.getUuid());
        }
        // ??????role?????????project????????????????????????
        List<CompositeResource> roleResList = compositeResDao.queryResourceList(param);
        roleResList = filterRoleListBySearch(roleResList, search);
        if (roleResList == null || roleResList.isEmpty()) {
            return buildRoleResultListResponse(reqCtx, 0, null);
        }

        // ????????????
        RequestHeadUser user = reqCtx.getUser();
		ThreeValue<List<CompositeResource>, List<CompAuthFilterResponse>,Map<String,Set<String>>> authResult = resAuthHelper.filterResourceList(
                user, reqCtx.getResAction(), roleResList, reqCtx.getFlattenConstraints());
        List<CompositeResource> filterResList = authResult.getKey();
        List<CompAuthFilterResponse> actionResList = authResult.getValue();
//        if (filterResList == null || filterResList.isEmpty()) {
//            return buildRoleResultListResponse(reqCtx, 0, null);
//        }
        // ??????RBAC??????role data
        List<CompositeResource> pageList = PaginateUtil.getPaginateQuerySet(reqCtx, filterResList);
        List<String> uuidList = CompositeResource.getUuidList(pageList);

        // ??????????????????????????????
        List<ListRolesResponse> rbacRoleList = rbacClient.listRoles(null, name,user.getNamespace(),Constants.Rbac.ROLE_TYPE_OPERATE,null);
        List<RbacRoleListItem> compRoleList = PayloadParseUtil.jacksonBaseParse(RbacRoleListItem.class, rbacRoleList);

        // merge??????????????? project???space???????????????resource_actions
        mergeRoleListData(compRoleList, pageList, actionResList);
        return buildRoleResultListResponse(reqCtx, filterResList.size(), compRoleList);
    }


    /**
    * ??????????????????????????????????????????????????????
    * @param request ????????????????????????
    * @return
    */
    @Override
    public PageResult<RbacRoleListItem> pageList(@RequestBody RolePageRequestPayload request) {
        if (null == request) {
            LOGGER.warn("pageList param templatePageRequest is null");
            return null;
        }
        RolePageRequest pageRequest = new RolePageRequest();
//        pageRequest.setPageSize(request.getPageSize());
//        pageRequest.setPageNo(request.getPageNo());
        BeanUtils.copyProperties(request, pageRequest);
        //??????????????????????????????PageRequest?????????
        PageResult<ListRolesResponse> roleDTOPageResponse = rbacClient.pageList(pageRequest);
        PageResult<RbacRoleListItem> returnPage=new PageResult<RbacRoleListItem>();
        BeanUtils.copyProperties(roleDTOPageResponse, returnPage);
        returnPage.setResult(PayloadParseUtil.jacksonBaseParse(RbacRoleListItem.class,roleDTOPageResponse.getResult()));
        return returnPage;
     }

    // ???????????????????????????????????????model
    private RoleListResponse buildRoleResultListResponse(final RequestAuthContext reqCtx, int totalCount,
            final List<RbacRoleListItem> compRoleList) {
        RoleListResponse response = new RoleListResponse();
        int pageCount = PaginateUtil.resolveResultPageCount(reqCtx, totalCount);
        int pageSize = PaginateUtil.resolveRequestPageInfo(reqCtx).getValue();
        response.setTotalCount(totalCount);
        response.setPageCount(pageCount);
        response.setPageSize(pageSize);
        response.setRoleDataList(compRoleList);
        return response;
    }

    /**
     * ???role????????????????????????merge. <br/>
     * ?????????role???????????? project???space???????????????resource_actions <br/>
     *
     * ????????? pengguihua
     *
     * @param compRoleList
     * @param pageList
     * @param actionResList
     * @return
     */
    private List<RbacRoleListItem> mergeRoleListData(final List<RbacRoleListItem> compRoleList,
            final List<CompositeResource> pageList, final List<CompAuthFilterResponse> actionResList) {
        for (RbacRoleListItem roleData : compRoleList) {
            String roleUuid = String.valueOf(roleData.getUuid());
            for (CompositeResource role : pageList) {
                if (roleUuid.equals(role.getUuid())) {
                    roleData.setSpaceUuid(role.getSpaceUuid());
                    roleData.setSpaceName(role.getSpaceName());
                    roleData.setProjectUuid(role.getProjectUuid());
                    roleData.setProjectName(role.getProjectName());
                    break;
                }
            }
            for (CompAuthFilterResponse authItem : actionResList) {
                RbacResource rbacRes = authItem.getResource();
                if (rbacRes == null || rbacRes.getUuid() == null) {
                    continue;
                }
                if (rbacRes.getUuid().equals(roleUuid)) {
                    roleData.setResTypeActionList(authItem.getResTypeActionList());
                    break;
                }
            }
        }
        return compRoleList;
    }

    /**
     * ??????????????????????????????.<br/>
     * projectName ??? roleName ??????
     *
     * ????????? pengguihua
     *
     * @param roleResList
     * @param serarch
     * @return
     */
    private List<CompositeResource> filterRoleListBySearch(List<CompositeResource> roleResList, String search) {
        if (StringUtils.isBlank(search) || roleResList == null || roleResList.isEmpty()) {
            return roleResList;
        }

        String[] splitArr = search.split(",");
        return roleResList.stream().filter(r -> {
            return Arrays.stream(splitArr).anyMatch(s -> {
                return (r.getName() != null && r.getName().indexOf(s) >= 0)
                        || (r.getProjectName() != null && r.getProjectName().indexOf(s) >= 0);
            });
        }).collect(Collectors.toList());
    }

    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "create")
    @LogCodeDefine("52")
    // public List<CompRoleCreatePayload> createRole(@PathVariable("namespace")
    // String namespace,
    // @RequestBody List<CompRoleCreatePayload> createRoleList)
    public List<CompRoleCreatePayload> createRole(@PathVariable("namespace") String namespace,
            @RequestBody CompRoleCreatePayload createRole) {
        List<CompRoleCreatePayload> createRoleList = new ArrayList<CompRoleCreatePayload>();
        createRoleList.add(createRole);
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        // ??????namespace
        List<String> roleNameList = new ArrayList<String>();
        for (CompRoleCreatePayload payload : createRoleList) {
            payload.setNamespace(namespace);
            roleNameList.add(payload.getName());
        }

        // ????????????
//        List<CompositeResource> authRoleList = PayloadParseUtil.parse2CompResList(createRoleList, false);
//        List<CompositeResource> filterList = resAuthHelper.filterResourceList(authCtx.getUser(), authCtx.getResAction(),
//                authRoleList, authCtx.getFlattenConstraints()).getKey();
//        if (authRoleList.size() != filterList.size()) {
//            throw new ResourceActionAuthException();
//        }

//        List<CompositeResource> existRoleResList = compositeResDao.queryResourcesByNameList(namespace,
//                authCtx.getResType(), roleNameList);
//        if (existRoleResList != null && !existRoleResList.isEmpty()) {
//            throw new BaseException(LastLogCodeEnum.RESOURCE_ALREADY_EXIST, ResultErrorEnum.BIZ_RESOURCE_ALREADY_EXIST);
//        }

        // ??????RBAC??????????????????
        List<InsertRoleRequest> rbacCreatRoleRequest = PayloadParseUtil.jacksonBaseParse(InsertRoleRequest.class,
                createRoleList);
        rbacCreatRoleRequest.stream().forEach(req->req.setRoleType(Constants.Rbac.ROLE_TYPE_OPERATE));
        List<InsertRoleResponse> rbacCreateRoleResponse = rbacClient.insertRoles(rbacCreatRoleRequest);
        List<CompRoleCreatePayload> resultList = PayloadParseUtil.jacksonBaseParse(CompRoleCreatePayload.class,
                rbacCreateRoleResponse);

        // ???????????????????????????Composite?????????????????????
//        String username = authCtx.getUser().getUsername();
//        String projectUuid = "";
//        String projectType = RequestConstraintEnum.project_name.getResType();
//        String projectConst = authCtx.getRawConstraints().get(RequestConstraintEnum.project_name.name());
//        CompositeResource proRes = null;
//        if (StringUtils.isNotBlank(projectConst)) {
//            proRes = compositeResDao.queryResourceByName(namespace, projectType, projectConst);
//            projectUuid = proRes == null ? "" : proRes.getUuid();
//        }

//        List<CompositeResource> compRoleList = PayloadParseUtil.parse2CompResList(resultList, true);
//        for (CompositeResource compRes : compRoleList) {
//            compRes.setNamespace(namespace);
//            compRes.setCreatedBy(username);
//            compRes.setCreatedAt(new Date());
//            compRes.setProjectUuid(projectUuid);
//        }
//        // DAO??????Composite????????????
//        compositeResDao.insertCompositeResource(compRoleList.toArray(new CompositeResource[0]));
//        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, authCtx.getResType(), createRole.getUuid(),
//                createRole.getName(), "create", 1, "generic", proRes);
//        String logJson = LogEventUtil.wrapLogEvents2Json(logEvent);
//        logClient.saveEventsLogInfo(logJson);
        LOGGER.debug("Add role success");
        return resultList;

    }


    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "role", action = "update")
    @LogCodeDefine("53")
    public BaseResponse addParent4Role(@PathVariable("namespace") String namespace,
            @PathVariable("role_name") String roleName, @RequestBody RoleAddParentRequest pRole) {
        LOGGER.debug("Welcome into addParent4Role");
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        // ??????????????????
        CompositeResource roleRes = compositeResDao.queryResourceByName(namespace, authCtx.getResType(), roleName);
        if (roleRes == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", roleRes.getName());
        resAuthHelper.resourceActionVerify(authCtx.getUser(), roleRes, authCtx.getResAction(),
                authCtx.getFlattenConstraints());

        // ??????RABC????????????????????????
        InsertParentRoleRequest rbacRequest = PayloadParseUtil.jacksonBaseParse(InsertParentRoleRequest.class, pRole);
        rbacClient.insertParentRole(rbacRequest, roleRes.getUuid());
        LOGGER.debug("insertParentRole success >>>>>>>");
        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, "parent_role", pRole.getUuid(),
                pRole.getRoleName(), "add", 0, "sub_resource", roleRes);
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_type", roleRes.getType());
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_id", roleRes.getUuid());
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_name", roleRes.getName());
        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
        logClient.saveEventsLogInfo(jacksonJson);
        return new BaseResponse();
    }

    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "role", action = "update")
    @LogCodeDefine("54")
    public BaseResponse removeParent4Role(@PathVariable("namespace") String namespace,
            @PathVariable("role_name") String roleName, @PathVariable("parent_uuid") String parentUuid) {
        LOGGER.debug("Welcome into removeParent4Role");
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        // ?????? Role
        CompositeResource roleRes = compositeResDao.queryResourceByName(namespace, authCtx.getResType(), roleName);
        if (roleRes == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", roleRes.getName());
        // ??????????????????
        resAuthHelper.resourceActionVerify(authCtx.getUser(), roleRes, authCtx.getResAction(),
                authCtx.getFlattenConstraints());
        rbacClient.deleteParentRole(roleRes.getUuid(), parentUuid);
        LOGGER.debug("Delete parentRole success>>>>>>");
        CompositeResource parentRoleRes = compositeResDao.queryResourceByUuid(namespace, authCtx.getResType(),
                parentUuid);
        String parentRoleName = parentRoleRes == null ? "" : parentRoleRes.getName();

        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, "parent_role", parentUuid, parentRoleName,
                "remove", 1, "sub_resource", parentRoleRes);
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_type", roleRes.getType());
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_id", roleRes.getUuid());
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_name", roleRes.getName());
        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
        logClient.saveEventsLogInfo(jacksonJson);
        return new BaseResponse();
    }

    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "role", action = "update")
    @LogCodeDefine("55")
    public BaseResponse removePermissionFromRole(@PathVariable("namespace") String namespace,
            @PathVariable("role_name") String roleName, @PathVariable("permission_uuid") String permissionUuid) {
        LOGGER.debug("Welcome into removePermissionFromRole");
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        CompositeResource roleRes = compositeResDao.queryResourceByName(namespace, authCtx.getResType(), roleName);
        if (roleRes == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", roleRes.getName());
        // ??????????????????
        resAuthHelper.resourceActionVerify(authCtx.getUser(), roleRes, authCtx.getResAction(),
                authCtx.getFlattenConstraints());
        GetRoleDetailResponse roleDetailResp = rbacClient.getRoleDetail(roleRes.getUuid());
        LOGGER.debug("Enter into rbac method getRoleDetail success>>>>");
        if (roleDetailResp == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        // ???????????????????????????
        rbacClient.deleteRolePermission(roleRes.getUuid(), permissionUuid);

        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, "permission", permissionUuid,
                permissionUuid, "remove", 1, "sub_resource", roleRes);
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_type", roleRes.getType());
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_id", roleRes.getUuid());
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_name", roleRes.getName());
        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
        logClient.saveEventsLogInfo(jacksonJson);
        return new BaseResponse();
    }

    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "view")
    @LogCodeDefine("56")
    public List<RoleUserPayload> listRoleUsers(@PathVariable("namespace") String namespace,
            @PathVariable("role_name") String roleName) {
        LOGGER.info("Welcome into listRoleUsers");
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        CompositeResource roleRes = compositeResDao.queryResourceByName(namespace, reqCtx.getResType(), roleName);
        if (roleRes == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", roleRes.getName());

        // ??????????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), roleRes, reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());
        List<String> uuidList = new ArrayList<String>();
        uuidList.add(roleRes.getUuid());

        // PYTHON?????????user???null
        List<ListRolesAssignedResponse> roleUserList = rbacClient.listRolesAssigned(namespace, uuidList, null);
        LOGGER.debug("Enter into rbac method 'listRolesAssigned' success");
        return PayloadParseUtil.jacksonBaseParse(RoleUserPayload.class, roleUserList);
    }

    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "assign")
    @LogCodeDefine("57")
    public List<RoleUserPayload> assignRoleUsers(@PathVariable("namespace") String namespace,
            @PathVariable("role_name") String roleName, @RequestBody List<RoleUserPayload> roleUserList) {
        LOGGER.info("Welcome into assignRoleUsers");
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        CompositeResource roleRes = compositeResDao.queryResourceByName(namespace, authCtx.getResType(), roleName);
        if (roleRes == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", roleRes.getName());
        List<String> assignUserList = new ArrayList<String>();
        for (RoleUserPayload item : roleUserList) {
            // ??????????????????
            item.setNamespace(namespace);
            item.setRoleUuid(roleRes.getUuid());
            item.setRoleName(roleRes.getName());
            assignUserList.add(item.getUser());
            LOGGER.debug("Add another attribute for role >>>>:{}", item.getNamespace());
        }

        RbacResource rbacRes = roleRes.getFlatten();
        List<RbacResource> assignUserResList = new ArrayList<RbacResource>();
        for (String assignUser : assignUserList) {
            RbacResource clone = PayloadParseUtil.jacksonBaseParse(RbacResource.class, rbacRes);
            clone.setSubAccount(assignUser);
            assignUserResList.add(clone);
            LOGGER.debug("Add attribution for role success>>>>>>");
        }
        LOGGER.debug("Bagin authentication authority>>>>>");
        // ????????????
        List<CompAuthFilterResponse> filterList = resAuthHelper.filterRbacResourceList(authCtx.getUser(),
                authCtx.getResAction(), assignUserResList, authCtx.getFlattenConstraints());
        LOGGER.debug("isAdmin >>>>>{}", authCtx.getUser().isAdmin());
        LOGGER.debug("filterList.size()>>>>>{}", filterList.size());
        LOGGER.debug("assignUserList.size()>>>>>{}", assignUserList.size());
        if (!authCtx.getUser().isAdmin() && filterList.size() != assignUserList.size()) {
            throw new ResourceActionAuthException();
        }
        // ????????????
        List<RolesAssignedRequest> rbacRequest = PayloadParseUtil.jacksonBaseParse(RolesAssignedRequest.class,
                roleUserList);
        // for(RolesAssignedRequest role :rbacRequest){
        // LOGGER.debug("**********?????????namespace????????????{}",role.getNamespace());
        // LOGGER.debug("**********?????????roleName????????????{}",role.getRoleName());
        // LOGGER.debug("**********?????????user????????????{}",role.getUser());
        // }
        rbacClient.rolesAssigned(rbacRequest);

        List<LogEventPayload> logEventList = new ArrayList<LogEventPayload>();
        for (RolesAssignedRequest assign : rbacRequest) {
            LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, "role", assign.getRoleUuid(),
                    assign.getRoleName(), "add", 1, "generic-svoa", roleRes);
            LogEventUtil.popupDetail(logEvent, "attribute_type", "member");
            LogEventUtil.popupDetail(logEvent, "attribute", assign.getUser());
            LOGGER.info("Add attribute success >>>>>");
            logEventList.add(logEvent);
        }
        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEventList.toArray(new LogEventPayload[0]));
        LOGGER.info("Assign users to roles???{}", jacksonJson);
        logClient.saveEventsLogInfo(jacksonJson);
        return roleUserList;
    }

    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "role", action = "revoke")
    @LogCodeDefine("58")
    public BaseResponse revokeRoleUsers(@PathVariable("namespace") String namespace,
            @PathVariable("role_name") String roleName, @RequestBody List<RoleUserPayload> roleUserList) {
        LOGGER.info("Welcome into revokeRoleUsers >>>>>>");
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        CompositeResource roleRes = compositeResDao.queryResourceByName(namespace, authCtx.getResType(), roleName);
        if (roleRes == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", roleRes.getName());
        for (RoleUserPayload userItem : roleUserList) {
            userItem.setNamespace(authCtx.getUser().getNamespace());
            userItem.setRoleName(roleName);
        }
        List<RolesRevokeRequest> rbacRequest = PayloadParseUtil.jacksonBaseParse(RolesRevokeRequest.class,
                roleUserList);
        rbacClient.rolesRevoke(rbacRequest);

        // ????????????
        List<LogEventPayload> logEventList = new ArrayList<LogEventPayload>();
        for (RoleUserPayload assign : roleUserList) {
            LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, "role", assign.getRoleUuid(),
                    assign.getRoleName(), "remove", 1, "generic-svoa", roleRes);
            LogEventUtil.popupDetail(logEvent, "attribute_type", "member");
            LogEventUtil.popupDetail(logEvent, "attribute", assign.getUser());
            logEventList.add(logEvent);
        }
        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEventList.toArray(new LogEventPayload[0]));
        logClient.saveEventsLogInfo(jacksonJson);
        return new BaseResponse();
    }

    // ??????????????????
    // ?????????
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "view")
    @LogCodeDefine("59")
    public RoleDetailResponse retrieveRoleByName(@PathVariable("namespace") String namespace,
            @PathVariable("role_name") String roleName) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ??????namespace???role_name?????????object??????
        CompositeResource object = compositeResDao.queryResourceByName(namespace, reqCtx.getResType(), roleName);
        if (object == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", object.getName());
        // ????????????object????????????/auth/verify/??????????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), object, reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());
        // ??????uuid????????????/roles/instances/{role_uuid}???get???????????????
        GetRoleDetailResponse roleDetail = rbacClient.getRoleDetail(object.getUuid());
        RabcRoleDetailPayload rbacRoleDetailResponse = PayloadParseUtil.jacksonBaseParse(RabcRoleDetailPayload.class,
                roleDetail);
        LOGGER.debug("Enter into rbac 'getRoleDetail' success >>>>>{}", roleDetail.getName());
        // ???????????????created_by??????
        rbacRoleDetailResponse.setCreateBy(object.getCreatedBy());

        // ???????????????????????????????????????actions
        List<RoleDetailParentsResponse> parents = rbacRoleDetailResponse.getRoleParentDataList();
        if (parents != null) {
            // ???rbac??????????????????parents????????????jakiro?????????????????????
            List<CompositeResource> parentResRoleList = PayloadParseUtil.parse2CompResList(parents);

            KeyValue<List<CompositeResource>, List<CompAuthFilterResponse>> filterActionList = resAuthHelper
                    .filterResourceList(reqCtx.getUser(), reqCtx.getResAction(), parentResRoleList,
                            reqCtx.getFlattenConstraints());
            LOGGER.info("Authority identification and get actions success >>>>>>");
            // ???????????????action??????,??????action??????
            List<CompAuthFilterResponse> authActionList = filterActionList.getValue();
            for (RoleDetailParentsResponse roleParent : parents) {
                for (CompAuthFilterResponse authAction : authActionList) {
                    if (roleParent.getUuid().equals(authAction.getResource().getUuid())) {
                        roleParent.setRoleAction(authAction.getResTypeActionList());
                        LOGGER.debug("If uuid is the same ,add actions to roleParent>>>>>>");
                    }
                }
            }
            // ??????parents
            rbacRoleDetailResponse.setRoleParentDataList(parents);
        }

        // rbac???????????????????????????jakiro??????????????????
        RoleDetailResponse jakiroRoleDetailResponse = new RoleDetailResponse();
        jakiroRoleDetailResponse.setUuid(rbacRoleDetailResponse.getUuid());
        jakiroRoleDetailResponse.setRoleParentDataList(rbacRoleDetailResponse.getRoleParentDataList());
        jakiroRoleDetailResponse.setAdminRole(rbacRoleDetailResponse.getAdminRole());
        jakiroRoleDetailResponse.setName(rbacRoleDetailResponse.getName());
        jakiroRoleDetailResponse.setRolePemissionList(rbacRoleDetailResponse.getRolePemissionList());
        jakiroRoleDetailResponse.setCreateTime(rbacRoleDetailResponse.getCreateTime());
        jakiroRoleDetailResponse.setUpdateTime(rbacRoleDetailResponse.getUpdateTime());
        // ????????????????????????????????????creat_by???namespace;
        jakiroRoleDetailResponse.setCreateBy(rbacRoleDetailResponse.getCreateBy());
        jakiroRoleDetailResponse.setOrgAccount(rbacRoleDetailResponse.getNamespace());
        List<String> resTypeActionList = resAuthHelper.resourceActions(reqCtx.getUser(), reqCtx.getResType(),
                reqCtx.getFlattenConstraints(), object.getFlatten());
        jakiroRoleDetailResponse.setResourceAction(resTypeActionList);
        // jakiroRoleDetailResponse.setResourceAction(rbacRoleDetailResponse.getRolePemissionList());
        // ??????????????????

        return jakiroRoleDetailResponse;
    }

    // ????????????
    // ?????????
    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "role", action = "update")
    @LogCodeDefine("60")
    public BaseResponse updateRoleById(@PathVariable("namespace") String namespace,
            @PathVariable("role_id") String roleId, @RequestBody CompRoleCreatePayload requestRole) {
        LOGGER.info("Welcome into updateRoleByName >>>>>>");

        GetRoleDetailResponse roleDetail = rbacClient.getRoleDetail(roleId);
        //admin?????????????????????
        if(roleDetail.getAdminRole()) {
        	throw new BaseException(LastLogCodeEnum.PERMISSION_ERROR, ResultErrorEnum.BIZ_RESOURCE_ACTION_DENY);
        }
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ??????namespace???role_name????????????object??????
//        CompositeResource object = compositeResDao.queryResourceByName(namespace, reqCtx.getResType(), roleName);
//        if (object == null) {
//            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
//        }
//        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", object.getName());
        // ????????????object????????????/auth/verify/??????????????????
        CompositeResource object=new CompositeResource();
        object.setUuid(roleId);
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), object, reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());

        // ??????rbac??????????????????
        ModifyRoleRequest jacksonBaseParse = PayloadParseUtil.jacksonBaseParse(ModifyRoleRequest.class, requestRole);
        rbacClient.modifyRole(roleId, jacksonBaseParse);
        LOGGER.debug("Enter into rbac modifyRole success >>>>>");
        // ??????????????????
        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(reqCtx, reqCtx.getResType(), object.getUuid(),
                object.getName(), reqCtx.getRawAction(), 1, "generic", object);
        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
//        logClient.saveEventsLogInfo(jacksonJson);
        return new BaseResponse();
    }

    // ????????????
    // ?????????
    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "role", action = "delete")
    @LogCodeDefine("61")
    public BaseResponse deleteRoleByName(@PathVariable("namespace") String namespace,
            @PathVariable("role_name") String roleName) {
        LOGGER.info("Welcome into deleteRoleByName>>>>");
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        // ??????namespace???role_name????????????object??????
        CompositeResource object = compositeResDao.queryResourceByName(namespace, authCtx.getResType(), roleName);
        if (object == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", object.getName());

        // ????????????object????????????/auth/verify/??????????????????
        resAuthHelper.resourceActionVerify(authCtx.getUser(), object, authCtx.getResAction(),
                authCtx.getFlattenConstraints());
        // ??????roles/instances/{uuid}??????????????????
        rbacClient.deleteRole(object.getUuid());
        LOGGER.debug("Enter into rbac to delete role success >>>>>");
        // ??????composite???????????????????????????
        compositeResDao.removeCompositeResource(namespace, authCtx.getResType(), object.getUuid());
        LOGGER.debug("Delete role from resources_resource after rbac delete role >>>>>");
        // ??????????????????
        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, authCtx.getResType(), object.getUuid(),
                object.getName(), authCtx.getRawAction(), 1, "generic", object);
        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
        logClient.saveEventsLogInfo(jacksonJson);
        return new BaseResponse();
    }


    // ????????????permission
    // ?????????
    @Override
    @ResponseStatus(HttpStatus.CREATED)
    @ResAction(resType = "role", action = "update")
    @LogCodeDefine("62")
    public RolePermission addPermission(@PathVariable("namespace") String namespace,
            @PathVariable("role_name") String roleName, @RequestBody RolePermission requestPermission) {
        LOGGER.info("Welcome into addPermission  >>>>");
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        // ??????namespace???role_name????????????object??????
        CompositeResource object = compositeResDao.queryResourceByName(namespace, authCtx.getResType(), roleName);

        if (object == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", object.getName());

        // ????????????object????????????/auth/verify/??????????????????
        resAuthHelper.resourceActionVerify(authCtx.getUser(), object, authCtx.getResAction(),
                authCtx.getFlattenConstraints());

        // ??????rbac???????????????role??????permission
        InsertRolePermissionsRequest requestPermissionParse = PayloadParseUtil
                .jacksonBaseParse(InsertRolePermissionsRequest.class, requestPermission);
        InsertRolePermissionsResponse permissionResponse = rbacClient.insertRolePermission(requestPermissionParse,
                object.getUuid());
        LOGGER.info("Enter into rbac insert role permission success >>>>>>");
        RolePermission permission = PayloadParseUtil.jacksonBaseParse(RolePermission.class, permissionResponse);

        // ??????????????????
        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, "permission", permission.getUuid(),
                permission.getName(), authCtx.getRawAction(), 0, "sub_resource", object);
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_type", authCtx.getResType());
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_id", object.getUuid());
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_name", object.getName());
        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
        logClient.saveEventsLogInfo(jacksonJson);
        return permission;
    }

    // ????????????
    // ?????????
    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "role", action = "update")
    @LogCodeDefine("63")
    public BaseResponse updatePermissionFromRole(@PathVariable("namespace") String namespace,
            @PathVariable("role_name") String roleName, @PathVariable("permission_uuid") String permissionUuid,
            @RequestBody RolePermission requestPermission) {
        LOGGER.info("Welcome into updatePermissionFromRole  >>>>");
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        // ??????namespace???role_name????????????object??????
        CompositeResource object = compositeResDao.queryResourceByName(namespace, authCtx.getResType(), roleName);

        if (object == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", object.getName());

        // ????????????object????????????/auth/verify/??????????????????
        resAuthHelper.resourceActionVerify(authCtx.getUser(), object, authCtx.getResAction(),
                authCtx.getFlattenConstraints());
        // ??????????????????
        InsertRolePermissionsRequest jacksonBaseParse = PayloadParseUtil
                .jacksonBaseParse(InsertRolePermissionsRequest.class, requestPermission);
        rbacClient.modifyRolePermission(jacksonBaseParse, object.getUuid(), permissionUuid);
        LOGGER.info("Enter into rbac modifyRolePermission success >>>>>");
        // ??????????????????
        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, "permission", requestPermission.getUuid(),
                requestPermission.getName(), authCtx.getRawAction(), 1, "sub_resource", object);
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_type", authCtx.getResType());
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_id", object.getUuid());
        LogEventUtil.popupDetailInnerMap(logEvent, "parent", "resource_name", object.getName());
        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
        logClient.saveEventsLogInfo(jacksonJson);
        return new BaseResponse();
    }
    // ????????????
    // ?????????
    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "role", action = "delete")
    @LogCodeDefine("64")
    public BaseResponse deleteRoleById(@PathVariable("namespace") String namespace,
            @PathVariable("role_id") String roleId) {
        LOGGER.info("Welcome into deleteRoleById>>>>");
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        GetRoleDetailResponse roleDetail = rbacClient.getRoleDetail(roleId);
        //admin?????????????????????
        if(roleDetail.getAdminRole()) {
        	throw new BaseException(LastLogCodeEnum.PERMISSION_ERROR, ResultErrorEnum.BIZ_RESOURCE_ACTION_DENY);
        }
        // ??????namespace???role_name????????????object??????
//        CompositeResource object = compositeResDao.queryResourceByName(namespace, authCtx.getResType(), roleName);
//        if (object == null) {
//            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
//        }
//        LOGGER.debug("Query resources_resource success ,the name is >>>> {}", object.getName());

        // ????????????object????????????/auth/verify/??????????????????
        resAuthHelper.resourceActionVerify(authCtx.getUser(), new RbacResource(), authCtx.getResAction(),
                authCtx.getFlattenConstraints());
        // ??????roles/instances/{uuid}??????????????????
        rbacClient.deleteRole(roleId);
//        LOGGER.debug("Enter into rbac to delete role success >>>>>");
//        // ??????composite???????????????????????????
//        compositeResDao.removeCompositeResource(namespace, authCtx.getResType(), roleId);
//        LOGGER.debug("Delete role from resources_resource after rbac delete role >>>>>");

//        // ??????????????????
//        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, authCtx.getResType(), object.getUuid(),
//                object.getName(), authCtx.getRawAction(), 1, "generic", object);
//        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
//        logClient.saveEventsLogInfo(jacksonJson);
        return new BaseResponse();
    }
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "create")
    @LogCodeDefine("64")
    public List<CompRoleCreatePayload> createResourceRole(@PathVariable("namespace") String namespace,
            @RequestBody CompRoleCreatePayload createRole) {
        List<CompRoleCreatePayload> createRoleList = new ArrayList<CompRoleCreatePayload>();
        createRoleList.add(createRole);
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        // ??????namespace
        List<String> roleNameList = new ArrayList<String>();
        for (CompRoleCreatePayload payload : createRoleList) {
            payload.setNamespace(namespace);
            roleNameList.add(payload.getName());
        }

        // ????????????
//        List<CompositeResource> authRoleList = PayloadParseUtil.parse2CompResList(createRoleList, false);
//        List<CompositeResource> filterList = resAuthHelper.filterResourceList(authCtx.getUser(), authCtx.getResAction(),
//                authRoleList, authCtx.getFlattenConstraints()).getKey();
//        if (authRoleList.size() != filterList.size()) {
//            throw new ResourceActionAuthException();
//        }

//        List<CompositeResource> existRoleResList = compositeResDao.queryResourcesByNameList(namespace,
//                authCtx.getResType(), roleNameList);
//        if (existRoleResList != null && !existRoleResList.isEmpty()) {
//            throw new BaseException(LastLogCodeEnum.RESOURCE_ALREADY_EXIST, ResultErrorEnum.BIZ_RESOURCE_ALREADY_EXIST);
//        }

        // ??????RBAC??????????????????
        List<InsertRoleRequest> rbacCreatRoleRequest = PayloadParseUtil.jacksonBaseParse(InsertRoleRequest.class,
                createRoleList);
        rbacCreatRoleRequest.stream().forEach(req->{
        	req.setRoleType(Constants.Rbac.ROLE_TYPE_RESOURCE);
        	req.getPermissions().stream().forEach(permission->{
        		permission.setActions(Arrays.asList("#"));
        		permission.setResource(new ArrayList<String>());
    		});
    	});
        List<InsertRoleResponse> rbacCreateRoleResponse = rbacClient.insertRoles(rbacCreatRoleRequest);
        List<CompRoleCreatePayload> resultList = PayloadParseUtil.jacksonBaseParse(CompRoleCreatePayload.class,
                rbacCreateRoleResponse);

        // ???????????????????????????Composite?????????????????????
//        String username = authCtx.getUser().getUsername();
//        String projectUuid = "";
//        String projectType = RequestConstraintEnum.project_name.getResType();
//        String projectConst = authCtx.getRawConstraints().get(RequestConstraintEnum.project_name.name());
//        CompositeResource proRes = null;
//        if (StringUtils.isNotBlank(projectConst)) {
//            proRes = compositeResDao.queryResourceByName(namespace, projectType, projectConst);
//            projectUuid = proRes == null ? "" : proRes.getUuid();
//        }

//        List<CompositeResource> compRoleList = PayloadParseUtil.parse2CompResList(resultList, true);
//        for (CompositeResource compRes : compRoleList) {
//            compRes.setNamespace(namespace);
//            compRes.setCreatedBy(username);
//            compRes.setCreatedAt(new Date());
//            compRes.setProjectUuid(projectUuid);
//        }
//        // DAO??????Composite????????????
//        compositeResDao.insertCompositeResource(compRoleList.toArray(new CompositeResource[0]));
//        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, authCtx.getResType(), createRole.getUuid(),
//                createRole.getName(), "create", 1, "generic", proRes);
//        String logJson = LogEventUtil.wrapLogEvents2Json(logEvent);
//        logClient.saveEventsLogInfo(logJson);
        LOGGER.debug("Add role success");
        return resultList;

    }
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "view")
    @LogCodeDefine("65")
    public RoleListResponse rolesRerouceList(@RequestParam(name = "id", required = false) String id,
			@RequestParam(name = "name", required = false) String name) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ????????????
        RequestHeadUser user = reqCtx.getUser();
		ThreeValue<List<CompositeResource>, List<CompAuthFilterResponse>,Map<String,Set<String>>> authResult = resAuthHelper.filterResourceList(
                user, reqCtx.getResAction(), null, reqCtx.getFlattenConstraints());
        List<CompositeResource> filterResList = authResult.getKey();
        List<CompAuthFilterResponse> actionResList = authResult.getValue();
//        if (filterResList == null || filterResList.isEmpty()) {
//            return buildRoleResultListResponse(reqCtx, 0, null);
//        }
        // ??????RBAC??????role data
        List<CompositeResource> pageList = PaginateUtil.getPaginateQuerySet(reqCtx, filterResList);
        List<String> uuidList = CompositeResource.getUuidList(pageList);

        // ??????????????????????????????
        List<ListRolesResponse> rbacRoleList = rbacClient.childListRoles(Arrays.asList(id), name,user.getNamespace(),Constants.Rbac.ROLE_TYPE_RESOURCE);
        List<RbacRoleListItem> compRoleList = PayloadParseUtil.jacksonBaseParse(RbacRoleListItem.class, rbacRoleList);

        // merge??????????????? project???space???????????????resource_actions
        mergeRoleListData(compRoleList, pageList, actionResList);
        return buildRoleResultListResponse(reqCtx, filterResList.size(), compRoleList);
    }
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "view")
    @LogCodeDefine("65")
    public List<ResourceRoleList> rolesRerouceAllList(@RequestParam(name = "id", required = false) String id,
			@RequestParam(name = "name", required = false) String name) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ????????????
        RequestHeadUser user = reqCtx.getUser();
		ThreeValue<List<CompositeResource>, List<CompAuthFilterResponse>,Map<String,Set<String>>> authResult = resAuthHelper.filterResourceList(
                user, reqCtx.getResAction(), null, reqCtx.getFlattenConstraints());

        // ??????RBAC??????role data

        // ??????????????????????????????
        List<ListRolesResponse> rbacRoleList = rbacClient.childListRoles(Arrays.asList(id), name,user.getNamespace(),Constants.Rbac.ROLE_TYPE_RESOURCE);
        List<ResourceRoleList> compRoleList = PayloadParseUtil.jacksonBaseParse(ResourceRoleList.class, rbacRoleList);


        getTree(compRoleList,user);

        return compRoleList;
    }

    /**
     * <p>
     * ???????????????
     * </p>
     * @author ?????????
     * @version 0.1 2019???3???14???
     * @param reuqest
     * @param user
     * void
     */
    private void getTree(List<ResourceRoleList> reuqest,RequestHeadUser user){
    	for(ResourceRoleList req:reuqest) {
    		List<ListRolesResponse> childListRoles = rbacClient.childListRoles(Arrays.asList(req.getUuid()), null ,user.getNamespace(),Constants.Rbac.ROLE_TYPE_RESOURCE);
			List<ResourceRoleList> childList = PayloadParseUtil.jacksonBaseParse(ResourceRoleList.class,childListRoles);
			req.setChildList(childList);
			getTree(childList,user);
    	}
    }
    // ??????????????????
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "view")
    @LogCodeDefine("66")
//    @Authentication(anonymous = true)
    public List<RoleDetailResponsePer> retrieveRoleByUserName(@PathVariable("namespace") String namespace,
                                                        @PathVariable("user_name") String userName) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ??????namespace???role_name?????????object??????
        // ????????????object????????????/auth/verify/??????????????????
//        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new CompositeResource(), reqCtx.getResAction(),
//                reqCtx.getFlattenConstraints());
        // ??????uuid????????????/roles/instances/{role_uuid}???get???????????????
        Map<String, String[]> map = rbacClient.getOperRoleIdByUserName(namespace, userName);
        String[] roleIdArray = map.get("ids");
        List<RoleDetailResponsePer> result = Lists.newArrayList();
        for (String roleId : roleIdArray) {
            GetRoleDetailResponse roleDetail = rbacClient.getRoleDetail(roleId);
            RoleDetailResponsePer jakiroRoleDetailResponse = getRoleDetailResponsePer(reqCtx, roleDetail);
            result.add(jakiroRoleDetailResponse);
        }
        return result;
    }

    private RoleDetailResponsePer getRoleDetailResponsePer(RequestAuthContext reqCtx, GetRoleDetailResponse roleDetail) {
        RabcRoleDetailPayload rbacRoleDetailResponse = PayloadParseUtil.jacksonBaseParse(RabcRoleDetailPayload.class,
                roleDetail);
        LOGGER.debug("Enter into rbac 'getRoleDetail' success >>>>>{}", roleDetail.getName());
        // ???????????????created_by??????
        rbacRoleDetailResponse.setCreateBy(reqCtx.getUser().getUsername());

        // ???????????????????????????????????????actions
        List<RoleDetailParentsResponse> parents = rbacRoleDetailResponse.getRoleParentDataList();
        if (!CollectionUtils.isEmpty(parents)) {
            // ???rbac??????????????????parents????????????jakiro?????????????????????
            List<CompositeResource> parentResRoleList = PayloadParseUtil.parse2CompResList(parents);

            KeyValue<List<CompositeResource>, List<CompAuthFilterResponse>> filterActionList = resAuthHelper
                    .filterResourceList(reqCtx.getUser(), reqCtx.getResAction(), parentResRoleList,
                            reqCtx.getFlattenConstraints());
            LOGGER.info("Authority identification and get actions success >>>>>>");
            // ???????????????action??????,??????action??????
            List<CompAuthFilterResponse> authActionList = filterActionList.getValue();
            for (RoleDetailParentsResponse roleParent : parents) {
                for (CompAuthFilterResponse authAction : authActionList) {
                    if (roleParent.getUuid().equals(authAction.getResource().getUuid())) {
                        roleParent.setRoleAction(authAction.getResTypeActionList());
                        LOGGER.debug("If uuid is the same ,add actions to roleParent>>>>>>");
                    }
                }
            }
            // ??????parents
            rbacRoleDetailResponse.setRoleParentDataList(parents);
        }

        // rbac???????????????????????????jakiro??????????????????
        RoleDetailResponsePer jakiroRoleDetailResponse = new RoleDetailResponsePer();
        jakiroRoleDetailResponse.setUuid(rbacRoleDetailResponse.getUuid());
        jakiroRoleDetailResponse.setRoleParentDataList(rbacRoleDetailResponse.getRoleParentDataList());
        jakiroRoleDetailResponse.setAdminRole(rbacRoleDetailResponse.getAdminRole());
        jakiroRoleDetailResponse.setName(rbacRoleDetailResponse.getName());

        //??????resource???action?????????name
        List<RolePermissionRes> list=new ArrayList<>();
        List<RolePermission> rolePemissionList = rbacRoleDetailResponse.getRolePemissionList();
        Map<String,String> nameMap=new HashMap<>();
        List<FetchResourceSchemaDetailResponse> allResourceSchama = resSchemaClient.fetchRoleSchemaList();
        Map<String, String> resourceMap = allResourceSchama.stream().collect(Collectors.toMap(item -> item.getResource(), item -> item.getName()));
        Map<String, Map<String, String>> actionMap = allResourceSchama.stream().collect(Collectors.toMap(item -> item.getResource(), item -> item.getActions()));
        for(RolePermission per:rolePemissionList) {
            for(String resourceType: per.getResourceList()) {
//                FetchResourceSchemaDetailResponse fetchRoleSchemaDetail = resSchemaClient.fetchRoleSchemaDetail(resourceType);

//                String resource = fetchRoleSchemaDetail.getResource();
//                if(resource != null) {
//                    nameMap.putAll(fetchRoleSchemaDetail.getActions());
//                    nameMap.put(resource, fetchRoleSchemaDetail.getName());
//                }
                nameMap.put(resourceType, resourceMap.get(resourceType));
                if (actionMap.get(resourceType) != null) {
                    nameMap.putAll(actionMap.get(resourceType));
                }
            }
        }
        for(RolePermission per:rolePemissionList) {
            Map<String,String> resourceName=new HashMap<String,String>();
            Map<String,String> actionName=new HashMap<String,String>();
            RolePermissionRes res=new RolePermissionRes();

            for(String resource:per.getResourceList()) {
                resourceName.put(resource, nameMap.get(resource));
            }
            res.setResourceList(resourceName);

            for(String actoin:per.getResTypeActionList()) {
                if(nameMap.containsKey(actoin)) {
                    actionName.put(actoin, nameMap.get(actoin));
                }
            }
            res.setResTypeActionList(actionName);

            res.setConstraints(per.getConstraints());
            res.setName(per.getName());
            res.setRoleUuid(per.getRoleUuid());
            res.setUuid(per.getUuid());
            list.add(res);
        }

        jakiroRoleDetailResponse.setRolePemissionList(list);


        jakiroRoleDetailResponse.setCreateTime(rbacRoleDetailResponse.getCreateTime());
        jakiroRoleDetailResponse.setUpdateTime(rbacRoleDetailResponse.getUpdateTime());
        // ????????????????????????????????????creat_by???namespace;
        jakiroRoleDetailResponse.setCreateBy(rbacRoleDetailResponse.getCreateBy());
        jakiroRoleDetailResponse.setOrgAccount(rbacRoleDetailResponse.getNamespace());
        jakiroRoleDetailResponse.setDescribe(rbacRoleDetailResponse.getDescribe());
        jakiroRoleDetailResponse.setRoleType(rbacRoleDetailResponse.getRoleType());
//        List<String> resTypeActionList = resAuthHelper.resourceActions(reqCtx.getUser(), reqCtx.getResType(),
//                reqCtx.getFlattenConstraints(), object.getFlatten());
//        jakiroRoleDetailResponse.setResourceAction(resTypeActionList);
        // jakiroRoleDetailResponse.setResourceAction(rbacRoleDetailResponse.getRolePemissionList());
        // ??????????????????
        return jakiroRoleDetailResponse;
    }

    // ??????????????????
    // ?????????
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "view")
    @LogCodeDefine("66")
    public RoleDetailResponsePer retrieveRoleById(@PathVariable("role_id") String roleId) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ??????namespace???role_name?????????object??????
        // ????????????object????????????/auth/verify/??????????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new CompositeResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());
        // ??????uuid????????????/roles/instances/{role_uuid}???get???????????????
        GetRoleDetailResponse roleDetail = rbacClient.getRoleDetail(roleId);
        RoleDetailResponsePer jakiroRoleDetailResponse = getRoleDetailResponsePer(reqCtx, roleDetail);
//        List<String> resTypeActionList = resAuthHelper.resourceActions(reqCtx.getUser(), reqCtx.getResType(),
//                reqCtx.getFlattenConstraints(), object.getFlatten());
//        jakiroRoleDetailResponse.setResourceAction(resTypeActionList);
        // jakiroRoleDetailResponse.setResourceAction(rbacRoleDetailResponse.getRolePemissionList());
        // ??????????????????

        return jakiroRoleDetailResponse;
    }

}
