package com.aspire.mirror.inspection.server.biz.impl;

import com.aspire.mirror.inspection.api.dto.ReportItemCallBackRequest;
import com.aspire.mirror.inspection.api.dto.ReportItemDetailResponse;
import com.aspire.mirror.inspection.api.dto.ReportItemValue;
import com.aspire.mirror.inspection.api.dto.constant.TaskStatus;
import com.aspire.mirror.inspection.api.dto.constant.TaskType;
import com.aspire.mirror.inspection.api.dto.model.ReportDTO;
import com.aspire.mirror.inspection.api.dto.model.ReportDataWrap;
import com.aspire.mirror.inspection.server.biz.ReportBiz;
import com.aspire.mirror.inspection.server.biz.ReportDataGenerateBiz;
import com.aspire.mirror.inspection.server.biz.ReportItemCallBackBiz;
import com.aspire.mirror.inspection.server.dao.ReportDao;
import com.aspire.mirror.inspection.server.dao.ReportItemDao;
import com.aspire.mirror.inspection.server.dao.TaskDao;
import com.aspire.mirror.inspection.server.dao.po.Report;
import com.aspire.mirror.inspection.server.dao.po.ReportItem;
import com.aspire.mirror.inspection.server.dao.po.ReportResultStatistic;
import com.aspire.mirror.inspection.server.dao.po.Task;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
//@Transactional
public class ReportItemCallBackBizImpl implements ReportItemCallBackBiz {
	@Autowired
	private ReportDao				reportDao;
	@Autowired
	private ReportItemDao			reportItemDao;
	@Autowired
	private ReportDataGenerateBiz	reportDataBiz;
	@Autowired
	private ReportBiz				reportBiz;

	@Autowired
	private TaskDao taskDao;
	
	@Override
	public void onCallBack(ReportItemCallBackRequest bizObj) {
		String reportId = bizObj.getExtendObj();
		Report report = reportDao.selectByPrimaryKey(reportId);
		if (!validate(report, reportId)) {
			return;
		}
		updateReportItem(bizObj);
		generateReportAndNotify(reportId, report.getTaskId());
	}
	
	/**
	* ??????????????????. <br/>
	*
	* ????????? pengguihua
	* @param
	* @return
	*/  
	private boolean validate(Report report, String reportId) {
		if (report == null) {
			log.error("There is no report with reportId {}", reportId);
			return false;
		}
		if (Report.STATUS_FINISHED.equals(report.getStatus())) {
			log.warn("The report of {} has already finished, the call back request will be ignored.", reportId);
			return false;
		}
		return true;
	}
	
	/**
	* ??????????????????????????????????????????. <br/>
	*
	* ????????? pengguihua
	* @param bizObj
	*/  
//	@Transactional(propagation=Propagation.REQUIRES_NEW)
	private void updateReportItem(ReportItemCallBackRequest bizObj) {
		ReportItem updateParam = new ReportItem();
		updateParam.setReportId(bizObj.getExtendObj());
		updateParam.setItemId(bizObj.getItemId());
		updateParam.setObjectType(bizObj.getObjectType());
		updateParam.setObjectId(bizObj.getObjectId());
		updateParam.setStatus(bizObj.getStatus());
		reportItemDao.updateStatusByUniqueKeys(updateParam);

		updateParam.setExecStatus(ReportItem.EXEC_STATUS_TRIGGER);
		reportItemDao.updateExecStatusByUniqueKeys(updateParam);
	}
	
	/**
	* ???????????????????????????. <br/>
	*
	* ????????? pengguihua
	*/  
//	@Transactional(propagation=Propagation.REQUIRES_NEW)
	public void generateReportAndNotify(String reportId, String taskId) {
		// ????????????????????????item?????????????????????
		int nofinishCount = reportItemDao.selectNoFinishItemCount(reportId, taskId);
		if (nofinishCount > 0) {
			log.debug("The report with reportId {} has not finished, ignore the report generation.", reportId);
			return;
		}
		// ?????????????????????????????????????????????
//		ReportItem selectParam = new ReportItem();
//		selectParam.setReportId(reportId);
//		selectParam.setExecStatus(ReportItem.EXEC_STATUS_TRIGGER);
		// ????????????????????????item????????????????????????2????????????
		int noResultCount = reportItemDao.selectNoResultCount(reportId);
		if (noResultCount > 0) {
			log.debug("The report with reportId {} status check is not finished, ignore the report generation.", reportId);
			return;
		}
		// ??????????????????, ????????????
		updateReportData(reportId);
		// ????????????????????????
		Task result = taskDao.selectByPrimaryKey(taskId);
		if (result.getType().equals(TaskType.HANDLED.getKey())) {
			Task task =new Task();
			task.setTaskId(taskId);
			task.setStatus(TaskStatus.OFF.getValue());
			taskDao.updateByPrimaryKeySelective(task);
		}
		try {
			ReportDataWrap reportData = reportDataBiz.generateReportData(reportId);
			reportDataBiz.reportDataNotify(reportData);
		} catch (Throwable e) {
			String msg = String.format("Error to notify the report data with reportId: %s", reportId);
			log.error(msg, e);
		}
	}
	
	// ????????????????????????
	private void updateReportData(String reportId) {
		ReportDTO param = new ReportDTO();
		param.setReportId(reportId);
		param.setStatus(Report.STATUS_FINISHED);
		int count = reportItemDao.selectCountByStatus(ReportItem.STATUS_NORESULT, reportId);
		if (count > 0) {
			param.setBizStatus(Report.BIZ_STATUS_FAIL);
		} else {
			param.setBizStatus(Report.BIZ_STATUS_SUCCESS);
		}

		param.setFinishTime(new Date());
		param.setTotalTime(param.calcTotalSeconds());
		//??????????????????
		// ??????????????????
		ReportResultStatistic statisticValue = reportItemDao.selectNumStatistic(reportId);
		String result = "????????????{0}?????????????????????{1}??????????????????{2} ?????????<span style=\"color: red;font-weight: 800;" +
				"\">{3}</span>????????????<span " +
				"style=\"color: green;font-weight: 800;\">{4}</span>???{5}???????????????{6}???????????????";
		result = MessageFormat.format(result, statisticValue.getDeviceNum(), statisticValue.getItemNum(), statisticValue.getResultNum(),
				statisticValue.getExceptionNum(), statisticValue.getNormalNum(),
				statisticValue.getNoResultNum(), statisticValue.getArtificialJudgmentNum());
		param.setResult(result);
		reportBiz.updateByPrimaryKeySelective(param);
	}
}
