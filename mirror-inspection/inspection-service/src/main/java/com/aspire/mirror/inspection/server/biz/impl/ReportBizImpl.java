package com.aspire.mirror.inspection.server.biz.impl;

import com.aspire.mirror.common.entity.Page;
import com.aspire.mirror.common.entity.PageRequest;
import com.aspire.mirror.common.util.PageUtil;
import com.aspire.mirror.inspection.api.dto.ReportItemExt;
import com.aspire.mirror.inspection.api.dto.ReportItemValue;
import com.aspire.mirror.inspection.api.dto.model.ReportDTO;
import com.aspire.mirror.inspection.api.dto.model.ReportItemDTO;
import com.aspire.mirror.inspection.api.dto.model.ReportTaskDTO;
import com.aspire.mirror.inspection.server.biz.ReportBiz;
import com.aspire.mirror.inspection.server.clientservice.payload.TriggersDetailResponse;
import com.aspire.mirror.inspection.server.controller.authcontext.RequestAuthContext;
import com.aspire.mirror.inspection.server.dao.ReportDao;
import com.aspire.mirror.inspection.server.dao.ReportItemDao;
import com.aspire.mirror.inspection.server.dao.TaskObjectDao;
import com.aspire.mirror.inspection.server.dao.po.Report;
import com.aspire.mirror.inspection.server.dao.po.ReportItem;
import com.aspire.mirror.inspection.server.dao.po.ReportResultStatistic;
import com.aspire.mirror.inspection.server.dao.po.ReportTask;
import com.aspire.mirror.inspection.server.dao.po.transform.ReportTransformer;
import com.googlecode.aviator.AviatorEvaluator;
import com.googlecode.aviator.Expression;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.ibatis.session.RowBounds;
import org.mybatis.spring.SqlSessionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * inspection_report??????????????????
 * <p>
 * ????????????:  ?????????????????????
 * ???:       com.aspire.mirror.inspection.server.biz.impl
 * ?????????:    ReportBizImpl.java
 * ?????????:    inspection_report????????????????????????
 * ?????????:    ZhangSheng
 * ????????????:  2018-07-27 13:48:08
 */
@Service
public class ReportBizImpl implements ReportBiz {
    private static final String CONTAIN_PREFIX = "::contains::";
    private static final String CRON_PREFIX = "::cron::";

    /**
     * ????????????
     *
     * @param reportDTO inspection_reportDTO??????
     * @return int ??????????????????
     */
    public int insert(final ReportDTO reportDTO) {
        if (null == reportDTO) {
            LOGGER.error("method[insert] param[reportDTO] is null");
            throw new RuntimeException("param[reportDTO] is null");
        }
        Report report = ReportTransformer.toPo(reportDTO);

        return reportDao.insert(report);
    }

    /**
     * ????????????inspection_report??????
     *
     * @param listReportDTO inspection_reportDTO????????????
     * @return int ??????????????????
     */
    public int insertByBatch(final List<ReportDTO> listReportDTO) {
        if (CollectionUtils.isEmpty(listReportDTO)) {
            LOGGER.error("method[insertByBatch] param[listReportDTO] is null");
            throw new RuntimeException("param[listReportDTO] is null");
        }
        List<Report> listReport = ReportTransformer.toPo(listReportDTO);
        return reportDao.insertByBatch(listReport);
    }

    /**
     * ????????????????????????
     *
     * @param reportId ??????
     * @return int ??????????????????
     */
    public int deleteByPrimaryKey(final String reportId) {
        if (StringUtils.isEmpty(reportId)) {
            LOGGER.error("method[eleteByPrimaryKey] param[reportId] is null");
            throw new RuntimeException("param[reportId] is null");
        }
        return reportDao.deleteByPrimaryKey(reportId);
    }

    /**
     * ????????????????????????????????????
     *
     * @param reportIdArrays ????????????
     * @return int ??????????????????
     */
    public int deleteByPrimaryKeyArrays(final String[] reportIdArrays) {
        if (ArrayUtils.isEmpty(reportIdArrays)) {
            LOGGER.error("method[deleteByPrimaryKeyArrays] param[reportIdArrays] is null");
            throw new RuntimeException("param[reportIdArrays] is null");
        }
        return reportDao.deleteByPrimaryKeyArrays(reportIdArrays);
    }

    /**
     * ????????????????????????
     *
     * @param reportDTO inspection_reportDTO??????
     * @return int ??????????????????
     */
    public int delete(final ReportDTO reportDTO) {
        if (null == reportDTO) {
            LOGGER.error("method[delete] param[reportDTO] is null");
            throw new RuntimeException("param[reportDTO] is null");
        }
        Report report = ReportTransformer.toPo(reportDTO);
        return reportDao.delete(report);
    }

