package com.migu.tsg.microservice.atomicservice.composite.controller.cmdb.index;

import com.alibaba.fastjson.JSONObject;
import com.aspire.mirror.composite.service.cmdb.index.IItCloudScreenAPI;
import com.aspire.ums.cmdb.index.payload.ItCloudScreenRequest;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.cmdb.index.ItCloudScreenClient;
import com.migu.tsg.microservice.atomicservice.composite.common.excel2pdf.POIModuleUtils;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.ExportExcelUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @ClassName ItCloudScreenController
 * @Description
 * @Author luowenbo
 * @Date 2020/2/27 14:11
 * @Version 1.0
 */
@RestController
@Slf4j
public class ItCloudScreenController implements IItCloudScreenAPI {

    @Autowired
    private ItCloudScreenClient itCloudScreenClient;

    @Autowired
    private POIModuleUtils poiModuleUtils;

    @Override
    public Map<String, Object> getResourceAllocateList(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getResourceAllocateList(req);
    }

    @Override
    public List<Map<String, Object>> getResourceAllocateByBizSystem(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getResourceAllocateByBizSystem(req);
    }

    @Override
    public List<Map<String, Object>> getBizSystemNotInpect(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getBizSystemNotInpect(req);
    }

    @Override
    public List<Map<String, Object>> getMaxUtilizationByMonth(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getMaxUtilizationByMonth(req);
    }

    @Override
    public List<Map<String, Object>> getMaxUtilizationByBizSystem(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getMaxUtilizationByBizSystem(req);
    }

    @Override
    public List<Map<String, Object>> getAvgUtilizationByMonth(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getAvgUtilizationByMonth(req);
    }

    @Override
    public List<Map<String, Object>> getAvgUtilizationByBizSystem(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getAvgUtilizationByBizSystem(req);
    }

    @Override
    public Map<String, Object> getMonthMaxUtilization(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getMonthMaxUtilization(req);
    }

    @Override
    public Map<String, Object> getMonthAvgUtilization(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getMonthAvgUtilization(req);
    }

    @Override
    public Map<String,String> getBizSystemCount(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getBizSystemCount(req);
    }

    @Override
    public Map<String, String> getPmBizSystemCount(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getPmBizSystemCount(req);
    }

    @Override
    public List<Map<String, Object>> getBizSystemListWithIdcType(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getBizSystemListWithIdcType(req);
    }

    @Override
    public Map<String, String> getVmBizSystemCount(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getVmBizSystemCount(req);
    }

    @Override
    public Map<String, String> getStoreBizSystemCount(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getStoreBizSystemCount(req);
    }

    @Override
    public List<Map<String, Object>> getStoreBizSystemListWithIdcType(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getStoreBizSystemListWithIdcType(req);
    }

    @Override
    public Map<String, Object> judgeStatus(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.judgeStatus(req);
    }

