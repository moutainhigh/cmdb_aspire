package com.aspire.ums.cmdb.maintenance.web;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import com.aspire.ums.cmdb.allocate.util.PageBean;
import com.aspire.ums.cmdb.dict.entity.ConfigDict;
import com.aspire.ums.cmdb.dict.service.ConfigDictService;
import com.aspire.ums.cmdb.maintenance.entity.MaintenHardware;
import com.aspire.ums.cmdb.maintenance.entity.MaintenSoftware;
import com.aspire.ums.cmdb.maintenance.service.MaintenHardService;
import com.aspire.ums.cmdb.maintenance.util.ExcelReaderUtils;
import com.aspire.ums.cmdb.maintenance.util.ExportExcelUtil;
import com.aspire.ums.cmdb.maintenance.util.MaintenHardwareRequest;
import com.aspire.ums.cmdb.maintenance.util.MaintenHardwareResp;
import com.aspire.ums.cmdb.maintenance.util.MaintenSoftwareRequest;
import com.aspire.ums.cmdb.maintenance.util.MaintenSoftwareResp;
import com.aspire.ums.cmdb.maintenance.util.MaintenHardPageRequest;
import com.aspire.ums.cmdb.maintenance.util.MaintenHardPageResp;

import lombok.extern.slf4j.Slf4j;

/**
 * Copyright (C), 2015-2019, ????????????????????????
 * FileName: MaintenHardwareController
 * Date:     2019/3/12 15:43
 * Description: ${DESCRIPTION}
 * History:
 * <author>          <time>          <version>          <desc>
 * ????????????           ????????????           ?????????              ??????
 */
 
@RestController
@Slf4j
@RequestMapping("/v1/cmdb/maintenhard")
public class MaintenHardController   {
	
	 
    @Autowired
    private MaintenHardService  maintenHardService;
    
    
    @Autowired
    private ConfigDictService configDictService;
   
    
    /**
     *  ??????????????????
     * @return ????????????.3
     */
    @PostMapping(value = "/insertMaintenHardware" )
    public String insertMaintenHardware(@RequestBody MaintenHardwareRequest maintenHardwareRequest) {
         
    	log.info("maintenHardwareRequest is {} ",maintenHardwareRequest);
    	 
    	MaintenHardware maintenHardware=new MaintenHardware();
    	
    	BeanUtils.copyProperties(maintenHardwareRequest, maintenHardware);
    	
    	maintenHardService.insertMaintenHardware( maintenHardware );
    	
    	return "success";
    	
    }
    
    
    /**
     *  ????????????????????????id
     * @return  
     */
    @GetMapping(value = "/selectMaintenHardwareById" )
    public MaintenHardwareResp selectMaintenHardwareById( @RequestParam("id") String id ) {
        
    	log.info("id is {} ",id);
    	
    	MaintenHardware maintenHardware=maintenHardService.selectMaintenHardwareById(id);
    	
    	MaintenHardwareResp maintenHardwareResp=new MaintenHardwareResp();
    	
    	BeanUtils.copyProperties(maintenHardware, maintenHardwareResp);
    	 
        return maintenHardwareResp;
    }
     
     
    /**
     *  ??????????????????????????????
     * @return  
     */
    @GetMapping(value = "/selectMaintenHardwareBySoftNmae" )
    public MaintenHardwareResp selectMaintenHardwareBySoftNmae( @RequestParam("device_serial_number") String deviceSerialNumber ) {
           
    	log.info("deviceSerialNumber is {} ",deviceSerialNumber);
    	 
    	
    	MaintenHardware maintenHardware=maintenHardService.selectMaintenHardwareBySoftNmae(deviceSerialNumber);
    	
    	MaintenHardwareResp maintenHardwareResp=new MaintenHardwareResp();
    		 	
        if (maintenHardware!=null) {
    		
    		BeanUtils.copyProperties(maintenHardware, maintenHardwareResp);
		}
    	
        return maintenHardwareResp;
    }
    
    
    
