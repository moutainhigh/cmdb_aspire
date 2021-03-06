package com.aspire.mirror.theme.server.biz.impl;

import com.alibaba.fastjson.JSON;
import com.aspire.mirror.common.auth.GeneralAuthConstraintsAware;
import com.aspire.mirror.common.constant.Constant;
import com.aspire.mirror.common.entity.Page;
import com.aspire.mirror.common.entity.PageRequest;
import com.aspire.mirror.common.entity.PageResponse;
import com.aspire.mirror.common.entity.Result;
import com.aspire.mirror.common.util.DateUtil;
import com.aspire.mirror.common.util.PageUtil;
import com.aspire.mirror.theme.api.dto.model.BizThemeDTO;
import com.aspire.mirror.theme.api.dto.model.BizThemeDimDTO;
import com.aspire.mirror.theme.api.dto.model.BizThemeDimData;
import com.aspire.mirror.theme.api.dto.model.ThemeDataDTO;
import com.aspire.mirror.theme.server.biz.BizThemeBiz;
import com.aspire.mirror.theme.server.biz.handler.ElasticsearchHandler;
import com.aspire.mirror.theme.server.biz.handler.MergeTask;
import com.aspire.mirror.theme.server.biz.helper.Reactor;
import com.aspire.mirror.theme.server.clientservice.elasticsearch.ThemeDataServiceClient;
import com.aspire.mirror.theme.server.dao.BizThemeDao;
import com.aspire.mirror.theme.server.dao.BizThemeDimDao;
import com.aspire.mirror.theme.server.dao.ThemeKeyDao;
import com.aspire.mirror.theme.server.dao.po.BizTheme;
import com.aspire.mirror.theme.server.dao.po.BizThemeDim;
import com.aspire.mirror.theme.server.dao.po.ThemeKey;
import com.aspire.mirror.theme.server.dao.po.transform.BizThemeTransformer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 * ???????????????????????????
 * <p>
 * ????????????:  mirror??????
 * ???:        com.aspire.mirror.theme.server.biz.impl
 * ?????????:    BizThemeBizImpl.java
 * ?????????:    ???????????????????????????
 * ?????????:    JinSu
 * ????????????:  2018/10/23 19:37
 * ??????:      v1.0
 */
@Service
public class BizThemeBizImpl implements BizThemeBiz {
    public static final String UP_STATUS_SUCCESS = "0";
    public static final String DELETE_FLAG_DELETED = "1";
    public static final String DELETE_FLAG_EXIST = "0";
    @Value("${index_name:mirror-theme-*}")
    private String indexName;

//    @Autowired
//    private ElasticSearchHelper elasticSearchClient;

    @Autowired
    private ThemeKeyDao themeKeyDao;

    @Autowired
    private ThemeDataServiceClient themeDataService;

    /**
     * ????????????
     *
     * @param bizThemeDTO ????????????
     * @return String ??????Id
     */
    @Override
    @Transactional
    public String insert(BizThemeDTO bizThemeDTO) {
        if (null == bizThemeDTO) {
            LOGGER.error("method[insert] param[bizThemeDTO] is null");
            throw new RuntimeException("param[bizThemeDTO] is null");
        }
        String themeId = UUID.randomUUID().toString();
        BizTheme bizTheme = BizThemeTransformer.toPo(bizThemeDTO);
        bizTheme.setThemeId(themeId);
        //???????????????????????????
        bizTheme.setUpStatus(UP_STATUS_SUCCESS);
        bizTheme.setDeleteFlag(DELETE_FLAG_EXIST);
        Date now = new Date();
        bizTheme.setCreateTime(now);
        bizTheme.setUpdateTime(now);
        bizTheme.setIndexName("mirror-theme-" + themeId);
//        if (StringUtils.isEmpty(bizTheme.getIndexName())) {
//            bizTheme.setIndexName(indexName);
//        }
        if (StringUtils.isEmpty(bizTheme.getThemeCode())) {
            String themeCode;
            if (StringUtils.isEmpty(bizTheme.getObjectId())) {
                themeCode = MessageFormat.format("02{0}", themeId.replace("-",""));
            } else {
                themeCode = MessageFormat.format("{0}01{1}", bizTheme.getObjectId(), themeId.replace("-",""));
            }
            bizTheme.setThemeCode(themeCode);
        } else {
            BizTheme result = bizThemeDao.selectByThemeCode(bizTheme.getThemeCode());
            if (result != null) {
                LOGGER.error("????????????????????????????????????");
                throw new RuntimeException("????????????????????????????????????");
            }
        }
        List<BizThemeDimDTO> dimDTOList = bizThemeDTO.getDimList();
        //??????????????????
        String logReg = insertDimList(dimDTOList, themeId);
//        bizTheme.setDimIds(dimIds);
        bizTheme.setLogReg(logReg);
        bizThemeDao.insert(bizTheme);
        //????????????key
        if (!StringUtils.isEmpty(bizThemeDTO.getThemeKey())) {
            ThemeKey themeKey = new ThemeKey();
            themeKey.setThemeId(themeId);
            themeKey.setDimCode(bizThemeDTO.getThemeKey());
            themeKeyDao.insert(themeKey);
        }
        return themeId;
    }