    /**
     * ?????????????????????????????????
     *
     * @param reportDTO inspection_reportDTO??????
     * @return int ????????????
     */
    public int updateByPrimaryKeySelective(final ReportDTO reportDTO) {
        if (null == reportDTO) {
            LOGGER.error("method[updateByPrimaryKey] param[reportDTO] is null");
            throw new RuntimeException("param[reportDTO] is null");
        }
        Report report = ReportTransformer.toPo(reportDTO);
        return reportDao.updateByPrimaryKeySelective(report);
    }

    /**
     * ????????????????????????
     *
     * @param reportDTO inspection_reportDTO??????
     * @return int ????????????
     */
    public int updateByPrimaryKey(final ReportDTO reportDTO) {
        if (null == reportDTO) {
            LOGGER.error("method[updateByPrimaryKey] param[reportDTO] is null");
            throw new RuntimeException("param[reportDTO] is null");
        }
        if (StringUtils.isEmpty(reportDTO.getReportId())) {
            LOGGER.warn("method[updateByPrimaryKey] param[reportId] is null");
            throw new RuntimeException("param[reportId] is null");
        }
        Report report = ReportTransformer.toPo(reportDTO);
        return reportDao.updateByPrimaryKey(report);
    }


    /**
     * ????????????????????????
     *
     * @param reportIdArrays ????????????
     * @return List<ReportDTO> ??????????????????
     */
    public List<ReportDTO> selectByPrimaryKeyArrays(final String[] reportIdArrays) {
        if (ArrayUtils.isEmpty(reportIdArrays)) {
            LOGGER.warn("method[selectByPrimaryKeyArrays] param[reportIdArrays] is null");
            return Collections.emptyList();
        }
        List<Report> listReport = reportDao.selectByPrimaryKeyArrays(reportIdArrays);
        return ReportTransformer.fromPo(listReport);
    }

    /**
     * ??????dto??????????????????
     *
     * @param reportDTO inspection_reportDTO??????
     * @return List<Report>  ????????????
     */
    public List<ReportDTO> select(final ReportDTO reportDTO) {
        if (null == reportDTO) {
            LOGGER.warn("select Object reportDTO is null");
            return Collections.emptyList();
        }
        Report report = ReportTransformer.toPo(reportDTO);
        List<Report> listReport = reportDao.select(report);
        return ReportTransformer.fromPo(listReport);
    }

    /**
     * ??????DTO??????????????????
     *
     * @param reportDTO inspection_reportDTO??????
     * @return int ????????????
     */
   /* public int selectCount(final ReportDTO reportDTO){
        if(null == reportDTO){
            LOGGER.warn("selectCount Object reportDTO is null");
        }
        Report report = ReportTransformer.toPo(reportDTO);
        return reportDao.selectCount(report);
    }*/
//--------------------------------------------------??????????????????---------------------------------------------------------->

    /**
     * ??????????????????
     *
     * @param reportId ??????
     * @return ReportDTO ????????????
     */
    public ReportDTO selectByPrimaryKey(final String reportId) {
        if (StringUtils.isEmpty(reportId)) {
            LOGGER.warn("method[selectByPrimaryKey] param[reportId] is null");
            return new ReportDTO();
        }
        Report report = reportDao.selectByPrimaryKey(reportId);

        return ReportTransformer.fromPo(report);
    }

    /**
     * ??????????????????????????????????????????
     */
  /*  @Override
	public List<ReportDTO> pageList(final PageRequest pageRequest) {
    	Page page =PageUtil.convert(pageRequest);
    	String queryName="com.aspire.mirror.inspection.server.dao.ReportDao.pageList";
    	Integer pageNo =page.getPageNo();
		Integer pageSize =page.getPageSize();
		Integer startIndex = (pageNo-1)*pageSize;
		List<Report> reports=null;
		try {
			reports=sessionTemplate.selectList(queryName,page,new RowBounds(startIndex, pageSize));
		}catch (Exception e) {
			LOGGER.warn("com.aspire.mirror.inspection.server.biz.impl.ReportBizImpl--->pageList Object reportDTO is
			null");
		}
    	List<ReportDTO> reportDTOs =ReportTransformer.fromPo(reports);
		return reportDTOs;
	}*/

    /**
     * ??????????????????????????????????????????
     */
    @Override
    public int pageCount(final PageRequest pageRequest) {
        Page page = PageUtil.convert(pageRequest);
        int count = 0;
        try {
            count = reportDao.selectCount(page);
        } catch (Exception e) {
            LOGGER.warn("com.aspire.mirror.inspection.server.biz.impl.ReportBizImpl--->pageCount Object reportDTO is " +
                    "null");
        }
        return count;
    }

