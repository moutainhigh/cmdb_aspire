package com.migu.tsg.microservice.atomicservice.composite.controller.project;

import static com.migu.tsg.microservice.atomicservice.composite.controller.util.PayloadParseUtil.jacksonBaseParse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.migu.tsg.microservice.atomicservice.architect.dto.CreateProjectTemplateRequest;
import com.migu.tsg.microservice.atomicservice.architect.dto.CreateProjectTemplateResponse;
import com.migu.tsg.microservice.atomicservice.architect.dto.FetchProjectTemplateResponse;
import com.migu.tsg.microservice.atomicservice.architect.dto.model.ProjectTemplateResourceDTO;
import com.migu.tsg.microservice.atomicservice.architect.dto.model.ProjectTemplateResourceParentDTO;
import com.migu.tsg.microservice.atomicservice.architect.dto.model.ProjectTemplateRoleDTO;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.log.LogClientService;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.log.payload.LogEventPayload;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.project.TemplatesServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestConstraintEnum;
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
import com.migu.tsg.microservice.atomicservice.composite.service.project.ICompProjectTemplateService;
import com.migu.tsg.microservice.atomicservice.composite.service.project.payload.CompProjectTemplateCreateRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.project.payload.CompProjectTemplateDetailResponse;

/**
 * ????????????controller Project Name:composite-service File
 * Name:CompProjectTemplateController.java Package
 * Name:com.migu.tsg.microservice.atomicservice.composite.controller.project
 * ClassName: CompProjectTemplateController <br/>
 * date: 2017???9???29??? ??????10:41:43 <br/>
 * ????????????controller
 * 
 * @author baiwp
 * @version
 * @since JDK 1.6
 */
@RestController
@LogCodeDefine("1050120")
public class CompProjectTemplateController implements ICompProjectTemplateService {

    @Autowired
    private CompositeResourceDao compositeResDao;

    @Autowired
    private TemplatesServiceClient templatesClient;

    public static final int NAMELENGTH = 1;
    
    /**
     * ???????????????
     */
    @Autowired
    private ResourceAuthHelper resAuthHelper;

    @Autowired
    private LogClientService logClient;

    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(action = "view", resType = "project_template")
    @LogCodeDefine("31")
    public List<CompProjectTemplateDetailResponse> listProjectTemplates(@PathVariable("namespace") String namespace) {
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();

        CompositeResource queryParam = new CompositeResource();
        queryParam.setType(authCtx.getResType());
        List<CompositeResource> compProjTempList = compositeResDao.queryResourceList(queryParam);

        // ???composite?????????????????????????????????????????????????????????
        if (CollectionUtils.isEmpty(compProjTempList)) {
            return new ArrayList<>();
        }

        List<String> uuids = CompositeResource.getUuidList(compProjTempList);
        // ?????????????????????????????????
        List<FetchProjectTemplateResponse> fetchTemplateList = templatesClient.fetchProjectTemplateList(uuids);

        List<CompProjectTemplateDetailResponse> jacksonBaseParse = PayloadParseUtil
                .jacksonBaseParse(CompProjectTemplateDetailResponse.class, fetchTemplateList);
        return jacksonBaseParse;
    }

