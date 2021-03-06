/*
 * @Author: isboyjc
 * @Date: 2020-02-14 13:01:46
 * @LastEditors: isboyjc
 * @LastEditTime: 2020-02-29 18:36:22
 * @Description: wechaty-puppet-padplus入口程序
 */

const { Wechaty, MemoryCard } = require("wechaty") // Wechaty核心包
const { PuppetPadplus } = require("wechaty-puppet-padplus") // padplus协议包
const config = require("./config") // 配置文件

const onScan = require("./onScan") // 机器人需要扫描二维码时监听回调
const onRoomJoin = require("./onRoomJoin") // 加入房间监听回调
const onMessage = require("./onMessage") // 消息监听回调
const onFriendShip = require("./onFriendShip") // 好友添加监听回调

// 初始化
const bot = new Wechaty({
    memory: new MemoryCard({ name: config.memorypath }),
    puppet: new PuppetPadplus({
        token: config.token
    }),
    name: config.name
})

bot
    .on("scan", onScan) // 机器人需要扫描二维码时监听
    //.on("room-join", onRoomJoin) // 加入房间监听
    .on("message", onMessage(bot)) // 消息监听
    //.on("friendship", onFriendShip) // 好友添加监听
    .start()
