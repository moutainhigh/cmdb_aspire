package com.aspire.mirror.ops.api.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.aspire.mirror.ops.api.domain.AgentHostQueryModel;
import com.aspire.mirror.ops.api.domain.GeneralResponse;
import com.aspire.mirror.ops.api.domain.GroupScenes;
import com.aspire.mirror.ops.api.domain.NormalAgentHostInfo;
import com.aspire.mirror.ops.api.domain.OpsAccount;
import com.aspire.mirror.ops.api.domain.OpsAccountQueryModel;
import com.aspire.mirror.ops.api.domain.OpsAgentStepInstanceResult;
import com.aspire.mirror.ops.api.domain.OpsAgentStepInstanceResult.OpsAgentStepInstanceResultQueryModel;
import com.aspire.mirror.ops.api.domain.OpsFileTransferActionModel;
import com.aspire.mirror.ops.api.domain.OpsIndexValueCollectRequest;
import com.aspire.mirror.ops.api.domain.OpsLabel;
import com.aspire.mirror.ops.api.domain.OpsLabel.OpsLabelQueryModel;
import com.aspire.mirror.ops.api.domain.OpsParam;
import com.aspire.mirror.ops.api.domain.OpsParamQueryModel;
import com.aspire.mirror.ops.api.domain.OpsParamReference;
import com.aspire.mirror.ops.api.domain.OpsParamType;
import com.aspire.mirror.ops.api.domain.OpsParamValue;
import com.aspire.mirror.ops.api.domain.OpsParamValueDetail;
import com.aspire.mirror.ops.api.domain.OpsPipelineDTO;
import com.aspire.mirror.ops.api.domain.OpsPipelineDTO.OpsPipelineQueryModel;
import com.aspire.mirror.ops.api.domain.OpsPipelineHis;
import com.aspire.mirror.ops.api.domain.OpsPipelineInstanceDTO;
import com.aspire.mirror.ops.api.domain.OpsPipelineInstanceLog;
import com.aspire.mirror.ops.api.domain.OpsPipelineInstanceQueryParam;
import com.aspire.mirror.ops.api.domain.OpsPipelineRunJob;
import com.aspire.mirror.ops.api.domain.OpsPipelineRunJob.OpsPipelineRunJobQueryModel;
import com.aspire.mirror.ops.api.domain.OpsPipelineScenes;
import com.aspire.mirror.ops.api.domain.OpsScript;
import com.aspire.mirror.ops.api.domain.OpsScriptExecuteActionModel;
import com.aspire.mirror.ops.api.domain.OpsScriptHis;
import com.aspire.mirror.ops.api.domain.OpsScriptQueryModel;
import com.aspire.mirror.ops.api.domain.OpsStepDTO;
import com.aspire.mirror.ops.api.domain.OpsStepHis;
import com.aspire.mirror.ops.api.domain.OpsStepInstanceDTO;
import com.aspire.mirror.ops.api.domain.PageListQueryResult;
import com.aspire.mirror.ops.api.domain.SimpleAgentHostInfo;
import com.aspire.mirror.ops.api.domain.SimpleAgentHostInfo.SimpleAgentHostQueryModel;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/** 
 *
 * ????????????: ops-api 
 * <p/>
 * 
 * ??????: IOpsManageService
 * <p/>
 *
 * ???????????????: Ops??????????????????
 * <p/>
 *
 * @author	pengguihua
 *
 * @date	2019???10???30???  
 *
 * @version	V1.0 
 * <br/>
 *
 * <b>Copyright(c)</b> 2019 ????????????-???????????? 
 *
 */
@Api(value = "ops????????????")
@RequestMapping(value = "/v1/ops-service/opsManage/")
public interface IOpsManageService {

