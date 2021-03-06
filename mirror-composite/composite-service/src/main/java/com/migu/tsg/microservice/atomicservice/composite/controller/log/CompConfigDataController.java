package com.migu.tsg.microservice.atomicservice.composite.controller.log;

import com.aspire.mirror.common.entity.PageResponse;
import com.aspire.mirror.log.api.dto.ConfigDataResponse;
import com.aspire.mirror.log.api.dto.ConfigDataSearchRequest;
import com.aspire.mirror.log.api.dto.ConfigFileUploadReq;
import com.google.common.collect.Maps;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.log.IConfigDataServiceClient;
import com.migu.tsg.microservice.atomicservice.composite.controller.CommonResourceController;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.EasyPoiUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.FileUtil;
import com.migu.tsg.microservice.atomicservice.composite.service.logs.ICompConfigDataService;
import com.migu.tsg.microservice.atomicservice.composite.service.logs.payload.CompConfigDataResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.logs.payload.CompConfigDataSearchRequest;
import com.migu.tsg.microservice.atomicservice.composite.service.logs.payload.CompConfigFileUploadReq;
import com.migu.tsg.microservice.atomicservice.composite.service.logs.payload.ConfigDataExportRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.migu.tsg.microservice.atomicservice.composite.controller.util.PayloadParseUtil.jacksonBaseParse;

/**
 * ?????????????????????
 * <p>
 * ????????????:  mirror??????
 * ???:        com.aspire.mirror.log.server.controller
 * ?????????:    ConfigDataController.java
 * ?????????:    ?????????????????????
 * ?????????:    JinSu
 * ????????????:  2019/6/19 14:38
 * ??????:      v1.0
 */
@RestController
@Slf4j
public class CompConfigDataController extends CommonResourceController implements ICompConfigDataService {

    @Autowired
    private IConfigDataServiceClient configDataService;

    /**
     * ????????????????????????
     *
     * @param request ????????????
     * @return PageResponse<CompConfigDataResponse> ??????page??????
     */
    @Override
    public PageResponse<CompConfigDataResponse> getConfigData(@RequestBody CompConfigDataSearchRequest request) {
        ConfigDataSearchRequest req = jacksonBaseParse(ConfigDataSearchRequest.class, request);
        req.setIsExport("0");
        PageResponse<ConfigDataResponse> result = configDataService.getConfigData(req);
        PageResponse<CompConfigDataResponse> response = new PageResponse<>();
        response.setCount(result.getCount());
        List<CompConfigDataResponse> list = jacksonBaseParse(CompConfigDataResponse.class, result.getResult());
        response.setResult(list);
        return response;
    }

    /**
     * ??????????????????
     *
     * @param request ????????????
     */
    @Override
    public void exportConfigData(@RequestBody ConfigDataExportRequest request, HttpServletResponse response) {
        ConfigDataSearchRequest req = jacksonBaseParse(ConfigDataSearchRequest.class, request);
        req.setIsExport("1");
        PageResponse<ConfigDataResponse> result = configDataService.getConfigData(req);
        List<CompConfigDataResponse> list = jacksonBaseParse(CompConfigDataResponse.class, result.getResult());
        EasyPoiUtil.exportExcel(list, "????????????????????????", "configdata", CompConfigDataResponse.class, "configdata_export",
                true, response);
    }

    @Override
    public CompConfigDataResponse getConfigById(@PathVariable("index") String index, @PathVariable("id") String id) {
        if (StringUtils.isEmpty(id) || StringUtils.isEmpty(index)) {
            return null;
        }
        ConfigDataResponse configDataResponse = configDataService.getConfigById(index, id);
        return jacksonBaseParse(CompConfigDataResponse.class, configDataResponse);
    }

