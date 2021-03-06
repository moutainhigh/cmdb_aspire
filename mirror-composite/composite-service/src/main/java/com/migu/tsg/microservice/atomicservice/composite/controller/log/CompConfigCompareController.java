package com.migu.tsg.microservice.atomicservice.composite.controller.log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aspire.mirror.common.entity.PageResponse;
import com.aspire.mirror.log.api.dto.ConfigCompareLogsResp;
import com.aspire.mirror.log.api.dto.ConfigCompareReq;
import com.aspire.mirror.log.api.dto.ConfigCompareResp;
import com.google.common.collect.Maps;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.log.IConfigCompareServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.common.StringUtils;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LastLogCodeEnum;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.ExportExcelUtil;
import com.migu.tsg.microservice.atomicservice.composite.exception.BaseException;
import com.migu.tsg.microservice.atomicservice.composite.exception.ResultErrorEnum;
import com.migu.tsg.microservice.atomicservice.composite.service.logs.ICompConfigCompareService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resources;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author baiwenping
 * @Title: CompConfigCompareController
 * @Package com.migu.tsg.microservice.atomicservice.composite.controller.log
 * @Description: TODO
 * @date 2020/12/24 11:10
 */
@Slf4j
@RestController
public class CompConfigCompareController implements ICompConfigCompareService {
    @Autowired
    private IConfigCompareServiceClient configCompareServiceClient;