    /**
     * ????????????
     *
     * @param bizThemeDTO ????????????
     */
    @Override
    @Transactional
    public void updateByPrimaryKey(BizThemeDTO bizThemeDTO) {
        BizTheme bizTheme = BizThemeTransformer.toPo(bizThemeDTO);
        List<BizThemeDimDTO> dimDTOList = bizThemeDTO.getDimList();

        // ???????????????null ???????????????ID????????????????????????
        if (dimDTOList != null) {
            bizThemeDimDao.deleteByThemeId(bizThemeDTO.getThemeId());
        }
        //??????????????????
        String logReg = insertDimList(dimDTOList, bizThemeDTO.getThemeId());
//            bizTheme.setDimIds(dimIds);
        bizTheme.setLogReg(logReg);
        if (StringUtils.isEmpty(bizTheme.getThemeCode())) {
            String themeCode = MessageFormat.format("{0}01{1}", bizTheme.getObjectId(), bizTheme.getThemeId());
            bizTheme.setThemeCode(themeCode);
        }
        bizTheme.setUpdateTime(new Date());
        bizThemeDao.updateByPrimaryKey(bizTheme);
        //????????????key
        themeKeyDao.deleteByThemeId(bizThemeDTO.getThemeId());
        if (!StringUtils.isEmpty(bizThemeDTO.getThemeKey())) {
            ThemeKey themeKey = new ThemeKey();
            themeKey.setThemeId(bizThemeDTO.getThemeId());
            themeKey.setDimCode(bizThemeDTO.getThemeKey());
            themeKeyDao.insert(themeKey);
        }
    }

    private String insertDimList(List<BizThemeDimDTO> dimDTOList, String themeId) {
//        List<Integer> dimIds = Lists.newArrayList();
        StringBuilder logReg = new StringBuilder();
        if (!CollectionUtils.isEmpty(dimDTOList)) {
            List<BizThemeDim> bizThemeDimList = Lists.newArrayList();
            for (BizThemeDimDTO bizThemeDimDTO : dimDTOList) {
                //???????????????????????????
//                logReg.append(bizThemeDimDTO.getDimReg());
                if (bizThemeDimDTO.getMatchFlag().equals("1")) {
                    logReg.append("%{").append(bizThemeDimDTO.getDimReg()).append(":").append(bizThemeDimDTO
                            .getDimCode()
                    ).append("}");
                } else {
                    //?????????????????????
                    String dimReg = bizThemeDimDTO.getDimReg();
                    for (String UnescapedReg : Constant.ESCAPE_DIM_REG_ARRAY) {

                        if (UnescapedReg.equals(dimReg)) {
//                        bizThemeDim.setDimReg("\\" + bizThemeDim.getDimReg());
                            dimReg = "\\" + dimReg;
                            break;
                        }
                    }
                    logReg.append(dimReg);
                }
                BizThemeDim bizThemeDim = new BizThemeDim();
                BeanUtils.copyProperties(bizThemeDimDTO, bizThemeDim);
                //?????????????????????ID
                bizThemeDim.setThemeId(themeId);
                bizThemeDimList.add(bizThemeDim);
            }
            bizThemeDimDao.insertBatch(bizThemeDimList);
            return logReg.toString();
        }
        return "";
    }