    private File getFile(MultipartFile multipartFile) {
        String property = System.getProperty("user.dir");
        // luowenbo 2020-07-24 ????????? '\\' -> File.separator
        String path = property + File.separator + "uploadFile.zip";
        log.info(path);
        File file = new File(path);
        try {
            if (file.exists()) {
                boolean deleteIsTrue = file.delete();
                if (!deleteIsTrue) {
                    String message = "The temporary file is deleted unsuccessfully ";
                    throw new RuntimeException(message);
                }
                boolean createIsTrue = file.createNewFile();
                if (!createIsTrue) {
                    String message = "The temporary file is created unsuccessfully ";
                    throw new RuntimeException(message);
                }
            } else {
                boolean createIsTrue = file.createNewFile();
                if (!createIsTrue) {
                    String message = "The temporary file is created unsuccessfully ";
                    throw new RuntimeException(message);
                }
            }
            FileUtil.inputStreamToFile(multipartFile.getInputStream(), file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return file;
    }

    @Override
    public Map<String, Object> uploadConfigFile(@RequestParam("idcType") String idcType,
                                                @RequestParam("uploadInfo") String uploadInfo,
                                                @RequestParam("file") MultipartFile file) {
        RequestAuthContext context = RequestAuthContext.currentRequestAuthContext();
        Map<String, Object> response = Maps.newHashMap();
        List<CompConfigFileUploadReq> compConfigFileUploadList = Lists.newArrayList();
        FileInputStream input = null;
        ZipInputStream zipInputStream = null;
        try {
            File f = getFile(file);
            //?????????????????????
            input = new FileInputStream(f);
            //??????ZIP?????????(????????????????????????Charset.forName("GBK")????????????java.lang.IllegalArgumentException: MALFORMED)
            zipInputStream = new ZipInputStream(new BufferedInputStream(input), Charset.forName("GBK"));
            //??????ZipEntry??????null,????????????????????????zipInputStream.getNextEntry???????????????????????????
            ZipEntry ze = null;
            //????????????
            while ((ze = zipInputStream.getNextEntry()) != null) {
                String name = ze.getName();
                log.info("????????????" + name + " ???????????????" + ze.getSize() + " bytes");
                //??????????????????
                StringBuilder stringBuffer = new StringBuilder();
                BufferedReader br = new BufferedReader(new InputStreamReader(zipInputStream,Charset.forName("GBK")));
                String line;
                //????????????????????????
                while ((line = br.readLine()) != null) {
//                    System.out.println(line);
//                    System.getProperty("line.separator")
                    stringBuffer.append(line).append(System.getProperty("line.separator"));
                }
                log.info("???????????????" + stringBuffer.toString());
                // ???????????????
                if (!name.contains("_")) {
                    response.put("flag", "error");
                    response.put("msg","??????????????????[IP_????????????.?????????]????????????!");
                    return response;
                }
                String[] nameSplit = name.split("_");
                if (!(nameSplit.length == 2)) {
                    response.put("flag", "error");
                    response.put("msg","??????????????????[IP_????????????.?????????]????????????!");
                    return response;
                }
                String deviceIp = nameSplit[0];
                if (!nameSplit[1].contains(".")) {
                    response.put("flag", "error");
                    response.put("msg","??????????????????[IP_????????????.?????????]????????????!");
                    return response;
                }
                String replace = nameSplit[1].replace( ".", "_" );
                String[] split = replace.split( "_" );
                String uploadTime = split[0];
                SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHHmmss");
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                CompConfigFileUploadReq compConfigFileUploadReq = new CompConfigFileUploadReq();
                compConfigFileUploadReq.setUuid(UUID.randomUUID().toString());
                compConfigFileUploadReq.setIdcType(idcType);
                compConfigFileUploadReq.setDeviceIp(deviceIp);
                compConfigFileUploadReq.setUploadTime(sdf2.format(sdf1.parse(uploadTime)));
                compConfigFileUploadReq.setUploadContent(stringBuffer.toString());
                compConfigFileUploadReq.setUploadInfo(uploadInfo);
                compConfigFileUploadReq.setUserName(context.getUser().getUsername());
                compConfigFileUploadList.add(compConfigFileUploadReq);
            }

            if (CollectionUtils.isNotEmpty(compConfigFileUploadList)) {
                configDataService.uploadConfigFile(jacksonBaseParse(ConfigFileUploadReq.class,compConfigFileUploadList));
            }

            response.put("flag", "success");
            response.put("msg","????????????!");
        } catch (Exception e) {
            log.error("[??????????????????] >>> error is {}",e);
            response.put("flag", "error");
            response.put("msg","??????????????????!");
        } finally {
            // luowenbo 2020-07-24 ????????? ????????????????????????????????????finally????????????????????????????????????????????????????????????????????????????????????
            if(null != zipInputStream) {
                //?????????
                try {
                    zipInputStream.closeEntry();
                    zipInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(null != input) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return response;
    }


}
