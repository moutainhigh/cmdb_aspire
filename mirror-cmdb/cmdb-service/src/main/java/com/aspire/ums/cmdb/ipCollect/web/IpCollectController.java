package com.aspire.ums.cmdb.ipCollect.web;

import com.aspire.ums.cmdb.common.Result;
import com.aspire.ums.cmdb.common.ResultVo;
import com.aspire.ums.cmdb.deviceStatistic.util.ExportExcelUtil;
import com.aspire.ums.cmdb.ipCollect.IIpCollectAPI;
import com.aspire.ums.cmdb.ipCollect.payload.entity.*;
import com.aspire.ums.cmdb.ipCollect.payload.vmware.BaseInstanceRequest;
import com.aspire.ums.cmdb.ipCollect.payload.vmware.InstanceCreateRequest;
import com.aspire.ums.cmdb.ipCollect.payload.vmware.InstanceDeleteRequest;
import com.aspire.ums.cmdb.ipCollect.payload.vmware.InstanceUpdateRequest;
import com.aspire.ums.cmdb.ipCollect.service.*;
import com.aspire.ums.cmdb.util.DateUtils;
import com.aspire.ums.cmdb.util.JavaBeanUtil;
import com.aspire.ums.cmdb.util.JsonUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @Author: fanshenquan
 * @Datetime: 2020/5/19 18:45
 */
