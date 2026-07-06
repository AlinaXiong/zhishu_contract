package com.hero.middleware.client.zhishu.response;

import lombok.Data;

import java.util.List;

@Data
public class QueryAllVendorResponse {
    /**
     * 是否还有更多项
     */
    private boolean hasMore;
    /**
     * 交易方，具体内容参考 创建交易方
     */
    private List<Item> items;
    /**
     * 分页标记，当 has_more 为 true 时，会同时返回新的 page_token，否则不返回 page_token
     */
    private String pageToken;

    @Data
    public static class Item {
        private String adCity;
        private String adCountry;
        private String address;
        private String adPostcode;
        private String adProvince;
        private List<ItemAppendix> appendix;
        private Boolean associatedWithLegalEntity;
        private String certificationId;
        private String certificationType;
        private String contactMobilePhone;
        private String contactPerson;
        private String contactTelephone;
        private String downPaymentTerm;
        private String eMail;
        private List<ItemExtendInfo> extendInfo;
        private String fax;
        private String glAccount;
        private String id;
        private String legalPerson;
        private String linkedCustomer;
        private String linkedEmployee;
        private String paymentTerm;
        private String shortText;
        private Long status;
        private String vendor;
        private List<VendorAccount> vendorAccounts;
        private List<VendorAddress> vendorAddresses;
        private String vendorCategory;
        private List<VendorCompanyView> vendorCompanyViews;
        private List<VendorContact> vendorContacts;
        private String vendorNature;
        private String vendorSiteCode;
        private String vendorText;
        private String vendorType;
    }
    @Data
    public static class ItemAppendix {
        private String downloadUrl;
        private String fileId;
        private String fileName;
        private Long fileSize;
        private String fileType;
    }

    @Data
    public static class ItemExtendInfo {
        private List<PurpleAppendix> appendix;
        private String date;
        private String fieldCode;
        private Long fieldType;
        private String fieldValue;
        private Double num;
        private List<String> options;
        private List<String> rangeDate;
    }

    @Data
    public static class PurpleAppendix {
        private String downloadUrl;
        private String fileId;
        private String fileName;
        private Long fileSize;
        private String fileType;
    }
    @Data
    public static class VendorAccount {
        private String account;
        private String accountName;
        private String bankAcronym;
        private String bankCode;
        private String bankControlCode;
        private String bankId;
        private String bankName;
        private String country;
        private List<VendorAccountExtendInfo> extendInfo;
        private String iban;
        private String id;
        private String swiftCode;
        private String vendorSiteCode;
    }

    @Data
    public static class VendorAccountExtendInfo {
        private List<FluffyAppendix> appendix;
        private String date;
        private String fieldCode;
        private Long fieldType;
        private String fieldValue;
        private Double num;
        private List<String> options;
        private List<String> rangeDate;
    }

    @Data
    public static class FluffyAppendix {
        private String downloadUrl;
        private String fileId;
        private String fileName;
        private Long fileSize;
        private String fileType;
    }

    @Data
    public static class VendorAddress {
        private String address;
        private String city;
        private String country;
        private String county;
        private List<VendorAddressExtendInfo> extendInfo;
        private String id;
        private String province;
    }

    @Data
    public static class VendorAddressExtendInfo {
        private List<TentacledAppendix> appendix;
        private String date;
        private String fieldCode;
        private Long fieldType;
        private String fieldValue;
        private Double num;
        private List<String> options;
        private List<String> rangeDate;
    }

    @Data
    public static class TentacledAppendix {
        private String downloadUrl;
        private String fileId;
        private String fileName;
        private Long fileSize;
        private String fileType;
    }

    @Data
    public static class VendorCompanyView {
        private String companyCode;
        private String downPaymentTerm;
        private List<VendorCompanyViewExtendInfo> extendInfo;
        private String glAccount;
        private String id;
        private String paymentTerm;
        private String vendorSiteCode;
    }

    @Data
    public static class VendorCompanyViewExtendInfo {
        private List<StickyAppendix> appendix;
        private String date;
        private String fieldCode;
        private Long fieldType;
        private String fieldValue;
        private Double num;
        private List<String> options;
        private List<String> rangeDate;
    }

    @Data
    public static class StickyAppendix {
        private String downloadUrl;
        private String fileId;
        private String fileName;
        private Long fileSize;
        private String fileType;
    }


    @Data
    public static class VendorContact {
        private String email;
        private List<VendorContactExtendInfo> extendInfo;
        private String id;
        private String name;
        private String phone;
        private String position;
        private String remark;
    }


    @Data
    public static class VendorContactExtendInfo {
        private List<IndigoAppendix> appendix;
        private String date;
        private String fieldCode;
        private Long fieldType;
        private String fieldValue;
        private Double num;
        private List<String> options;
        private List<String> rangeDate;
    }

    @Data
    public static class IndigoAppendix {
        private String downloadUrl;
        private String fileId;
        private String fileName;
        private Long fileSize;
        private String fileType;
    }

}