    /**
     * key????????????????????????. <br/>
     * <p>
     * ????????? pengguihua
     *
     * @param itemKey
     * @param expression
     * @return
     */
    private Pair<String, String> filterSpecialChars(String itemKey, String expression) {
        String originalKey = itemKey;
        String filterKey = "k_" + itemKey.replaceAll("\\W", "");
        String filterExpress = expression.replace(originalKey, filterKey);
        return Pair.of(filterKey, filterExpress);
    }

    @Override
    @Transactional
    public void regenerate(String reportId, Map<String, String> triggerMap) {
        List<ReportItem> reportItemList = reportItemDao.selectByReportId(reportId);
        if (!CollectionUtils.isEmpty(reportItemList)) {
            for (ReportItem reportItem : reportItemList) {
                ReportItemExt reportItemExt = new ReportItemExt();
                reportItemExt.setReportItemId(reportItem.getReportItemId());
                reportItemExt.setExpression(triggerMap.get(reportItem.getItemId()));
                reportItemDao.updateReportItemExtByReportItemId(reportItemExt);
                if (triggerMap.get(reportItem.getItemId()) != null && !CollectionUtils.isEmpty(reportItem.getReportItemValueList())) {
                    // ?????????????????????
                    int containIdx = triggerMap.get(reportItem.getItemId()).indexOf(CONTAIN_PREFIX);
                    int cronIndex = triggerMap.get(reportItem.getItemId()).indexOf(CRON_PREFIX);
                    for (ReportItemValue reportItemValue : reportItem.getReportItemValueList()) {
                        if (StringUtils.isEmpty(reportItemValue.getResultValue())) {
                            //?????????
                            reportItemValue.setResultStatus("2");
                        } else {
                            reportItemValue.setResultStatus(ReportItemDTO.STATUS_NORMAL);
                            if (containIdx >= 0) {
                                String containStr = triggerMap.get(reportItem.getItemId()).substring(containIdx + CONTAIN_PREFIX.length());
                                //???????????????????????????
                                String[] containArray = containStr.split(",");
                                for (String containItem : containArray) {
                                    if (reportItemValue.getResultValue().contains(containItem)) {
                                        reportItemValue.setResultStatus(ReportItemDTO.STATUS_EXCEPTION);
                                        break;
                                    }
                                }
//                                if (reportItemValue.getResultValue().contains(containStr)) {
//                                    reportItemValue.setResultStatus("1");
//                                } else {
//                                    reportItemValue.setResultStatus("0");
//                                }
                            } else if (cronIndex >= 0) {
                                reportItemValue.setResultStatus(ReportItemDTO.STATUS_NORMAL);
                                String cronStr = triggerMap.get(reportItem.getItemId()).substring(cronIndex + CRON_PREFIX.length());
                                Pattern p = Pattern.compile(cronStr);
                                Matcher m = p.matcher(reportItemValue.getResultValue());
                                while (m.find()) {
                                    reportItemValue.setResultStatus(ReportItemDTO.STATUS_EXCEPTION);
                                }
                            } else {
                                //???????????????  ?????????
                                Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");
                                String sourceValue = reportItemValue.getResultValue().trim();
                                Boolean isNum = true;
                                if (!pattern.matcher(sourceValue).matches()) {
                                    // ?????????????????????????????????
                                    sourceValue = String.format("'%s'", sourceValue);
                                    isNum = false;
                                }

                                String[] expressionValueArray = TriggersDetailResponse.getExpressionValue(triggerMap.get(reportItem.getItemId())).split(",");
                                String match = TriggersDetailResponse.getMatch(triggerMap.get(reportItem.getItemId()));
                                if (match.equals("!=")) {
                                    Boolean isMatch = true;
                                    for (String expressionVlaue : expressionValueArray) {
                                        if (!isNum) {
                                            expressionVlaue = String.format("'%s'", expressionVlaue);
                                        }
                                        Expression compiledExp = AviatorEvaluator.compile(sourceValue + match + expressionVlaue);
                                        if (!Boolean.class.cast(compiledExp.execute())) {
                                            isMatch = false;
                                            break;
                                        }
                                    }
                                    if (isMatch) {
                                        reportItemValue.setResultStatus(ReportItemDTO.STATUS_EXCEPTION);
                                    }
                                } else {
                                    for (String expressionVlaue : expressionValueArray) {
                                        if (!isNum) {
                                            expressionVlaue = String.format("'%s'", expressionVlaue);
                                        }
                                        Expression compiledExp = AviatorEvaluator.compile(sourceValue + match + expressionVlaue);
                                        boolean isMatch = Boolean.class.cast(compiledExp.execute());
                                        if (isMatch) {
                                            reportItemValue.setResultStatus(ReportItemDTO.STATUS_EXCEPTION);
                                            break;
                                        }
                                    }
                                }
//                                Expression compiledExp = AviatorEvaluator.compile(reportItemValue.getResultValue().trim() + triggerMap.get(reportItem.getItemId()));
//                                boolean isMatch = Boolean.class.cast(compiledExp.execute());

//                                if (isMatch) {
//                                    reportItemValue.setResultStatus("1");
//                                } else {
//                                    reportItemValue.setResultStatus("0");
//                                }
                            }
                        }
                        reportItemDao.updateReportItemValueStatus(reportItemValue);
                    }
                    reportItemDao.updateStatusByUniqueKeys(reportItem);
                }
            }
            Report report = new Report();
            report.setReportId(reportId);
            ReportResultStatistic statisticValue = reportItemDao.selectNumStatistic(reportId);
            String result = "????????????{0}?????????????????????{1}??????????????????{2} ?????????<span style=\"color: red;font-weight: 800;" +
                    "\">{3}</span>????????????<span " +
                    "style=\"color: green;font-weight: 800;\">{4}</span>???{5}????????????,{6}???????????????";
            result = MessageFormat.format(result, statisticValue.getDeviceNum(), statisticValue.getItemNum(), statisticValue.getResultNum(),
                    statisticValue.getExceptionNum(), statisticValue.getNormalNum(),
                    statisticValue.getNoResultNum(), statisticValue.getArtificialJudgmentNum());
            report.setResult(result);
            reportDao.updateByPrimaryKeySelective(report);
        }
    }

