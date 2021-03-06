package com.migu.tsg.microservice.atomicservice.composite.controller.rbac;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.aspire.mirror.common.entity.PageResult;
import com.google.common.collect.Lists;
import com.migu.tsg.microservice.atomicservice.composite.Constants;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.DepartmentServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.rabc.UserServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.controller.CommonResourceController;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.ResAction;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LastLogCodeEnum;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LogCodeDefine;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.PayloadParseUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.ResourceAuthHelper;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.TreeBuildUtilsForDept;
import com.migu.tsg.microservice.atomicservice.composite.exception.BaseException;
import com.migu.tsg.microservice.atomicservice.composite.exception.ResultErrorEnum;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.IDepartmentService;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.DepartmentPayload;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.DepartmentQueryPagePayload;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.DepartmentResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.DepartmentTreePayload;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.DepartmentUserTreePayload;
import com.migu.tsg.microservice.atomicservice.composite.vo.rbac.RbacResource;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.UserResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.DepartmentCreateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.DepartmentCreateResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.DepartmentPageRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.DepartmentUpdateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.DepartmentUpdateResponse;
import com.migu.tsg.microservice.atomicservice.rbac.dto.UserCreateRequest;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.DepartmentDTO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.DepartmentUserDTO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.model.UserDTO;
import com.migu.tsg.microservice.atomicservice.rbac.dto.vo.DepartmentVO;

/**   
 * <p>
 * ????????????????????????
 * </p>
 * @title RbacDepartmentController.java
 * @package com.migu.tsg.microservice.atomicservice.composite.controller.rbac 
 * @author ?????????
 * @version 0.1 2019???3???5???
 */
@RestController
@LogCodeDefine("1050221")
public class RbacDepartmentController extends CommonResourceController implements IDepartmentService {
	
	/**
     * ???????????????
     */
    @Autowired
    private ResourceAuthHelper resAuthHelper;

	@Autowired
	private DepartmentServiceClient departmentClient;
	
	@Autowired
	private UserServiceClient userClient;

