<script setup lang="ts">
import { reactive, ref, onMounted, computed } from "vue";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import { USER_ROLE_LABELS } from "@labreserve/shared";
import type { User, UserCreateRequest, UserUpdateRequest } from "@labreserve/shared";
import * as usersApi from "@/api/users";

const loading = ref(false);
const users = ref<User[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);

const filterRole = ref("");
const filterEnabled = ref<string | undefined>(undefined);

const dialogVisible = ref(false);
const dialogTitle = ref("创建用户");
const isEdit = ref(false);
const editUserId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  username: "",
  realName: "",
  password: "",
  role: "STUDENT" as string,
  email: "",
  phone: "",
  enabled: true,
});

const createRules: FormRules = {
  username: [
    { required: true, message: "请输入用户名", trigger: "blur" },
    { min: 3, max: 50, message: "用户名长度为3-50个字符", trigger: "blur" },
  ],
  realName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  password: [
    { required: true, message: "请输入密码", trigger: "blur" },
    { min: 6, max: 100, message: "密码长度为6-100个字符", trigger: "blur" },
  ],
  email: [{ type: "email", message: "请输入正确的邮箱格式", trigger: "blur" }],
};

const editRules: FormRules = {
  realName: [{ required: true, message: "请输入姓名", trigger: "blur" }],
  email: [{ type: "email", message: "请输入正确的邮箱格式", trigger: "blur" }],
};

const rules = computed(() => (isEdit.value ? editRules : createRules));

async function fetchUsers() {
  loading.value = true;
  try {
    const params: Record<string, unknown> = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    };
    if (filterRole.value) params.role = filterRole.value;
    if (filterEnabled.value !== undefined && filterEnabled.value !== "")
      params.enabled = filterEnabled.value === "true";
    const result = await usersApi.getUsers(params as never);
    users.value = result.records;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

function handlePageChange(p: number) {
  pageNum.value = p;
  fetchUsers();
}

function handleSizeChange(s: number) {
  pageSize.value = s;
  pageNum.value = 1;
  fetchUsers();
}

function openCreate() {
  isEdit.value = false;
  editUserId.value = null;
  dialogTitle.value = "创建用户";
  form.username = "";
  form.realName = "";
  form.password = "";
  form.role = "STUDENT";
  form.email = "";
  form.phone = "";
  form.enabled = true;
  dialogVisible.value = true;
}

function openEdit(user: User) {
  isEdit.value = true;
  editUserId.value = Number(user.id);
  dialogTitle.value = "编辑用户";
  form.username = user.username;
  form.realName = user.realName;
  form.password = "";
  form.role = user.role;
  form.email = user.email ?? "";
  form.phone = user.phone ?? "";
  form.enabled = user.enabled;
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    if (isEdit.value && editUserId.value) {
      const data: UserUpdateRequest = {
        realName: form.realName,
        email: form.email || undefined,
        phone: form.phone || undefined,
        role: form.role as UserUpdateRequest["role"],
        enabled: form.enabled,
      };
      await usersApi.updateUser(editUserId.value, data);
      ElMessage.success("更新成功");
    } else {
      const data: UserCreateRequest = {
        username: form.username,
        password: form.password,
        realName: form.realName,
        role: form.role as UserCreateRequest["role"],
        email: form.email || undefined,
        phone: form.phone || undefined,
      };
      await usersApi.createUser(data);
      ElMessage.success("创建成功");
    }
    dialogVisible.value = false;
    fetchUsers();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(user: User) {
  try {
    await ElMessageBox.confirm(
      `确定要删除用户「${user.realName}」吗？此操作不可恢复。`,
      "删除确认",
      {
        confirmButtonText: "确定删除",
        cancelButtonText: "取消",
        type: "warning",
      },
    );
  } catch {
    return;
  }
  await usersApi.deleteUser(Number(user.id));
  ElMessage.success("删除成功");
  fetchUsers();
}

async function handleToggle(user: User) {
  await usersApi.toggleEnabled(Number(user.id));
  ElMessage.success(user.enabled ? "已禁用" : "已启用");
  fetchUsers();
}

function roleTagType(role: string) {
  return role === "ADMIN" ? "danger" : role === "TEACHER" ? "warning" : "primary";
}

onMounted(() => {
  fetchUsers();
});
</script>

<template>
  <div class="admin-users">
    <h1>用户管理</h1>

    <div class="toolbar">
      <div class="filters">
        <el-select
          v-model="filterRole"
          placeholder="角色筛选"
          clearable
          style="width: 140px"
          @change="fetchUsers"
        >
          <el-option label="全部" value="" />
          <el-option
            v-for="(label, key) in USER_ROLE_LABELS"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
        <el-select
          v-model="filterEnabled"
          placeholder="状态筛选"
          clearable
          style="width: 140px; margin-left: 12px"
          @change="fetchUsers"
        >
          <el-option label="全部" value="" />
          <el-option label="启用" value="true" />
          <el-option label="禁用" value="false" />
        </el-select>
      </div>
      <el-button type="primary" @click="openCreate">创建用户</el-button>
    </div>

    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="100" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="roleTagType(row.role)" size="small">{{
            USER_ROLE_LABELS[row.role as keyof typeof USER_ROLE_LABELS] ?? row.role
          }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="180">
        <template #default="{ row }">{{ row.email || "-" }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="140">
        <template #default="{ row }">{{ row.phone || "-" }}</template>
      </el-table-column>
      <el-table-column prop="enabled" label="启用" width="80" align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.enabled" size="small" @change="handleToggle(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button size="small" text type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="3-50个字符" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="至少6位" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" style="width: 100%">
            <el-option
              v-for="(label, key) in USER_ROLE_LABELS"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="选填" />
        </el-form-item>
        <el-form-item v-if="isEdit" label="启用" prop="enabled">
          <el-switch v-model="form.enabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">{{
          submitting ? "保存中..." : "保存"
        }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-users {
  padding: 24px;
}

h1 {
  margin: 0 0 20px;
  font-size: 24px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  align-items: center;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
