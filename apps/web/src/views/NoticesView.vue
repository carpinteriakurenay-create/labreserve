<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from "element-plus";
import {
  NOTICE_TYPE_LABELS,
  NOTICE_PRIORITY_LABELS,
  type Notice,
  type NoticeCreateRequest,
  type NoticeUpdateRequest,
} from "@labreserve/shared";
import { useAuthStore } from "@/stores/auth";
import * as noticesApi from "@/api/notices";
import * as labsApi from "@/api/labs";
import type { Lab } from "@labreserve/shared";

const router = useRouter();
const authStore = useAuthStore();

const canManage = () => authStore.userRole === "TEACHER" || authStore.userRole === "ADMIN";

// ── list state ──

const loading = ref(false);
const notices = ref<Notice[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(20);

const filterType = ref("");
const filterPriority = ref("");

// ── detail dialog ──

const detailVisible = ref(false);
const currentNotice = ref<Notice | null>(null);

// ── create/edit dialog ──

const dialogVisible = ref(false);
const dialogTitle = ref("发布通知");
const isEdit = ref(false);
const editId = ref<number | null>(null);
const formRef = ref<FormInstance>();
const submitting = ref(false);

const form = reactive({
  title: "",
  content: "",
  type: "GENERAL" as string,
  priority: "NORMAL" as string,
  labId: undefined as number | undefined,
});

const rules: FormRules = {
  title: [{ required: true, message: "请输入标题", trigger: "blur" }],
  content: [{ required: true, message: "请输入内容", trigger: "blur" }],
};

const labs = ref<Lab[]>([]);

const showLabSelect = () => form.type === "LAB" || form.type === "EQUIPMENT";

// ── fetch ──

async function fetchNotices() {
  loading.value = true;
  try {
    const params: noticesApi.NoticeQueryParams = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
    };
    if (filterType.value) params.type = filterType.value;
    if (filterPriority.value) params.priority = filterPriority.value;
    const result = await noticesApi.getNotices(params);
    notices.value = result.records;
    total.value = result.total;
  } finally {
    loading.value = false;
  }
}

async function fetchLabs() {
  try {
    const result = await labsApi.getLabs({ pageSize: 1000 });
    labs.value = result.records;
  } catch {
    // ignore
  }
}

function handleSearch() {
  pageNum.value = 1;
  fetchNotices();
}

function handleReset() {
  filterType.value = "";
  filterPriority.value = "";
  pageNum.value = 1;
  fetchNotices();
}

function handlePageChange(p: number) {
  pageNum.value = p;
  fetchNotices();
}

function handleSizeChange(s: number) {
  pageSize.value = s;
  pageNum.value = 1;
  fetchNotices();
}

// ── detail ──

function openDetail(notice: Notice) {
  currentNotice.value = notice;
  detailVisible.value = true;
}

function navigateToLab(labId: string | null) {
  if (!labId) return;
  detailVisible.value = false;
  router.push(`/labs/${labId}`);
}

// ── create / edit ──

function openCreate() {
  isEdit.value = false;
  editId.value = null;
  dialogTitle.value = "发布通知";
  form.title = "";
  form.content = "";
  form.type = "GENERAL";
  form.priority = "NORMAL";
  form.labId = undefined;
  dialogVisible.value = true;
}

function openEdit(notice: Notice) {
  isEdit.value = true;
  editId.value = Number(notice.id);
  dialogTitle.value = "编辑通知";
  form.title = notice.title;
  form.content = notice.content;
  form.type = notice.type;
  form.priority = notice.priority;
  form.labId = notice.labId ? Number(notice.labId) : undefined;
  dialogVisible.value = true;
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false);
  if (!valid) return;

  submitting.value = true;
  try {
    if (isEdit.value && editId.value) {
      const data: NoticeUpdateRequest = {
        title: form.title,
        content: form.content,
        type: form.type as Notice["type"],
        priority: form.priority as Notice["priority"],
        labId: form.labId ? String(form.labId) : undefined,
      };
      await noticesApi.updateNotice(editId.value, data);
      ElMessage.success("更新成功");
    } else {
      const data: NoticeCreateRequest = {
        title: form.title,
        content: form.content,
        type: form.type as Notice["type"],
        priority: form.priority as Notice["priority"],
        labId: form.labId ? String(form.labId) : undefined,
      };
      await noticesApi.createNotice(data);
      ElMessage.success("发布成功");
    }
    dialogVisible.value = false;
    fetchNotices();
  } finally {
    submitting.value = false;
  }
}

async function handleDelete(notice: Notice) {
  try {
    await ElMessageBox.confirm(`确定要删除通知「${notice.title}」吗？`, "删除确认", {
      confirmButtonText: "确定删除",
      cancelButtonText: "取消",
      type: "warning",
    });
  } catch {
    return;
  }
  await noticesApi.deleteNotice(Number(notice.id));
  ElMessage.success("删除成功");
  fetchNotices();
}

// ── helpers ──

function priorityBarClass(priority: string) {
  return `bar-${priority.toLowerCase()}`;
}

function priorityTagType(priority: string) {
  const map: Record<string, string> = {
    URGENT: "danger",
    HIGH: "warning",
    NORMAL: "",
    LOW: "info",
  };
  return map[priority] ?? "info";
}

function typeTagType(type: string) {
  return type === "LAB" ? "success" : type === "EQUIPMENT" ? "warning" : "";
}

