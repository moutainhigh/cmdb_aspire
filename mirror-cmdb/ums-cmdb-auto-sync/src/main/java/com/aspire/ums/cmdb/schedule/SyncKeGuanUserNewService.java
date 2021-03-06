package com.aspire.ums.cmdb.schedule;

import com.aspire.mirror.common.util.DateUtil;
import com.aspire.ums.cmdb.common.StringUtils;
import com.aspire.ums.cmdb.sync.client.LdapServiceClient;
import com.aspire.ums.cmdb.sync.client.UserServiceClient;
import com.aspire.ums.cmdb.sync.util.Md5Util;
import com.aspire.ums.cmdb.vo.KgLdapUser;
import com.aspire.ums.cmdb.vo.KgLdapUserMapper;
import com.google.common.collect.Lists;
import com.migu.tsg.microservice.atomicservice.ldap.dto.GetLdapUserResponse;
import com.migu.tsg.microservice.atomicservice.ldap.dto.InsertLdapMemberRequest;
import com.migu.tsg.microservice.atomicservice.ldap.dto.ListPagenationResponse;
import com.migu.tsg.microservice.atomicservice.ldap.dto.UpdateLdapMemberRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserBatchCreateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserCreateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserUpdateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.ldap.NamingException;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.PagedResultsControl;
import javax.naming.ldap.PagedResultsResponseControl;
import java.util.*;

/**
 * @Author: huanggongrui
 * @Description: ????????????????????????CMDB????????????
 * @Date: create in 2020/7/21 18:29
 */
@Component
@Slf4j
@ConditionalOnExpression("${schedule.kgSync.flag:false}")
public class SyncKeGuanUserNewService {

    @Value("${ldapconfig.namespace:alauda}")
    private String namespace;
    private static final String USER_TYPE = "user.alauda";
    @Value("${kgSync.user.roleId:4bf87b9a-b885-431f-a891-b9440c0fd1d3}")
    private String userRoleId;
    @Value("${kgSync.user.deptId:1001}")
    private String deptId;
    @Value("${kgSync.user.initPwd:1234qwery}")
    private String initPwd;

    @Autowired
    private LdapTemplate ldapTemplate;

    @Autowired
    private UserServiceClient userClient;

    @Resource
    private LdapServiceClient ldapUserServiceClient;

    private MapperFactory mapperFactory = new DefaultMapperFactory.Builder().build();

    @Async
    @Scheduled(cron = "${kgSync.user.cron: 0 2 * * * ?}")
    public void syncUser() {
        // get user data from ldap server
        log.info("???????????????????????????");
        List<KgLdapUser> kgLdapUserList = getUserListFromLdap();
        if (!CollectionUtils.isEmpty(kgLdapUserList)) {
            syncEpcUserToOsa(kgLdapUserList);
            log.info("???????????????????????????");
        } else {
            log.info("???????????????????????????");
        }
    }

    /**
     * ?????????ldap??????ldap??????, ??????????????????
     * @since 2020-08-17 15:22:22
     * @return
     */
    private List<KgLdapUser> getUserListFromLdap() {
        List<KgLdapUser> rtnList = Lists.newArrayList();
        try {
//            List<KgLdapUser> rtn11List = ldapTemplate.search("ou=users,dc=cmri", "(&(objectClass=cmcc-abstractPerson))", SearchControls.ONELEVEL_SCOPE, new KgLdapUserMapper());
            SearchControls schCtrls = new SearchControls();
            //??????????????????
            /*
             * 0:OBJECT_SCOPE,??????????????????????????????
             * 1:ONELEVEL_SCOPE,???????????????????????????????????????????????????????????????
             * 2:SUBTREE_SCOPE,???????????????????????????????????????????????????  **/
            schCtrls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            int pageSize = 500;
            byte[] cookie = null;
            ContextSource contextSource = ldapTemplate.getContextSource();
            DirContext ctx = contextSource.getReadWriteContext();
            LdapContext lCtx = (LdapContext) ctx;
            //??????
            lCtx.setRequestControls(new Control[] { new PagedResultsControl(  pageSize, Control.CRITICAL) });
            int totalResults = 0;
            KgLdapUserMapper kgLdapUserMapper = new KgLdapUserMapper();
            do {
                //??????????????????????????????
                NamingEnumeration<SearchResult> results = lCtx.search("ou=users,dc=cmri", "(&(objectClass=cmcc-abstractPerson))", schCtrls);
                while (null != results && results.hasMoreElements()) {//????????????????????????
                    SearchResult sr = results.next();
                    Attributes attrs = sr.getAttributes();
                    KgLdapUser kgLdapUser = kgLdapUserMapper.mapFromAttributes(attrs);
                    rtnList.add(kgLdapUser);
                    totalResults++;
                }
                //cookie???????????????????????????????????????PagedResultsControl??????????????????????????????????????????
                cookie = parseControls(lCtx.getResponseControls());
                lCtx.setRequestControls(new Control[] { new PagedResultsControl(  pageSize, cookie, Control.CRITICAL) });
            } while ((cookie != null) && (cookie.length != 0));
            lCtx.close();
            log.info("Total = " + totalResults);

            log.info("???????????????ldap????????????: {}", rtnList.size());
        } catch (Exception e) {
            log.error("???????????????ldap????????????", e);
        }
        return rtnList;
    }

