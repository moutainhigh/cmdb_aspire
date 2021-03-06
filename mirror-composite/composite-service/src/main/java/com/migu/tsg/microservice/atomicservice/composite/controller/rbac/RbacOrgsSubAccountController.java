package com.migu.tsg.microservice.atomicservice.composite.controller.rbac;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.migu.tsg.microservice.atomicservice.composite.biz.impl.RbacAsynToBpmServiceImpl;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.log.LogClientService;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.log.payload.LogEventPayload;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.OrgsSubAccountClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.RoleServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.UserServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.payload.response.CompAuthFilterResponse;
import com.migu.tsg.microservice.atomicservice.composite.common.KeyValue;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext.RequestHeadUser;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.ResAction;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LastLogCodeEnum;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LogCodeDefine;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.LogEventUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.PayloadParseUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.ResourceAuthHelper;
import com.migu.tsg.microservice.atomicservice.composite.dao.CompositeResourceDao;
import com.migu.tsg.microservice.atomicservice.composite.dao.po.CompositeResource;
import com.migu.tsg.microservice.atomicservice.composite.exception.BaseException;
import com.migu.tsg.microservice.atomicservice.composite.exception.ResultErrorEnum;
import com.migu.tsg.microservice.atomicservice.composite.service.common.payload.BaseResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.ICompRbacOrgService;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.AuthProfileResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.CreateOrgAccount;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.InsertAccount;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsAssignRolesRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsAssignRolesResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsCompanyNameResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsCompanyRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsCreateSubAccountsRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsCreateSubAccountsResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsDetailResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsLogoFilesRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsLogoFilesRequest.File;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsPasswordRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsRoles;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsRolesPermissionResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsSubAccountDetailResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsSubAccountsListResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsSubAccountsListResponse.Result;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsUpdateAccountsRequest;
import com.migu.tsg.microservice.atomicservice.composite.vo.rbac.RbacResource;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RbacRoleListItem;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.UpdateBykeycloakPayload;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.UserResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.CreateOrgAccountRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.CreateOrgAccountResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.GetOrgDetailResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.GetOrgUserDetailResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.ListOrgAccountsResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.ListRolesAssignedResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.ListRolesResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.RolesAssignedRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.RolesAssignedResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.RolesRevokeRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateBykeycloakRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateOrgRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateOrgResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateSubAccountPasswordRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateSubAccountRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserCreateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.FileUpload;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.UserDTO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.vo.UserVO;

import net.sf.json.JSONObject;

@RestController
@LogCodeDefine("1050221")
public class RbacOrgsSubAccountController implements ICompRbacOrgService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RbacOrgsSubAccountController.class);

    /**
     * ???????????????
     */
    @Autowired
    private ResourceAuthHelper resAuthHelper;

    /**
     * RoleService???
     */
    @Autowired
    private RoleServiceClient rbacClient;

    /**
     * ???????????????
     */
