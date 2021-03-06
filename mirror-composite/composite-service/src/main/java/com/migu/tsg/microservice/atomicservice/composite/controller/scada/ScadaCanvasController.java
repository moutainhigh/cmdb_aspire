package com.migu.tsg.microservice.atomicservice.composite.controller.scada;

import com.alibaba.fastjson.JSON;
import com.aspire.mirror.alert.api.dto.alert.AlertValueSearchRequest;
import com.aspire.mirror.common.entity.PageResponse;
import com.aspire.mirror.composite.service.scada.IScadaCanvasService;
import com.aspire.mirror.composite.service.scada.payload.*;
import com.aspire.mirror.elasticsearch.api.dto.HistorySearchRequest;
import com.aspire.mirror.elasticsearch.api.dto.LldpInfo;
import com.aspire.mirror.scada.api.dto.PathRequest;
import com.aspire.mirror.scada.api.dto.ScadaCanvasPageRequest;
import com.aspire.mirror.scada.api.dto.ScadaCanvasReq;
import com.aspire.mirror.scada.api.dto.ScadaCanvasRes;
import com.aspire.mirror.scada.api.dto.model.ScadaCanvasDTO;
import com.aspire.ums.cmdb.instance.payload.CmdbQueryInstance;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.alert.AlertsServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.cmdb.instance.InstanceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.monitor.HistoryServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.es.LldpInfoServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.scada.ScadaCanvasServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.controller.LLDPPhysicalTopology;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.ResAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.migu.tsg.microservice.atomicservice.composite.controller.util.PayloadParseUtil.jacksonBaseParse;

/**
 * ????????????????????????????????????
 * <p>
 * ????????????:  mirror??????
 * ???:        com.aspire.mirror.composite.service.scada
 * ?????????:    ICanvasService.java
 * ?????????:    ??????????????????????????????
 * ????????????:  2019/6/11 14:40
 * ??????:      v1.0
 *
 * @author JinSu
 */

@Slf4j
@RestController
public class ScadaCanvasController implements IScadaCanvasService {
    /**
     * FTP???????????????
     */
//    private static String port = "21";
//    /**
//     * ??????????????????
//     */
//    private static String size = "300000";
//
//    private static String host103 = "10.1.5.103";
//    /**
//     * nginx????????????
//     */
//    private static String nginxPort103 = "8398";
//    private static String nginxPort62 = "8398";
//    /**
//     * FTP????????????
//     */
//    private static String user103 = "sudoroot";
//    /**
//     * FTP????????????
//     */
//    private static String password103 = "spider+999";
//    /**
//     * ??????????????????   103??????????????????"/opt/aspire/product/scada/uploadFile/"???
//     * 62??????????????????"/mnt/file/"???
//     */
//    private static String resourceHandler = "/images/";
//    private static String resourceLocations103 = "/opt/aspire/product/scada/uploadFile/";
//
//    private static String host62 = "10.12.70.62";
//    private static String user62 = "sudoroot";
//    private static String password62 = "Opstest+789";
//    private static String resourceLocations62 = "/mnt/file/";
    @Value("${lldp-topo-linesize:12}")
    private Integer maxNum;

    @Autowired
    private ScadaCanvasServiceClient scadaCanvasService;

//    @Autowired
//    private ConfigDictClient configDictClient;

    @Autowired
    private InstanceClient instanceClient;

    @Autowired
    private LldpInfoServiceClient lldpInfoServiceClient;

    @Autowired
    private HistoryServiceClient historyServiceClient;

    @Autowired
    private AlertsServiceClient alertsServiceClient;



    @Override
    @ResAction(action = "delete", resType = "scada")
    public ResMap delScadaCanvas(@PathVariable("id") String id) {
        com.aspire.mirror.scada.common.entity.ResMap resMap = scadaCanvasService.deleteByPrimaryKey(id);
        return changeResultMap(resMap);
    }

