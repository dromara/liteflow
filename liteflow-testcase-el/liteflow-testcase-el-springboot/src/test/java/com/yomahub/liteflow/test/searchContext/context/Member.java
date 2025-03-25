package com.yomahub.liteflow.test.searchContext.context;

public class Member {
    private String memberCode;

    private String memberName;

    public Member(String memberCode, String memberName) {
        this.memberCode = memberCode;
        this.memberName = memberName;
    }

    public String getMemberCode() {
        return memberCode;
    }

    public void setMemberCode(String memberCode) {
        this.memberCode = memberCode;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }
}
