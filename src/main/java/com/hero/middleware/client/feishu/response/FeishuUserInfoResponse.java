package com.hero.middleware.client.feishu.response;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class FeishuUserInfoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private User user;

    @Data
    public static class User implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "union_id")
        private String unionId;

        @JSONField(name = "user_id")
        private String userId;

        @JSONField(name = "open_id")
        private String openId;

        private String name;

        @JSONField(name = "en_name")
        private String enName;

        private String nickname;

        private String email;

        @JSONField(name = "enterprise_email")
        private String enterpriseEmail;

        private String mobile;

        @JSONField(name = "mobile_visible")
        private Boolean mobileVisible;

        private Integer gender;

        private Avatar avatar;

        private Status status;

        @JSONField(name = "department_ids")
        private List<String> departmentIds;

        @JSONField(name = "leader_user_id")
        private String leaderUserId;

        private String city;

        private String country;

        @JSONField(name = "work_station")
        private String workStation;

        @JSONField(name = "join_time")
        private Long joinTime;

        @JSONField(name = "employee_no")
        private String employeeNo;

        @JSONField(name = "employee_type")
        private Integer employeeType;

        @JSONField(name = "job_title")
        private String jobTitle;

        @JSONField(name = "is_tenant_manager")
        private Boolean tenantManager;

        private List<Order> orders;

        @JSONField(name = "custom_attrs")
        private List<CustomAttr> customAttrs;
    }

    @Data
    public static class Avatar implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "avatar_72")
        private String avatar72;

        @JSONField(name = "avatar_240")
        private String avatar240;

        @JSONField(name = "avatar_640")
        private String avatar640;

        @JSONField(name = "avatar_origin")
        private String avatarOrigin;
    }

    @Data
    public static class Status implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "is_frozen")
        private Boolean frozen;

        @JSONField(name = "is_resigned")
        private Boolean resigned;

        @JSONField(name = "is_activated")
        private Boolean activated;

        @JSONField(name = "is_exited")
        private Boolean exited;

        @JSONField(name = "is_unjoin")
        private Boolean unjoin;
    }

    @Data
    public static class Order implements Serializable {

        private static final long serialVersionUID = 1L;

        @JSONField(name = "department_id")
        private String departmentId;

        @JSONField(name = "user_order")
        private Long userOrder;

        @JSONField(name = "department_order")
        private Long departmentOrder;

        @JSONField(name = "is_primary_dept")
        private Boolean primaryDept;
    }

    @Data
    public static class CustomAttr implements Serializable {

        private static final long serialVersionUID = 1L;

        private String type;

        private String id;

        private Object value;
    }
}
