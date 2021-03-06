package com.aspire.mirror.alert.server.task.leakScan;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aspire.mirror.alert.server.biz.leakScan.SecurityLeakScanBiz;
import com.aspire.mirror.alert.server.clientservice.cmdb.InstanceSearchClient;
import com.aspire.mirror.alert.server.domain.GetLdapMemberRequest;
import com.aspire.mirror.alert.server.vo.leakScan.*;
import com.aspire.mirror.alert.server.clientservice.LdapClient;
import com.aspire.mirror.alert.server.clientservice.payload.GetLdapUserResponse;
import com.aspire.mirror.alert.server.clientservice.payload.ListPagenationResponse;
import com.aspire.mirror.alert.server.util.DateUtils;
import com.aspire.ums.cmdb.allocate.payload.BizSysRequestBody;
import com.google.common.collect.Lists;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.util.IOUtils;
import org.dom4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

@Component
@Order(3)
public class SecurityLeakScanBpmSubmitTask implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityLeakScanBpmSubmitTask.class);

    @Value("${sls.bpm.queue-size:1000}")
    private int SLS_BPM_QUEUE_SIZE;

    @Value("${sls.bpm.queue-consumer-num:1}")
    private int SLS_BPM_QUEUE_CONSUMER_NUM;

    @Value("${sls.bpm.queue-offer-timeout:5}")
    private int SLS_BPM_QUEUE_OFFER_TIMEOUT;

    @Value("${sls.bpm.upload.url}")
    private String BPM_UPLOAD_URL;

    @Value("${sls.ldap.namespace:alauda}")
    private String LDAP_NAMESPACE;

    /* ----------- webservice ------------------------------------------------------------------------------- */
    @Value(value = "${sls.bpm.webservice.url}")
    private String BPM_WEBSERVIE_URL;
    @Value(value = "${sls.bpm.webservice.flowkey}")
    private String BPM_WEBSERVIE_FLOWKEY;
    @Value(value = "${sls.bpm.webservice.account}")
    private String BPM_WEBSERVIE_ACOUNT;
    @Value(value = "${sls.bpm.webservice.namespace}")
    private String BPM_WEBSERVIE_NAMESPACE;
    @Value(value = "${sls.bpm.webservice.method}")
    private String BPM_WEBSERVIE_METHOD;
    @Value(value = "${sls.bpm.rest.url}")
    private String BPM_REST_URL;

    @Autowired
    private InstanceSearchClient instanceSearchClient;
    @Autowired
    private LdapClient ldapClient;


    private BlockingQueue<SecurityLeakSacnBpmSubstance> substanceBlockingQueue;

    @Autowired
    private SecurityLeakScanBiz securityLeakScanBiz;
    /**
     * Callback used to run the bean.
     *
     * @param args incoming application arguments
     * @throws Exception on error
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // ?????????????????????
        substanceBlockingQueue = new LinkedBlockingDeque<>(SLS_BPM_QUEUE_SIZE);
        Executors.newSingleThreadExecutor().execute(new BpmCallHandler(substanceBlockingQueue));
    }

    /**
     * ????????????????????????????????????????????????
     * @param substance
     */
    public void submitBpmSubstance(SecurityLeakSacnBpmSubstance substance) {
        String scanId = substance.getScanId();
        String attachFileName = substance.getAttachFile().getName();
        try {
            if (substanceBlockingQueue.offer(substance, SLS_BPM_QUEUE_OFFER_TIMEOUT, TimeUnit.SECONDS)) {
            	LOGGER.info("Successfully offered bpm task ! scanId: {} file: {} to bpm quue. current queue size: {}",
                        scanId, attachFileName, substanceBlockingQueue.size());
            } else {
            	LOGGER.error("Failed to offer bpm task ! scanId: {} file: {} to bpm quue.", scanId, attachFileName);
            }
        } catch (InterruptedException e) {
        	LOGGER.error("Interrupted, failed to offer bpm queue, scanId: {} file: {}", scanId, attachFileName, e);
        }
    }

    private String httpUpload (final File zipFile, final String url, final String token) {
        final String zipFileName = zipFile.getName();
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        String fileId = "";
        try {
            final HttpPost httpPost = new HttpPost(url);
            if (StringUtils.isNotEmpty(token)) {
                httpPost.addHeader("Authorization", "Bearer " + token);
            }
//                httpPost.setHeader("Content-Type", "application/json"); // ???????????????
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(Charset.forName("UTF-8"));
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            FileBody fileBody = new FileBody(zipFile);
            builder.addPart("file", fileBody);
//                builder.addTextBody("filename", fileName,  ContentType.DEFAULT_BINARY);
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            final HttpResponse httpResponse = httpClient.execute(httpPost);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            LOGGER.info("POST Response Status: {}", statusCode);
            HttpEntity responseEntity = httpResponse.getEntity();
            String result = "";
            if (responseEntity != null) {
                // ?????????????????????????????????
                result = EntityUtils.toString(responseEntity, Charset.forName("UTF-8"));
                LOGGER.info("POST ???????????? {} ??? {} ??????: {}", zipFileName, url, result);
            }
            if (statusCode == 200) {
                if (StringUtils.isNotEmpty(result)) {
                    JSONObject map = JSON.parseObject(result);
                    boolean state = Boolean.valueOf(map.getString("state"));
                    if (state) {
                    	LOGGER.info("?????????????????????: {}", zipFileName);
                        String value = map.getString("value");
                        JSONObject obj = JSON.parseObject(value);
                        fileId = obj.getString("fileId");
                    }
                }
            } else {
            	LOGGER.info("POST ???????????? HttpCode: {}  Content: {}", statusCode, result);
            }
        } catch (UnsupportedCharsetException e) {
        	LOGGER.error("send post error:", e);
        } catch (ClientProtocolException e) {
        	LOGGER.error("send post error:{}", e);
        } catch (IOException e) {
        	LOGGER.error("send post error:{}", e);
        } finally {
            IOUtils.closeQuietly(httpClient);
        }
        return fileId;
    }

    private void callBpmRestful(String scanId, String content, String token) {
        ProcessLoadRequestVo request = new ProcessLoadRequestVo();
        request.setAccount(BPM_WEBSERVIE_ACOUNT);
        request.setFlowKey(BPM_WEBSERVIE_FLOWKEY);
        request.setSubject("");
        request.setBusinessKey("");
        request.setData(content);
        try {
            final CloseableHttpClient httpClient = HttpClients.createDefault();

            final HttpPost httpPost = new HttpPost(BPM_REST_URL);
            httpPost.addHeader("Content-type","application/json; charset=utf-8");
            if (StringUtils.isNotEmpty(token)) {
                httpPost.addHeader("Authorization", "Bearer " + token);
            }
            String body = JSON.toJSONString(request);
            StringEntity se = new StringEntity(body, Charset.forName("UTF-8"));
//            se.setContentType("application/json; charset=utf-8");
            httpPost.setEntity(se);
            final HttpResponse httpResponse = httpClient.execute(httpPost);
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            LOGGER.info("POST Response Status: {}", statusCode);
            HttpEntity responseEntity = httpResponse.getEntity();
            String result = "";
            if (responseEntity != null) {
                // ?????????????????????????????????
                result = EntityUtils.toString(responseEntity, Charset.forName("UTF-8"));
                LOGGER.info("POST {} ??????: {}", BPM_REST_URL, result);
            }
            if (statusCode == 200) {
                if (StringUtils.isNotEmpty(result)) {
                    ProcessLoadResponseVo response = JSONObject.parseObject(result, ProcessLoadResponseVo.class);
                    if (response == null) {
                    	LOGGER.error("BPM ??????????????????! ...{}", result);
                        return;
                    }
                    String status = response.getStatus();
                    if ("1".equals(status)) {
                        String runId = response.getRunId();
                        securityLeakScanBiz.fillScanRecordBpmId(scanId, runId);
                        LOGGER.info("BPM ???????????? ??? BPM ID: {}", runId);
                    } else {
                    	LOGGER.error("BPM ??????????????????! ...?????????: {}", status);
                    }
                }
            } else {
            	LOGGER.error("BPM ??????????????????! ...{}", result);
            }
        } catch (IOException e) {
        	LOGGER.error("", e);
        }
    }

    private void callWebService(String scanId, String content) {
//        logger.info("Bpm webservice call content: {}", content);
        StringBuilder builder = new StringBuilder();
        // head
        builder.append("<req flowKey=\""
                + BPM_WEBSERVIE_FLOWKEY + "\" subject=\"\" account=\"" + BPM_WEBSERVIE_ACOUNT
                + "\" businessKey=\"\" runId=\"\">");
        builder.append("<data>" + content + "</data>"); // body
        builder.append("</req>"); // foot
        String xml = builder.toString();
//        logger.info("Bpm webservice call xml: {}", xml);
        try {
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(BPM_WEBSERVIE_URL));
            call.setOperationName(new QName(BPM_WEBSERVIE_NAMESPACE, BPM_WEBSERVIE_METHOD));// WSDL???????????????????????????
            call.addParameter(new QName("xml"), org.apache.axis.encoding.XMLType.XSD_STRING,
                    javax.xml.rpc.ParameterMode.IN);// ???????????????
            call.setReturnType(org.apache.axis.encoding.XMLType.XSD_STRING);// ??????????????????
            // ??????webservice??????
            String result = (String) call.invoke(new Object[]{xml});
            LOGGER.info("BPM webservice request body: {} call back???{}", xml, result);
            Document doc = null;
            String message = "";
            try {
                doc = DocumentHelper.parseText(result); // ??????????????????XML
                Element rootElt = doc.getRootElement(); // ???????????????
                Attribute messageAttr = rootElt.attribute("message");
                if (null != messageAttr) {
                    message = messageAttr.getValue();
                }
                Attribute attribute = rootElt.attribute("status");
                if (null == attribute) {
                	LOGGER.error("BPM ??????????????????! ...{}", message);
                }
                String rs = attribute.getValue();
                if ("1".equals(rs)) {
                    Attribute runAttr = rootElt.attribute("runId");
                    String runId = runAttr.getValue();
                    if (StringUtils.isNotEmpty(runId)) {
                    	LOGGER.info("BPM ???????????? ??? BPM ID: {}", runId);
                        securityLeakScanBiz.fillScanRecordBpmId(scanId, runId);
                    }
                } else {
                	LOGGER.error("BPM ??????????????????! ...?????????: {}", rs);
                }
            } catch (DocumentException e) {
            	LOGGER.error("parse result error : ", e);
            }
        } catch (ServiceException e) {
        	LOGGER.error("Webservice????????????: ", e);
        } catch (MalformedURLException e) {
        	LOGGER.error("Webservice????????????: ", e);
        } catch (RemoteException e) {
        	LOGGER.error("Webservice????????????: ", e);
        }
    }

    private void submitBpmTask(String scanId, String fileId, String token) throws RuntimeException {
        SecurityLeakScanRecordVo record = securityLeakScanBiz.getSecurityLeakScanRecordById(scanId);
        if (record == null) {
        	LOGGER.error("????????? scanId: {} ???????????????!", scanId);
            return;
        }
        List<SecurityLeakScanReportVo> reoportList = securityLeakScanBiz.getReportListByScanId(scanId);
        String bizLine = record.getBizLine();
        SimpleDateFormat format = new SimpleDateFormat(DateUtils.SHORT_DATE_PATTERN, Locale.CHINA);
        String smsj = format.format(record.getScanDate()); // ????????????
        String aqldmc = format.format(record.getScanDate()) + "_" + bizLine + "_????????????"; // ??????????????????
        String gsbm = ""; // ????????????
        List<Map<String, Object>> sub_ldxxxx = Lists.newArrayList();
        List<String> usernameList = Lists.newArrayList(); // ?????????????????? ??????
        List<String> usrnameList = Lists.newArrayList(); // ??????????????????ID ????????????
        if (CollectionUtils.isNotEmpty(reoportList)) {
            BizSysRequestBody requestBody = new BizSysRequestBody();
            requestBody.setIp(reoportList.get(0).getIp());
            requestBody.setBizSystem(record.getBizLine());
            Map<String, Object> users = instanceSearchClient.selectDepartBizSystemInfo(requestBody);
            LOGGER.info("CMDB Request: {}, Response: {}", JSON.toJSONString(requestBody), JSON.toJSONString(users));
            List<String> deptList = Lists.newArrayList();
            String department1 = String.valueOf(users.get("department1"));
            LOGGER.info("BPM department 1: {}", department1);
            Locale.setDefault(Locale.ENGLISH);
            if (StringUtils.isNotEmpty(department1) && !"null".equals(department1.toLowerCase())) {
                deptList.add(department1);
            }
            String department2 = String.valueOf(users.get("department2"));
            LOGGER.info("BPM department 2: {}", department2);
            if (StringUtils.isNotEmpty(department2) && !"null".equals(department2.toLowerCase())) {
                deptList.add(department2);
            }
            gsbm = CollectionUtils.isEmpty(deptList) ? "" : StringUtils.join(deptList.toArray(), "_"); // ????????????
            List<Map> contactList = JSON.parseArray(JSON.toJSONString(users.get("contactList")), Map.class);
            if (CollectionUtils.isNotEmpty(contactList)) {
                for (Map<String, String> user : contactList) {
                    String name = user.get("name");
                    String phone = user.get("phone");
                    String email = user.get("email");
                    usernameList.add(name);
                }
            }
            if (CollectionUtils.isNotEmpty(usernameList)) {
                GetLdapMemberRequest ldapMemberRequest = new GetLdapMemberRequest();
                ldapMemberRequest.setNamespace(LDAP_NAMESPACE);
                ldapMemberRequest.setUuids(Lists.newArrayList());
                ldapMemberRequest.setUsernames(Lists.newArrayList());
                ldapMemberRequest.setNames(usernameList);
                ldapMemberRequest.setProjects(Lists.newArrayList());
                ldapMemberRequest.setOrderBy(Lists.newArrayList());
                ListPagenationResponse listPagenationResponse = ldapClient.listLdapMemberInfo(ldapMemberRequest);
                LOGGER.info("LDAP Request: namespace: {}, body: {}, Response: {}", LDAP_NAMESPACE,
                        JSON.toJSONString(ldapMemberRequest), JSON.toJSONString(listPagenationResponse));
                List<GetLdapUserResponse> results = listPagenationResponse.getResults();
                if (CollectionUtils.isNotEmpty(results)) {
                    for (GetLdapUserResponse userInfo : results) {
                        usrnameList.add(userInfo.getUsername());
                    }
                }
            }
            // ????????????????????????
            for (SecurityLeakScanReportVo dto : reoportList) {
                Map<String, Object> map = new HashMap<>();
                map.put("zwldsl", dto.getMediumLeaks());
                map.put("dwldsl", dto.getLowLeaks());
                map.put("gwldsl", dto.getHighLeaks());
                map.put("fxz", dto.getRiskVal());
                map.put("IPdz", dto.getIp());
                sub_ldxxxx.add(map);
            }
        } else {
        	LOGGER.error("?????????????????????????????????IP??????! ??????????????????! scanId: {}", scanId);
            return;
        }
        // ?????? webservice
        Map<String, Object> model = new HashMap<>();
        Map<String, Object> ldsmdx = new HashMap<>(); // ??????????????????
        ldsmdx.put("aqldmc", aqldmc); // ??????????????????
        ldsmdx.put("xfsm", record.getRepairStat()); // ????????????
        ldsmdx.put("zycjkr", ""); // ??????????????????
        ldsmdx.put("zycjkrID", ""); // ???????????????ID
        ldsmdx.put("ywxjkr", StringUtils.join(usernameList.toArray(), "||")); // ??????????????????
        ldsmdx.put("ywxjkrID", StringUtils.join(usrnameList.toArray(), "||")); // ???????????????ID
        ldsmdx.put("smsj", smsj); // ????????????
        ldsmdx.put("gsbm", gsbm); // ????????????
        ldsmdx.put("fjxx", fileId); // ???????????? fileId
        ldsmdx.put("sszyc", ""); // ???????????????
        ldsmdx.put("gsywx", bizLine); // ???????????????
        ldsmdx.put("sub_ldxxxx", sub_ldxxxx); // ?????? (??????????????????)

        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("zwldsl", null); // ??????????????????
        innerMap.put("dwldsl", null); // ??????????????????
        innerMap.put("gwldsl", null); // ??????????????????
        innerMap.put("fxz", null); // ?????????
        innerMap.put("IPdz", null); // IP??????

        Map<String, Object> initDataMap = new HashMap<>();
        initDataMap.put("ldxxxx", innerMap);
        // ??????????????????
        ldsmdx.put("initData", initDataMap);
        model.put("ldsmdx", ldsmdx);
        // ?????? ?????? BPM ??????
        long startTime = System.currentTimeMillis();
//        callWebService(scanId, JSON.toJSONString(model));
        callBpmRestful(scanId, JSON.toJSONString(model), token);
        long endTime = System.currentTimeMillis();
        LOGGER.info("BPM ======= ????????????????????????: {}s", (endTime - startTime)/1000);
    }

    private class BpmCallHandler implements Runnable {

        boolean interrupted = false;
        private BlockingQueue<SecurityLeakSacnBpmSubstance> queue;

        public BpmCallHandler(BlockingQueue<SecurityLeakSacnBpmSubstance> queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    final SecurityLeakSacnBpmSubstance substance = queue.take();
                    File attachment = substance.getAttachFile();
                    String scanId = substance.getScanId();
                    if (StringUtils.isEmpty(scanId)) {
                    	LOGGER.error("BPM ????????????, ????????? scanId: {} ???????????????!", scanId);
                        return;
                    }
                    String token = substance.getToken();
                    if (StringUtils.isEmpty(token)) {
                    	LOGGER.error("BPM file upload token is empty ! scanId: {} file: {}", scanId, attachment.getName());
                        return;
                    }
                    String fileId = httpUpload(attachment, BPM_UPLOAD_URL, token);
                    LOGGER.info("BPM file upload url: {} callback fileId: {}", BPM_UPLOAD_URL, fileId);
                    if (StringUtils.isNotEmpty(fileId)) {
                        securityLeakScanBiz.fillScanRecordBpmFileId(scanId, fileId);
                    } else {
                    	LOGGER.error("BPM file upload: ???????????????????????? {} ????????? fileId", attachment.getName());
                        return;
                    }
                    try {
                    	LOGGER.info("BPM ===========================================START===============");
                        submitBpmTask(scanId, fileId, token);
                        LOGGER.info("BPM ??????????????????->????????????????????????: {}", queue.size());
                        LOGGER.info("BPM ============================================END================");
                    } catch (RuntimeException e) {
                    	LOGGER.error("", e);
                    }
                }
            } catch (InterruptedException e) {
                interrupted = true;
                LOGGER.error("FtpFileConsumer {} interrupted !", Thread.currentThread().getName(), e);
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}
