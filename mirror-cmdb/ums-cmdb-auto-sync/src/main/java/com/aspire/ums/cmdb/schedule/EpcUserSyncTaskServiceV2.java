/**
 *
 */
package com.aspire.ums.cmdb.schedule;

import com.migu.tsg.microservice.atomicservice.rbac.dto.vo.UserVO;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aspire.ums.cmdb.common.StringUtils;
import com.aspire.ums.cmdb.service.impl.OrgSyncInterfaceServiceImpl;
import com.aspire.ums.cmdb.mapper.OrgSyncManager;
//import com.aspire.ums.cmdb.mapper.UserSyncManager;
import com.aspire.ums.cmdb.sync.client.LdapServiceClient;
import com.aspire.ums.cmdb.sync.client.RbacServiceClient;
import com.aspire.ums.cmdb.sync.entity.Office;
import com.aspire.ums.cmdb.sync.entity.User;
import com.aspire.ums.cmdb.sync.util.UmsWebServiceUtils;
import com.google.common.collect.Lists;
import com.migu.tsg.microservice.atomicservice.ldap.dto.GetLdapUserResponse;
import com.migu.tsg.microservice.atomicservice.ldap.dto.InsertLdapMemberRequest;
import com.migu.tsg.microservice.atomicservice.ldap.dto.ListPagenationResponse;
import com.migu.tsg.microservice.atomicservice.ldap.dto.UpdateLdapMemberRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserBatchCreateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserCreateRequest;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;


import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lupeng
 *
 */
@Component
//@ConditionalOnExpression("${schedule.epcUser.flag:false}")
public class EpcUserSyncTaskServiceV2 {

	protected static Logger logger = LoggerFactory.getLogger(EpcUserSyncTaskServiceV2.class);
	@Value("${ldapconfig.namespace:alauda}")
	private String namespace;

	@Value("${sync.yunGuan.tokenUrl:http://10.144.91.163:30110/oauthserver/oauth/token}")
	private String sendUrl;
	@Value("${sync.yunGuan.userName:IWbqqFuqHw7GIqlWZRevR1NUothJR4VDMpO0NfGcv1r+RyIlWoaamJ+d/mL/bcrunv3B5D7y6hsIza6KmEct6Fa3hMbU4VLuA6mAaiRhFNUCKCiJd/renkZV2ddhTH7EeqHXy2g808c7HnXLWKsA7JgkYHx/sQ6UGV+lN+EoTZM=}")
	private String userName;
	@Value("${sync.yunGuan.password:YOeEctAnr2qXR/3sl1vlPyI3ox1GjRLE+NhacmUEjkDBgwgr1R8HO+/d9lLRmNs27Kn966YYfs/AUYmgZZekjmZqN0GiUON6/mhPbkAruIgn95QEI4fh5npYxWIvbTtFjvPbM+QN1fNNpZX/up0e1QV/BT6yIkxrREJo3vcvt9s=}")
	private String password;
	@Value("${sysdata.Token.username}")
	private String userNameForOld;
	@Value("${sysdata.Token.password}")
	private String passwordForOld;
	@Value("${sync.yunGuan.userUrl:http://10.144.91.163:30110/madrids/v1/external-platform/users?&platformName=BPM??????}")
	private String sysUrl;

	@Value("${sync.bpm.user:http://localhost:8088/api/YunGuanSync/v1/getSuYanData}")
	private String bpmUserUrl;

	//sysdata.Epcuser.url
	@Value("${sysdata.Epcuser.url}")
	private String sysUrlForOld;

	@Value("${sysdata.Token.url}")
	private String tokenUrlForOld;

	@Value("${server.port}")
	private String port;

	@Value("${cmic.org.user.default.role.id}")
	private static String DEFAULT_ROLE_ID;

	@Value("${cmic.org.user.default.user.type}")
	private static String DEFAULT_USER_TYPE;

	@Value("${rabc.sysadmin.roleid}")
	private String ROLE_ID;

	@Value("${rabc.root.departmentid}")
	private String DEPARTMENT_ID;
	//??????????????????, ????????????, ??????????????????descr??????,??? ??????
	@Value("EPC")
	private String descr;
	@Value("${sysdata.Epcuser.applicationName}")
	private String applicationName;
	// private final static int pagesize = 200000;

	// private String md5 = "{MD5}";
	@Autowired
	private OrgSyncInterfaceServiceImpl orgservice;

	@Autowired
	private LdapServiceClient ldapServiceClient;

