package com.tenderintelligence.module.infra.service.codegen.inner;

import com.tenderintelligence.framework.common.exception.ServiceException;
import com.tenderintelligence.module.infra.dal.dataobject.codegen.CodegenColumnDO;
import com.tenderintelligence.module.infra.dal.dataobject.codegen.CodegenTableDO;
import com.tenderintelligence.module.infra.enums.codegen.CodegenFrontTypeEnum;
import com.tenderintelligence.module.infra.enums.codegen.CodegenTemplateTypeEnum;
import com.baomidou.mybatisplus.annotation.DbType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CodegenEngine} 的 Vue3 + Element Plus 单元测试
 *
 * @author Tender Intelligence
 */
public class CodegenEngineElementPlusTest extends CodegenEngineAbstractTest {

    @Test
    public void testExecute_duplicateMasterSymbol() {
        // 准备主表
        CodegenTableDO table = getTable("student")
                .setFrontType(CodegenFrontTypeEnum.VUE3_ELEMENT_PLUS.getType())
                .setTemplateType(CodegenTemplateTypeEnum.MASTER_NORMAL.getType());
        List<CodegenColumnDO> columns = getColumnList("student");
        // 准备两个规范化名称相同的子表
        CodegenTableDO crmItemTable = getTable("contact").setModuleName("crm").setClassName("CrmOrderItem")
                .setTemplateType(CodegenTemplateTypeEnum.SUB.getType()).setSubJoinColumnId(100L).setSubJoinMany(true);
        CodegenTableDO erpItemTable = getTable("contact").setModuleName("erp").setClassName("ErpOrderItem")
                .setTemplateType(CodegenTemplateTypeEnum.SUB.getType()).setSubJoinColumnId(100L).setSubJoinMany(true);
        List<CodegenColumnDO> crmItemColumns = getColumnList("contact");
        List<CodegenColumnDO> erpItemColumns = getColumnList("contact");

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> codegenEngine.execute(DbType.MYSQL, table, columns,
                        Arrays.asList(crmItemTable, erpItemTable), Arrays.asList(crmItemColumns, erpItemColumns)));
        assertTrue(exception.getMessage().contains("主子表规范化类名(OrderItem)重复"));
    }

    @Test
    public void testExecute_duplicateMasterFieldNormal() {
        testExecute_duplicateMasterField(CodegenTemplateTypeEnum.MASTER_NORMAL);
    }

    @Test
    public void testExecute_duplicateMasterFieldInner() {
        testExecute_duplicateMasterField(CodegenTemplateTypeEnum.MASTER_INNER);
    }

    @Test
    public void testExecute_duplicateMasterFieldWithMainNormal() {
        testExecute_duplicateMasterFieldWithMain(CodegenTemplateTypeEnum.MASTER_NORMAL);
    }

    @Test
    public void testExecute_duplicateMasterFieldWithMainInner() {
        testExecute_duplicateMasterFieldWithMain(CodegenTemplateTypeEnum.MASTER_INNER);
    }

    @Test
    public void testRegisterGeneratedSource_duplicatePath() {
        // 准备参数
        Map<String, String> generatedSources = new HashMap<>();
        Map<String, Object> bindingMap = new HashMap<>();
        bindingMap.put("subIndex", 0);
        CodegenEngine.registerGeneratedSource(generatedSources, "same.vue", "form_sub.vm", bindingMap);
        bindingMap.put("subIndex", 1);

        // 调用
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> CodegenEngine.registerGeneratedSource(generatedSources, "same.vue", "form_sub.vm", bindingMap));
        // 断言
        assertTrue(exception.getMessage().contains("生成文件路径重复：same.vue"));
        assertTrue(exception.getMessage().contains("form_sub.vm[subIndex=0]"));
        assertTrue(exception.getMessage().contains("form_sub.vm[subIndex=1]"));
    }

    private void testExecute_duplicateMasterField(CodegenTemplateTypeEnum templateType) {
        // 准备主表
        CodegenTableDO table = getTable("student")
                .setFrontType(CodegenFrontTypeEnum.VUE3_ELEMENT_PLUS.getType())
                .setTemplateType(templateType.getType());
        List<CodegenColumnDO> columns = getColumnList("student");
        // 准备属性名同为 items 的子表：Item 一对多、Items 一对一
        CodegenTableDO crmItemTable = getTable("contact").setModuleName("crm").setClassName("CrmItem")
                .setTemplateType(CodegenTemplateTypeEnum.SUB.getType()).setSubJoinColumnId(100L).setSubJoinMany(true);
        CodegenTableDO erpItemsTable = getTable("contact").setModuleName("erp").setClassName("ErpItems")
                .setTemplateType(CodegenTemplateTypeEnum.SUB.getType()).setSubJoinColumnId(100L).setSubJoinMany(false);
        List<CodegenColumnDO> crmItemColumns = getColumnList("contact");
        List<CodegenColumnDO> erpItemsColumns = getColumnList("contact");

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> codegenEngine.execute(DbType.MYSQL, table, columns,
                        Arrays.asList(crmItemTable, erpItemsTable), Arrays.asList(crmItemColumns, erpItemsColumns)));
        assertTrue(exception.getMessage().contains("主子表属性名(items)重复"));
    }

    private void testExecute_duplicateMasterFieldWithMain(CodegenTemplateTypeEnum templateType) {
        // 准备主表
        CodegenTableDO table = getTable("student")
                .setFrontType(CodegenFrontTypeEnum.VUE3_ELEMENT_PLUS.getType())
                .setTemplateType(templateType.getType());
        List<CodegenColumnDO> columns = getColumnList("student");
        columns.stream().filter(column -> "memo".equals(column.getJavaField())).findFirst().orElseThrow()
                .setJavaField("items");
        // 准备一对多子表 Item，生成属性名 items
        CodegenTableDO itemTable = getTable("contact").setModuleName("crm").setClassName("CrmItem")
                .setTemplateType(CodegenTemplateTypeEnum.SUB.getType()).setSubJoinColumnId(100L).setSubJoinMany(true);

        // 调用并断言
        ServiceException exception = assertThrows(ServiceException.class,
                () -> codegenEngine.execute(DbType.MYSQL, table, columns,
                        List.of(itemTable), List.of(getColumnList("contact"))));
        assertTrue(exception.getMessage().contains("主子表属性名(items)重复"));
    }

}