    @Override
    @ResAction(action = "view", resType = "scada")
    public CompScadaCanvas findScadaCanvasInfoById(@RequestParam("id") String id) {
        ScadaCanvasDTO scadaCanvasDTO = scadaCanvasService.findByPrimaryKey(id);
        CompScadaCanvas compScadaCanvas = jacksonBaseParse(CompScadaCanvas.class, scadaCanvasDTO);
        return compScadaCanvas;
    }

//    @Override
//    @ResponseStatus(HttpStatus.OK)
//    @ResAction(action = "view", resType = "scada")
//    public ResMap pictureTransformatting(@RequestParam("picture") MultipartFile picture) {
//        String data = null;
//        if (picture != null) {
//            BASE64Encoder encoder = new BASE64Encoder();
//            // ??????base64???????????????
//            try {
//                data = encoder.encode(picture.getBytes());
//            } catch (IOException e) {
//            }
//        }
//        Map<String, String> map = new HashMap<>();
//        return ResMap.success(data);
//    }

    @Override
    @ResAction(action = "view", resType = "scada")
    public ResMap saveScadaCanvas(@RequestBody CompScadaCanvas compScadaCanvas) throws Exception {
        ScadaCanvasReq scadaCanvasReq = jacksonBaseParse(ScadaCanvasReq.class, compScadaCanvas);
        com.aspire.mirror.scada.common.entity.ResMap resMap = null;
        if (StringUtils.isEmpty(compScadaCanvas.getId())) {
            ScadaCanvasDTO result = scadaCanvasService.findScadaCanvasByName(compScadaCanvas.getName());
            if (result != null) {
                return ResMap.error(ResErrorEnum.RESOURCEEXIST, null);
            }
            resMap = scadaCanvasService.creatScadaCanvas(scadaCanvasReq);
        } else {
            resMap = scadaCanvasService.modifyByPrimaryKey(scadaCanvasReq);
        }
        if (!resMap.getCode().equals(ResErrorEnum.SUCCESS.getCode())) {
            //????????????resultMap????????????
            return changeResultMap(resMap);

        } else {
            //??????????????????data????????????
            compScadaCanvas = changeToCompScadaCanvas(resMap);

        }
        return ResMap.success(compScadaCanvas);
    }

    @Override
    public List<ScadaBindValueResponse> getScadaBindValue(@RequestBody ScadaBindValueRequest bindValueRequest) {
        List<ScadaBindValueResponse> result = Lists.newArrayList();
        if (CollectionUtils.isEmpty(bindValueRequest.getBindObjList())) {
            log.warn("????????????????????????????????????");
            return result;
        }
        for (ScadaBindVO scadaBindVO : bindValueRequest.getBindObjList()) {
            if (scadaBindVO.getIsBind()) {
                ScadaBindValueResponse valueItem = new ScadaBindValueResponse();
                BeanUtils.copyProperties(scadaBindVO, valueItem);
                if (scadaBindVO.getBandType() == 0) {
                    //?????????
                    List<Map<String, String>> deviceList = scadaBindVO.getDeviceList();
                    Map<String, List<String>> ipMap = Maps.newHashMap();
                    getIpMap(deviceList, ipMap);
                    AlertValueSearchRequest alertValueSearchRequest = new AlertValueSearchRequest();
                    alertValueSearchRequest.setIpMap(ipMap);
                    alertValueSearchRequest.setAlertLevel(scadaBindVO.getAlertLevelList());
                    List<String> itemIdList = scadaBindVO.getItemList().stream().map(map -> map.get("itemId")).collect(Collectors.toList());
                    alertValueSearchRequest.setItemIdList(itemIdList);
                    int value = alertsServiceClient.getAlertValue(alertValueSearchRequest);
                    valueItem.setValue((double) value);
                } else if (scadaBindVO.getBandType() == 1) {
                    //?????????
                    HistorySearchRequest historySearchRequest = new HistorySearchRequest();
                    List<String> itemKeyList = scadaBindVO.getItemList().stream().map(map -> map.get("key")).collect(Collectors.toList());
                    historySearchRequest.setItemList(itemKeyList);
                    List<Map<String, String>> deviceList = scadaBindVO.getDeviceList();
                    Map<String, List<String>> ipMap = Maps.newHashMap();
                    getIpMap(deviceList, ipMap);
                    historySearchRequest.setIpMap(ipMap);
                    historySearchRequest.setCountType(scadaBindVO.getCountType());
                    Map<String, Object> monitorValue = historyServiceClient.getMonitorValue(historySearchRequest);
                    valueItem.setValue((Double) monitorValue.get("value"));
                }
                result.add(valueItem);
            }
        }
        return result;
    }


