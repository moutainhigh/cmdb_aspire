package com.aspire.ums.cmdb.repertory.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aspire.ums.cmdb.module.service.ModuleService;
import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson.JSON;
import com.aspire.ums.cmdb.common.Constants;
import com.aspire.ums.cmdb.common.PoiTitle;
import com.aspire.ums.cmdb.maintain.entity.CasoptionsBean;
import com.aspire.ums.cmdb.maintain.entity.Instance;
import com.aspire.ums.cmdb.maintain.entity.FormValue;
import com.aspire.ums.cmdb.maintain.entity.InstanceCircle;
import com.aspire.ums.cmdb.maintain.entity.InstanceModel;
import com.aspire.ums.cmdb.maintain.entity.InstanceRelation;
import com.aspire.ums.cmdb.maintain.entity.ModuleRelation;
import com.aspire.ums.cmdb.maintain.entity.RelationLog;
import com.aspire.ums.cmdb.maintain.mapper.CasoptionsMapper;
import com.aspire.ums.cmdb.maintain.mapper.FormValueMapper;
import com.aspire.ums.cmdb.maintain.mapper.InstanceCircleMapper;
import com.aspire.ums.cmdb.maintain.mapper.InstanceMapper;
import com.aspire.ums.cmdb.maintain.mapper.InstanceRelationMapper;
import com.aspire.ums.cmdb.maintain.mapper.MaintainViewMapper;
import com.aspire.ums.cmdb.maintain.mapper.ModuleRelationMapper;
import com.aspire.ums.cmdb.maintain.mapper.RelationLogMapper;
import com.aspire.ums.cmdb.maintain.service.ConfigLogService;
import com.aspire.ums.cmdb.module.entity.Form;
import com.aspire.ums.cmdb.module.entity.FormOptions;
import com.aspire.ums.cmdb.module.entity.FormRule;
import com.aspire.ums.cmdb.module.entity.Module;
import com.aspire.ums.cmdb.module.mapper.FormMapper;
import com.aspire.ums.cmdb.module.mapper.FormOptionsMapper;
import com.aspire.ums.cmdb.module.mapper.FormParamMapper;
import com.aspire.ums.cmdb.module.mapper.FormRuleMapper;
import com.aspire.ums.cmdb.module.mapper.ModuleMapper;
import com.aspire.ums.cmdb.repertory.service.RepertoryService;
import com.aspire.ums.cmdb.util.DateUtils;
import com.aspire.ums.cmdb.util.ExcelImportUtils;
import com.aspire.ums.cmdb.util.StringUtils;
import com.aspire.ums.cmdb.util.UUIDUtil;

@Service
@Transactional
public class RepertoryServiceImpl implements RepertoryService {

	private final Logger logger = Logger.getLogger(getClass());

	@Autowired
	private ModuleMapper moduleMapper;

	@Autowired
	private FormMapper formMapper;

	@Autowired
	private FormParamMapper formParamMapper;

	@Autowired
	private FormOptionsMapper formOptionsMapper;

	@Autowired
	private FormRuleMapper formRuleMapper;

	@Autowired
	private ModuleRelationMapper moduleRelationMapper;

	@Autowired
	private InstanceMapper instanceMapper;

	@Autowired
	private FormValueMapper formValueMapper;

	@Autowired
	private InstanceRelationMapper instanceRelationMapper;
	
	@Autowired
	private InstanceCircleMapper instanceCircleMapper;
	
    @Autowired
    private MaintainViewMapper maintainViewMapper;
    
    @Autowired
    private CasoptionsMapper casoptionsMapper;
    
    @Autowired
    private ConfigLogService configLogService;
    
    @Autowired
    private RelationLogMapper relationLogMapper;

