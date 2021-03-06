/**
 *
 */
package com.aspire.mirror.theme.server.biz.handler;

import com.aspire.mirror.common.constant.Constant;
import com.aspire.mirror.common.util.DateUtil;
import com.aspire.mirror.theme.api.dto.model.BizThemeDimData;
import com.aspire.mirror.theme.api.dto.model.ThemeDataDTO;
import com.aspire.mirror.theme.server.dao.BizThemeDao;
import com.aspire.mirror.theme.server.dao.BizThemeDimDao;
import com.aspire.mirror.theme.server.dao.po.BizTheme;
import com.aspire.mirror.theme.server.dao.po.BizThemeDim;
import com.google.common.collect.Lists;
import io.krakens.grok.api.Grok;
import io.krakens.grok.api.GrokCompiler;
import io.krakens.grok.api.Match;
import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author lupeng
 */
public class MergeTask implements Callable<String> {

    private static final Logger logger = Logger.getLogger(MergeTask.class);

    private BizThemeDao bizThemeDao;

    private ElasticsearchHandler elasticsearchHandler;

    private BizThemeDimDao bizThemeDimDao;

    private ThemeDataDTO themeDataDTO;

    public MergeTask(ThemeDataDTO themeDataDTO, ElasticsearchHandler elasticsearchHandler, BizThemeDao bizThemeDao,
                     BizThemeDimDao bizThemeDimDao) {
        this.themeDataDTO = themeDataDTO;
        this.elasticsearchHandler = elasticsearchHandler;
        this.bizThemeDao = bizThemeDao;
        this.bizThemeDimDao = bizThemeDimDao;
    }


    @Override
    public String call() throws Exception {
        String res = null;
        try {
            BizTheme bizTheme = new BizTheme();
            res = invokeData(themeDataDTO, bizTheme);
            if (!StringUtils.isEmpty(res)) {
                elasticsearchHandler.inserEs(themeDataDTO, null, null, null, res);
                return res;
            }
            for (String data : themeDataDTO.getDatas()) {
                List<BizThemeDimData> themeDimDataList = Lists.newArrayList();
                res = validDataDim(data, bizTheme, themeDimDataList);
                elasticsearchHandler.inserEs(themeDataDTO, data, themeDimDataList, bizTheme, res);
            }
        } catch (Exception e) {
            logger.error("?????????????????????", e);
            return "?????????????????????";
        }
        return res;
    }