    /**
     * ????????????
     *
     * @param themeId ??????ID
     */
    @Override
    public void deleteByPrimaryKey(String themeId) {
        BizTheme bizTheme = new BizTheme();
        bizTheme.setThemeId(themeId);
        bizTheme.setUpdateTime(new Date());
        bizTheme.setDeleteFlag(DELETE_FLAG_DELETED);
        bizThemeDao.updateByPrimaryKey(bizTheme);
    }

    /**
     * ????????????
     *
     * @param pageRequest ??????????????????
     * @return PageResponse<BizThemeDTO> ????????????????????????
     */
    @Override
    public PageResponse<BizThemeDTO> pageList(PageRequest pageRequest) {

        Page page = PageUtil.convert(pageRequest);
        int count = bizThemeDao.pageListCount(page);
        PageResponse<BizThemeDTO> pageResponse = new PageResponse<BizThemeDTO>();
        pageResponse.setCount(count);
        if (count > 0) {
            List<BizTheme> listBizTheme = bizThemeDao.pageList(page);
            List<BizThemeDTO> listDTO = BizThemeTransformer.fromPo(listBizTheme);
            pageResponse.setResult(listDTO);
        }
        return pageResponse;
    }

    /**
     * ??????????????????
     *
     * @param param ??????????????????
     * @return ist<BizThemeDTO> ????????????
     */
    @Override
    public List<BizThemeDTO> select(BizThemeDTO param) {
        BizTheme bizTheme = BizThemeTransformer.toPo(param);
        List<BizTheme> bizThemeList = bizThemeDao.select(bizTheme);
        return BizThemeTransformer.fromPo(bizThemeList);
    }

    /**
     * ??????????????????
     *
     * @param themeId ??????ID
     * @return BizThemeDTO ????????????
     */
    @Override
    public BizThemeDTO selectByPrimaryKey(String themeId, GeneralAuthConstraintsAware authParam) {
        BizTheme bizTheme = bizThemeDao.selectByPrimaryKey(themeId, authParam);
        if (bizTheme != null) {
            BizThemeDTO bizThemeDTO = BizThemeTransformer.fromPo(bizTheme);
//            String[] dimIds = bizTheme.getDimIds().split(":");
            List<BizThemeDim> listDim = bizThemeDimDao.selectByThemeId(themeId);
            List<BizThemeDimDTO> dimDTOList = Lists.newArrayList();
            for (BizThemeDim dim : listDim) {
                BizThemeDimDTO bizThemeDimDTO = new BizThemeDimDTO();
                BeanUtils.copyProperties(dim, bizThemeDimDTO);
                dimDTOList.add(bizThemeDimDTO);
            }
            bizThemeDTO.setDimList(dimDTOList);
            ThemeKey themeKey = themeKeyDao.selectByThemeId(themeId);
            if (themeKey != null && themeKey.getDimCode() != null) {
                bizThemeDTO.setThemeKey(themeKey.getDimCode());
            }
            return bizThemeDTO;
        }
        return null;
    }

    /**
     * ??????????????????
     *
     * @param themeDataDTO ??????????????????
     * @return Result ????????????
     */
    @Override
    public Result createThemeData(ThemeDataDTO themeDataDTO) {
        themeDataDTO.setMessage(JSON.toJSONString(themeDataDTO));
        themeDataDTO.setReceiveTime(DateUtil.format(new Date(), DateUtil.DATE_TIME_FORMAT));
        //???????????????
        Future<String> f = Reactor.getInstance().submit(new MergeTask(themeDataDTO, elasticsearchHandler,
                bizThemeDao, bizThemeDimDao));
        try {
            String res = f.get();
            if (!StringUtils.isEmpty(res)) {
                return new Result("2", res);
            }
            return Result.success();
        } catch (Exception e) {
            LOGGER.error("???????????????????????????", e);
            return new Result("500", "???????????????????????????");
        }
    }