	@Autowired
	ModuleService moduleService;
	/**
	 * ????????????????????????excel????????????
	 * 
	 * @param moduleId
	 * @return
	 */
	@SuppressWarnings("rawtypes")
    public List<PoiTitle> getTitle(String moduleId) {
		List<PoiTitle> list = new ArrayList<PoiTitle>();
		// ?????????
		PoiTitle column_1 = new PoiTitle();
		column_1.setField(null);
		column_1.setComment("??????");
		column_1.setFormat("????????????");
		column_1.setDef("?????????");
		list.add(column_1);

		PoiTitle column_2 = new PoiTitle();
		column_2.setField("2_ID");
		column_2.setComment("ID");
		column_2.setFieldType("ID");
		column_2.setFormat("??????????????????");
		column_2.setDef("");
		list.add(column_2);

		// ?????????-???????????????
		List<Form> formList = formMapper.getFormsByModuleId(moduleId);
		for (Form form : formList) {
			PoiTitle column_form = new PoiTitle();
			column_form.setField(form.getCode());
			column_form.setFormId(form.getId());
			column_form.setComment(form.getName() + (StringUtils.isNotEmpty(form.getUnit()) ? ("(" + form.getUnit().toUpperCase() + ")") : "")
					+ (form.getRequired().equals("true") ? "[??????]" : ""));
			column_form.setDef(form.getDefaultvalue());
			column_form.setFormat("");
			column_form.setFieldType(form.getType());
			column_form.setFormCode(form.getCode());
			column_form.setFormName(form.getName());

			Map<String, String> paramMap = new HashMap<String, String>();
			List<Map<String, String>> params = formParamMapper.getParamsMapByFormId(form.getId());
			for (Map<String, String> param : params) {
				String key = param.get("key");
				String value = param.get("value");
				paramMap.put(key, value);
			}
			List<String> optionList = formOptionsMapper.getOptionsMapByFormId(form.getId());
	        column_form.setFormOptions(formOptionsMapper.getByFormId(form.getId()));
	        column_form.setCasoptionsBeans(casoptionsMapper.getOptionBeanByFormId(form.getId()));

			switch (form.getType()) {
			case Constants.MODULE_FORM_TYPE_SINGLEROWTEXT: // ????????????
				if (null != paramMap && StringUtils.isNotEmpty(paramMap.get(Constants.MODULE_FORM_PARAM_MINLENGTH))
						&& StringUtils.isNotEmpty(paramMap.get(Constants.MODULE_FORM_PARAM_MAXLENGTH))) {
					column_form.setMinLength(Integer.valueOf(paramMap.get(Constants.MODULE_FORM_PARAM_MINLENGTH)));
					column_form.setMaxLength(Integer.valueOf(paramMap.get(Constants.MODULE_FORM_PARAM_MAXLENGTH)));
					column_form.setFormat("????????????(" + paramMap.get(Constants.MODULE_FORM_PARAM_MINLENGTH) + "-"
							+ paramMap.get(Constants.MODULE_FORM_PARAM_MAXLENGTH) + ")??????");
				}
				list.add(column_form);
				break;
			case Constants.MODULE_FORM_TYPE_MULTIROWTEXT: // ????????????
				if (null != paramMap && StringUtils.isNotEmpty(paramMap.get(Constants.MODULE_FORM_PARAM_MINLENGTH))
						&& StringUtils.isNotEmpty(paramMap.get(Constants.MODULE_FORM_PARAM_MAXLENGTH))) {
					column_form.setMinLength(Integer.valueOf(paramMap.get(Constants.MODULE_FORM_PARAM_MINLENGTH)));
					column_form.setMaxLength(Integer.valueOf(paramMap.get(Constants.MODULE_FORM_PARAM_MAXLENGTH)));
					column_form.setFormat("????????????(" + paramMap.get(Constants.MODULE_FORM_PARAM_MINLENGTH) + "-"
							+ paramMap.get(Constants.MODULE_FORM_PARAM_MAXLENGTH) + ")??????");
				}
				list.add(column_form);
				break;
			case Constants.MODULE_FORM_TYPE_RICHTEXT: // ?????????
				// ?????????????????????
				break;
			case Constants.MODULE_FORM_TYPE_LISTSEL: // ????????????
				if (null != optionList && !optionList.isEmpty()) {
					column_form.setOptions(optionList);
					column_form.setFormat("????????????(??????):" + optionList + " ??????:[?????????????????????]");
				}
				list.add(column_form);
				break;
			case Constants.MODULE_FORM_TYPE_SINGLESEL: // ??????
				if (null != optionList && !optionList.isEmpty()) {
					column_form.setOptions(optionList);
					column_form.setFormat("????????????(??????):" + optionList + " ??????:[?????????????????????]");
				}
				list.add(column_form);
				break;
			case Constants.MODULE_FORM_TYPE_MULTISEL: // ??????
				if (null != optionList && !optionList.isEmpty()) {
					column_form.setOptions(optionList);
					column_form.setFormat("????????????(??????):" + optionList + " ??????:[?????????????????????\",\"??????]");
				}
				list.add(column_form);
				break;
			case Constants.MODULE_FORM_TYPE_DOUBLE: // ??????
				if (null != paramMap && StringUtils.isNotEmpty(paramMap.get(Constants.MODULE_FORM_PARAM_MIN))
						&& StringUtils.isNotEmpty(paramMap.get(Constants.MODULE_FORM_PARAM_MAX))
						&& StringUtils.isNotEmpty(paramMap.get(Constants.MODULE_FORM_PARAM_PRECISION))) {
					column_form.setMin(Integer.valueOf(paramMap.get(Constants.MODULE_FORM_PARAM_MIN)));
					column_form.setMax(Integer.valueOf(paramMap.get(Constants.MODULE_FORM_PARAM_MAX)));
					column_form.setPrecision(Integer.valueOf(paramMap.get(Constants.MODULE_FORM_PARAM_PRECISION)));
					column_form.setFormat("????????????(" + paramMap.get(Constants.MODULE_FORM_PARAM_MIN) + "-"
							+ paramMap.get(Constants.MODULE_FORM_PARAM_MAX) + ") " + paramMap.get(Constants.MODULE_FORM_PARAM_PRECISION) + "?????????");
				}
				list.add(column_form);
				break;
			case Constants.MODULE_FORM_TYPE_INT:// ??????
				if (null != paramMap && StringUtils.isNotEmpty(paramMap.get(Constants.MODULE_FORM_PARAM_MIN))
						&& StringUtils.isNotEmpty(paramMap.get(Constants.MODULE_FORM_PARAM_MAX))) {
					column_form.setMin(Integer.valueOf(paramMap.get(Constants.MODULE_FORM_PARAM_MIN)));
					column_form.setMax(Integer.valueOf(paramMap.get(Constants.MODULE_FORM_PARAM_MAX)));
					column_form.setFormat("????????????(" + paramMap.get(Constants.MODULE_FORM_PARAM_MIN) + "-"
							+ paramMap.get(Constants.MODULE_FORM_PARAM_MAX) + ")");
				}
				list.add(column_form);
				break;
			case Constants.MODULE_FORM_TYPE_IMAGE: // ??????
				// ?????????????????????
				break;
			case Constants.MODULE_FORM_TYPE_FILE: // ??????
				// ?????????????????????
				break;
			case Constants.MODULE_FORM_TYPE_DATETIME:// ??????
                column_form.setFormat("???????????????yyyy-MM-dd HH:mm:ss");
                column_form.setFormatDate(false);
				if (null != paramMap && StringUtils.isNotEmpty(paramMap.get(Constants.MODULE_FORM_PARAM_FORMATDATE))) {
					column_form.setFormatDate(Boolean.valueOf(paramMap.get(Constants.MODULE_FORM_PARAM_FORMATDATE)));
					if (paramMap.get(Constants.MODULE_FORM_PARAM_FORMATDATE).equals("true")) {
						column_form.setFormat("???????????????yyyy-mm-dd");
					} else {
					}
				}
				list.add(column_form);
				break;
			case Constants.MODULE_FORM_TYPE_TABLE: // ??????
				// ?????????????????????
				break;
			case Constants.MODULE_FORM_TYPE_CASCADER: // ??????
	             if (null != optionList && !optionList.isEmpty()) {
	                    column_form.setOptions(optionList);
	                }
                column_form.setFormat(" ??????:[?????????\",\"????????????]");
				list.add(column_form);
				break;
			case Constants.MODULE_FORM_TYPE_GROUPLINE: // ????????????
				// ?????????????????????
				break;
			default:
				break;
			}
		}

		// ?????????-???????????????
		ModuleRelation moduleRelation = new ModuleRelation();
		moduleRelation.setSourceModuleId(moduleId);
		List<Map> relations = moduleRelationMapper.getRetionByCondition(moduleRelation);
		for (Map map : relations) {
			PoiTitle column_form = new PoiTitle();
			column_form.setField(map.get("id").toString());
			column_form.setDef("");
			column_form.setRestriction(String.valueOf(map.get("restriction")));
			column_form.setFormat("??????[??????????????????????????????????????????][" + map.get("restrictionDec") + "]");

			if (StringUtils.isNotEmpty(map.get("sourceModuleId")) && map.get("sourceModuleId").equals(moduleId)) {
				column_form.setFieldType(Constants.MODULE_RELATION_SOURCE);
				column_form.setComment(map.get("relationName").toString() + "???(" + map.get("targetModuleName").toString() + ")");
	            column_form.setModuleId((String) map.get("targetModuleId"));
	            column_form.setFormName( map.get("targetModuleName").toString());
			} else {
				column_form.setFieldType(Constants.MODULE_RELATION_TARGET);
				column_form.setComment(map.get("relationName").toString() + "???(" + map.get("sourceModuleName").toString() + ")");
	            column_form.setModuleId((String)map.get("sourceModuleId"));
	            column_form.setFormName( map.get("sourceModuleName").toString());
			}
			list.add(column_form);
		}

		return list;
	}

