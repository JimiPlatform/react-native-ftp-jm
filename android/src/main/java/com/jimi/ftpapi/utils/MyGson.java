package com.jimi.ftpapi.utils;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jimi.ftpapi.model.ToJSBean;

import java.util.ArrayList;
import java.util.List;

/*
 * COPYRIGHT. ShenZhen JiMi Technology Co., Ltd. 2018.
 * ALL RIGHTS RESERVED.
 *
 * No part of this publication may be reproduced, stored in a retrieval system, or transmitted,
 * on any form or by any means, electronic, mechanical, photocopying, recording,
 * or otherwise, without the prior written permission of ShenZhen JiMi Network Technology Co., Ltd.
 *
 * @ProjectName newsmarthome2.0
 * @Description: 封装gson，实现字段过滤等功能
 * @Date 2018/12/20 11:19
 * @author HuangJiaLin
 * @version 2.0
 */public class MyGson {

    /**
     * 过滤掉field外的字段
     * @param fieldList 要保留的字段
     * @return gson
     */
    public static Gson reservedField(List<String> fieldList){
         Gson gson = new GsonBuilder().addSerializationExclusionStrategy(new ExclusionStrategy() {
             @Override
             public boolean shouldSkipField(FieldAttributes f) {
                 if(fieldList.contains(f.getName())){
                     return false;
                 }
                 return true;
             }

             @Override
             public boolean shouldSkipClass(Class<?> clazz) {
                 return false;
             }
         }).create();
         return gson;
     }

    /**
     * 根据方法名保留所需字段
     * @param methodName 方法名
     * @param bean 目标对象
     * @return 返回对象的序列化字符串
     */
    public static String getJsonByMethodName(String methodName , ToJSBean<T> bean){
        List<String> fieldList = new ArrayList<>();
        fieldList.add("callback");
        fieldList.add("method");
        fieldList.add("data");
        if(methodName.equals("jm_media.openVoiceModeChange")
                || methodName.equals("jm_media.changeVoiceMode")
                || methodName.equals("jm_media.closeVoiceModeChange")){
            fieldList.add("isSpeaker");
        } else if(methodName.equals("jm_image.choose")){
            fieldList.add("files");
        } else if(methodName.equals("jm_media.chooseVideo")){
            fieldList.add("path");
        } else if(methodName.equals("jm_media.playAudio")){
            fieldList.add("userData");
            fieldList.add("status");
        } else if(methodName.equals("jm_file.getFileList")){
            fieldList.add("files");
        } else if(methodName.equals("jm_file.getSmallAppPath")){
            fieldList.add("filePath");
        }
        //Type jsonType = new TypeToken<ToJSBean<T>>() {}.getType();
        Gson gson = reservedField(fieldList);
        return gson.toJson(bean);
    }

}