//    @Autowired
//    private LogClientService logClient;

    /**
     * ??????????????????
     */
    @Autowired
    private CompositeResourceDao compositeResDao;

    /**
     * OrgsService?????????
     */
    @Autowired
    private OrgsSubAccountClient orgsClient;

    @Autowired
    private UserServiceClient userClient;

    @Autowired
    private RbacAsynToBpmServiceImpl rbacAsynToBpmService;

    @Autowired
    private LogClientService logClient;

    // TODO ?????????
    // @Autowired
    // private Environment envObj;

    // 0.1 ????????????????????????
    // yangshilei
    @Override
    @ResponseStatus(HttpStatus.OK)
    @LogCodeDefine("01")
    public AuthProfileResponse getAuthProfileDetail(@PathVariable("namespace") String namespace) {

        // ??????rbac???????????????Orgs???????????????
        GetOrgDetailResponse orgDetail = orgsClient.getOrgDetail(namespace);
        // ??????????????????
        AuthProfileResponse authProfileDetail = PayloadParseUtil.jacksonBaseParse(AuthProfileResponse.class, orgDetail);
        // AuthProfileResponse authProfileDetail = new AuthProfileResponse();
        // authProfileDetail.setCompany("????????????");
        authProfileDetail.setCity("beijing");
        authProfileDetail.setIndustry("others");
        authProfileDetail.setPosition("beijing");
        authProfileDetail.setInformedWay("others");
        // authProfileDetail.setRealname("admin");
        authProfileDetail.setId(30491);
        authProfileDetail.setLastLogin("2017-09-17T08:07:22.772Z");
        // authProfileDetail.setUsername("admin");
        // ??????????????????RBAC??????????????????20171106
        // authProfileDetail.setEmail("alaudademo07@sina.com");
        authProfileDetail.setPreferedLanguage("zh-cn");
        authProfileDetail.setIsActive(true);
        authProfileDetail.setIsTrialuser(false);
        authProfileDetail.setPassIsEmpty(false);
        authProfileDetail.setJoinedTime("2016-10-09T09:21:06.572Z");
        authProfileDetail.setCurrency("DEFAULT");
        authProfileDetail.setAccountType(1);
        authProfileDetail.setIsAvailable(true);
        authProfileDetail.setUserLevel("normal");
        authProfileDetail.setCompanyWebsite("");
        authProfileDetail.setCompanySize(0);
        authProfileDetail.setMobile(null);
        authProfileDetail.setFeatureFlags(268);
        // authProfileDetail.setLogoFile(
        // "https://alauda-cn-jakiro.s3.cn-north-1.amazonaws.com.cn/user/alaudademo07/logo1505444937.jpg");
        authProfileDetail.setAppCreate(null);
        authProfileDetail.setRepoCreateTime("2016-12-12T05:19:41.668Z");
        authProfileDetail.setApiRevokedTime("2017-09-17T09:21:14.944Z");
        authProfileDetail.setReferenceCode("gbqjyk");
        authProfileDetail.setType("cn");
        authProfileDetail.setServiceRegionFlags(71);
        authProfileDetail.setApplyMode("");
        authProfileDetail.setApplyService("");
        authProfileDetail.setSource("alauda");
        // TODO ???????????????????????????
        // List<String> projects = authProfileDetail.getProjects();
        // List<String> projectNames =null;
        // if(null != projects){
        // try {
        // projectNames = getProjectValues(projects);
        // LOGGER.info("???????????????????????????[0]??????{}",projectNames.get(0));
        // } catch (Exception e) {
        // LOGGER.debug("??????????????????????????????");
        // }
        // }
        // if(null != projectNames){
        // authProfileDetail.setProjectNames(projectNames);
        // }
        return authProfileDetail;
    }

    // 0.2 ????????????????????????????????????????????????????????????
    // yangshilei
    /*
     * @Override public AuthProfileResponse updateAuthProfilePassword(@RequestBody
     * OrgsPasswordRequest userPasswordRequest,
     *
     * @RequestParam("username") String username, @PathVariable("namespace") String
     * namespace) {
     *
     * // ???requestBody??????????????????????????????????????? UpdateSubAccountPasswordRequest jacksonBaseParse
     * = PayloadParseUtil .jacksonBaseParse(UpdateSubAccountPasswordRequest.class,
     * userPasswordRequest);
     *
     * // ?????????????????????rbac????????????????????????????????????????????????????????? if
     * (StringUtils.isBlank(userPasswordRequest.getOldPassword()) ||
     * StringUtils.isBlank(userPasswordRequest.getPassword())) { throw new
     * BaseException(ResultErrorEnum.BAD_REQUEST, "Password cannot be empty"); }
     * orgsClient.updateSubAccountPassword(jacksonBaseParse, namespace, username);
     * // ??????rbac????????????????????????????????? GetOrgDetailResponse orgDetail =
     * orgsClient.getOrgDetail(namespace);
     *
     * AuthProfileResponse authProfileDetail =
     * PayloadParseUtil.jacksonBaseParse(AuthProfileResponse.class, orgDetail);
     * authProfileDetail.setRepoCreateTime(orgDetail.getCreatedAt());
     * authProfileDetail.setUsername(orgDetail.getName()); return authProfileDetail;
     * }
     */

    // 1.1 yangshilei
    // ???????????????????????????
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "subaccount", action = "view")
    @LogCodeDefine("02")
    public OrgsSubAccountsListResponse getOrgsSubAccounts(@PathVariable("org_name") String orgName,
                                                          @RequestParam(value = "username", required = false) String
                                                                  uuid,
                                                          @RequestParam(value = "search", required = false) String
                                                                      search,
                                                          @RequestParam(value = "order_by", required = false) String
                                                                      orderBy,
                                                          @RequestParam(value = "team_name_filter", required = false)
                                                                      String teamNameFilter,
                                                          @RequestParam(value = "assign", required = false,
                                                                  defaultValue = "false") Boolean assign,
                                                          @RequestParam(value = "page_size", required = false,
                                                                  defaultValue = "20") int pageSize,
                                                          @RequestParam(value = "page", required = false,
                                                                  defaultValue = "1") int currentPage,
                                                          @RequestParam(value = "project_name", required = false,
                                                                  defaultValue = "") String projectName) {

        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // 1.????????????????????????
//        LOGGER.debug("Get into the orginzation method 'getOrgsSubAccounts'---paramter>>>>???{}", orgName);
//        CompositeResource param = new CompositeResource();
//        param.setNamespace(orgName);
//        param.setType(reqCtx.getResType());
//        List<CompositeResource> queryResourceList = compositeResDao.queryResourceList(param);

//        if (null == queryResourceList || queryResourceList.size() == 0) {
//            return new OrgsSubAccountsListResponse();
//        }
//        LOGGER.debug("search for resources_resource,the List size is >>>>{}", queryResourceList.size());

        // 2.??????????????????????????????
//        if (StringUtils.isNotBlank(projectName) && !assign) {
//            CompositeResource projRes = compositeResDao.queryResourceByName(orgName, Constants.Resource.RES_PROJECT,
//                    projectName);
//            if (null != projRes) {
//                CompositeResource roleParam = new CompositeResource();
//                roleParam.setNamespace(orgName);
//                roleParam.setType(Constants.Resource.RES_ROLE);
//                roleParam.setProjectUuid(projRes.getUuid());
//                // ??????role?????????project????????????????????????
//                List<CompositeResource> roleResList = compositeResDao.queryResourceList(roleParam);
//                // ?????????????????????????????????
//                if (CollectionUtils.isEmpty(roleResList)) {
//                    return new OrgsSubAccountsListResponse();
//                }
//                String resAction = new StringBuffer(Constants.Resource.RES_ROLE).append(":view").toString();
//                List<CompositeResource> filterRoleList = resAuthHelper
//                        .filterResourceList(reqCtx.getUser(), resAction, roleResList, reqCtx.getFlattenConstraints())
//                        .getKey();
//                // ?????????????????????????????????
//                if (CollectionUtils.isEmpty(filterRoleList)) {
//                    return new OrgsSubAccountsListResponse();
//                }
//                List<String> roleUuidList = CompositeResource.getUuidList(filterRoleList);
//                List<ListRolesAssignedResponse> roleUserList = rbacClient.listRolesAssigned(orgName, roleUuidList,
//                        null);
//                if (CollectionUtils.isEmpty(roleUserList)) {
//                    return new OrgsSubAccountsListResponse();
//                }
//                LOGGER.debug("call rbacClient.listRolesAssigned return list>>>>>>>> : {}", roleUserList.size());
//                // ??????
//                queryResourceList = queryResourceList.stream().filter(res -> {
//                    return roleUserList.stream().anyMatch(roleUser -> {
//                        return res.getName().equalsIgnoreCase(roleUser.getUser());
//                    });
//                }).collect(Collectors.toList());
//            }
//        }

        // 3.??????rbac???????????????????????????????????????
        ListOrgAccountsResponse listOrgAccounts = orgsClient.listOrgAccounts(orgName, teamNameFilter, uuid, search,
                orderBy,
                pageSize, currentPage);
        if (null == listOrgAccounts || null == listOrgAccounts.getResults()) {
            return new OrgsSubAccountsListResponse();
        }
        LOGGER.info("call orgsClient.listOrgAccounts return ListOrgAccountsResponse>>> : {}",
                JSONObject.fromObject(listOrgAccounts));
//        List<AccountDTO> accList = listOrgAccounts.getResults();
        // 4.????????????????????????
//        queryResourceList = queryResourceList.stream().filter(res -> {
//            return accList.stream().anyMatch(acc -> {
//                return res.getName().equalsIgnoreCase(acc.getUsername());
//            });
//        }).collect(Collectors.toList());

        // ??????
        KeyValue<List<CompositeResource>, List<CompAuthFilterResponse>> filterResourceList = resAuthHelper
                .filterResourceList(reqCtx.getUser(), reqCtx.getResAction(), null,
                        reqCtx.getFlattenConstraints());
//        queryResourceList = filterResourceList.getKey();
//        if (CollectionUtils.isEmpty(queryResourceList)) {
//            return new OrgsSubAccountsListResponse();
//        }
//        LOGGER.debug("filter authResourceList return >>> : {}", queryResourceList.toString());

        // ??????
//        final int PAGENUMBER = 1;
//        if (currentPage < PAGENUMBER) {
//            currentPage = 1;
//        }
//        if (pageSize < PAGENUMBER) {
//            pageSize = 20;
//        }
//        int pageStart;
//        int pageEnd;
//        if (!assign) {
//            pageStart = (currentPage - 1) * pageSize;
//            pageEnd = currentPage * pageSize - 1;
//        } else {
//            pageStart = 0;
////            pageEnd = queryResourceList.size();
//        }
//
//        List<Result> resultlist = new ArrayList<>();
//        for (int i = 0; i < queryResourceList.size(); i++) {
//            if (i >= pageStart && i <= pageEnd) {
//                CompositeResource res = queryResourceList.get(i);
//                for (AccountDTO acc : accList) {
//                    // ?????????????????????????????????????????????
//                    if (res.getName().equalsIgnoreCase(acc.getUsername())) {
//                        resultlist.add(PayloadParseUtil.jacksonBaseParse(Result.class, acc));
//                        break;
//                    }
//                }
//            }
//        }
//        // ??????resource_actions
//        for (Result result : resultlist) {
//            for (CompAuthFilterResponse authItem : filterResourceList.getValue()) {
//                if (authItem.getResource() == null || authItem.getResource().getUuid() == null) {
//                    continue;
//                }
//                if (authItem.getResource().getName().equalsIgnoreCase(result.getUsername())) {
//                    result.setResultActions(authItem.getResTypeActionList());
//                    LOGGER.debug("Add actions to user>>>>");
//                }
//            }
//        }
        // TODO ?????????????????????project???????????????????????????
        // LOGGER.info("????????????????????????");
        // for(Result result : resultlist){
        // addProjectName(result);
        // LOGGER.info("???????????????????????????");
        // }
        // ????????????
        OrgsSubAccountsListResponse subAccounts = PayloadParseUtil.jacksonBaseParse(OrgsSubAccountsListResponse
                .class, listOrgAccounts);

        for (Result acc : subAccounts.getResults()) {
            UserCreateRequest req = new UserCreateRequest();
            req.setLdapId(acc.getUsername());
            List<UserDTO> dtoList = userClient.queryList(req);
            if (CollectionUtils.isNotEmpty(dtoList)) {
                acc.setUserInfo(PayloadParseUtil.jacksonBaseParse(UserResponse.class, dtoList.get(0)));
            }
            List<ListRolesResponse> rbacRoleList = rbacClient.listRoles(null, null, reqCtx.getUser().getNamespace(),
                    null, acc.getUsername());
            if (CollectionUtils.isNotEmpty(rbacRoleList)) {
                acc.setRoles(PayloadParseUtil.jacksonBaseParse(RbacRoleListItem.class, rbacRoleList));
            }
        }
//        subAccounts.setNumPages(currentPage);
//        if (!assign) {
//            subAccounts.setPageSize(pageSize);
//        } else {
//            subAccounts.setPageSize(queryResourceList.size());
//        }
//        subAccounts.setTotalCount(queryResourceList.size());
//        subAccounts.setResults(resultlist);
        return subAccounts;
    }
    // TODO ??????yml?????????????????????rbac???keys?????????????????????
    // private List<String> getProjectValues(List<String> projects) throws
    // Exception{
    // LOGGER.info("????????????yml????????????");
    // Yaml yml = new Yaml();
    // //?????????????????????application??????yml????????????
    // String urlPath = envObj.getProperty("composite.yamlUrl");
    // LOGGER.info("????????????yml?????????????????????{}",urlPath);
    // URL url = new URL(urlPath);
    // LOGGER.info("?????????URL????????????{}",url.getPath());
    // URLConnection openConnection = url.openConnection();
    //
    // //??????yml????????????
    // InputStream fis = openConnection.getInputStream();
    // Map map = (Map)yml.load(fis);
    // //????????????navigation?????????Map??????
    // Map navigation = (Map)map.get("navigation");
    // //????????????mappings?????????List??????
    // List list = (List)navigation.get("mappings");
    //
    // //??????????????????????????????????????????
    // List<String> projectNames = new ArrayList<String>();
    // LOGGER.info("??????????????????values?????????");
    // for(Object obj : list){
    // Map proKeyValue = (Map)obj;
    // //??????????????????rbac???key
    // String serviceName = (String)proKeyValue.get("serviceName");
    // //??????????????????yml??????value
    // String chName = (String)proKeyValue.get("chName");
    // LOGGER.info("???????????????{}",chName);
    // for(String projectKey : projects){
    // if(serviceName.equals(projectKey)){
    // projectNames.add(chName);
    // }
    // }
    // }
    // LOGGER.info("????????????yml????????????");
    // return projectNames;
    // }
    // TODO ??????yml??????????????????????????????????????????rbac???keys
    // private List<String> getProjectKeys(List<String> projectNames) throws
    // Exception{
    // LOGGER.info("????????????yml????????????");
    // Yaml yml = new Yaml();
    // //?????????????????????application??????yml????????????
    // String urlPath = envObj.getProperty("composite.yamlUrl");
    // LOGGER.info("????????????yml?????????????????????{}",urlPath);
    // URL url = new URL(urlPath);
    // LOGGER.info("?????????URL????????????{}",url.getPath());
    // URLConnection openConnection = url.openConnection();
    //
    // //??????yml????????????
    // InputStream fis = openConnection.getInputStream();
    // Map map = (Map)yml.load(fis);
    // //????????????navigation?????????Map??????
    // Map navigation = (Map)map.get("navigation");
    // //????????????mappings?????????List??????
    // List list = (List)navigation.get("mappings");
    //
    // //?????????????????????
    // List<String> projectKeys = new ArrayList<String>();
    // LOGGER.info("??????????????????values?????????");
    // for(Object obj : list){
    // Map proKeyValue = (Map)obj;
    // //??????????????????rbac???key
    // String serviceName = (String)proKeyValue.get("serviceName");
    // //??????????????????yml??????value
    // String chName = (String)proKeyValue.get("chName");
    // LOGGER.info("???????????????{}",chName);
    // for(String projectName : projectNames){
    // if(chName.equals(projectName)){
    // projectKeys.add(serviceName);
    // }
    // }
    // }
    // LOGGER.info("????????????yml????????????");
    // return projectKeys;
    // }
    // TODO ?????????????????????????????????
    // private void addProjectName(Result result){
    // List<String> projects = result.getProjects();
    // try {
    // List<String> projectNames = getProjectValues(projects);
    // if(null != projectNames){
    // result.setProjectNames(projectNames);
    // }
    // } catch (Exception e) {
    // LOGGER.debug("??????????????????????????????");
    // }
    // }

    // 1.2 yangshilei
    // ?????????????????????
    @Override
    @ResponseStatus(HttpStatus.CREATED)
    @ResAction(resType = "subaccount", action = "create")
    @LogCodeDefine("03")
    public List<OrgsCreateSubAccountsResponse> createOrgsSubAccounts(@PathVariable("org_name") String orgName,
                                                                     @PathVariable("user_id") String userId, @RequestBody OrgsCreateSubAccountsRequest req) {

        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        UserVO vo = userClient.findByPrimaryKey(userId);
        if (vo == null) {
            throw new BaseException("The username already exists", LastLogCodeEnum.RESOURCE_ALREADY_EXIST,
                    ResultErrorEnum.BIZ_RESOURCE_ALREADY_EXIST);
        }
        if (StringUtils.isNotEmpty(vo.getLdapId())) {
            throw new BaseException("The username is connected", LastLogCodeEnum.RESOURCE_ALREADY_EXIST,
                    ResultErrorEnum.BIZ_RESOURCE_ALREADY_EXIST);
        }
        // 1.????????????????????????
//        List<String> usernames = req.getUsernames();
        // ?????????????????????????????????????????????????????????????????????
//        for (String username : usernames) {
//            CompositeResource queryResourceByName = compositeResDao.queryResourceByName(orgName, reqCtx.getResType(),
//                    username);
//            LOGGER.debug("Qurey database 'resources_resource' success >>>>");
//            if (null != queryResourceByName) {
//                throw new BaseException("The user name already exists", LastLogCodeEnum.RESOURCE_ALREADY_EXIST,
//                        ResultErrorEnum.BIZ_RESOURCE_ALREADY_EXIST);
//            }
//        }

//         2.?????????????????????????????????20171220???
//        List<CompositeResource> userList = new ArrayList<CompositeResource>();
//        List<String> userNames = req.getUsernames();
//        CompositeResource composite = new CompositeResource();
//        for(String userName : userNames){
//            composite.setName(userName);
//            composite.setNamespace(orgName);
//            composite.setType(reqCtx.getResType());
//            userList.add(composite);
//        }

        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new CompositeResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());
