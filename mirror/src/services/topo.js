/*
* 组态模块
*/
import rbHttp from 'assets/js/utility/rb-http.factory'

export default class TopoApi {
    // 根据资源池和ip查询设备详情
    static async queryDeviceByRoomAndIP (obj) {
        return rbHttp.sendRequest({
            method: 'POST',
            data: obj,
            url: '/v1/cmdb/instance/queryDeviceByRoomAndIP'
        }).then(function (data) {
            return data
        })
    }
    // 根据本端设备id查询设备关系
    static async queryInstancePortRelation (obj) {
        return rbHttp.sendRequest({
            method: 'POST',
            data: obj,
            url: '/v1/cmdb/instancePortRelation/list'
        }).then(function (data) {
            return data
        })
    }
    // 根据设备ip和资源池获取告警数量
    static async getAlertCountByIpAndIdc (obj) {
        return rbHttp.sendRequest({
            method: 'POST',
            data: obj,
            url: '/v1/alerts/statistic/getAlertCountByIpAndIdc'
        }).then(function (data) {
            return data
        })
    }
    // 获取资源池
    static async getDictsByType (obj) {
        return rbHttp.sendRequest({
            method: 'GET',
            params: obj,
            url: '/v1/cmdb/configDict/getDictsByType'
        }).then(function (data) {
            return data
        })
    }

    static async saveScadaCanvas (obj) {
        return rbHttp.sendRequest({
            method: 'POST',
            data: obj,
            url: '/v1/scada/saveScadaCanvas'
        }).then(function (data) {
            return data
        })
    }
    //  根据区域ID获取画布
    static async findScadaCanvasList (obj) {
        return rbHttp.sendRequest({
            method: 'POST',
            url: '/v1/scada/findScadaCanvasList',
            data: obj
        }).then(function (data) {
            return data
        })
    }
    static async pageList (obj) {
        return rbHttp.sendRequest({
            method: 'POST',
            url: '/v1/scada/pageList',
            data: obj
        }).then(function (data) {
            return data
        })
    }
    // 查询在线视图
    static async findOnlineScada(obj) {
        return rbHttp.sendRequest({
            method: 'POST',
            url: '/v1/scada/findOnlineScada',
            data: obj
        }).then(function (data) {
            return data
        })
    }
    //  获取视图类型
    static async getScadaCanvasNodeInfo () {
        return rbHttp.sendRequest({
            method: 'GET',
            url: '/v1/scada/getScadaCanvasNodeInfo'
        }).then(function (data) {
            return data
        })
    }

    static async findScadaCanvasInfoById(scadaId) {
        return rbHttp.sendRequest({
            method: 'GET',
            url: '/v1/scada/findScadaCanvasInfoById',
            params: {id: scadaId}
        }).then(function (data) {
            return data
        })
    }
    // 删
    static async delScadaCanvas (obj) {
        return rbHttp.sendRequest({
            method: 'DELETE',
            url: `/v1/scada/delScadaCanvas/${obj.id}`
        }).then(function (data) {
            return data
        })
    }

    static async getPath(obj) {
        return rbHttp.sendRequest({
            method: 'POST',
            url: '/v1/scada/getPath',
            data: obj
        }).then(function (data) {
            return data
        })
    }
}
