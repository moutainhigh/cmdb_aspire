package com.aspire.mirror.alert.server.biz.helper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aspire.mirror.alert.server.biz.model.AlertFieldBiz;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aspire.mirror.alert.server.vo.cabinetAlert.IdcCabinetVo;
import com.aspire.mirror.alert.server.constant.AlertCommonConstant;
import com.aspire.mirror.alert.server.dao.cabinetAlert.AlertCabinetColumnDao;
import com.aspire.mirror.alert.server.dao.common.AlertScheduleIndexDao;
import com.aspire.mirror.alert.server.dao.cabinetAlert.po.AlertCabinetColumnConfigData;
import com.aspire.mirror.alert.server.vo.common.AlertScheduleIndexVo;
import com.aspire.mirror.alert.server.util.DateUtils;
import com.aspire.mirror.alert.server.constant.AlertConfigConstants;
import com.aspire.mirror.alert.server.vo.model.AlertFieldVo;
import com.aspire.mirror.alert.server.vo.alert.AlertsV2Vo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CabinetColumnAlertHandler {
	private static final Logger LOGGER = Logger.getLogger(CabinetColumnAlertHandler.class);
	@Autowired
	private AlertCabinetColumnDao alertCabinetColumnDao;

	@Autowired
	private AlertScheduleIndexDao alertScheduleIndexDao;

	@Autowired
	private AlertsHandleV2Helper alertHandleHelper;

	@Autowired
	private AlertFieldBiz alertFieldBiz;

	private Map<String, IdcCabinetVo> idcCabinetMap = Maps.newHashMap();

	public void setIdcCabinetMap(JSONObject alertJson, int type) {
		String idcType = alertJson.getString("idc_type");
		String roomId = alertJson.getString("source_room");
		String idcCabinet = alertJson.getString("idc_cabinet");
		String idcCabinetColumn = idcCabinet.substring(0, 1);
		String mapKey = "";
		if (type == 1) {
			mapKey = String.format("%s_%s_%s", idcType, roomId, idcCabinetColumn);
		} else {
			mapKey = String.format("%s_%s_%s", idcType, roomId, idcCabinet);
		}
		if (idcCabinetMap.containsKey(mapKey)) {
			IdcCabinetVo v = idcCabinetMap.get(mapKey);
			synchronized (v) {
				v.setCount(v.getCount() + 1);
			}
		} else {
			IdcCabinetVo v = new IdcCabinetVo(idcType, roomId, idcCabinet, idcCabinetColumn, 1);
			idcCabinetMap.put(mapKey, v);
		}
		log.info("count_???:{}", idcCabinetMap);
	}

	// ??????????????????
	public void handleCabinetAlert(JSONObject alertJson, JSONArray array) {
		String idcType = alertJson.getString("idc_type");
		String roomId = alertJson.getString("source_room");
		String idcCabinet = alertJson.getString("idc_cabinet");
		String mapKey = String.format("%s_%s_%s", idcType, roomId, idcCabinet.substring(0, 1));
		IdcCabinetVo v = idcCabinetMap.get(mapKey);
		if (v == null) {
			return;
		}
		synchronized (v) {
			try {
				AlertCabinetColumnConfigData data = getConfigData(idcType, roomId, idcCabinet);

				int cabineAlertCount = getCabinetAlertCount(idcType, roomId, idcCabinet, 2);
				// ?????????????????????????????????,?????????????????????????????????????????????

				// cabineAlertCount = 1000;
				if (cabineAlertCount >= 2) {// ??????????????????????????????????????????
					handleAlert(data, AlertCommonConstant.CABINET_COLUMN_ITEM, array, alertJson, 1);
				} 
			} catch (Exception e) {
			} finally {
				setCount(v, mapKey);
			}

		}
	}

	/*
	 * void updateConfigDataStatus(String ids, int alertStatus) {
	 * log.info("??????????????????????????????_?????????{},{}", ids, alertStatus); Map<String, Object> params
	 * = Maps.newHashMap(); params.put("ids", ids.split(","));
	 * params.put("alertStatus", alertStatus); params.put("updateTime", new Date());
	 * alertCabinetColumnDao.updateConfigDataById(params);
	 * log.info("***??????????????????????????????_??????****"); }
	 */

	// ??????????????????
	void handlePowerAlert(JSONObject alertJson, JSONArray array) {
		// ????????????????????????????????????
		String idcType = alertJson.getString("idc_type");
		String roomId = alertJson.getString("source_room");
		String idcCabinet = alertJson.getString("idc_cabinet");
		String mapKey = String.format("%s_%s_%s", idcType, roomId, idcCabinet);
		IdcCabinetVo v = idcCabinetMap.get(mapKey);
		if (v == null) {
			return;
		}
		synchronized (v) {
			try {
				AlertCabinetColumnConfigData data = getConfigData(idcType, roomId, idcCabinet);
				// ?????????????????????
				if (data.getStatus() == 0) {
					return;
				}
				int timeRange = data.getTimeRange();
				int percent = data.getAlertPercentage();
				// int deviceCount = data.getDeviceCount();
				// ????????????????????????????????????

				int deviceAlertCount = getPowerCount(idcType, roomId, idcCabinet, timeRange, 1, array,
						alertJson.getString("cur_moni_time"));
				// ??????????????????,??????????????????
				// deviceAlertCount = 1000;
				if (deviceAlertCount == 0) {
					return;
				}
				int cabinetDeviceCount = getCabinetPowerCount(idcType, roomId, idcCabinet);
				if (cabinetDeviceCount < 3) {// ??????????????????>=3
					if (deviceAlertCount >= 3) {// ??????3????????????????????????
						cabinetDeviceCount = 3;
					} else {
						return;
					}

				}
				if (deviceAlertCount * 100 >= cabinetDeviceCount * percent) {
					// ?????????????????????????????????
					int cabineAlertCount = getCabinetAlertCount(idcType, roomId, idcCabinet, 1);
					if (cabineAlertCount > 0) {
						return;
					}

					handleAlert(data, AlertCommonConstant.CABINET_ITEM, array, alertJson, 1);

				} 
			} catch (Exception e) {

			} finally {
				setCount(v, mapKey);
			}

		}

	}

	private void setCount(IdcCabinetVo v, String mapKey) {
	
			int count = v.getCount();
			if (count == 1) {
				idcCabinetMap.remove(mapKey);
			} else {
				v.setCount(count - 1);
			}
		
		log.info("count_???:{}", idcCabinetMap);
	}

	// ??????????????????????????????
	void handleAlert(AlertCabinetColumnConfigData data, String item, JSONArray array, JSONObject alertJson,
			int alertType) {
		log.info("*****??????????????????_??????****");
		String idcType = alertJson.getString("idc_type");
		String roomId = alertJson.getString("source_room");
		String idcCabinet = alertJson.getString("idc_cabinet");
		List<AlertScheduleIndexVo> dataList = alertScheduleIndexDao.getAlertScheduleIndexDetail(item);
		if (dataList.size() > 0) {
			Date date = new Date();
			List<AlertFieldVo> alertFieldList = alertFieldBiz
					.getModelField(AlertConfigConstants.REDIS_MODEL_ALERT);
			AlertsV2Vo dto = new AlertsV2Vo();
			String value = dataList.get(0).getIndexValue();
			JSONArray infoArray = JSONArray.parseArray(value);
			String deviceClass = "";
			String projectName = "";
			for (int i = 0; i < infoArray.size(); i++) {
				JSONObject a = infoArray.getJSONObject(i);
				String key = a.getString("alertCol");
				String valueTemp = a.getString("content");
				if (key.equals("????????????")) {
					dto.setKeyComment(valueTemp);
					continue;
				}
				if (key.equals("????????????")) {
					valueTemp = a.getString("value");
					valueTemp = getValue(valueTemp, alertJson, data);
					dto.setMoniIndex(valueTemp);
					continue;
				}
				if (key.equals("????????????")) {
					dto.setAlertLevel(a.getString("value"));
					continue;
				}
				if (key.equals("?????????")) {
					dto.setIdcType(idcType);
					continue;
				}
				if (key.equals("????????????")) {
					deviceClass = valueTemp;
					continue;
				}
				if (key.equals("????????????")) {
					projectName = alertJson.getString("project_name");
					continue;
				}

			}
			dto.setObjectType(3 + "");
			dto.setSource(AlertCommonConstant.CABINET_COLUMN_SOURCE);
			dto.setAlertType(alertType + "");
			dto.setMoniObject(AlertCommonConstant.CABINET_COLUMN_MONIOBJECT);
			dto.setCurMoniTime(date);
			dto.setAlertStartTime(date);
			String itemid = "";
			if (item.equals(AlertCommonConstant.CABINET_COLUMN_ITEM)) {
				itemid = String.format("%s_%s_%s", idcType, roomId, data.getCabinetColumnName());
			} else {
				itemid = String.format("%s_%s_%s", idcType, roomId, idcCabinet);
			}
			dto.setItemId(itemid);
			ObjectMapper objectMapper = new ObjectMapper();
			String jsonString = "{}";
			try {
				jsonString = objectMapper.writeValueAsString(dto);
			} catch (JsonProcessingException e) {
			}

			JSONObject alertJson2 = JSONObject.parseObject(jsonString);

			alertJson2.put("device_class", deviceClass);
			alertJson2.put("project_name", projectName);
			if (item.equals(AlertCommonConstant.CABINET_COLUMN_ITEM)) {
				alertJson2.put("idc_cabinet", data.getCabinetColumnName());
			} else {
				alertJson2.put("idc_cabinet", idcCabinet);
			}

			alertJson2.put("source_room", roomId);
			alertHandleHelper.handleAlert(dto, alertJson2, alertFieldList);
		} else {
			LOGGER.error("?????????????????????");
		}
		log.info("*****??????????????????_??????****");
	}

	String getValue(String str, JSONObject alertJson, AlertCabinetColumnConfigData data) {
		String pattern = "\\{[^}]*\\}";

		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(str);
		while (m.find()) {
			String g = m.group();
			String target = g.substring(1, g.length() - 1);// ???????????????

			if (target.equals("alert_percentage")) {
				if (null != data.getAlertPercentage()) {
					str = str.replace(g, data.getAlertPercentage() + "");
				} else {
					str = str.replace(g, "");
				}

			} else if (target.equals("idc_cabinet_column")) {
				if (null != data.getCabinetColumnName()) {
					str = str.replace(g, data.getCabinetColumnName());
				} else {
					str = str.replace(g, "");
				}

			} else {
				if (null != alertJson.getString(target)) {
					str = str.replace(g, alertJson.getString(target));
				} else {
					str = str.replace(g, "");
				}

			}

		}
		return str;
	}

	// ????????????????????????????????????????????????
	void handleCabinetAlertHis(JSONObject alertJson, JSONArray array) {
		String idcType = alertJson.getString("idc_type");
		String roomId = alertJson.getString("source_room");
		String idcCabinet = alertJson.getString("idc_cabinet");

		String mapKey = String.format("%s_%s_%s", idcType, roomId, idcCabinet);
		IdcCabinetVo v = idcCabinetMap.get(mapKey);
		if (v == null) {
			return;
		}
		synchronized (v) {
			try {
				AlertCabinetColumnConfigData data = getConfigData(idcType, roomId, idcCabinet);
				// int alertStatus = data.getAlertStatus();

				// ??????????????????????????????

				int cabineAlertCount = getCabinetAlertCount(idcType, roomId, idcCabinet, 1);
				if (cabineAlertCount == 0) {
					return;
				}

				// ?????????????????????????????????
				int deviceAlertCount = getPowerCount(idcType, roomId, idcCabinet, data.getTimeRange(), 1, array,
						alertJson.getString("cur_moni_time"));
				// ??????????????????????????????
				int cabinetDeviceCount = getCabinetPowerCount(idcType, roomId, idcCabinet);
				if (cabinetDeviceCount < 3) {// ??????????????????>=3
					if(deviceAlertCount<3) {
						handleAlert(data, AlertCommonConstant.CABINET_ITEM, array, alertJson, 2);
					}else {
						return;
					}

				}else {
					// ???????????????????????????,??????
					if (deviceAlertCount * 100 < data.getAlertPercentage() * cabinetDeviceCount) {
						// revokeAlerts(alertJson,AlertCommonConstant.CABINET_ALERT_TITLE);
						handleAlert(data, AlertCommonConstant.CABINET_ITEM, array, alertJson, 2);
					}
				}
				
			} catch (Exception e) {
			} finally {
				setCount(v, mapKey);
			}
		}
	}

	// ?????????????????????????????????
	void handleCabinetColumnAlertHis(JSONObject alertJson, JSONArray array) {
		String idcType = alertJson.getString("idc_type");
		String roomId = alertJson.getString("source_room");
		String idcCabinet = alertJson.getString("idc_cabinet");

		String mapKey = String.format("%s_%s_%s", idcType, roomId, idcCabinet.substring(0, 1));
		IdcCabinetVo v = idcCabinetMap.get(mapKey);
		if (v == null) {
			return;
		}
		synchronized (v) {
			try {
				AlertCabinetColumnConfigData data = getConfigData(idcType, roomId, idcCabinet);
				int cabineAlertCount = getCabinetAlertCount(idcType, roomId, idcCabinet, 2);
				// ?????????????????????????????????
				if (cabineAlertCount == 0) {
					// revokeAlerts(alertJson,AlertCommonConstant.CABINETCOLUMN_ALERT_TITLE);
					handleAlert(data, AlertCommonConstant.CABINET_COLUMN_ITEM, array, alertJson, 2);
					

				}
			} catch (Exception e) {
			} finally {
				setCount(v, mapKey);
			}
		}
	}

	// ??????????????????????????????
	int getCabinetPowerCount(String idcType, String roomId, String idcCabinet) {
		Map<String, Object> params = Maps.newHashMap();
		params.put("idcType", idcType);
		params.put("roomId", roomId);
		params.put("cabinetName", idcCabinet);

		int deviceAlertCount = alertCabinetColumnDao.queryCabinetPowerDeviceCount(params);
		return deviceAlertCount;
	}

	int getPowerCount(String idcType, String roomId, String idcCabinet, int timeRange, int type, JSONArray array,
			String curMoniTme) {
		Map<String, Object> params = Maps.newHashMap();
		params.put("idcType", idcType);
		params.put("roomId", roomId);
		if (type == 1) {
			params.put("cabinetName", idcCabinet);
		} else {
			params.put("cabinetColumn", idcCabinet.substring(0, 1));
		}
		Date endTime = new Date();
		if (StringUtils.isNotBlank(curMoniTme)) {
			endTime = DateUtils.stringToDate(DateUtils.DEFAULT_DATETIME_FMT, curMoniTme);
		}
		params.put("deviceItem", array);

		Date startTime = DateUtils.getSpecifiedDay(endTime, -timeRange);
		params.put("endTime", endTime);
		params.put("startTime", startTime);
		int deviceAlertCount = alertCabinetColumnDao.queryPowerDeviceCount(params);
		return deviceAlertCount;
	}

	int getCabinetAlertCount(String idcType, String roomId, String idcCabinet, int type) {
		Map<String, Object> params = Maps.newHashMap();
		params.put("idcType", idcType);
		params.put("roomId", roomId);
		if (type == 1) {
			params.put("cabinetName", idcCabinet);
		} else {
			params.put("cabinetColumn", idcCabinet.substring(0, 1));
		}

		params.put("source", AlertCommonConstant.CABINET_COLUMN_SOURCE);
		params.put("keyComment", AlertCommonConstant.CABINET_ALERT_TITLE);
		int cabineAlertCount = alertCabinetColumnDao.queryCabinetCount(params);
		return cabineAlertCount;
	}

	AlertCabinetColumnConfigData getConfigData(String idcType, String roomId, String idcCabinet) {
		Map<String, Object> params = Maps.newHashMap();
		params.put("idcType", idcType);
		params.put("roomId", roomId);
		params.put("cabinetColumnName", idcCabinet.substring(0, 1));
		List<AlertCabinetColumnConfigData> list = alertCabinetColumnDao.getConfigDataList(params);
		AlertCabinetColumnConfigData data = list.get(0);
		return data;
	}

	/**
	 * ??????:??????gaoj/???????????????
	 * 
	 * @param alerts
	 * @param alertFieldList
	 */
	/*
	 * void revokeAlerts(JSONObject alertJson,String item) {
	 * log.info("*****???????????????_??????****"); List<AlertFieldRequestDTO> alertFieldList =
	 * alertFieldBiz.getModelField(AlertConfigConstants.REDIS_MODEL_ALERT);
	 * AlertsDTOV2 dto = new AlertsDTOV2();
	 * dto.setAlertType(AlertsDTOV2.ALERT_REVOKE); dto.setObjectType(3+"");
	 * dto.setSource(AlertCommonConstant.CABINET_COLUMN_SOURCE);
	 * dto.setKeyComment(item );
	 * 
	 * String idcCabinet = "";
	 * if(item.equals(AlertCommonConstant.CABINET_ALERT_TITLE)) { idcCabinet =
	 * alertJson.getString("idc_cabinet"); }else { idcCabinet =
	 * alertJson.getString("idc_cabinet").substring(0, 1); } String itemid =
	 * String.format("%s_%s_%s",
	 * alertJson.getString("idc_type"),alertJson.getString("source_room")
	 * ,idcCabinet); dto.setItemId(itemid);
	 * 
	 * ObjectMapper objectMapper = new ObjectMapper(); String jsonString = "{}"; try
	 * { jsonString = objectMapper.writeValueAsString(dto); } catch
	 * (JsonProcessingException e) { } JSONObject alertJson2 =
	 * JSONObject.parseObject(jsonString); alertJson2.put("idc_cabinet",
	 * idcCabinet); try { alertHandleHelper.handleAlert(dto, alertJson2,
	 * alertFieldList); } catch (Exception e) { log.error("error {}", e); }
	 * log.info("*****???????????????_??????****"); }
	 */
}