    private byte[] parseControls(Control[] controls)
            throws NamingException {
        byte[] cookie = null;
        if (controls != null) {
            for (int i = 0; i < controls.length; i++) {
                if (controls[i] instanceof PagedResultsResponseControl) {
                    PagedResultsResponseControl prrc = (PagedResultsResponseControl) controls[i];
                    cookie = prrc.getCookie();
                    System.out.println(">>Next Page \n");
                }
            }
        }
        return (cookie == null) ? new byte[0] : cookie;
    }

    private InsertLdapMemberRequest convertToAnotherLdap(KgLdapUser user) {
        mapperFactory.classMap(KgLdapUser.class, InsertLdapMemberRequest.class)
                .field("cn", "name")
                .byDefault().register();
        MapperFacade mapper = mapperFactory.getMapperFacade();
        InsertLdapMemberRequest request = mapper.map(user, InsertLdapMemberRequest.class);
        request.setUsername(user.getUid());
        request.setType(USER_TYPE);
        request.setDept(user.getO());
        request.setPassword(StringUtils.isEmpty(user.getUserPassword()) ? initPwd:user.getUserPassword());
        request.setDescription("uim " + user.getRole() + " user");
        return request;
    }

    private UserCreateRequest convertLdapUser(KgLdapUser domain) {
        mapperFactory.classMap(KgLdapUser.class, UserCreateRequest.class)
                .field("cn", "name")
                .field("uid", "code")
                .field("uid", "ldapId")
                .field("gender", "sex")
                // .field("telephoneNumber", "phone")
                .byDefault().register();
        MapperFacade mapper = mapperFactory.getMapperFacade();
        UserCreateRequest userData = mapper.map(domain, UserCreateRequest.class);
        String id = Md5Util.getMD5String(domain.getUid());
        String o = StringUtils.isEmpty(domain.getO())?deptId : domain.getO();
        userData.setDeptId(o);
        List<String> oList = Lists.newArrayList();
        oList.add(o);
        userData.setDeptIds(oList);
        userData.setRoles(userRoleId);
        userData.setDescr("uim " + domain.getRole() + " user");
        userData.setNamespace(namespace);
        userData.setUserType(1);
        userData.setUserId(id);
        return userData;
    }

