package com.zdy.project.wechat_chatroom_helper.wechat.plugins.message

import android.database.Cursor
import com.zdy.project.wechat_chatroom_helper.ChatInfoModel
import com.zdy.project.wechat_chatroom_helper.PageType
import com.zdy.project.wechat_chatroom_helper.io.AppSaveInfo
import com.zdy.project.wechat_chatroom_helper.wechat.plugins.main.adapter.ConversationItemHandler
import com.zdy.project.wechat_chatroom_helper.wechat.plugins.main.adapter.MainAdapter
import com.zdy.project.wechat_chatroom_helper.wechat.WXObject
import de.robv.android.xposed.XposedHelpers

object MessageFactory {

    private const val SqlForGetAllOfficial = "select unReadCount, status, isSend, conversationTime," +
            "rconversation.username, rcontact.nickname, content, msgType ,digest, digestUser, attrflag, editingMsg, " +
            "atCount, unReadMuteCount, UnReadInvite from rconversation, rcontact " +
            "where ( rcontact.username = rconversation.username and rcontact.verifyFlag = 24) and ( parentRef is null  or parentRef = '' )  " +
            "and ( 1 !=1 or rconversation.username like '%@chatroom' or rconversation.username like '%@openim' or rconversation.username not like '%@%' )  " +
            "and rconversation.username != 'qmessage' order by flag desc"

    private const val SqlForGetAllChatRoom = "select unReadCount, status, isSend, conversationTime, " +
            "rconversation.username, rcontact.nickname, content, msgType, digest, digestUser, attrflag, editingMsg, " +
            "atCount, unReadMuteCount, UnReadInvite from rconversation, rcontact " +
            "where  rcontact.username = rconversation.username and  rconversation.username like '%@chatroom' order by flag desc"

    private fun SqlForByUsername(field_username: String) = "select unReadCount, status, isSend, " +
            "conversationTime, rconversation.username, rcontact.nickname, content, msgType, digest," +
            "digestUser, attrflag, editingMsg, atCount, unReadMuteCount, UnReadInvite " +
            "from rconversation, rcontact " +
            "where rconversation.username = rcontact.username and rconversation.username = '$field_username'"


    @JvmStatic
    fun getDataBaseFactory(any: Any) = XposedHelpers.findField(any::class.java, "mCursorFactory").apply { isAccessible = true }.get(any)


    fun getAllChatRoom(): ArrayList<ChatInfoModel> {

        val cursor = XposedHelpers.callMethod(MessageHandler.MessageDatabaseObject, "rawQuery", SqlForGetAllChatRoom, null) as Cursor

        val list = arrayListOf<ChatInfoModel>()

        while (cursor.moveToNext()) {
            list.add(buildChatInfoModelByCursor(cursor))
        }
        return list
    }

    fun getSpecChatRoom(): ArrayList<ChatInfoModel> {
        val list = getAllChatRoom()
        val chatroomList = AppSaveInfo.getWhiteList(AppSaveInfo.WHITE_LIST_CHAT_ROOM)
        return ArrayList(list.filterNot { chatroomList.contains(it.field_username) })
    }

    fun getAllOfficial(): ArrayList<ChatInfoModel> {
        val cursor = XposedHelpers.callMethod(MessageHandler.MessageDatabaseObject, "rawQuery", SqlForGetAllOfficial, null) as Cursor

        val list = arrayListOf<ChatInfoModel>()

        while (cursor.moveToNext()) {
            list.add(buildChatInfoModelByCursor(cursor))
        }
        return list
    }

    fun getSpecOfficial(): ArrayList<ChatInfoModel> {
        val list = getAllOfficial()
        val officialList = AppSaveInfo.getWhiteList(AppSaveInfo.WHITE_LIST_OFFICIAL)
        return ArrayList(list.filterNot { officialList.contains(it.field_username) })
    }

    fun getUnReadCountItem(list: ArrayList<ChatInfoModel>) = list.count { it.unReadCount > 0 }

    fun getSingle(field_username: String) =
            buildChatInfoModelByCursor((XposedHelpers.callMethod(MessageHandler.MessageDatabaseObject,
                    WXObject.Message.M.QUERY, getDataBaseFactory(MessageHandler.MessageDatabaseObject!!),
                    SqlForByUsername(field_username), null, null) as Cursor).apply { moveToNext() })


    private fun buildChatInfoModelByCursor(cursor: Cursor): ChatInfoModel {

        return ChatInfoModel().apply {
            field_username = cursor.getString(cursor.getColumnIndex("username"))
            field_nickname = cursor.getString(cursor.getColumnIndex("nickname"))
            field_content = cursor.getString(cursor.getColumnIndex("content"))
            field_digest = cursor.getString(cursor.getColumnIndex("digest"))
            field_digestUser = cursor.getString(cursor.getColumnIndex("digestUser"))
            field_editingMsg = cursor.getString(cursor.getColumnIndex("editingMsg"))
            field_msgType = cursor.getString(cursor.getColumnIndex("msgType"))
            field_conversationTime = cursor.getLong(cursor.getColumnIndex("conversationTime"))
            field_isSend = cursor.getInt(cursor.getColumnIndex("isSend"))
            field_status = cursor.getInt(cursor.getColumnIndex("status"))
            field_attrflag = cursor.getInt(cursor.getColumnIndex("attrflag"))
            field_atCount = cursor.getInt(cursor.getColumnIndex("atCount"))
            field_unReadMuteCount = cursor.getInt(cursor.getColumnIndex("unReadMuteCount"))
            field_UnReadInvite = cursor.getInt(cursor.getColumnIndex("UnReadInvite"))
            field_unReadCount = cursor.getInt(cursor.getColumnIndex("unReadCount"))


            nickname = if (field_nickname.isEmpty()) "群聊" else field_nickname
            content = (ConversationItemHandler.getConversationContent(MainAdapter.originAdapter, this)
                    ?: (field_content)).toString()
            conversationTime = ConversationItemHandler.getConversationTimeString(MainAdapter.originAdapter, field_conversationTime)

            unReadCount = field_unReadCount
            unReadMuteCount = field_unReadMuteCount
        }
    }

}