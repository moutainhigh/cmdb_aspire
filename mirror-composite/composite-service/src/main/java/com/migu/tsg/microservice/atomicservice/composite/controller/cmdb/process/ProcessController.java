package com.migu.tsg.microservice.atomicservice.composite.controller.cmdb.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aspire.mirror.composite.service.cmdb.process.IProcessAPI;
import com.aspire.mirror.composite.service.cmdb.process.payload.ProcessParams;
import com.aspire.ums.cmdb.instance.payload.ImportProcess;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.cmdb.ConfigDictClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.cmdb.DemandClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.cmdb.module.ModuleClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.cmdb.process.ProcessClient;
import com.migu.tsg.microservice.atomicservice.composite.common.excel2pdf.POIModuleUtils;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.cmdb.process.conf.ImportTemplate;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.ExportExcelUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Copyright (C), 2015-2019, ???????????????????????? FileName: ProcessController Author: zhu.juwang Date: 2019/6/11 10:05 Description:
 * ${DESCRIPTION} History: <author> <time> <version> <desc> ???????????? ???????????? ????????? ??????
 */
@RestController
@Slf4j
public class ProcessController implements IProcessAPI {

    @Autowired
    private ProcessClient processClient;

    @Autowired
    private ModuleClient moduleClient;

    @Autowired
    private DemandClient demandClient;

    @Autowired
    private ImportTemplate importTemplate;

    @Autowired
    private ConfigDictClient configDictClient;

    @Autowired
    private POIModuleUtils poiModuleUtils;

