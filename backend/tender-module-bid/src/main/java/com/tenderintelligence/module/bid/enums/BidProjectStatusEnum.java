package com.tenderintelligence.module.bid.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;
import java.util.Set;

/**
 * 投标项目状态枚举 + 状态机允许迁移表
 *
 * 流程: SUBMITTED已递交 → OTB待开标 → WON中标 / LOST未中标
 *       任意非终态 → ABANDONED放弃
 */
@Getter
@AllArgsConstructor
public enum BidProjectStatusEnum {

    /** 已递交投标文件 */
    SUBMITTED("SUBMITTED", "已递交"),
    /** 待开标 */
    OTB("OTB", "待开标"),
    /** 中标 */
    WON("WON", "中标"),
    /** 未中标 */
    LOST("LOST", "未中标"),
    /** 放弃 */
    ABANDONED("ABANDONED", "放弃");

    private final String status;

    /** 状态展示名称 */
    private final String name;

    /** 允许迁移表(注意: WON/LOST 为终态) */
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            SUBMITTED.status, Set.of(OTB.status, ABANDONED.status),
            OTB.status, Set.of(WON.status, LOST.status, ABANDONED.status),
            WON.status, Set.of(),
            LOST.status, Set.of(),
            ABANDONED.status, Set.of()
    );

    /**
     * 校验状态迁移是否合法
     */
    public static boolean allowed(String from, String to) {
        Set<String> next = TRANSITIONS.getOrDefault(from, Set.of());
        return next.contains(to);
    }

    /**
     * 状态码 → 枚举(非法返回 null)
     */
    public static BidProjectStatusEnum resolve(String status) {
        for (BidProjectStatusEnum value : values()) {
            if (value.status.equals(status)) {
                return value;
            }
        }
        return null;
    }
}
