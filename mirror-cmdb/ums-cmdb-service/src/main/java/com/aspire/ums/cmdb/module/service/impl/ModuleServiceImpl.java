package com.aspire.ums.cmdb.module.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aspire.ums.cmdb.common.Constants;
import com.aspire.ums.cmdb.maintain.entity.ConfigLog;
import com.aspire.ums.cmdb.maintain.mapper.CircleMapper;
import com.aspire.ums.cmdb.maintain.mapper.ConfigLogMapper;
import com.aspire.ums.cmdb.maintain.mapper.InstanceMapper;
import com.aspire.ums.cmdb.maintain.mapper.InstanceRelationMapper;
import com.aspire.ums.cmdb.maintain.mapper.MaintainViewMapper;
import com.aspire.ums.cmdb.module.entity.Form;
import com.aspire.ums.cmdb.module.entity.FormParam;
import com.aspire.ums.cmdb.module.entity.FormScript;
import com.aspire.ums.cmdb.module.entity.FormTag;
import com.aspire.ums.cmdb.module.entity.Module;
import com.aspire.ums.cmdb.module.mapper.FormMapper;
import com.aspire.ums.cmdb.module.mapper.FormParamMapper;
import com.aspire.ums.cmdb.module.mapper.FormScriptMapper;
import com.aspire.ums.cmdb.module.mapper.FormTagMapper;
import com.aspire.ums.cmdb.module.mapper.ModuleMapper;
import com.aspire.ums.cmdb.module.service.ModuleService;
import com.aspire.ums.cmdb.util.BusinessException;
import com.aspire.ums.cmdb.util.UUIDUtil;
/**
 * 
 * <p>Project: ums-cmdb-service</p>
 *
 * @Description: 
 *
 * @author: mingjianyong
 *
 * @Date: 2017-6-30
 */
@Service("moduleService")
@Transactional
public class ModuleServiceImpl implements ModuleService {
	
	@Autowired
	private ModuleMapper moduleMapper;
	@Autowired
	private FormMapper formMapper;
	@Autowired
	private FormParamMapper formParmMapper;
	@Autowired
	private FormTagMapper formTagMapper;
    @Autowired
    private InstanceMapper instanceMapper;
    @Autowired
    private CircleMapper circleMapper;
    @Autowired
    private FormScriptMapper formScriptMapper;
    @Autowired
    private InstanceRelationMapper instanceRelationMapper;
    @Autowired
    private ConfigLogMapper configLogMapper;
	   
	private static Logger logger = Logger.getLogger(ModuleServiceImpl.class);
	
	@Override
	@Transactional
	public Module addModule(Module module,List<String> list) throws Exception {
		String moduleId = UUIDUtil.getUUID();
		module.setId(moduleId);
		module.setDisabled("false");//???????????????
		module.setIsdelete(0);
		module.setBuiltin("false");
		int maxIndex = moduleMapper.selectMaxIndex();
		module.setSortindex(maxIndex+1);
		//???????????????
		moduleMapper.insertSelective(module);
		//?????????????????????????????????????????????????????????
		Form f = new Form();
		String fid = UUIDUtil.getUUID();
		f.setId(fid);
		f.setType(Constants.MODULE_FORM_TYPE_SINGLEROWTEXT);
		f.setBuiltin("true");f.setSortindex(1);
		f.setModuleid(moduleId);f.setCode("Y_name");f.setName("??????");f.setDefaultvalue("");
		f.setKeyattr("true");f.setRequired("true");f.setIsdelete(0);f.setHidden("false");
		//??????????????????????????????
		FormParam fp = new FormParam(UUIDUtil.getUUID(),"validation","",fid,0);
		FormParam fp2 = new FormParam(UUIDUtil.getUUID(),"minLength","1",fid,0);
		FormParam fp3 = new FormParam(UUIDUtil.getUUID(),"maxLength","100",fid,0);
		formParmMapper.insertSelective(fp);
		formParmMapper.insertSelective(fp2);
		formParmMapper.insertSelective(fp3);
		//???????????????????????????
		Form group = new Form();
		group.setId(UUIDUtil.getUUID());group.setName("????????????");group.setIsdelete(0);
		group.setModuleid(moduleId);group.setCode("group");group.setHidden("false");
		group.setKeyattr("false");group.setRequired("false");
		group.setType(Constants.MODULE_FORM_TYPE_GROUPLINE);
		group.setSortindex(0);
		formMapper.insertSelective(f);
		formMapper.insertSelective(group);
		//??????????????????????????????
		for(String s:list){
			FormTag ft = new FormTag();
			ft.setId(UUIDUtil.getUUID());
			ft.setModuleid(moduleId);
			ft.setTag(s);
			formTagMapper.insertSelective(ft);
		}	
		return module;
	}

