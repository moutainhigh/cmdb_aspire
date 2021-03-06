package com.migu.tsg.microservice.atomicservice.rbac.biz.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import com.google.common.collect.Lists;
import com.migu.tsg.microservice.atomicservice.common.client.LdapServiceClient;
import com.migu.tsg.microservice.atomicservice.ldap.dto.GetLdapUserResponse;
import com.migu.tsg.microservice.atomicservice.ldap.dto.InsertLdapMemberRequest;
import com.migu.tsg.microservice.atomicservice.ldap.dto.ListPagenationResponse;
import com.migu.tsg.microservice.atomicservice.rbac.biz.DepartmentBiz;
import com.migu.tsg.microservice.atomicservice.rbac.biz.UserBiz;
import com.migu.tsg.microservice.atomicservice.rbac.biz.UserDepmentSyncEipBiz;
import com.migu.tsg.microservice.atomicservice.rbac.dao.po.DepartmentUser;
import com.migu.tsg.microservice.atomicservice.rbac.dao.po.User;
import com.migu.tsg.microservice.atomicservice.rbac.dto.DepartmentCreateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserBatchCreateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserCreateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.DepartmentDTO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.UserDTO;
import com.migu.tsg.microservice.atomicservice.rbac.entity.EipDeptDTO;
import com.migu.tsg.microservice.atomicservice.rbac.entity.EipUserDTO;
import com.migu.tsg.microservice.atomicservice.rbac.service.DepartmentService;
import com.migu.tsg.microservice.atomicservice.rbac.service.UserService;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@Service
public class UserDeptSyncEipBizImpl implements UserDepmentSyncEipBiz{
	
	@Value("${eip.url.use-dept}")
	private String urlUseDept;
	@Value("${eip.url.all-user}")
	private String urlAllUser;
	@Value("${eip.sync.env}")
	private String syncEnv;
	@Value("${ldapconfig.namespace:alauda}")
	private String namespace;

	@Autowired
	DepartmentService departmentService;
	@Autowired
	UserService userService;
	@Autowired
	UserBiz userBiz;
	@Autowired
	DepartmentBiz departmentBiz;
	@Autowired
	LdapServiceClient ldapServiceClient;
	
	
	private static final Logger logger = LoggerFactory.getLogger(UserDeptSyncEipBizImpl.class);
	
	/**
	 * OA-??????????????????ID
	 */
	private static final String DEPT_SHUMA_ID = "D01500";
	
	/**
	 *  ?????????OA????????????
	 */
	private static final String OA_SYNC = "OA-SYNC";
	
	private static final String CURR_ENV_UMS = "UMS";
	
	

	@Override
	public void userDeptSyncProcess() {
		// ????????????Config??????????????????????????????????????????
		logger.info("?????????????????????{}", syncEnv);
		if(!CURR_ENV_UMS.equals(syncEnv)){
			return ;
		}
		RestTemplate restTemplate = new RestTemplate();
		ParameterizedTypeReference<List<EipDeptDTO>> typeRef = new ParameterizedTypeReference<List<EipDeptDTO>>() {
		};
		ResponseEntity<List<EipDeptDTO>> responseEntity = restTemplate.exchange(urlUseDept, HttpMethod.GET, null,
				typeRef);
		List<EipDeptDTO> deptList = responseEntity.getBody();

		// ??????OA???????????????????????????
		List<EipDeptDTO> smDeptList = deptList.stream().filter(item -> item.getFullId().contains(DEPT_SHUMA_ID))
				.collect(Collectors.toList());
		// ??????????????????Map
		Map<String, EipDeptDTO> smDeptMap = smDeptList.stream()
				.collect(Collectors.toMap(EipDeptDTO::getDeptId, EipDeptDTO -> EipDeptDTO));

		List<DepartmentDTO> umsDepartList = departmentBiz.getAll();
		List<String> umsDepartIdList = umsDepartList.stream().map(b -> b.getUuid()).collect(Collectors.toList());
		// EIP???????????????????????????ID??????????????????????????????
		smDeptList.stream().filter(it -> !umsDepartIdList.contains(it.getDeptId())).forEach(d -> createNewDept(d));
		
		// ???????????????????????????????????????
		initUser(smDeptMap);
	}