    @Override
    public Map<String, String> importProcess(@RequestParam(value = "filename") MultipartFile file, ProcessParams processParams,
            HttpServletRequest request) {
        Map<String, String> returnMap = new HashMap<>();
        log.info("?????????????????? -> {}", request.getCharacterEncoding());
        if (null == file) {
            returnMap.put("flag", "false");
            returnMap.put("message", "?????????????????????");
            return returnMap;
        }
        log.info("?????????????????? -> {}", file.getOriginalFilename());
        if (!file.getOriginalFilename().matches("^.+\\.(?i)(xlsx)$") && !file.getOriginalFilename().matches("^.+\\.(?i)(xls)$")) {
            returnMap.put("flag", "false");
            returnMap.put("message", "???????????????excel?????????");
            return returnMap;
        }
        log.info("?????????????????? -> {}", file.getSize());
        if (file.getSize() == 0) {
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
            Sheet comboSheet = null;
            int totalRows = sheet.getPhysicalNumberOfRows();
            if (totalRows <= 1) {
                returnMap.put("flag", "false");
                returnMap.put("message", "Excel???????????????, ????????????Excel?????????, ?????????????????????!");
                return returnMap;
            }
            if ("instance".equals(processParams.getImportType().toLowerCase(Locale.ENGLISH))) {
                if (StringUtils.isEmpty(processParams.getModuleType()) &&
                        StringUtils.isEmpty(processParams.getModuleId())) {
                    returnMap.put("flag", "false");
                    returnMap.put("message", "Import params.moduleType is require.");
                    return returnMap;
                }
            }
            // ???????????????sheet?????????
            if (wb.getSheetAt(1) != null) {
                comboSheet = wb.getSheetAt(1);
            }
            // ??????????????????
            if (StringUtils.isEmpty(processParams.getOperatorUser())) {
                RequestAuthContext authContext = RequestAuthContext.currentRequestAuthContext();
                processParams.setOperatorUser(authContext.getUser().getUsername());
            }
            String processId = UUID.randomUUID().toString();
            // ????????????????????????
            ProcessThread processThread;
            if (("instance").equals(processParams.getImportType().toLowerCase(Locale.ENGLISH))) {
                processThread = new InstanceProcessThread();
                ((InstanceProcessThread) processThread).setConfigDictClient(configDictClient);
                // ((InstanceProcessThread) processThread).setModuleClient(moduleClient);
            } else if (("demand").equals(processParams.getImportType().toLowerCase(Locale.ENGLISH))) {
                processThread = new DemandProcessThread();
                ((DemandProcessThread) processThread).setDemandClient(demandClient);
            } else if (("bizresourcephy").equals(processParams.getImportType().toLowerCase(Locale.ENGLISH))
                    || ("bizresourcevm").equals(processParams.getImportType().toLowerCase(Locale.ENGLISH))) {
                processThread = new BizResourceProcessThread();
            } else {
                processThread = new BasicProcessThread();
            }
            processThread.setProcessId(processId);
            processThread.setImportType(processParams.getImportType());
            processThread.setSheet(sheet);
            processThread.setComboSheet(comboSheet);
            processThread.setProcessClient(processClient);
            processThread.setImportTemplate(importTemplate);
            processThread.setProcessParams(processParams);
            processThread.run();
            returnMap.put("processId", processId);
            returnMap.put("flag", "true");
            return returnMap;
        } catch (Exception e) {
            log.error("", e);
            returnMap.put("flag", "false");
            returnMap.put("message", "??????Excel????????????:" + e.getMessage());
            returnMap.put("error", e.getMessage());
            return returnMap;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Map<String, Object> getImportProcess(@PathVariable("processId") String processId) {
        return processClient.getImportProcess(processId);
    }

    @Override
    public Map<String, Object> removeImportProcess(@PathVariable("processId") String processId) {
        return processClient.removeImportProcess(processId);
    }

    @Override
    public Map<String, String> exportErrorFile(@PathVariable("processId") String processId, HttpServletResponse response) {
        Map<String, String> returnMap = new HashMap<>();
        try {
            ImportProcess importProcess = processClient.exportErrorFile(processId);
            if (importProcess != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                String fileName = dateFormat.format(new Date()) + "_" + importProcess.getSheetName()
                        + "??????????????????.xlsx";
                response.setHeader("Content-Disposition",
                        "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
                response.setHeader("Connection", "close");
                response.setHeader("Content-Type", "application/vnd.ms-excel");
                String sheetName = StringUtils.isEmpty(importProcess.getSheetName()) ? fileName : importProcess.getSheetName();
                XSSFWorkbook workbook = new XSSFWorkbook();
                List<Map<String, Object>> dataList = importProcess.getErrorList();
                Map<String, List<Map<String, Object>>> comboDataMap = importProcess.getComboDataMap();
                List<String> headerList = importProcess.getHeaderList();
                headerList.add("????????????");
                poiModuleUtils.generateExcel(workbook, sheetName, headerList, dataList, comboDataMap);
//
//                Module module = moduleClient.getModuleDetail(moduleId);
//                List<CmdbSimpleCode> headerList = instanceClient.getInstanceHeader(moduleId, null);
//
//                // ????????????Cell
//                List<String> excludeList = Arrays.asList("id", "module_id", "insert_time", "insert_person", "update_time", "update_person");
//                List<CmdbCode> afterFilterList = new LinkedList<>();
//                headerList.forEach((code) -> {
//                    CmdbCode cmdbCode = codeClient.getCodeByCodeId(code.getCodeId());
//                    if (cmdbCode != null && !excludeList.contains(cmdbCode.getFiledCode().toLowerCase())) {
//                        afterFilterList.add(cmdbCode);
//                    }
//                });
//
//                CmdbCode cmdbCode = afterFilterList.get(i);
//                List<CmdbV3CodeValidate> validates = cmdbCode.getValidates();
//                boolean isRequire = false;
//                if (validates != null && validates.size() > 0) {
//                    for (CmdbV3CodeValidate validate : validates) {
//                        if (("??????").equals(validate.getValidType())) {
//                            isRequire = true;
//                            break;
//                        }
//                    }
//                }


                // sheet??????  List<String> header List<Data> List<Data2>
                OutputStream os = response.getOutputStream();// ???????????????
//                eeu.exportExcel(book, 0, sheetName, titles, dataList, titles);
                workbook.write(os);
                os.flush();
                os.close();
//                if (dataList.size() > 0) {
//                    String[] titles = new String[dataList.get(0).keySet().size()];
//                    dataList.get(0).keySet().toArray(titles);
//                    ExportExcelUtil eeu = new ExportExcelUtil();
//                }
                returnMap.put("code", "success");
                response.setStatus(HttpStatus.NO_CONTENT.value());
            }
        } catch (Exception e) {
            log.error("??????Excel????????????!", e);
            returnMap.put("code", "error");
            returnMap.put("message", e.getMessage());
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return returnMap;
    }

    /**
     * ??????????????????
     * 
     * @param exportType
     *            ????????????
     * @param exportParams
     *            ????????????
     * @return
     */
    @Override
    public Map<String, String> exportReport(@PathVariable("exportType") String exportType,
            @RequestBody Map<String, Object> exportParams, HttpServletResponse response) {
        Map<String, String> returnMap = new HashMap<>();
        try {
            log.info("Request ProcessController.exportReport exportType -> {} exportParams -> {}", exportType, exportParams);
            if (!exportParams.containsKey("header")) {
                throw new RuntimeException("????????????Excel???????????????.");
            }
            if (!exportParams.containsKey("header_key")) {
                throw new RuntimeException("????????????Excel??????Key??????.");
            }
            if (!exportParams.containsKey("fileName")) {
                throw new RuntimeException("???????????????????????????.");
            }
            List<String> titleList = (List<String>) exportParams.get("header");
            List<String> keyList = (List<String>) exportParams.get("header_key");
            String[] titles = new String[titleList.size()];
            String[] keys = new String[keyList.size()];
            String fileName = exportParams.get("fileName") + ".xlsx";
            List<Map<String, Object>> dataList = processClient.exportReport(exportType, exportParams);
            response.setHeader("Content-Disposition",
                    "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setHeader("Content-Type", "application/vnd.ms-excel");
            if (dataList.size() > 0) {
                OutputStream os = response.getOutputStream();// ???????????????
                ExportExcelUtil eeu = new ExportExcelUtil();
                Workbook book = new SXSSFWorkbook(128);
                eeu.exportExcel(book, 0, fileName, titleList.toArray(titles), dataList, keyList.toArray(keys));
                book.write(os);
                os.flush();
                os.close();
            }
            returnMap.put("flag", "success");
            return returnMap;
        } catch (Exception e) {
            log.error("Export excel report error. Cause: {}", e.getMessage(), e);
            returnMap.put("flag", "error");
            returnMap.put("msg", e.getMessage());
        }
        return returnMap;
    }
}