    private ResMap changeResultMap(com.aspire.mirror.scada.common.entity.ResMap resMap) {
        ResMap resMap1 = new ResMap();
        BeanUtils.copyProperties(resMap, resMap1);
        return resMap1;
    }

    private void getIpMap(List<Map<String, String>> deviceList, Map<String, List<String>> ipMap) {
        for (int i = 0; i < deviceList.size(); i++) {
            Map<String, String> map = deviceList.get(i);
//            JSONObject jsonObject = deviceArray.getJSONObject(i);
            String idcType = map.get("idcType");
            if (ipMap.get(idcType) == null) {
                List<String> ipList = Lists.newArrayList();
                ipList.add(map.get("ip"));
                ipMap.put(idcType, ipList);
            } else {
                List<String> ipList = ipMap.get(idcType);
                ipList.add(map.get("ip"));
            }
        }
    }
    private CompScadaCanvas changeToCompScadaCanvas(com.aspire.mirror.scada.common.entity.ResMap resMap) throws
            Exception {
        Object object = MapToObj.mapToObject((Map<String, String>) resMap.getData(), ScadaCanvasRes.class);
        ScadaCanvasRes scadaCanvasRes = (ScadaCanvasRes) object;
        return jacksonBaseParse(CompScadaCanvas.class, scadaCanvasRes);
    }
//
//    @Override
//    @ResAction(action = "view", resType = "scada")
//    public ResMap getScadaCanvasInfoByPrecinctId(@RequestParam(value = "precinctId") String precinctId,
//                                                 @RequestParam(value = "pictureType",required = false)Integer
// pictureType) throws Exception {
//        if(pictureType == null){
//            ResMap resMap = findScadaCanvasList(precinctId);
//            return resMap;
//        }else {
//            com.aspire.mirror.template.common.entity.ResMap resMap = scadaCanvasService
// .getScadaCanvasInfoByPrecinctId(precinctId, pictureType);
//            if (resMap.getCode().equals(ResErrorEnum.SUCCESS.getCode())&&resMap.getData()!=null){
//                Object object = MapToObj.mapToObject((Map<String, String>) resMap.getData(), ScadaCanvasDTO.class);
//                ScadaCanvasDTO scadaCanvasDTO = (ScadaCanvasDTO) object;
//                CompScadaCanvas compScadaCanvas = PayloadParseUtil.jacksonBaseParse(CompScadaCanvas.class,
// scadaCanvasDTO);
//                return ResMap.success(compScadaCanvas);
//            }else {
//                return changeResultMap( resMap) ;
//            }
//        }
//
//
//    }


//    @Override
//    @ResAction(action = "view", resType = "scada")
//    public ResMap upload(HttpServletRequest request, @RequestParam(value = "files") MultipartFile file) throws
//            Exception {
//
//        UploadResult uploadResult = new UploadResult();
//        if (file == null) {
//            return ResMap.notice("??????????????????", null);
//        }
//        InputStream is = null;
//        String result = null;
//        //?????????????????????????????????????????????????????????
//        int ftpPort = Integer.valueOf(port);
//        Long iconMaxsize = Long.valueOf(size);
//        String fileName = file.getOriginalFilename();
//        System.out.println(file.getSize());
//        if (file.getSize() > iconMaxsize) {
//            return ResMap.notice("?????????????????????", null);
//        }
//        if (!file.isEmpty()) {
//            is = file.getInputStream();
//            //????????????IP???????????????????????????
//            InetAddress inetAddress = InetAddress.getLocalHost();
//            String ia = inetAddress.getHostAddress().toString();
//            if (host62.equals(ia)) {
//                int nginxPort = Integer.parseInt(nginxPort62);
//                result = FtpUtls.uploadFile(host62, ftpPort, user62, password62, nginxPort,
//                        resourceLocations62, resourceHandler, fileName, is);
//                uploadResult.setUrl("http://" + result);
//            }
////              else if (host103.equals(ia)){
//            else {
////                int nginxPort = Integer.parseInt(nginxPort103);
////                result = FtpUtls.uploadFile(host103, ftpPort, user103, password103,nginxPort,
////                        resourceLocations103, resourceHandler,fileName, is);
//                int nginxPort = Integer.parseInt(nginxPort62);
//                result = FtpUtls.uploadFile(host62, ftpPort, user62, password62, nginxPort,
//                        resourceLocations62, resourceHandler, fileName, is);
//                uploadResult.setUrl("http://" + result);
//            }
//            if (null != result && !"".equals(result)) {
//            } else {
//                return ResMap.notice("??????[\"+fileName+\"]????????????", null);
//            }
//        }
//
//        uploadResult.setName(fileName);
//
//        return ResMap.success(uploadResult);
//    }

//    @Override
//    @ResAction(action = "view", resType = "scada")
//    public List<AlertAndMeteVO> getAlertCountOrMeteValueByPrecinctId(@RequestBody ComTree comTree) {
//        List<ComTree> comTrees = comTree.getComTrees();
//        if (!CollectionUtils.isEmpty(comTrees)){
//            MeteReq meteReq = new MeteReq();
//            List<MeteReq> meteReqs = new ArrayList<>();
//            QueryAlertsReq req = new QueryAlertsReq();
//            List<QueryAlertsReq> queryAlertsReqList = new ArrayList<>();
//            //????????????????????????????????????????????????????????????
//            for (ComTree comTree1 : comTrees){
//                if (StringUtils.isEmpty(comTree1.getPrecinctId())&&StringUtils.isEmpty(comTree1.getDeviceType())){
//                    ComMeteReq comMeteReq = new ComMeteReq();
//                    comMeteReq.setDeviceId(comTree1.getDeviceId());
//                    comMeteReq.setMeteId(comTree1.getId());
//                    comMeteReq.setMeteCode(comTree1.getDeviceModel());
//                    MeteReq meteReq1 = PayloadParseUtil.jacksonBaseParse(MeteReq.class, comMeteReq);
//                    meteReqs.add(meteReq1);
//
//                }
//                //??????
//                else{
//                    ComQueryAlertsReq comQueryAlertsReq = new ComQueryAlertsReq();
//                    comQueryAlertsReq.setPrecinctId(comTree1.getPrecinctId());
//                    comQueryAlertsReq.setPrecinctKind(comTree1.getPrecinctKind());
//                    comQueryAlertsReq.setDeviceKind(comTree1.getDeviceKind());
//                    comQueryAlertsReq.setDeviceType(comTree1.getDeviceType());
//                    comQueryAlertsReq.setIsParent(comTree1.getIsParent());
//                    comQueryAlertsReq.setIsPrecinct(comTree1.getIsPrecinct());
//                    QueryAlertsReq queryAlertsReq = PayloadParseUtil.jacksonBaseParse(QueryAlertsReq.class,
// comQueryAlertsReq);
//                    queryAlertsReqList.add(queryAlertsReq);
//                }
//            }
//            List<AlertAndMeteVO> alertAndMeteVOList = new ArrayList<>();
//            if (!CollectionUtils.isEmpty(meteReqs)){
//                meteReq.setMeteReqs(meteReqs);
//                List<MetePageItemVO> metePageItemVOList = monitorServiceClient.findMeasureVal(meteReq);
//                if (!CollectionUtils.isEmpty(metePageItemVOList)){
//                    for (MetePageItemVO metePageItemVO : metePageItemVOList){
//                        AlertAndMeteVO alertAndMeteVO = new AlertAndMeteVO();
//                        alertAndMeteVO.setId(metePageItemVO.getMeteId());
//                        alertAndMeteVO.setDeviceName(metePageItemVO.getDeviceName());
//                        alertAndMeteVO.setDeviceId(metePageItemVO.getDeviceId());
//                        alertAndMeteVO.setMeteId(metePageItemVO.getMeteId());
//                        alertAndMeteVO.setMeteCode(metePageItemVO.getMeteCode());
//                        alertAndMeteVO.setMeteKind(metePageItemVO.getMeteKind());
//                        alertAndMeteVO.setMeteName(metePageItemVO.getMeteName());
//                        alertAndMeteVO.setUnit(metePageItemVO.getUnit());
//                        alertAndMeteVO.setMeasureTime(metePageItemVO.getMeasureTime());
//                        alertAndMeteVO.setValue(metePageItemVO.getValue());
//                        alertAndMeteVO.setNodeType(1);
//                        alertAndMeteVOList.add(alertAndMeteVO);
//                    }
//                }
//            }
//            if (!CollectionUtils.isEmpty(queryAlertsReqList)){
//                req.setQueryAlertsReqList(queryAlertsReqList);
//                List<QueryAlertRes> queryAlertResList = alertsServiceClient.queryAlertByPrecinctId(req);
//                if (!CollectionUtils.isEmpty(queryAlertResList)){
//                    for (QueryAlertRes queryAlertRes : queryAlertResList){
//                        AlertAndMeteVO alertAndMeteVO = new AlertAndMeteVO();
//                        alertAndMeteVO.setId(queryAlertRes.getPrecinctId());
//                        alertAndMeteVO.setHasAlert(queryAlertRes.isHasAlert());
//                        alertAndMeteVO.setLevelsCount(queryAlertRes.getLevelsCount());
//                        alertAndMeteVO.setNodeType(0);
//                        alertAndMeteVOList.add(alertAndMeteVO);
//                    }
//                }
//            }
//            return alertAndMeteVOList;
//        }else {
//            return null;
//        }
//    }

