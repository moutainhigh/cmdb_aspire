package com.migu.tsg.microservice.atomicservice.rbac.controller;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.migu.tsg.microservice.atomicservice.common.annotation.ResultCode;
import com.migu.tsg.microservice.atomicservice.common.config.KeycloakProperties;
import com.migu.tsg.microservice.atomicservice.ldap.dto.UpdateLdapMemberRequest;
import com.migu.tsg.microservice.atomicservice.rbac.biz.OrgsBiz;
import com.migu.tsg.microservice.atomicservice.rbac.biz.UserBiz;
import com.migu.tsg.microservice.atomicservice.rbac.dto.CreateOrgAccountRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.CreateOrgAccountResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.GetOrgDetailResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.GetOrgUserDetailResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.ListOrgAccountsResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateBykeycloakRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateOrgRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateOrgResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateSubAccountPasswordRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateSubAccountRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateSubAccountResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.CreateOrgAccountDTO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.FileUpload;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.ListOrgAccountsDTO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.OrgDetailDTO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.UserDTO;
import com.migu.tsg.microservice.atomicservice.rbac.service.OrgsService;

/**
 * ????????????: rbac-service <br>
 * ???: com.migu.tsg.microservice.atomicservice.rbac.controller <br>
 * ?????????: OrgsController.java <br>
 * ?????????: ??????????????? <br>
 * ?????????: WangSheng <br>
 * ????????????: 2017???8???14?????????3:56:34 <br>
 * ??????: v1.0
 */
@RestController
public class OrgsController implements OrgsService {

    @Autowired
    private OrgsBiz orgsBiz;
    
    @Autowired
    private KeycloakProperties properties;
    

	/**
	 * service
	 */
	 @Autowired
    private UserBiz userBiz;

    /**
     * ????????????(??????)??????
     * @param request ????????????
     * @param namespace ????????????
     * @param username ????????????
     */
    @ResultCode("105010201")
    public void updateSubAccountPassword(@RequestBody final UpdateSubAccountPasswordRequest request,
            @PathVariable("org_name") final String namespace,
            @PathVariable("username") final String username) {
        orgsBiz.updateSubAccountPassword(request.getPassword(), request.getOldPassword(), namespace,
                username);
    }
    


	/* (non-Javadoc)
	 * @see com.migu.tsg.microservice.atomicservice.rbac.service.OrgsService#updateByKeycloak(com.migu.tsg.microservice.atomicservice.rbac.dto.UpdateBykeycloakRequest)
	 */
	@Override
	public void updateByKeycloak(@RequestBody UpdateBykeycloakRequest request) {
		Keycloak kcMaster = Keycloak.getInstance(properties.getUrl(), "master", properties.getUsername(), properties.getPassword(), "admin-cli");
		RealmResource realmResource = kcMaster.realm(properties.getRealm());
		 // ????????????????????????????????????keycloak?????? User??????
        String username = request.getUsername();
		UserRepresentation updateUser = realmResource.users().search(username).get(0);
        Boolean enable = request.getEnable();
		updateUser.setEnabled(enable);
        UserResource userResource = realmResource.users().get(updateUser.getId());
		userResource.update(updateUser);
		UserDTO userDTO=new UserDTO();
		userDTO.setLdapId(username);
		List<UserDTO> userList=userBiz.select(userDTO);
		if(CollectionUtils.isNotEmpty(userList)) {
			UserDTO dt=userList.get(0);
			dt.setLdapStatus(enable?"1":"0");
			userBiz.updateByPrimaryKey(dt);
		}
	}

    /**
     * ????????????
     * @param namespace ????????????
     * @param username ????????????
     */
    @ResultCode("105010202")
    public void removeOrgAccount(@PathVariable("org_name") final String namespace,
            @PathVariable("username") final String username) {
        orgsBiz.removeOrgAccount(namespace, username);
    }

    /**
     * ??????????????????(????????????,????????????)
     * @param request ????????????
     * @param namespace ????????????
     * @return ????????????
     */
    @ResultCode("105010203")
    public UpdateOrgResponse updateOrg(@RequestBody final UpdateOrgRequest request,
            @PathVariable("org_name") final String namespace) {
        OrgDetailDTO updateOrg = orgsBiz.updateOrg(request.getCompany(), request.getEmail(),
                request.getNewPassword(), request.getOldPassword(), namespace);
        UpdateOrgResponse response = new UpdateOrgResponse();
        BeanUtils.copyProperties(updateOrg, response);
        return response;
    }

    /**
     * ???????????????(??????/??????)????????????
     * @param namespace ????????????
     * @return ????????????
     */
    @ResultCode("105010204")
    public GetOrgDetailResponse getOrgDetail(@PathVariable("org_name") final String namespace) {
        OrgDetailDTO orgDetail = orgsBiz.getOrgDetail(namespace);
        GetOrgDetailResponse response = new GetOrgDetailResponse();
        BeanUtils.copyProperties(orgDetail, response);
        return response;
    }