	@Override
	public List<Module> selectModule() {
		return moduleMapper.selectModule();
	}
	/**
	 * 1.????????????????????????????????????????????????
	 */
	@Override
	@Transactional
	public void deleteModule(Module module) throws BusinessException, Exception {
		if(!"".equals(module.getId()) && null!= module.getId()){
			try {
				formMapper.deleteByModuleId(module.getId());
				module.setIsdelete(1);
				moduleMapper.updateByPrimaryKeySelective(module);
			} catch (Exception e) {
				logger.error("?????????????????????");
				throw new BusinessException("?????????????????????");
			}
		}else{
			logger.error("???????????????????????????ID??????");
			throw new BusinessException("???????????????????????????ID??????");
		}
		
	}

	@Override
	public List<Module> selectSelective(Module module) {
		return moduleMapper.selectSelective(module);
	}
	/**
	 * ????????????
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
	@Transactional
	public void updateModule(Module module,List<FormTag> tags) {
		moduleMapper.updateByPrimaryKeySelective(module);
		//????????????
		if("true".equals(module.getDisabled())){
		    try{
    		    List<Map> ids =   instanceMapper.getInstanceIdByModuleId(module.getId());
    		    if(ids !=null && ids.size()>0 ){
    		        for(Map m:ids){
        	            circleMapper.deleteInstance((String) m.get("instanceId"));
        	            //????????????
        	            Map inMap = new HashMap();
        	            inMap.put("targerInstanceId", (String) m.get("instanceId"));
        	            instanceRelationMapper.delete(inMap);
        	            inMap.clear();
        	            inMap.put("sourceInstanceId", (String) m.get("instanceId"));
        	            instanceRelationMapper.delete(inMap);
        	            
                        ConfigLog configLog = new ConfigLog();
                        configLog.setId(UUIDUtil.getUUID());
                        configLog.setName( (String) m.get("instanceName"));
                        configLog.setModuleName((String) m.get("moduleName"));
                        configLog.setAction(Constants.LOG_ACTION_TYPE_DELINSTANCE_NAME);
                        configLog.setCircleId((String)  m.get("circleId"));
                        configLog.setInstanceId((String) m.get("instanceId"));
                        configLogMapper.insert(configLog);
    		        }
    		    }
		    }catch(Exception e){
                logger.error("????????????[" + module.getId() + "]?????????????????????", e);
                e.printStackTrace();
		    }
		}
		//?????????????????????????????????
		List<String> tagids = formTagMapper.selectTagIdByModuleId(module.getId());
		for (FormTag ft:tags){
			if(ft.getId()!=null && !"".equals(ft.getId())){
				if(tagids.contains(ft.getId())){
					tagids.remove(ft.getId());
				}
				formTagMapper.updateByPrimaryKeySelective(ft);
			}else{
				ft.setId(UUIDUtil.getUUID());
				ft.setModuleid(module.getId());
				formTagMapper.insertSelective(ft);
			}
		}
		//?????????????????????tag
		if(tagids!=null && tagids.size()>0){
			for(String id:tagids){
				formTagMapper.deleteByPrimaryKey(id);
				//???????????????????????????????????????????????????
				formScriptMapper.deleteByTagId(id);
			}
		}
	}

	@Override
	public Module getModuleByModuleName(String moduleName) {
		return moduleMapper.getModuleByModuleName(moduleName);
	}

	@Override
	public List<FormTag> getModuleTag(String mid) {
		return formTagMapper.selectByModuleId(mid);
	}
	/**
	 * 
	 *Description:????????????id??????????????????
	 * @param tagId
	 * @return
	 */
	@Override
	public List<FormScript> getScriptByTagId(String tagId) {
		return formScriptMapper.selectScriptByTag(tagId);
	}

	@Override
	public List<Module> selectModuleByParentId(String parentId) {
		return moduleMapper.selectModuleByParentId(parentId);
	}

}