    @Override
    @ResAction(action = "view", resType = "scada")
    public ResMap findScadaCanvasList(@RequestBody ScadaCanvasSearchReq scadaCanvasSearchReq) throws Exception {
        ScadaCanvasReq scadaCanvasReq = new ScadaCanvasReq();
        BeanUtils.copyProperties(scadaCanvasSearchReq, scadaCanvasReq);
        com.aspire.mirror.scada.common.entity.ResMap resMap = scadaCanvasService.findScadaCanvasList(scadaCanvasReq);
        List<CompScadaCanvas> compScadaCanvasList = new ArrayList<>();
        if (resMap.getCode().equals(ResErrorEnum.SUCCESS.getCode()) && resMap.getData() != null) {
            List<Map<String, String>> list = (List<Map<String, String>>) resMap.getData();
            for (Map<String, String> li : list) {
                Object object = MapToObj.mapToObject(li, ScadaCanvasDTO.class);
                ScadaCanvasDTO scadaCanvasDTO = (ScadaCanvasDTO) object;
                CompScadaCanvas compScadaCanvas = jacksonBaseParse(CompScadaCanvas.class, scadaCanvasDTO);
                compScadaCanvasList.add(compScadaCanvas);
            }
            return ResMap.success(compScadaCanvasList);
        } else {
            return changeResultMap(resMap);
        }
    }

