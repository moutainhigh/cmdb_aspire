package com.migu.tsg.microservice.atomicservice.composite.service.resmgr;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.migu.tsg.microservice.atomicservice.composite.service.common.payload.BaseResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.CompLbCreateRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.CompLbCreateResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.CompServiceCreateRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.CompServiceCreateResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.CompServiceLbhealthinforResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.CompServiceStartResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.CompServiceStopResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.CompServiceUpdateRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.CompServiceUpdateResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.LbListResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.ListResServicesResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.Logs;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.PutServiceApplabel;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.RetrieveResServiceDetaileResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.ServiceApplicationLabelResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.ServiceLogAggregation;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.ServiceLogResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.resmgr.payload.UpdateLbResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * composite??????????????????????????? Project Name:composite-api File
 * Name:ICompResServicesService.java Package
 * Name:com.migu.tsg.microservice.atomicservice.composite.service.resMgr
 * ClassName: ICompResServicesService <br/>
 * date: 2017???9???14??? ??????11:02:30 <br/>
 * 
 * @author pengguihua
 * @version
 * @since JDK 1.6
 */
@RequestMapping("${version}/services")
@Api(value = "Composite Resource management(Services)", description = "Composite Resource management(Services)")
public interface ICompResServicesService {

