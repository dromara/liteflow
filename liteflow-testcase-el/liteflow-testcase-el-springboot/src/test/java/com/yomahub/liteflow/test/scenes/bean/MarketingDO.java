package com.yomahub.liteflow.test.scenes.bean;

import java.time.LocalDateTime;

public class MarketingDO {

    /** id */
    private String id;

    /** 名称 */
    private String name;

    /** 类型(枚举)
     * @see com.yomahub.liteflow.test.scenes.enums.MarketingTypeEnum
     * */
    private String type;

    /** 模式(枚举)
     * @see com.yomahub.liteflow.test.scenes.enums.MarketingModeEnum
     * */
    private String mode;

    /** 开始时间 */
    private LocalDateTime startDatetime;

    /** 结束时间 */
    private LocalDateTime endDatetime;

    /** 限制总次数 */
    private Integer limitTotalNum;

    /** 限制每天次数 */
    private Integer limitDayNum;

    /** 参与区域(运营商)
     * */
    private String joinInst;

    /** 用户模型json
     * */
    private String userModel;

    /** 消息模版json
     */
    private String msgTemplate;

    /** 奖励规则json
     * */
    private String rewardRule;

    /** 描述 */
    private String remark;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public LocalDateTime getStartDatetime() {
        return startDatetime;
    }

    public void setStartDatetime(LocalDateTime startDatetime) {
        this.startDatetime = startDatetime;
    }

    public LocalDateTime getEndDatetime() {
        return endDatetime;
    }

    public void setEndDatetime(LocalDateTime endDatetime) {
        this.endDatetime = endDatetime;
    }

    public Integer getLimitTotalNum() {
        return limitTotalNum;
    }

    public void setLimitTotalNum(Integer limitTotalNum) {
        this.limitTotalNum = limitTotalNum;
    }

    public Integer getLimitDayNum() {
        return limitDayNum;
    }

    public void setLimitDayNum(Integer limitDayNum) {
        this.limitDayNum = limitDayNum;
    }

    public String getJoinInst() {
        return joinInst;
    }

    public void setJoinInst(String joinInst) {
        this.joinInst = joinInst;
    }

    public String getUserModel() {
        return userModel;
    }

    public void setUserModel(String userModel) {
        this.userModel = userModel;
    }

    public String getMsgTemplate() {
        return msgTemplate;
    }

    public void setMsgTemplate(String msgTemplate) {
        this.msgTemplate = msgTemplate;
    }

    public String getRewardRule() {
        return rewardRule;
    }

    public void setRewardRule(String rewardRule) {
        this.rewardRule = rewardRule;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