    private static final String COMPARE_TEMPLATE_PATH = "/download/compare_template.xlsx";
    /**
     * ??????????????????
     *
     * @param masterIp
     * @param backupIp
     * @param compareTimeFrom
     * @param compareTimeTo
     * @param compareType
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public PageResponse<ConfigCompareResp> getCompareList(@RequestParam(name = "masterIp", required = false) String masterIp,
                                                          @RequestParam(name = "backupIp", required = false) String backupIp,
                                                          @RequestParam(name = "compareTimeFrom", required = false) String compareTimeFrom,
                                                          @RequestParam(name = "compareTimeTo", required = false) String compareTimeTo,
                                                          @RequestParam(name = "compareType", required = false) String compareType,
                                                          @RequestParam(name = "pageNum", defaultValue = "1") Integer pageNum,
                                                          @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize) {
        return configCompareServiceClient.getCompareList(masterIp, backupIp, compareTimeFrom, compareTimeTo, compareType, pageNum, pageSize);
    }

    /**
     * ??????????????????
     *
     * @param ids
     * @return
     */
    @Override
    public void exportCompareList(@RequestBody List<Integer> ids) {
        List<ConfigCompareResp> list = configCompareServiceClient.exportCompareList(ids);
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = servletRequestAttributes.getResponse();

        String[] headerList = {"?????????","?????????IP","?????????IP","??????","???????????????","???????????????","???????????????","??????????????????"};
        String[] keyList = {"idcType","masterIp","backupIp","brand","addCount","modifyCount","delCount","compareTime"};
        String title = "??????????????????";
        String fileName = title+".xlsx";

        try {
            List<Map<String, Object>> dataLists = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (ConfigCompareResp configCompareResp : list) {
                Map<String, Object>  map= ExportExcelUtil.objectToMap(configCompareResp);
                if (configCompareResp.getCompareTime() != null) {
                    map.put("compareTime",  sdf.format(configCompareResp.getCompareTime()));
                }
                dataLists.add(map);
            }
            OutputStream os = response.getOutputStream();// ???????????????
            response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setHeader("Content-Type", "application/vnd.ms-excel");
            //excel constuct
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, title, headerList, dataLists, keyList);
            book.write(os);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        }
    }

    /**
     *
     */
    public void downloadTemplate() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = servletRequestAttributes.getResponse();
        String fileName = "compare_template.xlsx";
        InputStream input = null;
        OutputStream out = null;
        try {
            response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setHeader("Content-Type", "application/vnd.ms-excel");
            //excel constuct
//            input = this.getClass().getResourceAsStream(COMPARE_TEMPLATE_PATH);
            input = new ClassPathResource(COMPARE_TEMPLATE_PATH).getInputStream();
//            input = new FileInputStream(filePath1);
            out = response.getOutputStream();
            byte[] b = new byte[2048];
            int len;
            while ((len = input.read(b)) != -1) {
                out.write(b, 0, len);
            }
            //?????? Excel??????xxx.xlsx?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????"???"
//            response.setHeader("Content-Length", String.valueOf(input.getChannel().size()));
            input.close();
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * ??????????????????
     *
     * @param ids
     * @return
     */
    @Override
    public void exportCompareDetailList(@RequestBody List<Integer> ids) {
        List<ConfigCompareResp> list = configCompareServiceClient.exportCompareList(ids);
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = servletRequestAttributes.getResponse();

        String[] headerList = {"?????????","?????????IP","?????????IP","??????","????????????","???????????????","???????????????","???????????????","???????????????","?????????","??????????????????"};
        String[] keyList = {"idcType","masterIp","backupIp","brand","compareType","master_name","master_content","backup_name","backup_content","compare_result","compareTime"};
        String title = "??????????????????";
        String fileName = title+".xlsx";

        try {
            List<Map<String, Object>> dataLists = new ArrayList<Map<String,Object>>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (ConfigCompareResp configCompareResp : list) {
                Map<String, Object>  compareMap= ExportExcelUtil.objectToMap(configCompareResp);
                if (configCompareResp.getCompareTime() != null) {
                    compareMap.put("compareTime",  sdf.format(configCompareResp.getCompareTime()));
                }

                // ???????????????
                String addDatas = configCompareResp.getAddDatas();
                Integer addCount = configCompareResp.getAddCount();
                if (!StringUtils.isEmpty(addDatas) && addCount != null && addCount > 0) {
                    try {
                        JSONArray array = JSON.parseArray(addDatas);
                        for (int i = 0; i < array.size(); i++) {
                            JSONObject jsonObject = array.getJSONObject(i);
                            Map<String, Object>  map = Maps.newHashMap();
                            map.putAll(jsonObject);
                            String backupContent = jsonObject.getString("backup_content");
                            if (!StringUtils.isEmpty(backupContent) && backupContent.indexOf(",") > -1) {
                                jsonObject.put("backup_content",backupContent.replaceAll(",","\n"));
                            }
                            String masterContent = jsonObject.getString("master_content");
                            if (!StringUtils.isEmpty(masterContent) && masterContent.indexOf(",") > -1) {
                                jsonObject.put("master_content",masterContent.replaceAll(",","\n"));
                            }
                            map.putAll(compareMap);
                            map.put("compareType", "???????????????");
                            dataLists.add(map);
                        }
                    } catch (Exception e) {

                    }
                }

                // ???????????????
                String modifyDatas = configCompareResp.getModifyDatas();
                Integer modifyCount = configCompareResp.getModifyCount();
                if (!StringUtils.isEmpty(modifyDatas) && modifyCount != null && modifyCount > 0) {
                    try {
                        JSONArray array = JSON.parseArray(modifyDatas);
                        for (int i = 0; i < array.size(); i++) {
                            JSONObject jsonObject = array.getJSONObject(i);
                            String backupContent = jsonObject.getString("backup_content");
                            if (!StringUtils.isEmpty(backupContent) && backupContent.indexOf(",") > -1) {
                                jsonObject.put("backup_content",backupContent.replaceAll(",","\n"));
                            }
                            String masterContent = jsonObject.getString("master_content");
                            if (!StringUtils.isEmpty(masterContent) && masterContent.indexOf(",") > -1) {
                                jsonObject.put("master_content",masterContent.replaceAll(",","\n"));
                            }
                            Map<String, Object>  map = Maps.newHashMap();
                            map.putAll(jsonObject);
                            map.putAll(compareMap);
                            map.put("compareType", "???????????????");
                            dataLists.add(map);
                        }
                    } catch (Exception e) {

                    }
                }

                // ???????????????
                String delDatas = configCompareResp.getDelDatas();
                Integer delCount = configCompareResp.getDelCount();
                if (!StringUtils.isEmpty(delDatas) && delCount != null && delCount > 0) {
                    try {
                        JSONArray array = JSON.parseArray(delDatas);
                        for (int i = 0; i < array.size(); i++) {
                            JSONObject jsonObject = array.getJSONObject(i);
                            String backupContent = jsonObject.getString("backup_content");
                            if (!StringUtils.isEmpty(backupContent) && backupContent.indexOf(",") > -1) {
                                jsonObject.put("backup_content",backupContent.replaceAll(",","\n"));
                            }
                            String masterContent = jsonObject.getString("master_content");
                            if (!StringUtils.isEmpty(masterContent) && masterContent.indexOf(",") > -1) {
                                jsonObject.put("master_content",masterContent.replaceAll(",","\n"));
                            }
                            Map<String, Object>  map = Maps.newHashMap();
                            map.putAll(jsonObject);
                            map.putAll(compareMap);
                            map.put("compareType", "???????????????");
                            dataLists.add(map);
                        }
                    } catch (Exception e) {

                    }
                }

            }
            OutputStream os = response.getOutputStream();// ???????????????
            response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setHeader("Content-Type", "application/vnd.ms-excel");
            //excel constuct
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, title, headerList, dataLists, keyList);
            book.write(os);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        }
    }

    /**
     * ??????
     *
     * @param id
     * @param masterIndex
     * @param backupIndex
     * @return
     */
    @Override
    public Map<String, Object> compare(@PathVariable("id") Integer id,
                                     @RequestParam(name = "masterIndex") String masterIndex,
                                     @RequestParam(name = "backupIndex") String backupIndex) {
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        return configCompareServiceClient.compare(id, authCtx.getUser().getUsername(), masterIndex, backupIndex);
    }

    /**
     * ??????
     *
     * @param configCompare
     * @return
     */
    @Override
    public void insert(@RequestBody ConfigCompareReq configCompare) {
        if (configCompare == null || StringUtils.isEmpty(configCompare.getMasterIp()) || StringUtils.isEmpty(configCompare.getBackupIp())) {
            throw new BaseException(LastLogCodeEnum.VALIDATE_ERROR, ResultErrorEnum.BAD_REQUEST);
        }
        RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
        configCompare.setCreateUser(authCtx.getUser().getUsername());
        configCompareServiceClient.insert(configCompare);
    }

    /**
     * ??????????????????
     *
     * @param file
     * @return
     */
    @Override
    public Map<String, Object> importFile(@RequestPart("file") MultipartFile file) {
        Map<String, Object> returnMap = new HashMap<>();
        if (null == file) {
            returnMap.put("flag", "false");
            returnMap.put("message", "?????????????????????");
            return returnMap;
        }
        InputStream is = null;
        Workbook wb;
        try {
            is = file.getInputStream();
            wb = WorkbookFactory.create(is);
            Sheet sheet = wb.getSheetAt(0);
            int totalRows = sheet.getPhysicalNumberOfRows();
            if (totalRows <= 1) {
                returnMap.put("flag", "false");
                returnMap.put("message", "Excel???????????????, ????????????Excel?????????, ?????????????????????!");
                return returnMap;
            }
            List<ConfigCompareReq> list = Lists.newArrayList();
            RequestAuthContext authCtx = RequestAuthContext.currentRequestAuthContext();
            for (int r = 1 ; r < totalRows; r++) {
                Row row = sheet.getRow(r);
                // ?????????
                Cell cell0 = row.getCell(0);
                // ?????????IP
                Cell cell1 = row.getCell(1);
                // ?????????IP
                Cell cell2 = row.getCell(2);
                // ??????
                Cell cell3 = row.getCell(3);
                if (cell1 == null || cell2 == null
                        || StringUtils.isEmpty(cell1.getStringCellValue())
                        || StringUtils.isEmpty(cell2.getStringCellValue())) {
                    continue;
                }
                ConfigCompareReq compareReq = new ConfigCompareReq();
                if (cell0 != null) {
                    compareReq.setIdcType(cell0.getStringCellValue());
                }
                compareReq.setMasterIp(cell1.getStringCellValue());
                compareReq.setBackupIp(cell2.getStringCellValue());
                if (cell3 != null) {
                    compareReq.setBrand(cell3.getStringCellValue());
                }
                compareReq.setCreateUser(authCtx.getUser().getUsername());
                list.add(compareReq);
            }
            if (list.size() > 0) {
                configCompareServiceClient.importFile(list);
            } else {
                returnMap.put("flag", "false");
                returnMap.put("message", "?????????????????????");
            }
        } catch (Exception e) {
            returnMap.put("flag", "false");
            returnMap.put("message", "??????Excel????????????:" + e.getMessage());
            returnMap.put("error", e.getMessage());
            return returnMap;
        } finally {
            IOUtils.closeQuietly(is);
        }

        return returnMap;
    }

    /**
     * ??????????????????
     *
     * @param compareId
     * @return
     */
    @Override
    public List<ConfigCompareLogsResp> getLogs(@PathVariable("compare_id") Integer compareId) {
        return configCompareServiceClient.getLogs(compareId);
    }

    /**
     * ??????????????????
     *
     * @param compareId
     * @return
     */
    @Override
    public void exportLogs(@PathVariable("compare_id") Integer compareId) {
        List<ConfigCompareLogsResp> list = configCompareServiceClient.getLogs(compareId);
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletResponse response = servletRequestAttributes.getResponse();

        String[] headerList = {"?????????????????????","?????????????????????","???????????????","???????????????","???????????????","??????????????????"};
        String[] keyList = {"masterConfigFile","backupConfigFile","addResult","modifyResult","delResult","compareTime"};
        String title = "????????????";
        String fileName = title+".xlsx";

        try {
            List<Map<String, Object>> dataLists = new ArrayList<Map<String,Object>>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (ConfigCompareLogsResp configCompareLogsResp : list) {
                String addResult = configCompareLogsResp.getAddResult();
                if (!StringUtils.isNotEmpty(addResult) && addResult.indexOf(",") > -1) {
                    configCompareLogsResp.setAddResult(addResult.replaceAll(",", "\n"));
                }
                String delResult = configCompareLogsResp.getDelResult();
                if (!StringUtils.isNotEmpty(delResult) && delResult.indexOf(",") > -1) {
                    configCompareLogsResp.setDelResult(delResult.replaceAll(",", "\n"));
                }
                String masterConfigFile = configCompareLogsResp.getMasterConfigFile();
                if (!StringUtils.isEmpty(masterConfigFile)) {
                    configCompareLogsResp.setMasterConfigFile(masterConfigFile.substring(masterConfigFile.lastIndexOf("/")));
                }
                String backupConfigFile = configCompareLogsResp.getBackupConfigFile();
                if (!StringUtils.isEmpty(backupConfigFile)) {
                    configCompareLogsResp.setBackupConfigFile(backupConfigFile.substring(backupConfigFile.lastIndexOf("/")));
                }
                Map<String, Object>  map= ExportExcelUtil.objectToMap(configCompareLogsResp);
                map.put("compareTime",  sdf.format(configCompareLogsResp.getCompareTime()));
                dataLists.add(map);
            }
            OutputStream os = response.getOutputStream();// ???????????????
            response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setHeader("Content-Type", "application/vnd.ms-excel");
            //excel constuct
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, title, headerList, dataLists, keyList);
            book.write(os);
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
        }
    }

    /**
     *
     * @param compareId
     * @return
     */
    public Map<String, Object> getIndex(@PathVariable("compare_id") Integer compareId) {
        return configCompareServiceClient.getIndex(compareId);
    }
}