    @Override
    public JSONObject exportAllInstanceData(@RequestBody ItCloudScreenRequest req, HttpServletResponse response) {
        JSONObject jsonObject = new JSONObject();
        try {
            List<Map<String, Object>> list = itCloudScreenClient.exportAllInstanceData(req);
            String fileName = "instanceDetialInfo.xlsx";
            response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode
                    (fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setHeader("Content-Type", "application/vnd.ms-excel");
            String[] header = new String[] {"????????????","????????????","????????????","??????IP??????","??????IP","???????????????","????????????",
                    "??????????????????","??????????????????","???????????????","??????????????????","????????????","????????????","????????????","POD??????","????????????"};
            String[] keys = new String[] {"department1","department2","bizSystem","ip","other_ip","idcType","device_type",
                    "insert_time","inspect_time","VM_type","device_os_type","device_mfrs","device_model","project_name","pod_name","roomId"};
            OutputStream os = response.getOutputStream();// ???????????????
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, "????????????", header, list, keys);
            book.write(os);
            os.flush();
            os.close();
            jsonObject.put("flag", "success");
        } catch (Exception e) {
            jsonObject.put("flag", "error");
            jsonObject.put("msg", e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return jsonObject;
    }

    @Override
    public JSONObject partDataListExport(@RequestBody ItCloudScreenRequest req, HttpServletResponse response) {
        JSONObject jsonObject = new JSONObject();
        try {
            List<Map<String, Object>> list = itCloudScreenClient.exportPartInstanceData(req);
            String fileName = "instanceDetialInfo.xlsx";
            response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode
                    (fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setHeader("Content-Type", "application/vnd.ms-excel");
            String[] header = null;
            String[] keys = null;
            if(req.getModuleFlag() != null) {
                String deviceType = req.getDeviceType() + "??????????????????????????????";
                header = new String[] {"????????????","?????????","???????????????","????????????","????????????","?????????","POD???",deviceType,
                        "??????CPU?????????????????????","???????????????????????????","??????CPU?????????????????????","???????????????????????????"};
                keys = new String[] {"biz_system","business_concat","business_concat_phone","department2","department1",
                        "resource_pool","pod","total","cpuAvg","storeAvg","cpuMax","storeMax"};
            } else {
                header = new String[] {"????????????","?????????","???????????????","????????????","????????????","?????????","POD???",
                        "FCSAN?????????????????????","IPSAN?????????????????????","??????????????????????????????","?????????????????????????????????",
                        "?????????????????????????????????","???????????????????????????????????????"};
                keys = new String[] {"biz_system","business_concat","business_concat_phone","department2","department1",
                        "resource_pool","pod","fcsan","ipsan","kcc","bfcc","wjcc","dxcc"};
            }
            OutputStream os = response.getOutputStream();// ???????????????
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, "????????????", header, list, keys);
            book.write(os);
            os.flush();
            os.close();
            jsonObject.put("flag", "success");
        } catch (Exception e) {
            jsonObject.put("flag", "error");
            jsonObject.put("msg", e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return jsonObject;
    }

    @Override
    public Map<String, Object> getCheckScoreList(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getCheckScoreList(req);
    }

    @Override
    public LinkedHashMap<String, Object> getSpecificDeviceByBz(@RequestBody ItCloudScreenRequest req) {
        return itCloudScreenClient.getSpecificDeviceByBz(req);
    }

    @Override
    public Map<String, String> exportSpecificDeviceByBz(@RequestBody ItCloudScreenRequest req) {
        Map<String, String> returnMap = new HashMap<>();
        String fileName = "???????????????.xlsx";
        HttpServletResponse response = ((ServletRequestAttributes)RequestContextHolder.getRequestAttributes()).getResponse();
        try {
            response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        response.setHeader("Connection", "close");
        response.setHeader("Content-Type", "application/vnd.ms-excel");
        String bizSystem = req.getBizSystem();
        List<String> keyList = Arrays.asList("bizSystem","deviceName","idcType","ip","serviceIp","cpu","memory");
        LinkedHashMap<String, String> headerMap = new LinkedHashMap<>();
        headerMap.put("bizSystem", "????????????");
        headerMap.put("deviceName", "????????????");
        headerMap.put("idcType", "?????????");
        headerMap.put("ip", "??????ip");
        headerMap.put("serviceIp", "??????ip");
        List<List<String>> titleLists = new ArrayList<>();
        List<LinkedHashMap<String, Object>> dataList = (List<LinkedHashMap<String, Object>>)itCloudScreenClient.getSpecificDeviceByBz(req).get("data");
        List<LinkedHashMap<String, Object>> resultList = new ArrayList<>();
        List<String> firstList = new ArrayList<>();
        List<String> secondList = new ArrayList<>();
        firstList.addAll(headerMap.values());
        secondList.addAll(headerMap.values());
//        headerMap.put("cpu", "CPU");
//        headerMap.put("memory", "??????");
         // ?????????key?????????datedisplay -- 1??????2???...
        if (dataList == null || dataList.size() == 0) {
            returnMap.put("code", "error");
            returnMap.put("message", "???????????????????????????");
            return returnMap;
        }
        for (String firstKey : dataList.get(0).keySet()) {
            if (!keyList.contains(firstKey) && !firstKey.equals("resourceId")) {
                Map<String, String> hasSecond = (Map<String, String>)dataList.get(0).get(firstKey);
                firstList.add(hasSecond.get("dateDisplay"));
                firstList.add(hasSecond.get("dateDisplay"));
                secondList.add("CPU");
                secondList.add("??????");
            }
        }
        for (Map<String, Object> data : dataList) {
            LinkedHashMap<String, Object> dataMap = new LinkedHashMap<>();
            for (String key : data.keySet()) {
                if (headerMap.containsKey(key)) {
                    dataMap.put(headerMap.get(key), data.get(key) == null ? "-" : data.get(key));
                } else if(!key.equals("resourceId")) {
                    Map<String, String> listMap = (Map<String, String>)data.get(key);
                    Map<String, String> tempTistMap = new HashMap<>();
                    tempTistMap.put("CPU", listMap.get("cpu"));
                    tempTistMap.put("??????", listMap.get("memory"));
                    dataMap.put(listMap.get("dateDisplay"), tempTistMap);
                }
                dataMap.put("????????????", bizSystem);
            }
            resultList.add(dataMap);
        }
        titleLists.add(firstList);
        titleLists.add(secondList);
        ExportExcelUtil eeu = new ExportExcelUtil();
        Workbook book = new SXSSFWorkbook(128);
        try {
            OutputStream os = response.getOutputStream();// ???????????????
            eeu.exportExcel(book, 0, "test" ,titleLists, resultList, null);
            book.write(os);
            os.flush();
            os.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("??????Excel????????????!", e);
            returnMap.put("code", "error");
            returnMap.put("message", e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        returnMap.put("code", "success");
        response.setStatus(HttpStatus.NO_CONTENT.value());
        return returnMap;
    }
}
