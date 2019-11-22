package com.jimi.ftpapi.model;

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
 * @Description:
 * @Date 2018/12/20 10:34
 * @author HuangJiaLin
 * @version 2.0
 */
public class ToJSData {
    public List files;//选中图片的路径集合
    public String path;//选中视频的路径集合
    public boolean isSpeaker;//是否开启扬声器
    public String userData;//用户自定义信息
    public int status; //播放状态
    public String filePath;//文件路径
}