//        for (CompositeResource user : userList) {
//            resAuthHelper.resourceActionVerify(reqCtx.getUser(), user, reqCtx.getResAction(),
//                    reqCtx.getFlattenConstraints());
//        } 

        // 3.??????rbac????????????
        // TODO ?????????????????????rbac?????????????????????20171107
        List<String> usernames2 = req.getUsernames();
        // List<String> emailList = req.getEmailList();
        // ????????????????????????values??????keys
        // List<String> projectsNames = req.getProjectNames();
        // List<String> projects2 = null;
        // if(null != projectsNames){
        // try {
        // projects2 = getProjectKeys(projectsNames);
        // LOGGER.info("????????????????????????????????????");
        // } catch (Exception e1) {
        // LOGGER.info("???????????????keys??????yml??????????????????values");
        // }
        // }

        // ??????????????????????????????????????????????????????????????????????????????
        List<InsertAccount> accounts = new ArrayList<InsertAccount>();
        for (int i = 0; i < usernames2.size(); i++) {
            InsertAccount user = new InsertAccount();
            user.setUsername(usernames2.get(i));
            user.setDescription(req.getDescription());
            user.setType(req.getType());
            if (StringUtils.isNotEmpty(vo.getMail())) {
                user.setEmail(vo.getMail());
            }
            if (StringUtils.isNotEmpty(vo.getMobile())) {
                user.setMobile(vo.getMobile());
            }
            // user.setEmail(emailList.get(i));
            // if(null != projects2){
            // user.setProjects(projects2);
            // }
            accounts.add(user);
        }
        CreateOrgAccount createOrgAccounts = new CreateOrgAccount();
        createOrgAccounts.setAccounts(accounts);
        createOrgAccounts.setRoles(req.getRoles());
        createOrgAccounts.setPassword(req.getPassword());

        CreateOrgAccountRequest jacksonBaseParse = PayloadParseUtil.jacksonBaseParse(CreateOrgAccountRequest.class,
                createOrgAccounts);

        jacksonBaseParse.setUserId(userId);
        List<CreateOrgAccountResponse> createOrgAccount = orgsClient.createOrgAccount(jacksonBaseParse, orgName);
        LOGGER.info("Create subaccount successful;Return the length of collention is >>>>>>>>???{}",
                createOrgAccount.size());

        // ?????????????????????????????????BPM
        Map<String,String> mp = new HashMap<>();
        if(!req.getUsernames().isEmpty()) {
            mp.put("loginName",req.getUsernames().get(0));
            mp.put("password",req.getPassword());
            mp.put("name",vo.getName());
            mp.put("mobile",vo.getMobile());
            mp.put("email",vo.getMail());
        }
        rbacAsynToBpmService.syncUserToBpm(mp,"ADD");

        // ????????????????????????????????????model
        List<OrgsCreateSubAccountsResponse> OrgsSubAccountList = PayloadParseUtil
                .jacksonBaseParse(OrgsCreateSubAccountsResponse.class, createOrgAccount);
        // TODO ??????????????????
        // for(OrgsCreateSubAccountsResponse user : OrgsSubAccountList){
        // List<String> projects = user.getProjects();
        // try {
        // List<String> projectNames = getProjectValues(projects);
        // if(null != projectNames){
        // user.setProjectNames(projectNames);
        // }
        // } catch (Exception e) {
        // LOGGER.debug("??????????????????????????????");
        // }
        // }
        // 3.??????????????????resources_resource????????????
