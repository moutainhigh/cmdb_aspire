package com.aspire.ums.cmdb.IpResource.web;

import com.aspire.ums.cmdb.IpResource.entity.CmdbIpAddressEntity;
import com.aspire.ums.cmdb.IpResource.entity.CmdbNetworkSegmentEntity;
import com.aspire.ums.cmdb.IpResource.service.AssetExportExcelService;
import com.aspire.ums.cmdb.IpResource.service.CmdbAssetInfoService;
import com.aspire.ums.cmdb.IpResource.service.ComboboxService;
import com.aspire.ums.cmdb.IpResource.service.PhysicalAndVirtualService;
import com.aspire.ums.cmdb.common.Result;
import com.aspire.ums.cmdb.common.ResultVo;
import com.aspire.ums.cmdb.ipResource.IIpResouceAPI;
import com.aspire.ums.cmdb.ipResource.payload.*;
import com.aspire.ums.cmdb.util.AssetExcelExportUtils;
import com.aspire.ums.cmdb.util.StringUtils;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: fanshenquan
 * @Datetime: 2020/6/17 13:57
 */
@Slf4j
@RestController
public class IpResouceController implements IIpResouceAPI {

    @Autowired
    private ComboboxService comboboxService;
    @Autowired
    private PhysicalAndVirtualService physicalAndVirtualService;
    @Autowired
    private CmdbAssetInfoService assetInfoService;
    @Autowired
    private AssetExportExcelService assetExportExcelService;