    /**
     *  ??????????????????
     * @return ????????????
     */
    @PostMapping(value = "/updateMaintenHardware" )
    public String updateMaintenHardware(@RequestBody MaintenHardwareRequest maintenHardwareRequest) {
    	
    	log.info("maintenHardwareRequest is {} ",maintenHardwareRequest); 
    	
        MaintenHardware maintenHardware=new MaintenHardware();
    	
    	BeanUtils.copyProperties(maintenHardwareRequest, maintenHardware);
    	
    	
    	maintenHardService.updateMaintenHardware (maintenHardware );
    	
    	return "success";
    	
    }
    
    
    /**
     *  ????????????????????????
     * @return ????????????
     */
    @PostMapping(value = "/batchUpdateMaintenHardware" )
    public String batchUpdateMaintenHardware(@RequestBody MaintenHardwareRequest maintenHardwareRequest) {
    	
    	log.info("maintenHardwareRequest is {} ",maintenHardwareRequest); 
    	
    	 MaintenHardware maintenHardware=new MaintenHardware();
     	
     	BeanUtils.copyProperties(maintenHardwareRequest, maintenHardware);
    	
     	maintenHardService.batchUpdateMaintenHardware(maintenHardware );
    	
    	return "success";
    	
    } 
    
    
     
    
    /**
     *  ??????????????????
     * @return ????????????
     */
    @DeleteMapping(value = "/deleteMaintenHardware" )
    public String deleteMaintenHardware( @RequestParam("ids") String ids ) {
    	
    	log.info("ids is {} ",ids);
    	
    	maintenHardService.deleteMaintenHardwareById(ids);
    	
    	return "success";  
    	
    }
    

   
    /**
     *  ??????????????????????????????
     * @return ????????????
     */
    @PostMapping(value = "/listMaintenHardwareByPage" )  
    public PageBean<MaintenHardPageResp> selectMaintenHardwareByPage( @RequestBody MaintenHardPageRequest maintenHardPageRequest ) {
        
    	log.info("maintenHardPageRequest is {} ",maintenHardPageRequest);
    	
        return maintenHardService.selectMaintenHardByPage(maintenHardPageRequest);
        
    }
       
    
    /**
     *  ????????????????????????
     * @return ????????????
     */
    @PostMapping(value = "/downloadMaintenHardware" ) 
    public List<MaintenHardPageResp> downloadMaintenHardware( @RequestBody MaintenHardPageRequest maintenHardPageRequest  ){
    	
    	log.info("maintenHardPageRequest is {} ",maintenHardPageRequest);
    	
    	return maintenHardService.getMaintenHardwareExcelData(maintenHardPageRequest);
            	
    }
    
    
    /**
     *  ????????????????????????
     * @return ????????????
     */
    @PostMapping(value = "/uploadMaintenHardware" )
	 public String uploadMaintenHardware( @RequestBody List<MaintenHardwareRequest>  maintenHardwareRequestList ) {
  
    	log.info("maintenHardwareRequestList  size is {} ",maintenHardwareRequestList.size());
	  
    	List<MaintenHardware> maintenHardwareList=new ArrayList<MaintenHardware>();
    	
    	
    	for(MaintenHardwareRequest maintenHardwareRequest : maintenHardwareRequestList ){
    		
    		 MaintenHardware maintenHardware=new MaintenHardware();
    	    	
    	     BeanUtils.copyProperties(maintenHardwareRequest, maintenHardware);
    		
    	     maintenHardwareList.add(maintenHardware);
    	}
    	
    	
		maintenHardService.insertMaintenHardwareList(  maintenHardwareList ); 
			 
		return "success";
	}
    
    
    
    
    /**
     *  ????????????????????????
     * @return ????????????
     */
    @PostMapping(value = "/downloadMaintenHardware11" ) 
	public void downloadMaintenHardware11( @RequestBody MaintenHardPageRequest compMaintenManagePageRequest ) {
		
		log.info("compMaintenManagePageRequest is {} ",compMaintenManagePageRequest);
		 
		
		ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        HttpServletResponse response = servletRequestAttributes.getResponse();
		
        List<MaintenHardPageResp> maintenManagePageRespList=null;
        
        if (compMaintenManagePageRequest.getPageSize().equals("0")) {
            maintenManagePageRespList= new ArrayList<MaintenHardPageResp>();
            
		}else{
			
			maintenManagePageRespList= maintenHardService.getMaintenHardwareExcelData(compMaintenManagePageRequest);	
			
		}
        
        List<ConfigDict> dictList= configDictService.selectDictsByType("idcType",null,null,null);
        List<String> resourcePoolList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList){
 			resourcePoolList.add(dict.getName());
 		}
 		
