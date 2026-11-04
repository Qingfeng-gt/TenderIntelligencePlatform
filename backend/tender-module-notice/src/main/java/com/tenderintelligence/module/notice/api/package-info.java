/**
 * notice API 包，定义并实现提供给其它模块的 API
 *
 * <p>这是毕设插槽 7-②（doc/03-系统架构.md）。<b>第 1 周末冻结签名，冻结后只加不改。</b>
 *
 * <p>消费方：
 * <ul>
 *   <li><b>乙（M2 crawler）</b> —— 写入侧 {@code upsert(NoticeDTO)}。已改为经
 *       {@code NoticeApi} 写入，由本模块 {@code NoticeIngestionService} 落
 *       notice / project / tender_project / tender_lot / enterprise 与采集治理表</li>
 *   <li><b>丁（M4 ai）</b>、<b>戊（M5 tag）</b> —— 读取侧。直接连表的话，
 *       本模块改字段就炸两家</li>
 * </ul>
 *
 * <p>实现：{@code NoticeApiImpl}（读取走四层模型组装，写入走 {@code NoticeIngestionService}）。
 */
package com.tenderintelligence.module.notice.api;