	@Autowired
	private RbacServiceClient rbacServiceClient;
	@Autowired
	private UmsWebServiceUtils umsWebServiceUtils;
	@Autowired
	private OrgSyncManager orgSyncManager;
	@Value("${bomc.enable.sync.status:false}")
	private boolean enableSyncStatus;
	/*@Autowired
	private UmsWebServiceUtils umsWebServiceUtils;*/
	//"0 0/30 * * *  ?"
	//@Scheduled(cron = "${syncEpcUserData.cron}")
	public void syncEpcUserData() {
		logger.info("???????????????????????????????????????...");
		try {
			sysEpcUserData();
			if (enableSyncStatus) {
				System.out.println("ZWN_???????????????????????????");
				this.syncStatus("ZWN_???????????????");
				this.syncStatus(this.applicationName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void syncStatus(String applicationName) throws Exception {
		// ??????token????????????
		JSONObject token = this.getTokenWithOldUrl();
		logger.info("+++++++++??????Token:" + token);
		if (token == null) {
			throw new Exception("?????????Token???NULL");
		}
		String access_token = (String) token.get("access_token");
		if (StringUtils.isEmpty(access_token)) {
			throw new Exception("????????????access_token??????");
		}
		StringBuffer sendUrl = new StringBuffer(sysUrlForOld).append("?" +"applicationName="+ applicationName+"&&token=" + access_token);
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet getMethod = new HttpGet(sendUrl.toString());
		try {
			httpClient.execute(getMethod);
		} catch (Exception e) {
			logger.error("????????????????????????:" + sendUrl, e);
			throw new Exception("????????????????????????");
		}
	}

	/**
	 * ??????token
	 *
	 * @return
	 * @throws Exception
	 */
	public JSONObject getToken() throws Exception {
		JSONObject responseEntity = null;
		logger.info("************", port);

		logger.info("sysdata.Token.url is : {}", sendUrl);
		logger.info("?????????" + userName);
		logger.info("??????" + password);

		// ????????????????????????
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost postMethod = new HttpPost(sendUrl);
		postMethod.addHeader("Content_Type", "application/x-www-form-urlencoded");
		postMethod.addHeader("Authorization", "Basic Q0xJRU5UOlNFQ1JFVA==");
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("username", userName));
		params.add(new BasicNameValuePair("password", password));
		HttpEntity paramEntity = new UrlEncodedFormEntity(params, "UTF-8");
		postMethod.setEntity(paramEntity);
		HttpResponse response = null;
		try {
			response = httpClient.execute(postMethod);
			logger.info("????????????" + postMethod);
			logger.info("????????????" + postMethod.getEntity().getContent().toString());

			// ?????????????????????????????????
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				// ?????????????????????
				HttpEntity httpEntity = response.getEntity();
				String entityStr = EntityUtils.toString(httpEntity);
				logger.info("call get token result is : {}", entityStr);
				// ?????????????????????Json??????
				JSONObject jsonObject = JSONObject.parseObject(entityStr);
				responseEntity = jsonObject.getJSONObject("entity");
				return responseEntity;
			}
		} catch (Exception e) {
			logger.error("????????????Token??????:", e);
			throw e;
		}
		return responseEntity;
	}

	/**
	 * ??????token
	 *
	 * @return
	 * @throws Exception
	 */
	public JSONObject getTokenWithOldUrl() throws Exception {
		JSONObject responseEntity = null;
		logger.info("************", port);

		logger.info("sysdata.Token.url is : {}", tokenUrlForOld);
		logger.info("?????????" + userNameForOld);
		logger.info("??????" + passwordForOld);

		// ????????????????????????
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpPost postMethod = new HttpPost(tokenUrlForOld);
		postMethod.addHeader("Content_Type", "application/x-www-form-urlencoded");
		postMethod.addHeader("Authorization", "Basic Q0xJRU5UOlNFQ1JFVA==");
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("username", userNameForOld));
		params.add(new BasicNameValuePair("password", passwordForOld));
		HttpEntity paramEntity = new UrlEncodedFormEntity(params, "UTF-8");
		postMethod.setEntity(paramEntity);
		HttpResponse response = null;
		try {
			response = httpClient.execute(postMethod);
			logger.info("????????????" + postMethod);
			logger.info("????????????" + postMethod.getEntity().getContent().toString());

			// ?????????????????????????????????
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				// ?????????????????????
				HttpEntity httpEntity = response.getEntity();
				String entityStr = EntityUtils.toString(httpEntity);
				logger.info("call get token result is : {}", entityStr);
				// ?????????????????????Json??????
				JSONObject jsonObject = JSONObject.parseObject(entityStr);
				responseEntity = jsonObject.getJSONObject("entity");
				return responseEntity;
			}
		} catch (Exception e) {
			logger.error("????????????Token??????:", e);
			throw e;
		}
		return responseEntity;
	}
	public JSONArray getEpcUserData() throws Exception {

		// ??????token????????????
		com.alibaba.fastjson.JSONObject token = this.getToken();
		logger.info("+++++++++??????Token:" + token);
		if (token == null) {
			throw new Exception("?????????Token???NULL");
		}
		String access_token = (String) token.get("access_token");
		if (StringUtils.isEmpty(access_token)) {
			throw new Exception("????????????access_token??????");
		}


       /*  .url("http://10.144.91.163:30110/madrids/v1/external-platform/organizations")
                .method("GET", null)
                .addHeader("Authorization", "bearer c26eebdc-edd8-40d8-89ad-8efe033ff94f")*/
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet getMethod = new HttpGet(sysUrl);
		String value =  access_token;
		logger.info("??????????????????value???" + value);
		getMethod.addHeader("Authorization", "bearer "+access_token);
		HttpResponse response = null;
		JSONArray entity = null;
		try {
			response = httpClient.execute(getMethod);
			// ??????????????????
			String responseStr = EntityUtils.toString(response.getEntity());
			logger.info("call sync users interface return is : " + sysUrl);
			logger.info("call sync users interface status is : " + response.getStatusLine().getStatusCode());
			//logger.info("call sync users interface return is : " + responseStr);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				JSONObject jsonObject = JSONObject.parseObject(responseStr).getJSONObject("entity");
				entity = jsonObject.getJSONArray("list");

				return entity;
			} else {
				logger.error("????????????????????????:" + sysUrl);
			}
		} catch (Exception e) {
			logger.error("????????????????????????:" + sysUrl, e);
			throw new Exception("????????????????????????");
		}