	/**
	 * ????????????????????????excel????????????
	 * 
	 * @param moduleId
	 * @return
	 */
	public List<Map<String, String>> getContent(String moduleId) {
		List<Map<String, String>> content = new ArrayList<Map<String, String>>();

		List<Instance> instances = instanceMapper.getInstanceByModuleId(moduleId);
		for (Instance instance : instances) {
			Map<String, String> values = new HashMap<String, String>();
			// ??????????????????
			List<Map<String, String>> forms = formValueMapper.getFormValueMapByInstanceId(instance.getId());
			if (null == forms || forms.isEmpty()) {
				continue;
			}
			for (Map<String, String> formMap : forms) {
				String formCode = formMap.get("formCode");
				String formValue = formMap.get("formValue");
				values.put(formCode, formValue);
			}
			values.put("2_ID", instance.getId());

			// ??????????????????
			List<Map<String, String>> relations = instanceRelationMapper.getMuduleRelationIdAndInstanceName(instance.getId());
			for (Map<String, String> map : relations) {
				String moduleRelationId = map.get("moduleRelationId");
				String instanceName = map.get("instanceName");
				if (values.containsKey(moduleRelationId)) {
					values.put(moduleRelationId, values.get(moduleRelationId) + "\r\n" + instanceName);
				} else {
					values.put(moduleRelationId, instanceName);
				}
			}
			content.add(values);
		}
		return content;
	}