    /**
     * ??????????????????
     *
     * @param scadaCanvasSearchReq
     * @return
     * @throws Exception
     */
    @Override
    public ResMap findOnlineScada(@RequestBody ScadaCanvasSearchReq scadaCanvasSearchReq) throws Exception {
        if (scadaCanvasSearchReq == null) {
            log.warn("ScadaCanvasController[findOnlineScada] param is null");
            return null;
        }
//        if (scadaCanvasSearchReq.getPictureType() != 1) {
//            log.warn("ScadaCanvasController[findOnlineScada] param pictureType is not logical topology");
//            return null;
//        }
//        List<String> idcList = Lists.newArrayList();
//        List<String> podList = Lists.newArrayList();
//        if (!StringUtils.isEmpty(scadaCanvasSearchReq.getIdc())) {
//            idcList.add(scadaCanvasSearchReq.getIdc());
//        } else {
//            List<ConfigDict> dictList = configDictClient.getDictsByType("idcType", null, null, null);
//            idcList = dictList.stream().map(configDict -> configDict.getValue()).collect(Collectors.toList());
//        }
//        if (!StringUtils.isEmpty(scadaCanvasSearchReq.getPod())) {
//            podList.add(scadaCanvasSearchReq.getPod());
//        } else {
//            List<ConfigDict> dictList = configDictClient.getDictsByType("pod_name", null, null, null);
//            podList = dictList.stream().map(configDict -> configDict.getValue()).collect(Collectors.toList());
//        }
        CmdbQueryInstance cmdbQueryInstance = new CmdbQueryInstance();
        if (!StringUtils.isEmpty(scadaCanvasSearchReq.getIdc())) {
            cmdbQueryInstance.setIdcType(scadaCanvasSearchReq.getIdc());
        }
        if (!StringUtils.isEmpty(scadaCanvasSearchReq.getPod())) {
            cmdbQueryInstance.setPod(scadaCanvasSearchReq.getPod());
        }
//        ResMap resMap = new ResMap(ResErrorEnum.SUCCESS.getCode(), "????????????");
        try {
            List<Map<String, Object>> result = instanceClient.getNetworkAndSafetyDeivce(cmdbQueryInstance);
            List<CompScadaCanvas> compScadaCanvasList = Lists.newArrayList();
            for (Map<String, Object> map : result) {
                String ipString = (String) map.get("ipString");
                String[] ipAndDeviceTypeArray = ipString.split(",");
                Map<String, String> ipMap = Maps.newHashMap();
                for (String ipAndDeviceType : ipAndDeviceTypeArray) {
                    String[] temp = ipAndDeviceType.split("_");
                    if (temp.length >= 2) {
                        ipMap.put(temp[0], temp[1]);
                    }
                }
                if (ipMap.size() > 0 && !StringUtils.isEmpty(map.get("idc")) && !StringUtils.isEmpty(map.get("pod"))) {
                    String ids = Joiner.on(",").join(ipMap.keySet());
                    String idc = (String) map.get("idc");
                    String pod = (String) map.get("pod");
                    List<LldpInfo> list = lldpInfoServiceClient.querylldpInfoByidcAndIp(idc, ids);
                    if (!CollectionUtils.isEmpty(list)) {
                        String content = LLDPPhysicalTopology.analysisPhysicalTopology(list, ipMap, instanceClient, maxNum);
                        CompScadaCanvas compScadaCanvas = new CompScadaCanvas();
                        compScadaCanvas.setName(idc + "-" + pod + "-????????????");
                        compScadaCanvas.setIdc(idc);
                        compScadaCanvas.setPod(pod);
                        compScadaCanvas.setContent(content);
                        compScadaCanvas.setPictureType(1);
                        compScadaCanvasList.add(compScadaCanvas);
                    }
                }
            }
            return ResMap.success(compScadaCanvasList);
        } catch (Exception e) {
            log.error("????????????????????????", e);
            return ResMap.error();
        }
    }

