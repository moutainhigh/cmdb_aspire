package com.aspire.ums.cmdb.ipAudit.web;

import com.aspire.ums.cmdb.common.ResultVo;
import com.aspire.ums.cmdb.deviceStatistic.util.ExportExcelUtil;
import com.aspire.ums.cmdb.ipAudit.IIpAuditAPI;
import com.aspire.ums.cmdb.ipAudit.enums.IpAuditEnum;
import com.aspire.ums.cmdb.ipAudit.payload.*;
import com.aspire.ums.cmdb.ipAudit.service.*;
import com.aspire.ums.cmdb.ipAudit.service.IIpAuditAssignedUnsurvivingIntranetIpService;
import com.aspire.ums.cmdb.ipAudit.service.IIpAuditRecordedUnsurvivingIntranetIpService;
import com.aspire.ums.cmdb.ipAudit.service.IIpAuditSurvivingUnplanIpv6Service;
import com.aspire.ums.cmdb.ipAudit.service.IIpAuditSurvivingUnrecordIntranetIpService;
import com.aspire.ums.cmdb.util.JavaBeanUtil;
import com.aspire.ums.cmdb.util.StringUtils;
import com.google.common.collect.Lists;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class IpAuditController implements IIpAuditAPI {

    @Autowired
    private IIpAuditSurvivingUnrecordIntranetIpService survivingUnrecordIntranetIpService;
    @Autowired
    private IIpAuditRecordedUnsurvivingIntranetIpService recordedUnsurvivingIntranetIpService;
    @Autowired
    private IIpAuditAssignedUnsurvivingIntranetIpService assignedUnsurvivingIntranetIpService;
    @Autowired
    private IIpAuditSurvivingUnplannedIntranetIpService survivingUnplannedIntranetIpService;
    @Autowired
    private IIpAuditRegistrationExpiredService registrationExpiredService;
    @Autowired
    private IIpAuditSurvivingUnassignIntranetIpService survivingUnassignIntranetIpService;
    @Autowired
    private IIpAuditSurvivingUnplanIpv6Service ipv6UnPlanService;
    @Autowired
    private IIpAuditSurvivingUnassignIpV6Service ipv6UnAssignService;
    @Autowired
    private IIpAuditSurvivingUnplanPublicIpService publicIpUnPlanService;
    @Autowired
    private IIpAuditSurvivingUnassignPublicIpService publicIpUnAssignService;
    @Autowired
    private IpAuditConfigDictService configDictService;

    /**
     * ??????????????????
     * @param object
     */
    private void dealWithReq(Object object) {
        if (object instanceof IpAuditCommonReq) {
            IpAuditCommonReq req = (IpAuditCommonReq) object;
            if (req.getPageNo() != null && req.getPageSize() != null) {
                req.updatePageNum();
            }
            String ipList = req.getIpList();
            if (StringUtils.isNotEmpty(ipList)) {
                String[] ips = ipList.split(",");
                req.setIps(Lists.newArrayList(ips));
            }
        } else if (object instanceof IpAuditUpdateRequest) {
            IpAuditUpdateRequest request = (IpAuditUpdateRequest) object;
            if (CollectionUtils.isEmpty(request.getMainId())) {
                if (request.getMainId().stream().allMatch(StringUtils::isEmpty)) {
                    throw new RuntimeException("??????ID mainId ???????????????");
                }
            } else if (StringUtils.isEmpty(request.getHandleStatus())) {
                throw new RuntimeException("???????????? handleStatus ???????????????");
            } else if (request.getHandleStatus().equals("????????????") && StringUtils.isEmpty(request.getNotHandleReason())) {
                throw new RuntimeException("????????????????????????????????????????????????");
            } else if ((request.getHandleStatus().equals("????????????") || request.getHandleStatus().equals("?????????"))
                    && StringUtils.isEmpty(request.getOperator())) {
                throw new RuntimeException("????????????????????????");
            }
        }
    }

    @Override
    public IpAuditPageBean<SurvivingUnrecordIntranetIpResp> getSurvivingUnrecordIntranetIpList(@RequestBody SurvivingUnrecordIntranetIpReq req) {
        log.info("survivingUnrecordIntranetIpReq is {} ", req);
        IpAuditPageBean<SurvivingUnrecordIntranetIpResp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(survivingUnrecordIntranetIpService.getList(req));
            int listCount = survivingUnrecordIntranetIpService.getListCount(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = survivingUnrecordIntranetIpService.getStatisticsData(req);
            respPageBean.setNumOfUnrecordIp(listCount);
            respPageBean.setNumOfNotProcessIp(Integer.valueOf(statisticsData.get("numOfNotProcessIp") == null ? "0" : statisticsData.get("numOfNotProcessIp").toString()));
            respPageBean.setNumOfToBeProcessedIp(Integer.valueOf(statisticsData.get("numOfToBeProcessedIp") == null ? "0" : statisticsData.get("numOfToBeProcessedIp").toString()));
            respPageBean.setNumOfUnsuitableUseIp(Integer.valueOf(statisticsData.get("numOfUnsuitableUseIp") == null ? "0" : statisticsData.get("numOfUnsuitableUseIp").toString()));
        } catch (Exception e) {
            log.error("??????IP?????????CMDB?????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportSurvivingUnrecordIntranetIpList(@RequestBody SurvivingUnrecordIntranetIpReq param, HttpServletResponse response) {
        log.info("SurvivingUnrecordIntranetIpReq is {}", param);
        String[] headerList = {"????????????", "IP", "????????????", "????????????", "????????????",
                "????????????", "??????????????????", "????????????", "????????????", "?????????", "?????????",
                "????????????IP", "???????????????", "????????????", "????????????", "?????????", "????????????"};
        String[] keyList = {"checkTime", "ip", "useStatus", "networkSegmentType", "networkSegmentSubType", "ipUse",
                "firstBusinessSystem", "assignStatus", "assignTime", "assignUser", "requestPerson",
                "deviceIp", "dc", "processingStatus", "reason", "operator", "operatingTime"};
        String title = "??????IP?????????CMDB";
        String fileName = title + ".xlsx";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<SurvivingUnrecordIntranetIpResp> list = survivingUnrecordIntranetIpService.getList(param);
            for (SurvivingUnrecordIntranetIpResp entity : list) {
                Map<String, Object> map = ExportExcelUtil.objectToMap(entity);
                dataLists.add(map);
            }
            os = response.getOutputStream();// ???????????????
            response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setHeader("Content-Type", "application/vnd.ms-excel");
            //excel constuct
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, title, headerList, dataLists, keyList);
            book.write(os);
            os.flush();
            log.info("??????/????????????: {} ??????!", fileName);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public ResultVo updateSurvivingUnrecordIntranetIpProcessStatus(@RequestBody IpAuditUpdateRequest request) {
        log.info("ipAuditUpdateRequest request is {}", request);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            dealWithReq(request);
            survivingUnrecordIntranetIpService.updateProcess(request);
        } catch (Exception e) {
            log.error("???????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public IpAuditPageBean<RecordedUnsurvivingIntranetIpResp> getRecordedUnsurvivingIntranetIpList(@RequestBody RecordedUnsurvivingIntranetIpReq req) {
        log.info("recordedUnsurvivingIntranetIpReq is {} ", req);
        IpAuditPageBean<RecordedUnsurvivingIntranetIpResp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(recordedUnsurvivingIntranetIpService.getList(req));
            int listCount = recordedUnsurvivingIntranetIpService.getListCount(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = recordedUnsurvivingIntranetIpService.getStatisticsData(req);
            respPageBean.setNumOfUnsurvivingIp(listCount);
            respPageBean.setNumOfProblemAsset(Integer.valueOf(statisticsData.get("numOfProblemAsset") == null ? "0" :
                    statisticsData.get("numOfProblemAsset").toString()));
            respPageBean.setNumOfToBeProcessAsset(Integer.valueOf(statisticsData.get("numOfToBeProcessAsset") == null ? "0" :
                    statisticsData.get("numOfToBeProcessAsset").toString()));
        } catch (Exception e) {
            log.error("??????CMDB??????????????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportRecordedUnsurvivingIntranetIpList(@RequestBody RecordedUnsurvivingIntranetIpReq param, HttpServletResponse response) {
        log.info("recordedUnsurvivingIntranetIpReq is {}", param);
        String[] headerList = {"????????????", "IP", "IP??????", "????????????IP", "??????",
                "???????????????", "??????????????????", "?????????????????????", "????????????", "??????????????????",
                "???????????????(??????)", "????????????", "????????????", "?????????", "????????????"};
        String[] keyList = {"checkTime", "ip", "ipType", "deviceIp", "hostBackup", "dc",
                "businessLevel1", "businessLevel2", "surviveStatus", "lastSurviveTime",
                "unsurvivedDuration", "processingStatus", "reason", "operator", "operatingTime"};
        String title = "??????CMDB?????????IP";
        String fileName = title + ".xlsx";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<RecordedUnsurvivingIntranetIpResp> list = recordedUnsurvivingIntranetIpService.getList(param);
            for (RecordedUnsurvivingIntranetIpResp entity : list) {
                Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                dataLists.add(map);
            }
            os = response.getOutputStream();// ???????????????
            response.setHeader("Content-Disposition",
                    "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setContentType("application/vnd.ms-excel");
            // excel constuct
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, title, headerList, dataLists, keyList);
            book.write(os);
            os.flush();
            log.info("??????/????????????: {} ??????!", fileName);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public ResultVo updateRecordedUnsurvivingIntranetIpProcessStatus(@RequestBody IpAuditUpdateRequest request) {
        log.info("ipAuditUpdateRequest request is {}", request);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            dealWithReq(request);
            recordedUnsurvivingIntranetIpService.updateProcess(request);
        } catch (Exception e) {
            log.error("???????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public IpAuditPageBean<AssignedUnsurvivingIntranetIpResp> getAssignedUnsurvivingIntranetIpList(@RequestBody AssignedUnsurvivingIntranetIpReq req) {
        log.info("assignedUnsurvivingIntranetIpReq is {} ", req);
        IpAuditPageBean<AssignedUnsurvivingIntranetIpResp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(assignedUnsurvivingIntranetIpService.getList(req));
            int listCount = assignedUnsurvivingIntranetIpService.getListCount(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = assignedUnsurvivingIntranetIpService.getStatisticsData(req);
            respPageBean.setNumOfUnsurvivingIp(listCount);
            respPageBean.setNumOfUnsurvivingIpWithinOneMonth(Integer.valueOf(statisticsData.get("numOfUnsurvivingIpWithinOneMonth") == null ? "0" : statisticsData.get("numOfUnsurvivingIpWithinOneMonth").toString()));
            respPageBean.setNumOfUnsurvivingIpWithinThreeMonths(Integer.valueOf(statisticsData.get("numOfUnsurvivingIpWithinThreeMonths") == null ? "0" : statisticsData.get("numOfUnsurvivingIpWithinThreeMonths").toString()));
            respPageBean.setNumOfToBeProcessedIp(Integer.valueOf(statisticsData.get("numOfToBeProcessedIp") == null ? "0" : statisticsData.get("numOfToBeProcessedIp").toString()));
        } catch (Exception e) {
            log.error("??????????????????IP?????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportAssignedUnsurvivingIntranetIpList(@RequestBody AssignedUnsurvivingIntranetIpReq assignedUnsurvivingIntranetIpReq, HttpServletResponse response) {
        log.info("assignedUnsurvivingIntranetIpReq is {}", assignedUnsurvivingIntranetIpReq);
        String[] headerList = {"????????????", "IP", "???????????????", "????????????", "????????????",
                "????????????", "??????????????????", "??????????????????", "????????????", "????????????", "?????????", "?????????", "????????????",
                "????????????", "??????????????????", "???????????????(??????)", "????????????", "????????????", "?????????", "????????????"};
        String[] keyList = {"checkTime", "ip", "dc", "networkSegmentType", "networkSegmentSubType", "ipUse",
                "firstBusinessSystem", "aloneBusinessSystem", "assignStatus", "assignTime", "assignUser", "requestPerson",
                "requestTime", "surviveStatus", "lastSurviveTime", "unsurvivedDuration", "processingStatus",
                "reason", "operator", "operatingTime"};
        String title = "??????????????????IP";
        String fileName = title + ".xlsx";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<AssignedUnsurvivingIntranetIpResp> list = assignedUnsurvivingIntranetIpService.getList(assignedUnsurvivingIntranetIpReq);
            if (!CollectionUtils.isEmpty(list)) {
                for (AssignedUnsurvivingIntranetIpResp entity : list) {
                    Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                    dataLists.add(map);
                }
            }
            os = response.getOutputStream();// ???????????????
            response.setHeader("Content-Disposition",
                    "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setContentType("application/vnd.ms-excel");
            // excel constuct
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, title, headerList, dataLists, keyList);
            book.write(os);
            os.flush();
            log.info("??????/????????????: {} ??????!", fileName);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public ResultVo updateAssignedUnsurvivingIntranetIpProcessStatus(@RequestBody IpAuditUpdateRequest request) {
        log.info("ipAuditUpdateRequest request is {}", request);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            dealWithReq(request);
            assignedUnsurvivingIntranetIpService.updateProcess(request);
        } catch (Exception e) {
            log.error("???????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public IpAuditPageBean<SurvivingUnplannedIntranetIpResp> getSurvivingUnplannedIntranetIpList(@RequestBody SurvivingUnplannedIntranetIpReq req) {
        log.info("survivingUnplannedIntranetIpReq is {} ", req);
        IpAuditPageBean<SurvivingUnplannedIntranetIpResp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(survivingUnplannedIntranetIpService.getList(req));
            int listCount = survivingUnplannedIntranetIpService.getListCount(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = survivingUnplannedIntranetIpService.getStatisticsData(req);
            respPageBean.setNumOfSurvivingUnplannedIp(listCount);
            respPageBean.setNumOfToBeProcessedIp(Integer.valueOf(statisticsData.get("numOfToBeProcessedIp") == null ? "0" : statisticsData.get("numOfToBeProcessedIp").toString()));
        } catch (Exception e) {
            log.error("??????????????????IP?????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportSurvivingUnplannedIntranetIpList(@RequestBody SurvivingUnplannedIntranetIpReq param, HttpServletResponse response) {
        log.info("survivingUnplannedIntranetIpReq is {}", param);
        String[] headerList = {"????????????", "IP", "????????????", "???????????????", "???????????????",
                "????????????", "????????????", "?????????", "????????????", "?????????"};
        String[] keyList = {"checkTime", "ip", "deviceIp", "dc", "isNotify", "processingStatus",
                "reason", "operator", "operatingTime", "orderNum"};
        String title = "???????????????IP";
        String fileName = title + ".xlsx";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<SurvivingUnplannedIntranetIpResp> list = survivingUnplannedIntranetIpService.getList(param);
            if (!CollectionUtils.isEmpty(list)) {
                for (SurvivingUnplannedIntranetIpResp entity : list) {
                    Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                    dataLists.add(map);
                }
            }
            os = response.getOutputStream();// ???????????????
            response.setHeader("Content-Disposition",
                    "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setContentType("application/vnd.ms-excel");
            // excel constuct
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, title, headerList, dataLists, keyList);
            book.write(os);
            os.flush();
            log.info("??????/????????????: {} ??????!", fileName);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public ResultVo updateSurvivingUnplannedIntranetIpProcessStatus(@RequestBody IpAuditUpdateRequest request) {
        log.info("ipAuditUpdateRequest request is {}", request);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            dealWithReq(request);
            survivingUnplannedIntranetIpService.updateProcess(request);
        } catch (Exception e) {
            log.error("???????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public IpAuditPageBean<SurvivingUnassignIntranetIpResp> getSurvivingUnassignIntranetIpList(@RequestBody SurvivingUnassignIntranetIpReq req) {
        log.info("survivingUnassignIntranetIpReq is {} ", req);
        IpAuditPageBean<SurvivingUnassignIntranetIpResp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(survivingUnassignIntranetIpService.getList(req));
            int listCount = survivingUnassignIntranetIpService.getListCount(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = survivingUnassignIntranetIpService.getStatisticsData(req);
            respPageBean.setNumOfSurvivingUnassginIp(listCount);
            respPageBean.setNumOfUnrecordIp(listCount);
            respPageBean.setNumOfToBeProcessedIp(Integer.valueOf(statisticsData.get("numOfToBeProcessedIp") == null ? "0" : statisticsData.get("numOfToBeProcessedIp").toString()));
        } catch (Exception e) {
            log.error("??????????????????IP?????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportSurvivingUnassignIntranetIpList(@RequestBody SurvivingUnassignIntranetIpReq param, HttpServletResponse response) {
        log.info("survivingUnassignIntranetIpReq is {}", param);
        String[] headerList = {"????????????", "IP", "VLAN???", "???????????????", "????????????", "????????????",
                "?????????", "????????????", "??????????????????", "??????????????????", "????????????CMDB", "?????????????????????",
                "????????????", "????????????", "?????????", "????????????"};
        String[] keyList = {"checkTime", "ip", "vlanNumber", "dc", "networkSegmentType", "networkSegmentSubType",
                "safeRegion", "ipUse", "firstBusinessSystem", "aloneBusinessSystem", "isCmdbManager",
                "onlineBusiness", "processingStatus", "reason", "operator", "operatingTime"};
        String title = "???????????????IP";
        String fileName = title + ".xlsx";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<SurvivingUnassignIntranetIpResp> list = survivingUnassignIntranetIpService.getList(param);
            if (!CollectionUtils.isEmpty(list)) {
                for (SurvivingUnassignIntranetIpResp entity : list) {
                    Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                    dataLists.add(map);
                }
            }
            os = response.getOutputStream();// ???????????????
            response.setHeader("Content-Disposition",
                    "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setContentType("application/vnd.ms-excel");
            // excel constuct
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, title, headerList, dataLists, keyList);
            book.write(os);
            os.flush();
            log.info("??????/????????????: {} ??????!", fileName);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public ResultVo updateSurvivingUnassignIntranetIpProcessStatus(@RequestBody IpAuditUpdateRequest request) {
        log.info("ipAuditUpdateRequest request is {}", request);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            dealWithReq(request);
            survivingUnassignIntranetIpService.updateProcess(request);
        } catch (Exception e) {
            log.error("???????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public IpAuditPageBean<RegistrationExpiredIpResp> getRegistrationExpiredIpList(@RequestBody RegistrationExpiredIpReq req) {
        log.info("registrationExpiredIpReq is {} ", req);
        IpAuditPageBean<RegistrationExpiredIpResp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(registrationExpiredService.getList(req));
            int listCount = registrationExpiredService.getListCount(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = registrationExpiredService.getStatisticsData(req);
            respPageBean.setNumOfUnregistration(listCount);
            respPageBean.setNumOfToBeProcessedIp(Integer.valueOf(statisticsData.get("numOfToBeProcessedIp") == null ? "0" : statisticsData.get("numOfToBeProcessedIp").toString()));
        } catch (Exception e) {
            log.error("??????????????????IP?????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportRegistrationExpiredIpList(@RequestBody RegistrationExpiredIpReq param, HttpServletResponse response) {
        log.info("registrationExpiredIpReq is {}", param);
        String[] headerList = {"????????????", "IP", "???????????????", "????????????", "????????????",
                "?????????", "????????????", "??????????????????", "??????????????????", "????????????CMDB", "?????????????????????", "?????????", "????????????",
                "?????????", "????????????", "????????????", "?????????", "????????????"};
        String[] keyList = {"checkTime", "ip", "dc", "networkSegmentType", "networkSegmentSubType", "safeRegion",
                "ipUse", "firstBusinessSystem", "aloneBusinessSystem", "isCmdbManager", "onlineBusiness", "requestPerson",
                "requestTime", "expirationDate", "processingStatus", "reason", "operator", "operatingTime"};
        String title = "???????????????IP";
        String fileName = title + ".xlsx";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<RegistrationExpiredIpResp> list = registrationExpiredService.getList(param);
            if (!CollectionUtils.isEmpty(list)) {
                for (RegistrationExpiredIpResp entity : list) {
                    Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                    dataLists.add(map);
                }
            }
            os = response.getOutputStream();// ???????????????
            response.setHeader("Content-Disposition",
                    "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setContentType("application/vnd.ms-excel");
            // excel constuct
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, title, headerList, dataLists, keyList);
            book.write(os);
            os.flush();
            log.info("??????/????????????: {} ??????!", fileName);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public ResultVo updateRegistrationExpiredIpProcessStatus(@RequestBody IpAuditUpdateRequest request) {
        log.info("ipAuditUpdateRequest request is {}", request);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            dealWithReq(request);
            registrationExpiredService.updateProcess(request);
        } catch (Exception e) {
            log.error("???????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    //============================ ??????IP =================================
    @Override
    public void generateAuditUnPlanData4PublicIp() {
        publicIpUnPlanService.generateAuditData();
    }

    @Override
    public void generateAuditUnassignData4PublicIp() {
        publicIpUnAssignService.generateAuditData();
    }

    @Override
    public IpAuditPageBean<IpAuditCommonResp> getUnPlanPublicIpList(@RequestBody IpAuditCommonReq req) {
        IpAuditPageBean<IpAuditCommonResp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(publicIpUnPlanService.getList(req));
            int listCount = publicIpUnPlanService.getListCount(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = publicIpUnPlanService.getStatisticsData(req);
            respPageBean.setNumOfUnPlanIp(listCount);
            respPageBean.setNumOfToBeProcessedIp(Integer.valueOf(statisticsData.get("numOfToBeProcessedIp").toString()));
            respPageBean.setNumOfNotProcessIp(Integer.valueOf(statisticsData.get("numOfNotProcessIp").toString()));
        } catch (NumberFormatException e) {
            log.error("?????????????????????IP?????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportUnPlanPublicIpList(@RequestBody IpAuditCommonReq param, HttpServletResponse response) {
        String[] headerList = {"????????????", "IP","????????????IP","???????????????","???????????????","????????????","????????????","?????????","????????????","?????????"};
        String[] keyList = {"checkTime","ip","deviceIp","dc","isNotify","processingStatusDesc","reason","operator","operatingTime","orderNum"};
        String title = "?????????????????????IP";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<IpAuditCommonResp> list = publicIpUnPlanService.getList(param);
            for (IpAuditCommonResp entity : list) {
                Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                dataLists.add(map);
            }
            os = response.getOutputStream();// ???????????????
            exportExcel(os,response,title,headerList,keyList,dataLists);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public IpAuditPageBean<UnassignPublicIpResp> getUnAssignPublicIpList(@RequestBody UnassignPublicIpReq req) {
        IpAuditPageBean<UnassignPublicIpResp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(publicIpUnAssignService.getList(req));
            int listCount = publicIpUnAssignService.getListCount(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = publicIpUnAssignService.getStatisticsData(req);
            respPageBean.setNumOfUnPlanIp(listCount);
            respPageBean.setNumOfToBeProcessedIp(Integer.valueOf(statisticsData.get("numOfToBeProcessedIp").toString()));
            respPageBean.setNumOfNotProcessIp(Integer.valueOf(statisticsData.get("numOfNotProcessIp").toString()));
        } catch (NumberFormatException e) {
            log.error("?????????????????????IP?????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportUnAssignPublicIpList(@RequestBody UnassignPublicIpReq param, HttpServletResponse response) {
        String[] headerList = {"????????????", "IP","????????????","???????????????","?????????????????????","????????????","??????????????????","??????????????????","????????????","????????????","?????????","????????????"};
        String[] keyList = {"checkTime","ip","networkName","belongIdc","idcManageFlag","circuitType","businessName1","businessName2","processingStatus","reason","operator","operatingTime"};
        String title = "?????????????????????IP";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<UnassignPublicIpResp> list = publicIpUnAssignService.getList(param);
            for (IpAuditCommonResp entity : list) {
                Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                dataLists.add(map);
            }
            os = response.getOutputStream();// ???????????????
            exportExcel(os,response,title,headerList,keyList,dataLists);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public ResultVo updatePublicIpProcessStatus(@RequestBody IpAuditUpdateRequest request) {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            dealWithReq(request);
            publicIpUnAssignService.updateProcess(request);
        } catch (Exception e) {
            log.error("???????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public IpAuditPageBean<RegistrationExpiredPublicIpResp> getRegistrationExpiredPublicIpList(@RequestBody RegistrationExpiredPublicIpReq req) {
        IpAuditPageBean<RegistrationExpiredPublicIpResp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(registrationExpiredService.listByEntity4PublicIp(req));
            int listCount = registrationExpiredService.getListCount4PublicIp(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = registrationExpiredService.getStatisticsData4PublicIp(req);
            respPageBean.setNumOfUnPlanIp(listCount);
            respPageBean.setNumOfToBeProcessedIp(Integer.valueOf(statisticsData.get("numOfToBeProcessedIp").toString()));
            respPageBean.setNumOfNotProcessIp(Integer.valueOf(statisticsData.get("numOfNotProcessIp").toString()));
        } catch (NumberFormatException e) {
            log.error("?????????????????????IP?????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportRegistrationExpiredPublicIpList(@RequestBody RegistrationExpiredPublicIpReq param, HttpServletResponse response) {
        String[] headerList = {"????????????", "IP","????????????","???????????????","?????????????????????","????????????","??????????????????","??????????????????","????????????","?????????","????????????","?????????","????????????","?????????","????????????"};
        String[] keyList = {"checkTime","ip","networkName","belongIdc","idcManageFlag","circuitType","businessName1","businessName2","processingStatus","requestPerson","requestTime","expirationDate","reason","operator","operatingTime"};
        String title = "?????????????????????IP";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<RegistrationExpiredPublicIpResp> list = registrationExpiredService.listByEntity4PublicIp(param);
            for (IpAuditCommonResp entity : list) {
                Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                dataLists.add(map);
            }
            os = response.getOutputStream();// ???????????????
            exportExcel(os,response,title,headerList,keyList,dataLists);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    //============================ IPv6 =================================
    @Override
    public void generateAuditUnPlanData4Ipv6() {
        ipv6UnPlanService.generateAuditData();
    }

    @Override
    public void generateAuditUnassignData4Ipv6() {
        ipv6UnAssignService.generateAuditData();
    }

    @Override
    public IpAuditPageBean<IpAuditCommonResp> getUnPlanIPv6List(@RequestBody IpAuditCommonReq req) {
        log.info("IPv6?????????????????????_UnPlanIPv6Req:{}",req);
        IpAuditPageBean<IpAuditCommonResp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(ipv6UnPlanService.getList(req));
            int listCount = ipv6UnPlanService.getListCount(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = ipv6UnPlanService.getStatisticsData(req);
            respPageBean.setNumOfUnPlanIp(listCount);
            respPageBean.setNumOfToBeProcessedIp(Integer.valueOf(statisticsData.get("numOfToBeProcessedIp").toString()));
            respPageBean.setNumOfNotProcessIp(Integer.valueOf(statisticsData.get("numOfNotProcessIp").toString()));
        } catch (NumberFormatException e) {
            log.error("???????????????IPv6?????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportUnPlanIPv6List(@RequestBody IpAuditCommonReq param, HttpServletResponse response) {
        log.info("IPv6???????????????_UnPlanIPv6Req:{}",param);
        String[] headerList = {"????????????", "IP","????????????IP","???????????????","???????????????","????????????","????????????","?????????","????????????","?????????"};
        String[] keyList = {"checkTime","ip","deviceIp","dc","isNotify","processingStatusDesc","reason","operator","operatingTime","orderNum"};
        String title = "???????????????IPv6";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<IpAuditCommonResp> list = ipv6UnPlanService.getList(param);
            for (IpAuditCommonResp entity : list) {
                Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                dataLists.add(map);
            }
            os = response.getOutputStream();// ???????????????
            exportExcel(os,response,title,headerList,keyList,dataLists);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public IpAuditPageBean<UnassignIpV6Resp> getUnAssignIPv6List(@RequestBody UnassignIpV6Req req) {
        IpAuditPageBean<UnassignIpV6Resp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(ipv6UnAssignService.getList(req));
            int listCount = ipv6UnAssignService.getListCount(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = ipv6UnAssignService.getStatisticsData(req);
            respPageBean.setNumOfUnPlanIp(listCount);
            respPageBean.setNumOfToBeProcessedIp(Integer.valueOf(statisticsData.get("numOfToBeProcessedIp").toString()));
            respPageBean.setNumOfNotProcessIp(Integer.valueOf(statisticsData.get("numOfNotProcessIp").toString()));
        } catch (NumberFormatException e) {
            log.error("???????????????IPv6?????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportUnAssignIPv6List(@RequestBody UnassignIpV6Req param, HttpServletResponse response) {
        log.info("IPv6???????????????_UnPlanIPv6Req:{}",param);
        String[] headerList = {"????????????", "IP","????????????","???????????????","?????????????????????","????????????","????????????","????????????","??????????????????","??????????????????","????????????CMDB","?????????????????????","????????????","????????????","?????????","????????????"};
        String[] keyList = {"checkTime","ip","networkName","belongIdc","idcManageFlag","networkType","networkSub","addressUse","businessName1","businessName2","cmdbManageFlag","businessNameUse","processingStatus","reason","operator","operatingTime"};
        String title = "???????????????IPv6";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<UnassignIpV6Resp> list = ipv6UnAssignService.getList(param);
            for (IpAuditCommonResp entity : list) {
                Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                dataLists.add(map);
            }
            os = response.getOutputStream();// ???????????????
            exportExcel(os,response,title,headerList,keyList,dataLists);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public ResultVo updateIPv6ProcessStatus(@RequestBody IpAuditUpdateRequest request) {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            dealWithReq(request);
            ipv6UnAssignService.updateProcess(request);
        } catch (Exception e) {
            log.error("???????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public IpAuditPageBean<RegistrationExpiredIpv6Resp> getRegistrationExpiredIpv6List(@RequestBody RegistrationExpiredIpv6Req req) {
        IpAuditPageBean<RegistrationExpiredIpv6Resp> respPageBean = new IpAuditPageBean<>();
        try {
            dealWithReq(req);
            respPageBean.setResult(registrationExpiredService.listByEntity4Ipv6(req));
            int listCount = registrationExpiredService.getListCount4Ipv6(req);
            respPageBean.setCount(listCount);
            Map<String, Object> statisticsData = registrationExpiredService.getStatisticsData4Ipv6(req);
            respPageBean.setNumOfUnPlanIp(listCount);
            respPageBean.setNumOfToBeProcessedIp(Integer.valueOf(statisticsData.get("numOfToBeProcessedIp").toString()));
            respPageBean.setNumOfNotProcessIp(Integer.valueOf(statisticsData.get("numOfNotProcessIp").toString()));
        } catch (NumberFormatException e) {
            log.error("???????????????IPv6?????????????????????", e);
        }
        return respPageBean;
    }

    @Override
    public void exportRegistrationExpiredIpv6List(@RequestBody RegistrationExpiredIpv6Req param, HttpServletResponse response) {
        String[] headerList = {"????????????", "IP","????????????","???????????????","?????????????????????","????????????","????????????","????????????","??????????????????","??????????????????","????????????CMDB","?????????????????????","????????????","?????????","????????????","?????????","????????????","?????????","????????????"};
        String[] keyList = {"checkTime","ip","networkName","belongIdc","idcManageFlag","networkType","networkSub","addressUse","businessName1","businessName2","cmdbManageFlag","businessNameUse","processingStatus","requestPerson","requestTime","expirationDate","reason","operator","operatingTime"};
        String title = "???????????????IPv6";
        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<RegistrationExpiredIpv6Resp> list = registrationExpiredService.listByEntity4Ipv6(param);
            for (IpAuditCommonResp entity : list) {
                Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                dataLists.add(map);
            }
            os = response.getOutputStream();// ???????????????
            exportExcel(os,response,title,headerList,keyList,dataLists);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
    }

    @Override
    public ResultVo getResource(String type, String pid) {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            List<Map<String, String>> dataMap = configDictService.getDictByType(type, pid);
            resultVo.setData(dataMap);
        } catch (Exception e) {
            log.error("????????????????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg("????????????????????????????????????");
        }
        return resultVo;
    }

    @Override
    public ResultVo updateIpRepositoryInfo(@RequestBody IpAuditUpdateRequest param) {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            if (param == null || CollectionUtils.isEmpty(param.getIpIds()) || CollectionUtils.isEmpty(param.getSegmentIds())) {
                throw new RuntimeException("IP?????????ID?????????????????????ID???????????????");
            }
            if (param.getIpIds().stream().allMatch(StringUtils::isEmpty)) {
                throw new RuntimeException("IP?????????ID???????????????");
            }
            if (param.getSegmentIds().stream().allMatch(StringUtils::isEmpty)) {
                throw new RuntimeException("IP???????????????ID???????????????");
            }
            if (StringUtils.isEmpty(param.getOpIpType())) {
                throw new RuntimeException("IP?????????????????????");
            }
            String ipType = param.getOpIpType();
            if (ipType.equals(IpAuditEnum.INNER_IP.getValue())) {
                configDictService.updateIpRepositoryInfo(param);
            } else if (ipType.equals(IpAuditEnum.PUBLIC_IP.getValue())) {
                publicIpUnAssignService.updatePublicInfo(param);
            } else if (ipType.equals(IpAuditEnum.IPV6.getValue())) {
                ipv6UnAssignService.updateIpv6Info(param);
            }
        } catch (Exception e) {
            log.error("IP???????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("IP???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public void doSync(String type) {
        if (StringUtils.isNotEmpty(type)) {
            switch (type) {
                case "1":
                    survivingUnrecordIntranetIpService.generateAuditData();
                    break;
                case "2":
                    recordedUnsurvivingIntranetIpService.generateAuditData();
                    break;
                case "3":
                    assignedUnsurvivingIntranetIpService.generateAuditData();
                    break;
                case "4":
                    survivingUnassignIntranetIpService.generateAuditData();
                    break;
                case "5":
                    survivingUnplannedIntranetIpService.generateAuditData();
                    break;
                case "6":
                    registrationExpiredService.genRegistrationExpiredIpData("??????IP");
                    break;
                case "7":
                    publicIpUnPlanService.generateAuditData();
                    break;
                case "8":
                    publicIpUnAssignService.generateAuditData();
                    break;
                case "9":
                    registrationExpiredService.genRegistrationExpiredIpData("??????IP");
                    break;
                case "10":
                    ipv6UnPlanService.generateAuditData();
                    break;
                case "11":
                    ipv6UnAssignService.generateAuditData();
                    break;
                case "12":
                    registrationExpiredService.genRegistrationExpiredIpData("IPv6");
                    break;
                default:
                    break;
            }
        }
    }

    private void exportExcel(OutputStream os, HttpServletResponse response, String title, String[] headerList, String[] keyList, List<Map<String, Object>> dataLists) throws Exception{
        String fileName = title + ".xlsx";
        response.setHeader("Content-Disposition",
                "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
        response.setHeader("Connection", "close");
        response.setContentType("application/vnd.ms-excel");
        // excel constuct
        ExportExcelUtil eeu = new ExportExcelUtil();
        Workbook book = new SXSSFWorkbook(128);
        eeu.exportExcel(book, 0, title, headerList, dataLists, keyList);
        book.write(os);
        os.flush();
        log.info("??????/????????????: {} ??????!", fileName);
    }
}
