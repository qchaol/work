package jlinksz.com.repository;

import android.content.Context;
import android.util.Base64;

import com.iflytek.aiui.AIUIConstant;
import com.iflytek.aiui.AIUIMessage;

import org.json.JSONObject;

import jlinksz.com.EntityData.DynamicEntityData;

/**
 * Created by allen on 18年5月21日.
 */
public class MessagesCenterRepository {

    private String mAppid;

    public MessagesCenterRepository(Context context, JSONObject config) {
        mAppid = config.optJSONObject("login").optString("appid");

    }

    public void syncDynamicEntity(DynamicEntityData data){
        try {
            // schema数据同步
            JSONObject syncSchemaJson = new JSONObject();
            JSONObject paramJson = new JSONObject();

            paramJson.put("appid", mAppid);
            paramJson.put("id_name", data.idName);
            paramJson.put("id_value", data.idValue);
            paramJson.put("res_name", data.resName);

            syncSchemaJson.put("param", paramJson);
            syncSchemaJson.put("data", Base64.encodeToString(
                    data.syncData.getBytes(), Base64.DEFAULT | Base64.NO_WRAP));

            // 传入的数据一定要为utf-8编码
            byte[] syncData = syncSchemaJson.toString().getBytes("utf-8");

            AIUIMessage syncAthenaMessage = new AIUIMessage(AIUIConstant.CMD_SYNC,
                    AIUIConstant.SYNC_DATA_SCHEMA, 0, "", syncData);
            sendMessage(syncAthenaMessage);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    private void sendMessage(AIUIMessage message){
//        if(mCurrentState != AIUIConstant.STATE_WORKING){
//            mAgent.sendMessage(new AIUIMessage(AIUIConstant.CMD_WAKEUP, 0, 0, "", null));
//        }
//
//        mAgent.sendMessage(message);
    }
}