 		List<ConfigDict> dictList1= configDictService.selectDictsByType("bizSystem",null,null,null);
        List<String> systemNameList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList1){
 			systemNameList.add(dict.getName());
 		}
 		
 		List<ConfigDict> dictList2= configDictService.selectDictsByType("device_class",null,null,null);
        List<String> deviceClassifyList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList2){
 			deviceClassifyList.add(dict.getValue());
 		}
 		
 		List<ConfigDict> dictList3= configDictService.selectDictsByType("device_type",null,null,null);
        List<String> deviceTypeList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList3){
 			deviceTypeList.add(dict.getValue());
 		}
 		
 		List<ConfigDict> dictList4= configDictService.selectDictsByType("device_model",null,null,null);
        List<String> deviceModelList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList4){
 			deviceModelList.add(dict.getValue());
 		}
 		
 		List<String> optionsTrueFalseList=new ArrayList<String>();
 		optionsTrueFalseList.add("???");
 		optionsTrueFalseList.add("???");
 		
 		List<String> originBuyExplainList=new ArrayList<String>();
 		originBuyExplainList.add("?????????????????????????????????????????????");
 		originBuyExplainList.add("???????????????????????????");
 		originBuyExplainList.add("??????????????????");
 		originBuyExplainList.add("????????????????????????????????????");
 		
  
 		List<ConfigDict> dictList5= configDictService.selectDictsByType("provider",null,null,null);
        List<String> maintenFactoryList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList5){
 			maintenFactoryList.add(dict.getValue());
 		}
 		
 		List<String> realMaintenList=new ArrayList<String>();
 		realMaintenList.add("??????");
 		realMaintenList.add("?????????");
 		 
        
		
		String[] headerList = {"?????? ","???/??? ","????????? "+resourcePoolList,"????????????"+systemNameList,"????????????"+deviceClassifyList,"???????????? "+deviceTypeList,
				"???????????? "+deviceModelList,"????????????","??????????????? ","???????????? ","????????????","??????????????????"+optionsTrueFalseList,"??????????????????"+optionsTrueFalseList,
				"?????????????????????????????????"+originBuyExplainList,"????????????????????????"+maintenFactoryList,"????????????"+maintenFactoryList,"???????????????????????????","????????????????????????",
   			    "???????????????????????? ","????????????????????????","????????????????????????"+realMaintenList,"?????????" };
        String[] keyList = {"province","city","resourcePool","systemName","deviceClassify","deviceType","deviceModel","deviceName",
        		"deviceSerialNumber","assetsNumber","warrantyDate","buyMainten","originBuy","originBuyExplain","adviceMaintenFactory",
        		"maintenFactory","maintenSupplyContact","maintenFactoryContact","maintenBeginDate","maintenEndDate","realMaintenType",
        		"admin" };
        String title = "??????????????????";
        String fileName = title+".xlsx";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            List<Map<String, Object>> dataLists = new ArrayList<Map<String,Object>>();
            for (MaintenHardPageResp maintenHardPageResp : maintenManagePageRespList) {
                Map<String, Object>  map=ExportExcelUtil.objectToMap(maintenHardPageResp);
           	
           	  if(maintenHardPageResp.getWarrantyDate()!=null){
           		map.put("warrantyDate",  sdf.format(maintenHardPageResp.getWarrantyDate())); 
           	 }
           	 if(maintenHardPageResp.getMaintenBeginDate()!=null){
           		map.put("maintenBeginDate",  sdf.format(maintenHardPageResp.getMaintenBeginDate())); 
           	 }
           	 if(maintenHardPageResp.getMaintenEndDate()!=null){
           		map.put("maintenEndDate",  sdf.format(maintenHardPageResp.getMaintenEndDate())); 
           	 }
           	    	   
               dataLists.add(map);
              
            }
            OutputStream os = response.getOutputStream();// ???????????????
            response.setHeader("Content-Disposition", "attachment;filename=".concat(String.valueOf(URLEncoder.encode(fileName, "UTF-8"))));
            response.setHeader("Connection", "close");
            response.setHeader("Content-Type", "application/vnd.ms-excel");
            //excel constuct
            ExportExcelUtil eeu = new ExportExcelUtil();
            Workbook book = new SXSSFWorkbook(128);
            eeu.exportExcel(book, 0, title, headerList, dataLists, keyList );
            book.write(os);
        } catch (Exception e) {
       	 log.error("??????Excel????????????!", e);
        }
		
	}
    

    /**
     *  ????????????????????????
     * @return ????????????
     */
    @PostMapping(value = "/uploadMaintenHardware11" )
	public Map<String, Object> uploadMaintenHardware11( @RequestParam("file")  MultipartFile file) {
		 
		Map<String, Object> map = new HashMap<String, Object>();
		 
		if (file == null) {
			map.put("success", false);
			map.put("message", "?????????????????????");
			return map;
		}

		String fileName = file.getOriginalFilename();
		log.info("filename is : " + fileName);
		if (!ExcelReaderUtils.validateExcel(fileName)) {
			map.put("success", false);
			map.put("message", "???????????????excel?????????");
			return map;
		}

		
		List<ConfigDict> dictList= configDictService.selectDictsByType("idcType",null,null,null);
        List<String> resourcePoolList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList){
 			resourcePoolList.add(dict.getName());
 		}
 		
 		List<ConfigDict> dictList1= configDictService.selectDictsByType("bizSystem",null,null,null);
        List<String> systemNameList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList1){
 			systemNameList.add(dict.getName());
 		}
 		
 		List<ConfigDict> dictList2= configDictService.selectDictsByType("device_class",null,null,null);
        List<String> deviceClassifyList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList2){
 			deviceClassifyList.add(dict.getValue());
 		}
 		
 		List<ConfigDict> dictList3= configDictService.selectDictsByType("device_type",null,null,null);
        List<String> deviceTypeList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList3){
 			deviceTypeList.add(dict.getValue());
 		}
 		
 		List<ConfigDict> dictList4= configDictService.selectDictsByType("device_model",null,null,null);
        List<String> deviceModelList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList4){
 			deviceModelList.add(dict.getValue());
 		}
 		
 		List<String> optionsTrueFalseList=new ArrayList<String>();
 		optionsTrueFalseList.add("???");
 		optionsTrueFalseList.add("???");
 		
 		List<String> originBuyExplainList=new ArrayList<String>();
 		originBuyExplainList.add("?????????????????????????????????????????????");
 		originBuyExplainList.add("???????????????????????????");
 		originBuyExplainList.add("??????????????????");
 		originBuyExplainList.add("????????????????????????????????????");
 		
  
 		List<ConfigDict> dictList5= configDictService.selectDictsByType("provider",null,null,null);
        List<String> maintenFactoryList=new ArrayList<String>();
 		for(ConfigDict  dict:dictList5){
 			maintenFactoryList.add(dict.getValue());
 		}
 		
 		List<String> realMaintenList=new ArrayList<String>();
 		realMaintenList.add("??????");
 		realMaintenList.add("?????????");
		

		try {
			
			ExcelReaderUtils excelReader = new ExcelReaderUtils();
			 
			List<MaintenHardware> maintenHardwareList=excelReader.doUploadMaintenHardwareData(file ,resourcePoolList, 
		 		      systemNameList,  deviceClassifyList, deviceTypeList,  deviceModelList,  optionsTrueFalseList, originBuyExplainList,
		 		      maintenFactoryList,  realMaintenList);
			
			maintenHardService.insertMaintenHardwareList(maintenHardwareList);
		
			map.put("success", true);
			map.put("message", null); 
			
		} catch (Exception e) {
			
			map.put("success", false);
			map.put("message", e.getMessage());
			
			return map;
		}
			

		return map;
		
		 
	}
    
     
    
}
