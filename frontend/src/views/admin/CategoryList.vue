<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listAdminCategories,
  createCategory,
  updateCategory,
  updateCategoryStatus,
  deleteCategory,
} from '../../api/admin/category'

const categories = ref([])
const loading = ref(true)

const showDialog = ref(false)
const dialogMode = ref('create') // create | edit
const editingId = ref(null)
const parentContext = ref({ id: null, name: '無(頂層分類)' })
const formRef = ref()
const form = reactive({ name: '', sortOrder: 0 })

const rules = {
  name: [{ required: true, message: '請輸入分類名稱', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    categories.value = await listAdminCategories()
  } finally {
    loading.value = false
  }
}

function openCreateRoot() {
  dialogMode.value = 'create'
  editingId.value = null
  parentContext.value = { id: null, name: '無(頂層分類)' }
  form.name = ''
  form.sortOrder = 0
  showDialog.value = true
}

function openCreateChild(parent) {
  dialogMode.value = 'create'
  editingId.value = null
  parentContext.value = { id: parent.id, name: parent.name }
  form.name = ''
  form.sortOrder = 0
  showDialog.value = true
}

function openEdit(category) {
  dialogMode.value = 'edit'
  editingId.value = category.id
  parentContext.value = {
    id: category.parentId,
    name: category.parentId ? findCategoryName(categories.value, category.parentId) : '無(頂層分類)',
  }
  form.name = category.name
  form.sortOrder = category.sortOrder
  showDialog.value = true
}

function findCategoryName(list, id) {
  for (const item of list) {
    if (item.id === id) return item.name
    const found = item.children?.length ? findCategoryName(item.children, id) : null
    if (found) return found
  }
  return ''
}

async function handleSave() {
  await formRef.value.validate()
  const payload = { name: form.name, sortOrder: form.sortOrder, parentId: parentContext.value.id }
  if (dialogMode.value === 'edit') {
    await updateCategory(editingId.value, payload)
  } else {
    await createCategory(payload)
  }
  ElMessage.success('儲存成功')
  showDialog.value = false
  await load()
}

async function handleToggleStatus(category, enabled) {
  try {
    await updateCategoryStatus(category.id, enabled ? 'ACTIVE' : 'DISABLED')
    ElMessage.success('狀態更新成功')
  } finally {
    await load()
  }
}

async function handleDelete(category) {
  try {
    await ElMessageBox.confirm(`確定要刪除「${category.name}」嗎?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteCategory(category.id)
    ElMessage.success('刪除成功')
    await load()
  } catch {
    // 錯誤訊息已由全域攔截器顯示(例如底下還有子分類或商品)
  }
}

onMounted(load)
</script>

<template>
  <div v-loading="loading">
    <div class="header-row">
      <h3>分類管理</h3>
      <el-button type="primary" @click="openCreateRoot">新增頂層分類</el-button>
    </div>

    <el-empty v-if="!loading && categories.length === 0" description="尚無分類" />

    <el-tree
      v-else
      :data="categories"
      :props="{ children: 'children', label: 'name' }"
      node-key="id"
      default-expand-all
      class="category-tree"
    >
      <template #default="{ data }">
        <div class="tree-row">
          <span class="tree-label">
            {{ data.name }}
            <el-tag size="small" :type="data.status === 'ACTIVE' ? 'success' : 'info'">
              {{ data.status === 'ACTIVE' ? '啟用' : '停用' }}
            </el-tag>
          </span>
          <span class="tree-actions">
            <el-switch
              :model-value="data.status === 'ACTIVE'"
              size="small"
              @click.stop
              @change="(v) => handleToggleStatus(data, v)"
            />
            <el-button link size="small" @click.stop="openCreateChild(data)">新增子分類</el-button>
            <el-button link size="small" @click.stop="openEdit(data)">編輯</el-button>
            <el-button link size="small" type="danger" @click.stop="handleDelete(data)">刪除</el-button>
          </span>
        </div>
      </template>
    </el-tree>

    <el-dialog v-model="showDialog" :title="dialogMode === 'edit' ? '編輯分類' : '新增分類'" width="400px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="上層分類">
          <span>{{ parentContext.name }}</span>
        </el-form-item>
        <el-form-item label="名稱" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave">儲存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.category-tree {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 8px;
  padding: 8px;
}

.tree-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding-right: 8px;
}

.tree-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tree-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