	private void initUser(Map<String, EipDeptDTO> smDeptMap) {
		//??????OA?????????eip_user
		RestTemplate restTemplate = new RestTemplate();
		ParameterizedTypeReference<List<EipUserDTO>> typeRef = new ParameterizedTypeReference<List<EipUserDTO>>() {};
		ResponseEntity<List<EipUserDTO>> responseEntity = restTemplate.exchange(urlAllUser, HttpMethod.GET, null,typeRef);
		List<EipUserDTO> eipUserList = responseEntity.getBody();
		
		List<EipUserDTO> smUserList = eipUserList.stream().filter(item -> smDeptMap.containsKey(item.getDeptId())).collect(Collectors.toList());
		//List<EipUserDTO> smUserList = eipUserList.stream().filter(item -> item.getDept().contains("????????????")).collect(Collectors.toList());
		logger.info("???????????????????????????????????????{}", smUserList.size());
		Map<String,EipUserDTO> smUserMap = smUserList.stream().collect(Collectors.toMap(EipUserDTO::getUserLogin,EipDeptDTO->EipDeptDTO));
		
		List<UserDTO> userList = userBiz.getAll();
		List<String> allUmsUserAccount = userList.stream().map(b -> b.getLdapId()).collect(Collectors.toList());
		
		// ?????????????????????????????????UMS?????????????????????; UMS?????????????????????
		List<UserDTO> deletUserList = userList.stream().filter(it -> OA_SYNC.equals(it.getDescr()) && !smUserMap.containsKey(it.getLdapId())).collect(Collectors.toList());
		logger.info("??????EIP????????????????????????????????????{}", JSONArray.fromObject(deletUserList));
		deletUserList.forEach(vo -> userBiz.deleteByPrimaryKey(vo.getUuid()));
		
		//1. ????????????????????????UMS???????????????????????????????????????
		smUserList.stream().filter(it -> !allUmsUserAccount.contains(it.getUserLogin())).forEach(v -> createNewSmUser(v));
		//2. UMS????????????????????????????????????????????????????????????
		List<UserDTO> eipDeptNotExistUser = userList.stream().filter(
				it -> smUserMap.containsKey(it.getLdapId()) && !StringUtils.isEmpty(it.getDeptId()) && !it.getDeptId().startsWith("D")).collect(Collectors.toList());
		logger.info("UMS????????????????????????????????????????????????????????????????????????{}", eipDeptNotExistUser.size());
		addEipDeptment(smUserMap, eipDeptNotExistUser);
		
		//3. ?????????UMS??????????????????????????????????????????
		userList.stream().filter(it -> smUserMap.containsKey(it.getLdapId())).forEach(v -> updateUserDept(v, smUserMap));
		
		//4. ??????roles_user????????????????????????????????????????????????????????????role_uuid  = 'cadcd1e8-3e9c-4dc9-a49f-ddb44ba44f89'	;??????????????????
		userBiz.addDefaultRoleForUms();
		//5. ??????t_user_classify_account?????????????????????????????????????????????????????????user_classify_uuid ='9f05464a-30da-423f-a38a-c7d7f0d36371';??????????????????
		userBiz.addDefaultUserClassifyAccountForUms();
		
	    //6. ??????ldap??????--??????smUserList?????????
		ldapUserSync(smUserList);
	}

	@SuppressWarnings("unchecked")
	private void ldapUserSync(List<EipUserDTO> smUserList) {
		int pageSize = 200000;
		int currentPage = 1;
		ListPagenationResponse response = ldapServiceClient.listLdapMember(namespace, null, null,null,Collections.EMPTY_LIST, Collections.EMPTY_LIST,
				pageSize, currentPage);
		List<GetLdapUserResponse> results = response.getResults();
		List<String> ldapUserNames = results.stream().map(b -> b.getUsername()).collect(Collectors.toList());
		List<EipUserDTO> needAdds = smUserList.stream().filter(it -> !ldapUserNames.contains(it.getUserLogin())).collect(Collectors.toList());
		List<InsertLdapMemberRequest> request = Lists.newArrayList();
		needAdds.forEach(e -> {
            request.add(convertLdap(e));
        });
		ldapServiceClient.insertLdapMembers(namespace, request);
	}

	private InsertLdapMemberRequest convertLdap(EipUserDTO user) {
		InsertLdapMemberRequest insert = new InsertLdapMemberRequest();
		insert.setMobile(user.getTel());
		insert.setName(user.getUserName());
		insert.setUsername(user.getUserLogin());
		insert.setMail(user.getMail());
		logger.info("??????Ldap?????????{}",JSONObject.fromObject(insert));
		return insert;
	}

