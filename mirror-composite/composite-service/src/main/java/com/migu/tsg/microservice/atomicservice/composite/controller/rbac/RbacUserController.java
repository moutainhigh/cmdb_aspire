package com.migu.tsg.microservice.atomicservice.composite.controller.rbac;

import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import cn.afterturn.easypoi.handler.inter.IExcelDataHandler;
import com.aspire.mirror.common.entity.PageResult;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.migu.tsg.microservice.atomicservice.composite.Constants;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.DepartmentServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.UserServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.controller.CommonResourceController;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.ResAction;
import com.migu.tsg.microservice.atomicservice.composite.controller.handler.UserExcelHandler;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LastLogCodeEnum;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LogCodeDefine;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.EasyPoiUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.PayloadParseUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.ResourceAuthHelper;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.Upload;
import com.migu.tsg.microservice.atomicservice.composite.exception.BaseException;
import com.migu.tsg.microservice.atomicservice.composite.exception.ResultErrorEnum;
import com.migu.tsg.microservice.atomicservice.composite.helper.SmsSendHelper;
import com.migu.tsg.microservice.atomicservice.composite.service.common.payload.BaseResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.IUserService;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.DepartmentResponse;
import com.migu.tsg.microservice.atomicservice.composite.vo.rbac.RbacResource;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.UserPayload;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.UserPicturePayload;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.UserQueryPagePayload;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.UserResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserCreateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserCreateResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserPageRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserUpdateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserUpdateResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.DepartmentDTO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.UserDTO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.vo.DepartmentVO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.vo.UserVO;
import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.util.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import sun.misc.BASE64Encoder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * ????????????????????????
 * </p>
 *
 * @author ?????????
 * @version 0.1 2019???3???5???
 * @title RbacUserController.java
 * @package com.migu.tsg.microservice.atomicservice.composite.controller.rbac
 */
@SuppressWarnings("restriction")
@RestController
@LogCodeDefine("1050221")
public class RbacUserController extends CommonResourceController implements IUserService {

    /**
     * ???????????????
     */
    @Autowired
    private ResourceAuthHelper resAuthHelper;

    @Autowired
    private UserServiceClient userClient;

    @Autowired
    private DepartmentServiceClient departmentClient;

    @Autowired
    private SmsSendHelper smsSendHelper;

    Logger logger = LoggerFactory.getLogger(RbacUserController.class);