    @Override
    public ResultVo getDropDownBoxList(@RequestParam(value = "queryType") String queryType) {
        log.info("IpResouceController.getDropDownBoxList is {}", queryType);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            Map<String, Object> resultMap = Maps.newHashMap();
            if (queryType.equals("1")) {//????????????
                resultMap.put("segmentList", comboboxService.getSegmentType(null));
                resultMap.put("firstBusinessList", comboboxService.getFirstBusiness(null));
            } else if (queryType.equals("2")) {
                resultMap.put("businessList1", comboboxService.getAloneBusiness(null));
                resultMap.put("deviceClassList", comboboxService.getDevicesClassOrType(null));
                resultMap.put("machineRoomList", comboboxService.getIdcLocationType());
            } else {
                throw new RuntimeException("???????????????" + queryType);
            }
            resultMap.put("idcList", comboboxService.getIdcVal());
            resultVo.setData(resultMap);
        } catch (Exception e) {
            log.error("???????????????????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo getComboboxList(@PathVariable(value = "path") String path, @RequestParam(value = "pid", required = false) String pid) {
        log.info("IpResouceController.getComboboxList is path={},pid={}", path, pid);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            switch (path) {
                case "getIdcList"://?????????
                    resultVo.setData(comboboxService.getIdcVal());
                    break;
                case "getDevicesClassOrType"://????????????->????????????
                    resultVo.setData(comboboxService.getDevicesClassOrType(pid));
                    break;
                case "getFirstBusiness"://???????????????->???????????????
                    resultVo.setData(comboboxService.getFirstBusiness(pid));
                    break;
                case "getAloneBusiness"://???????????????->?????????????????????
                    resultVo.setData(comboboxService.getAloneBusiness(pid));
                    break;
                case "getSegmentType"://????????????->????????????
                    resultVo.setData(comboboxService.getSegmentType(pid));
                    break;
                case "getInnerSegmentUse"://????????????->??????IP????????????
                    resultVo.setData(comboboxService.getInnerSegmentUse(pid));
                    break;
                case "getInnerSegmentSubUse"://??????IP????????????->??????IP????????????
                    resultVo.setData(comboboxService.getInnerSegmentSubUse(pid));
                    break;
                case "getPublicSegmentUse"://??????IP????????????->??????IP????????????
                    resultVo.setData(comboboxService.getPublicSegmentUse(pid));
                    break;
                case "getIpv6SegmentUse"://??????IP????????????->??????IP????????????
                    resultVo.setData(comboboxService.getIpv6SegmentUse(pid));
                    break;
                case "getIpTypeForAsset"://??????IP??????
                    resultVo.setData(comboboxService.getIpTypeForAsset(pid));
                    break;
                default:
                    throw new RuntimeException("???????????????+" + path);
            }
        } catch (Exception e) {
            log.error("???????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo getIpListByCount(@RequestBody AutoAllocateIpParam param) {
        log.info("IpResouceController.getIpListByCount is {}", param);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            Map<String, Object> resultMap = physicalAndVirtualService.ipAutoAssign(param);
            resultVo.setData(resultMap);
        } catch (Exception e) {
            log.error("???????????????IP?????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo getSegmentInfoList(@RequestBody SegmentInfoParam param) {
        log.info("IpResouceController.getSegmentInfoList is {}", param);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            Result<CmdbNetworkSegmentEntity> data = new Result<>();
            data.setData(physicalAndVirtualService.getNetworkSegmentList(param));
            data.setTotalSize(physicalAndVirtualService.getNetworkSegmentListCount(param));
            resultVo.setData(data);
        } catch (Exception e) {
            log.error("?????????????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo getIpInfoInfoList(@RequestBody IpInfoParam param) {
        log.info("IpResouceController.getIpInfoInfoList is {}", param);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            Result<CmdbIpAddressEntity> data = new Result<>();
            data.setData(physicalAndVirtualService.getIpAddressList(param));
            data.setTotalSize(physicalAndVirtualService.getIpAddressListCount(param));
            resultVo.setData(data);
        } catch (Exception e) {
            log.error("?????????????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo getAssetInfoList(@RequestBody AssetInfoParam param) {
        log.info("IpResouceController.getAssetInfoList is {}", param);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            Result data = new Result<>();
            if (StringUtils.isEmpty(param.getAssetIds())) {
                data.setData(assetInfoService.getAssetList(param));
                data.setTotalSize(assetInfoService.getAssetListCount(param));
            } else {
                data.setData(assetInfoService.getBackfillAsset(param));
                data.setTotalSize(assetInfoService.getBackfillAssetCount(param));
            }
            resultVo.setData(data);
        } catch (Exception e) {
            log.error("???????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo getAssetIpList(@RequestBody AssetIpInfoParam param) {
        log.info("IpResouceController.getAssetIpList is {}", param);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            Result data = new Result<>();
            data.setData(assetInfoService.getAssetIpList(param));
            // data.setTotalSize(assetInfoService.getAssetIpListCount(param));
            resultVo.setData(data);
        } catch (Exception e) {
            log.error("?????????????????????IP?????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo getChangeIpList(@RequestBody SegmentIpInfoParam param) {
        log.info("IpResouceController.getChangeIpList is {}", param);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            Result data = new Result<>();
            data.setData(physicalAndVirtualService.getSegmentIpList(param));
            data.setTotalSize(physicalAndVirtualService.getSegmentIpListCount(param));
            resultVo.setData(data);
        } catch (Exception e) {
            log.error("??????-IP???????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo getAssetExcelInfoList() {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            List<List<Object>> assetExportInfo = assetExportExcelService.getAssetExportInfo();
            if (null != assetExportInfo) {
                resultVo.setData(assetExportInfo);
            }
        } catch (Exception e) {
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo getAssetExcelHeaderList() {
        return new ResultVo(true, "???????????????", assetExportExcelService.buildAssetExcelHeader());
    }

    @Override
    public void exportAssetExcel(HttpServletResponse response) throws Exception {
        String[] headerList = assetExportExcelService.buildAssetExcelHeader();
        List<List<Object>> result = assetExportExcelService.getAssetExportInfo();
        String title = "cmdbAsset";
        String filePath = "D:\\opt\\aspire\\attachment";
        String fileName = title + ".xlsx";
//        OutputStream os = response.getOutputStream();// ???????????????
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(filePath + fileName);
//            response.setHeader("Content-Disposition",
//                    "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
//            response.setHeader("Connection", "close");
//            response.setHeader("Content-Type", "application/vnd.ms-excel");
            XSSFWorkbook wb = new XSSFWorkbook();
            AssetExcelExportUtils.cmdbUtilss(wb, result, headerList, title);
            wb.write(os);
        } catch (IOException e) {
            log.error("??????????????????excel??????", e.getMessage());
        } finally {
            // hangfang 2020.07.30 ?????????????????????
            if(null != os) {
                try {
                    os.flush();
                    os.close();
                } catch (IOException e) {
                    e.getStackTrace();
                    log.error("???????????????");
                }

            }
        }
    }

    @Override
    public ResultVo updateIpInfo(@RequestBody IpInfoUpdateParam param) {
        log.info("IpResouceController.updateIpInfo param is {}", param);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            physicalAndVirtualService.updateIpInfo(param);
        } catch (Exception e) {
            log.error("???????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo delAssetInfoById(@RequestBody Map<String, String> param) {
        log.info("IpResouceController.updateIpInfo param is {}", param);
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            if (!param.containsKey("assetIds")) {
                throw new RuntimeException("assetIds???????????????");
            }
            assetInfoService.updateIsDelete(param.get("assetIds"));
        } catch (Exception e) {
            log.error("???????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo getKvmTemplateList(@RequestBody Map<String, String> param) {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            resultVo.setData(assetInfoService.getKvmTemplateList(param));
        } catch (Exception e) {
            log.error("kvm???????????????????????????",e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }

    @Override
    public ResultVo getDropDownBoxListByParam(@RequestBody Map<String, String> param) {
        ResultVo resultVo = new ResultVo(true, "???????????????");
        try {
            Map<String, Object> resultMap = Maps.newHashMap();
            List<Map<String, String>> list = assetInfoService.getOneBusinessAndAloneBusiness(param);
            if(!list.isEmpty()) {
                Map<String, String> map = list.get(0);
                List<Map<String,String>> firstBusinessList = new ArrayList<>();
                List<Map<String,String>> businessList1 = new ArrayList<>();
                Map<String,String> firstBusiness = new HashMap<>();
                firstBusiness.put("label",map.get("onBusinessName"));
                firstBusiness.put("value",map.get("onBusinessId"));
                firstBusinessList.add(firstBusiness);
                Map<String,String> business = new HashMap<>();
                business.put("label",map.get("businessName"));
                business.put("value",map.get("businessId"));
                businessList1.add(business);
                resultMap.put("firstBusinessList", firstBusinessList);
                resultMap.put("businessList1", businessList1);
            } else {
                resultMap.put("firstBusinessList", comboboxService.getFirstBusiness(null));
                resultMap.put("businessList1", comboboxService.getAloneBusiness(null));
            }
            resultMap.put("deviceClassList", comboboxService.getDevicesClassOrType(null));
            resultMap.put("machineRoomList", comboboxService.getIdcLocationType());
            resultMap.put("idcList", comboboxService.getIdcVal());
            resultVo.setData(resultMap);
        } catch (Exception e) {
            log.error("???????????????????????????????????????", e);
            resultVo.setSuccess(false);
            resultVo.setMsg(String.format("???????????????%s", e.getMessage()));
        }
        return resultVo;
    }
}
