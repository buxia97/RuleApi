package com.RuleApi.common;

/**
 * 用户信息脱敏工具类
 * 用于在接口返回时对手机号、邮箱等敏感信息进行脱敏处理
 *
 * 约定（要求 04「接口调出来的时候做隐私加密」）：
 * 1. 任何接口都不下发完整的手机号 / 邮箱，管理后台也一样；
 * 2. 唯一例外是「用户查自己」——前端要靠它判断是否已绑定；
 * 3. 打码后的值一旦被前端回传到写接口，必须用 isMasked() 识别并忽略，
 *    否则会把 138****8000 当成新手机号写进库。
 */
public class MaskUtils {

    private static final String MASK4 = "****";
    private static final String MASK3 = "***";

    /**
     * 手机号脱敏：138****1234
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + MASK4 + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏：a***@example.com
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 1) {
            return "*@" + parts[1];
        }
        return parts[0].charAt(0) + MASK3 + "@" + parts[1];
    }

    /**
     * 手机号或邮箱统一脱敏，自动判别类型。
     *
     * - 邮箱：本地名 ≤2 位原样保留（再截用户自己都认不出），否则取前 2 位 + ***@域名
     * - 手机号：前 3 后 4，中间 ****；带区号（+86xxx）也能正确截取
     *
     * 已脱敏过的值原样返回（幂等，重复调用不会出现 ******）。
     */
    public static String maskContact(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim();
        if (v.isEmpty()) {
            return "";
        }
        if (isMasked(v)) {
            return v;
        }
        int at = v.indexOf('@');
        if (at > 0) {
            String name = v.substring(0, at);
            String domain = v.substring(at);
            if (name.length() <= 2) {
                return name + MASK3 + domain;
            }
            return name.substring(0, 2) + MASK3 + domain;
        }
        if (v.length() > 7) {
            return v.substring(0, 3) + MASK4 + v.substring(v.length() - 4);
        }
        return v;
    }

    /**
     * 判断一个值是不是"已经打过码的"。
     *
     * 用途：后台表单编辑时预填的是打码值，用户没改这个字段直接保存，
     * 就会把 138****8000 当成新手机号写库。写接口入口统一用它拦掉。
     */
    public static boolean isMasked(String raw) {
        return raw != null && raw.indexOf('*') >= 0;
    }
}
