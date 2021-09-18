package com.aspire.ums.cmdb.v2.code.service;

import com.aspire.ums.cmdb.code.payload.CmdbModuleCodeValidate;

import java.util.List;

/**
* 描述：
* @author
* @date 2019-05-17 09:51:14
*/
public interface ICmdbModuleCodeValidateService {
     /**
     * 获取所有实例
     * @return 返回所有实例数据
     */
    List<CmdbModuleCodeValidate> list();

    /**
     * 根据主键ID 获取数据信息
     * @param entity 实例信息
     * @return 返回实例信息的数据信息
     */
    CmdbModuleCodeValidate get(CmdbModuleCodeValidate entity);

    /**
     * 新增实例
     * @param entity 实例数据
     * @return
     */
    void insert(CmdbModuleCodeValidate entity);

    /**
     * 修改实例
     * @param entity 实例数据
     * @return
     */
    void update(CmdbModuleCodeValidate entity);

    /**
     * 删除实例
     * @param entity 实例数据
     * @return
     */
    void delete(CmdbModuleCodeValidate entity);
}