@Slf4j
@RestController
public class IpCollectController implements IIpCollectAPI {
    @Autowired
    private CmdbCollectApiService cmdbCollectApiService;
    @Autowired
    private CmdbIpCollectService cmdbIpCollectService;
    @Autowired
    private CmdbVipCollectService cmdbVipCollectService;
    @Autowired
    private CmdbVmwareInstanceLogService instanceLogService;
    @Autowired
    private VmwareFullSyncApiService vmwareFullSyncApiService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    @Override
    public ResultVo create(@RequestBody InstanceCreateRequest createRequest) {
        log.info(">>>>> ?????????????????????????????????[??????-??????]?????? >>>>");
        ResultVo resultInfo = new ResultVo(true, "????????????!");
        createRequest.setRequestBody(JsonUtil.toJacksonJson(createRequest));
        log.info("???????????????????????????requestBody={}", createRequest.getRequestBody());
        try {
            if (!cmdbCollectApiService.instanceCreate(createRequest)) {
                resultInfo.setSuccess(false);
                resultInfo.setMsg("???????????????");
            } else {
                resultInfo.setSuccess(true);
            }

        } catch (Exception e) {
            log.error("???????????????", e);
            resultInfo.setSuccess(false);
            resultInfo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        log.info("<<<< ????????????????????????[??????-??????]????????????! <<<<<");
        return resultInfo;
    }

    @Override
    public ResultVo update(@RequestBody InstanceUpdateRequest updateRequest) {
        log.info(">>>>> ?????????????????????????????????[??????-??????]?????? >>>>");
        ResultVo resultInfo = new ResultVo(true, "???????????????");
        updateRequest.setRequestBody(JsonUtil.toJacksonJson(updateRequest));
        log.info("???????????????????????????requestBody={}", updateRequest.getRequestBody());
        try {
            if (!cmdbCollectApiService.instanceUpdate(updateRequest)) {
                resultInfo.setSuccess(false);
                resultInfo.setMsg("???????????????");
            } else {
                resultInfo.setSuccess(true);
            }
        } catch (Exception e) {
            log.error("???????????????", e);
            resultInfo.setSuccess(false);
            resultInfo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        log.info("<<<< ????????????????????????[??????-??????]????????????! <<<<<");
        return resultInfo;
    }

    @Override
    public ResultVo delete(@RequestBody InstanceDeleteRequest deleteRequest) {
        log.info(">>>>> ?????????????????????????????????[??????-??????]?????? >>>>");
        ResultVo resultInfo = new ResultVo(true, "???????????????");
        deleteRequest.setRequestBody(JsonUtil.toJacksonJson(deleteRequest));
        log.info("???????????????????????????requestBody={}", deleteRequest.getRequestBody());
        try {
            if (!cmdbCollectApiService.instanceDelete(deleteRequest)) {
                resultInfo.setSuccess(false);
                resultInfo.setMsg("???????????????");
            } else {
                resultInfo.setSuccess(true);
            }
        } catch (Exception e) {
            log.error("???????????????", e);
            resultInfo.setSuccess(false);
            resultInfo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        log.info("<<<< ????????????????????????[??????-??????]????????????! <<<<<");
        return resultInfo;
    }

    @Override
    public ResultVo<List<CmdbIpCollectResponse>> findListS(@RequestBody CmdbIpCollectRequest cmdbIpCollectRequest) {
        log.info("IpCollectController.findList is {}", cmdbIpCollectRequest);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            cmdbIpCollectRequest.updatePageNo();
            cmdbIpCollectRequest.setPageNo(0);
            cmdbIpCollectRequest.setPageSize(0);
            setHisFlag4CmdbIp(cmdbIpCollectRequest);
            resultVo.setData(buildIpPageCondition(cmdbIpCollectRequest));
        } catch (Exception e) {
            log.error("findList ???????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("findList ???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public CmdbIpCollectResult findPageS(@RequestBody CmdbIpCollectRequest cmdbIpCollectRequest) {
        log.info("cmdbIpCollectRequest is {}", cmdbIpCollectRequest);
        CmdbIpCollectResult result = new CmdbIpCollectResult();
        try {
            cmdbIpCollectRequest.updatePageNo();
            setHisFlag4CmdbIp(cmdbIpCollectRequest);
            result.setData(buildIpPageCondition(cmdbIpCollectRequest));
            result.setTotalSize(cmdbIpCollectService.findPageCount(cmdbIpCollectRequest));
            result.setTopTotal(cmdbIpCollectService.findTopTotal(cmdbIpCollectRequest));
        } catch (Exception e) {
            log.error("findPageS ???????????????", e);
        }
        return result;
    }

    @Override
    public ResultVo getResourceS() {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            List<Map> dataMap = cmdbIpCollectService.getResource();
            resultVo.setData(dataMap);
        } catch (Exception e) {
            log.error("??????????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg("??????????????????????????????");
        }
        return resultVo;
    }

    @Override
    public ResultVo getSourceS() {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            List<Map> dataMap = cmdbIpCollectService.getSource();
            resultVo.setData(dataMap);
        } catch (Exception e) {
            log.error("???????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg("???????????????????????????");
        }
        return resultVo;
    }

    @Override
    public void exportS(@RequestBody CmdbIpCollectRequest cmdbIpCollectRequest, HttpServletResponse response) {
        log.info("cmdbIpCollectRequest is {}", cmdbIpCollectRequest);
        String[] headerList = {"????????????", "IP", "MAC??????", "IP??????", "????????????IP", "???????????????"};
        String[] keyList = {"time", "ip", "mac", "iptype", "gateway", "resource"};
        String title = "??????IP??????";
        String fileName = title + ".xlsx";

        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            // List<CmdbIpAddressPoolEntity> entityList = cmdbIpAddressPoolService.findData(cmdbIpCollectRequest);
            // for (CmdbIpAddressPoolEntity entity : entityList) {
            //     Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
            //     map.put("time", sdf.format(entity.getTime()));
            //     dataLists.add(map);
            // }

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
    public ResultVo<List<CmdbVipCollectEntity>> findListF(@RequestBody CmdbVipCollectRequest cmdbVipCollectRequest) {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            cmdbVipCollectRequest.updatePageNo();
            cmdbVipCollectRequest.setPageNo(0);
            cmdbVipCollectRequest.setPageSize(0);
            resultVo.setData(cmdbVipCollectService.findData(cmdbVipCollectRequest));
        } catch (Exception e) {
            log.error("findList ???????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("findList ???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public Result<CmdbVipCollectEntity> findPageF(@RequestBody CmdbVipCollectRequest cmdbVipCollectRequest) {
        log.info("cmdbVipConllecRequest is {}", cmdbVipCollectRequest);
        Result result = new Result();
        try {
            cmdbVipCollectRequest.updatePageNo();
            result.setData(cmdbVipCollectService.findData(cmdbVipCollectRequest));
            result.setTotalSize(cmdbVipCollectService.findDataCount(cmdbVipCollectRequest));
        } catch (Exception e) {
            log.error("findPageF ???????????????", e);
        }
        return result;
    }

    @Override
    public ResultVo getResourceF() {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            List<Map<String, String>> dataMap = cmdbVipCollectService.getResource();
            resultVo.setData(dataMap);
        } catch (Exception e) {
            log.error("getResourceF ??????????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg("getResourceF ??????????????????????????????");
        }
        return resultVo;
    }

    @Override
    public ResultVo getUserTypeF() {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            List<Map<String, String>> dataMap = cmdbVipCollectService.getUseType();
            resultVo.setData(dataMap);
        } catch (Exception e) {
            log.error("getUserTypeF ?????????????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg("getUserTypeF ?????????????????????????????????");
        }
        return resultVo;
    }

    @Override
    public void exportF(@RequestBody CmdbVipCollectRequest cmdbVipCollectRequest, HttpServletResponse response) {
        log.info("cmdbVipCollectRequest is {}", cmdbVipCollectRequest);
        String[] headerList = {"????????????", "??????IP", "????????????IP", "??????IP??????", "??????IP????????????", "???????????????"};
        String[] keyList = {"time", "vip", "bindip", "iplist", "usetype", "resource"};
        String title = "??????IP??????";
        String fileName = title + ".xlsx";

        OutputStream os = null;
        List<Map<String, Object>> dataLists = Lists.newArrayList();
        try {
            List<CmdbVipCollectEntity> entityList = cmdbVipCollectService.findData(cmdbVipCollectRequest);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (CmdbVipCollectEntity entity : entityList) {
                Map<String, Object> map = JavaBeanUtil.convertBeanToMap(entity);
                map.put("time", sdf.format(entity.getTime()));
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

    /**
     * ??????????????????
     *
     * @param request
     */
    private void saveLog(BaseInstanceRequest request) {
        executorService.execute(() -> instanceLogService.saveInstanceLog(request));
    }

    @Override
    public ResultVo updateIpInfoByTask() {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            if ("Y".equals(cmdbIpCollectService.getIpUpdateConfig("1"))) {
                cmdbIpCollectService.updateCmdbAssetAllIpInfo();
            }
            if ("Y".equals(cmdbIpCollectService.getIpUpdateConfig("2"))) {
                cmdbIpCollectService.updatePublicIpInfo();
            }
            if ("Y".equals(cmdbIpCollectService.getIpUpdateConfig("3"))) {
                cmdbIpCollectService.updateIpv6Info();
            }
            if ("Y".equals(cmdbIpCollectService.getIpUpdateConfig("4"))) {
                cmdbIpCollectService.updateIpBusinessByAsset();
            }
            if ("Y".equals(cmdbIpCollectService.getIpUpdateConfig("5"))) {
                vmwareFullSyncApiService.syncIpAddressPool();
            }
            if ("Y".equals(cmdbIpCollectService.getIpUpdateConfig("6"))) {
                vmwareFullSyncApiService.syncIpConfPool();
            }
            if ("Y".equals(cmdbIpCollectService.getIpUpdateConfig("7"))) {
                vmwareFullSyncApiService.syncIpArpPool();
            }
            if ("Y".equals(cmdbIpCollectService.getIpUpdateConfig("8"))) {
                cmdbIpCollectService.updateFirstSurvivalTime4IpInfo();
            }
            if ("Y".equals(cmdbIpCollectService.getIpUpdateConfig("9"))) {
                cmdbIpCollectService.buildAndSaveIpClashList4Now();
                cmdbIpCollectService.updateCmdbAssetSurvialInfo();
            }
        } catch (Exception e) {
            log.error("??????????????????",e);
            resultVo.setSuccess(false);
            resultVo.setMsg("??????????????????" + e.getMessage());
        }
        return resultVo;
    }

    /**
     * ????????????IP???????????????
     * @param cmdbIpCollectRequest ????????????
     */
    private List<CmdbIpCollectResponse> buildIpPageCondition(CmdbIpCollectRequest cmdbIpCollectRequest) {
        List<CmdbIpCollectResponse> pageList;
        if("ip_remove_repetition".equals(cmdbIpCollectRequest.getSource())) {
            cmdbIpCollectRequest.setOneIpFlag("???");
            cmdbIpCollectRequest.setSource("");
            pageList = cmdbIpCollectService.findPage(cmdbIpCollectRequest);
        } else {
            pageList = cmdbIpCollectService.findPage(cmdbIpCollectRequest);
        }
        return buildIpPageList(pageList);
    }

    /**
     * ???????????????IP???????????????
     * @param pageList ??????IP??????
     */
    private List<CmdbIpCollectResponse> buildIpPageList(List<CmdbIpCollectResponse> pageList) {
        List<CmdbIpCollectResponse> retList = new ArrayList<>();
        Map<String, String> configMap = cmdbIpCollectService.buildConfig4Map("cmdbIpCollectSourceType");
        if (pageList.isEmpty()) {
            return retList;
        }
        pageList.forEach(map -> {
            String source = map.getSource();
            String[] split = source.split(",");
            if (split.length == 1) {
                map.setSource(configMap.get(split[0]));
            } else {
                Set<String> tempSet = new HashSet<>();
                for (String s : split) {
                    tempSet.add(configMap.get(s));
                }
                ArrayList<String> tempList = new ArrayList<>(tempSet);
                String join = String.join(";", tempList);
                map.setSource(join);
            }
            retList.add(map);
        });
        return retList;
    }

    /**
     * ???????????????????????????????????????????????????????????????
     * @param cmdbIpCollectRequest ??????IP????????????
     */
    private void setHisFlag4CmdbIp(CmdbIpCollectRequest cmdbIpCollectRequest) {
        Date startTime = cmdbIpCollectRequest.getStartTime();
        Date endTime = cmdbIpCollectRequest.getEndTime();
        if(null == startTime || null == endTime ) {
            cmdbIpCollectRequest.setHisFlag("1");
            return;
        }
        try {
            String startTimeStr = DateUtils.toDateH(startTime);
            String endTimeStr = DateUtils.toDateH(endTime);

            String nowDate = DateUtils.getDateTimeStr();
            String subDate = nowDate.substring(0,10);
            String nowDateStart = DateUtils.startDateH(subDate);
            String nowDateEnd = DateUtils.endDateH(subDate);
            if (nowDateStart.equals(startTimeStr) && nowDateEnd.equals(endTimeStr)) {
                return;
            }

            if (!DateUtils.isToDay(startTime)) {
                cmdbIpCollectRequest.setHisFlag("1");
                return;
            }
            if (!DateUtils.isToDay(endTime)) {
                cmdbIpCollectRequest.setHisFlag("1");
            }
        } catch (Exception e) {
            cmdbIpCollectRequest.setHisFlag("1");
            log.error("??????IP????????????????????????",e);
        }
    }
}
