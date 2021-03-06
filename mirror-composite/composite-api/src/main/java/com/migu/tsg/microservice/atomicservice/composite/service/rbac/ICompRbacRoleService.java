package com.migu.tsg.microservice.atomicservice.composite.service.rbac;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.aspire.mirror.common.entity.PageResponse;
import com.aspire.mirror.common.entity.PageResult;
import com.migu.tsg.microservice.atomicservice.composite.ErrorsResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.common.payload.BaseResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.CompRoleCreatePayload;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RbacRoleListItem;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.ResourceRoleList;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RoleAddParentRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RoleDetailResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RoleDetailResponsePer;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RoleListResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RolePageRequestPayload;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RolePermission;
import com.migu.tsg.microservice.atomicservice.composite.service.rbac.payload.RoleUserPayload;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
* ??????????????????Controller
* Project Name:composite-service
* File Name:RbacRolesController.java
* Package Name:com.migu.tsg.microservice.atomicservice.composite.controller.rabc
* ClassName: RbacRolesController <br/>
* date: 2017???8???24??? ??????2:56:56 <br/>
* ??????????????????Controller
* @author pengguihua
* @version
* @since JDK 1.6
*/
@RequestMapping(value="${version}/roles", produces = "application/json;charset=UTF-8")
@Api(value = "Composite Rbac role service", description = "Composite Rbac role service")
public interface ICompRbacRoleService {
    /**
     * retrieveRoleByName?????????????????????
     * ????????? yangshilei
     * @param reqCtx
     * @param namespace
     * @param roleName
     * @return
     */
    @ApiOperation(value = "?????????????????????????????????", notes = "?????????????????????????????????", response = RoleDetailResponse.class,
            tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return the role detail data",
            response = RoleDetailResponse.class)})
    @GetMapping(path = "/{namespace}/getRoleByUserName/{user_name}", produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.OK)
    List<RoleDetailResponsePer> retrieveRoleByUserName(@PathVariable("namespace") String namespace,
                                              @PathVariable("user_name") String userName);
    /**
    * ???????????????????????? <br/>
    *
    * ????????? pengguihua
    * @param namespace
    * @param search
    * @return
    */
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????",
            response = RoleListResponse.class, tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List roles for the namespace",
    response = RoleListResponse.class)})
    @GetMapping(path = "/{namespace}")
    @ResponseStatus(HttpStatus.OK)
    RoleListResponse rolesList(@PathVariable("namespace") String namespace,
                                @RequestParam(name = "search", required = false) String search,
                                @RequestParam(name = "project_name", required = false) String projectName,
                                @RequestParam(name = "name", required = false) String Name);


    /**
     * ?????????????????????????????? <br/>
     *
     * ????????? pengguihua
     * @param namespace
     * @param search
     * @return
     */
     @ApiOperation(value = "??????????????????????????????", notes = "??????????????????????????????",
             response = PageResponse.class, tags = {"Composite Role service API"})
     @ApiResponses(value = {@ApiResponse(code = 200, message = "List roles for the namespace",
     response = PageResponse.class)})
     @PostMapping(path = "/pageList")
     @ResponseStatus(HttpStatus.OK)
    public PageResult<RbacRoleListItem> pageList(@RequestBody RolePageRequestPayload request);
    /**
    * ???????????????????????? <br/>
    *
    * ????????? pengguihua
    * @param namespace
    * @param search
    * @return
    */
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????",
            response = RoleListResponse.class, tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List roles for the namespace",
    response = RoleListResponse.class)})
    @GetMapping(path = "/rolesRerouceList")
    @ResponseStatus(HttpStatus.OK)
    RoleListResponse rolesRerouceList(@RequestParam(name = "id", required = false) String id,
    							@RequestParam(name = "name", required = false) String name);

    /**
    * ???????????????????????? <br/>
    *
    * ????????? pengguihua
    * @param namespace
    * @param search
    * @return
    */
    @ApiOperation(value = "?????????????????????", notes = "?????????????????????",
            response = RoleListResponse.class, tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List roles for the namespace",
    response = RoleListResponse.class)})
    @GetMapping(path = "/rolesRerouceAllList")
    @ResponseStatus(HttpStatus.OK)
    List<ResourceRoleList> rolesRerouceAllList(@RequestParam(name = "id", required = false) String id,
    							@RequestParam(name = "name", required = false) String name);
    /**
     * retrieveRoleByName?????????????????????
     * ????????? yangshilei
     * @param reqCtx
     * @param namespace
     * @param roleName
     * @return
     */
    @ApiOperation(value = "?????????????????????????????????", notes = "?????????????????????????????????", response = RoleDetailResponse.class,
            tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return the role detail data",
    response = RoleDetailResponse.class)})
    @GetMapping(path = "/{namespace}/{role_name}", produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.OK)
    RoleDetailResponse retrieveRoleByName(@PathVariable("namespace") String namespace,
                                                       @PathVariable("role_name") String roleName);
    /**
     * retrieveRoleByName?????????????????????
     * ????????? yangshilei
     * @param reqCtx
     * @param namespace
     * @param roleName
     * @return
     */
    @ApiOperation(value = "????????????ID??????????????????", notes = "????????????ID??????????????????", response = RoleDetailResponse.class,
            tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Return the role detail data",
    response = RoleDetailResponse.class)})
    @GetMapping(path = "/detail/{role_id}", produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.OK)
    RoleDetailResponsePer retrieveRoleById(@PathVariable("role_id") String roleId);
    /**
     * updateRoleByName?????????????????????
     * ????????? yangshilei
     * @param reqCtx
     * @param namespace
     * @param role_name
     * @return
     */
    @ApiOperation(value = "????????????", notes = "????????????", tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Role was modified successfully"),
                           @ApiResponse(code = 403, message = "User doesn't have permission to change role",
                           response = ErrorsResponse.class)})
    @PutMapping(path = "/{namespace}/{role_id}", produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    BaseResponse updateRoleById(@PathVariable("namespace") String namespace,
                                 @PathVariable("role_id")String roleId,
                                 @RequestBody CompRoleCreatePayload requestRole);

    /**
    * deleteRoleByName:(????????????). <br/>
    * ????????? yangshilei
    * @param reqCtx
    * @param namespace
    * @param role_name
    */
    @ApiOperation(value = "???????????????????????????", notes = "???????????????????????????", tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Deleted successfully"),
                           @ApiResponse(code = 403, message = "No permission to remove role",
                           response = ErrorsResponse.class),
                           @ApiResponse(code = 409, message = "Role is inherited by other roles and cannot be removed",
                           response = ErrorsResponse.class)})
    @DeleteMapping(path = "/{namespace}/{role_name}", produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    BaseResponse deleteRoleByName(@PathVariable("namespace") String namespace,
                                 @PathVariable("role_name") String roleName);

    /**
    * deleteRoleByName:(????????????). <br/>
    * ????????? yangshilei
    * @param reqCtx
    * @param namespace
    * @param role_name
    */
    @ApiOperation(value = "????????????ID????????????", notes = "????????????ID????????????", tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Deleted successfully"),
                           @ApiResponse(code = 403, message = "No permission to remove role",
                           response = ErrorsResponse.class),
                           @ApiResponse(code = 409, message = "Role is inherited by other roles and cannot be removed",
                           response = ErrorsResponse.class)})
    @DeleteMapping(path = "/delete/{namespace}/{role_id}", produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    BaseResponse deleteRoleById(@PathVariable("namespace") String namespace,
                                 @PathVariable("role_id") String roleId);
    /**
    * Create a one or more roles for the namespace.<br/>
    *
    * ????????? pengguihua
    * @param namespace
    * @param roleName
    * @param createRoleList
    * @return
    */
    @ApiOperation(value = "??????????????????", notes = "Create a one or more roles for the namespace",
                  response = CompRoleCreatePayload.class, tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "create role(s) successfully.",
    response = CompRoleCreatePayload.class),
                           @ApiResponse(code = 403, message = "Failed to create the role(s)",
                           response = ErrorsResponse.class)})
    @PostMapping(value = "/{namespace}", 
                 consumes = "application/json",
                    produces = "application/json; charset=UTF-8")
    @ResponseStatus(HttpStatus.OK)
    List<CompRoleCreatePayload> createRole(@PathVariable("namespace") String namespace,
                                           @RequestBody CompRoleCreatePayload createRole);
    /**
     * Create a one or more roles for the namespace.<br/>
     *
     * ????????? ?????????
     * @param namespace
     * @param roleName
     * @param createRoleList
     * @return
     */
     @ApiOperation(value = "??????????????????", notes = "Create a one or more roles for the namespace",
                   response = CompRoleCreatePayload.class, tags = {"Composite Role service API"})
     @ApiResponses(value = {@ApiResponse(code = 200, message = "create role(s) successfully.",
     response = CompRoleCreatePayload.class),
                            @ApiResponse(code = 403, message = "Failed to create the role(s)",
                            response = ErrorsResponse.class)})
     @PostMapping(value = "/resource/{namespace}", 
                  consumes = "application/json",
                     produces = "application/json; charset=UTF-8")
     @ResponseStatus(HttpStatus.OK)
     List<CompRoleCreatePayload> createResourceRole(@PathVariable("namespace") String namespace,
                                            @RequestBody CompRoleCreatePayload createRole);
    /**
    * add parent for the Role. <br/>
    *
    * ????????? pengguihua
    * @param roleName
    */
    @ApiOperation(value = "???????????????", notes = "???????????????", tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Parent role added successfully"),
                           @ApiResponse(code = 403, message = "Failed to add parent role.",
                           response = ErrorsResponse.class),
                           @ApiResponse(code = 404, message = "Parent role not found",
                           response = ErrorsResponse.class)})
    @PostMapping("/{namespace}/{role_name}/parents")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    BaseResponse addParent4Role(@PathVariable("namespace") String namespace,
                               @PathVariable("role_name") String roleName,
                               @RequestBody RoleAddParentRequest reqBody);
    /**
    * Remove one parent instance.<br/>
    *
    * ????????? pengguihua
    * @param namespace
    * @param roleName
    * @param parentUuid
    */
    @ApiOperation(value = "???????????????", notes = "???????????????", tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Parent role removed successfully"),
                           @ApiResponse(code = 403, message = "Failed to remove parent role.",
                           response = ErrorsResponse.class),
                           @ApiResponse(code = 404, message = "Parent role is not in the role.",
                           response = ErrorsResponse.class)})
    @DeleteMapping("/{namespace}/{role_name}/parents/{parent_uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    BaseResponse removeParent4Role(@PathVariable("namespace") String namespace,
                                  @PathVariable("role_name") String roleName,
                                  @PathVariable("parent_uuid") String parentUuid);
    /**
    * addPermission:(??????????????????). <br/>
    * ????????? yangshilei
    * @param reqCtx
    * @param namespace
    * @param roleName
    * @return
    */
    @ApiOperation(value = "??????????????????", notes = "??????????????????", tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 201, message = "Permission added")})
    @PostMapping("/{namespace}/{role_name}/permissions")
    @ResponseStatus(HttpStatus.CREATED)
    RolePermission addPermission(@PathVariable("namespace") String namespace,
                              @PathVariable("role_name") String roleName,
                              @RequestBody RolePermission requestPermission);
    /**
    * Remove a permission from a role.<br/>
    *
    * ????????? pengguihua
    * @param namespace
    * @param roleName
    * @param permissionUuid
    * @param requestPermission
    */
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????", tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "Permission removed successfully"),
                           @ApiResponse(code = 404, message = "Permission not found.")})
    @DeleteMapping("/{namespace}/{role_name}/permissions/{permission_uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    BaseResponse removePermissionFromRole(@PathVariable("namespace") String namespace,
                                         @PathVariable("role_name") String roleName,
                                         @PathVariable("permission_uuid") String permissionUuid);
    /**
    * List users that belong to a role.<br/>
    *
    * ????????? pengguihua
    * @param namespace
    * @param roleName
    * @return
    */
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????",
            response = RoleUserPayload.class, tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "", response = RoleUserPayload.class)})
    @GetMapping(path = "/{namespace}/{role_name}/users", produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.OK)
    List<RoleUserPayload> listRoleUsers(@PathVariable("namespace") String namespace,
                                               @PathVariable("role_name") String roleName);
    /**
    * ?????????????????????.<br/>
    *
    * ????????? pengguihua
    * @param namespace
    * @param roleName
    * @param roleUserList
    * @return
    */
    @ApiOperation(value = "?????????????????????", notes = "?????????????????????", response = RoleUserPayload.class,
            tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Assigned successfully",
            response = RoleUserPayload.class),
    @ApiResponse(code = 400, message = "Validation failed because one of the users already have role",
            response = ErrorsResponse.class)})
    @PostMapping(path = "/{namespace}/{role_name}/users", produces = "application/json; charset=UTF-8")
    @ResponseStatus(HttpStatus.OK)
    List<RoleUserPayload> assignRoleUsers(@PathVariable("namespace") String namespace,
                                              @PathVariable("role_name") String roleName,
                                              @RequestBody List<RoleUserPayload> roleUserList);
    /**
    * Revoke a role from one or more users.<br/>
    * @param namespace
    * @param roleName
    * @param roleUserList
    * ????????? pengguihua
    */
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????", tags = {"Composite Role service API"})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "All revoked successfully"),
                           @ApiResponse(code = 404,
                           message = "Validation failed because one of the users dont have the role",
                           response = ErrorsResponse.class)})
    @DeleteMapping(path = "/{namespace}/{role_name}/users", produces = "application/json;charset=UTF-8")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    BaseResponse revokeRoleUsers(@PathVariable("namespace") String namespace,
                                @PathVariable("role_name") String roleName,
                                @RequestBody List<RoleUserPayload> roleUserList);
    /**
     * update a permission from a role.<br/>
     * ????????? yangshilei
     * @param namespace
     * @param roleName
     * @param permissionUuid
     * @param requestPermission
     */
     @ApiOperation(value = "????????????????????????", notes = "????????????????????????", tags = {"Composite Role service API"})
     @ApiResponses(value = {@ApiResponse(code = 204, message = "Permission updated successfully"),
                            @ApiResponse(code = 404, message = "Permission not found.")})
     @PutMapping("/{namespace}/{role_name}/permissions/{permission_uuid}")
     @ResponseStatus(HttpStatus.NO_CONTENT)
     BaseResponse updatePermissionFromRole(@PathVariable("namespace") String namespace,
                                          @PathVariable("role_name") String roleName,
                                          @PathVariable("permission_uuid") String permissionUuid,
                                          @RequestBody RolePermission requestPermission);


}