    public static void main(String[] args) {
//        Expression compiledExp = AviatorEvaluator.compile("??????==??????");
//        String aa = "027";
//        Pattern pattern = Pattern.compile("-?[0-9]+\\.?[0-9]*");
//        String sourceValue = "027";
//        Boolean isNum = true;
//        if (!pattern.matcher(sourceValue).matches()) {
//            // ?????????????????????????????????
//            sourceValue = String.format("'%s'", sourceValue);
//            isNum = false;
//        }
//
//        String[] expressionValueArray = TriggersDetailResponse.getExpressionValue("!=027,077").split(",");
//        for (String expressionVlaue : expressionValueArray) {
//            if (!isNum) {
//                expressionVlaue = String.format("'%s'", expressionVlaue);
//            }
//            String match = TriggersDetailResponse.getMatch("!=027,077");
        //027  077 ??????
        Expression compiledExp = AviatorEvaluator.compile("027 != 027");
        boolean isMatch = Boolean.class.cast(compiledExp.execute());
        if (isMatch) {
            System.out.println("??????");
//                break;
        }
//        }
//
//        boolean isMatch = Boolean.class.cast(AviatorEvaluator.execute( String.format("'%s'", aa)+ "==" + String.format("'%s'", bb)));
//        System.out.println(isMatch);
    }

    @Override
    public int updateReportFilePath(String reportId, String filePath) {
        return reportDao.updateReportFilePath(reportId, filePath);
    }

    /**
     * ????????????id??????????????????
     */
    @Override
    public List<ReportTaskDTO> selectByPage(final PageRequest pageRequest) {
        List<ReportTaskDTO> reprotDTOList = null;
        Page page = PageUtil.convert(pageRequest);
        Map<String, List<String>> resFilterConfig = RequestAuthContext.currentRequestAuthContext().getUser().getResFilterConfig();
        page.getParams().put("resFilterMap", resFilterConfig);
        List<ReportTask> reportList = null;
        String queryName = "com.aspire.mirror.inspection.server.dao.ReportDao.selectByPage";
        Integer pageNo = page.getPageNo();
        Integer pageSize = page.getPageSize();
        Integer startIndex = (pageNo - 1) * pageSize;
        try {
            reportList = sessionTemplate.selectList(queryName, page, new RowBounds(startIndex, pageSize));
            reprotDTOList = ReportTransformer.fromPoTask(reportList);
        } catch (Exception e) {
            LOGGER.warn("selectByTaskId Object reportDTO is null");
        }
        return reprotDTOList;
    }

    /**
     * ????????????id??????
     */
    @Override
    public int selectCount(final PageRequest pageRequest) {
        int count = 0;
        Map<String, List<String>> resFilterConfig = RequestAuthContext.currentRequestAuthContext().getUser().getResFilterConfig();
        Page page = PageUtil.convert(pageRequest);
        page.getParams().put("resFilterMap", resFilterConfig);
        try {
            count = reportDao.selectCount(page);
        } catch (Exception e) {
            LOGGER.warn("selectCount Object reportDTO is null");
        }
        return count;
    }

    /**
     * slf4j
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportBizImpl.class);

    /**
     * ?????????????????????
     */
    @Autowired
    private ReportDao reportDao;

    @Autowired
    private ReportItemDao reportItemDao;
    @Autowired
    private SqlSessionTemplate sessionTemplate;

    @Autowired
    private TaskObjectDao taskObjectDao;
} 