    @PostMapping(value = "/mockKafkaMessageSend/{topicName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????kafka????????????", notes = "??????kafka????????????", tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 204, message = "??????"), @ApiResponse(code = 500, message = "Unexpected error")})
    void mockKafkaMessageSend(@PathVariable("topicName") String topicName, @RequestBody Map<String, Object> messageObj);
    
    @GetMapping(value = "/getAgentHostInfoLoadSource", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????agent?????????????????????", notes = "??????agent?????????????????????",
            response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    public GeneralResponse getAgentHostInfoLoadSource();

    @PostMapping(value = "/fetchUserAuthedAgentHostList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????????????????agent????????????", notes = "??????????????????????????????agent????????????",
            response = SimpleAgentHostInfo.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = SimpleAgentHostInfo.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    PageListQueryResult<SimpleAgentHostInfo> fetchUserAuthedAgentHostList(@RequestBody SimpleAgentHostQueryModel queryParam);

    @GetMapping(value = "/queryAgentInfoByProxyIdConcatIP/{proxyIdConcatIP:.+}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????proxyid:agentIP??????????????????", notes = "??????proxyid:agentIP??????????????????",
            response = SimpleAgentHostInfo.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = SimpleAgentHostInfo.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    SimpleAgentHostInfo queryAgentInfoByProxyIdConcatIP(@PathVariable("proxyIdConcatIP") String proxyIdConcatIP);
    
    @GetMapping(value = "/loadOpsStatusList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????",
            response = Map.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = Map.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<Map<Integer, String>> loadOpsStatusList();

    @GetMapping(value = "/loadOpsTriggerWayList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????ops????????????????????????", notes = "??????ops????????????????????????",
            response = Map.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = Map.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<Map<Integer, String>> loadOpsTriggerWayList();

    @PostMapping(value = "/saveOpsAccount", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse saveOpsAccount(@RequestBody OpsAccount account);

    @DeleteMapping(value = "/removeOpsAccount/{accountName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse removeOpsAccount(@PathVariable("accountName") String accountName);

    @PostMapping(value = "/queryOpsAccountList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = OpsAccount.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsAccount.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<OpsAccount> queryOpsAccountList(@RequestBody OpsAccountQueryModel queryParam);

    @PostMapping(value = "/saveOpsLabel", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse saveOpsLabel(@RequestBody OpsLabel label);

    @DeleteMapping(value = "/removeLabel/{labelCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse removeOpsLabel(@PathVariable("labelCode") String labelCode);

    @PostMapping(value = "/queryOpsLabelList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????", response = OpsLabel.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsLabel.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<OpsLabel> queryOpsLabelList(@RequestBody OpsLabelQueryModel queryParam);

    @PostMapping(value = "/saveScript", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse saveOpsScript(@RequestBody OpsScript script);

    @DeleteMapping(value = "/removeScript/{scriptId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse removeOpsScript(@PathVariable("scriptId") Long scriptId);

    @PostMapping(value = "/queryOpsScriptList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = OpsScript.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsScript.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    PageListQueryResult<OpsScript> queryOpsScriptList(@RequestBody OpsScriptQueryModel queryParam);

    @GetMapping(value = "/queryOpsScriptById", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "?????????id????????????", notes = "?????????id????????????", response = OpsScript.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsScript.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    OpsScript queryOpsScriptById(@RequestParam("scriptId") Long scriptId);

    @PostMapping(value = "/saveOpsPipeline", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse saveOpsPipeline(@RequestBody OpsPipelineDTO pipeline);

    @DeleteMapping(value = "/removePipeline/{pipelineId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse removeOpsPipeline(@PathVariable("pipelineId") Long pipelineId);

    @PostMapping(value = "/queryOpsPipelineList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = OpsPipelineDTO.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsPipelineDTO.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    PageListQueryResult<OpsPipelineDTO> queryOpsPipelineList(@RequestBody OpsPipelineQueryModel queryParam);

    @GetMapping(value = "/queryOpsPipelineById/{pipelineId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????id????????????", notes = "??????id????????????", response = OpsPipelineDTO.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsPipelineDTO.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    OpsPipelineDTO queryOpsPipelineById(@PathVariable("pipelineId") Long pipelineId);

    @GetMapping(value = "/queryOpsStepListByPipelineId/{pipelineId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????", response = OpsStepDTO.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsStepDTO.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<OpsStepDTO> queryOpsStepListByPipelineId(@PathVariable("pipelineId") Long pipelineId);

    @PostMapping(value = "/queryOpsPipelineInstanceList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = OpsPipelineInstanceDTO.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsPipelineInstanceDTO.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    PageListQueryResult<OpsPipelineInstanceDTO> queryOpsPipelineInstanceList(@RequestBody OpsPipelineInstanceQueryParam queryParam);

    @GetMapping(value = "/queryOpsPipelineInstanceById/{pipelineInstanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????id??????????????????????????????", notes = "??????id??????????????????????????????", response = OpsPipelineInstanceDTO.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsPipelineDTO.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    OpsPipelineInstanceDTO queryOpsPipelineInstanceById(@PathVariable("pipelineInstanceId") Long pipelineInstanceId);

    @PostMapping(value = "/queryStepInstListByPipelineInstId/{pipelineInstanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????", response = OpsStepInstanceDTO.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsStepInstanceDTO.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<OpsStepInstanceDTO> queryStepInstListByPipelineInstId(@PathVariable("pipelineInstanceId") Long pipelineInstanceId);

    @GetMapping(value = "/queryStepInstanceById", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????id????????????????????????", notes = "??????id????????????????????????", response = OpsStepInstanceDTO.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsStepInstanceDTO.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    OpsStepInstanceDTO queryStepInstanceById(@RequestParam("stepInstId") Long stepInstId);

    @PostMapping(value = "/queryOpsStepInstanceAgentRunResultList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????????????????????????????", notes = "????????????????????????????????????", response = OpsAgentStepInstanceResult.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsAgentStepInstanceResult.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    PageListQueryResult<OpsAgentStepInstanceResult> queryOpsStepAgentRunResultList(@RequestBody OpsAgentStepInstanceResultQueryModel param);

    @PutMapping(value = "/realtimeScriptExecute", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse executeRealtimeScript(@RequestBody OpsScriptExecuteActionModel requestData);

    @PutMapping(value = "/realtimeFileTransfer", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse executeRealtimeFileTransfer(@RequestBody OpsFileTransferActionModel requestData);

    @PostMapping(value = "/uploadFile4Transfer", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse uploadFile4Transfer(@RequestParam("file") MultipartFile file);

    @PutMapping(value = "/pipelineExecute/{pipelineId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse executePipeline(@PathVariable("pipelineId") Long pipelineId,
                                    @RequestBody(required = false) Map<String, Object> params);
    
    @PutMapping(value = "/manualHandleOpsStepInstance/{stepInstanceId}/{status}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????????????????", notes = "??????????????????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse manualHandleOpsStepInstance(@PathVariable("stepInstanceId") Long stepInstanceId, 
    			@PathVariable("status") Integer status);

    @PutMapping(value = "/executeIndexValueScriptCollect", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse executeIndexValueScriptCollect(@RequestBody OpsIndexValueCollectRequest indexCollectData);

    @PostMapping(value = "/saveOpsPipelineRunJob", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse saveOpsPipelineRunJob(@RequestBody OpsPipelineRunJob runJob);

    @DeleteMapping(value = "/removeOpsPipelineRunJob/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse removeOpsPipelineRunJob(@PathVariable("jobId") Long jobId);

    @PostMapping(value = "/queryOpsPipelineRunJobList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    PageListQueryResult<OpsPipelineRunJob> queryOpsPipelineRunJobList(@RequestBody OpsPipelineRunJobQueryModel queryParam);

    @PutMapping(value = "/schedulePipelineCronJob/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse schedulePipelineCronJob(@PathVariable("jobId") Long jobId);

    @PutMapping(value = "/unSchedulePipelineCronJob/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse unSchedulePipelineCronJob(@PathVariable("jobId") Long jobId);

    @PutMapping(value = "/executePipelineCronJob/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????????????????", notes = "??????????????????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse executePipelineCronJob(@PathVariable("jobId") Long jobId);

    @PutMapping(value = "/reviewSensitiveApply/{pipelineInstanceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse reviewSensitiveApply(@PathVariable("pipelineInstanceId") Long pipelineInstanceId);

    @PostMapping(value = "/savePipelineScenes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse savePipelineScences(@RequestBody OpsPipelineScenes scenes);

    @GetMapping(value = "/pipelineScenesAllList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = GroupScenes.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GroupScenes.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<GroupScenes> pipelineScenesAllList();

    @GetMapping(value = "/pipelineScenesById", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = OpsPipelineScenes.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsPipelineScenes.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    OpsPipelineScenes pipelineScenesById(@RequestParam("pipeline_scenes_id") Long pipelineScenesId);

    @DeleteMapping(value = "/deletePipelineScenes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse deletePipelineScenes(@RequestParam("scenes_ids") String scenesIds);

    @GetMapping(value = "/logPackageApply", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse logPackageApply(@RequestParam("pipelineInstanceId") Long pipelineInstanceId);

    @GetMapping(value = "/getPipelineInstanceLog", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = OpsPipelineInstanceLog.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsPipelineInstanceLog.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    OpsPipelineInstanceLog getPipelineInstanceLog(@RequestParam("pipelineInstanceId") Long pipelineInstanceId);

    @GetMapping(value = "/outputPackage", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "?????????????????????", notes = "?????????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse outputPackage(@RequestParam("pipelineInstanceId") Long pipelineInstanceId);

    @GetMapping(value = "/getParamAllList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsParam.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<OpsParam> getParamAllList();
    
    @PostMapping(value = "/searchParamList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????????????????", notes = "??????????????????????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsParam.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    PageListQueryResult<OpsParam> searchParamList(@RequestBody OpsParamQueryModel paramModel);
    
    @PostMapping(value = "/createOpsParam", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse createOpsParam(@RequestBody OpsParam createModel);
    
    @PutMapping(value = "/updateOpsParam", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse updateOpsParam(@RequestBody OpsParam updateModel);
    
    @DeleteMapping(value = "/deleteOpsParamById/{paramId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????id????????????", notes = "??????id????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse deleteOpsParamById(@PathVariable("paramId") Long paramId);
    
    @GetMapping(value = "/loadAllParamTypeList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsParamType.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    public List<OpsParamType> loadAllParamTypeList();
    
    @GetMapping(value = "/queryReferParamListByEntityId/{entityId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????id????????????????????????", notes = "????????????id????????????????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsParamReference.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    public List<OpsParamReference> queryReferParamListByEntityId(@PathVariable("entityId") Long entityId);

    @PutMapping(value = "/auditPipeline", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsParam.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse auditPipeline(@RequestParam("pipelineId") Long pipelineId, @RequestParam("auditStatus") String auditStatus, @RequestParam(name = "auditDesc", required = false) String auditDesc);

    @PutMapping(value = "/auditScript", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????", notes = "????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsParam.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse auditScript(@RequestParam("scriptId") Long scriptId, @RequestParam("auditStatus") String auditStatus, @RequestParam(name = "auditDesc") String auditDesc);

    @PutMapping(value = "/continueExecInstance", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsParam.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse continueExecInstance(@RequestParam("pipelineInstanceId") Long pipelineInstanceId);

    @GetMapping(value = "/getPipelineHisListByPipelineId", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????id????????????????????????", notes = "????????????id????????????????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsParam.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<OpsPipelineHis> getPipelineHisListByPipelineId(@RequestParam("pipelineId") Long pipelineId);

    @GetMapping(value = "/getScriptHisListByScriptId", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????id????????????????????????", notes = "????????????id????????????????????????", response = OpsParam.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsParam.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<OpsScriptHis> getScriptHisListByScriptId(@RequestParam("scriptId") Long scriptId);

    @GetMapping(value = "/queryOpsStepHisListByPipelineHisId", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????????????????", notes = "??????????????????????????????", response = OpsStepHis.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsStepHis.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<OpsStepHis> queryOpsStepHisListByPipelineHisId(@RequestParam("pipelineHisId") Long pipelineHisId);

    @GetMapping(value = "/queryOpsPipelineHisById", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????", response = OpsPipelineHis.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsPipelineHis.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    OpsPipelineHis queryOpsPipelineHisById(@RequestParam("pipelineHisId") Long pipelineHisId);

    @GetMapping(value = "/queryOpsScriptHisById", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????id????????????????????????", notes = "??????id????????????????????????", response = OpsScriptHis.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsScriptHis.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    OpsScriptHis queryOpsScriptHisById(@RequestParam("scriptHisId") Long scriptHisId);

    @PutMapping(value = "/updatePipelineVersion", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse updatePipelineVersion(@RequestParam("pipelineHisId") Long pipelineHisId);

    @PutMapping(value = "/updateScriptVersion", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????", notes = "??????????????????", response = GeneralResponse.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = GeneralResponse.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    GeneralResponse updateScriptVersion(@RequestParam("scriptHisId") Long scriptHisId);

    @PostMapping(value = "/getNormalAgentHostList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "?????????????????????agent????????????", notes = "?????????????????????agent????????????",
            response = NormalAgentHostInfo.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = NormalAgentHostInfo.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    PageListQueryResult<NormalAgentHostInfo> getNormalAgentHostList(@RequestBody AgentHostQueryModel queryParam);

    @GetMapping(value = "/getUsernameListByAgentIp", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????agentIp????????????????????????????????????", notes = "??????agentIp????????????????????????????????????",
            response = String.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = String.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<String> getUsernameListByAgentIp(@RequestParam("agentIp") String agentIp);

    @PostMapping(value = "/queryParamValueList", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "?????????????????????????????????", notes = "?????????????????????????????????",
            response = NormalAgentHostInfo.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = NormalAgentHostInfo.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<OpsParamValueDetail> queryParamValueList(@RequestBody OpsParamValue paramValue);

    @GetMapping(value = "/queryNormalAgentHostByStepId/{step_id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "????????????????????????", notes = "????????????????????????",
            response = NormalAgentHostInfo.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = OpsStepDTO.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    List<NormalAgentHostInfo> queryNormalAgentHostByStepId(@PathVariable("step_id") Long stepId);

    @GetMapping(value = "/querySimpleHostByPoolAndHostIp", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "??????????????????IP????????????", notes = "??????????????????IP????????????",
            response = NormalAgentHostInfo.class, tags = {"Ops Manage service API"})
    @ApiResponses(value = {@ApiResponse(code = 200, message = "??????", response = SimpleAgentHostInfo.class),
            @ApiResponse(code = 500, message = "Unexpected error")})
    SimpleAgentHostInfo querySimpleHostByPoolAndHostIp(@RequestParam("pool") String pool, @RequestParam("ip") String ip);
}