    @Override
    public PageResponse<CompScadaCanvas> pageList(@RequestBody CompScadaCanvasPageRequest pageRequest) {
        if (pageRequest == null) {
            log.warn("ScadaCanvasController[pageList] param is null");
            return null;
        }
        PageResponse<CompScadaCanvas> response = new PageResponse<>();
        ScadaCanvasPageRequest scadaCanvasPageRequest = new ScadaCanvasPageRequest();
        BeanUtils.copyProperties(pageRequest, scadaCanvasPageRequest);
        PageResponse<ScadaCanvasDTO> pageResponse = scadaCanvasService.pageList(scadaCanvasPageRequest);
        response.setCount(pageResponse.getCount());
        if (!CollectionUtils.isEmpty(pageResponse.getResult())) {
            List<CompScadaCanvas> compScadaCanvasList = jacksonBaseParse(CompScadaCanvas.class, pageResponse
                    .getResult());
            response.setResult(compScadaCanvasList);
        }
        return response;
    }

	@Override
	public ResMap getPath(@RequestBody ComPathRequest pathRequest) throws Exception {
		if (StringUtils.isEmpty(pathRequest.getContent()) || StringUtils.isEmpty(pathRequest.getDestIp()) 
   			 ||StringUtils.isEmpty(pathRequest.getSourceIp())) {
			log.warn("getPath param error:{}",JSON.toJSONString(pathRequest));
			  return new ResMap(5,"????????????");
        }
		return jacksonBaseParse(ResMap.class, scadaCanvasService.getPath(jacksonBaseParse(PathRequest.class,pathRequest)));
	}


//    @Override
//    public List<ScadaCanvasNodeInfo> getScadaCanvasNodeInfo() throws Exception{
//
//        List<ScadaCanvasNodeInfo> resultList = new ArrayList<ScadaCanvasNodeInfo>();
//
//        //??????????????????(??????)???????????????????????????
//        Map<String, Object> firstMap = deviceService.getDicsByColNames("scada_kind");
//
//        //???????????????????????????????????????
//        List<Object> firstList = (List<Object>) firstMap.get("scada_kind");
//        for(Object firstBean:firstList){
//            String[] firsSplit = firstBean.toString().replace("{", "").replace("}", "").split(", ");
//            ScadaCanvasNodeInfo fistNode = new ScadaCanvasNodeInfo();
//            fistNode.setId((firsSplit[1].split("="))[1]);
//            fistNode.setLabel((firsSplit[3].split("="))[1]);
//
//            //??????????????????(??????)???????????????????????????
//            JSONArray resultsubList = deviceService.getSubListByUpDict((firsSplit[0].split("="))[1]);
//
//            //???????????????????????????????????????
//            ObjectMapper objectMapper = new ObjectMapper();
//            JsonNode jsonNode = objectMapper.readTree(resultsubList.toString());
//            if(jsonNode.isArray()){
//                List<ScadaCanvasNodeInfo> subList = new ArrayList<ScadaCanvasNodeInfo>();
//                Iterator<JsonNode> it = jsonNode.iterator();
//                while (it.hasNext()){
//                    JsonNode node = it.next();
//                    ScadaCanvasNodeInfo subNode = new ScadaCanvasNodeInfo();
//                    subNode.setId(node.get("dictCode").asText());
//                    subNode.setLabel(node.get("dictNote").asText());
//                    subList.add(subNode);
//                }
//                fistNode.setChildren(subList);
//            }
//
//
//            resultList.add(fistNode);
//        }
//        return resultList;
//    }


//    @Override
//    public List<Map<String, Object>> getExcelData(@RequestParam(value = "files") MultipartFile file) throws Exception {
//
//        //??????????????????????????????
//        ArrayList<Map<String, Object>> resultList = new ArrayList<>();
//
//
//        //???????????????????????????????????????
//        //??????????????????????????????myspringboot??????FileUploadController???
//        XSSFWorkbook workbook = new XSSFWorkbook(file.getInputStream());
//        for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
//
//            //?????????==================================================================================?????????
//            //?????????===================== ??????excel??????????????????=========================================?????????
//
//            //??????????????????????????????????????????????????????????????????????????????????????????????????????
//            TreeMap<String, String> DataMap = new TreeMap<>(new Comparator<String>() {
//                @Override
//                public int compare(String o1, String o2) {
//                    return o1.compareTo(o2);
//                }
//            });
//
//
//            XSSFSheet sheet = workbook.getSheetAt(sheetNum);
//            //????????????????????????sheet
//            if (sheet.getSheetName().equals("????????????") || sheet.getSheetName().equals("???????????????????????????--??????")) {
//                continue;
//            }
//
//
//            for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
//                CellRangeAddress mergedRegion = sheet.getMergedRegion(i);
//                int firstRow = mergedRegion.getFirstRow();
//                //????????????
//                if (firstRow <= 1) {
//                    continue;
//                }
//
//                int firstColumn = mergedRegion.getFirstColumn();
//
//                //???????????????????????????0??????1??????4??????????????????????????????
//                if (firstColumn != 0 & firstColumn != 1 & firstColumn != 4) {
//                    continue;
//                }
//
//                //????????????????????????????????????id
//                //???????????????sheet??????+row??????+column??????
//                //??????????????????4?????????????????????????????????????????????compareTo???????????????????????????????????????????????????????????????
//                String key = "s" + String.format("%04d", sheetNum) + "r" + String.format("%04d", firstRow) + "c" +
//                        String.format("%04d", firstColumn);
//                DataMap.put(key, sheet.getRow(firstRow).getCell(firstColumn).getStringCellValue());
//            }
//
//            //?????????==================================================================================?????????
//            //?????????===================== ??????excel??????????????????=========================================?????????
//
//
//            //?????????==================================================================================?????????
//            //?????????====?????????????????????????????????????????????(ps???????????????????????????????????????????????????????????????????????????????????????)?????????
//
//            //??????????????????????????????????????????????????????????????????????????????
//            ArrayList<Map<String, String>> sheetList = new ArrayList<>();
//
//            //?????????????????????????????????????????????
//            int counter = 0;
//            LinkedHashMap<String, String> rowMap = new LinkedHashMap<>();
//            for (String key : DataMap.keySet()) {
//                if (counter == 3) {
//                    //???????????????????????????map?????????sheetList.
//                    sheetList.add(rowMap);
//
//                    //????????????????????????????????????
//                    counter = 0;
//                    rowMap = new LinkedHashMap<>();
//                }
//                counter = counter + 1;
//                rowMap.put("num" + counter, DataMap.get(key)); //rowMap?????????key?????????????????????????????????????????????????????????
//            }
//
//
//            //???sheetList?????????sheetMap
//            //sheetMap???????????????
//            //key???sheet.getSheetName()?????????sheet?????????
//            //value???sheetList??????????????????rowMap
//            LinkedHashMap<String, Object> sheetMap = new LinkedHashMap<>();
//            sheetMap.put(sheet.getSheetName(), sheetList);
//
//
//            //??????????????????sheet???????????????????????????????????????resultList???
//            resultList.add(sheetMap);
//
//
//            //?????????==================================================================================?????????
//            //?????????============== ?????????????????????????????????????????????=========================================?????????
//
//        }
//
//
//        return resultList;
//    }


