/*
 * @Author: isboyjc
 * @Date: 2020-02-18 16:31:25
 * @LastEditors: isboyjc
 * @LastEditTime: 2020-03-01 02:16:17
 * @Description: 消息监听回调
 */
const { Message } = require("wechaty")
const mysqlStore = require('./store/mysql')
const msgProcess = require("./message")


// 消息监听回调
module.exports = bot => {
    return async function onMessage(msg) {
        // 判断消息来自自己，直接return
        if (msg.self()) {
            if (msg.text().includes('【退出登录】')) {
                console.log('机器人退出登录了================')
                bot.stop()
            } else if (msg.text().includes('【重新登录】')) {
                console.log('机器人重新登录了================')
                bot.start()
            }
            return
        }

        // console.log("=============================")
        // console.log(`msg : ${msg}`)
        // console.log(
        //   `from: ${msg.from() ? msg.from().name() : null}: ${
        //     msg.from() ? msg.from().id : null
        //   }`
        // )
        // console.log(`to: ${msg.to()}`)
        // console.log(`text: ${msg.text()}`)
        // console.log(`isRoom: ${msg.room()} ${msg.room()?msg.room().id:''}`)
        // console.log("=============================")

        // 判断此消息类型是否为文本
        if (msg.type() == Message.Type.Text) {
            // 判断消息类型来自群聊
            if (msg.room()) {
                // 获取群聊
                const room = await msg.room()

                // 收到消息，提到自己
                if (await msg.mentionSelf()) {
                    // 获取提到自己的名字
                    let self = await msg.to()
                    self = self ? "@" + self.name() : self
                    // 获取消息内容，拿到整个消息文本，去掉 @+名字
                    let sendText = msg.text().replace(self, "")
                    // 发送咨询格式
                    if (!sendText.includes('【咨询】') && !sendText.includes('【回复】') && !sendText.includes('【群信息】') && !sendText.includes('公告')) {
                        let formatText = '请按照以下格式发布咨询：\n@我【咨询】问题描述'
                        room.say(formatText, msg.from())
                        return
                    }

                    // 请求机器人接口回复
                    let res = await msgProcess.exec(bot, msg)

                    // 返回消息，并@来自人
                    if (res) {
                        room.say(res, msg.from())
                    }
                    return
                }

                //   // 收到消息，没有提到自己  忽略
                // } else {
                //   // 回复信息是关键字 “加群”
                //   if (await isAddRoom(msg)) return

                //   // 回复信息是所管理的群聊名
                //   if (await isRoomName(bot, msg)) return

                //   // 请求机器人聊天接口
                //   let res = await requestRobot(msg.text())
                //   // 返回聊天接口内容
                //   await msg.say(res)
            }
        } else {
            console.log("消息不是文本！")
        }
    }
}

// /**
//  * @description 回复信息是关键字 “加群” 处理函数
//  * @param {Object} msg 消息对象
//  * @return {Promise} true-是 false-不是
//  */
// async function isAddRoom(msg) {
//   // 关键字 加群 处理
//   if (msg.text() == "加群") {
//     let roomListName = Object.keys(roomList)
//     let info = `${name}当前管理群聊有${roomListName.length}个，回复群聊名即可加入哦\n\n`
//     roomListName.map(v => {
//       info += "【" + v + "】" + "\n"
//     })
//     msg.say(info)
//     return true
//   }
//   return false
// }

// /**
//  * @description 回复信息是所管理的群聊名 处理函数
//  * @param {Object} bot 实例对象
//  * @param {Object} msg 消息对象
//  * @return {Promise} true-是群聊 false-不是群聊
//  */
// async function isRoomName(bot, msg) {
//   // 回复信息为管理的群聊名
//   if (Object.keys(roomList).some(v => v == msg.text())) {
//     // 通过群聊id获取到该群聊实例
//     const room = await bot.Room.find({ id: roomList[msg.text()] })

//     // 判断是否在房间中 在-提示并结束
//     if (await room.has(msg.from())) {
//       await msg.say("您已经在房间中了")
//       return true
//     }

//     // 发送群邀请
//     await room.add(msg.from())
//     await msg.say("已发送群邀请")
//     return true
//   }
//   return false
// }

// /**
//  * @description 机器人请求接口 处理函数
//  * @param {String} info 发送文字
//  * @return {Promise} 相应内容
//  */
// function requestRobot(info) {
//   return new Promise((resolve, reject) => {
//     let url = `https://open.drea.cc/bbsapi/chat/get?keyWord=${urlencode(info)}`
//     request(url, (error, response, body) => {
//       if (!error && response.statusCode == 200) {
//         let res = JSON.parse(body)
//         if (res.isSuccess) {
//           let send = res.data.reply
//           // 免费的接口，所以需要把机器人名字替换成为自己设置的机器人名字
//           send = send.replace(/Smile/g, name)
//           resolve(send)
//         } else {
//           if (res.code == 1010) {
//             resolve("没事别老艾特我，我还以为爱情来了")
//           } else {
//             resolve("你在说什么，我听不懂")
//           }
//         }
//       } else {
//         resolve("你在说什么，我脑子有点短路诶！")
//       }
//     })
//   })
// }
