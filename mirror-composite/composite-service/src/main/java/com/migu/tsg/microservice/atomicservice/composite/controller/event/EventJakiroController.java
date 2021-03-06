package com.migu.tsg.microservice.atomicservice.composite.controller.event;

import com.aspire.mirror.log.api.dto.EventLogListRequest;
import com.google.gson.Gson;
import com.migu.tsg.microservice.atomicservice.composite.Constants;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.event.EventAtomicClient;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.log.LogClientService;
import com.migu.tsg.microservice.atomicservice.composite.clientservice.log.payload.LogEventPayload;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.RequestAuthContext.RequestHeadUser;
import com.migu.tsg.microservice.atomicservice.composite.controller.authcontext.ResAction;
import com.migu.tsg.microservice.atomicservice.composite.controller.logcontext.LogCodeDefine;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.LogEventUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.PayloadParseUtil;
import com.migu.tsg.microservice.atomicservice.composite.controller.util.ResourceAuthHelper;
import com.migu.tsg.microservice.atomicservice.composite.dao.CompositeResourceDao;
import com.migu.tsg.microservice.atomicservice.composite.dao.po.CompositeResource;
import com.migu.tsg.microservice.atomicservice.composite.exception.ResourceActionAuthException;
import com.migu.tsg.microservice.atomicservice.composite.service.common.payload.BaseResponse;
import com.migu.tsg.microservice.atomicservice.composite.service.event.IEventJakiroService;
import com.migu.tsg.microservice.atomicservice.composite.service.event.dto.EventInfo;
import com.migu.tsg.microservice.atomicservice.composite.service.event.dto.EventLogList;
import com.migu.tsg.microservice.atomicservice.composite.service.event.dto.RequestEventInfo;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@LogCodeDefine("1050114")
public class EventJakiroController implements IEventJakiroService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventJakiroController.class);

    /**
     * ??????????????????
     */
    @Autowired
    private EventAtomicClient client;

    /**
     * ??????????????????
     */
    @Autowired
    private CompositeResourceDao compositeResDao;

    /**
     * ???????????????
     */
    @Autowired
    private LogClientService logClient;

    /**
     * ???????????????
     */
    @Autowired
    private ResourceAuthHelper resAuthHelper;

    private final Gson gson = new Gson();

    /**
     * 
     * ??????:GET /events/{namespace} ??????:???????????????????????????
     * 
     * @author zhangqing Date:2017???10???9?????????4:21:16
     * @see com.migu.tsg.microservice.atomicservice.composite.service.event.
     *      IEventJakiroService#listEvents(java.lang.String, long, long, int)
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "event", action = "view")
    @LogCodeDefine("01")
    public String listEvents(@PathVariable("namespace") String namespace,
            @RequestParam(value = "project_name", required = false) String projectName,
            @RequestParam(value = "start_time") String startTime, @RequestParam(value = "end_time") String endTime,
            @RequestParam(value = "size") String size, @RequestParam(value = "pageno") String pageno,
            @RequestParam(value = "resource_type", required = false) String resourceType,
            @RequestParam(value = "resource_id", required = false) String resourceId,
            @RequestParam(value = "query_string", required = false) String queryString) {
        // 1.??????????????????
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        RequestHeadUser requestHeader = reqCtx.getUser();
        String org = requestHeader.getNamespace();

        //??????
        CompositeResource resource = new CompositeResource();
        resource.setNamespace(namespace);
        resource.setUuid(namespace);
        resource.setName(namespace);
        try {
            resAuthHelper.resourceActionVerify(reqCtx.getUser(), resource, reqCtx.getResAction(),
                    reqCtx.getFlattenConstraints());
        }catch (ResourceActionAuthException e) {
            // ???????????????????????????????????????????????????
            return "";
        }

        
        // 2.?????????????????????
        EventLogListRequest request = new EventLogListRequest();
        // startTime??????????????????1508256000?????????????????????
        long startTimeSecs = Long.parseLong(startTime);
        // endTime??????????????????1510816209.486????????????????????????
        Double endTimeDouble = Double.valueOf(endTime) * Math.pow(10, 6);
        Long endTimeSecs = endTimeDouble.longValue();
        request.setEndTime(endTimeSecs);
        request.setNamespace(org);
        request.setPageNo(pageno);
        request.setResourceType(resourceType);
        request.setResourceUuid(resourceId);
        request.setQueryString(queryString);
        request.setSize(size);
        request.setStartTime(startTimeSecs * 1000 * 1000);
        request.setProjectName(projectName);
        LOGGER.info("1.*******???????????????request???{}", request);

        // 3.???????????????
        String result = client.listEventLogs(gson.toJson(request));
        if(null == result){
            result = "";
        }
        LOGGER.info("2.*******????????????????????????????????????result??????{}", result);

        JSONObject respObj = JSONObject.fromObject(result);
        JSONObject eventsObj = respObj.getJSONObject("events");
        EventLogList eventLogList = PayloadParseUtil.jacksonBaseParse(EventLogList.class, eventsObj);
        int totalPages = eventLogList.getTotalPages();
        eventLogList.setTotalPages(totalPages);
        LOGGER.info("3.*******??????????????????eventLogList??????{}", eventLogList);
        for (EventInfo info : eventLogList.getResults()) {
            String time = info.getTime();
            StringBuilder sb = new StringBuilder(time);
            StringBuilder insert = sb.insert(time.length() - 6, ".");
            info.setTime(insert.toString());
        }
        return PayloadParseUtil.jacksonBase2JsonStr(eventLogList);
    }

    /**
     * 
     * ??????:GET /events/{namespace}/{resource_type} ??????:?????????????????????????????????????????????????????????????????????????????????
     * 
     * @author zhangqing Date:2017???10???9?????????4:21:16
     * @see com.migu.tsg.microservice.atomicservice.composite.service.event.
     *      IEventJakiroService#listEvents(java.lang.String, long, long, int)
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "event", action = "view")
    @LogCodeDefine("02")
    public String listEventsByResourceType(@PathVariable("namespace") String namespace,
            @PathVariable("resource_type") String resourceType,
            @RequestParam(value = "project_name", required = false) String projectName,
            @RequestParam(value = "start_time") String startTime, @RequestParam(value = "end_time") String endTime,
            @RequestParam(value = "size", required = false) String size, @RequestParam(value = "pageno") String pageno,
            @RequestParam(value = "query_string", required = false) String queryString) {
        // 1.??????????????????
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        RequestHeadUser requestHeader = reqCtx.getUser();
        String org = requestHeader.getNamespace();

        //??????
        CompositeResource resource = new CompositeResource();
        resource.setNamespace(namespace);
        resource.setUuid(namespace);
        resource.setName(namespace);
        try {
            resAuthHelper.resourceActionVerify(reqCtx.getUser(), resource, reqCtx.getResAction(),
                    reqCtx.getFlattenConstraints());
        }catch (ResourceActionAuthException e) {
            // ???????????????????????????????????????????????????
            return "";
        }
        
        // 2.?????????????????????
        EventLogListRequest request = new EventLogListRequest();
        // startTime??????????????????1508256000?????????????????????
        long startTimeSecs = Long.parseLong(startTime);
        // endTime??????????????????1510816209.486????????????????????????
        Double endTimeDouble = Double.valueOf(endTime) * Math.pow(10, 6);
        Long endTimeSecs = endTimeDouble.longValue();
        request.setEndTime(endTimeSecs);
        request.setNamespace(org);
        request.setPageNo(pageno);
        request.setQueryString(queryString);
        request.setSize(size);
        request.setStartTime(startTimeSecs * 1000 * 1000);
        request.setResourceType(resourceType);
        request.setProjectName(projectName);
        LOGGER.info("1.???????????????request???{}", request);

        // 3.???????????????
        String result = client.listEventLogs(gson.toJson(request));
        if(null == result){
            result = "";
        }
        LOGGER.info("2.????????????????????????????????????result??????{}", result);

        JSONObject respObj = JSONObject.fromObject(result);
        JSONObject eventsObj = respObj.getJSONObject("events");
        EventLogList eventLogList = PayloadParseUtil.jacksonBaseParse(EventLogList.class, eventsObj);
        int totalPages = eventLogList.getTotalPages() + 1;
        eventLogList.setTotalPages(totalPages);
        LOGGER.info("3.??????????????????eventLogList??????{}", eventLogList);
        for (EventInfo info : eventLogList.getResults()) {
            String time = info.getTime();
            StringBuilder sb = new StringBuilder(time);
            StringBuilder insert = sb.insert(time.length() - 6, ".");
            info.setTime(insert.toString());
        }
        return PayloadParseUtil.jacksonBase2JsonStr(eventLogList);
    }

    /**
     * 
     * ??????:GET /events/{namespace}/{resource_type}/{resource_uuid}
     * ??????:??????????????????????????????????????????????????????????????????????????????
     * 
     * @author zhangqing Date:2017???10???9?????????4:21:16
     * @see com.migu.tsg.microservice.atomicservice.composite.service.event.
     *      IEventJakiroService#listEvents(java.lang.String, long, long, int)
     */
    @Override
    @ResponseStatus(HttpStatus.OK)
    @ResAction(resType = "event", action = "view")
    @LogCodeDefine("03")
    public String listEventsByResourceTypeAndUuid(@PathVariable("namespace") String namespace,
            @PathVariable("resource_type") String resourceType, 
            @PathVariable("resource_uuid") String resourceUuid,
            @RequestParam(value = "start_time") String startTime, 
            @RequestParam(value = "end_time") String endTime,
            @RequestParam(value = "size") String size, 
            @RequestParam(value = "pageno") String pageno,
            @RequestParam(value = "query_string", required = false) String queryString) {
        // 1.??????????????????
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        RequestHeadUser requestHeader = reqCtx.getUser();
        String org = requestHeader.getNamespace();

        //??????
        CompositeResource resource = new CompositeResource();
        resource.setNamespace(namespace);
        resource.setUuid(namespace);
        resource.setName(namespace);
        try {
            resAuthHelper.resourceActionVerify(reqCtx.getUser(), resource, reqCtx.getResAction(),
                    reqCtx.getFlattenConstraints());
        }catch (ResourceActionAuthException e) {
            // ???????????????????????????????????????????????????
            return "";
        }
        
        // 2.?????????????????????
        EventLogListRequest request = new EventLogListRequest();
        // startTime??????????????????1508256000?????????????????????
        long startTimeSecs = Long.parseLong(startTime);
        // endTime??????????????????1510816209.486????????????????????????
        Double endTimeDouble = Double.valueOf(endTime) * Math.pow(10, 6);
        Long endTimeSecs = endTimeDouble.longValue();
        request.setEndTime(endTimeSecs);
        request.setNamespace(org);
        request.setPageNo(pageno);
        request.setQueryString(queryString);
        request.setSize(size);
        request.setStartTime(startTimeSecs * 1000 * 1000);
        request.setResourceType(resourceType);
        request.setResourceUuid(resourceUuid);
        LOGGER.info("1.???????????????request???{}", request);

        // 3.???????????????
        String result = client.listEventLogs(gson.toJson(request));
        if(null == result){
            result = "";
        }
        LOGGER.info("2.????????????????????????????????????result??????{}", result);

        JSONObject respObj = JSONObject.fromObject(result);
        JSONObject eventsObj = respObj.getJSONObject("events");
        EventLogList eventLogList = PayloadParseUtil.jacksonBaseParse(EventLogList.class, eventsObj);
        int totalPages = eventLogList.getTotalPages() + 1;
        eventLogList.setTotalPages(totalPages);
        LOGGER.info("3.??????????????????eventLogList??????{}", eventLogList);
        for (EventInfo info : eventLogList.getResults()) {
            String time = info.getTime();
            StringBuilder sb = new StringBuilder(time);
            StringBuilder insert = sb.insert(time.length() - 6, ".");
            info.setTime(insert.toString());
        }
        return PayloadParseUtil.jacksonBase2JsonStr(eventLogList);
    }

    @Override
    @LogCodeDefine("04")
    public BaseResponse envetInfo(@PathVariable(name = "resource_name") String resource_name,
            @PathVariable("namespace") String namespace,
            @RequestBody RequestEventInfo request) {

        String resourceType = request.getResource_type();
        String operation = request.getOperation();
        RequestAuthContext reqCtx = RequestAuthContext.currentRequestAuthContext();
        CompositeResource accountResource = null;
        if(reqCtx.getUser().getNamespace().equals(resource_name)) {
            accountResource = compositeResDao.queryResourceByName(
                    reqCtx.getUser().getNamespace(), resourceType, resource_name);
         // ????????????
            LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(reqCtx, resourceType, accountResource.getUuid(),
                    resource_name, operation, 1, "generic", accountResource);
            String logJson = LogEventUtil.wrapLogEvents2Json(logEvent);
            logClient.saveEventsLogInfo(logJson);
        }
        if(reqCtx.getUser().getUsername().equals(resource_name)) {
            accountResource = compositeResDao.queryResourceByName(
                    reqCtx.getUser().getNamespace(), Constants.RbacResource.SUBACCOUNT, resource_name);
         // ????????????
            LogEventPayload logEvent = LogEventUtil.buildBasicLogEvent(reqCtx, Constants.RbacResource.SUBACCOUNT, 
                    accountResource.getUuid(), resource_name, operation, 1, "generic", accountResource);
            String logJson = LogEventUtil.wrapLogEvents2Json(logEvent);
            logClient.saveEventsLogInfo(logJson);
        }

        return new BaseResponse();
    }

}