    public String validDataDim(String data, BizTheme bizTheme, List<BizThemeDimData>
            themeDimDataList) {
//
//        map.put("theme_code", themeCode);

        // Aggregate_Flag
//        String aggregateFlag = data.getAggregateFlag();
//        if (StringUtils.isEmpty(aggregateFlag) || !aggregateFlag.equals("0")) {
//            return "Aggregate_Flag:" + (StringUtils.isEmpty(aggregateFlag) ? "" : aggregateFlag) + "?????????0";
//        }
//        tmpJkzbDataDto.setAggregateFlag(aggregateFlag);

        // JCZB_Dim_Value
        List<BizThemeDim> bizThemeDimList = bizThemeDimDao.selectByThemeId(bizTheme.getThemeId());
        StringBuffer themeRegBuffer = new StringBuffer();
//        Map<String, BizThemeDim> dimCodeMap = Maps.newHashMap();
        for (BizThemeDim bizThemeDim : bizThemeDimList) {
            if (bizThemeDim.getMatchFlag().equals("1")) {
//                dimCodeMap.put(bizThemeDim.getDimCode(), bizThemeDim);
                themeRegBuffer.append("%{").append(bizThemeDim.getDimReg()).append(":").append(bizThemeDim.getDimCode
                        ()).append("}");
            } else {
                //?????????????????????
                for (String UnescapedReg : Constant.ESCAPE_DIM_REG_ARRAY) {
                    if (UnescapedReg.equals(bizThemeDim.getDimReg())) {
                        bizThemeDim.setDimReg("\\" + bizThemeDim.getDimReg());
                        break;
                    }
                }
                themeRegBuffer.append(bizThemeDim.getDimReg());
            }
        }
        GrokCompiler grokCompiler = GrokCompiler.newInstance();
        grokCompiler.registerDefaultPatterns();
        final Grok grok = grokCompiler.compile(themeRegBuffer.toString());
        Match grokMatch = grok.match(data);
        Map<String, Object> resultMap = grokMatch.captureFlattened();
        for (BizThemeDim bizThemeDim : bizThemeDimList) {
            if (!bizThemeDim.getMatchFlag().equals("2")) {
                if (resultMap.get(bizThemeDim.getDimCode()) == null) {
                    return "???????????????" + bizThemeDim.getDimName() + "?????????????????????";
                } else {
                    BizThemeDimData bizThemeDimData = new BizThemeDimData();
                    bizThemeDimData.setDimType(bizThemeDim.getDimType());
                    bizThemeDimData.setDimValue(resultMap.get(bizThemeDim.getDimCode()).toString());
                    bizThemeDimData.setDimCode(bizThemeDim.getDimCode());
                    bizThemeDimData.setDimName(bizThemeDim.getDimName());
                    // ????????????????????????????????????
                    if (bizThemeDim.getMatchFlag().equals("1")) {
                        String pattern = "%{" + bizThemeDim.getDimReg() + ":" + bizThemeDim
                                .getDimCode() + "}";
                        bizThemeDimData.setPattern(pattern);
                    }
                    themeDimDataList.add(bizThemeDimData);
                }
            }
        }
//        List<BizThemeDimData> lstJczbDims = Lists.newArrayList();
//        String dimValues = data;
//        if (StringUtils.isEmpty(dimValues)) {
//            return "???????????????????????????";
//        }
//        String[] dims = bizTheme.getDimIds().split(":");
//        String[] dimValusArray = dimValues.split(";");
//        for (String val : dimValusArray) {
//            String[] keyPair = val.split(":");
//            if (keyPair.length != 2) {
//                return "JCZB_Dim_Value:" + dimValues + "??????????????????";
//            }
//            if (!isInt(keyPair[0])) {
//                return "JCZB_Dim_Value:" + dimValues + "?????????[" + keyPair[0] + "]????????????";
//            }
//            BizThemeDimData jkzbDimDto = new BizThemeDimData();
//            jkzbDimDto.setDimCode(keyPair[0]);
//            jkzbDimDto.setDimValue(keyPair[1]);
//            BizThemeDim dim = bizThemeDimDao.selectByPrimaryKey(keyPair[0]);
//            if (null != dim) {
//                jkzbDimDto.setDimType(dim.getDimType());
//            } else {
//                return "JCZB_Dim_Value:" + dimValues + "?????????[" + keyPair[0] + "]????????????";
//            }
//            lstJczbDims.add(jkzbDimDto);
//        }
//        if (dims.length != dimValusArray.length) {
//            return "JCZB_Dim_Value:" + dimValues + "????????????????????????";
//        }
//        for (BizThemeDimData jd : lstJczbDims) {
//            if (jd.getDimCode().equals("10")) {
//                if (null == DateUtil.getTime(jd.getDimValue())) {
//                   return "JCZB_Dim_Value:" + dimValues + "?????????[10]?????????????????????";
//                }
//            } else {
//                //????????????????????????
//                if (jd.getDimType().equals("1")) {
//                    if (!isDouble(jd.getDimValue())) {
//                        return "JCZB_Dim_Value:" + dimValues + "?????????[" + jd.getDimCode() + "]??????????????????";
//                    }
//                }
//                //????????????????????????
//                if (jd.getDimType().equals("3")) {
//                    if (null == DateUtil.getTime(jd.getDimValue())) {
//                        return "JCZB_Dim_Value:" + dimValues + "?????????[" + jd.getDimCode() + "]????????????????????????";
//                    }
//                }
//            }
//
//        }
//        for (String dim : dims) {
//            boolean match = false;
//            for (BizThemeDimData jd : lstJczbDims) {
//                if (dim.equals(jd.getDimCode())) {
//                    match = true;
//                    break;
//                }
//            }
//            if (!match) {
//                return "JCZB_Dim_Value:" + dimValues + "????????????????????????";
//            }
//        }
//        tmpJkzbDataDto.setJczbDimValue(dimValues);
//        tmpJkzbDataDto.setLstJczbDim(lstJczbDims);
//
//        // JCZB_Value
//        String jczbValue = data.getJczbValue();
//        if (StringUtils.isEmpty(jczbValue)) {
//            return "JCZB_Value??????????????????";
//        }
//        if (bizTheme.getValueType().equals("0") && !isDouble(jczbValue)) {
//            return "JCZB_Value:" + jczbValue + "??????????????????";
//        }
//        tmpJkzbDataDto.setJczbValue(jczbValue);
//
//        // Log_Time
//        String logTime = data.getLogTime();
//        if (StringUtils.isEmpty(logTime)) {
//            return "Log_Time??????????????????";
//        }
//        if (null == DateUtil.getTime(logTime)) {
//            return "Log_Time:" + logTime + "?????????????????????";
//        }
//        tmpJkzbDataDto.setLogTime(logTime);
        return null;
    }

//    public static boolean isDouble(String in) {
//        try {
//            Pattern pattern = Pattern.compile("(-)?\\d{1,3}(,\\d{3})*(.\\d+)?$");
//            Matcher isNum = pattern.matcher(in);
//            if (isNum.matches()) {
//                in = in.replace(",", "");
//            }
//            Double.parseDouble(in);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }
//
//    public static boolean isInt(String in) {
//        try {
//            Integer.parseInt(in);
//            return true;
//        } catch (Exception e) {
//            return false;
//        }
//    }

    public String invokeData(ThemeDataDTO themeDataDTO, BizTheme bizTheme) {


        String themeCode = themeDataDTO.getThemeCode();
        if (StringUtils.isEmpty(themeCode)) {
            return "ThemeCode???????????????";
        }
        // JCZB_Code
        BeanUtils.copyProperties(bizThemeDao.selectByThemeCode(themeCode), bizTheme);

        // ????????????SysCode
        if (bizTheme.getObjectType().equals(Constant.OBJECT_TYPE_BIZ_SYS)) {
            String sysCode = themeDataDTO.getSysCode();
            if (StringUtils.isEmpty(sysCode)) {
                return "SysCode???????????????";
            }
        } else if (bizTheme.getObjectType().equals(Constant.OBJECT_TYPE_DEVICE)) {
            String deviceIp = themeDataDTO.getDeviceIp();
            if (StringUtils.isEmpty(deviceIp)) {
                return "DeviceIp???????????????";
            }
        }

        // TODO ????????????IP???????????????????????????
        // ??????
        if (null == bizTheme || (bizTheme.getDeleteFlag() != null && bizTheme.getDeleteFlag().equals("1"))) {
            return "JCZB_Code:\"" + themeCode + "\"????????????????????????";
        }
        // ????????????Time
        String time = themeDataDTO.getTime();
        if (StringUtils.isEmpty(time)) {
            return "Time???????????????";
        }
        Date date = DateUtil.getDate(time, DateUtil.DATE_TIME_FORMAT);
        if (null == date) {
            return "Time:\"" + time + "\"???????????????";
        }
        // ????????????Data
        List<String> data = themeDataDTO.getDatas();
        if (null == data || data.isEmpty()) {
            return "Data?????????????????????";
        }
        return null;
    }
}
