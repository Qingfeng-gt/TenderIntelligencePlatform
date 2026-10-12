/**
 * notice API 包，定义并实现提供给其它模块的 API
 *
 * <p>这是毕设插槽 7-②（doc/毕设 §7）。<b>第 1 周末冻结签名，冻结后只加不改。</b>
 *
 * <p>消费方：
 * <ul>
 *   <li><b>乙（M2 crawler）</b> —— 写入侧 {@code upsert(NoticeDTO)}。他的
 *       {@code NoticeUpsertService} 目前直接 import 了本模块的 DO 与 Mapper，
 *       要收敛到本接口（doc/毕设 §6 第 6 项）</li>
 *   <li><b>丁（M4 ai）</b>、<b>戊（M5 tag）</b> —— 读取侧。直接连表的话，
 *       本模块改字段就炸两家</li>
 * </ul>
 *
 * <p>做法：插槽阶段<b>只定签名不实现</b>。乙/丁/戊 先写 mock 实现（放在自己的模块里），
 * 第 4 周换成真实现。这件事本身就是丙论文的创新点 1（统一字段映射层）。
 */
package com.tenderintelligence.module.notice.api;
