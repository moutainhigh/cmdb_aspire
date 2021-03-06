package com.aspire.mirror.alert.server.task.leakScan;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.aspire.mirror.alert.server.biz.leakScan.SecurityLeakScanBiz;
import com.aspire.mirror.alert.server.vo.leakScan.SecurityLeakSacnBpmSubstance;
import com.aspire.mirror.alert.server.vo.leakScan.SecurityLeakScanRecordVo;
import com.aspire.mirror.alert.server.dao.leakScan.po.SecurityLeakScanReportFile;
import com.aspire.mirror.alert.server.util.DateUtils;
import com.aspire.mirror.alert.server.util.Md5Utils;
import com.aspire.mirror.alert.server.vo.leakScan.SecurityLeakScanUploadFileSubstance;
import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.BlockingQueue;

@Component
public class SecurityLeakScanTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityLeakScanTask.class);

    @Autowired
    private SecurityLeakScanBiz securityLeakScanBiz;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;
    @Value("${sls.ftp.download.host}")
    private String SLS_FTP_HOST;
    @Value("${sls.ftp.download.port:21}")
    private int SLS_FTP_PORT;
    @Value("${sls.ftp.download.user}")
    private String SLS_FTP_USER;
    @Value("${sls.ftp.download.password}")
    private String SLS_FTP_PASSWORD;
    @Value("${sls.ftp.download.mode:pasv")
    private String SLS_FTP_MODE;
    @Value("${sls.ftp.download.path:/scan_report/pkg_ops_report}")
    private String SLS_FTP_BASE_PATH;
    @Value("${sls.local.store}")
    private String SLS_LOCAL_STORE_BASE_PATH;

    private static final String SLS_FTP_MODE_PORT = "port";
    private static final String SLS_FTP_MODE_PASV = "pasv";
    private static final String SYSTEM_LINE_SEPRATOR = System.getProperty("file.separator");
    /** ?????????????????? */
    private static String LOCAL_CHARSET = "GBK";
    // FTP???????????????????????????????????????iso-8859-1
    private static final String SERVER_CHARSET = "ISO-8859-1";

    @Autowired
    private SecurityLeakScanBpmSubmitTask bpmSubmitTask;

    public void scan(Date date) {
        final String dateStr = new SimpleDateFormat("yyyyMMdd").format(date);
        SimpleDateFormat format = new SimpleDateFormat(DateUtils.SHORT_DATE_PATTERN, Locale.CHINA);
        final String dateStrToClear = DateUtils.getPastDate(date, SLS_FTP_CLEAR_DAYS, format);

        final String ftpPath = SLS_FTP_BASE_PATH + "/" + dateStr;
        FTPClient ftpClient = new FTPClient();
        LOGGER.debug("#=========>>> FTP MODE: {}, DOWNLOAD PATH: {}", SLS_FTP_MODE, ftpPath);
        LOGGER.debug("#=========>>> FTP META INFO:\n #=========> URL: {}, PORT: {}, USER: {}, PASSWORD: {}",
                SLS_FTP_HOST, SLS_FTP_PORT, SLS_FTP_USER, SLS_FTP_PASSWORD);
        try {
            // Http ???????????????????????? token
            Map<String, String> params = new HashMap<>();
            params.put("username", BPM_TOKEN_USER);
            params.put("password", BPM_TOKEN_PASSWORD);
            final String token = getHttpFileUploadToken(BPM_TOKEN_URL, JSON.toJSONString(params));
            if (StringUtils.isEmpty(token)) {
                throw new RuntimeException("BPM token ????????????! URL: "+ BPM_TOKEN_URL +", USER: "+ BPM_TOKEN_USER +", PASSWORD: " + BPM_TOKEN_PASSWORD);
            }
            initFtpClient(
                    ftpClient,
                    SLS_FTP_HOST,
                    SLS_FTP_PORT,
                    SLS_FTP_USER,
                    SLS_FTP_PASSWORD,
                    SLS_FTP_MODE
            );
            if (ftpClient.changeWorkingDirectory(ftpPath)) {
            	LOGGER.debug("#======>>> FTP ??????: {} ????????????!", ftpPath);
                FTPFile[] ftpFiles = ftpClient.listFiles();
                for (FTPFile file : ftpFiles) {
                    String name = new String(file.getName().getBytes(),"UTF-8");
                    if (!StringUtils.endsWith(name.toLowerCase(), ".zip")) {
                        continue;
                    }
                    String ftpFilePath = ftpPath + "/" + name;
                    List<SecurityLeakScanRecordVo> recordsInDB = securityLeakScanBiz.getSecurityLeakScanRecordByDateAndFileName(dateStr, name);
                    if (CollectionUtils.isNotEmpty(recordsInDB)) {
                        continue;
                    }
                    List<SecurityLeakScanReportFile> reportFileInDB = securityLeakScanBiz.getFileByFtpPath(Md5Utils.generateCheckCode(ftpFilePath));
                    if (CollectionUtils.isNotEmpty(reportFileInDB)) {
//                        logger.info("FTP ????????????????????????: {} ???????????????????????????????????????????????????!", ftpFilePath);
                        continue;
                    }
                    // ????????????????????????
                    String targetPath = SLS_LOCAL_STORE_BASE_PATH + SYSTEM_LINE_SEPRATOR + dateStr;
                    File targetDir = new File(targetPath);
                    if (!targetDir.exists()) {
                        targetDir.mkdirs();
                    }
                    String targetFile = targetPath + SYSTEM_LINE_SEPRATOR + name;
                    File localFile = new File(targetFile);
                    OutputStream outputStream = new FileOutputStream(localFile);
                    ftpClient.retrieveFile(new String(file.getName().getBytes(LOCAL_CHARSET),
                            SERVER_CHARSET), outputStream);
                    if (localFile.exists()) {
                    	LOGGER.info("??????????????????: " + name + " ?????????: " + localFile.getAbsolutePath());
                        taskExecutor.execute(
                                new LeakScanFileHandel (
                                        ftpFilePath,
                                        localFile,
                                        dateStr,
                                        dateStrToClear,
                                        token
                                )
                        );
                    } else {
                    	LOGGER.error("FTP ????????????: {}/{} ?????? !!!!", ftpPath, name);
                    }
                }
            } else {
            	LOGGER.error("#======>>> FTP ??????: {} ????????????! ????????????????????????????????????????????????!", ftpPath);
            }
        } catch (Exception e) {
        	LOGGER.error("FTP ??????: {} ?????????????????? !!!!", ftpPath, e);
        } finally {
            if (ftpClient.isConnected()) {
                try {
                    ftpClient.disconnect();
                } catch (IOException e) {
                	LOGGER.error("??????FTP????????????!", e);
                }
            }
        }
    }

    private String getHttpFileUploadToken (String url, String param) {
        String token  = "";
        final CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            final HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            final StringEntity entity = new StringEntity(param, "UTF-8");
            httpPost.setEntity(entity);

            final HttpResponse httpResponse = httpClient.execute(httpPost);
            HttpEntity responseEntity = httpResponse.getEntity();
            int statusCode = httpResponse.getStatusLine().getStatusCode();
            LOGGER.debug("POST Response Status: {}", statusCode);
            if (statusCode == 200) {
                String result = EntityUtils.toString(responseEntity, Charset.forName("UTF-8"));
                JSONObject json = JSON.parseObject(result);
                token = json.getString("token");
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
        return token;
    }

    @Value("${sls.ftp.upload.host}")
    private String SLS_FTP_UPLOAD_HOST;

    @Value("${sls.ftp.upload.port:21}")
    private int SLS_FTP_UPLOAD_PORT;

    @Value("${sls.ftp.upload.user}")
    private String SLS_FTP_UPLOAD_USER;

    @Value("${sls.ftp.upload.password}")
    private String SLS_FTP_UPLOAD_PASSWORD;

    @Value("${sls.ftp.upload.mode:pasv")
    private String SLS_FTP_UPLOAD_MODE;

    @Value("${sls.ftp.upload.path}")
    private String SLS_FTP_UPLOAD_PATH;

    private BlockingQueue<SecurityLeakScanUploadFileSubstance> uploadQueue;

    @Value("${sls.ftp.upload.queue.offer-timeout:5}")
    private int SLS_FTP_UPLOAD_QUEUE_OFFER_TIMEOUT;

    @Value("${sls.ftp.upload.queue.size:1000}")
    private int SLS_FTP_UPLOAD_QUEUE_SIZE;

    @Value("${sls.ftp.upload.queue.consumer-num:3}")
    private int SLS_FTP_UPLOAD_QUEUE_CONSUMER_NUM;

    @Value("${sls.ftp.clear.days:7}")
    private int SLS_FTP_CLEAR_DAYS;

    @Value("${sls.bpm.token.url}")
    private String BPM_TOKEN_URL;

    @Value("${sls.bpm.token.username}")
    private String BPM_TOKEN_USER;

    @Value("${sls.bpm.token.password}")
    private String BPM_TOKEN_PASSWORD;

    private void initFtpClient(FTPClient ftpClient, String host, int port, String username, String password, String mode) throws IOException {
        ftpClient.connect(host, port);
        ftpClient.login(username, password);
        ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
        // ??????Linux??????
        FTPClientConfig conf = new FTPClientConfig( FTPClientConfig.SYST_UNIX);
        ftpClient.configure(conf);
        switch (mode) {
            case SLS_FTP_MODE_PORT:
                ftpClient.enterLocalActiveMode();//?????????????????????
                break;
            case SLS_FTP_MODE_PASV:
                ftpClient.enterLocalPassiveMode();//?????????????????????
                ftpClient.setRemoteVerificationEnabled(false);
                break;
            default:
        }
        int reply = ftpClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            LOGGER.error("#======>>> FTP ????????? {} ??????????????????!!!", host);
            return;
        }
        LOGGER.debug("#======>>> FTP {} ??????????????????{}", host, reply);
        if (FTPReply.isPositiveCompletion(ftpClient.sendCommand("OPTS UTF8", "ON"))) {
            LOCAL_CHARSET = "UTF-8";
        }
        ftpClient.setControlEncoding(LOCAL_CHARSET);
    }

    private class LeakScanFileHandel implements Runnable {

        private String ftpFilePath;
        private File zipFile;
        private String dateStr;
        private String dateToClearStr;
        private String token;

        public LeakScanFileHandel(String ftpFilePath, File localFile, final String dateStr, final String dateToClearStr, final String token)
        {
            this.ftpFilePath = ftpFilePath;
            this.zipFile = localFile;
            this.dateStr = dateStr;
            this.dateToClearStr = dateToClearStr;
            this.token = token;
        }

        @Override
        public void run() {
            final String fileName = zipFile.getName();
            final String bizLine = fileName.substring(0, fileName.indexOf("."));
            try {
                final String scanId = securityLeakScanBiz.persistScanRecords(zipFile, dateStr, ftpFilePath, fileName);
                // ??????????????????????????????????????????
//                securityLeakScanBiz.clearPastScanRecords(bizLine, dateStrToClear);
                // ????????????????????????????????? FTP
                final String zipFileDir = zipFile.getParentFile().getPath();
                final String unzipedFileDir = zipFileDir + SYSTEM_LINE_SEPRATOR + bizLine;
                // ??????????????? && ???????????????????????????????????? FTP ?????? && ???????????? FTP ??????????????????
                List<File> filesToUpload = Lists.newArrayList();
                filesToUpload.add(zipFile);
                filesToUpload.add(new File(unzipedFileDir));
                SecurityLeakScanUploadFileSubstance uploadSubstance = new SecurityLeakScanUploadFileSubstance();
                uploadSubstance.setZipFileName(fileName);
                uploadSubstance.setBizLine(bizLine);
                uploadSubstance.setDateStr(dateStr);
                uploadSubstance.setDateStrToClear(dateToClearStr);
                uploadSubstance.setFileList(filesToUpload);
                taskExecutor.submit(new FileUploader(uploadSubstance));
                // ?????? Bpm ?????? WebService ???????????? ID
                SecurityLeakSacnBpmSubstance substance = new SecurityLeakSacnBpmSubstance();
                substance.setScanId(scanId);
                substance.setAttachFile(zipFile);
                substance.setToken(token);
                bpmSubmitTask.submitBpmSubstance(substance);
            } catch (Exception e) {
            	LOGGER.error("?????????????????????: {} ???????????? !", fileName , e);
            }
        }
    }

    private class FileUploader implements Runnable {

        private SecurityLeakScanUploadFileSubstance substance;

        public FileUploader(SecurityLeakScanUploadFileSubstance substance) {
            this.substance = substance;
        }

        private void upload(FTPClient ftpClient, File file) throws IOException {
            if (file.isDirectory()) {
                String directory = new String(file.getName().getBytes(LOCAL_CHARSET), SERVER_CHARSET);
                if (!ftpClient.changeWorkingDirectory(directory)) {
                    ftpClient.makeDirectory(directory);
                    ftpClient.changeWorkingDirectory(directory);
                }
                String[] files = file.list();
                if (null != files && files.length > 0) {
                    for (String fstr : files) {
                        File innerFile = new File(file.getPath() + SYSTEM_LINE_SEPRATOR + fstr);
                        if (innerFile.isDirectory()) {
                            upload(ftpClient, innerFile);
                            ftpClient.changeToParentDirectory();
                        } else {
                            FileInputStream input = null;
                            try {
                                File file2 = new File(file.getPath() + SYSTEM_LINE_SEPRATOR + fstr);
                                input = new FileInputStream(file2);
                                ftpClient.storeFile(new String(file2.getName().getBytes(LOCAL_CHARSET), SERVER_CHARSET), input);
                            } catch (IOException e) {
                            	LOGGER.error("??????????????????!", e);
                            } finally {
                                IOUtils.closeQuietly(input);
                            }
                        }
                    }
                }
            } else {
                FileInputStream input = null;
                try {
                    File file2 = new File(file.getPath());
                    input = new FileInputStream(file2);
                    ftpClient.storeFile(new String(file2.getName().getBytes(LOCAL_CHARSET), SERVER_CHARSET), input);
                } catch (IOException e) {
                	LOGGER.error("??????????????????!", e);
                } finally {
                    IOUtils.closeQuietly(input);
                }
            }
        }

        private void ftpUpload(final String dateStr, final List<File> files) {
            FTPClient ftpClient = new FTPClient();
            LOGGER.debug("#=========>>> FTP META INFO:\n #=========> URL: {}, PORT: {}, USER: {}, PASSWORD: {}",
                    SLS_FTP_UPLOAD_HOST, SLS_FTP_UPLOAD_PORT, SLS_FTP_UPLOAD_USER, SLS_FTP_UPLOAD_PASSWORD);
            try {
                initFtpClient(
                        ftpClient,
                        SLS_FTP_UPLOAD_HOST,
                        SLS_FTP_UPLOAD_PORT,
                        SLS_FTP_UPLOAD_USER,
                        SLS_FTP_UPLOAD_PASSWORD,
                        SLS_FTP_UPLOAD_MODE
                );
                if (ftpClient.changeWorkingDirectory(SLS_FTP_UPLOAD_PATH)) {
                    if (!ftpClient.changeWorkingDirectory(dateStr)) {
                        ftpClient.makeDirectory(dateStr);
                        ftpClient.changeWorkingDirectory(dateStr);
                    }
                    for (File file : files) {
                    	LOGGER.info("#=== ????????????: {} ", file.getName());
                        upload(ftpClient, file);
                    }
                } else {
                	LOGGER.error("#======>>> FTP ????????????: {} ????????????! ????????????????????????????????????????????????!",
                            SLS_FTP_UPLOAD_PATH);
                }
            } catch (IOException e) {
            	LOGGER.error("FTP ????????????! ftp host: {} ", SLS_FTP_UPLOAD_HOST, e);
            } finally {
                if (ftpClient.isConnected()) {
                    try {
                        ftpClient.disconnect();
                    } catch (IOException e) {
                    	LOGGER.error("??????FTP????????????!", e);
                    }
                }
            }
        }

        private void ftpDelete(String dateStr, String bizLine, String zipFileName) {
            FTPClient ftpClient = new FTPClient();
            try {
                initFtpClient(
                        ftpClient,
                        SLS_FTP_UPLOAD_HOST,
                        SLS_FTP_UPLOAD_PORT,
                        SLS_FTP_UPLOAD_USER,
                        SLS_FTP_UPLOAD_PASSWORD,
                        SLS_FTP_UPLOAD_MODE
                );
                if (ftpClient.changeWorkingDirectory(SLS_FTP_UPLOAD_PATH)) {
                    if (!ftpClient.changeWorkingDirectory(dateStr)) {
                    	LOGGER.info("#======>>> FTP ????????????: {} ????????????! ????????????????????????????????????????????????!",
                                SLS_FTP_UPLOAD_PATH + "/" + dateStr);
                        return;
                    }
                    ftpClient.changeToParentDirectory();
                    // ??????????????????
                    String bizLineDir = SLS_FTP_UPLOAD_PATH + "/" + dateStr + "/" + bizLine;
                    delete(ftpClient, bizLineDir);
                    String zipFile = SLS_FTP_UPLOAD_PATH + "/" + dateStr + "/"+ zipFileName;
                    if (ftpClient.deleteFile(new String(zipFile.getBytes(LOCAL_CHARSET), SERVER_CHARSET))) {
                    	LOGGER.info("??????????????????????????????{}", zipFile);
                    } else {
                    	LOGGER.error("??????????????????: {} ??????!", zipFile);
                    }
                    List<String> ftpFileList = Lists.newArrayList();
                    FTPFile[] ftpFiles = ftpClient.listFiles();
                    for (FTPFile file : ftpFiles) {
                        String name = file.getName();
                        if (".".equals(name) || "..".equals(name)) {
                            continue;
                        }
                        ftpFileList.add(name);
                    }
                    if (CollectionUtils.isEmpty(ftpFileList)) {
                        if (ftpClient.changeToParentDirectory()) {
                            String datePath = SLS_FTP_UPLOAD_PATH + "/" + dateStr;
                            if (ftpClient.removeDirectory(new String(datePath.getBytes(LOCAL_CHARSET), SERVER_CHARSET))) {
                            	LOGGER.info("????????????????????????: {} ", datePath);
                            } else {
                            	LOGGER.error("??????????????? {} ??????!", datePath);
                            }
                        }
                    }
                } else {
                	LOGGER.error("#======>>> FTP ????????????: {} ????????????! ????????????????????????????????????????????????!",
                            SLS_FTP_UPLOAD_PATH);
                }
            } catch (IOException e) {
            	LOGGER.error("FTP ????????????! ftp host: {}", SLS_FTP_UPLOAD_HOST, e);
            } finally {
                if (ftpClient.isConnected()) {
                    try {
                        ftpClient.disconnect();
                    } catch (IOException e) {
                    	LOGGER.error("??????FTP????????????!", e);
                    }
                }
            }
        }

        private void delete(FTPClient ftpClient, String path) throws IOException {
            String ftpPath = new String(path.getBytes(LOCAL_CHARSET), SERVER_CHARSET);
            FTPFile[] ftpFiles = ftpClient.listFiles(ftpPath);
            if (ftpFiles != null && ftpFiles.length > 0) {
                for (FTPFile ftpFile : ftpFiles) {
                    String ftpFileName = ftpFile.getName();
                    if (".".equals(ftpFileName) || "..".equals(ftpFileName)) {
//                        ftpClient.removeDirectory(new String((path + "/" + ftpFileName).getBytes(LOCAL_CHARSET), SERVER_CHARSET));
                        continue;
                    }
                    if (ftpFile.isDirectory()) {
                        delete(ftpClient, path + "/" + ftpFileName);
                        // ?????????????????????
                        String parent = path.substring(0, path.lastIndexOf("/"));
                        if (ftpClient.changeWorkingDirectory(new String(parent.getBytes(LOCAL_CHARSET), SERVER_CHARSET))) {
                        	LOGGER.info("????????????: {} ?????? !", parent);
                        } else {
                        	LOGGER.error("????????????: {} ??????!", parent);
                        }
                    } else {
                        String filePath = path + "/" + ftpFile.getName();
                        if (!ftpClient.deleteFile(new String(filePath.getBytes(LOCAL_CHARSET), SERVER_CHARSET))) {
                        	LOGGER.error("??????????????????: {} ??????!", filePath);
                        } else {
                        	LOGGER.info("?????????????????????{}", filePath);
                        }
                    }
                }
            }
            String parent = path.substring(0, path.lastIndexOf("/"));
            String parentPath = new String(parent.getBytes(LOCAL_CHARSET), SERVER_CHARSET);
            if (ftpClient.changeWorkingDirectory(parentPath)) { // ?????????????????????
                if (!ftpClient.removeDirectory(new String(path.getBytes(LOCAL_CHARSET), SERVER_CHARSET))) {
                	LOGGER.error("???????????????{} ??????!", path);
                } else {
                	LOGGER.info("???????????????{} ??????!", path);
                }
            } else {
            	LOGGER.error("????????????: {} ??????!", parent);
            }
        }

        @Override
        public void run() {
            try {
                String dateStr = substance.getDateStr();
                List<File> filesToUpload = substance.getFileList();
                ftpUpload(dateStr, filesToUpload);
                // ??????IP????????????
//                    securityLeakScanBiz.fillScanReportHtmlPath(scanId, dateStr, bizLine);
                // ?????????????????????????????????
                ftpDelete(substance.getDateStrToClear(), substance.getBizLine(), substance.getZipFileName());
            } catch (Exception e) {
            	LOGGER.error("FileUploader {} error !", Thread.currentThread().getName(), e);
            }
        }
    }
}
