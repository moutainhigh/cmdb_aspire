package com.migu.tsg.microservice.atomicservice.composite.controller.alert;

import com.alibaba.fastjson.JSONObject;
import com.aspire.mirror.alert.api.dto.alert.*;
import com.aspire.mirror.alert.api.dto.alert.AlertStatisticSummaryDTO;
import com.aspire.mirror.common.entity.PageResponse;
import com.aspire.mirror.composite.payload.alert.*;
import com.aspire.mirror.composite.service.alert.ICompAlertsStatisticService;
import com.google.common.collect.Maps;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.alert.AlertsStatisticServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.controller.CommonResourceV2Controller;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.ResAction;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.ExportExcelUtil;
import com.migu.tsg.microservice.atomicservice.composite.vo.rbac.RbacResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.assertj.core.util.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.migu.tsg.microservice.atomicservice.composite.controller.util.PayloadParseUtil.jacksonBaseParse;
@RestController
@Slf4j
public class CompAlertsStatisticController extends CommonResourceV2Controller implements ICompAlertsStatisticService {
    private Logger logger = LoggerFactory.getLogger(CompAlertsStatisticController.class);
//    private static final SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private void showOperator() {
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        logger.info("[pageList]Username is {}.", authCtx.getUser().getUsername());
    }
    @Autowired
    private AlertsStatisticServiceClient statisticServiceClient;
    /**
     * ????????????
     *
     * @return AlertStatisticSummaryResponse ????????????
     */
    @Override
    @ResAction(resType="mainIndexPage", action="summary", loadResFilter=true)
    public CompAlertStatisticSummaryResp summary(@RequestParam(name = "idcType", required = false) String idcType) {
//        showOperator();
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        resAuthHelper.resourceActionVerify(authCtx.getUser(), new RbacResource(), authCtx.getResAction(), authCtx.getFlattenConstraints());
        return jacksonBaseParse(CompAlertStatisticSummaryResp.class, statisticServiceClient.summary(idcType));
    }

    @Override
    @ResAction(resType="mainIndexPage", action="getAlertSummary", loadResFilter=true)
    public CompAlertSummaryResponse getAlertSummary() {
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        resAuthHelper.resourceActionVerify(authCtx.getUser(), new RbacResource(), authCtx.getResAction(), authCtx.getFlattenConstraints());
        Map<String, Object> constraints = Maps.newHashMap();
//        GeneralAuthConstraintsAware generalAuthConstraintsAware = new GeneralAuthConstraintsAware();
//        Map<String, Object> constraints = Maps.newHashMap();
//        try {
//            if (!authCtx.getUser().isAdmin() && !authCtx.getUser().isSuperUser()) {
//                Map<String, Set<String>> totalConstraints = resAuthHelper.verifyActionAndGetResource(
//                        authCtx.getUser(), authCtx.getResAction(), authCtx.getFlattenConstraints());
//
//                if (!super.applyGeneralAuthConstraints(totalConstraints, generalAuthConstraintsAware)) {
//                    return new CompAlertSummaryResponse();
//                }
//                constraints.put("userName", authCtx.getUser().getUsername());
//                constraints.put("authBizSystemIdList",generalAuthConstraintsAware.getAuthBizSystemIdList());
//                constraints.put("authDeviceIdList",generalAuthConstraintsAware.getAuthDeviceIdList());
//                constraints.put("authDeviceTypeList",generalAuthConstraintsAware.getAuthDeviceTypeList());
//                constraints.put("authIdcIdList",generalAuthConstraintsAware.getAuthIdcIdList());
//                constraints.put("authPrecinctList",generalAuthConstraintsAware.getAuthPrecinctList());
//            }
//            response = jacksonBaseParse(CompAlertSummaryResponse.class, statisticServiceClient.getAlertSummary(constraints));
//        } catch (Exception e) {
//            log.error("[Composite]>>> getAlertSummary error is {}", e);
//            throw new RuntimeException("[Composite]>>> getAlertSummary error is {}", e);
//        }
        constraints.put("userName", authCtx.getUser().getUsername());
        return jacksonBaseParse(CompAlertSummaryResponse.class, statisticServiceClient.getAlertSummary(constraints));
    }

    @Override
    public Map<String, Object> getAlertSummaryByDeviceClass(@RequestParam("code") String code) {
        log.info("[Composite]>>> getAlertSummaryByDeviceClass request is {}", code);
        Map<String, Object> response = Maps.newHashMap();
        // ??????
        List<String> deviceClass = Lists.newArrayList();
        // ????????????
        List<Integer> deviceSum = Lists.newArrayList();
        // ???
        List<Integer> lowCount = Lists.newArrayList();
        // ???
        List<Integer> mediumCount = Lists.newArrayList();
        // ???
        List<Integer> highCount = Lists.newArrayList();
        // ??????
        List<Integer> seriousCount = Lists.newArrayList();
        try {
            List<AlertMainSummaryResponse> alertSummaryByDeviceClass =
                    statisticServiceClient.getAlertSummaryByDeviceClass(code);
            for (AlertMainSummaryResponse str : alertSummaryByDeviceClass) {
                AlertStatisticSummaryDTO alertStatisticSummaryDTO = str.getAlertStatisticSummaryDTO();
                deviceClass.add(StringUtils.isEmpty(str.getDeviceClass()) ? "???" : str.getDeviceClass());
                deviceSum.add(null == alertStatisticSummaryDTO.getSummary() ? 0 : alertStatisticSummaryDTO.getSummary());
                lowCount.add(null == alertStatisticSummaryDTO.getLow() ? 0 : alertStatisticSummaryDTO.getLow());
                mediumCount.add(null == alertStatisticSummaryDTO.getMedium() ? 0 : alertStatisticSummaryDTO.getMedium());
                highCount.add(null == alertStatisticSummaryDTO.getHigh() ? 0 : alertStatisticSummaryDTO.getHigh());
                seriousCount.add(null == alertStatisticSummaryDTO.getSerious() ? 0 : alertStatisticSummaryDTO.getSerious());
            }
            response.put("deviceClass", deviceClass);
            response.put("sum", deviceSum);
            response.put("low", lowCount);
            response.put("medium", mediumCount);
            response.put("high", highCount);
            response.put("serious", seriousCount);
            log.info("[Composite]>>> getAlertSummaryByDeviceClass response is {}", JSONObject.toJSONString(response));
        } catch (Exception e) {
            log.error("[Composite]>>> getAlertSummaryByDeviceClass response is {}", e);
            throw new RuntimeException("[Composite]>>> getAlertSummaryByDeviceClass error is {}", e);
        }
        return response;
    }

    /**
     * ????????????
     *
     * @return AlertStatisticSummaryResponse ????????????
     */
    @Override
    public CompAlertsOverViewResponse overview(@RequestParam(value = "codes") String codes) throws ParseException {
        showOperator();
//        codes = "0,1";
        return jacksonBaseParse(CompAlertsOverViewResponse.class, statisticServiceClient.overview(codes));
    }

    /**
     * ??????-??????????????????
     *
     * @return AlertStatisticSummaryResponse ????????????
     */
    @Override
    public List<String> monitObjectList() {
        showOperator();
        return statisticServiceClient.monitObjectList();
    }

    /**
     * ????????????-??????????????????
     *
     * @return AlertsSummaryResponse ??????????????????
     */
    @Override
    public List<String> monitObjectHisList() {
        showOperator();
        return statisticServiceClient.monitObjectHisList();
    }

    /**
     * ????????????????????????
     *
     * @param span
     * @return AlertStatisticSummaryDTO ??????????????????
     */
    @Override
    public CompAlertStatisticLevelResp alertLevelSummay(@RequestParam(value = "span") String span) {
        showOperator();
        return jacksonBaseParse(CompAlertStatisticLevelResp.class, statisticServiceClient.alertLevelSummayByTimeSpan(span));
    }

    /**
     * ????????????????????????
     *
     * @param inteval
     * @return
     */
    @Override
    public Map trend(@RequestParam(value = "span") String inteval) {
        showOperator();
        return statisticServiceClient.trend(inteval);
    }

    /**
     * ??????????????????
     *
     * @param limit
     * @return
     */
    @Override
    @ResAction(resType="mainIndexPage", action="latestAlertLimit20", loadResFilter=true)
    public List<CompAlertsDetailResp> latest(@RequestParam(value = "limit") Integer limit) {
//        showOperator();
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        resAuthHelper.resourceActionVerify(authCtx.getUser(), new RbacResource(), authCtx.getResAction(), authCtx.getFlattenConstraints());
        List<AlertsDetailResponse> detailResponseList = statisticServiceClient.latest(limit);
        List<CompAlertsDetailResp> respList = Lists.newArrayList();
        for (AlertsDetailResponse detailResponse : detailResponseList) {
            CompAlertsDetailResp resp = new CompAlertsDetailResp();
            BeanUtils.copyProperties(detailResponse, resp);
            respList.add(resp);
        }
        return respList;
    }

    /**
     * ????????????????????????
     *
     * @param span
     * @return
     * @throws ParseException
     */
    @Override
    public List<CompAlertsStatisticClassifyResp> classifyByTimeSpan(@RequestParam(value = "span") String span) {
        showOperator();
        List<AlertsStatisticClassifyDTO> classifyList = statisticServiceClient.classifyByTimeSpan(span);
        List<CompAlertsStatisticClassifyResp> respList = Lists.newArrayList();
        for (AlertsStatisticClassifyDTO classifyDTO : classifyList) {
            CompAlertsStatisticClassifyResp resp = new CompAlertsStatisticClassifyResp();
            BeanUtils.copyProperties(classifyDTO, resp);
            respList.add(resp);
        }
        return respList;
    }

    /**
     * ?????????????????????
     *
     * @param span
     * @return
     */
    @Override
    public List<Map<String, Object>> sourceClassifyByTimeSpan(@RequestParam(value = "span") String span) {
        showOperator();
        List<Map<String, Object>> list = statisticServiceClient.sourceClassifyByTimeSpan(span);
        return list;
    }

    private static final String CMDB_HELPER_KEY_IDCTYPE = "idcType";

    /**
     * ??????/?????? ?????? ????????????
     *
     * @param queryRequest
     * @return
     */
    @Override
    public PageResponse<CompAlertsDetailResp> query(@RequestBody CompAlertsDataRequest queryRequest) throws ParseException {
        showOperator();
        AlertsDataRequest alertsQueryRequest = jacksonBaseParse(AlertsDataRequest.class, queryRequest);
        logger.info("#=====> alert query entity: {}", JSONObject.toJSONString(alertsQueryRequest));
        PageResponse<AlertsDetailResponse> pageResponse = statisticServiceClient.query(alertsQueryRequest);
        PageResponse<CompAlertsDetailResp> compResponse = new PageResponse<>();
        compResponse.setCount(pageResponse.getCount());
        List<CompAlertsDetailResp> alertsRespList = Lists.newArrayList();
        List<AlertsDetailResponse> alertsList = pageResponse.getResult();
        if (!CollectionUtils.isEmpty(alertsList)) {
            alertsRespList = jacksonBaseParse(CompAlertsDetailResp.class, alertsList);
        }
        compResponse.setResult(alertsRespList);
        return compResponse;
    }

    /**
     * ????????????????????????
     *
     * @param queryRequest
     * @param response
     */
    @Override
    public void export(@RequestBody CompAlertsDataRequest queryRequest, HttpServletResponse response) throws Exception {
        AlertsDataRequest alertsQueryRequest = jacksonBaseParse(AlertsDataRequest.class, queryRequest);
        String[] headerList = {"??????","??????IP","????????????","?????????","????????????","????????????","????????????","?????????","????????????","pod???","????????????","?????????","????????????","????????????","????????????","????????????","????????????","????????????","????????????","??????","????????????","????????????","????????????","?????????","????????????","????????????","????????????","?????????","????????????","????????????","????????????","????????????","????????????","????????????"};
        String[] keyList = {"alertLevel","deviceIp","moniObject","curMoniValue","moniIndex","alertStartTime","curMoniTime","idcType","projectName","podName","sourceRoom","bizSystem","deviceClass","deviceType","deviceMfrs","deviceModel","hostName","source","objectType","orderStatus","reportType","reportStatus","reportTime","transUser","transStatus","transTime","toConfirmUser","confirmedUser","confirmedTime","confirmedContent","deliverStatus","deliverTime","orderType","alertCount"};
        SimpleDateFormat sDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String title = "??????????????????_" + sDateFormat.format(new Date());
        String fileName = title+".xlsx";
        List<Map<String, Object>> dataList = statisticServiceClient.export(alertsQueryRequest);
        OutputStream os = response.getOutputStream();// ???????????????
        response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
        response.setHeader("Connection", "close");
        response.setHeader("Content-Type", "application/vnd.ms-excel");
        //excel constuct
        ExportExcelUtil eeu = new ExportExcelUtil();
        Workbook book = new SXSSFWorkbook(128);
        eeu.exportExcel(book, 0, title, headerList, dataList, keyList);
        book.write(os);
    }

    /**
     * ??????/?????? ???????????? ????????????
     *
     * @param queryRequest
     * @return
     */
    @Override
    public PageResponse<CompAlertsHisDetailResp> queryHis(@RequestBody CompAlertsHisDataRequest queryRequest) throws ParseException {
        AlertsHisDataRequest alertsQueryRequest = jacksonBaseParse(AlertsHisDataRequest.class, queryRequest);
        PageResponse<AlertsHisDetailResponse> hisPageList = statisticServiceClient.queryHis(alertsQueryRequest);
        PageResponse<CompAlertsHisDetailResp> hisCompResponse = new PageResponse<>();
        hisCompResponse.setCount(hisPageList.getCount());
        List<CompAlertsHisDetailResp> hisAlertsRespList = Lists.newArrayList();
        List<AlertsHisDetailResponse> hisAlertsList = hisPageList.getResult();
        if (!CollectionUtils.isEmpty(hisAlertsList)) {
            hisAlertsRespList = jacksonBaseParse(CompAlertsHisDetailResp.class, hisAlertsList);
        }
        hisCompResponse.setResult(hisAlertsRespList);
        return hisCompResponse;
    }

    /**
     * ??????????????????????????????
     *
     * @param queryRequest
     * @param response
     */
    @Override
    public Map<String, Object> exportHis(@RequestBody CompAlertsHisDataRequest queryRequest, HttpServletResponse response) throws Exception {
        Map<String, Object> map = new HashMap<>();
        if (queryRequest == null) {
            logger.error("Alert query param pageRequset is null or query type is empty !");
            map.put("code","0001");
            map.put("message", "query paramters are blank.");
            return map;
        }
        AlertsHisDataRequest alertsQueryRequest = jacksonBaseParse(AlertsHisDataRequest.class, queryRequest);
//        String[] headerList = {"??????","??????IP","????????????","?????????","????????????","????????????","????????????","?????????","????????????","pod???","????????????","?????????","????????????","????????????","????????????","????????????","????????????","????????????","????????????","??????","????????????","????????????","????????????","?????????","????????????","????????????","????????????","?????????","????????????","????????????","????????????","????????????","????????????","??????????????????","?????????","????????????","????????????"};
//        String[] keyList = {"alertLevel","deviceIp","moniObject","curMoniValue","moniIndex","alertStartTime","curMoniTime","idcType","projectName","podName","sourceRoom","bizSystem","deviceClass","deviceType","deviceMfrs","deviceModel","hostName","source","objectType","orderStatus","reportType","reportStatus","reportTime","transUser","transStatus","transTime","toConfirmUser","confirmedUser","confirmedTime","confirmedContent","deliverStatus","deliverTime","orderType","alertEndTime","clearUser","clearTime","clearContent"};
//        String title = "????????????????????????_" + sDateFormat.format(new Date());
//        String fileName = title+".xlsx";
//        List<Map<String, Object>> dataList = statisticServiceClient.exportHis(alertsQueryRequest);
//        OutputStream os = response.getOutputStream();// ???????????????
//        response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
//        response.setHeader("Connection", "close");
//        response.setHeader("Content-Type", "application/vnd.ms-excel");
        //excel constuct
//        ExportExcelUtil eeu = new ExportExcelUtil();
//        Workbook book = new SXSSFWorkbook(128);
//        eeu.exportExcel(book, 0, title, headerList, dataList, keyList);
////        book.write(os);
//        ByteArrayOutputStream ops = null;
//        ByteArrayInputStream in = null;
//        try {
//            ops = new ByteArrayOutputStream();
//            book.write(ops);
//            byte[] b = ops.toByteArray();
//            in = new ByteArrayInputStream(b);
//            map = ftpService.uploadtoFTP(fileName, in);
//            ops.flush();
//        } catch (Exception e) {
//            log.error("??????excel????????????????????????", e);
//            map.put("code","0005");
//            map.put("message", e.getMessage());
//        }finally {
//            IOUtils.closeQuietly(book);
//            IOUtils.closeQuietly(ops);
//            IOUtils.closeQuietly(in);
//            return map;
//        }
        return statisticServiceClient.exportHis(alertsQueryRequest);
    }

    @Override
    public List<CompAlertTargetCountResponse> getAlertTargetCount(@RequestParam(value = "alertLevel", required = false) String alertLevel,
                                                                  @RequestParam("type") String type) {
        List<CompAlertTargetCountResponse> response = Lists.newArrayList();
        try {
            List<AlertTargetCountResponse> alertTargetCountResponses = statisticServiceClient.getAlertTargetCount(alertLevel, type);
            for (AlertTargetCountResponse alertTargetCountResponse : alertTargetCountResponses) {
                response.add(jacksonBaseParse(CompAlertTargetCountResponse.class, alertTargetCountResponse));
            }
        } catch (Exception e) {
            throw new RuntimeException("[Composite]>>> getAlertTargetCount error is {}", e);
        }
        return response;
    }

    @Override
    public List<Map<String, Object>> getAlertCountByIpAndIdc(@RequestBody List<Map<String,Object>> request) {

        return statisticServiceClient.getAlertCountByIpAndIdc(request);
    }
}