	/**
	 * ??????????????????????????????excel
	 * 
	 * @param titles
	 * @param
	 * @param workbook
	 */
	public void generateExcelForAs(List<PoiTitle> titles, List<Map<String, String>> contents, HSSFWorkbook workbook, String moduleId) {
		// ??????
		HSSFCellStyle commonCellStyle = workbook.createCellStyle();
		commonCellStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);// ????????????
		commonCellStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);// ????????????
		commonCellStyle.setWrapText(true);// ??????????????????

		// ??????
		HSSFSheet sheet = workbook.createSheet();
		workbook.setSheetName(0, getModuleName(moduleId));
		sheet.setColumnWidth(0, 10 * 256); // ???????????????
		sheet.setDefaultColumnWidth(22); // ????????????
		// sheet.setDefaultRowHeight((short)(13*20));

		int excelRow = 0;
		int excelColomn = 0;
		HSSFRow row = null;

		// ????????? ?????????
		row = sheet.createRow(excelRow);
		for (PoiTitle pt : titles) {
			HSSFCellStyle firstColumnStyle = workbook.createCellStyle();
			firstColumnStyle.setAlignment(HSSFCellStyle.ALIGN_CENTER);// ????????????
			firstColumnStyle.setVerticalAlignment(HSSFCellStyle.VERTICAL_CENTER);// ????????????
			firstColumnStyle.setWrapText(true);// ??????????????????
			firstColumnStyle.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
			firstColumnStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

			HSSFCell cell = row.createCell(excelColomn);
			cell.setCellStyle(firstColumnStyle);
			cell.setCellValue(pt.getComment());
			excelColomn++;
		}
		excelRow++;

		// ????????? ???????????????
		excelColomn = 0;
		row = sheet.createRow(excelRow);
		for (PoiTitle pt : titles) {
			HSSFCell cell = row.createCell(excelColomn);
			cell.setCellStyle(commonCellStyle);
			cell.setCellValue(pt.getFormat());
			excelColomn++;
		}
		excelRow++;

		// ????????? ????????????
		excelColomn = 0;
		row = sheet.createRow(excelRow);
		for (PoiTitle pt : titles) {
			HSSFCell cell = row.createCell(excelColomn);
			cell.setCellStyle(commonCellStyle);
			cell.setCellValue(pt.getDef());
			excelColomn++;
		}
		excelRow++;

		if (null == contents) {
			return;
		}

		// ???4-N??? ?????????
		for (int i = 0; i < contents.size(); i++) { // ?????????
			Map<String, String> map = contents.get(i);
			row = sheet.createRow(excelRow);
			HSSFCell firstCell = row.createCell(0);
			firstCell.setCellStyle(commonCellStyle);
			firstCell.setCellValue(i == 0 ? "??????" : "");
			excelColomn = 1;
			for (int t = 1; t < titles.size(); t++) { // ?????????
				HSSFCell cell = row.createCell(excelColomn);
				cell.setCellStyle(commonCellStyle);
				if(Constants.MODULE_FORM_TYPE_LISTSEL.equals(titles.get(t).getFieldType())  || Constants.MODULE_FORM_TYPE_SINGLESEL.equals(titles.get(t).getFieldType())    
				        ){
				    if(titles.get(t).getFormOptions() != null && titles.get(t).getFormOptions().size() > 0){
                        for(FormOptions nf : titles.get(t).getFormOptions()){
                                if(nf.getValue().equals(map.get(titles.get(t).getField()))){
                                    cell.setCellValue(nf.getName()); 
                                }
                        }
                    }
				}else if( Constants.MODULE_FORM_TYPE_MULTISEL.equals(titles.get(t).getFieldType()) 
                        ){
                    List<String> newOps = new ArrayList<String>();
                    newOps = JSON.parseArray(map.get(titles.get(t).getField()),String.class);
                    String newStr = new String("");
                    if(titles.get(t).getFormOptions() != null && titles.get(t).getFormOptions().size() > 0){
                        for(FormOptions nf : titles.get(t).getFormOptions()){
                            for(String n:newOps){
                                if(nf.getValue().equals(n)){
                                    newStr+=nf.getName();
                                    newStr+=",";
                                }
                            }
                        }

                    }
                    if(newStr.contains(",")){newStr = newStr.substring(0, newStr.length() -1);}
                    cell.setCellValue(newStr); 
                }else if( Constants.MODULE_FORM_TYPE_CASCADER.equals(titles.get(t).getFieldType()) 
                        ){
                    List<String> newOps = new ArrayList<String>();
                    newOps = JSON.parseArray(map.get(titles.get(t).getField()),String.class);
                    String newStr = new String("");
                    if(titles.get(t).getCasoptionsBeans() != null && titles.get(t).getCasoptionsBeans().size() > 0){
                        for(CasoptionsBean nf1 : titles.get(t).getCasoptionsBeans()){
                            for(String n:newOps){
                                //??????
                                if(nf1.getValue().equals(n)){
                                    newStr+=nf1.getLabel();
                                    newStr+=",";
                                }
                                //??????
                                if(nf1.getChildren()!= null && nf1.getChildren().size()>0){
                                    for(CasoptionsBean nf2: nf1.getChildren()){
                                        if(nf2.getValue().equals(n)){
                                            newStr+=nf2.getLabel();
                                            newStr+=",";
                                        }
                                            if(nf2.getChildren()!= null && nf2.getChildren().size()>0){
                                                for(CasoptionsBean nf3: nf2.getChildren()){
                                                    if(nf3.getValue().equals(n)){
                                                        newStr+=nf3.getLabel();
                                                        newStr+=",";
                                                    }
                                                }
                                            }
                                    }
                                }
                            }
                        }
                        
                    }
                    if(newStr.contains(",")){newStr = newStr.substring(0, newStr.length() -1);}
                    cell.setCellValue(newStr); 
                }else{
		             cell.setCellValue(map.get(titles.get(t).getField())); 
				}
				excelColomn++;
			}
			excelRow++;
		}

	}

	@Override
	public byte[] getExcelData(String moduleId) {
		ByteArrayOutputStream out = null;
		try {
			HSSFWorkbook workbook = new HSSFWorkbook();
			generateExcelForAs(getTitle(moduleId), getContent(moduleId), workbook, moduleId);
			out = new ByteArrayOutputStream();
			HSSFWorkbook hssWb = (HSSFWorkbook) workbook;
			hssWb.write(out);
		} catch (IOException e) {
			logger.error("??????[" + moduleId + "]excel?????????????????????", e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return out.toByteArray();
	}

	@Override
	public byte[] getExcel(String moduleId) {
		ByteArrayOutputStream out = null;
		try {
			HSSFWorkbook workbook = new HSSFWorkbook();
			generateExcelForAs(getTitle(moduleId), null, workbook, moduleId);
			out = new ByteArrayOutputStream();
			HSSFWorkbook hssWb = (HSSFWorkbook) workbook;
			hssWb.write(out);
		} catch (IOException e) {
			logger.error("??????[" + moduleId + "]excel?????????????????????", e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return out.toByteArray();
	}

	@Override
	public String getModuleName(String moduleId) {
		Module module = moduleMapper.selectByPrimaryKey(moduleId);
		if (null == module) {
			return null;
		}
		return module.getName();
	}

	@Override
	public Map<String, Object> uploadData(MultipartFile file, String circleId) {
		Map<String, Object> map = new HashMap<String, Object>();
		InputStream is = null;
		Workbook wb = null;
		try {
			is = file.getInputStream();
			// ??????????????????????????????2003????????????2007??????
			if (ExcelImportUtils.isExcel2007(file.getOriginalFilename())) {
				wb = new XSSFWorkbook(is);
			} else {
				wb = new HSSFWorkbook(is);
			}
			map = readExcel(wb,circleId);
		} catch (Exception e) {
			map.put("success", false);
			map.put("message", "excel???????????????");
			logger.error("excel???????????????", e);
		}
		return map;

	}

	public Map<String, Object> readExcel(Workbook wb, String circleId) {
		Map<String, Object> map = new HashMap<String, Object>();
		// ???????????????shell
		Sheet sheet = wb.getSheetAt(0);

		// excel?????????????????????
		String name = sheet.getSheetName();
		Module module = moduleService.getModuleByModuleName(name);
		// ??????Excel?????????
		int totalRows = sheet.getPhysicalNumberOfRows();
		if (totalRows <= 3) {
			map.put("success", false);
			map.put("message", "excel??????????????????");
			return map;
		}
		// ?????????
		int totalCells = 0;
		// ??????Excel?????????(??????????????????)?????????????????????
		if (sheet.getRow(1) != null) {
			totalCells = sheet.getRow(1).getPhysicalNumberOfCells();
		}

		List<PoiTitle> titles = getTitle(module.getId());
//		if (totalCells <= 2 || totalCells < titles.size()) {
//			map.put("success", false);
//			map.put("message", "excel??????????????????");
//			return map;
//		}

		String message = "";
		Boolean result = true;
		// ??????Excel??????,???????????????????????????
		for (int r = 3; r < totalRows; r++) {
			List<PoiTitle> pts = new ArrayList<PoiTitle>();
			// ??????Excel??????,???????????????????????????
			for (int c = 1; c < totalCells; c++) {
				if (c > titles.size()) {
					break;
				}
				PoiTitle pt = (PoiTitle) titles.get(c).clone();
				pt.setRowNum(r + 1);
				pt.setColumnNum(c + 1);
				Cell cc = sheet.getRow(r).getCell(c);
				if(cc!=null){
				      cc.setCellType(Cell.CELL_TYPE_STRING);
			     }
				pt.setFieldValue(cc==null ? "" :cc.getStringCellValue().trim());
				pts.add(pt);
			}
			// ???????????????
			message += validatePoi(pts, module,circleId);
		}

		if (message.trim().length() > 0) {
			result = false;
		}

		map.put("success", result);
		map.put("message", message);
		return map;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
    @Transactional
	public String validatePoi(List<PoiTitle> pts, Module module, String circleId) {
		String message = "";
		// ??????????????????
		for (PoiTitle pt : pts) {
			String fieldValue = pt.getFieldValue();
			boolean isError = false;
			message += "???" + pt.getRowNum() + "??????" + pt.getColumnNum() + "???"+"[??????:"+pt.getFormName()+"]";
			if (StringUtils.isEmpty(fieldValue)) {
			    message="";
				continue;
			}
			switch (pt.getFieldType()) {
			case "ID": // ??????
				Instance uploadInstance = instanceMapper.getInstanceById(fieldValue);
				if(org.apache.commons.lang.StringUtils.isNotBlank(fieldValue) && uploadInstance == null ){
                    message += "????????????ID???????????????";
                    isError = true;
                    return message;
				}
				if (null != uploadInstance && !uploadInstance.getModuleId().equals(module.getId())) {
					message += "??????????????????????????????";
					isError = true;
					return message;
				}
				break;
			case Constants.MODULE_FORM_TYPE_SINGLEROWTEXT: // ????????????
			    if("Y_name".equals(pt.getFormCode())){
			        Map inMap = new HashMap();
			        inMap.put("name", fieldValue);
			        inMap.put("moduleId", module.getId());
			        List<Map> clist = instanceMapper.checkInstanceName(inMap);
			        if( StringUtils.isEmpty(pts.get(0).getFieldValue())){
			            if(clist != null && clist.size() > 0 ){
			            message += fieldValue+"????????????";
			            isError = true;
			            return message;
			            }
			        }else{
			            Instance ins = instanceMapper.getInstanceById(pts.get(0).getFieldValue());
			            if(ins != null && !ins.getName().equals(fieldValue) && clist != null && clist.size() > 0 ){
		                     message += fieldValue+"????????????";
		                        isError = true;
		                        return message;
			            }
			        }
			    }
			    
				if (null != pt.getMinLength() && fieldValue.length() < pt.getMinLength()) {
					message += "??????????????????" + pt.getMinLength() + "??????";
					isError = true;
					return message;
				}
				if (null != pt.getMaxLength() && fieldValue.length() > pt.getMaxLength()) {
					message += "??????????????????" + pt.getMaxLength() + "??????";
					isError = true;
					return message;
				}
				if (null != pt.getValidation()) {
					FormRule formRule = formRuleMapper.selectByCode(pt.getValidation());
					if (null != formRule && !StringUtils.matches(formRule.getRule(), fieldValue)) {
						message += pt.getValidation() + "???????????????";
						isError = true;
						return message;
					}
				}
				break;
			case Constants.MODULE_FORM_TYPE_MULTIROWTEXT: // ????????????
				if (null != pt.getMinLength() && fieldValue.length() < pt.getMinLength()) {
					message += "??????????????????" + pt.getMinLength() + "??????";
					isError = true;
					return message;
				}
				if (null != pt.getMaxLength() && fieldValue.length() > pt.getMaxLength()) {
					message += "??????????????????" + pt.getMaxLength() + "??????";
					isError = true;
					return message;
				}
				break;
			case Constants.MODULE_FORM_TYPE_LISTSEL: // ????????????
				if (null != pt.getOptions() && !pt.getOptions().isEmpty() && !pt.getOptions().contains(fieldValue)) {
					message += "?????????????????????";
					isError = true;
					return message;
				}
				break;
			case Constants.MODULE_FORM_TYPE_SINGLESEL: // ??????
				if (null != pt.getOptions() && !pt.getOptions().isEmpty() && !pt.getOptions().contains(fieldValue)) {
					message += "?????????????????????";
					isError = true;
					return message;
				}
				break;
			case Constants.MODULE_FORM_TYPE_MULTISEL: // ??????
				if (null != pt.getOptions() && !pt.getOptions().isEmpty()) {
					String[] options = fieldValue.split(",");
					for (String option : options) {
						if (!pt.getOptions().contains(option)) {
							message += "????????????????????????";
							isError = true;
							return message;
						}
					}
				}
				break;
			case Constants.MODULE_FORM_TYPE_DOUBLE: // ??????
				if (!StringUtils.isNum(fieldValue)) {
					message += "????????????";
					isError = true;
					return message;
				}
				Double doubleFieldValue = Double.valueOf(fieldValue);
				if (null != pt.getPrecision()) {
					BigDecimal bg = new BigDecimal(doubleFieldValue);
					doubleFieldValue = bg.setScale(Integer.valueOf(pt.getPrecision()), BigDecimal.ROUND_HALF_UP).doubleValue();
					pt.setFieldValue(doubleFieldValue.toString()); // ??????????????????????????????
				}
				if (null != pt.getMin() && doubleFieldValue < pt.getMin()) {
					message += "?????????????????????" + pt.getMin();
					isError = true;
					return message;
				}
				if (null != pt.getMax() && doubleFieldValue > pt.getMax()) {
					message += "?????????????????????" + pt.getMin();
					isError = true;
					return message;
				}

				break;
			case Constants.MODULE_FORM_TYPE_INT:// ??????
				if (!StringUtils.isInt(fieldValue)) {
					message += "????????????";
					isError = true;
					return message;
				}
				Double intFieldValue = Double.valueOf(fieldValue);
				if (null != pt.getMin() && intFieldValue < pt.getMin()) {
					message += "?????????????????????" + pt.getMin();
					isError = true;
					return message;
				}
				if (null != pt.getMax() && intFieldValue > pt.getMax()) {
					message += "?????????????????????" + pt.getMin();
					isError = true;
					return message;
				}
				break;

			case Constants.MODULE_FORM_TYPE_DATETIME:// ??????
				Date finalDate = null;
				String finalFormat = null;
				if (pt.getFormatDate()) {
					finalDate = DateUtils.getDateFromString(fieldValue, DateUtils.DEFAULT_DATE_FMT);
					if (null == finalDate) {
						message += "????????????????????????";
						isError = true;
						return message;
					}

					finalFormat = DateUtils.DEFAULT_DATE_FMT;
				} else {
					finalDate = DateUtils.getDateFromString(fieldValue, DateUtils.DEFAULT_DATETIME_FMT);
					if (null == finalDate) {
						message += "????????????????????????";
						isError = true;
						return message;
					}
					finalFormat = DateUtils.DEFAULT_DATETIME_FMT;
				}
				pt.setFieldValue(DateUtils.datetimeToString(finalFormat, finalDate)); // ????????????
				break;
			case Constants.MODULE_FORM_TYPE_CASCADER: // ??????
				// ?????????????????????
				break;
			case Constants.MODULE_RELATION_SOURCE: // ????????????
				String[] targetNames = fieldValue.split("\n");
				// ????????????????????????id
				for (String targetName : targetNames) {
                    Map inMap = new HashMap();
                    inMap.put("name", targetName);
                    inMap.put("moduleId", pt.getModuleId());
                    List<Map> clist = instanceMapper.checkInstanceName(inMap);
                    if(clist != null && clist.size() > 0){
						String targetModuleId = pt.getModuleId();
						// ??????????????????
						ModuleRelation mr = new ModuleRelation();
						mr.setSourceModuleId(module.getId());
						mr.setTargetModuleId(targetModuleId);
						mr = moduleRelationMapper.getModuleRelation(mr);
						if (null == mr) {
							message += "???????????????????????????";
							isError = true;
							return message;
						}

						if (!mr.getId().equals(pt.getField())) {
							message += "????????????????????????";
							isError = true;
							return message;
						}

						// ?????????????????????????????????
						if (mr.getRestriction().equals(Constants.MODULE_RELATION_ONETOONE)
								|| mr.getRestriction().equals(Constants.MODULE_RELATION_MANYTOONE)) {
//                            Map inMap = new HashMap();
//                            inMap.put("moduleRelationId", mr.getId());
//                            List<Map> clist = moduleRelationMapper.checkRelationInstanceList(inMap);
						    
							if (targetNames.length > 1 ) {
								message += "????????????????????????";
								isError = true;
								return message;
							}
							
					        Map map = new HashMap();
					        map.put("instanceIds", new String[]{(String) clist.get(0).get("id")});
					        map.put("targetModuleId", module.getId());
					        if(StringUtils.isNotEmpty(pts.get(0).getFieldValue())){
					            map.put("targetInstanceId", pts.get(0).getFieldValue());
					        }
					        List<Map> list = instanceRelationMapper.checkInstanceRelation(map);

					        if(list != null && list.size() > 0 ){
					            for(Map m:list){
		                               message += (m.get("sname") + "?????????" + m.get("tname") + "????????????; \n ");
		                                isError = true;
		                               
					            }
                                 if(isError){
                                     return message;
                                 }
					       }
						}
					} else {
						message += "????????????????????????";
						isError = true;
						return message;
					}
				}
				break;
			case Constants.MODULE_RELATION_TARGET: // ????????????
				String[] sourceNames = fieldValue.split("\n");
				// ????????????????????????id
				for (String sourceName : sourceNames) {
                    Map inMap = new HashMap();
                    inMap.put("name", sourceName);
                    inMap.put("moduleId", pt.getModuleId());
                    List<Map> clist = instanceMapper.checkInstanceName(inMap);
                    if(clist != null && clist.size() > 0){
						String sourceModuleId = pt.getModuleId();
						// ??????????????????
						ModuleRelation mr = new ModuleRelation();
						mr.setSourceModuleId(sourceModuleId);
						mr.setTargetModuleId(module.getId());
						mr = moduleRelationMapper.getModuleRelation(mr);
						if (null == mr) {
							message += "???????????????????????????";
							isError = true;
							return message;
						}

						if (!mr.getId().equals(pt.getField())) {
							message += "????????????????????????";
							isError = true;
							return message;
						}

						// ?????????????????????????????????
						if (mr.getRestriction().equals(Constants.MODULE_RELATION_ONETOONE)
								|| mr.getRestriction().equals(Constants.MODULE_RELATION_ONETOMANY)) {
//			                    Map inMap = new HashMap();
//			                    inMap.put("moduleRelationId", mr.getId());
//			                    List<Map> clist = moduleRelationMapper.checkRelationInstanceList(inMap);
						    
							if (sourceNames.length > 1 ) {
								message += "????????????????????????";
								isError = true;
								return message;
							}
							
	                         Map map = new HashMap();
	                         map.put("instanceIds", new String[]{(String) clist.get(0).get("id")});
	                         map.put("targetModuleId", module.getId());
	                            if(StringUtils.isNotEmpty(pts.get(0).getFieldValue())){
	                                map.put("targetInstanceId", pts.get(0).getFieldValue());
	                            }
	                            List<Map> list = instanceRelationMapper.checkInstanceRelation(map);

	                            if(list != null && list.size() > 0 ){
	                                for(Map m:list){
	                                       message += (m.get("sname") + "?????????" + m.get("tname") + "????????????; \n ");
	                                        isError = true;
	                                       
	                                }
	                                 if(isError){
	                                     return message;
	                                 }
	                           }
						}
					} else {
						message += "????????????????????????";
						isError = true;
						return message;
					}
				}
				break;
			default:
				break;
			}
			if(!isError){
			    message ="";
			}
		}
		
		if(org.apache.commons.lang.StringUtils.isNotBlank(message)){
		    return message;
		}
		

        
		// ????????????/????????????
		String id = pts.get(0).getFieldValue();
		Date date = new Date();
		Instance instance = null;
		Boolean isAdd = true;
		if (StringUtils.isEmpty(id)) { // ????????????
			instance = new Instance();
			instance.setId(UUIDUtil.getUUID());
			instance.setIsDelete(0);
			instance.setModuleId(module.getId());
			instance.setName(pts.get(1).getFieldValue());
			instance.setInsertTime(date);
			instance.setUpdateTime(date);
			instanceMapper.addInstance(instance);
			
		} else { // ????????????
		    isAdd = false;
	        instance = new Instance();
	        instance.setId(id);
			instance.setName(pts.get(1).getFieldValue());
	        instanceMapper.update(instance);

			// ??????????????????
			instanceRelationMapper.deleteByInstanceId(id);
		}
		
        //????????????
        if(!StringUtils.isEmpty(circleId) && !"undefined".equals(circleId)){
            InstanceCircle instanceCircle = new InstanceCircle();
            instanceCircle.setCircleId(circleId);
            instanceCircle.setInstanceId(instance.getId());
            instanceCircleMapper.insert(instanceCircle);
        }

		// ?????????????????????????????????
		List<FormValue> formValues = new ArrayList<FormValue>();
		for (int c = 1; c < pts.size(); c++) {
			PoiTitle pt = pts.get(c);
			if(StringUtils.isEmpty(pt.getFieldValue())){
				continue;
			}
			if (Constants.MODULE_RELATION_SOURCE.equals(pt.getFieldType())) {
				String[] targerNames = pt.getFieldValue().split("\n");
				for (String targerName : targerNames) {
			        Map inMap = new HashMap();
			        inMap.put("name", targerName);
			        inMap.put("moduleId", pt.getModuleId());
			        List<Map> clist = instanceMapper.checkInstanceName(inMap);
			        if(clist != null && clist.size() > 0){
						String targetModuleId = (String) clist.get(0).get("moduleId");
						// ??????????????????
						ModuleRelation mr = new ModuleRelation();
						mr.setSourceModuleId(module.getId());
						mr.setTargetModuleId(targetModuleId);
						mr = moduleRelationMapper.getModuleRelation(mr);

						// ??????????????????
						InstanceRelation ir = new InstanceRelation();
						ir.setId(UUIDUtil.getUUID());
						ir.setModuleRelationId(mr.getId());
						ir.setSourceInstanceId(instance.getId());
						ir.setTargerInstanceId((String) clist.get(0).get("id"));
						ir.setInsertTime(date);
						ir.setUpdateTime(date);
						instanceRelationMapper.insert(ir);
						
						
						//?????????
						try{
    		                Map inmap = new HashMap();
    		                inmap.put("sourceInstanceId", instance.getId());
    		                inmap.put("moduleRelationId", mr.getId());
    		                List<Map> outList = configLogService.getRelationInfoList(inmap);
    		                if(outList!=null && outList.size()>0){
    		                    RelationLog relationLog = new RelationLog();
    		                    relationLog.setAction(Constants.LOG_ACTION_TYPE_ADDINSTANCE_RELATION_NAME);
    		                    relationLog.setCircleId(circleId);
    		                    relationLog.setId(UUIDUtil.getUUID());
    		                    relationLog.setInstanceId(instance.getId());
    		                    
    		                    if(!instance.getId().equals((String) outList.get(0).get("sourceInstanceId"))){
    		                        relationLog.setName((String) outList.get(0).get("targetInstanceName"));
    		                        relationLog.setTargetName((String) outList.get(0).get("sourceInstanceName"));
    		                        }else{
    		                            relationLog.setName((String) outList.get(0).get("sourceInstanceName"));
    		                            relationLog.setTargetName((String) outList.get(0).get("targetInstanceName")); 
    		                   }
    		                    relationLog.setRelationName((String) outList.get(0).get("relationName"));
    		                    relationLogMapper.insert(relationLog);
    		                }
						}catch(Exception e){
				            logger.error("???????????????????????????????????????instanceId:[" + instance.getId().toString() + "]", e);
				            e.printStackTrace();
						}
					}
				}
			} else if (Constants.MODULE_RELATION_TARGET.equals(pt.getFieldType())) {
				String[] sourceNames = pt.getFieldValue().split("\n");
				for (String sourceName : sourceNames) {
				    
                    Map inMap = new HashMap();
                    inMap.put("name", sourceName);
                    inMap.put("moduleId", pt.getModuleId());
                    List<Map> clist = instanceMapper.checkInstanceName(inMap);
                    if(clist != null && clist.size() > 0){

						String sourceModuleId = (String) clist.get(0).get("moduleId");
						// ??????????????????
						ModuleRelation mr = new ModuleRelation();
						mr.setSourceModuleId(sourceModuleId);
						mr.setTargetModuleId(module.getId());
						mr = moduleRelationMapper.getModuleRelation(mr);
						// ??????????????????
						InstanceRelation ir = new InstanceRelation();
						ir.setId(UUIDUtil.getUUID());
						ir.setModuleRelationId(mr.getId());
						ir.setSourceInstanceId((String) clist.get(0).get("id"));
						ir.setTargerInstanceId(instance.getId());
						ir.setInsertTime(date);
						ir.setUpdateTime(date);
						instanceRelationMapper.insert(ir);
						
	                      //?????????
                        try{
                            Map inmap = new HashMap();
                            inmap.put("sourceInstanceId", instance.getId());
                            inmap.put("moduleRelationId", mr.getId());
                            List<Map> outList = configLogService.getRelationInfoList(inmap);
                            if(outList!=null && outList.size()>0){
                                RelationLog relationLog = new RelationLog();
                                relationLog.setAction(Constants.LOG_ACTION_TYPE_ADDINSTANCE_RELATION_NAME);
                                relationLog.setCircleId(circleId);
                                relationLog.setId(UUIDUtil.getUUID());
                                relationLog.setInstanceId(instance.getId());
                                
                                if(!instance.getId().equals((String) outList.get(0).get("sourceInstanceId"))){
                                    relationLog.setName((String) outList.get(0).get("targetInstanceName"));
                                    relationLog.setTargetName((String) outList.get(0).get("sourceInstanceName"));
                                    }else{
                                        relationLog.setName((String) outList.get(0).get("sourceInstanceName"));
                                        relationLog.setTargetName((String) outList.get(0).get("targetInstanceName")); 
                               }
                                relationLog.setRelationName((String) outList.get(0).get("relationName"));
                                relationLogMapper.insert(relationLog);
                            }
                        }catch(Exception e){
                            logger.error("???????????????????????????????????????instanceId:[" + instance.getId().toString() + "]", e);
                            e.printStackTrace();
                        }
					}
				}
			} else {
				FormValue formValue = new FormValue();
				formValue.setId(UUIDUtil.getUUID());
				formValue.setFormId(pt.getFormId());
				formValue.setFormCode(pt.getFormCode());
				 
				if(pt.getFieldType().equals(Constants.MODULE_FORM_TYPE_MULTISEL)){ // ??????????????????
					String[] multiSel = pt.getFieldValue().split(",");
					List<String> rseult = new ArrayList<String>();
					for(String ms : multiSel){
						if(!ms.startsWith("\"") && !ms.endsWith("\"")){
		                    if(pt.getFormOptions() != null && pt.getFormOptions().size() > 0){
		                        for(FormOptions nf : pt.getFormOptions()){
		                                if(nf.getName().equals(ms)){
		                                    rseult.add("\"" + nf.getValue() + "\"");
		                                }
		                        }
		                    }
						}
					}
					pt.setFieldValue(rseult.toString());
				}else if(pt.getFieldType().equals(Constants.MODULE_FORM_TYPE_LISTSEL) || pt.getFieldType().equals(Constants.MODULE_FORM_TYPE_SINGLESEL)){
				    String rseult = "";
                    if(pt.getFormOptions() != null && pt.getFormOptions().size() > 0){
                        for(FormOptions nf : pt.getFormOptions()){
                                if(nf.getName().equals(pt.getFieldValue())){
                                    rseult=nf.getValue();
                                }
                        }
                    }
                    pt.setFieldValue(rseult);
				}else if(pt.getFieldType().equals(Constants.MODULE_FORM_TYPE_CASCADER)){
                    String[] multiSel = pt.getFieldValue().split(",");
                    List<String> rseult = new ArrayList<String>();
                    if(pt.getCasoptionsBeans() != null && pt.getCasoptionsBeans().size() > 0){
                        for(CasoptionsBean nf1 : pt.getCasoptionsBeans()){
                            for(String ms : multiSel){
                                if(!ms.startsWith("\"") && !ms.endsWith("\"")){
                                                //??????
                                                if(nf1.getLabel().equals(ms)){
                                                    rseult.add("\"" + nf1.getValue() + "\"");
                                                }
                                                //??????
                                                if(nf1.getChildren()!= null && nf1.getChildren().size()>0){
                                                    for(CasoptionsBean nf2: nf1.getChildren()){
                                                        if(nf2.getLabel().equals(ms)){
                                                            rseult.add("\"" + nf2.getValue() + "\"");
                                                        }
                                                            if(nf2.getChildren()!= null && nf2.getChildren().size()>0){
                                                                for(CasoptionsBean nf3: nf2.getChildren()){
                                                                    if(nf3.getValue().equals(ms)){
                                                                        rseult.add("\"" + nf3.getValue() + "\"");
                                                                    }
                                                                }
                                                            }
                                                    }
                                                }
                                 }
                                        
                             }
                        }
                  }

                    pt.setFieldValue(rseult.toString());
                }
				
				formValue.setFormValue(pt.getFieldValue());
				
				formValue.setInstanceId(instance.getId());
				formValues.add(formValue);
			}
		}
		if(isAdd){
            configLogService.saveInstanceLog(instance.getId() ,Constants.LOG_ACTION_TYPE_ADDINSTANCE_NAME);
		}else{
            //?????????
            InstanceModel im = new InstanceModel();
            im.setId(id);
            im.setName(pts.get(1).getFieldValue());
            Map inMap = new HashMap();
            inMap.put("instanceId", id);
            im.setFormValues(formValues);
            configLogService.saveInstanceUpdateLog(im);
            
            // ??????????????????????????????
            formValueMapper.deleteByInstanceId(id);
		}
		formValueMapper.insert(formValues);

		return message;
	}
	
    @Override
    public List<Module> selectModule() {
        return maintainViewMapper.selectModule();
    }
}