		return entity;
	}

	/**
	 * ????????????
	 *
	 * @param
	 * @return
	 */

	public void sysEpcUserData() throws Exception {
		//JSONArray entity = this.getEpcUserData();

		JSONArray entity = this.getUserDataByTestFile();
		if (entity == null) {
			throw new Exception("??????????????????NULL");
		}//UV0001
		// ???????????????????????????
		Map<String, List<User>> userMap = syncEpcUserToOsa(entity);
		// ???????????????BPM
		orgservice.postUserData(bpmUserUrl,userMap);

	}

	private JSONArray getUserDataByTestFile() {
		String path2 = "D:json/user106.json";
		String String = readJsonFile(path2);
		JSONArray entity = JSONObject.parseArray(String);
		return entity;
	}

	/**
	 * ??????portal ws ?????????????????????user
	 *
	 * @param entity
	 * @return
	 */
	private Map<String, List<User>> syncEpcUserToOsa(JSONArray entity) {

//		String namespace = "alauda";
		String projects = null;
		List<String> usernames = new ArrayList();
		List<String> projectss = new ArrayList();
		List<String> orderBy = new ArrayList();
		int pageSize = 200000;
		int currentPage = 1;

		logger.info("SyncCmicUserData: filter cmic users data start");
		Map<String, List<User>> returnMap = new HashMap<>();
		List<User> addList = new ArrayList<>();
		List<User> modiList = new ArrayList<>();
		List<User> delList = new ArrayList<>();
		List<User> addListProcess = new ArrayList<>();

		List<String> delSyncAccounts = new ArrayList<>();
		boolean hasDel = false;

		// add jisnu ???????????? ????????????
		List<UserCreateRequest> userCreateRequestList = Lists.newArrayList();

//		List<UserUpdateRequest> userUpdateRequestList = Lists.newArrayList();
		try {
			// ??? ???ldap?????????????????? ????????? key???login Name??? cnmsUserMap
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
			Date createDate = new Date();
			String remark = sdf.format(createDate);
			String markAdd = remark + "_add";
			String markUpdate = remark + "_update";
			String markSuspend = remark + "_suspend";
			String markDelete = remark + "_delete";
			List<User> userList = orgSyncManager.getMysqlUserList();
//			ldapServiceClient.listLdapMember(namespace, null, null, projectss, orderBy,
//					pageSize, currentPage);
//			List<GetLdapUserResponse> results = response.getResults();
			logger.info("SyncCmicUserData listLdapMember");
			if (userList.size() > 0) {
				logger.info("Find mysql user size -> {}", userList.size());
				logger.info("Find BOMC user size -> {}", entity.size());
//				List<User> list = new ArrayList<>();
//
//				for (GetLdapUserResponse result : results) {
//					// list.add((User) ClassConvertUtil.convertClass(result,
//					// GetLdapUserResponse.class));
//					User user = new User();
//					user.setLoginName(result.getUsername());
//					user.setMobile(result.getMobile());
//					user.setName(result.getName());
//					user.setEmail(result.getMail());
//					user.setPassword(result.getPassword());
//					user.setCreateDate(sdf.parse(result.getCreateTime()));
//					user.setFrom("EPC");
//					// ??????source_uuid??????
//					list.add(user);
//				}
				Map<String, User> cnmsUserMap = new HashMap<>(userList.size());
				List<String> interfaceUser = new ArrayList<>(userList.size());
				for (User user : userList) {
					cnmsUserMap.put(user.getLoginName(), user);
				}

				// ???  ?????? ???????????? ????????????????????? ???,  ????????????username ???cnmsUserMap?????????loginName
				// 2.1 ldap????????? ??? ???????????? ??????new User??? ??????addList
				//2.2 ????????????  378 delList ???????????????
				boolean isDel;
				for (int i = 0; i < entity.size(); i++) {
					JSONObject object = (JSONObject) entity.get(i);
					String id = (String) object.get("code");
					// String organizationId = (String) object.get("organizationId");
					// String applicationName = (String) object.get("applicationName");
					// String roleId = (String) object.get("roleId");
					//TODO ????????????????????????????????????
					String status = object.get("status").equals("OFFLINE")?"SUSPEND":"";
					String suspendFlag = ((String) object.get("status"));
					// String effectTime = (String) object.get("effectTime");
					String name = (String) object.get("name");
					String username = (String) object.get("username");
					String password = (String) object.get("password"); //new User
					String email = (String) object.get("email");
					String mobile = (String) object.get("phone");
					//?????????????????????????????????
					String deptId = (String) object.get("organizationCode");
					User cmnsUser = cnmsUserMap.get(username);
					//???????????????????????????????????????
					interfaceUser.add(username);
					// Pattern loginNamePattern1 = Pattern.compile("^dw", Pattern.CASE_INSENSITIVE);
					// Pattern loginNamePattern2 = Pattern.compile("^dw_",
					// Pattern.CASE_INSENSITIVE);
					// String loginName = cmicUser.getPortalUserId();
					if (StringUtils.isEmpty(name)) {
						continue;
					}
					if (StringUtils.isEmpty(email)) {
						continue;
					}
//					String emailReg = "^([a-zA-Z0-9_\\-\\.]+)@\\w+(\\.\\w+)+";
//					if (!email.matches(emailReg)) {
//						continue;
//					}
					isDel = false;
					// ??????
					if (null == cmnsUser) {
						if ("DELETED".equalsIgnoreCase(status)) {
							continue;
						}
						// Matcher matcher1=loginNamePattern1.matcher(username);
						// //????????????dw??????
						// if(matcher1.find()) {
						// //???????????????dw_??????, dw_????????????????????????, dw???????????????????????????
						// if(!loginNamePattern2.matcher(username).find()) {
						// username = matcher1.replaceFirst("dw_");
						// }
						// }
						Office o = new Office();
						o.setId("1");
						cmnsUser = new User(id, o, // ????????????
								o, // ????????????
								username, password, name, email, mobile, DEFAULT_USER_TYPE, markAdd, createDate, deptId,descr,suspendFlag);
						addList.add(cmnsUser);
						// logger.info("add: "+cmicUser.getEmail());

					} else {
						switch (status.toUpperCase()) {
							case "SUSPEND":// ??????
								cmnsUser.setRemarks(markSuspend);
								cmnsUser.setSuspendFlag("1");
								break;
							default:// ??????
								cmnsUser.setSuspendFlag("0");
								break;
						}
						// ??? 2.3???????????? ?????????status ??????DELETE , ???????????????cmnsUser(ldap??????) ??????????????????modilist
						if (!isDel) {
							// ldap?????????MD5???????????????
							// StringBuffer sb = new StringBuffer();
							// sb.append(md5);
							// sb.append(EncryptUtil.encryptMd5(password));
							//
							// ldap?????????????????????
							if (!password.equals(cmnsUser.getPassword()) || !mobile.equals(cmnsUser.getMobile())
									|| !email.equals(cmnsUser.getEmail()) || !name.equals(cmnsUser.getName())
							) {
								cmnsUser.setEmail(email);
								cmnsUser.setMobile(mobile);
								cmnsUser.setPassword(password);
								cmnsUser.setName(name);
								cmnsUser.setRemarks(markUpdate);
								cmnsUser.setDeptId(cmnsUser.getDeptId());
								//cmnsUser.setSuspendFlag(suspendFlag);
								// cmnsUser.setUpdateDate(createDate);
								modiList.add(cmnsUser);
							}
						}
					//	cnmsUserMap.remove(username);  //cnmsUserMap ??????ldap?????? ???????????????????????????
					}


					// for (User tempUser : cnmsUserMap.values()) {
					// String loginName = tempUser.getLoginName();
					// if (!isDel&& StringUtils.isNotEmpty(loginName) &&
					// loginName.equalsIgnoreCase(username)) {
					// tempUser.setDelFlag("1");
					// tempUser.setRemarks("????????????" + markDelete);
					// onlyDelList.add(tempUser);
					// }
					// }
				}

              //  cnmsUserMap.entrySet().iterator().forEachRemaining(item -> delList.add(item.getValue()));
				logger.info("Need to add user size -> {}", addList.size());
				logger.info("Need to update user size -> {}", modiList.size());


				// ??? ??????addList ??????UserCreateRequest ?????? rbac ??????list
				//    3.2 ?????????insert, ldap ????????????
				// ????????????
				for (User user : addList) {
					// add jinsu ??????????????????
					UserCreateRequest rbacUser = new UserCreateRequest();
					rbacUser.setLdapId(user.getLoginName());
					rbacUser.setNamespace(namespace);
					rbacUser.setName(user.getName());
					// ?????????????????????????????????
					rbacUser.setUserType(1);
					rbacUser.setMail(user.getEmail());
					rbacUser.setMobile(user.getMobile());
					rbacUser.setDeptId(user.getDeptId());
					rbacUser.setRoles(ROLE_ID);
					rbacUser.setCode(user.getLoginName());
					rbacUser.setDescr("EPC");
					userCreateRequestList.add(rbacUser);
					logger.info("Add user info data. -> {}", JSONObject.toJSON(user));
					if (StringUtils.isEmpty(user.getEmail())) {
						continue;
					}

					if (StringUtils.isEmpty(user.getMobile())) {
						continue;
					}

					if (StringUtils.isEmpty(user.getName())) {
						continue;
					}


					List<InsertLdapMemberRequest> request = new ArrayList<>();
					InsertLdapMemberRequest insert = new InsertLdapMemberRequest();
					// request.add((InsertLdapMemberRequest)ClassConvertUtil.convertClass(user,InsertLdapMemberRequest.class));
					insert.setMobile(user.getMobile());
					insert.setName(user.getName());
					insert.setUsername(user.getLoginName());
					insert.setMail(user.getEmail());
					insert.setPassword(user.getPassword());
					request.add(insert);
					try {
						ldapServiceClient.insertLdapMembers(namespace, request);
					} catch (Exception e) {
						logger.error("Insert ldap error. ", e);
					}
				}
				logger.info("after add ldap menbers , userlist is :" + userCreateRequestList.size());
				// userDao.save(addList);
				// userDao.save(addListProcess); ????????????????????????
				// contactsService.addOrgUsers(ouAddList);

				// ??????????????????
				// ??? 3.3 ?????? ?????? ldap
				//    ??????rbaclist
				UpdateLdapMemberRequest req;
				for (User user : modiList) {
					// add jinsu ??????????????????
					UserCreateRequest rbacUser = new UserCreateRequest();
					rbacUser.setLdapId(user.getLoginName());
					rbacUser.setNamespace(namespace);
					rbacUser.setName(user.getName());
					// ?????????????????????????????????
					rbacUser.setUserType(1);
					rbacUser.setMail(user.getEmail());
					rbacUser.setMobile(user.getMobile());
					rbacUser.setDeptId(user.getDeptId());
					rbacUser.setRoles(ROLE_ID);
					rbacUser.setCode(user.getLoginName());
					rbacUser.setDescr("EPC");
					List<String> ds = Arrays.asList(user.getDeptId());
					rbacUser.setDeptIds(ds);
					userCreateRequestList.add(rbacUser);
					logger.info("Update user info data. -> {}", JSONObject.toJSON(user));
					if (StringUtils.isEmpty(user.getEmail())) {
						continue;
					}

					if (StringUtils.isEmpty(user.getMobile())) {
						continue;
					}

					if (StringUtils.isEmpty(user.getName())) {
						continue;
					}


					req = new UpdateLdapMemberRequest();
					req.setName(user.getName());
					req.setNewPassword(user.getPassword());
					req.setMail(user.getEmail());
					req.setMobile(user.getMobile());
					try {
						ldapServiceClient.updateLdapMember(namespace, user.getLoginName(), req);
					} catch (Exception e) {
						logger.error("Update ldap error.", e);
					}
				}
				logger.info("after mod ldap menbers , userlist is :" + userCreateRequestList.size());
				// userDao.save(modiList);
				// contactsService.delOrgUsers(ouDelList);
				// ????????????
			/*	for (User user : delList) {
					ldapServiceClient.deleteLdapMember(namespace, user.getLoginName());
				}*/

				//UV??????ldap??????

				delSyncAccounts = getDelAccountList(interfaceUser);
				hasDel = (null != delSyncAccounts && delSyncAccounts.size() != 0);
				if (hasDel) {
					logger.info("sync>> start del user in ldap and rbac");
					for (String ac : delSyncAccounts) {
						try {
							ldapServiceClient.deleteLdapMember(namespace, ac);
							logger.info("sync>> Del ldapUser info data. -> {}", ac);
						}catch (Exception e){
							logger.error("del ldap user error.{}",e);
						}
						UserVO user = rbacServiceClient.findByLdapId(ac);
						if (null != user&&null!=user.getUuid()) {
							rbacServiceClient.deleteByPrimaryKey(user.getUuid());
							logger.info("sync>> Del rbacUser info ldapId:"+ac+"  uuid"+user.getUuid());
						}
					}
				}

			} else {
				// ?????????ldap??????????????????????????????????????????????????????
				for (int i = 0; i < entity.size(); i++) {
					JSONObject object = (JSONObject) entity.get(i);
					String id = (String) object.get("id");
					String status = (String) object.get("status");
					// String effectTime = (String) object.get("effectTime");
					String name = (String) object.get("name");
					String username = (String) object.get("username");
					String password = (String) object.get("password");
					String email = (String) object.get("email");
					String moile = (String) object.get("phone");
					String deptpId = (String) object.get("organizationCode");
					String suspendFlag = (String) object.get("status");
					if (StringUtils.isEmpty(name)) {
						continue;
					}
					if (StringUtils.isEmpty(email)) {
						continue;
					}
					String emailReg = "^([a-zA-Z0-9_\\-\\.]+)@\\w+(\\.\\w+)+";
					if (!email.matches(emailReg)) {
						continue;
					}

					User addUser;
					// ??????

					if (!"ONLINE".equalsIgnoreCase(status)) {
						continue;
					}
					// Matcher matcher1=loginNamePattern1.matcher(username);
					// //????????????dw??????
					// if(matcher1.find()) {
					// //???????????????dw_??????, dw_????????????????????????, dw???????????????????????????
					// if(!loginNamePattern2.matcher(username).find()) {
					// username = matcher1.replaceFirst("dw_");
					// }
					// }
					Office o = new Office();
					o.setId("1");
					addUser = new User(id, o, // ????????????
							o, // ????????????
							username, password, name, email, moile, DEFAULT_USER_TYPE, markAdd, createDate,deptpId,descr,suspendFlag);
					addList.add(addUser);
					List<InsertLdapMemberRequest> request = new ArrayList<>();
					if (StringUtils.isEmpty(addUser.getMobile())) {
						continue;
					}
					// add jinsu ??????????????????
					UserCreateRequest rbacUser = new UserCreateRequest();
					rbacUser.setLdapId(addUser.getLoginName());
					rbacUser.setNamespace(namespace);
					rbacUser.setName(addUser.getName());
					// ?????????????????????????????????
					rbacUser.setUserType(1);
					rbacUser.setMail(addUser.getEmail());
					rbacUser.setMobile(addUser.getMobile());
					rbacUser.setDeptId(deptpId);
					rbacUser.setRoles(ROLE_ID);
					rbacUser.setCode(addUser.getLoginName());
					rbacUser.setDescr(descr);
					List<String> ds = Arrays.asList(addUser.getDeptId());
					rbacUser.setDeptIds(ds);
					userCreateRequestList.add(rbacUser);

					// request.add((InsertLdapMemberRequest)ClassConvertUtil.convertClass(user,InsertLdapMemberRequest.class));
					InsertLdapMemberRequest insert = new InsertLdapMemberRequest();
					insert.setMail(addUser.getEmail());
					insert.setMobile(addUser.getMobile());
					insert.setName(addUser.getName());
					insert.setUsername(addUser.getLoginName());
					insert.setPassword(addUser.getPassword());
					request.add(insert);
					try {
						ldapServiceClient.insertLdapMembers(namespace, request);
					} catch (Exception e) {
						logger.error("insert ldap member error: {}", e.getMessage());
					}
				}

//				if (!CollectionUtils.isEmpty(userUpdateRequestList)) {
//					rbacServiceClient.modifyArrayByCode(userUpdateRequestList);
//				}
			}
			logger.info("already sync user to database:" + userCreateRequestList.size());
			// ??? ??????rbac??????
			if (!CollectionUtils.isEmpty(userCreateRequestList)) {
				UserBatchCreateRequest userBatchCreateRequest = new UserBatchCreateRequest();
				int loop = 0;
				List<UserCreateRequest> tempList = Lists.newArrayList();
				for (UserCreateRequest userCreateRequest : userCreateRequestList) {
					tempList.add(userCreateRequest);
					if (loop % 100 == 0 || loop >= userCreateRequestList.size()) {
						try{
						logger.info("Current loop -> {} UserCreateRequestList Size -> {}", loop, userCreateRequestList.size());
						userBatchCreateRequest.setListUser(tempList);
						rbacServiceClient.batchCreatedUser(userBatchCreateRequest);
						tempList.clear();}catch (Exception e){
							logger.error("rbac - batchCreatedUser exception -> {}",e);
						}
					}
					loop++;
				}
			}
			// ??? ?????????bpm map
			returnMap.put("addList", addList);
			// returnMap.put("addListProcess", addListProcess); ????????????????????????
			returnMap.put("addListProcess", new ArrayList<>());
			returnMap.put("modiList", modiList);
			if(hasDel) {
				for (String s :
						delSyncAccounts) {
					delList.add(new User(s));
				}
			}
			logger.info("Del bpmUser info delList:"+delList.toString());
			returnMap.put("delList", delList);
			logger.info("SyncCmicUserData: sync cmic users to osa completed, added users :" + addList.size());
			logger.info("SyncCmicUserData: sync cmic 3rd party users to osa completed, added users :"
					+ addListProcess.size());
			logger.info("SyncCmicUserData: sync cmic users to osa completed, modified users :" + modiList.size());
			logger.info("SyncCmicUserData: sync cmic users to osa completed, deleted users :" + delList.size());
		} catch (Exception e) {
			logger.error("SyncCmicUserData: sync cmic users to osa failed", e);
		}
		return returnMap;
	}



	/**
	 * ??????user???bpm,???????????????????????????
	 *
	 * @param syncToBpmMap
	 * @return
	 */
	private void syncPortalUserToBpm(Map<String, List<User>> syncToBpmMap, Boolean initFlag) {
		List<User> addList = syncToBpmMap.get("addList");
		List<User> modiList = syncToBpmMap.get("modiList");
		List<User> delList = syncToBpmMap.get("delList");
		List<User> addListProcess = syncToBpmMap.get("addListProcess");
		try {
			if (!CollectionUtils.isEmpty(modiList)) {
				for (User user : modiList) {
					String operatorType = "1".equals(user.getSuspendFlag()) ?
							UmsWebServiceUtils.UMS_USER_OPERATE_TYPE__SUSPEND :
							UmsWebServiceUtils.UMS_USER_OPERATE_TYPE_MODI;
					Map<String, Object> syscUserResult = umsWebServiceUtils.syscUserData(user, operatorType);
					if ((Boolean) syscUserResult.get("result")) {
						logger.info("update portal user to bpm success, user name:" + user.getLoginName());
					} else {
						logger.error("update portal user to bpm failed, user name:" + user.getLoginName());
						if (null != syscUserResult.get("message")) {
							String msg = syscUserResult.get("message").toString();

							if (msg.contains("???????????????")) {
								logger.info("try re-add portal user to bpm , user name:" + user.getLoginName());
								addList.add(user);
							}
						}
					}
				}
			}

	/*		// ????????????, ???????????????
			for (User user : addListProcess) {
				// ??????????????????
				Map<String, Object> syscUserResult = UmsWebServiceUtils.syscUserData(user,
						// ????????????true, ??????????????????
						(initFlag ? UmsWebServiceUtils.UMS_USER_OPERATE_TYPE_ADD_FOR_SYNC
								: UmsWebServiceUtils.UMS_USER_OPERATE_TYPE_ADD_FOR_PROCEDURE));
				if ((Boolean) syscUserResult.get("result")) {
					logger.info("sync portal user to bpm success, user name:" + user.getLoginName());
					// ??????????????????????????????
//					Map<String, Object> syscRelationResult = UmsWebServiceUtils
//							.syscBusinessUserRelationData(user.getLoginName(), "8000|8000");
//					if ((Boolean) syscRelationResult.get("result")) {
//						logger.info("sync portal user relationship to bpm success, user name:" + user.getLoginName());
//					} else {
//						logger.error("sync portal user relationship to bpm failed, user name:" + user.getLoginName());
//					}
				} else {
					logger.error("sync portal user to bpm failed, user name:" + user.getLoginName());
				}

			}*/
			if (!CollectionUtils.isEmpty(addList)) {
				for (User user : addList) {
					// ??????????????????
					Map<String, Object> syscUserResult = umsWebServiceUtils.syscUserData(user,
							UmsWebServiceUtils.UMS_USER_OPERATE_TYPE_ADD);
					if ((Boolean) syscUserResult.get("result")) {
						logger.info("sync portal user to bpm success, user name:" + user.getLoginName());
						// ??????????????????????????????
//						Map<String, Object> syscRelationResult = UmsWebServiceUtils
//								.syscBusinessUserRelationData(user.getLoginName(), "8000|8000");
//						if ((Boolean) syscRelationResult.get("result")) {
//							logger.info(
//									"sync portal user relationship to bpm success, user name:" + user.getLoginName());
//						} else {
//							logger.error(
//									"sync portal user relationship to bpm failed, user name:" + user.getLoginName());
//						}
					} else {
						logger.error("sync portal user to bpm failed, user name:" + user.getLoginName());
					}

				}
			}

			if (!CollectionUtils.isEmpty(delList)) {
				for (User user : delList) {
					Map<String, Object> deleteResult = umsWebServiceUtils.syscUserData(user,
							UmsWebServiceUtils.UMS_USER_OPERATE_TYPE_DEL);
					if ((Boolean) deleteResult.get("result")) {
						logger.info("delete portal user to bpm success, user name:" + user.getLoginName());
					} else {
						logger.error("delete portal user to bpm failed, user name:" + user.getLoginName());
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * ?????????????????? ???????????????????????????  ?????? ???????????????????????????
	 * ?????? ??????  ?????????????????????
	 * ??????  ?????????????????????
	 *
	 * @param interfaceUser ??????????????????????????????????????? Key ????????? value ??????
	 * @return
	 */
	public List<String> getDelAccountList(List<String> interfaceUser) {

		//??????????????????????????????
		List<String> syncAccounts = orgSyncManager.getAccountsBySyncSrc(descr);
		if (null != syncAccounts && syncAccounts.size() != 0) {
			/* ??????interface??? Map
			1 ??????????????????
			2 ????????????????????????
			dellist  ?????????inter???????????????
			??????????????? ?????????????????????, ??????????????????-> ????????? ????????? ????????????????????????
			 */
			HashMap<String, Integer> acRecordMap = new HashMap();
			for (String s :
					syncAccounts) {
				acRecordMap.put(s, 1);
			}
			//??????
			List<String> interfaceUserForRemove = interfaceUser.parallelStream().filter(entry-> null!=acRecordMap.get(entry)).collect(Collectors.toList());
			//syncAccounts ???????????????list
			syncAccounts.removeAll(interfaceUserForRemove);
			logger.info("sync>> sync_account need to del size{}",syncAccounts.size());
			//interfaceUser ???????????????list
			interfaceUser.removeAll(interfaceUserForRemove);
			logger.info("sync>> sync_account need to insert size{}",interfaceUser.size());
			//?????????????????????
			if (null != syncAccounts && syncAccounts.size() != 0) {
				orgSyncManager.delAccountsInSyncAccounts(syncAccounts, descr);
			}
			if (null != interfaceUser && interfaceUser.size() != 0) {
				orgSyncManager.insertAccounts(interfaceUser, descr);
			}
		}else {
					//???????????????0, ????????????
					orgSyncManager.insertAccounts(interfaceUser, descr);
				}
				List<String> userDels =	orgSyncManager.getUserDelList(descr);
		return userDels;
	}


	public void testSuyanAddUserByFile(String jsonData) throws Exception {
		JSONArray entity = JSONObject.parseArray(jsonData);
		if (entity == null) {
			throw new Exception("??????????????????NULL");
		}
		// ???????????????????????????
		Map<String, List<User>> userMap = syncEpcUserToOsa(entity);
		// ???????????????BPM
		orgservice.postUserData(bpmUserUrl, userMap);
	}

	//??????json??????
	public static String readJsonFile(String fileName) {
		String jsonStr = "";
		try {
			File jsonFile = new File(fileName);
			FileReader fileReader = new FileReader(jsonFile);
			Reader reader = new InputStreamReader(new FileInputStream(jsonFile),"utf-8");
			int ch = 0;
			StringBuffer sb = new StringBuffer();
			while ((ch = reader.read()) != -1) {
				sb.append((char) ch);
			}
			fileReader.close();
			reader.close();
			jsonStr = sb.toString();
			return jsonStr;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}



}