    @Override
    @ResponseStatus(HttpStatus.CREATED)
    @ResAction(action = "create", resType = "project_template")
    @LogCodeDefine("32")
    public CompProjectTemplateDetailResponse saveProjectTemplate(@PathVariable("namespace") String namespace,
            @RequestBody CompProjectTemplateCreateRequest compReq) {
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        // ??????????????????
        CompositeResource resResource = jacksonBaseParse(CompositeResource.class, compReq);
        resAuthHelper.resourceActionVerify(authCtx.getUser(), resResource, authCtx.getResAction(),
                authCtx.getFlattenConstraints());

        String templateName = compReq.getName();
        // ????????????????????????
        if (StringUtils.isBlank(templateName)) {
            String tipMsg = "the {} attribute is required.";
            throw new BaseException(
                    tipMsg, LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BIZ_PARAMETER_CHECK_FAIL, "name");
        }
        // ????????????????????????
        boolean flag = templateName.matches("^[A-Za-z][A-Za-z0-9\\-\\.]*[A-Za-z0-9](?!\\n)$");
        if (!flag) {
            throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BIZ_RESOURCE_NOT_MATCH);
        }
        // ????????????????????????????????????
        CompositeResource queryParam = new CompositeResource();
        queryParam.setType(authCtx.getResType());
        queryParam.setName(templateName);
        if (compositeResDao.queryResourceCount(queryParam) > 0) {
            throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BIZ_RESOURCE_ALREADY_EXIST);
        }
        FetchProjectTemplateResponse projTemplate = queryProjectTemplate(authCtx.getUser().getNamespace(),
                compReq.getTemplate());
        // ?????????????????????????????????
        List<ProjectTemplateRoleDTO> returnRoles = getRoles(projTemplate, compReq);
        // ??????????????????????????????
        List<Map<String, Object>> resourceNames = compReq.getResources();
        List<ProjectTemplateResourceDTO> projTempResDtos = getProjTempResDtos(resourceNames);
        int i = 1;
        for (ProjectTemplateRoleDTO role : returnRoles) {
            role.setId(i);
            i++;
        }
        for (ProjectTemplateResourceDTO projTempResDto : projTempResDtos) {
            projTempResDto.setId(i);
            i++;
        }
        // ?????????????????????????????????
        CreateProjectTemplateRequest request = new CreateProjectTemplateRequest();
        request.setName(templateName);
        request.setResources(projTempResDtos);
        request.setRoles(returnRoles);
        CreateProjectTemplateResponse response = templatesClient.createProjectTemplate(request);
        // ????????????????????????
        CompositeResource projectResource = new CompositeResource();
        projectResource.setUuid(response.getUuid());
        projectResource.setName(response.getName());
        projectResource.setType(authCtx.getResType());
        projectResource.setNamespace(authCtx.getUser().getNamespace());
        projectResource.setCreatedBy(authCtx.getUser().getUsername());
        compositeResDao.insertCompositeResource(projectResource);
        // ????????????
        LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(authCtx, authCtx.getResType(), response.getUuid(),
                response.getName(), "create", 1, "generic", queryParam);
        String logJson = LogEventUtil.wrapLogEvents2Json(logEvent);
        logClient.saveEventsLogInfo(logJson);
        return PayloadParseUtil.jacksonBaseParse(CompProjectTemplateDetailResponse.class, response);
    }

    /**
     * ???????????????????????????---??????????????????????????? checkRoles:(?????????????????????????????????????????????). <br/>
     * ????????? baiwp
     * 
     * @param projTemplate
     * @param compReq
     */
    private List<ProjectTemplateRoleDTO> getRoles(FetchProjectTemplateResponse projTemplate,
            CompProjectTemplateCreateRequest compReq) {
        List<ProjectTemplateRoleDTO> returnRoles = new ArrayList<>();
        List<ProjectTemplateRoleDTO> roles = projTemplate.getRoles();
        List<Map<String, Object>> requestRoles = compReq.getRoles();
        List<Integer> roleIds = requestRoles.stream().map(requestRole -> {
            String requestIdObj = String.valueOf(requestRole.get("id"));
            return Integer.parseInt(requestIdObj);
        }).collect(Collectors.toList());
        for (Map<String, Object> requestRole : requestRoles) {
            String requestIdObj = String.valueOf(requestRole.get("id"));
            if (StringUtils.isEmpty(requestIdObj)) {
                continue;
            }
            Integer requestId = Integer.parseInt(requestIdObj);
            for (ProjectTemplateRoleDTO role : roles) {
                Integer id = role.getId();
                if (requestId.equals(id)) {
                    returnRoles.add(role);
                    List<Integer> dependsOnList = role.getDependsOn();
                    if (CollectionUtils.isNotEmpty(dependsOnList)) {
                        // ??????????????????????????????
                        for (Integer integer : dependsOnList) {
                            if (!roleIds.contains(integer)) {
                                String tipMsg = "the role {} must depond on parent role";
                                throw new BaseException(tipMsg, LastLogCodeEnum.VALIDATE_ERROR, 
                                        ResultErrorEnum.BIZ_PARAMETER_CHECK_FAIL, role.getName());
                            }
                        }
                    }
                }
            }
        }
        return returnRoles;
    }

    /**
     * 
     * queryProjectTemplate:??????????????????????????????????????????. <br/>
     * ????????? baiwp
     * 
     * @param namespace
     * @param templateName
     * @return
     */
    private FetchProjectTemplateResponse queryProjectTemplate(String namespace, String templateName) {
        // ????????????????????????
        if (StringUtils.isBlank(templateName)) {
            String tipMsg = "the {} attribute is required.";
            throw new BaseException(tipMsg, LastLogCodeEnum.VALIDATE_ERROR, 
                    ResultErrorEnum.BIZ_PARAMETER_CHECK_FAIL, "template");
        }

        // ????????????????????????
        CompositeResource queryParam = new CompositeResource();
        queryParam.setType(RequestConstraintEnum.project_template.getResType());
        queryParam.setName(templateName);
        List<CompositeResource> compProjTempList = compositeResDao.queryResourceList(queryParam);
        // ????????????????????????????????????
        if (CollectionUtils.isEmpty(compProjTempList)) {
            String msg = "There is no resource exists with orgAccount = {}, name = {}, type = {} ";
            throw new BaseException(msg, LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST,
                    namespace, templateName, RequestConstraintEnum.project_template.getResType());
        }

        // ????????????????????????????????????????????????
        String uuid = compProjTempList.get(0).getUuid();
        List<FetchProjectTemplateResponse> fetchProjectTemplateResponseList = templatesClient
                .fetchProjectTemplateList(Arrays.asList(uuid));
        // ???????????????????????????????????????
        if (CollectionUtils.isEmpty(fetchProjectTemplateResponseList)) {
            String msg = "There is no resource exists with orgAccount = {}, name = {}, type = {} ";
            throw new BaseException(msg, LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BIZ_RESOURCE_NOT_EXIST,
                    namespace, templateName, RequestConstraintEnum.project_template.getResType());
        }

        return fetchProjectTemplateResponseList.get(0);
    }

    /**
     * 
     * getProjTempResDtos:???????????????????????????. <br/>
     * ????????? baiwp
     * 
     * @param resourceNames
     * @return
     */
    private List<ProjectTemplateResourceDTO> getProjTempResDtos(List<Map<String, Object>> resourceNames) {
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        Map<String, List<String>> resTypeNameMap = new HashMap<>();
        // ???????????????????????????????????????
        if (CollectionUtils.isNotEmpty(resourceNames)) {
            for (Map<String, Object> resourceNameMap : resourceNames) {
                String name = (String) resourceNameMap.get("name");
                String type = (String) resourceNameMap.get("type");
                // ??????????????????????????????????????????????????????
                if (StringUtils.isEmpty(type) || StringUtils.isEmpty(name)) {
                    continue;
                }
                type = type.toLowerCase(Locale.getDefault());
                if (resTypeNameMap.containsKey(type)) {
                    List<String> nameList = resTypeNameMap.get(type);
                    nameList.add(name);
                } else {
                    List<String> nameList = new ArrayList<>();
                    nameList.add(name);
                    resTypeNameMap.put(type, nameList);
                }
            }
        }

        List<ProjectTemplateResourceDTO> projTempResDtos = new ArrayList<>();
        // ???????????????????????????
        for (Entry<String, List<String>> entry : resTypeNameMap.entrySet()) {
            String type = entry.getKey();
            List<CompositeResource> bindResourceList = compositeResDao
                    .queryResourcesByNameList(authCtx.getUser().getNamespace(), type, entry.getValue());
            for (CompositeResource compositeResource : bindResourceList) {
                ProjectTemplateResourceDTO proTemDto = new ProjectTemplateResourceDTO();
                proTemDto.setDependsOn(new ArrayList<>());
                proTemDto.setName(compositeResource.getName());
                proTemDto.setType(type);
                proTemDto.setRequired(true);
                ProjectTemplateResourceParentDTO projTempParentDto = new ProjectTemplateResourceParentDTO();
                if (RequestConstraintEnum.priv_regis_proj.getResType().equalsIgnoreCase(type)) {
                    projTempParentDto.setType(RequestConstraintEnum.priv_registry.getResType());
                    String[] names = compositeResource.getName().split(":");
                    if (names.length > NAMELENGTH) {
                        proTemDto.setName(names[1]);
                        projTempParentDto.setName(names[0]);
                    }
                } else if (RequestConstraintEnum.knamespace_name.getResType().equalsIgnoreCase(type)) {
                    projTempParentDto.setType(RequestConstraintEnum.cluster_name.getResType());
                    projTempParentDto.setName(compositeResource.getRegionName());
                }
                proTemDto.setParentResource(projTempParentDto);
                projTempResDtos.add(proTemDto);
            }
        }
        return projTempResDtos;
    }
}