	/**
	 * ????????????
	 */
	@Override
	@ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "department", action = "create")
	public DepartmentResponse createdDepartment(DepartmentPayload departmentPayload) {
		RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
		//??????????????????
		DepartmentCreateRequest req=new DepartmentCreateRequest();
//		req.setParentId(departmentPayload.getParentId());
		req.setName(departmentPayload.getName());
		List<DepartmentDTO> dtoList=departmentClient.queryList(req);
		if(CollectionUtils.isNotEmpty(dtoList)) {
			throw new BaseException(LastLogCodeEnum.RESOURCE_ALREADY_EXIST, ResultErrorEnum.BIZ_NAME_ALREADY_EXIST);
		}
		//????????????
		resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx.getFlattenConstraints());
		// ???????????????????????????????????????
		DepartmentVO parentDepartment = departmentClient.findByPrimaryKey(departmentPayload.getParentId());
		if (parentDepartment == null) {
			throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.PARENT_DEPARTEMNT_NOT_EXIST);
		}
		// luowenbo 2020-07-24 ????????? ???==??? -> equals
		if (Constants.Rbac.DEPARTMENT_TYPE_TEMP.equals(parentDepartment.getDeptType()) && Constants.Rbac.DEPARTMENT_TYPE_FORMAL.equals(departmentPayload.getDeptType())) {
			throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.TEMP_DEPARTEMNT_NOT_ALLOWED);
		}
		DepartmentCreateRequest request=PayloadParseUtil.jacksonBaseParse(DepartmentCreateRequest.class, departmentPayload);
		DepartmentCreateResponse response=departmentClient.createdDepartment(request);
		return PayloadParseUtil.jacksonBaseParse(DepartmentResponse.class, response);
	}

	/**
	 * ????????????ID????????????
	 */
	@Override
	@ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "department", action = "delete")
	public ResponseEntity<String> deleteByPrimaryKey(@PathVariable("department_id") String departmentId) {
		RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
		DepartmentCreateRequest req=new DepartmentCreateRequest();
		req.setParentId(departmentId);
		List<DepartmentDTO> dtoList=departmentClient.queryList(req);
		//???????????????????????????????????????
		if(CollectionUtils.isNotEmpty(dtoList)) {
			throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.FATHER_DEPARTEMNT_NOT_DELETE);
		}
		
		UserCreateRequest userReq=new UserCreateRequest();
		userReq.setDeptId(departmentId);
		List<UserDTO> userList=userClient.queryList(userReq);
		//?????????????????????????????????
		if(CollectionUtils.isNotEmpty(userList)) {
			throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.DEPARTEMNT_EXIST_USER_NOT_DELETE);
		}
		
		//????????????
		resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx.getFlattenConstraints());
		ResponseEntity<String> response=departmentClient.deleteByPrimaryKey(departmentId);
		return response;
	}

	/**
	 * ????????????ID????????????
	 */
	@Override
	@ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "department", action = "update")
	public DepartmentResponse modifyByPrimaryKey(@PathVariable("department_id") String departmentId, 
			@RequestBody DepartmentPayload departmentPayload) {
		RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
		//??????????????????
		DepartmentVO vo=departmentClient.findByPrimaryKey(departmentId);
		DepartmentCreateRequest req=new DepartmentCreateRequest();
		req.setParentId(departmentPayload.getParentId());
		req.setName(departmentPayload.getName());
		List<DepartmentDTO> dtoList=departmentClient.queryList(req);
		if(CollectionUtils.isNotEmpty(dtoList)&&!vo.getName().equals(departmentPayload.getName())) {
			throw new BaseException(LastLogCodeEnum.RESOURCE_ALREADY_EXIST, ResultErrorEnum.BIZ_NAME_ALREADY_EXIST);
		}
		if (!StringUtils.isEmpty(departmentPayload.getParentId())) {
			DepartmentVO parentDepartment = departmentClient.findByPrimaryKey(departmentPayload.getParentId());
			if (parentDepartment == null) {
				throw new BaseException(LastLogCodeEnum.RESOURCE_NOT_EXIST, ResultErrorEnum.PARENT_DEPARTEMNT_NOT_EXIST);
			}
			// luowenbo 2020-07-24 ????????? ???==??? -> equals
			if (Constants.Rbac.DEPARTMENT_TYPE_TEMP.equals(parentDepartment.getDeptType()) && Constants.Rbac.DEPARTMENT_TYPE_FORMAL.equals(vo.getDeptType()) ) {
				throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.TEMP_DEPARTEMNT_NOT_ALLOWED);
			}
		}
		//????????????
		resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx.getFlattenConstraints());
		DepartmentUpdateRequest request=PayloadParseUtil.jacksonBaseParse(DepartmentUpdateRequest.class, departmentPayload);
		
		DepartmentUpdateResponse  response=departmentClient.modifyByPrimaryKey(departmentId, request);
		return PayloadParseUtil.jacksonBaseParse(DepartmentResponse.class, response);
	}

	/**
	 * ????????????ID????????????
	 */
	@Override
	@ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "department", action = "view")
	public DepartmentResponse findByPrimaryKey(@PathVariable("department_id") String departmentId) {
//		RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
//		//????????????
//		resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx.getFlattenConstraints());
//		if(StringUtils.isEmpty(departmentId)) {
//			throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BAD_REQUEST);
//		}
		DepartmentVO vo=departmentClient.findByPrimaryKey(departmentId);
		return PayloadParseUtil.jacksonBaseParse(DepartmentResponse.class, vo);
	}

	/**
	 * ??????????????????ID????????????
	 */
	@Override
	@ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "department", action = "view")
	public List<DepartmentResponse> listByPrimaryKeyArrays(@PathVariable("department_id") String departmentIds) {
		RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
		//????????????
		resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx.getFlattenConstraints());
		List<DepartmentVO> listVo=departmentClient.listByPrimaryKeyArrays(departmentIds);
		return PayloadParseUtil.jacksonBaseParse(DepartmentResponse.class, listVo);
	}

	/**
	 * ??????????????????
	 */
	@Override
	@ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "department", action = "view")
	public PageResult<DepartmentResponse> pageList(@RequestBody DepartmentQueryPagePayload request) {
		RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
		//????????????
		resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx.getFlattenConstraints());
		DepartmentPageRequest departmentPageRequest=PayloadParseUtil.jacksonBaseParse(DepartmentPageRequest.class, request);
		PageResult<DepartmentDTO> page=departmentClient.pageList(departmentPageRequest);
		PageResult<DepartmentResponse> response=new PageResult<DepartmentResponse>();
		BeanUtils.copyProperties(page, response);
		response.setResult(PayloadParseUtil.jacksonBaseParse(DepartmentResponse.class, page.getResult()));
		return response;
	}

	@Override
	@ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "department", action = "view")
	public List<DepartmentResponse> queryList(@RequestBody DepartmentPayload departmentPayload) {
		/**
		 * zhujuwang 2019.10.11 ?????????????????? ?????????????????????????????????????????????
		 * ????????????????????????????????????????????????, ??????????????????????????????????????????, ???????????????????????????. ????????? ????????????. ??????????????????
		 */
//		RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
//		//????????????
//		resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx.getFlattenConstraints());
		DepartmentCreateRequest req=PayloadParseUtil.jacksonBaseParse(DepartmentCreateRequest.class, departmentPayload);
		List<DepartmentDTO> dtoList=departmentClient.queryList(req);
		return PayloadParseUtil.jacksonBaseParse(DepartmentResponse.class, dtoList);
	}

	@Override
	@ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "department", action = "view")
	public List<DepartmentTreePayload> queryTree(@RequestBody DepartmentPayload departmentPayload) {
		RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
		//????????????
		resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx.getFlattenConstraints());
		DepartmentCreateRequest req=PayloadParseUtil.jacksonBaseParse(DepartmentCreateRequest.class, departmentPayload);
		req.setTop(false);
		if(null==departmentPayload.getParentId()||"".equals(departmentPayload.getParentId())) {
			req.setTop(true);
		}else {
			req.setParentId(departmentPayload.getParentId());
		}
		List<DepartmentDTO> dtoList=departmentClient.queryList(req);
		List<DepartmentTreePayload>  returnList = PayloadParseUtil.jacksonBaseParse(DepartmentTreePayload.class, dtoList);


		//getTree(returnList);

		return returnList;
	}

	/**
	 * ???????????????
	 * @param dlist
	 * @return
	 * @throws Exception
	 */
	public List<DepartmentTreePayload> getdept(List<DepartmentTreePayload> dlist,String parentId){

		//??????????????????
		List<DepartmentTreePayload> treeList;
		long start = System.currentTimeMillis();
		//??????
		treeList = TreeBuildUtilsForDept.build(dlist,parentId);

		long end = System.currentTimeMillis();
		System.out.println("???????????????" + (end  - start) + "ms");
		return treeList;
	}


    /**
     * <p>
     * ???????????????
     * </p>
     * @author ?????????
     * @version 0.1 2019???3???14???
     * @param reuqest
     * void
     */
    private void getTree(List<DepartmentTreePayload> reuqest){
    	for(DepartmentTreePayload reqPayload:reuqest) {
    		DepartmentCreateRequest reqCre=new DepartmentCreateRequest();
    		reqCre.setParentId(reqPayload.getUuid());
    		List<DepartmentDTO> queryList = departmentClient.queryList(reqCre);
			List<DepartmentTreePayload> childList = PayloadParseUtil.jacksonBaseParse(DepartmentTreePayload.class,queryList);
			reqPayload.setChildList(childList);
			getTree(childList);
    	}
    }
	@Override
	@ResponseStatus(HttpStatus.OK)
	@ResAction(resType = "department", action = "view")
	public Map<String, Object> deptTree(String deptId) {
		RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
		//????????????
		resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(), reqCtx.getFlattenConstraints());
		DepartmentUserTreePayload departmentUserTreePayload = new DepartmentUserTreePayload();
		Map<String,Object> map = new HashMap<>();
		List<DepartmentDTO> dtoList=departmentClient.queryByDeptId(deptId);
		if (CollectionUtils.isNotEmpty(dtoList)){
			List<DepartmentUserTreePayload> returnList = PayloadParseUtil.jacksonBaseParse(DepartmentUserTreePayload.class, dtoList);
			getTreeAndUser(returnList);
			departmentUserTreePayload.setChildList(returnList);
			map.put("deptList",returnList);
		}else {
			map.put("deptList",null);
		}
		List<UserDTO> userDTOList = userClient.getByDefId(deptId);
		if (CollectionUtils.isNotEmpty(userDTOList)){
			List<UserResponse> userPayloadList = PayloadParseUtil.jacksonBaseParse(UserResponse.class,userDTOList);
			departmentUserTreePayload.setUserPayloadList(userPayloadList);
			map.put("userList",userPayloadList);
		}else {
			map.put("userList",null);
		}
		return map;
	}

	/**
	 * <p>
	 * </p>
	 * @param reuqest
	 * void
	 */
	private void getTreeAndUser(List<DepartmentUserTreePayload> reuqest){
		for(DepartmentUserTreePayload reqPayload:reuqest) {
			List<DepartmentDTO> queryList = departmentClient.queryByDeptId(reqPayload.getUuid());
			if(CollectionUtils.isNotEmpty(queryList)){
				reqPayload.setHasChild(1);
			}
			List<UserDTO> userDTOList = userClient.getByDefId(reqPayload.getUuid());
			if (CollectionUtils.isNotEmpty(userDTOList)){
				List<UserResponse> userPayloadList = PayloadParseUtil.jacksonBaseParse(UserResponse.class,userDTOList);
				reqPayload.setUserPayloadList(userPayloadList);
				reqPayload.setHasChild(1);
			}
			List<DepartmentUserTreePayload> childList = PayloadParseUtil.jacksonBaseParse(DepartmentUserTreePayload.class,queryList);
			reqPayload.setChildList(childList);
		}
	}

    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "department", action = "view")
    public Map<String, Object> deptUserTree(String deptId) {
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        // ????????????
        resAuthHelper.resourceActionVerify(reqCtx.getUser(), new RbacResource(), reqCtx.getResAction(),
                reqCtx.getFlattenConstraints());

        Map<String, Object> map = new HashMap<>();
        List<DepartmentUserDTO> dtoList = Lists.newArrayList();
        if (org.apache.commons.lang3.StringUtils.isBlank(deptId)) {
            DepartmentUserDTO dto = new DepartmentUserDTO();
            dto.setType("depart");
            dto.setName("????????????");
            dto.setParentId("9000000");
            dto.setUuid("179744684");
            dtoList.add(dto);
        } else {
            dtoList = departmentClient.queryDepartAndUser(deptId);
        }
        List<DepartmentUserDTO> deptList = Lists.newArrayList();
        List<DepartmentUserDTO> userList = Lists.newArrayList();
        for (DepartmentUserDTO dto : dtoList) {
            if ("depart".equals(dto.getType())) {
                deptList.add(dto);
            }
            if ("user".equals(dto.getType())) {
                userList.add(dto);
            }
        }
        if (CollectionUtils.isNotEmpty(deptList)) {
            map.put("deptList", deptList);
        }
        if (CollectionUtils.isNotEmpty(userList)) {
            map.put("userList", userList);
        }
        return map;
    }
}
