package com.aspire.mirror.zabbixintegrate.biz.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aspire.mirror.zabbixintegrate.bean.CommonHttpResponse;
import com.aspire.mirror.zabbixintegrate.bean.HwResponse;
import com.aspire.mirror.zabbixintegrate.bean.HwSyncLog;
import com.aspire.mirror.zabbixintegrate.bean.IndicatorInfoHw;
import com.aspire.mirror.zabbixintegrate.bean.InstanceHw;
import com.aspire.mirror.zabbixintegrate.biz.HwMonitorSyncService;
import com.aspire.mirror.zabbixintegrate.config.HWProperties;
import com.aspire.mirror.zabbixintegrate.daoCmdb.AlertRestfulDao;
import com.aspire.mirror.zabbixintegrate.daoCmdb.CmdbInstanceDao;
import com.aspire.mirror.zabbixintegrate.daoCmdb.po.CmdbInstance;
import com.aspire.mirror.zabbixintegrate.util.DateUtil;
import com.aspire.mirror.zabbixintegrate.util.HttpUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class HwMonitorSyncServiceImpl implements HwMonitorSyncService {
	private static final Logger LOGGER = LoggerFactory.getLogger(HwMonitorSyncServiceImpl.class);

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;
	
	private static final int MONITORSTATUS = 1;
	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	
	@Autowired
	private AlertRestfulDao alertRestfulDao;
	
	 @Autowired
	private CmdbInstanceDao cmdbInstanceDao;
		@Autowired
		private HWProperties hWProperties;
	 private String url ;
	 private int tag_type;
	 private String idcTypeTag ;
	 
	 private String tokenHeaderName ;
	 
	 @PostConstruct
	 	private void init(){
			 tag_type = hWProperties.getTag_type();
			 tokenHeaderName = hWProperties.getTokenHeaderName();
			 url = hWProperties.getUrl();
			 idcTypeTag = hWProperties.getIdcType()+"_"+tag_type;
	 	}

	public String getToken() throws Exception {
		// ??????????????????
		JSONObject ob = new JSONObject();
		ob.put("grantType", "password");
		ob.put("userName", hWProperties.getOc_username());
		ob.put("value",hWProperties.getOc_password());

	
		HwSyncLog syncLog = new HwSyncLog();// ????????????
		// ????????????????????????
		String urlCur = url + hWProperties.getToken_url();
		formLog(syncLog, urlCur, "token");// ??????????????????
		log.info("???????????????????????????{},url:{}", ob,urlCur);
		CommonHttpResponse rs = HttpUtils.httpPut(ob.toJSONString(), urlCur, null, null);
		int status = rs.getStatus();
		String content = rs.getContent();
		if (!rs.isResponsedNew()) {// ??????????????????
			rs = HttpUtils.httpPut(ob.toJSONString(), urlCur, null, null);
			status = rs.getStatus();
			content = rs.getContent();
		}

		syncLog.setContent(content);
		syncLog.setStatusCode(status);
		if (!rs.isResponsedNew()) {// ??????????????????
			syncLog.setStatusFail();
		}

		if (status != 200) {
			log.error("????????????????????????:{},content:{}", ob.toJSONString(), syncLog.getContent());
			syncLog.setCreateTime(new Date());
			alertRestfulDao.insertHwSyncLog(syncLog);
			throw new Exception("????????????????????????:" + content);
		}
		JSONObject obToken = JSONObject.parseObject(content);
		String token = obToken.getString("accessSession");
		log.info("????????????????????????token???{}", token);
		return token;

	}

	private void formLog(HwSyncLog syncLog, String url, String config_type) {
		syncLog.setUrl(url);
		syncLog.setConfigType(config_type);
		syncLog.setIdcTypeTag(this.idcTypeTag);
		syncLog.setExecTime(new Date());
	}

	/**
	 * 2.2??????????????????????????????
	 * 
	 * @param token
	 * @return
	 * @throws Exception
	 */
	public JSONArray getMonitorTypeData(String token) throws Exception {
		HwSyncLog syncLog = new HwSyncLog();// ????????????
		// ????????????????????????
		String urlCur = url +hWProperties.getMonitor_type_url();
		formLog(syncLog, urlCur, "monitorType");// ??????????????????
		log.info("????????????????????????????????????url:{}", urlCur);
		CommonHttpResponse rs = HttpUtils.httpGet(urlCur, token, this.tokenHeaderName);
		String content = rs.getContent();
		if (!rs.isResponsedNew()) {// ??????????????????
			rs = HttpUtils.httpGet(urlCur, token, this.tokenHeaderName);
			content = rs.getContent();
		}
		/*
		 * if (!rs.isResponsedNew()) {// ?????????????????? syncLog.setStatusFail();
		 * syncLog.setContent(content); }
		 */
		HwResponse ob = JSON.parseObject(content, HwResponse.class);
		if (rs.getStatus() != 200) {// ?????????????????????????????????
			if(!rs.isResponsedNew()) {
				log.error("??????????????????????????????????????????:content:{}", rs.getContent());
				syncLog.setStatusFail();
				syncLog.setStatusCode(rs.getStatus());
				syncLog.setContent(rs.getContent());
				
			}else {
				log.error("??????????????????????????????????????????:content:{}", ob.getError_msg());
				syncLog.setStatusCode(ob.getError_code());
				syncLog.setContent(ob.getError_msg());
				syncLog.setCreateTime(new Date());
			}
			alertRestfulDao.insertHwSyncLog(syncLog);
			throw new Exception("??????????????????????????????????????????:" + syncLog.getContent());
		}

		Object data = ob.getData();
		if (null == data) {
			return new JSONArray();
		}
		JSONArray arr = JSON.parseArray(JSON.toJSONString(data));
		return arr;
	}

	/**
	 * 2.3?????????????????????????????????????????????
	 * 
	 * @param token
	 * @param obTemp
	 * @return
	 * @throws Exception
	 */
	public List<Long> getMonitorIndicatorsRelData(String token, JSONObject obTemp) throws Exception {
		HwSyncLog syncLog = new HwSyncLog();// ????????????
		long objTypeId = obTemp.getLongValue("obj_type_id");

		String indicatorsRel_url = String.format(this.url + hWProperties.getMonitor_indicatorsrel_url(), objTypeId);
		formLog(syncLog, indicatorsRel_url, "monitorIndicatorsRel");// ??????????????????
		log.info("?????????????????????????????????????????????url:{}", indicatorsRel_url);
		CommonHttpResponse rs2 = HttpUtils.httpGet(indicatorsRel_url, token, this.tokenHeaderName);
		String content2 = rs2.getContent();
		if (!rs2.isResponsedNew()) {// ??????????????????
			rs2 = HttpUtils.httpGet(indicatorsRel_url, token, this.tokenHeaderName);
			content2 = rs2.getContent();
		}
		/*
		 * if (!rs2.isResponsed()) {// ?????????????????? syncLog.setStatusFail();
		 * syncLog.setContent(content2); }
		 */

		HwResponse rsOb = JSON.parseObject(content2, HwResponse.class);
		if (rs2.getStatus() != 200) {// ?????????????????????????????????
			
			if(!rs2.isResponsedNew()) {
				log.error("???????????????????????????????????????????????????:{}", rs2.getContent());
				syncLog.setStatusFail();
				syncLog.setStatusCode(rs2.getStatus());
				syncLog.setContent(rs2.getContent());
				
			}else {
				log.error("?????????????????????????????????????????????????????????:{}", rsOb.getError_msg());
				syncLog.setStatusCode(rsOb.getError_code());
				syncLog.setContent(rsOb.getError_msg());
				syncLog.setCreateTime(new Date());
			}
			alertRestfulDao.insertHwSyncLog(syncLog);
			
			throw new Exception("?????????????????????????????????????????????????????????:" + syncLog.getContent());
		}

		JSONObject ob = JSONObject.parseObject(JSON.toJSONString(rsOb.getData()));
		List<Long> ids = ob.getObject("indicator_ids", List.class);
		return ids;
	}

	/**
	 * ??????????????????????????????
	 * 
	 * @param token
	 * @param obTemp
	 * @return
	 * @throws Exception
	 */
	public Map<Object, IndicatorInfoHw> getMonitorIndicators(String token, List<Long> ids) throws Exception {

		HwSyncLog syncLog = new HwSyncLog();// ????????????
		// ??????????????????
		String urlCur = this.url + hWProperties.getMonitor_indicators_url();
		formLog(syncLog, urlCur, "monitorIndicators");// ??????????????????
		log.info("????????????????????????????????????url:{}", urlCur);
		CommonHttpResponse rs3 = HttpUtils.httpPost(urlCur, token, JSON.toJSONString(ids), this.tokenHeaderName);

		String content2 = rs3.getContent();
		if (!rs3.isResponsedNew()) {// ??????????????????
			rs3 = HttpUtils.httpPost(urlCur, token, JSON.toJSONString(ids), this.tokenHeaderName);
			content2 = rs3.getContent();
		}
		

		HwResponse rsOb = JSON.parseObject(content2, HwResponse.class);
		if (rs3.getStatus() != 200) {// ?????????????????????????????????
			if(!rs3.isResponsedNew()) {
				log.error("????????????????????????????????????:content:{}", rs3.getContent());
				syncLog.setStatusFail();
				syncLog.setStatusCode(rs3.getStatus());
				syncLog.setContent(rs3.getContent());
				
			}else {
				log.error("????????????????????????????????????:content:{}", rsOb.getError_msg());
				syncLog.setStatusCode(rsOb.getError_code());
				syncLog.setContent(rsOb.getError_msg());
				syncLog.setCreateTime(new Date());
			}
			alertRestfulDao.insertHwSyncLog(syncLog);
			throw new Exception("????????????????????????????????????:" + syncLog.getContent());
			
			
		}

		Map<Object, IndicatorInfoHw> ob = JSONObject.parseObject(JSON.toJSONString(rsOb.getData()), Map.class);
		return ob;
	}

	/**
	 * ????????????
	 * 
	 * @param token
	 * @throws Exception
	 */
	@Override
	public void syncMonitorConfigData() throws Exception {
		log.info("??????????????????????????????");
		//??????token
		String token = getToken();
		// ??????type
		JSONArray arr = getMonitorTypeData(token);

		// ????????????
		for (int i = 0; i < arr.size(); i++) {
			JSONObject obTemp = arr.getJSONObject(i);
			long objTypeId = obTemp.getLongValue("obj_type_id");
			String zhCn = obTemp.getString("zh_cn");
			// ??????????????????
			List<Long> ids = getMonitorIndicatorsRelData(token, obTemp);
			if (null != ids && ids.size() > 0) {
				// ????????????
				Map<Object, IndicatorInfoHw> dataMap = getMonitorIndicators(token, ids);
				List<IndicatorInfoHw> dataList = Lists.newArrayList();
				Date date = new Date();
				for (Map.Entry<Object, IndicatorInfoHw> entry : dataMap.entrySet()) {
					Long indicatorId = Long.parseLong(entry.getKey().toString());
					IndicatorInfoHw value = JSON.parseObject(JSONObject.toJSONString(entry.getValue()),IndicatorInfoHw.class);
					value.setIndicator_id(indicatorId);
					value.setObj_type_id(objTypeId);
					value.setZh_cn_obj_type(zhCn);
					//value.setMonitor_status(1);
					value.setUpdate_time(date);
					value.setTag_type(this.tag_type);
					dataList.add(value);
				}
				if (dataList.size() > 0) {
					alertRestfulDao.insertIndicatorInfoHw(dataList);
				}
			}
		}

		log.info("??????????????????????????????");
	}

	/*
	 * 2.5??????Region??????
	 */
	public JSONObject getRegionDatas(String token, int pageNo) throws Exception {

		HwSyncLog syncLog = new HwSyncLog();// ????????????
		// ??????????????????

		String regionUrl = String.format(this.url + hWProperties.getRegion_url(), hWProperties.getRegionClassName());
		formLog(syncLog, regionUrl, "region");// ??????????????????
		String contentSelector = "[\"locales\",\"nativeId\",\"resId\"]";
		regionUrl = regionUrl + "?pageNo=" + pageNo + "&pageSize=" + hWProperties.getRegionPageSize()
				+ "&contentSelector="+contentSelector;
		log.info("?????????Region??????url:{}", regionUrl);
		CommonHttpResponse rs = HttpUtils.httpGet(regionUrl, token, this.tokenHeaderName);

		String content2 = rs.getContent();
		if (!rs.isResponsedNew()) {// ??????????????????
			rs = HttpUtils.httpGet(regionUrl, token, this.tokenHeaderName);
			content2 = rs.getContent();
		}
		

		if (rs.getStatus() != 200) {// ?????????????????????????????????
			log.error("?????????Region????????????:content:{}", content2);
			syncLog.setStatusFail();
			syncLog.setStatusCode(rs.getStatus());
			syncLog.setContent(content2);
			syncLog.setCreateTime(new Date());
			alertRestfulDao.insertHwSyncLog(syncLog);
			throw new Exception("?????????Region????????????:" + syncLog.getContent());
		}
		JSONObject ob = JSON.parseObject(content2);
		return ob;
	}

	/**
	 * ?????????????????????
	 * 
	 * @param token
	 * @param pageNo
	 * @param region
	 * @return
	 * @throws Exception
	 */
	public JSONObject getDeviceDatas(String token, int pageNo, JSONObject region) throws Exception {
		log.info("?????????????????????????????????");
		HwSyncLog syncLog = new HwSyncLog();// ????????????
		// ??????????????????

		String nativeId = region.getString("nativeId");
		String deviceUrl = String.format(this.url + hWProperties.getDevice_url(), hWProperties.getDeviceClassName());
		formLog(syncLog, deviceUrl, "device");// ??????????????????
		deviceUrl = deviceUrl + "?pageNo="+pageNo+"&pageSize=" + hWProperties.getDevicePageSize()
				+ "&contentSelector=[\"id\",\"name\",\"nativeId\",\"hostId\",\"privateIps\",\"bizRegionId\",\"createdAt\",\"extraSpecs\",\"status\"] "
				+ "&condition={\"constraint\":[{\"simple\":{\"name\":\"bizRegionNativeId\",\"value\":\"" + nativeId
				+ "\"}}]}";
		log.info("???????????????????????????URL:{}", deviceUrl);
		CommonHttpResponse rs = HttpUtils.httpGet(deviceUrl, token, this.tokenHeaderName);

		String content2 = rs.getContent();
		if (!rs.isResponsedNew()) {// ??????????????????
			rs = HttpUtils.httpGet(deviceUrl, token, this.tokenHeaderName);
			content2 = rs.getContent();
		}
		

		if (rs.getStatus() != 200) {// ?????????????????????????????????
			log.error("?????????????????????????????????:region:{},content:{}", region, content2);
			syncLog.setStatusFail();
			syncLog.setStatusCode(rs.getStatus());
			syncLog.setContent(content2);
			syncLog.setCreateTime(new Date());
			alertRestfulDao.insertHwSyncLog(syncLog);
			
			throw new Exception("?????????????????????????????????:" + syncLog.getContent());
		}
		JSONObject ob = JSON.parseObject(content2);
		return ob;
	}

	/**
	 * ??????????????????
	 * 
	 * @param token
	 * @throws Exception
	 */
	@Override
	public void syncDeviceData() throws Exception {
		log.info("?????????????????????????????????");
		//??????token
		String token = getToken();
		// ??????region
		JSONObject ob = getRegionDatas(token, 1);
		JSONArray arr = ob.getJSONArray("objList");
		if (null != arr) {

			for (int i = 0; i < arr.size(); i++) {
				JSONObject region = arr.getJSONObject(i);
				// ??????Region????????????
				DeviceThread thread = new DeviceThread(region, token);
				taskExecutor.execute(thread);
				// handleDeviceData(region,token);
			}

		}
		// ??????region??????
		int totalPageNo = ob.getIntValue("totalPageNo");
		log.info("regionData:{}",ob);
		if (totalPageNo > 1) {
			for (int kk = 2; kk <= totalPageNo; kk++) {
				JSONObject ob2 = getRegionDatas(token, kk);
				JSONArray arr2 = ob2.getJSONArray("objList");
				if (null != arr2) {
					for (int i = 0; i < arr2.size(); i++) {
						JSONObject region = arr2.getJSONObject(i);
						DeviceThread thread = new DeviceThread(region, token);
						taskExecutor.execute(thread);
						// handleDeviceData(region,token);
					}

				}
			}
		}
		log.info("?????????????????????????????????");

	}

	public class DeviceThread implements Runnable {
		private JSONObject region;
		private String token;

		public DeviceThread(JSONObject region, String token) {
			this.region = region;
			this.token = token;
		}

		public void run() {
			HwSyncLog syncLog = new HwSyncLog();// ????????????
			try {
				formLog(syncLog, null, "syncDeviceData");// ??????????????????
				handleDeviceData(region, token);
			} catch (Exception e) {
				log.error("????????????????????????", e);
				syncLog.setStatusFail();
				String message = e.getMessage()==null?"":e.getMessage().toString();
				syncLog.setContent(e.getClass().getName() + ":" + message);
			}finally {
				alertRestfulDao.insertHwSyncLog(syncLog);
			}
		}

	}

	// ????????????????????????
	public void handleDeviceData(JSONObject region, String token) throws Exception {
		log.info("???????????????????????????{}", region);
		Date date = new Date();
		JSONObject ob2 = getDeviceDatas(token, 1, region);
		JSONArray arr2 = ob2.getJSONArray("objList");
		List<String> idAllList = Lists.newArrayList();
		handleDevicePageData( region,arr2,date,idAllList);
		
		// ?????????
		int totalPageNo = ob2.getIntValue("totalPageNo");
		log.info("???????????????{},totalPageNo:{}", arr2.size(),totalPageNo);
		if (totalPageNo > 1) {
			for (int kk = 2; kk <= totalPageNo; kk++) {
				JSONObject obTemp = getDeviceDatas(token, kk, region);
				JSONArray arrTemp = obTemp.getJSONArray("objList");
				handleDevicePageData(region,arrTemp,date,idAllList);
			}
		}
		idAllList.clear();
		log.info("???????????????????????????{}", region);
	}
	
	public void handleDevicePageData(JSONObject region,JSONArray arr2,Date date, List<String> idAllList){
		Map<String,InstanceHw> idMap = Maps.newHashMap();
		 List<InstanceHw> inertList = Lists.newArrayList();
		 List<String> idList = Lists.newArrayList();
		for (int k = 0; k < arr2.size(); k++) {
			InstanceHw device = arr2.getObject(k, InstanceHw.class);
			device.setUpdate_time(date);
			//device.setMonitor_status(1);
			device.setTag_type(tag_type);
			device.setLocales(region.getString("locales"));
			device.setRegionId(region.getString("nativeId"));
			String id = device.getId();
			if(idAllList.contains(id)) {
				continue;
			}
			idAllList.add(id);
			idList.add(id);
			idMap.put(id, device);
			inertList.add(device);
		}
		if (inertList.size() > 0) {
			List<CmdbInstance> dsList = cmdbInstanceDao.getDeviceInfo(idList);
			for(CmdbInstance info:dsList) {
				String id = info.getInstanceId();
				if(idMap.containsKey(id)) {
					idMap.get(id).setInstance_info(JSONObject.toJSONString(info));
					idMap.get(id).setIdcType(info.getIdcType());
				}
			}
			alertRestfulDao.insertInstanceHw(inertList);
			inertList.clear();
			idMap.clear();
			idList.clear();
		}
	}

	/**
	 * ?????????????????????
	 * 
	 * @param token
	 * @param ob
	 * @return
	 * @throws Exception
	 */
	public HwResponse getMonitorDatas(String token, JSONObject ob) throws Exception {

		HwSyncLog syncLog = new HwSyncLog();// ????????????
		// ??????????????????
		String urlCur = this.url + hWProperties.getMonitor_data_url();
		formLog(syncLog, urlCur, "monitorDatas");// ??????????????????
		log.info("?????????????????????????????????????????????url:{}",urlCur);
		log.debug("?????????????????????????????????????????????data:{}",ob);
		CommonHttpResponse rs3 = HttpUtils.httpPost(urlCur, token, ob.toJSONString(), this.tokenHeaderName);

		String content2 = rs3.getContent();
		if (!rs3.isResponsedNew()) {// ??????????????????
			rs3 = HttpUtils.httpPost(urlCur, token, ob.toJSONString(), this.tokenHeaderName);
			content2 = rs3.getContent();
		}
		if (!rs3.isResponsed()) {// ??????????????????
			syncLog.setStatusFail();
			syncLog.setContent(content2);
		}

		HwResponse rsOb = JSON.parseObject(content2, HwResponse.class);
		//log.info("MonitorDataThread:{}",rsOb);
		if (rs3.getStatus() != 200) {// ?????????????????????????????????
			if(!rs3.isResponsedNew()) {
				log.error("???????????????????????????????????????????????????:{},content:{}", ob.toJSONString(), content2);
				syncLog.setStatusFail();
				syncLog.setStatusCode(rs3.getStatus());
				syncLog.setContent(rs3.getContent());
				
			}else {
				log.error("???????????????????????????????????????????????????:{},content:{}", ob.toJSONString(), rsOb.getError_msg());
				syncLog.setStatusCode(rsOb.getError_code());
				syncLog.setContent(rsOb.getError_msg());
				syncLog.setCreateTime(new Date());
			}
			alertRestfulDao.insertHwSyncLog(syncLog);
			throw new Exception("???????????????????????????????????????????????????:" + syncLog.getContent());
			
		}
		/*
		 * String dataStr = JSON.toJSONString(rsOb.getData()); if
		 * (StringUtils.isBlank(dataStr)) { return null; } Map<String, Map<String,
		 * JSONObject>> monitorOb =
		 * JSONObject.parseObject(JSON.toJSONString(rsOb.getData()), Map.class);
		 */
	
		return rsOb;
	}

	
	/**
	 * ??????????????????
	 * 
	 * @param token
	 * @throws Exception
	 */
	@Override
	public void syncMonitorDatas(Date startTime,Date endTime) throws Exception {
		log.info("????????????????????????????????????");
		//??????token
		String token = getToken();
				
		JSONObject ob = new JSONObject();
		
	
		ob.put("begin_time", startTime.getTime() + "");
		ob.put("end_time", endTime.getTime() + "");
		ob.put("interval", hWProperties.getInterval());
		ob.put("range", "BEGIN_END_TIME");
		

		Map<String, Object> params = Maps.newHashMap();
		params.put("tagType", this.tag_type);
		params.put("monitorStatus", MONITORSTATUS);
		// ????????????????????????
		List<String> objTypeIdList = alertRestfulDao.getIndicatorObjTypeIdList(params);
		int  deviceCount = hWProperties.getDeviceCount();
		for (String objTypeId : objTypeIdList) {
			params.put("objTypeId", objTypeId);
			// ??????????????????
			List<IndicatorInfoHw> indicatorList = alertRestfulDao.getIndicatorList(params);

			int count = indicatorList.size();
			ob.put("obj_type_id", objTypeId);// ????????????????????????id

			for (int i = 0; i < count; i += hWProperties.getIndicatorCount()) {
				List<String> indicators = Lists.newArrayList();
				Map<String, IndicatorInfoHw> IndicatorMap = Maps.newHashMap();
				for (int ii = i; ii < i + hWProperties.getIndicatorCount() && ii<count; ii++) {
					IndicatorInfoHw v = indicatorList.get(ii);
					indicators.add(v.getIndicator_id() + "");
					IndicatorMap.put(v.getIndicator_id() + "", v);
				}
				ob.put("indicator_ids", indicators);// ???????????????id
				Map<String, Object> instanceParam = Maps.newHashMap();
				instanceParam.put("tagType", this.tag_type);
				instanceParam.put("monitorStatus", this.MONITORSTATUS);
				instanceParam.put("idcTypeList", hWProperties.getIdcType().split(","));
				instanceParam.put("pageSize", deviceCount);
				instanceParam.put("status", "active");
				//????????????
				int isCount = alertRestfulDao.getInstanceHwPageListCount(instanceParam);
				int getTotalPage = getTotalPage(isCount, deviceCount);
				for (int k = 0; k < getTotalPage; k++) {
					int begin = k * deviceCount;
					instanceParam.put("begin", begin);
					// ??????????????????
					List<InstanceHw> instanceList = alertRestfulDao.getInstanceHwPageList(instanceParam);
					List<String> instanceIdList = Lists.newArrayList();
					Map<String, InstanceHw> deviceMap = Maps.newHashMap();
					for (InstanceHw is : instanceList) {
						instanceIdList.add(is.getId());
						deviceMap.put(is.getId(), is);
					}
					ob.put("obj_ids", instanceIdList);
					JSONObject obNew = JSONObject.parseObject(ob.toJSONString());
					
					MonitorDataThread thread = new MonitorDataThread(obNew,deviceMap,IndicatorMap, token);
					taskExecutor.execute(thread);
					
				}

			}

		}
		
		log.info("?????????????????????????????????");

	}
	
	public class MonitorDataThread implements Runnable {
		private JSONObject ob;
		private Map<String, InstanceHw> deviceMap;
		private Map<String, IndicatorInfoHw> IndicatorMap;
		private String token;

		public MonitorDataThread(JSONObject ob,Map<String, InstanceHw> deviceMap
				,Map<String, IndicatorInfoHw> IndicatorInfoMap,String token) {
			this.ob = ob;
			this.deviceMap = deviceMap;
			this.IndicatorMap = IndicatorInfoMap;
			this.token = token;
		}

		public void run() {
			HwSyncLog syncLog = new HwSyncLog();// ????????????
			try {
				formLog(syncLog, null, "syncMonitorDatas");// ??????????????????
				syncLog.setFromTime(DateUtil.format(ob.getDate("begin_time"), DateUtil.DEFAULT_DATETIME_FMT));
				syncLog.setToTime(DateUtil.format(ob.getDate("end_time"), DateUtil.DEFAULT_DATETIME_FMT));
				HwResponse rsOb = getMonitorDatas(token, ob);
				Map<String, Map<String, JSONObject>> map = JSONObject.parseObject(JSON.toJSONString(rsOb.getData()),
						Map.class);
				syncLog.setContent(String.format("%s:code:%s:msg:%s", hWProperties.getIdcType(),rsOb.getError_code(),rsOb.getError_msg()));
				if (null != map && map.size()>0) {
					handleMonitorData( map,  deviceMap,IndicatorMap);
				}
				
				//syncLog.setContent(content);%s
			} catch (Exception e) {
				log.error("????????????????????????", e);
				syncLog.setStatusFail();
				String message = e.getMessage()==null?"":e.getMessage().toString();
				syncLog.setContent(e.getClass().getName() + ":" + message);
				
			}finally {
				alertRestfulDao.insertHwSyncLog(syncLog);
			}
		}

	}

	// ??????????????????
	public void handleMonitorData(Map<String, Map<String, JSONObject>> map, Map<String, InstanceHw> deviceMap,
			Map<String, IndicatorInfoHw> IndicatorInfoMap) {
		// ??????es???????????????kafka
		for (Map.Entry<String, Map<String, JSONObject>> en : map.entrySet()) {
			String id = en.getKey();
			InstanceHw device = deviceMap.get(id);
			CmdbInstance instance = null;
			String instanceStr = device.getInstance_info();
			if (StringUtils.isNotBlank(instanceStr)) {
				instance = JSONObject.parseObject(instanceStr, CmdbInstance.class);
			}
			Map<String, JSONObject> indicatorMap = en.getValue();
			for (Map.Entry<String, JSONObject> ca : indicatorMap.entrySet()) {
				String indicatorId = ca.getKey();
				IndicatorInfoHw indicator = IndicatorInfoMap.get(indicatorId);
				Map<String, Object> es = Maps.newHashMap();
				if (null != instance) {
					es.put("host", instance.getIp());
					es.put("resourceId", instance.getInstanceId());
					es.put("bizSystem", instance.getBizSystem());
					es.put("department1", instance.getDepartment1());
					es.put("department2", instance.getDepartment2());
					es.put("deviceClass", instance.getDeviceClass());
					es.put("deviceType", instance.getDeviceType());

					es.put("idcType", instance.getIdcType());
					es.put("roomId", instance.getRoomId());
					es.put("podName", instance.getPodName());
					es.put("nodeType", instance.getNodeType());
				} else {
					es.put("idcType", device.getIdcType());
					try {
						String locales = device.getLocales();
						JSONObject l = JSONObject.parseObject(locales);
						//int index = l.getString("zh_cn").indexOf("-");
						es.put("podName", l.getString("zh_cn"));
						es.put("host", device.getPrivateIps().split("@")[1]);
					} catch (Exception e) {
						es.put("host", device.getPrivateIps());
						log.error("??????privateIps????????????:id:{},ip:{}", id, device.getPrivateIps());
					}
				}

				es.put("item", indicator.getItem());
				es.put("suyanUuid", id);
				es.put("device_source", hWProperties.getDevice_source());// TODO
				String dataType = indicator.getData_type();
				String historyType = indicator.getHistory_type();
				JSONObject ob = ca.getValue();
				List<JSONObject> data = JSONObject.parseObject(ob.getString("series"), List.class);
				for (JSONObject obb : data) {
					Map<String, Object> esTemp = Maps.newHashMap();
					esTemp.putAll(es);
					Set<String> keyset = obb.keySet();
					if(keyset.size()==0) {
						continue;
					}
					boolean flag = false;
					for (String vv : keyset) {
						flag = true;
						long time = Long.parseLong(vv);
						String value = obb.getString(vv);
						String operator = indicator.getOperator();
						String operatorValue = indicator.getOperator_value();
						if(StringUtils.isNotBlank(operator) && StringUtils.isNotBlank(operatorValue) ) {
							Double v = Double.parseDouble(value);
							Long v1 = Long.parseLong(operatorValue);
							if (operator.equals("/")) {
								v = v / v1;
							} else {
								v = v * v1;
							}
							/*
							 * if(!historyType.equals("history")) { value = Math.round(v) +""; }else { value
							 * = v+""; }
							 */
							value = v+"";
						}
						esTemp.put("value", value);
						esTemp.put("datetime", new Date(time).toInstant().toString());
						esTemp.put("clock", time / 1000);
					}
					if(StringUtils.isNotBlank(historyType)) {
						if (historyType.equalsIgnoreCase("history")) {// TODO
							kafkaTemplate.send(hWProperties.getToKafkaTopic(), JSONObject.toJSONString(esTemp));
						} else {
							kafkaTemplate.send(hWProperties.getToKafkaTopic_uint(), JSONObject.toJSONString(esTemp));
						}
					}else {
						if (dataType.equalsIgnoreCase("float") || dataType.equalsIgnoreCase("double")) {// TODO
							kafkaTemplate.send(hWProperties.getToKafkaTopic(), JSONObject.toJSONString(esTemp));
						} else {
							kafkaTemplate.send(hWProperties.getToKafkaTopic_uint(), JSONObject.toJSONString(esTemp));
						}
					}
					if(flag) {//???????????????
						break;
					}
				}
				

			}
		}
	}

	// ??????????????????
	public int getTotalPage(int total, int size) {
		int totalPages = total / size;
		if (total % size != 0) {
			totalPages++;
		}
		return totalPages;
	}
}