    /**
     * ??????????????????
     *
     * @param themeId ??????id
     * @param host ??????IP
     * @param bizCode ????????????
     * @param themeCode ????????????
     * @return
     */
    @Override
    public Map<String, Object> getThemeData(String themeId, String host, String bizCode, String themeCode) {
        BizTheme bizTheme = bizThemeDao.selectByPrimaryKey(themeId, new GeneralAuthConstraintsAware());
        String indexName;
        if (bizTheme != null && !StringUtils.isEmpty(bizTheme.getIndexName())) {
            indexName = bizTheme.getIndexName();
        } else {
            return Maps.newHashMap();
        }
        //??????????????????
        Date startDate;
        final Date curDate = new Date();
        if (!StringUtils.isEmpty(bizTheme.getLastUpTime())) {
            startDate = bizTheme.getLastUpTime();
        } else {
            startDate = curDate;
        }
        long startTime = startDate.getTime() - 100 * 1000;
        final long endTime = curDate.getTime() + 100 * 1000;
        Map<String, Object> resMap = themeDataService.getThemeData(indexName, startTime, endTime, host, bizCode, themeCode);
        if (resMap != null) {
            resMap.put("dim_list", dimIdsToDimNamesByResMap(resMap, bizTheme));
        }
        return resMap;
    }

    @Override
    @Deprecated
    public Map<String, Object> thematicHistoryInfo(String indexName, String themeCode, Long startTime, Long endTime) {
        final Map<String, Object> res = new HashMap<>();
        List<Object[]> itemValuesList = themeDataService.themeHistoryInfo(indexName, themeCode, startTime, endTime);
        res.put("lstDetail", itemValuesList);
        return res;
    }

    @Override
    public Map<String, Object> getThemeDataHis(String themeId) {
        BizTheme bizTheme = bizThemeDao.selectByPrimaryKey(themeId, new GeneralAuthConstraintsAware());
//        String indexName = bizTheme.getIndexName();
        Map<String, Object> resMap = themeDataService.getThemeDataHis(bizTheme);
        if (resMap != null) {
            resMap.put("dim_list", dimIdsToDimNamesByResMap(resMap, bizTheme));
        }
        return resMap;
    }

    // ??????elSearch?????????map???????????????????????????????????????
    public List<BizThemeDimData> dimIdsToDimNamesByResMap(Map<String, Object> resMap, BizTheme bizTheme) {
        final List<BizThemeDimData> dimNameAndValueList = new ArrayList<BizThemeDimData>();
        List<BizThemeDim> bizThemeDimList = bizThemeDimDao.selectByThemeId(bizTheme.getThemeId());
        for (BizThemeDim bizThemeDim : bizThemeDimList) {
            if (resMap.get(bizThemeDim.getDimCode()) != null) {
                final BizThemeDimData jkzbDimTmp = new BizThemeDimData();
                jkzbDimTmp.setDimName(bizThemeDim.getDimName());
                jkzbDimTmp.setDimValue((String) resMap.get(bizThemeDim.getDimCode()));
                try {
                    final String dimType = bizThemeDim.getDimType();
                    if (!StringUtils.isEmpty(dimType)
                            && "3".equalsIgnoreCase(dimType) && !bizThemeDim.getDimCode().equals("timestamp")) {
                        String dateDimValue = (String) resMap.get(bizThemeDim.getDimCode());
                        final Date transfTimestamp = DateUtil.getDate(dateDimValue,
                                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
                        dateDimValue = DateUtil.format(transfTimestamp, "yyyyMMddHHmmss");

                        jkzbDimTmp.setDimValue(dateDimValue);
                    }
                } catch (final Exception e) {
                    LOGGER.error("dimIdsToDimNamesByResMap()--> type conversion error:", e);
                }
                dimNameAndValueList.add(jkzbDimTmp);
            }

        }
        return dimNameAndValueList;
    }

    Logger LOGGER = LoggerFactory.getLogger(BizThemeBizImpl.class);

    @Autowired
    private BizThemeDao bizThemeDao;

    @Autowired
    private ElasticsearchHandler elasticsearchHandler;

    @Autowired
    private BizThemeDimDao bizThemeDimDao;

}
