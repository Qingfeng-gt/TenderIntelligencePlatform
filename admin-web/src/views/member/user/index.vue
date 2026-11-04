<template>
  <ContentWrap>
    <!-- 搜索 -->
    <ContentWrap>
      <el-form
        class="-mb-15px"
        :model="queryParams"
        ref="queryFormRef"
        :inline="true"
        label-width="72px"
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="queryParams.username"
            placeholder="请输入账号"
            clearable
            @keyup.enter="handleQuery"
            class="!w-220px"
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input
            v-model="queryParams.nickname"
            placeholder="请输入昵称"
            clearable
            @keyup.enter="handleQuery"
            class="!w-220px"
          />
        </el-form-item>
        <el-form-item label="手机号" prop="mobile">
          <el-input
            v-model="queryParams.mobile"
            placeholder="请输入手机号"
            clearable
            @keyup.enter="handleQuery"
            class="!w-220px"
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select
            v-model="queryParams.status"
            placeholder="请选择状态"
            clearable
            class="!w-180px"
          >
            <el-option
              v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button @click="handleQuery"><Icon icon="ep:search" />搜索</el-button>
          <el-button @click="resetQuery"><Icon icon="ep:refresh" />重置</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <!-- 列表 -->
    <ContentWrap>
      <el-table v-loading="loading" :data="list">
        <el-table-column label="会员编号" align="center" prop="id" width="90" />
        <el-table-column
          label="账号"
          align="center"
          prop="username"
          :show-overflow-tooltip="true"
        />
        <el-table-column
          label="昵称"
          align="center"
          prop="nickname"
          :show-overflow-tooltip="true"
        />
        <el-table-column label="手机号" align="center" prop="mobile" width="130" />
        <el-table-column label="状态" align="center" width="100">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="0"
              :inactive-value="1"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="最后登录 IP" align="center" prop="loginIp" width="150" />
        <el-table-column
          label="最后登录时间"
          align="center"
          prop="loginDate"
          :formatter="dateFormatter"
          width="180"
        />
        <el-table-column
          label="创建时间"
          align="center"
          prop="createTime"
          :formatter="dateFormatter"
          width="180"
        />
        <el-table-column label="操作" align="center" width="120">
          <template #default="scope">
            <el-button type="primary" link @click="handleResetPwd(scope.row)">
              <Icon icon="ep:key" />重置密码
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </ContentWrap>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { CommonStatusEnum } from '@/utils/constants'
import * as MemberUserApi from '@/api/member/user'

defineOptions({ name: 'MemberUser' })

const message = useMessage()

const loading = ref(true)
const total = ref(0)
const list = ref<MemberUserApi.MemberUserVO[]>([])
const queryParams = reactive<MemberUserApi.MemberUserPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  username: undefined,
  nickname: undefined,
  mobile: undefined,
  status: undefined
})
const queryFormRef = ref()

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await MemberUserApi.getMemberUserPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

/** 修改状态 */
const handleStatusChange = async (row: MemberUserApi.MemberUserVO) => {
  try {
    const text = row.status === CommonStatusEnum.ENABLE ? '启用' : '停用'
    await message.confirm(`确认要"${text}""${row.username}"会员吗？`)
    await MemberUserApi.updateMemberUserStatus(row.id, row.status)
    await getList()
  } catch {
    row.status =
      row.status === CommonStatusEnum.ENABLE ? CommonStatusEnum.DISABLE : CommonStatusEnum.ENABLE
  }
}

/** 重置密码 */
const handleResetPwd = async (row: MemberUserApi.MemberUserVO) => {
  try {
    const result = await message.prompt(`请输入"${row.username}"的新密码`, '重置密码')
    const password = result.value
    await MemberUserApi.resetMemberUserPassword(row.id, password)
    message.success('修改成功，新密码是：' + password)
  } catch {}
}

onMounted(() => {
  getList()
})
</script>