//        for (String username : usernames) {
//            CompositeResource resource = new CompositeResource();
//            resource.setNamespace(orgName);
//            resource.setCreatedBy(orgName);
//            resource.setType(reqCtx.getResType());
//            resource.setName(username);
//            // ???????????????uuid???2017.11.06
//            String userUuid = UUID.randomUUID().toString();
//            resource.setUuid(userUuid);
//            resource.setCreatedAt(new Date());
//            compositeResDao.insertCompositeResource(resource);
//            LOGGER.info("Add user to resources_resource success >>>>>");
//        }

        // ????????????
//        List<OrgsRoles> roleList = req.getRoles();
//        List<String> usernameList = req.getUsernames();
//        List<LogEventPayload> logEventList = new ArrayList<LogEventPayload>();
//        for (String subAccount : usernameList) {
//            if (roleList.isEmpty()) {
//                LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(reqCtx, "organization", orgName, orgName,
//                        "add", 1, "generic-svoa", null);
//                LogEventUtil.popupDetail(logEvent, "attribute_type", "member");
//                LogEventUtil.popupDetail(logEvent, "attribute", subAccount);
//                logEventList.add(logEvent);
//            } else {
//                for (OrgsRoles role : roleList) {
//                    // ??????????????????
//                    LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(reqCtx, "role", role.getUuid(),
//                            role.getName(), "add", 0, "generic-svoa", null);
//                    LogEventUtil.popupDetail(logEvent, "attribute_type", "member");
//                    LogEventUtil.popupDetail(logEvent, "attribute", subAccount);
//                    logEventList.add(logEvent);
//                }
//            }
//        }
//        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEventList.toArray(new LogEventPayload[0]));
//        logClient.saveEventsLogInfo(jacksonJson);

        LOGGER.info("Return object is >>>>>>{}???", OrgsSubAccountList);
        return OrgsSubAccountList;
    }


    @ResponseStatus(HttpStatus.CREATED)
    @ResAction(resType = "subaccount", action = "update")
    @Override
    public BaseResponse updateOrgsSubAccounts(@PathVariable("username") String username,
                                              @PathVariable("user_id") String userId,
                                              @RequestBody OrgsUpdateAccountsRequest req) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new CompositeResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());

        UserVO vo = userClient.findByPrimaryKey(userId);

        UpdateSubAccountRequest request = PayloadParseUtil.jacksonBaseParse(UpdateSubAccountRequest.class, req);
        String namespace = reqCtx.getUser().getNamespace();
        if (vo != null && StringUtils.isNotEmpty(vo.getMail())) {
            request.setMail(vo.getMail());
        }
        if (vo != null && StringUtils.isNotEmpty(vo.getMobile())) {
            request.setMobile(vo.getMobile());
        }
        request.setUserId(userId);
        orgsClient.updateSubAccount(request, namespace, username);

        // ??????????????????????????????BPM
        Map<String,String> mp = new HashMap<>();
        if(null != vo) {
            mp.put("loginName",username);
            mp.put("password",req.getPassword());
            mp.put("name",vo.getName());
            mp.put("mobile",vo.getMobile());
            mp.put("email",vo.getMail());
        }
        rbacAsynToBpmService.syncUserToBpm(mp,"UPDATE");

        //?????????????????????????????????
        List<RolesRevokeRequest> exist = new ArrayList<>();
        RolesRevokeRequest e = new RolesRevokeRequest();
        e.setUser(username);
        e.setNamespace(namespace);
        exist.add(e);
        rbacClient.rolesRevoke(exist);

        //???????????????????????????
        List<OrgsAssignRolesRequest> roleIds = req.getRoles().stream().map(input -> {
            OrgsAssignRolesRequest out = new OrgsAssignRolesRequest();
            out.setRoleId(input.getUuid());
            out.setRoleName(input.getName());
            return out;
        }).collect(Collectors.toList());
        assignOn(username, roleIds);

        return new BaseResponse();
    }

    /**
     * ??????: /orgs/{org_name}/accounts/{username} GET ??????: Used to get one specific
     * user's details
     *
     * @author zhangqing
     * @see com.migu.tsg.microservice.atomicservice.composite.service
     * rbac.ICompRbacOrgService#getSubAccountDetail(java.lang.String,
     * java.lang.String)
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @LogCodeDefine("04")
    @ResAction(resType = "subaccount", action = "view")
    public OrgsSubAccountDetailResponse getSubAccountDetail(@PathVariable("org_name") String orgName,
                                                            @PathVariable("username") String username) {
        LOGGER.debug("1.Enter into  RbacOrgsSubAccountController.getSubAccountDetail()>>>>>>");
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ?????????????????????????????????URL?????????namespace,???????????????????????????????????????
        RequestHeadUser requestHeader = reqCtx.getUser();
        String namespace = requestHeader.getNamespace();

        LOGGER.debug("2.Authority identification >>>>>>");
//        CompositeResource subAccount = compositeResDao.queryResourceByName(namespace, reqCtx.getResType(), username);
//        if (subAccount == null) {
//            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
//        }
        resAuthHelper.resourceActionVerify(requestHeader, new RbacResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());

        LOGGER.debug("3.Authority identification success; Call rbac service 'OrgsService.java' to get user >>>>>>");
        OrgsSubAccountDetailResponse response2Client;
        if (requestHeader.isAdmin()) {
            GetOrgDetailResponse response = this.orgsClient.getOrgDetail(namespace);
            response2Client = PayloadParseUtil.jacksonBaseParse(OrgsSubAccountDetailResponse.class, response);
            LOGGER.debug("administrator account >>>>>???{}", response.getName());
        } else {
            GetOrgUserDetailResponse getOrgUserDetailResponse = orgsClient.getOrgUserDetail(namespace, username);
            response2Client = PayloadParseUtil.jacksonBaseParse(OrgsSubAccountDetailResponse.class,
                    getOrgUserDetailResponse);
            LOGGER.debug("Administrator account is >>>>>>>: {}", getOrgUserDetailResponse.getUsername());
        }
        // ?????????????????????????????????
        List<String> actions = resAuthHelper.resourceActions(reqCtx.getUser(), reqCtx.getResType(),
                reqCtx.getFlattenConstraints(), new RbacResource());
//        response2Client.setResource_actions(actions);
        // TODO ??????rbac??????????????????keys????????????????????????????????????values???????????????????????????
        // List<String> projects = response2Client.getProjects();
        // try {
        // List<String> projectNames = getProjectValues(projects);
        // if(null != projectNames){
        // response2Client.setProjectNames(projectNames);
        // }
        // } catch (Exception e) {
        // LOGGER.info("??????????????????");
        // }
        // LOGGER.info("????????????RbacOrgsSubAccountController.getSubAccountDetail()");
        return response2Client;
    }

    /**
     * ??????: /orgs/{org_name}/accounts/{username} PUT ??????: Used to update subaccounts
     * data. Currently only supports updating the password If the user updating the
     * user account's password. Only admin users can use this method. The user can
     * also use it to update his/her own password but needs to provide
     * **old_password**
     *
     * @author zhangqing Date:2017???9???14?????????12:19:15
     * @see com.migu.tsg.microservice.atomicservice.composite.service.rbac
     * ICompRbacOrgService#updateSubAccountPassword(java.lang.String,java.lang.String,
     * com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.OrgsPasswordRequest)
     */
    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "subaccount", action = "update_password")
    @LogCodeDefine("05")
    public BaseResponse updateSubAccountPassword(@PathVariable("org_name") String orgName,
                                                 @PathVariable("username") String username, @RequestBody
                                                             OrgsPasswordRequest passwordRequest) {
        LOGGER.debug("Enter into RbacOrgsSubAccountController.updateSubAccountPassword() >>>>>");
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ?????????????????????????????????URL?????????namespace,???????????????????????????????????????
        RequestHeadUser requestHeader = reqCtx.getUser();
        String namespace = requestHeader.getNamespace();
//        String oldPassword = passwordRequest.getOldPassword();
//        String password = passwordRequest.getPassword();
//        LOGGER.debug("oldpassword= >>>>" + oldPassword + ",password= >>>>>>>>" + password);

//        CompositeResource subAccount = compositeResDao.queryResourceByName(namespace, reqCtx.getResType(), username);
//        if (subAccount == null) {
//            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
//        }
        resAuthHelper.resourceActionVerify(requestHeader, new CompositeResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());

        LOGGER.debug("3.Authority identification success; update password >>>>");
        UpdateSubAccountPasswordRequest updateSubAccountPasswordRequest = PayloadParseUtil
                .jacksonBaseParse(UpdateSubAccountPasswordRequest.class, passwordRequest);
        this.orgsClient.updateSubAccountPassword(updateSubAccountPasswordRequest, namespace, username);

        LOGGER.debug("4.SubAccount password update success>>>>");
//        CompositeResource org = compositeResDao.queryResourceByName(namespace, "organization", namespace);
//        if (null == org) {
//            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
//        }
//        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(reqCtx, "organization", orgName, org.getName(),
//                "update", 1, "generic-svoa", org);
//        LogEventUtil.popupDetail(logEvent, "attribute_type", "member");
//        LogEventUtil.popupDetail(logEvent, "attribute", username);
//        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
//        logClient.saveEventsLogInfo(jacksonJson);
        return new BaseResponse();
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "subaccount", action = "update_enable")
    @LogCodeDefine("05")
    @Override
    public BaseResponse updateByKeycloak(UpdateBykeycloakPayload updateByKeycloakrest) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        RequestHeadUser requestHeader = reqCtx.getUser();
        resAuthHelper.resourceActionVerify(requestHeader, new CompositeResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());
        UpdateBykeycloakRequest request = PayloadParseUtil.jacksonBaseParse(UpdateBykeycloakRequest.class,
                updateByKeycloakrest);
        orgsClient.updateByKeycloak(request);
        return new BaseResponse();
    }

    /**
     * ??????: /orgs/{org_name}/accounts/{username} DELETE ??????: Used to delete users
     * inside the org
     *
     * @author zhangqing Date:2017???9???14?????????4:15:31
     * @see com.migu.tsg.microservice.atomicservice.composite.service.rbac
     * ICompRbacOrgService#deleteSubAccount(java.lang.String, java.lang.String)
     */
    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "subaccount", action = "delete")
    @LogCodeDefine("06")
    public BaseResponse deleteSubAccount(@PathVariable("org_name") String orgName,
                                         @PathVariable("username") String username) {
        LOGGER.debug("Enter into RbacOrgsSubAccountController.deleteSubAccount() >>>>>>>");
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ?????????????????????????????????URL?????????namespace,???????????????????????????????????????
        RequestHeadUser requestHeader = reqCtx.getUser();
        String namespace = requestHeader.getNamespace();

        LOGGER.debug("2.Authentication authority >>>>>");
//        CompositeResource subAccount = compositeResDao.queryResourceByName(namespace, reqCtx.getResType(), username);
//        if (subAccount == null) {
//            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
//        }
        resAuthHelper.resourceActionVerify(requestHeader, new CompositeResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());

        LOGGER.debug("3.Authentication authority success; 'OrgsService.java'>>>>>>>");
        this.orgsClient.removeOrgAccount(namespace, username);

//        this.compositeResDao.removeCompositeResource(namespace, reqCtx.getResType(), subAccount.getUuid());

//        LOGGER.info("4.Delete subaccount success, add events >>>>>");
//        CompositeResource org = compositeResDao.queryResourceByName(namespace, "organization", namespace);
//        if (null == org) {
//            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
//        }
//        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(reqCtx, "organization", orgName, org.getName(),
//                "delete", 1, "generic-svoa", subAccount);
//        LogEventUtil.popupDetail(logEvent, "attribute_type", "member");
//        LogEventUtil.popupDetail(logEvent, "attribute", username);
//        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
//        logClient.saveEventsLogInfo(jacksonJson);
        return new BaseResponse();
    }

    // 4.1 yangshilei
    // ???????????????????????????
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "view")
    @LogCodeDefine("07")
    public List<OrgsRolesPermissionResponse> getPermissionOnRoles(@PathVariable("org_name") String orgName,
                                                                  @PathVariable("username") String username) {

        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        // ????????????????????????
        CompositeResource param = new CompositeResource();
        param.setNamespace(orgName);
        param.setType(authCtx.getResType());
        List<CompositeResource> roleList = compositeResDao.queryResourceList(param);
        if (null == roleList || roleList.size() == 0) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        List<String> roleUuids = new ArrayList<String>();
        for (CompositeResource com : roleList) {
            roleUuids.add(com.getUuid());
        }

        // ??????orgName???user???roles,??????rbac????????????
        List<ListRolesAssignedResponse> listAccountRoles = rbacClient.listRolesAssigned(orgName, roleUuids, username);
        // List<ListAccountRolesResponse> listAccountRoles =
        // orgsClient.listAccountRoles(orgName, username);
        LOGGER.debug("Get the length of the roles under the subaccount >>>>>>>???{}", listAccountRoles.size());

        // ???????????????????????????????????????
        List<RbacResource> rbacResList = PayloadParseUtil.parse2RbacResource(listAccountRoles);
        List<CompAuthFilterResponse> filterRbacResourceList = resAuthHelper.filterRbacResourceList(authCtx.getUser(),
                authCtx.getResAction(), rbacResList, authCtx.getFlattenConstraints());

        // ???ListAccountRolesResponse?????????OrgsRolesPermissionResponse
        List<OrgsRolesPermissionResponse> orgsRolesPermissions = PayloadParseUtil
                .listAccount2OrgsRoles(listAccountRoles);
        LOGGER.debug(" Return the result of search for rbac >>>>>>:{}", orgsRolesPermissions.size());
        // ???resourcesMixedFilter???actions???????????????rbac??????????????????????????????
        for (OrgsRolesPermissionResponse orgsRolesPermission : orgsRolesPermissions) {
            for (CompAuthFilterResponse comp : filterRbacResourceList) {
                if (orgsRolesPermission.getRoleName().equals(comp.getResource().getName())) {
                    orgsRolesPermission.setResourceActions(comp.getResTypeActionList());
                    LOGGER.debug("Add actions success>>>>>>>");
                }
            }
        }
        return orgsRolesPermissions;
    }

    /**
     * ??????: /orgs/{org_name}/accounts/{username}/roles POST ??????: assign role to a user
     *
     * @author zhangqing Date:2017???9???14?????????4:18:19
     * @see com.migu.tsg.microservice.atomicservice.composite.service
     * rbac.ICompRbacOrgService#assignRoleToSubAccount(java.lang.String,
     * java.lang.String, java.util.List)
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "assign")
    @LogCodeDefine("08")
    public List<OrgsAssignRolesResponse> assignRoleToSubAccount(@PathVariable("org_name") String orgName,
                                                                @PathVariable("username") String username,
                                                                @RequestBody List<OrgsAssignRolesRequest> roleNames) {
        LOGGER.debug("Welcome into assignRoleToSubAccount>>>>>>>");
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ?????????????????????????????????URL?????????namespace,???????????????????????????????????????
        RequestHeadUser requestHeader = reqCtx.getUser();
        String namespace = requestHeader.getNamespace();
        List<String> roleNameList = new ArrayList<String>(roleNames.size());
        for (OrgsAssignRolesRequest assign : roleNames) {
            String role = assign.getRoleName();
            roleNameList.add(role);
        }

        // ????????????
//        List<CompositeResource> roleResList = this.compositeResDao.queryResourcesByNameList(orgName,
//                reqCtx.getResType(), roleNameList);

        resAuthHelper.resourceActionVerify(requestHeader, new CompositeResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());
//        for (CompositeResource resource : roleResList) {
//            resAuthHelper.resourceActionVerify(requestHeader, resource, reqCtx.getResAction(),
//                    reqCtx.getFlattenConstraints());
//        }

        // ??????????????????????????????
//        List<LogEventPayload> logEventList = new ArrayList<LogEventPayload>();// ????????????????????????
        List<RolesAssignedRequest> request2atomicList = new ArrayList<RolesAssignedRequest>(roleNames.size());
        for (OrgsAssignRolesRequest resource : roleNames) {
            String roleName = resource.getRoleName();
            String roleUuid = resource.getRoleId();
            RolesAssignedRequest request2atomic = new RolesAssignedRequest(roleUuid, roleName, username, namespace);
            request2atomicList.add(request2atomic);
//            LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(reqCtx, "role", roleUuid, roleName, "add", 0,
//                    "generic-svoa", resource);
//            LogEventUtil.popupDetail(logEvent, "attribute_type", "member");
//            LogEventUtil.popupDetail(logEvent, "attribute", username);
//            logEventList.add(logEvent);
        }
        // ??????????????????
//        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEventList.toArray(new LogEventPayload[0]));
//        logClient.saveEventsLogInfo(jacksonJson);

        List<RolesAssignedResponse> responseFromAtomicList = this.rbacClient.rolesAssigned(request2atomicList);
        List<OrgsAssignRolesResponse> OrgsSubAccountList = PayloadParseUtil
                .jacksonBaseParse(OrgsAssignRolesResponse.class, responseFromAtomicList);
        return OrgsSubAccountList;
    }

    /**
     * ???????????????????????????(????????????id??????)
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "assign")
    @LogCodeDefine("08")
    public List<OrgsAssignRolesResponse> assignRoleToAccount(@PathVariable("org_name") String orgName,
                                                             @PathVariable("username") String username, @RequestBody
                                                                         List<OrgsAssignRolesRequest> roleIds) {
        LOGGER.debug("Welcome into assignRoleToSubAccount>>>>>>>");
        List<RolesAssignedResponse> responseFromAtomicList = assignOn(username, roleIds);
        List<OrgsAssignRolesResponse> OrgsSubAccountList = PayloadParseUtil
                .jacksonBaseParse(OrgsAssignRolesResponse.class, responseFromAtomicList);
        return OrgsSubAccountList;
    }

    private List<RolesAssignedResponse> assignOn(String username, List<OrgsAssignRolesRequest> roleIds) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ?????????????????????????????????URL?????????namespace,???????????????????????????????????????
        RequestHeadUser requestHeader = reqCtx.getUser();
        String namespace = requestHeader.getNamespace();

        // ??????????????????????????????
        List<LogEventPayload> logEventList = new ArrayList<LogEventPayload>();// ????????????????????????
        List<RolesAssignedRequest> request2atomicList = new ArrayList<RolesAssignedRequest>(roleIds.size());
        for (OrgsAssignRolesRequest resource : roleIds) {
            String roleUuid = resource.getRoleId();
            String roleName = resource.getRoleName();
            RolesAssignedRequest request2atomic = new RolesAssignedRequest(roleUuid, roleName, username, namespace);
            request2atomicList.add(request2atomic);

        }
        // ??????????????????
//        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEventList.toArray(new LogEventPayload[0]));
//        logClient.saveEventsLogInfo(jacksonJson);

        List<RolesAssignedResponse> responseFromAtomicList = this.rbacClient.rolesAssigned(request2atomicList);
        return responseFromAtomicList;
    }

    /**
     * ??????: /orgs/{org_name} GET ??????: Get details regarding one organization (requires
     * user to be inside the org)
     *
     * @author zhangqing Date:2017???9???14?????????4:22:30
     * @see com.migu.tsg.microservice.atomicservice.composite.service.rbac
     * ICompRbacOrgService#retrieveOrgsDetail(java.lang.String)
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "organization", action = "view")
    @LogCodeDefine("09")
    public OrgsDetailResponse retrieveOrgsDetail(@PathVariable("org_name") String orgName) {
        LOGGER.debug("Welcome into RbacOrgsSubAccountController.retrieveOrgsDetail()");
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ?????????????????????????????????URL?????????namespace,???????????????????????????????????????
        RequestHeadUser requestHeader = reqCtx.getUser();

//        CompositeResource organization = compositeResDao.queryResourceByName(namespace, reqCtx.getResType(),
// namespace);
//        if (organization == null) {
//            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
//        }
        resAuthHelper.resourceActionVerify(requestHeader, new RbacResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());

        LOGGER.debug("3.Authentication authority success???retrieveOrgsDetail>>>>> ");
        GetOrgDetailResponse response = this.orgsClient.getOrgDetail(orgName);
        OrgsDetailResponse orgsDetailResponse = PayloadParseUtil.jacksonBaseParse(OrgsDetailResponse.class, response);

        UserCreateRequest req = new UserCreateRequest();
        String username = orgsDetailResponse.getUsername();
        req.setLdapId(username);
        List<UserDTO> dtoList = userClient.queryList(req);
        if (CollectionUtils.isNotEmpty(dtoList)) {
            orgsDetailResponse.setUserInfo(PayloadParseUtil.jacksonBaseParse(UserResponse.class, dtoList.get(0)));
        }
        List<ListRolesResponse> rbacRoleList = rbacClient.listRoles(null, null, reqCtx.getUser().getNamespace(),
                null, username);
        if (CollectionUtils.isNotEmpty(rbacRoleList)) {
            orgsDetailResponse.setRoles(PayloadParseUtil.jacksonBaseParse(RbacRoleListItem.class, rbacRoleList));
        }
        // TODO ???????????????keys???????????????
        // List<String> projects = orgsDetailResponse.getProjects();
        // List<String> projectNames =null;
        // if(null != projects){
        // try {
        // projectNames = getProjectValues(projects);
        // LOGGER.info("???????????????????????????[0]??????{}",projectNames.get(0));
        // } catch (Exception e) {
        // LOGGER.debug("??????????????????????????????");
        // }
        // }
        // if(null != projectNames){
        // orgsDetailResponse.setProjectNames(projectNames);
        // }
        return orgsDetailResponse;
    }

    /**
     * ??????: /orgs/{org_name} PUT ??????: Updates org's company name or email(requires
     * admin privilege)
     *
     * @author zhangqing Date:2017???9???14?????????4:23:18
     * @see com.migu.tsg.microservice.atomicservice.composite.service.rbac
     * ICompRbacOrgService#companyName(java.lang.String,
     * com.migu.tsg.microservice.atomicservice.composite.service.rbac.
     * payload.OrgsCompanyRequest)
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "organization", action = "update")
    @LogCodeDefine("10")
    public OrgsCompanyNameResponse companyName(@PathVariable("org_name") String orgName,
                                               @RequestBody OrgsCompanyRequest company) {
        LOGGER.info("Welcome into update companyName()");
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ?????????????????????????????????URL?????????namespace,???????????????????????????????????????
        RequestHeadUser requestHeader = reqCtx.getUser();
        String namespace = requestHeader.getNamespace();

        CompositeResource organization = compositeResDao.queryResourceByName(namespace, reqCtx.getResType(), namespace);
        if (organization == null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST);
        }
        resAuthHelper.resourceActionVerify(requestHeader, organization, reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());

        LOGGER.info("3.Authentication authority success,update organization name >>>>>");
        UpdateOrgRequest updateOrgRequest = PayloadParseUtil.jacksonBaseParse(UpdateOrgRequest.class, company);
        UpdateOrgResponse responseFromAtomic = this.orgsClient.updateOrg(updateOrgRequest, orgName);
        OrgsCompanyNameResponse response2Client = PayloadParseUtil.jacksonBaseParse(OrgsCompanyNameResponse.class,
                responseFromAtomic);
        //???????????? *20171228
        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(reqCtx, reqCtx.getResType(), organization.getUuid(),
                organization.getName(), reqCtx.getRawAction(), 1, "generic", organization);
        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
//        logClient.saveEventsLogInfo(jacksonJson);
        return response2Client;
    }

    @Override
    @ResponseStatus(HttpStatus.OK)
    @LogCodeDefine("11")
    public BaseResponse updateLogo(@PathVariable("namespace") String namespace,
                                   @RequestBody OrgsLogoFilesRequest file) {
        LOGGER.info("1???Welcome into updateLogo>>>>>>>");
        File files = file.getFiles();
        FileUpload jacksonBaseParse = PayloadParseUtil.jacksonBaseParse(FileUpload.class, files);
        orgsClient.fileUpload(jacksonBaseParse, namespace);
        LOGGER.info("2???update logo success >>>>>");
        //???????????? *20171228
//        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
//        CompositeResource organization = compositeResDao.queryResourceByName(namespace, "organization", namespace);
//        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(reqCtx, "organization", organization.getUuid(),
//        		organization.getName(), "update", 1, "generic", organization);
//        String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEvent);
//        logClient.saveEventsLogInfo(jacksonJson);
        return new BaseResponse();
    }
    /**
     * updateSubAccount:(?????????????????????????????????????????????). <br/>
     * TODO(?????????????????????????????????).<br/>
     *
     * ????????? yangshilei
     *
     * @param namespace
     * @param username
     * @param request
     * @return
     */
    // @Override
    // public UpdateAccountResponse updateSubAccount(@PathVariable("org_name")String
    // namespace,
    // @PathVariable("username")String username,
    // @RequestBody UpdateSubAccount request) {
    // LOGGER.debug("???????????????????????????");
    // UpdateSubAccountRequest requestUser = PayloadParseUtil.jacksonBaseParse(
    // UpdateSubAccountRequest.class, request);
    // //1.????????????????????????????????????keys????????????rbac?????????
    // List<String> projectKeys = null;
    // if(null != request.getProjectNames()){
    // try {
    // projectKeys = getProjectKeys(request.getProjectNames());
    // } catch (Exception e) {
    // LOGGER.info("?????????????????????????????????");
    // }
    // }
    // requestUser.setProjects(projectKeys);
    //
    // //2.??????rbac???????????????????????????
    // UpdateSubAccountResponse updateSubAccount =
    // orgsClient.updateSubAccount(requestUser, namespace, username);
    // LOGGER.debug("??????rbac??????????????????????????????????????????{}",updateSubAccount.getEmail());
    // UpdateAccountResponse jacksonBaseParse2 = PayloadParseUtil.jacksonBaseParse(
    // UpdateAccountResponse.class, updateSubAccount);
    //
    // //3.???????????????????????????keys???????????????yml????????????????????????values
    // List<String> projects = jacksonBaseParse2.getProjects();
    // List<String> projectNames = null;
    // if(null != projects){
    // try {
    // projectNames = getProjectValues(projects);
    // } catch (Exception e) {
    // LOGGER.debug("????????????????????????");
    // }
    // }
    // if(projectNames != null){
    // jacksonBaseParse2.setProjectNames(projectNames);
    // }
    //
    // return jacksonBaseParse2;
    // }

    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "role", action = "assign")
    @LogCodeDefine("08")
    public List<OrgsAssignRolesResponse> assignUserRoles(@RequestBody OrgsUpdateAccountsRequest req) {
        LOGGER.debug("Welcome into batchAssignRoleToSubAccount>>>>>>>");
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ?????????????????????????????????URL?????????namespace,???????????????????????????????????????
        RequestHeadUser requestHeader = reqCtx.getUser();
        String namespace = requestHeader.getNamespace();

        resAuthHelper.resourceActionVerify(requestHeader, new CompositeResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());

        List<RolesAssignedRequest> rbacRequestList = Lists.newArrayList();
        List<LogEventPayload> logEventList = Lists.newArrayList();
        List<OrgsRoles> roles = req.getRoles();
        List<String> userNames = req.getUserNames();
        if (CollectionUtils.isEmpty(roles)) {
            return Lists.newArrayList();
        }

        // ?????????????????????????????????
        List<RolesRevokeRequest> exist = new ArrayList<>();

        for (OrgsRoles role : roles) {
            String roleUuid = role.getUuid();
            String roleName = role.getName();
            for (String userName : userNames) {
                RolesAssignedRequest rbacRequest = new RolesAssignedRequest(roleUuid, roleName, userName, namespace);
                rbacRequestList.add(rbacRequest);

                RolesRevokeRequest e = new RolesRevokeRequest();
                e.setUser(userName);
                e.setNamespace(namespace);
                e.setRoleUuid(roleUuid);
                exist.add(e);

                // LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(reqCtx, "role", roleUuid, roleName, "add", 1,
                // "generic-svoa", new CompositeResource());
                // LogEventUtil.popupDetail(logEvent, "attribute_type", "member");
                // LogEventUtil.popupDetail(logEvent, "attribute", userName);
                // logEventList.add(logEvent);
            }
        }
        // ????????? ?????????
        rbacClient.rolesRevoke(exist);
        List<RolesAssignedResponse> responseFromAtomicList = rbacClient.rolesAssigned(rbacRequestList);
        List<OrgsAssignRolesResponse> OrgsSubAccountList = PayloadParseUtil.jacksonBaseParse(OrgsAssignRolesResponse.class,
                responseFromAtomicList);

        // String jacksonJson = LogEventUtil.wrapLogEvents2Json(logEventList.toArray(new LogEventPayload[0]));
        // LOGGER.info("Assign users to roles???{}", jacksonJson);
        // logClient.saveEventsLogInfo(jacksonJson);

        return OrgsSubAccountList;
    }

    @Override
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResAction(resType = "role", action = "revoke")
    @LogCodeDefine("58")
    public BaseResponse revokeUserRoles(@RequestBody OrgsUpdateAccountsRequest req) {
        LOGGER.debug("Welcome into batchUnAssignRoleToSubAccount>>>>>>>");
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ?????????????????????????????????URL?????????namespace,???????????????????????????????????????
        RequestHeadUser requestHeader = reqCtx.getUser();
        String namespace = requestHeader.getNamespace();

        resAuthHelper.resourceActionVerify(requestHeader, new CompositeResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());
        List<RolesRevokeRequest> rbacRequestList = Lists.newArrayList();
        List<LogEventPayload> logEventList = Lists.newArrayList();
        List<OrgsRoles> roles = req.getRoles();
        List<String> userNames = req.getUserNames();
        for (OrgsRoles role : roles) {
            String roleName = role.getName();
            String roleUuid = role.getUuid();
            for (String userName : userNames) {
                RolesRevokeRequest rbacRequest = new RolesRevokeRequest(roleUuid, roleName, userName, namespace);
                rbacRequestList.add(rbacRequest);
            }
        }
        rbacClient.rolesRevoke(rbacRequestList);

        return new BaseResponse();
    }
}