    /**
     * ????????????
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "user", action = "create")
    public UserPayload createdUser(UserPayload userPayload, @RequestParam(name = "picture", required = false)
            MultipartFile picture) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        //????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx
                .getFlattenConstraints());
        UserVO user = userClient.findByCode(userPayload.getCode());
        if (user != null) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_ALREADY_EXIST, ResultErrorEnum.BIZ_NAME_ALREADY_EXIST);
        }
        List<DepartmentVO> listDepartment = departmentClient.listByPrimaryKeyArrays(userPayload.getDeptIds());
        if (CollectionUtils.isEmpty(listDepartment)) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.DEPARTMENT_NOT_FIND);
        } else {
            int formalDeptNum = 0;
            for (DepartmentVO departmentVO : listDepartment) {
                if (departmentVO.getDeptType().equals(Constants.Rbac.DEPARTMENT_TYPE_FORMAL)) {
                    formalDeptNum++;
                }
            }
            if (formalDeptNum > 1) {
                throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum
                        .FORMAL_DEPARTEMNT_NOT_ALLOW_MULTIPLE);
            }
        }
        UserCreateRequest request = PayloadParseUtil.jacksonBaseParse(UserCreateRequest.class, userPayload);
        request.setDeptIds(Arrays.asList(userPayload.getDeptIds().split(",")));
        request.setNamespace(reqCtx.getUser().getNamespace());
        if (picture != null) {
            BASE64Encoder encoder = new BASE64Encoder();
            // ??????base64???????????????
            try {
                String data = encoder.encode(picture.getBytes());
                request.setPicture(data);
            } catch (IOException e) {
            }
        }

        UserCreateResponse response = userClient.createdUser(request);
        return PayloadParseUtil.jacksonBaseParse(UserPayload.class, response);
    }

    /**
     * ????????????ID????????????
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "user", action = "delete")
    public ResponseEntity<String> deleteByPrimaryKey(String userId) {

        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        //????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx
                .getFlattenConstraints());

        UserVO user = userClient.findByPrimaryKey(userId);
        if (user != null && !StringUtils.isEmpty(user.getLdapId())) {
            throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.USER_CONNECTED_ACCTOUNT_DELETE);
        }
        UserCreateRequest param = new UserCreateRequest();
        param.setRelationPerson(userId);
        List<UserDTO> listUser = userClient.queryList(param);
        if (!CollectionUtils.isEmpty(listUser)) {
            throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.USER_CONNECTED_USER_DELETE);
        }
        ResponseEntity<String> response = userClient.deleteByPrimaryKey(userId);
        return response;
    }

    /**
     * ????????????ID????????????
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "user", action = "update")
    public UserPayload modifyByPrimaryKey(@PathVariable(name = "user_id", required = false) String userId,
                                          UserPayload userPayload, @RequestParam(name = "picture", required = false)
                                                  MultipartFile picture) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        //????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx
                .getFlattenConstraints());
        UserUpdateRequest request = PayloadParseUtil.jacksonBaseParse(UserUpdateRequest.class, userPayload);
        List<DepartmentVO> listDepartment = departmentClient.listByPrimaryKeyArrays(userPayload.getDeptIds());
        if (CollectionUtils.isEmpty(listDepartment)) {
            throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.DEPARTMENT_NOT_FIND);
        } else {
            int formalDeptNum = 0;
            for (DepartmentVO departmentVO : listDepartment) {
                if (departmentVO.getDeptType().equals(Constants.Rbac.DEPARTMENT_TYPE_FORMAL)) {
                    formalDeptNum++;
                }
            }
            if (formalDeptNum > 1) {
                throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum
                        .FORMAL_DEPARTEMNT_NOT_ALLOW_MULTIPLE);
            }
        }
        request.setDeptIds(Arrays.asList(userPayload.getDeptIds().split(",")));
        if (picture != null) {
            BASE64Encoder encoder = new BASE64Encoder();
            // ??????base64???????????????
            try {
                String data = encoder.encode(picture.getBytes());
                request.setPicture(data);
            } catch (IOException e) {
            }
        }
        UserUpdateResponse response = userClient.modifyByPrimaryKey(userId, request);
        return PayloadParseUtil.jacksonBaseParse(UserPayload.class, response);
    }

    /**
     * ????????????ID????????????
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "user", action = "view")
    public UserPicturePayload findByPrimaryKey(String userId) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        //????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx
                .getFlattenConstraints());
        if (StringUtils.isEmpty(userId)) {
            throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BAD_REQUEST);
        }
        UserVO vo = userClient.findByPrimaryKey(userId);
        List<DepartmentResponse> list = PayloadParseUtil.jacksonBaseParse(DepartmentResponse.class, vo.getDeptList());
        UserPicturePayload result = PayloadParseUtil.jacksonBaseParse(UserPicturePayload.class, vo);
        result.setDeptList(list);
        return result;
    }

    /**
     * ??????????????????ID????????????
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "user", action = "view")
    public List<UserPayload> listByPrimaryKeyArrays(String userIds) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        //????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx
                .getFlattenConstraints());
        List<UserVO> listVo = userClient.listByPrimaryKeyArrays(userIds);
        return PayloadParseUtil.jacksonBaseParse(UserPayload.class, listVo);
    }

    /**
     * ??????????????????
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "user", action = "view")
    public PageResult<UserResponse> pageList(UserQueryPagePayload request) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        //????????????
//        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx
//                .getFlattenConstraints());
        UserPageRequest userPageRequest = PayloadParseUtil.jacksonBaseParse(UserPageRequest.class, request);
        PageResult<UserDTO> page = userClient.pageList(userPageRequest);
        PageResult<UserResponse> response = new PageResult<UserResponse>();
        BeanUtils.copyProperties(page, response);
        if (page.getResult() != null) {
            for (UserDTO userDTO : page.getResult()) {
                List<String> listDeptName = userDTO.getDeptList().stream().map(DepartmentDTO::getName).collect
                        (Collectors.toList());
                userDTO.setDeptId(Joiner.on(",").join(listDeptName));
            }
            response.setResult(PayloadParseUtil.jacksonBaseParse(UserResponse.class, page.getResult()));
        }
        return response;
    }

    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "user", action = "view")
    public List<UserResponse> queryList(UserPayload userPayload) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        //????????????
//        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx
//                .getFlattenConstraints());
        UserCreateRequest req = PayloadParseUtil.jacksonBaseParse(UserCreateRequest.class, userPayload);
        if (!StringUtils.isEmpty(userPayload.getDeptIds())) {
            req.setDeptIds(Arrays.asList(userPayload.getDeptIds().split(",")));
        }
        List<UserDTO> dtoList = userClient.queryList(req);
        return PayloadParseUtil.jacksonBaseParse(UserResponse.class, dtoList);
    }

    @Override
    public void importTemplate(HttpServletResponse response) {
        String fileName = "user_template";

        EasyPoiUtil.exportExcel(new ArrayList<UserPayload>(), "??????????????????", "user", UserPayload.class, fileName, true,
                response);

    }

    @Override
    public int batchModifyDeptIdByUserIdArrays(@RequestParam("dept_id") String deptId, @RequestParam("user_ids")
            String userIds) {
        return userClient.batchModifyDeptIdByUserIdArrays(deptId, userIds);
    }

    @Override
    public UserResponse findByLdapId(@RequestParam("ldapId") String ldapId) {
        UserVO user = userClient.findByLdapId(ldapId);
        if (user != null) {
            List<DepartmentResponse> list = PayloadParseUtil.jacksonBaseParse(DepartmentResponse.class, user.getDeptList());
            UserResponse userResponse = PayloadParseUtil.jacksonBaseParse(UserResponse.class, user);
            userResponse.setDeptList(list);
            return userResponse;
        }
        return null;
    }

    @Override
    public BaseResponse sendSmsCaptcha(@RequestParam(name = "namespace") String namespace, @RequestParam(name = "username")
            String username, @RequestParam(name = "mobile") String mobile, HttpServletRequest request) {
        UserCreateRequest user = new UserCreateRequest();
        user.setLdapId(username);
        user.setNamespace(namespace);
        user.setMobile(mobile);
        List<UserDTO> userList = userClient.queryList(user);
        if (CollectionUtils.isEmpty(userList)) {
            throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BAD_REQUEST);
        }
        String randomCode = RandomStringUtils.randomNumeric(6);

        request.getSession().setMaxInactiveInterval(5 * 60);
        request.getSession().setAttribute(mobile, randomCode);
        logger.info("??????????????????{}, ????????????????????? {}", mobile, randomCode);
        String text = MessageFormat.format("{0}, ?????????????????????????????????: {1}, ?????????????????????????????????.[????????????UMS]", userList.get(0).getName(),
                randomCode);
        List<String> mobileList = Lists.newArrayList();
        mobileList.add(mobile);
        if (!smsSendHelper.send(mobileList, text)) {
            throw new BaseException(LastLogCodeEnum.RPC_INVOKE_ERROR, ResultErrorEnum.UNKNOWN_ISSUE);
        }
        return new BaseResponse();
    }

    @Override
    public BaseResponse validSmsCaptcha(@RequestParam(name = "mobile") String mobile, @RequestParam(name =
            "validCode") String validCode, HttpServletRequest request) {
        Object validCode2 = request.getSession().getAttribute(mobile);
        logger.info("???????????????{}, session??????????????? :{}", mobile, validCode2);
        if (validCode == null || !validCode.equals(validCode2)) {
            throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.INVALID_LOGIN_CODE);
        }
        return new BaseResponse();
    }

    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "user", action = "view")
    public void excelExport(UserPayload userPayload, HttpServletResponse response) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        //????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx
                .getFlattenConstraints());
        String fileName = "user_export";
//		response.setHeader("contentType", "application/vnd.ms-excel");
//		try {
//			response.setHeader("Content-Disposition", "attachment;filename*= UTF-8''"+URLEncoder.encode(fileName,
// "UTF-8"));
//		} catch (UnsupportedEncodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
        UserCreateRequest req = PayloadParseUtil.jacksonBaseParse(UserCreateRequest.class, userPayload);
        req.setExcel(true);
        List<UserDTO> dtoList = userClient.queryList(req);
        List<UserPayload> data = PayloadParseUtil.jacksonBaseParse(UserPayload.class, dtoList);

        EasyPoiUtil.exportExcel(data, "??????????????????", "user", UserPayload.class, fileName, true, response);
    }

    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "user", action = "create")
    public ResponseEntity<String> excelImport(@RequestParam("file") MultipartFile file) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        //????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx
                .getFlattenConstraints());
        ImportParams importParams = new ImportParams();
        importParams.setTitleRows(1);
        // ????????????
        IExcelDataHandler<UserPayload> handler = new UserExcelHandler(departmentClient, userClient);
        handler.setNeedHandlerFields(new String[]{"??????", "?????????"});// ????????????????????????excel????????????????????????????????????????????????
        importParams.setDataHandler(handler);

        // ????????????
        importParams.setNeedVerfiy(true);
        try {
            ExcelImportResult<UserPayload> result = ExcelImportUtil.importExcelMore(file.getInputStream(),
                    UserPayload.class,
                    importParams);

            List<UserPayload> successList = result.getList();
            List<UserPayload> failList = result.getFailList();
            List<UserPayload> tempList = Lists.newArrayList();
            tempList.addAll(successList);
            Set<String> userNameSet = Sets.newHashSet();
            for (UserPayload user : tempList) {
                if (userNameSet.contains(user.getCode())) {
                    successList.remove(user);
                } else {
                    UserVO paramUser = userClient.findByCode(user.getCode());
                    if (paramUser != null) {
                        failList.add(user);
                        successList.remove(user);
                    }
                    userNameSet.add(user.getCode());
                }
            }
            Set<String> failUserNameSet = Sets.newHashSet();
            if (!CollectionUtils.isEmpty(failList)) {
                for (UserPayload user : failList) {
                    failUserNameSet.add(user.getCode());
                }
            }


            List<UserCreateRequest> list = PayloadParseUtil.jacksonBaseParse(UserCreateRequest.class, successList);
            for (UserCreateRequest u : list) {
                u.setNamespace(reqCtx.getUser().getNamespace());
                // ???????????????????????????
                List<String> deptIds = Lists.newArrayList();
                deptIds.add(u.getDeptId());
                u.setDeptIds(deptIds);
                userClient.createdUser(u);
            }
            return new ResponseEntity<String>(CollectionUtils.isEmpty(failList) ? "????????????" : "???????????????????????????????????????????????????: " +
                    Joiner.on(",").join(failUserNameSet) + "??????????????? ?????????????????????", HttpStatus.OK);
        } catch (Exception e) {
            // ?????????????????????????????????????????? luowenbo 20200730
            e.printStackTrace();
            return new ResponseEntity<String>("??????", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * ??????Excel???????????????
     *
     * @param file ???????????????????????????
     * @return
     */
//    @RequestMapping(value = "Index/layer/excelparser")
    @ResponseBody
    public void Excel(HttpServletRequest request, @RequestParam("file") MultipartFile file) throws Exception {
        Map<String, Object> dataMap = new HashMap<String, Object>();
        String fileName1 = request.getParameter("fileName");// ??????????????????????????????????????????????????????????????????
        String fileName = null;
        try {
            //??????????????????


            String uploadDir = request.getSession().getServletContext().getRealPath("/") + "upload/";
            uploadDir = uploadDir.substring(0, uploadDir.length() - 1);
            // luowenbo 2020-07-24 ????????? '\\' -> File.separator
            uploadDir = uploadDir + File.separator;//????????????
            String realPath = uploadDir + fileName1;//
            realPath = realPath.replace("\\",File.separator).replace("/",File.separator);
            File dir = new File(realPath);
            if (!dir.exists()) {
                dir.mkdir();
            }
            //??????????????????
            fileName = Upload.executeUpload1(uploadDir, file, fileName1);
            uploadDir = uploadDir.substring(0, uploadDir.length() - 1);
            dataMap.put("fileName", fileName);
            dataMap.put("dir", uploadDir);
        } catch (Exception e) {
            //????????????????????????
            e.printStackTrace();
        }
        ExcelParser(fileName);
    }

    public void ExcelParser(String fileName) throws Exception {
        ImportParams params = new ImportParams();
        params.setTitleRows(1);
        params.setHeadRows(1);
        List<UserCreateRequest> list = new ArrayList<>();
        list = Upload.importExcel("C:/Users/sl/Desktop/layer/layer/src/main/webapp/upload/" + fileName, 1, 1,
                UserCreateRequest.class);
        for (int i = 0; i < list.size(); i++) {
            userClient.createdUser(list.get(i));
        }
    }

}