    /**
     * ???????????????(??????/??????)???????????????????????????
     * @param namespace ?????????(?????????)
     * @param username ????????????
     * @return ????????????
     */
    @ResultCode("105010205")
    public GetOrgUserDetailResponse getOrgUserDetail(@PathVariable("org_name") String namespace,
            @PathVariable("username") String username) {
        OrgDetailDTO orgDetail = orgsBiz.getOrgUserDetail(namespace, username);
        GetOrgUserDetailResponse response = new GetOrgUserDetailResponse();
        BeanUtils.copyProperties(orgDetail, response);
        return response;
    }

    /**
     * ??????????????????
     * @param request ????????????
     * @param namespace ????????????
     * @return ????????????
     */
    @ResultCode("105010206")
    public List<CreateOrgAccountResponse> createOrgAccount(@RequestBody final CreateOrgAccountRequest request,
            @PathVariable("org_name") final String namespace) {
        List<CreateOrgAccountDTO> result = orgsBiz.createOrgAccount(namespace, request.getAccounts(),
                request.getRoles(), request.getPassword());
        UserDTO userDTO =new UserDTO(); 
        userDTO.setUuid(request.getUserId());
        userDTO.setLdapId(request.getAccounts().get(0).getUsername());
        userDTO.setLdapStatus("1");
        userDTO.setNamespace(namespace);
        userDTO.setLdapPasswordUpdatetime(new Timestamp(System.currentTimeMillis()));
        userBiz.updateByPrimaryKey(userDTO);
        List<CreateOrgAccountResponse> response = result.stream().map(dto -> {
            CreateOrgAccountResponse resposne = new CreateOrgAccountResponse();
            BeanUtils.copyProperties(dto, resposne);
            return resposne;
        }).collect(Collectors.toList());
        return response;
    }

    /**
     * ??????????????????
     * @param namespace ????????????
     * @param teamNameFilter ?????????????????????????????????(????????????)
     * @param search ??????????????????????????????????????????????????????????????????????????????test,repo?????????????????????
     * @param orderBy ??????????????????????????????,?????????"createTime"???"username"??????????????????????????????-??????????????????,???????????????????????????createTime,-username?????????????????????
     * @param pageSize ?????????????????????????????????????????????20,????????????????????????,??????????????????????????????????????????
     * @param currentPage ???????????????????????????1
     * @return ????????????
     */
    @ResultCode("105010207")
    public ListOrgAccountsResponse listOrgAccounts(@PathVariable("org_name") String namespace,
            @RequestParam(value = "team_name_filter", required = false) final String teamNameFilter,
            @RequestParam(value = "username", required = false) String uuid,
            @RequestParam(value = "search", required = false) final String search,
            @RequestParam(value = "order_by", required = false,
                    defaultValue = "-createTime") final String orderBy,
            @RequestParam(value = "page_size", required = false, defaultValue = "20") final int pageSize,
            @RequestParam(value = "page", required = false, defaultValue = "1") final int currentPage) {
        ListOrgAccountsDTO dto = orgsBiz.listOrgAccounts(namespace, teamNameFilter, uuid, search, orderBy, pageSize,
                currentPage);
        ListOrgAccountsResponse response = new ListOrgAccountsResponse();
        BeanUtils.copyProperties(dto, response);
        return response;
    }

    /**
     * ????????????LOGO
     * @param namespace ?????????(?????????)
     * @param request ????????????
     */
    @ResultCode("105010208")
    public void fileUpload(@RequestBody final FileUpload request,
            @PathVariable("namespace") final String namespace) {
        orgsBiz.fileUpload(request.getLogoFile(), namespace);
    }

    /**
     * ????????????(??????)??????
     * @param request ????????????
     * @param namespace ?????????(?????????)
     * @param username ????????????
     */
    @ResultCode("105010209")
    public UpdateSubAccountResponse updateSubAccount(@RequestBody final UpdateSubAccountRequest request,
            @PathVariable("org_name") final String namespace,
            @PathVariable("username") final String username) {
        UserDTO userDTO =new UserDTO();
        userDTO.setUuid(request.getUserId());
        userDTO.setLdapId(username);
        userDTO.setNamespace(namespace);
        userDTO.setLdapStatus("1");
        if (StringUtils.isNotEmpty(request.getNewPassword())) {
            userDTO.setLdapPasswordUpdatetime(new Timestamp(System.currentTimeMillis()));
        }
        userBiz.updateByPrimaryKey(userDTO);
    	UpdateLdapMemberRequest req=new UpdateLdapMemberRequest();
    	BeanUtils.copyProperties(request, req);
        return orgsBiz.updateSubAccount(namespace, username, req);
    }



	/**
	 * ??????????????????????????????
	 */
	@Override
	public int hasAdminRole(@PathVariable("namespace") String namespace,@PathVariable("username")  String username) {
		return orgsBiz.hasAdminRole(username, namespace);
	}

}
