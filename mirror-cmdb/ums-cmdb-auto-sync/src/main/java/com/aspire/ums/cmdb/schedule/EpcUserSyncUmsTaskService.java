/**
 * 
 */
package com.aspire.ums.cmdb.schedule;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.aspire.ums.cmdb.sync.client.RbacServiceClient;
import com.google.common.collect.Lists;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import com.aspire.ums.cmdb.common.StringUtils;
import com.aspire.ums.cmdb.sync.client.LdapServiceClient;
import com.aspire.ums.cmdb.sync.entity.Office;
import com.aspire.ums.cmdb.sync.entity.OfficeUser;
import com.aspire.ums.cmdb.sync.entity.User;
import com.aspire.ums.cmdb.sync.util.ClassConvertUtil;
import com.aspire.ums.cmdb.sync.util.UmsWebServiceUtils;
import com.migu.tsg.microservice.atomicservice.ldap.dto.GetLdapUserResponse;
import com.migu.tsg.microservice.atomicservice.ldap.dto.InsertLdapMemberRequest;
import com.migu.tsg.microservice.atomicservice.ldap.dto.ListPagenationResponse;
import com.migu.tsg.microservice.atomicservice.ldap.dto.UpdateLdapMemberRequest;
import org.springframework.util.CollectionUtils;

/**
 * @author lupeng
 *
 */
@Component
@ConditionalOnExpression("${schedule.epcUserUms.flag:false}")
public class EpcUserSyncUmsTaskService {

	protected static Logger logger = LoggerFactory.getLogger(EpcUserSyncUmsTaskService.class);
	@Value("${ldapconfig.namespace:alauda}")
	private String namespace;

	@Value("${sysdata.Token.url}")
	private String sendUrl;
	@Value("${sysdata.Token.username}")
	private String userName;

	@Value("${sysdata.Token.password}")
	private String password;

	@Value("${sysdata.Epcuser.url}")
	private String sysUrl;
	
	@Value("${sysdata.Epcuser.applicationName}")
	private String applicationName;

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
	// private final static int pagesize = 200000;

	// private String md5 = "{MD5}";

	@Autowired
	private LdapServiceClient ldapServiceClient;

	@Autowired
	private RbacServiceClient rbacServiceClient;
	@Autowired
	private UmsWebServiceUtils umsWebServiceUtils;
	//"0 0/30 * * *  ?"
	@Scheduled(cron = "${syncEpcUserData.cron}")
	public void syncEpcUserData() {
		logger.info("?????????????????????UMS??????????????????...");
		try {
			sysEpcUserData();
		} catch (Exception e) {
			e.printStackTrace();
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

	public JSONArray getEpcUserData() throws Exception {
		// ??????token????????????
		JSONObject token = this.getToken();
		logger.info("+++++++++??????Token:" + token);
		if (token == null) {
			throw new Exception("?????????Token???NULL");
		}
		String access_token = (String) token.get("access_token");
		if (StringUtils.isEmpty(access_token)) {
			throw new Exception("????????????access_token??????");
		}
	//	String applicationName = "ZW_????????????";
		StringBuffer sendUrl = new StringBuffer(sysUrl).append("?" +"applicationName="+ applicationName+"&&token=" + access_token);
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet getMethod = new HttpGet(sendUrl.toString());
		// String value = token_type + " " + access_token;
		// logger.info("??????????????????value???" + value);
		 getMethod.addHeader("Authorization", "Bearer fafc1be3-4eb4-4f36-bdc2-9922d886d897");
		HttpResponse response = null;
		JSONArray entity = null;
		try {
			response = httpClient.execute(getMethod);
			// ??????????????????
			String responseStr = EntityUtils.toString(response.getEntity());
			logger.info("call sync users interface return is : " + sendUrl);
			logger.info("call sync users interface status is : " + response.getStatusLine().getStatusCode());
			logger.info("call sync users interface return is : " + responseStr);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				JSONObject jsonObject = JSONObject.parseObject(responseStr);
				entity = jsonObject.getJSONArray("entity");
				return entity;
			} else {
				logger.error("????????????????????????:" + sendUrl);
			}
		} catch (Exception e) {
			logger.error("????????????????????????:" + sendUrl, e);
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
		JSONArray entity = this.getEpcUserData();
		if (entity == null) {
			throw new Exception("??????????????????NULL");
		}
		logger.info("??????????????????????????????????????????:");
	}

	



	
}