    //????????????
    //???????????????????????????
//    @Override
//    public String batchSaveScadaCanvas() throws Exception{
//
//        File path = new File("d:/667788/");
//        File listFile = new File("c:/Users/zhujiahao/Desktop/??????????????????/??????????????????/????????????/??????????????????/?????????????????????????????????id??????.txt");
//        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(listFile),
// "UTF-8"));
//        String s = null;
//        while((s = bufferedReader.readLine()) != null){
//
//            String[] array = s.split("=");
//
//            //???????????????????????????
//            String content = null;
//            File file = new File(path, array[0]);
//            Long filelength = file.length();
//            byte[] filecontent = new byte[filelength.intValue()];
//            try {
//                FileInputStream in = new FileInputStream(file);
//                in.read(filecontent);
//                in.close();
//                content = new String(filecontent,"UTF-8");
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            //??????????????????
//            String scadaName = array[0].replace(".xml","").replace("??????1???","");
//
//            //????????????id
//            String precinctId = "";
//            String precinctName = "";
//            if(array.length >= 2){
//                precinctId = array[1];
//                precinctName = array[2];
//            }
//
//            CompScadaCanvas canvas = new CompScadaCanvas();
//            canvas.setContent(content);
//            canvas.setName(scadaName);
//            canvas.setPrecinctId(precinctId);
//            canvas.setPictureType(1000);
//            canvas.setPageType(0);
//            ResMap resMap = this.saveScadaCanvas(canvas);
//
//            System.out.println("????????????"+scadaName+"******??????id???"+precinctId+"******???????????????"+precinctName);
//        }
//
//
//        return null;
//    }
}