    private void syncEpcUserToOsa(List<KgLdapUser> ldapDataList) {
        List<String> projectss = new ArrayList();
        List<String> orderBy = new ArrayList();
        int pageSize = 200000;
        int currentPage = 1;

        log.info("SyncCmicUserData: filter cmic users data start");
        List<InsertLdapMemberRequest> addList = new ArrayList<>();
        List<UserCreateRequest> userCreateRequestList = Lists.newArrayList();

        ListPagenationResponse response = ldapUserServiceClient.listLdapMember(namespace, null, null, projectss, orderBy,
                pageSize, currentPage);
        List<GetLdapUserResponse> results = response.getResults();
        log.info("SyncCmicUserData listLdapMember");
        if (results.size() > 0) {
            boolean flag;
            for (KgLdapUser ldapData: ldapDataList) {
                flag = false;
                String userName = ldapData.getUid();
                for (GetLdapUserResponse result: results) {
                    if (!StringUtils.isEmpty(userName) && userName.equals(result.getUsername())) {
                        flag = true;
                        String password = ldapData.getUserPassword();
                        String mail = ldapData.getMail();
                        String mobile = ldapData.getMobile();
                        String o = ldapData.getO();
                        String cn = ldapData.getCn();
                        if ((!StringUtils.isEmpty(password) && !password.equals(result.getPassword())) ||
                                (!StringUtils.isEmpty(mail) && !mail.equals(result.getMail())) ||
                                (!StringUtils.isEmpty(mobile) && !mobile.equals(result.getMobile())) ||
                                (!StringUtils.isEmpty(cn) && !cn.equals(result.getName())) ||
                                (!StringUtils.isEmpty(o) && !o.equals(result.getDept()))) {
                            UpdateLdapMemberRequest request = new UpdateLdapMemberRequest();
                            request.setOldPassword(result.getPassword());
                            request.setNewPassword(password);
                            request.setMail(mail);
                            request.setMobile(mobile);
                            request.setUpdateTime(true);
                            request.setDept(o);
                            request.setName(cn);
                            request.setDescription("uim " + ldapData.getRole() + " user at " + DateUtil.formatDate(DateUtil.DATE_TIME_CH_FORMAT));
                            log.info("update ???????????????, username is : {}", userName);
                            // ??????
                            try {
                                ldapUserServiceClient.updateLdapMember(namespace, result.getUsername(), request);
                                UserVO userVO = userClient.findByLdapId(userName);
//                                String id = Md5Util.getMD5String(userName);
//                                List<UserVO> userVOList = userClient.listByPrimaryKeyArrays(id);
                                if (userVO != null && !StringUtils.isEmpty(userVO.getUuid())) {
                                    UserUpdateRequest userUpdateRequest = new UserUpdateRequest();
                                    userUpdateRequest.setMail(mail);
                                    userUpdateRequest.setMobile(mobile);
                                    userUpdateRequest.setDeptId(o);
                                    userUpdateRequest.setName(cn);
                                    List<String> oList = Lists.newArrayList();
                                    oList.add(o);
                                    userUpdateRequest.setDeptIds(oList);
                                    userUpdateRequest.setDescr("update at " + DateUtil.formatDate(DateUtil.DATE_TIME_CH_FORMAT));
                                    userClient.modifyByPrimaryKey(userVO.getUuid(), userUpdateRequest);
                                } else {
                                    userCreateRequestList.add(convertLdapUser(ldapData));
                                }
                            } catch (Exception e) {
                                log.error("???????????? {}", e);
                            }
                        }
                    }
                }
                if (!flag) {
                    addList.add(convertToAnotherLdap(ldapData));
                    userCreateRequestList.add(convertLdapUser(ldapData));
                }
            }
        } else {
            for (KgLdapUser ldapData: ldapDataList) {
                addList.add(convertToAnotherLdap(ldapData));
                userCreateRequestList.add(convertLdapUser(ldapData));
            }
        }

        // ????????????
        try {
            if (!addList.isEmpty()) {
                ldapUserServiceClient.insertLdapMembers(namespace, addList);
            }
            if (!userCreateRequestList.isEmpty()) {
                UserBatchCreateRequest user = new UserBatchCreateRequest();
                user.setListUser(userCreateRequestList);
                userClient.batchCreatedUser(user);
            }
        } catch (Exception e) {
            log.error("??????????????????????????? {}", e);
            // ????????????
            if (!addList.isEmpty()) {
                for (InsertLdapMemberRequest user: addList) {
                    ldapUserServiceClient.deleteLdapMember(namespace, user.getUsername());
                }
            }
        }

        boolean flag;
        for (GetLdapUserResponse result: results) {
            String userName = result.getUsername();
            String description = result.getDescription();
            flag = false;
            for (KgLdapUser ldapData : ldapDataList) {
                String kgUserName = ldapData.getUid();
                if (!StringUtils.isEmpty(userName) && userName.equals(kgUserName)) {
                    flag = true;
                    break;
                }
            }

            if (!flag && (!StringUtils.isEmpty(description) && description.startsWith("uim"))) {
                try {
                    ldapUserServiceClient.deleteLdapMember(namespace, userName);
                    UserVO userVO = userClient.findByLdapId(userName);
                    if (userVO != null && !StringUtils.isEmpty(userVO.getUuid())) {
                        userClient.deleteByPrimaryKey(userVO.getUuid());
                    }
                } catch (Exception e) {
                    log.error("?????????????????????????????????{}???{}", userName, e);
                    try {
                        InsertLdapMemberRequest insertLdapMemberRequest = new InsertLdapMemberRequest();
                        insertLdapMemberRequest.setDept(result.getDept());
                        insertLdapMemberRequest.setDescription(result.getDescription());
                        insertLdapMemberRequest.setMail(result.getMail());
                        insertLdapMemberRequest.setMobile(result.getMobile());
                        insertLdapMemberRequest.setName(result.getName());
                        insertLdapMemberRequest.setPassword(result.getPassword());
                        insertLdapMemberRequest.setProjects(result.getProjects());
                        insertLdapMemberRequest.setUsername(result.getUsername());
                        List<InsertLdapMemberRequest> list = Lists.newArrayList();
                        ldapUserServiceClient.insertLdapMembers(namespace, list);
                    } catch (Exception e1) {
                        log.error("?????????????????????????????????{}???{}", userName, e);
                    }

                }

            }
        }
    }
}