	private Object updateUserDept(UserDTO user, Map<String, EipUserDTO> smUserMap) {
		String smDeptId = smUserMap.get(user.getLdapId()).getDeptId();
		List<DepartmentDTO> departmentList = user.getDeptList();
		if(CollectionUtils.isNotEmpty(departmentList) && smDeptId != null){
			Optional<DepartmentDTO> option = departmentList.stream().filter(it -> it.getUuid().startsWith("D")).findAny();
			if(!option.isPresent()){
				List<UserDTO> eipDeptNotExistUser = new ArrayList<>();
				eipDeptNotExistUser.add(user);
				addEipDeptment(smUserMap, eipDeptNotExistUser);
			} else {
				for(DepartmentDTO dept : departmentList){
					if(dept.getUuid().startsWith("D") && !dept.getUuid().equals(smDeptId)){
						userBiz.modifyDeptIdBatchByUserIdArrays(smDeptId, Arrays.asList(dept.getUuid().split(",")));
					}
				}
			}
		}
		return null;
	}

	// UMS?????????????????????EIP???????????????????????????????????????
	private void addEipDeptment(Map<String, EipUserDTO> smUserMap, List<UserDTO> eipDeptNotExistUser) {
		if(CollectionUtils.isNotEmpty(eipDeptNotExistUser)){
			List<UserCreateRequest> userList2 = new ArrayList<>();
			eipDeptNotExistUser.forEach(user -> {
				UserCreateRequest requ = new UserCreateRequest();
				requ.setCode(user.getCode());
				String deptId = smUserMap.get(user.getLdapId()).getDeptId();
				requ.setDeptId(deptId);
				userList2.add(requ);
				
			});
			UserBatchCreateRequest request3 = new UserBatchCreateRequest();
			request3.setListUser(userList2);
			//userService.batchInsertDepartmentUser(request3);
			
			List<DepartmentUser> departmentUserList = Lists.newArrayList();
            List<UserCreateRequest> list = request3.getListUser();
            if (!CollectionUtils.isEmpty(list)) {
                list.forEach(e -> {

                    List<User> userList = userBiz.selectByLdapIdAndNamespace(e.getCode(), "alauda");
                    if (org.apache.commons.collections.CollectionUtils.isEmpty(userList)) {
                        return;
                    }
                    userList.forEach(m -> {
                        DepartmentUser departmentUser = new DepartmentUser();
                        departmentUser.setDeptId(e.getDeptId());
                        departmentUser.setUserId(m.getUuid());
                        departmentUserList.add(departmentUser);
                    });
                });
                userBiz.batchInsertDepartmentUser(departmentUserList);
           }
       }
	}

	//????????????????????????????????????????????? department_user
	private void createNewSmUser(EipUserDTO eipUser) {
		UserCreateRequest user = new UserCreateRequest();
		user.setDeptId(eipUser.getDeptId());
		user.setLdapId(eipUser.getUserLogin());
		user.setCode(eipUser.getUserLogin());
		user.setMail(eipUser.getMail());
		user.setMobile(eipUser.getTel());
		user.setName(eipUser.getUserName());
		user.setUserType(1);
		user.setNamespace("alauda");
		user.setDescr(OA_SYNC);
		user.setNo(eipUser.getUserCode());
		
		List<String> deptIds = Lists.newArrayList();
		deptIds.add(eipUser.getDeptId());
		user.setDeptIds(deptIds);
		
		logger.info("OA??????????????????:{}",JSONObject.fromObject(eipUser));
		//userService.createdUser(user);
		
		UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user, userDTO);
        userBiz.insert(userDTO);
	}
	
	// ??????????????????
	private void createNewDept(EipDeptDTO dept) {
		DepartmentCreateRequest departmentCreateRequest = new DepartmentCreateRequest();
		departmentCreateRequest.setDepartmentId(dept.getDeptId());
		departmentCreateRequest.setName(dept.getDeptName());
		if(DEPT_SHUMA_ID.equals(dept.getParentId())){
			departmentCreateRequest.setParentId("1001");
		}else{
			departmentCreateRequest.setParentId(dept.getParentId());
		}
		departmentCreateRequest.setNo(OA_SYNC);
		departmentCreateRequest.setDescr(dept.getFullName());
		departmentCreateRequest.setNamespace("alauda");
		departmentCreateRequest.setDeptType(1);
		logger.info("OA:{},????????????--???{}",dept.getFullName(), JSONObject.fromObject(departmentCreateRequest));
		//departmentService.createdDepartment(departmentCreateRequest);
		
		DepartmentDTO departmentDTO = new DepartmentDTO();
        BeanUtils.copyProperties(departmentCreateRequest, departmentDTO);
        if(!StringUtils.isEmpty(departmentCreateRequest.getDepartmentId())){
        	departmentDTO.setUuid(departmentCreateRequest.getDepartmentId());
        }
	    departmentBiz.insert(departmentDTO);
	}
}
