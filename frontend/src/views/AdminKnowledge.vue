<template>
  <div class="knowledge-container">
    <div class="header">
      <h2>📚 垃圾分类知识库管理</h2>
      <el-button type="primary" @click="openDialog('add')">新增条目</el-button>
    </div>

    <!-- 搜索 -->
    <el-input v-model="searchKey" placeholder="搜索分类或物品名称..." style="width: 300px; margin: 20px 0;" clearable @clear="fetchList" @input="fetchList" />

    <el-table :data="tableData" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="category" label="分类" width="150">
        <template #default="scope">
          <el-tag :type="getCategoryTag(scope.row.category)">{{ scope.row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="name" label="物品名称" width="200" />
      <el-table-column prop="description" label="分类说明" show-overflow-tooltip />
      <el-table-column prop="suggestion" label="投放建议" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" type="primary" @click="openDialog('edit', scope.row)">编辑</el-button>
          <el-popconfirm title="确定删除这条知识吗？" @confirm="handleDelete(scope.row.id)">
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 弹窗：新增/编辑 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="可回收物" value="可回收物" />
            <el-option label="有害垃圾" value="有害垃圾" />
            <el-option label="厨余垃圾" value="厨余垃圾" />
            <el-option label="其他垃圾" value="其他垃圾" />
          </el-select>
        </el-form-item>
        <el-form-item label="物品名称">
          <el-input v-model="form.name" placeholder="例如：塑料瓶" />
        </el-form-item>
        <el-form-item label="分类说明">
          <el-input type="textarea" v-model="form.description" placeholder="说明如何分类..." />
        </el-form-item>
        <el-form-item label="投放建议">
          <el-input type="textarea" v-model="form.suggestion" placeholder="建议如何投放..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const API_BASE = '/api'
const tableData = ref([])
const searchKey = ref('')
const dialogVisible = ref(false)
const dialogTitle = ref('新增知识')
const form = ref({ category: '', name: '', description: '', suggestion: '' })

const fetchList = async () => {
  const res = await axios.get(`${API_BASE}/admin/knowledge/list`, { params: { keyword: searchKey.value } })
  tableData.value = res.data
}

const getCategoryTag = (category) => {
  if (category === '可回收物') return 'success'
  if (category === '有害垃圾') return 'danger'
  if (category === '厨余垃圾') return 'warning'
  return 'info'
}

const openDialog = (mode, row) => {
  dialogVisible.value = true
  dialogTitle.value = mode === 'add' ? '新增知识' : '编辑知识'
  if (row) {
    form.value = { ...row }
  } else {
    form.value = { category: '', name: '', description: '', suggestion: '' }
  }
}

const submitForm = async () => {
  if (!form.value.category || !form.value.name) {
    ElMessage.warning('分类和物品名称不能为空')
    return
  }
  if (form.value.id) {
    await axios.put(`${API_BASE}/admin/knowledge/update`, form.value)
  } else {
    await axios.post(`${API_BASE}/admin/knowledge/add`, form.value)
  }
  dialogVisible.value = false
  fetchList()
  ElMessage.success('保存成功')
}

const handleDelete = async (id) => {
  await axios.delete(`${API_BASE}/admin/knowledge/delete/${id}`)
  fetchList()
  ElMessage.success('删除成功')
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.header { display: flex; justify-content: space-between; align-items: center; }
</style>