	/**
	 * ??????????????????.<br/>
	 *
	 * ????????? pengguihua
	 * 
	 * @param namespace
	 * @param regionName
	 * @param basicFlag
	 * @param detailFlag
	 * @param projectName
	 * @return
	 */
	@ApiOperation(value = "??????????????????", notes = "??????????????????", response = ListResServicesResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "List service resources for the namespace in the region", response = ListResServicesResponse.class) })
	@GetMapping(path = "/{namespace}", produces = "application/json;charset=UTF-8")
	ListResServicesResponse listResServices(@PathVariable("namespace") String namespace,
			@RequestParam("region_name") String regionName,
			@RequestParam(name = "basic", required = false) boolean basicFlag,
			@RequestParam(name = "detail", required = false) boolean detailFlag,
			@RequestParam(name = "project_name", required = false) String projectName,
			@RequestParam(name = "name", required = false) String name,
			@RequestParam(name = "label", required = false) String label,
			@RequestParam(name = "instance_ip", required = false) String instanceIp,
			@RequestParam(name = "labels", required = false) String labels);

	/**
	 * ????????????.<br/>
	 *
	 * ????????? pengguihua
	 * 
	 * @param namespace
	 * @param projectName
	 * @param compReq
	 * @return
	 */
	@ApiOperation(value = "????????????", notes = "????????????", response = CompServiceCreateResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Create a new service.", response = CompServiceCreateResponse.class) })
	@PostMapping(path = "/{namespace}", produces = "application/json;charset=UTF-8")
	CompServiceCreateResponse createResService(@PathVariable("namespace") String namespace,
			@RequestParam(name = "project_name", required = false) String projectName,
			@RequestBody CompServiceCreateRequest compReq);

	/**
	 * ??????????????????.<br/>
	 *
	 * ????????? pengguihua
	 * 
	 * @param namespace
	 * @param serviceName
	 * @param application
	 */
	@ApiOperation(value = "??????????????????", notes = "??????????????????", response = RetrieveResServiceDetaileResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Retrieve the detail of service.", response = RetrieveResServiceDetaileResponse.class) })
	@GetMapping(path = "/{namespace}/{service_name}", produces = "application/json;charset=UTF-8")
	RetrieveResServiceDetaileResponse retrieveServiceDetail(@PathVariable("namespace") String namespace,
			@PathVariable("service_name") String serviceName,
			@RequestParam(name = "application", required = false) String application);

	/**
	 * ????????????.<br/>
	 *
	 * ????????? pengguihua
	 * 
	 * @param namespace
	 * @param serviceName
	 * @param application
	 * @param request
	 * @return
	 */
	@ApiOperation(value = "????????????", notes = "????????????", response = CompServiceUpdateResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Update the service.", response = CompServiceUpdateResponse.class) })
	@PutMapping(path = "/{namespace}/{service_name}", produces = "application/json;charset=UTF-8")
	CompServiceUpdateResponse updateResService(@PathVariable("namespace") String namespace,
			@PathVariable("service_name") String serviceName,
			@RequestParam(name = "application", required = false) String application,
			@RequestBody CompServiceUpdateRequest request);

	/**
	 * ????????????.<br/>
	 *
	 * ????????? pengguihua
	 * 
	 * @param namespace
	 * @param serviceName
	 */
	@ApiOperation(value = "????????????", notes = "????????????", response = CompServiceUpdateResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = { @ApiResponse(code = 204, message = "Delete a service", response = BaseResponse.class) })
	@DeleteMapping(path = "/{namespace}/{service_name}")
	BaseResponse removeResService(@PathVariable("namespace") String namespace,
			@PathVariable("service_name") String serviceName,
			@RequestParam(name = "application", required = false) String appName);

	@ApiOperation(value = "????????????", notes = "????????????", response = CompServiceStartResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Start the service.", response = CompServiceStartResponse.class) })
	@PutMapping(path = "/{namespace}/{service_name}/start", produces = "application/json;charset=UTF-8")
	CompServiceStartResponse startResService(@PathVariable("namespace") String namespace,
			@PathVariable("service_name") String serviceName,
			@RequestParam(name = "application", required = false) String appName);

	@ApiOperation(value = "????????????", notes = "????????????", response = CompServiceStopResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Stop the service.", response = CompServiceStopResponse.class) })
	@PutMapping(path = "/{namespace}/{service_name}/stop", produces = "application/json;charset=UTF-8")
	CompServiceStopResponse stopResService(@PathVariable("namespace") String namespace,
			@PathVariable("service_name") String serviceName,
			@RequestParam(name = "application", required = false) String appName);

	@ApiOperation(value = "????????????", notes = "????????????", response = CompServiceStopResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "retry update service in UpdateError status.", response = CompServiceStopResponse.class) })
	@PutMapping(path = "/{namespace}/{service_name}/retryupdate", produces = "application/json;charset=UTF-8")
	CompServiceUpdateResponse retryUpdateResService(@PathVariable("namespace") String namespace,
			@PathVariable("service_name") String serviceName,
			@RequestParam(name = "application", required = false) String appName);

	@ApiOperation(value = "????????????", notes = "????????????", response = CompServiceStopResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "rollback service in UpdateError status.", response = CompServiceStopResponse.class) })
	@PutMapping(path = "/{namespace}/{service_name}/rollback", produces = "application/json;charset=UTF-8")
	CompServiceUpdateResponse rollbackResService(@PathVariable("namespace") String namespace,
			@PathVariable("service_name") String serviceName,
			@RequestParam(name = "application", required = false) String appName);

	/**
	 * ???????????????lb????????????.<br/>
	 * ????????? weishuai
	 * 
	 * @param namespace
	 * @param serviceName
	 */
	@ApiOperation(value = "???????????????lb????????????", notes = "???????????????lb????????????", response = CompServiceLbhealthinforResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Get service's lb health info.", response = CompServiceLbhealthinforResponse.class) })
	@GetMapping(path = "/{namespace}/{service_name}/lb-health-info", produces = "application/json;charset=UTF-8")
	List<CompServiceLbhealthinforResponse> getServiceLbHeaithInfo(@PathVariable("service_name") String serviceName);

	/**
	 * ????????????.<br/>
	 * ????????? weishuai
	 * 
	 * @param namespace
	 * @param serviceName
	 * @param uuid
	 */
	@ApiOperation(value = "????????????", notes = "????????????", tags = { "Composite Resource management(Services) service API" })
	@ApiResponses(value = { @ApiResponse(code = 204, message = "Delete a instance by id.") })
	@DeleteMapping(path = "/{namespace}/{service_name}/instances/{uuid}/")
	BaseResponse deleteServiceInstanceByUuid(@PathVariable("service_name") String serviceName,
			@PathVariable("uuid") String uuid);

	/**
	 * ????????????????????????.<br/>
	 * ????????? weishuai
	 * 
	 * @param namespace6
	 * @param serviceName
	 */
	@ApiOperation(value = "????????????????????????", notes = "????????????????????????", response = ServiceApplicationLabelResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Get an application's labels.", response = ServiceApplicationLabelResponse.class) })
	@GetMapping(path = "/{namespace}/{service_name}/labels", produces = "application/json;charset=UTF-8")
	List<ServiceApplicationLabelResponse> getServiceAppliacationLabels(
			@PathVariable("service_name") String serviceName);

	/**
	 * ????????????????????????.<br/>
	 * ????????? weishuai
	 * 
	 * @param namespace6
	 * @param serviceName
	 */
	@ApiOperation(value = "????????????????????????", notes = "????????????????????????", response = PutServiceApplabel.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Update/add an application's labels.", response = PutServiceApplabel.class) })
	@PutMapping(path = "/{namespace}/{service_name}/labels", produces = "application/json;charset=UTF-8")
	List<PutServiceApplabel> putServiceAppliacationLabels(@PathVariable("service_name") String serviceName,
			@RequestBody List<PutServiceApplabel> appbale);

	/**
	 * ??????????????????.<br/>
	 * ????????? weishuai
	 * 
	 * @param namespace
	 * @param serviceName
	 */
	@ApiOperation(value = "??????????????????", notes = "??????????????????", response = Logs.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Get service's logs.", response = ServiceLogResponse.class) })
	@GetMapping(path = "/{namespace}/{service_name}/logs", produces = "application/json;charset=UTF-8")
	ServiceLogResponse getServiceLogs(@PathVariable("service_name") String serviceName,
			@RequestParam(name = "pageno", required = false, defaultValue = "1") int pageno,
			@RequestParam(name = "start_time") String start_time, @RequestParam(name = "end_time") String end_time,
			@RequestParam(name = "log_source", required = false) String log_source,
			@RequestParam(name = "application", required = false) String application,
			@RequestParam(name = "query_string", required = false) String query_string,
			@RequestParam(name = "size", required = false) int size,
			@RequestParam(name = "tail", required = false) String tail,
			@RequestParam(value = "region_id", required = false) String regionId);

	/**
	 * ????????????????????????.<br/>
	 * ????????? weishuai
	 * 
	 * @param namespace
	 * @param serviceName
	 */
	@ApiOperation(value = "????????????????????????", notes = "????????????????????????", response = ServiceLogAggregation.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Get service's log aggregation.", response = ServiceLogAggregation.class) })
	@GetMapping(path = "/{namespace}/{service_name}/logs/aggregations", produces = "application/json;charset=UTF-8")
	ServiceLogAggregation getServiceLogAggregation(@PathVariable("service_name") String serviceName,
			@RequestParam(name = "start_time") String start_time, @RequestParam(name = "end_time") String end_time,
			@RequestParam(name = "log_source", required = false) String log_source,
			@RequestParam(name = "application", required = false) String application,
			@RequestParam(name = "query_string", required = false) String query_string,
			@RequestParam(name = "size", required = false, defaultValue = "500") int size,
			@RequestParam(value = "region_id", required = false) String regionId);

	/**
	 * ??????????????????.<br/>
	 * ????????? weishuai
	 * 
	 * @param namespace
	 * @param serviceName
	 */
	@ApiOperation(value = "??????????????????", notes = "??????????????????", tags = { "Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "get log source, support both alauda_stdout, alauda_stderr, log_file_name.") })
	@GetMapping(path = "/{namespace}/{service_name}/logs/sources")
	List<String> getServiceLogSource(@PathVariable("service_name") String serviceName,
			@RequestParam(name = "application", required = false) String appName,
			@RequestParam(value = "region_id", required = false) String regionId);

	/**
	 * ??????????????????.<br/>
	 * ????????? weishuai
	 * 
	 * @param namespace
	 * @param serviceName
	 */
	@ApiOperation(value = "??????????????????", notes = "??????????????????", tags = { "Composite Resource management(Services) service API" })
	@ApiResponses(value = { @ApiResponse(code = 200, message = "get service log context.") })
	@GetMapping(path = "/{namespace}/{service_name}/logs/context", produces = { "application/json" })
	String getServiceLogContext(@PathVariable("service_name") String serviceName,
			@RequestParam(name = "instance_id") String instanceId, @RequestParam(name = "log_time") String log_time,
			@RequestParam(name = "application", required = false) String application,
			@RequestParam(name = "log_source", required = false) String log_source,
			@RequestParam(name = "direction", required = false) String direction,
			@RequestParam(value = "region_id", required = false) String regionId);

	/**
	 * ???????????????.<br/>
	 * ????????? weishuai
	 * 
	 * @param namespace
	 * @param serviceName
	 */
	@ApiOperation(value = "???????????????", notes = "???????????????", tags = { "Composite Resource management(Services) service API" })
	@ApiResponses(value = { @ApiResponse(code = 400, message = "It's not a validate name.") })
	@GetMapping(path = "/{namespace}/{service_name}/check")
	void checkServiceName(@PathVariable("service_name") String service_name,
			@RequestParam(name = "knamespace", required = false) String knamespace);

	/**
	 * ?????????????????????.
	 *
	 * ????????? qianchunhui
	 * 
	 * @param namespace
	 * @param projectName
	 * @param compReq
	 * @return
	 */
	@ApiOperation(value = "?????????????????????", notes = "?????????????????????", response = CompLbCreateResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Create a new service.", response = CompLbCreateResponse.class) })
	@PostMapping(path = "/{namespace}/lb", produces = "application/json;charset=UTF-8")
	CompLbCreateResponse createLoadBalancers(@PathVariable("namespace") String namespace,
			@RequestParam(name = "project_name", required = false) String projectName,
			@RequestBody CompLbCreateRequest compReq);

	/**
	 * ???????????????????????????.
	 *
	 * ????????? qianchunhui
	 * 
	 * @param namespace
	 * @param projectName
	 * @return
	 */
	@ApiOperation(value = "???????????????????????????", notes = "???????????????????????????", response = String.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "List service resources for the namespace in the region", response = String.class) })
	@GetMapping(path = "/{namespace}/lb", produces = "application/json;charset=UTF-8")
	List<LbListResponse> listBalancers(@PathVariable("namespace") String namespace,
			@RequestParam("region_name") String regionName,
			@RequestParam(name = "project_name", required = false) String projectName);

	/**
	 * ???????????????????????????. ????????? qianchunhui
	 * 
	 * @param namespace
	 * @param name
	 * @return
	 */
	@ApiOperation(value = "???????????????????????????", notes = "???????????????????????????", response = LbListResponse.class, tags = {
			"Composite Resource management(Services)service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Get Load balancers details", response = LbListResponse.class)})
	@GetMapping(path = "/{namespace}/{name}/lb", produces = "application/json;charset=UTF-8")
	public LbListResponse getLoadBalancersDetail(@PathVariable("namespace") String namespace,
			@PathVariable("name") String name);
	
	
	/**
     * ?????????????????????.
     * ????????? qianchunhui
     * @param namespace
     * @param name
     * @return
     */
    @ApiOperation(value = "?????????????????????", notes = "?????????????????????", 
                  tags = {"Composite Resource management(Services) service API"})
    @ApiResponses(value = {@ApiResponse(code=200, message = "Delete one LoadBalancers")})
    @DeleteMapping(path = "/{namespace}/{name}/lb")
    public BaseResponse deleteLoadBalancers(@PathVariable("namespace") String namespace,
            @PathVariable("name") String name);
    
    /**
	 * ?????????????????????.
	 *
	 * ????????? qianchunhui
	 * 
	 * @param namespace
	 * @param Name
	 * @param request
	 * @return
	 */
	@ApiOperation(value = "?????????????????????", notes = "?????????????????????", response = UpdateLbResponse.class, tags = {
			"Composite Resource management(Services) service API" })
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Update the LoadBalancers.", response = UpdateLbResponse.class) })
	@PutMapping(path = "/{namespace}/{name}/lb", produces = "application/json;charset=UTF-8")
	UpdateLbResponse updateLoadBalancers(@PathVariable("namespace") String namespace,
			@PathVariable("name") String name,
			@RequestBody UpdateLbResponse request);
}