function truncateContent(text: string, max: number) {
  if (text.length <= max) return text;
  return text.slice(0, max) + "...";
}

// ── init ──

onMounted(() => {
  fetchNotices();
  fetchLabs();
});
</script>

<template>
  <div class="notices">
    <h1>通知公告</h1>

    <div class="toolbar">
      <div class="filters">
        <el-select
          v-model="filterType"
          placeholder="通知类型"
          clearable
          style="width: 140px"
          @change="handleSearch"
        >
          <el-option
            v-for="(label, key) in NOTICE_TYPE_LABELS"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
        <el-select
          v-model="filterPriority"
          placeholder="优先级"
          clearable
          style="width: 140px; margin-left: 12px"
          @change="handleSearch"
        >
          <el-option
            v-for="(label, key) in NOTICE_PRIORITY_LABELS"
            :key="key"
            :label="label"
            :value="key"
          />
        </el-select>
        <el-button style="margin-left: 12px" type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <el-button v-if="canManage()" type="primary" @click="openCreate">发布通知</el-button>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && notices.length === 0" description="暂无通知" />

      <div v-else class="notice-list">
        <el-card
          v-for="notice in notices"
          :key="notice.id"
          class="notice-card"
          shadow="hover"
          @click="openDetail(notice)"
        >
          <div :class="['priority-bar', priorityBarClass(notice.priority)]"></div>
          <div class="card-content">
            <div class="card-header">
              <span class="title">{{ notice.title }}</span>
              <div class="card-tags">
                <el-tag :type="typeTagType(notice.type)" size="small" effect="plain">
                  {{ NOTICE_TYPE_LABELS[notice.type] ?? notice.type }}
                </el-tag>
                <el-tag :type="priorityTagType(notice.priority)" size="small" effect="dark">
                  {{ NOTICE_PRIORITY_LABELS[notice.priority] ?? notice.priority }}
                </el-tag>
              </div>
            </div>
            <p class="summary">{{ truncateContent(notice.content, 150) }}</p>
            <div class="card-footer">
              <span class="meta">
                {{ notice.publisherName ?? "未知" }} · {{ notice.createdAt?.slice(0, 10) }}
              </span>
              <span v-if="canManage()" class="card-actions" @click.stop>
                <el-button size="small" text type="primary" @click="openEdit(notice)"
                  >编辑</el-button
                >
                <el-button size="small" text type="danger" @click="handleDelete(notice)"
                  >删除</el-button
                >
              </span>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <div v-if="total > pageSize" class="pagination">
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

    <!-- Detail dialog -->
    <el-dialog v-model="detailVisible" title="通知详情" width="600px">
      <template v-if="currentNotice">
        <div class="detail-header">
          <h2>{{ currentNotice.title }}</h2>
          <div class="detail-tags">
            <el-tag :type="typeTagType(currentNotice.type)" size="small">
              {{ NOTICE_TYPE_LABELS[currentNotice.type] ?? currentNotice.type }}
            </el-tag>
            <el-tag :type="priorityTagType(currentNotice.priority)" size="small" effect="dark">
              {{ NOTICE_PRIORITY_LABELS[currentNotice.priority] ?? currentNotice.priority }}
            </el-tag>
          </div>
        </div>
        <div class="detail-meta">
          <span>发布者：{{ currentNotice.publisherName ?? "未知" }}</span>
          <span>发布时间：{{ currentNotice.createdAt?.slice(0, 10) }}</span>
          <span v-if="currentNotice.labName">
            关联实验室：
            <a class="lab-link" @click="navigateToLab(currentNotice.labId)">{{
              currentNotice.labName
            }}</a>
          </span>
        </div>
        <div class="detail-content">{{ currentNotice.content }}</div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- Create / Edit dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      @closed="formRef?.resetFields()"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="通知标题" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option
              v-for="(label, key) in NOTICE_TYPE_LABELS"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-select v-model="form.priority" style="width: 100%">
            <el-option
              v-for="(label, key) in NOTICE_PRIORITY_LABELS"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="showLabSelect()" label="关联实验室" prop="labId">
          <el-select
            v-model="form.labId"
            clearable
            style="width: 100%"
            placeholder="选择关联实验室（可选）"
          >
            <el-option
              v-for="lab in labs"
              :key="lab.id"
              :label="lab.name"
              :value="Number(lab.id)"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="通知正文内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ submitting ? "保存中..." : "保存" }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.notices {
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
  margin-bottom: 20px;
}

.filters {
  display: flex;
  align-items: center;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notice-card {
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.notice-card:hover {
  border-color: #409eff;
}

.priority-bar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}

.bar-urgent {
  background: #f56c6c;
}
.bar-high {
  background: #e6a23c;
}
.bar-normal {
  background: #409eff;
}
.bar-low {
  background: #909399;
}

.card-content {
  padding-left: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.card-header .title {
  font-size: 16px;
  font-weight: 600;
}

.card-tags {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
  margin-left: 12px;
}

.summary {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 10px;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-footer .meta {
  color: #909399;
  font-size: 13px;
}

.card-actions {
  display: flex;
  gap: 4px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* detail dialog */
.detail-header {
  margin-bottom: 12px;
}

.detail-header h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.detail-tags {
  display: flex;
  gap: 8px;
}

.detail-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #909399;
  font-size: 13px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.lab-link {
  color: #409eff;
  cursor: pointer;
}

.lab-link:hover {
  text-decoration: underline;
}

.detail-content {
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
