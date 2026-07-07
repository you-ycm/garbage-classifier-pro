<template>
  <div class="admin-container">
    <h2>⚙️ 管理员后台</h2>

    <el-tabs v-model="activeTab" type="border-card">
      <!-- 1. 系统概览 -->
      <el-tab-pane label="📊 系统概览" name="overview">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>
                <div class="stat-header">👤 用户总数</div>
              </template>
              <div class="stat-number">{{ stats.userCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>
                <div class="stat-header">📄 识别记录总数</div>
              </template>
              <div class="stat-number">{{ stats.recordCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>
                <div class="stat-header">⭐ 收藏总数</div>
              </template>
              <div class="stat-number">{{ stats.favoriteCount || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card shadow="hover">
              <template #header>
                <div class="stat-header">📊 分类数</div>
              </template>
              <div class="stat-number">{{ stats.categoryCount || 0 }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" style="margin-top: 20px;">
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <span>📊 垃圾分类统计</span>
              </template>
              <div ref="pieChartRef" style="height: 300px;"></div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card shadow="hover">
              <template #header>
                <span>📈 近7天识别趋势</span>
              </template>
              <div ref="barChartRef" style="height: 300px;"></div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- 2. 用户管理 -->
      <el-tab-pane label="👤 用户管理" name="users">
        <div class="filter-area">
          <div class="filter-left">
            <el-input
                v-model="searchKeyword"
                placeholder="搜索用户名"
                clearable
                prefix-icon="User"
                @input="loadUsers"
                style="width: 150px;"
            />

            <el-input
                v-model="searchEmail"
                placeholder="搜索邮箱"
                clearable
                prefix-icon="Message"
                @input="loadUsers"
                style="width: 150px;"
            />

            <el-select
                v-model="filterRole"
                placeholder="全部角色"
                clearable
                @change="loadUsers"
                style="width: 150px;"
            >
              <el-option label="全部角色" value="" />
              <el-option label="管理员" value="ROLE_ADMIN" />
              <el-option label="普通用户" value="ROLE_USER" />
            </el-select>

            <el-date-picker
                v-model="selectedDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                @change="loadUsers"
                style="width: 180px;"
                clearable
            />

            <el-radio-group v-model="userSortOrder" @change="loadUsers" size="small">
              <el-radio-button label="desc">最新优先</el-radio-button>
              <el-radio-button label="asc">最早优先</el-radio-button>
            </el-radio-group>

            <span style="color: #999; font-size: 14px; margin-left: 8px;">共 {{ filteredUsers.length }} 条记录</span>
          </div>

          <div class="filter-right">
            <el-button
                type="success"
                size="small"
                @click="exportUsersCSV"
                :loading="userExportLoading"
            >
              📥 导出CSV
            </el-button>
            <el-button
                type="danger"
                size="small"
                @click="batchDeleteUsers"
                :disabled="selectedUsers.length === 0"
            >
              🗑️ 批量删除 ({{ selectedUsers.length }})
            </el-button>
          </div>
        </div>

        <el-table
            :data="filteredUsers"
            style="width: 100%; margin-top: 12px;"
            @selection-change="handleUserSelectionChange"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column prop="role" label="角色">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ROLE_ADMIN' ? 'danger' : 'success'">
                {{ row.role === 'ROLE_ADMIN' ? '管理员' : '普通用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createdAt" label="注册时间" width="180" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-popconfirm title="确定删除该用户吗？" @confirm="deleteUser(row.id)">
                <template #reference>
                  <el-button type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 3. 识别记录管理 -->
      <el-tab-pane label="📄 识别记录管理" name="records">
        <div class="filter-area">
          <div class="filter-left">
            <el-input
                v-model="searchUsername"
                placeholder="搜索用户名"
                clearable
                prefix-icon="User"
                @input="loadRecords"
                style="width: 150px;"
            />

            <el-input
                v-model="searchKeyword2"
                placeholder="搜索物品名称"
                clearable
                prefix-icon="Search"
                @input="loadRecords"
                style="width: 150px;"
            />

            <el-select
                v-model="filterCategory"
                placeholder="全部类别"
                clearable
                @change="loadRecords"
                style="width: 150px;"
            >
              <el-option label="全部类别" value="" />
              <el-option label="可回收物" value="可回收物" />
              <el-option label="有害垃圾" value="有害垃圾" />
              <el-option label="厨余垃圾" value="厨余垃圾" />
              <el-option label="其他垃圾" value="其他垃圾" />
            </el-select>

            <el-date-picker
                v-model="selectedDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                @change="loadRecords"
                style="width: 180px;"
                clearable
            />

            <el-radio-group v-model="sortOrder" @change="loadRecords" size="small">
              <el-radio-button label="desc">最新优先</el-radio-button>
              <el-radio-button label="asc">最早优先</el-radio-button>
            </el-radio-group>

            <span style="color: #999; font-size: 14px; margin-left: 8px;">共 {{ total }} 条记录</span>
          </div>
          <div class="filter-right">
            <el-button
                type="primary"
                @click="shareToEmail"
                :disabled="selectedRecords.length === 0 || shareLoading"
                :loading="shareLoading"
            >
              📧 分享 ({{ selectedRecords.length }})
            </el-button>
            <el-button
                type="success"
                @click="exportCSV"
                :loading="exportLoading"
            >
              📥 导出CSV
            </el-button>
            <el-button
                type="danger"
                @click="batchDeleteRecords"
                :disabled="selectedRecords.length === 0 || deleteLoading"
                :loading="deleteLoading"
            >
              🗑️ 删除 ({{ selectedRecords.length }})
            </el-button>
            <el-button
                type="danger"
                plain
                size="small"
                @click="clearSelection"
                :disabled="selectedRecords.length === 0"
            >
              取消选择
            </el-button>
          </div>
        </div>

        <el-table
            :data="records"
            stripe
            style="width: 100%"
            @selection-change="handleSelectionChange"
            ref="tableRef"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column label="图片" width="80">
            <template #default="{ row }">
              <el-image
                  v-if="row.imageUrl"
                  :src="`http://192.168.58.128:8080${row.imageUrl}`"
                  fit="cover"
                  style="width: 50px; height: 50px; border-radius: 4px; cursor: pointer;"
                  :preview-src-list="[`http://192.168.58.128:8080${row.imageUrl}`]"
                  :preview-teleported="true"
              />
              <span v-else style="color: #999; font-size: 12px;">无图片</span>
            </template>
          </el-table-column>
          <el-table-column prop="itemName" label="物品名称" width="150" />
          <el-table-column prop="category" label="分类" width="120">
            <template #default="{ row }">
              <el-tag :type="getCategoryType(row.category)">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="⭐" width="60" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isFavorited" type="warning" size="small">⭐</el-tag>
              <span v-else style="color: #ccc;">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="confidence" label="置信度" width="150">
            <template #default="{ row }">
              {{ (row.confidence * 100).toFixed(2) }}%
            </template>
          </el-table-column>
          <el-table-column prop="username" label="用户名称" width="150" />
          <el-table-column prop="createdAt" label="识别时间" width="180" />
          <el-table-column label="操作" width="150">
            <template #default="{ row }">
              <el-button type="danger" size="small" @click="deleteRecord(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            :page-sizes="[5, 10, 20, 50]"
            @current-change="loadRecords"
            @size-change="loadRecords"
            style="margin-top: 16px;"
        />
      </el-tab-pane>

      <!-- 4. 操作日志 -->
      <el-tab-pane label="📋 操作日志" name="logs">
        <div class="filter-area">
          <div class="filter-left">
            <el-input
                v-model="logSearchUsername"
                placeholder="搜索用户名"
                clearable
                prefix-icon="User"
                @input="loadLogs"
                style="width: 150px;"
            />

            <el-input
                v-model="logSearchOperation"
                placeholder="操作类型(登录/删除)"
                clearable
                prefix-icon="Operation"
                @input="loadLogs"
                style="width: 150px;"
            />

            <el-input
                v-model="logSearchIp"
                placeholder="IP地址"
                clearable
                prefix-icon="Connection"
                @input="loadLogs"
                style="width: 150px;"
            />

            <el-date-picker
                v-model="logSelectedDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                @change="loadLogs"
                style="width: 180px;"
                clearable
            />

            <el-radio-group v-model="logSortOrder" @change="loadLogs" size="small">
              <el-radio-button label="desc">最新优先</el-radio-button>
              <el-radio-button label="asc">最早优先</el-radio-button>
            </el-radio-group>

            <span style="color: #999; font-size: 14px; margin-left: 8px;">共 {{ logTotal }} 条记录</span>
          </div>
          <div class="filter-right">
            <el-button
                type="success"
                size="small"
                @click="exportLogsCSV"
                :loading="logExportLoading"
            >
              📥 导出CSV
            </el-button>
            <el-button
                type="danger"
                size="small"
                @click="batchDeleteLogs"
                :disabled="selectedLogs.length === 0"
                :loading="logDeleteLoading"
            >
              🗑️ 批量删除 ({{ selectedLogs.length }})
            </el-button>
          </div>
        </div>

        <el-table
            :data="logs"
            stripe
            style="width: 100%; margin-top: 12px;"
            @selection-change="handleLogSelectionChange"
            ref="logTableRef"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="username" label="用户" width="120" />
          <el-table-column prop="operation" label="操作" width="120" />
          <el-table-column prop="detail" label="详情" />
          <el-table-column prop="ip" label="IP" width="140" />
          <el-table-column prop="createdAt" label="操作时间" width="180" />
        </el-table>

        <el-pagination
            v-model:current-page="logPage"
            v-model:page-size="logSize"
            :total="logTotal"
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="loadLogs"
            @size-change="loadLogs"
            style="margin-top: 16px;"
        />
      </el-tab-pane>

      <!-- 5. 收藏管理（已增强：显示图片和置信度） -->
      <el-tab-pane label="⭐ 收藏管理" name="favorites">
        <div class="filter-area">
          <div class="filter-left">
            <el-input
                v-model="favSearchUsername"
                placeholder="搜索用户名"
                clearable
                prefix-icon="User"
                @input="loadFavorites"
                style="width: 150px;"
            />

            <el-input
                v-model="favSearchItemName"
                placeholder="搜索物品名称"
                clearable
                prefix-icon="Search"
                @input="loadFavorites"
                style="width: 150px;"
            />

            <el-select
                v-model="favCategory"
                placeholder="全部类别"
                clearable
                @change="loadFavorites"
                style="width: 150px;"
            >
              <el-option label="全部类别" value="" />
              <el-option label="可回收物" value="可回收物" />
              <el-option label="有害垃圾" value="有害垃圾" />
              <el-option label="厨余垃圾" value="厨余垃圾" />
              <el-option label="其他垃圾" value="其他垃圾" />
            </el-select>

            <el-date-picker
                v-model="favSelectedDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                @change="loadFavorites"
                style="width: 180px;"
                clearable
            />

            <el-radio-group v-model="favSortOrder" @change="loadFavorites" size="small">
              <el-radio-button label="desc">最新优先</el-radio-button>
              <el-radio-button label="asc">最早优先</el-radio-button>
            </el-radio-group>

            <span style="color: #999; font-size: 14px; margin-left: 8px;">共 {{ favTotal }} 条收藏</span>
          </div>
          <div class="filter-right">
            <el-button
                type="primary"
                size="small"
                @click="shareFavorites"
                :disabled="selectedFavorites.length === 0 || favShareLoading"
                :loading="favShareLoading"
            >
              📧 分享 ({{ selectedFavorites.length }})
            </el-button>
            <el-button
                type="success"
                size="small"
                @click="exportFavoritesCSV"
                :loading="favExportLoading"
            >
              📥 导出CSV
            </el-button>
            <el-button
                type="danger"
                size="small"
                @click="batchDeleteFavorites"
                :disabled="selectedFavorites.length === 0 || favDeleteLoading"
                :loading="favDeleteLoading"
            >
              🗑️ 批量删除 ({{ selectedFavorites.length }})
            </el-button>
            <el-button
                type="danger"
                plain
                size="small"
                @click="clearFavSelection"
                :disabled="selectedFavorites.length === 0"
            >
              取消选择
            </el-button>
          </div>
        </div>

        <el-table
            :data="favList"
            stripe
            style="width: 100%"
            @selection-change="handleFavSelectionChange"
            ref="favTableRef"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="userId" label="用户ID" width="80" />
          <el-table-column prop="username" label="用户名" width="120" />

          <!-- 🆕 图片列 -->
          <el-table-column label="图片" width="80">
            <template #default="{ row }">
              <el-image
                  v-if="row.imageUrl"
                  :src="`http://192.168.58.128:8080${row.imageUrl}`"
                  fit="cover"
                  style="width: 50px; height: 50px; border-radius: 4px; cursor: pointer;"
                  :preview-src-list="[`http://192.168.58.128:8080${row.imageUrl}`]"
                  :preview-teleported="true"
              />
              <span v-else style="color: #ccc; font-size: 12px;">无图片</span>
            </template>
          </el-table-column>

          <el-table-column prop="itemName" label="物品名称" width="150" />

          <el-table-column label="分类" width="120">
            <template #default="{ row }">
              <el-tag :type="getCategoryType(row.category)">{{ row.category }}</el-tag>
            </template>
          </el-table-column>

          <!-- 🆕 置信度列 -->
          <el-table-column prop="confidence" label="置信度" width="120">
            <template #default="{ row }">
              <span v-if="row.confidence && row.confidence > 0">
                {{ (row.confidence * 100).toFixed(2) }}%
              </span>
              <span v-else style="color: #ccc;">-</span>
            </template>
          </el-table-column>

          <el-table-column prop="createdAt" label="收藏时间" width="180" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button type="danger" size="small" @click="deleteFavorite(row.id)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
            v-model:current-page="favPage"
            v-model:page-size="favSize"
            :total="favTotal"
            layout="total, sizes, prev, pager, next"
            :page-sizes="[10, 20, 50, 100]"
            @current-change="loadFavorites"
            @size-change="loadFavorites"
            style="margin-top: 16px;"
        />
      </el-tab-pane>

      <!-- 6. 知识库管理 -->
      <el-tab-pane label="📚 知识库管理" name="knowledge">
        <div class="filter-area">
          <div class="filter-left">
            <el-input
                v-model="knowledgeKeyword"
                placeholder="搜索知识库..."
                clearable
                prefix-icon="Search"
                @input="loadKnowledge"
                style="width: 200px;"
            />
            <el-radio-group v-model="knowledgeOrder" @change="loadKnowledge" size="small">
              <el-radio-button label="desc">最新优先</el-radio-button>
              <el-radio-button label="asc">最早优先</el-radio-button>
            </el-radio-group>
            <span style="color: #999; font-size: 14px;">共 {{ knowledgeTotal }} 条记录</span>
          </div>
          <div class="filter-right">
            <el-button type="success" @click="openBatchAddDialog">📋 批量新增</el-button>
            <el-button type="primary" @click="openKnowledgeDialog('add')">➕ 新增条目</el-button>
          </div>
        </div>

        <div class="filter-area" style="margin-top: -8px; background: #fafafa;">
          <div class="filter-left">
            <span style="font-size: 13px; color: #666;">批量操作：</span>
            <el-button
                type="danger"
                size="small"
                @click="batchDeleteKnowledge"
                :disabled="selectedKnowledge.length === 0"
                :loading="knowledgeDeleteLoading"
            >
              🗑️ 删除 ({{ selectedKnowledge.length }})
            </el-button>
            <el-button
                type="primary"
                size="small"
                @click="shareKnowledge"
                :disabled="selectedKnowledge.length === 0 || knowledgeShareLoading"
                :loading="knowledgeShareLoading"
            >
              📧 分享 ({{ selectedKnowledge.length }})
            </el-button>
            <el-button
                type="success"
                size="small"
                @click="exportKnowledgeCSV"
                :loading="knowledgeExportLoading"
            >
              📥 导出CSV
            </el-button>
            <el-button
                type="danger"
                plain
                size="small"
                @click="clearKnowledgeSelection"
                :disabled="selectedKnowledge.length === 0"
            >
              取消选择
            </el-button>
          </div>
        </div>

        <el-table
            :data="knowledgeList"
            stripe
            style="width: 100%;"
            @selection-change="handleKnowledgeSelectionChange"
            ref="knowledgeTableRef"
        >
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="60" />
          <el-table-column prop="category" label="分类" width="120">
            <template #default="{ row }">
              <el-tag :type="getCategoryTag(row.category)">{{ row.category }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="物品名称" width="150" />
          <el-table-column prop="description" label="分类说明" show-overflow-tooltip />
          <el-table-column prop="suggestion" label="投放建议" show-overflow-tooltip />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="openKnowledgeDialog('edit', row)">编辑</el-button>
              <el-popconfirm title="确定删除该知识条目吗？" @confirm="deleteKnowledge(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 新增/编辑知识库的弹窗 -->
    <el-dialog v-model="knowledgeDialogVisible" :title="knowledgeDialogTitle" width="500px">
      <el-form :model="knowledgeForm" label-width="100px">
        <el-form-item label="分类">
          <el-select v-model="knowledgeForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="可回收物" value="可回收物" />
            <el-option label="有害垃圾" value="有害垃圾" />
            <el-option label="厨余垃圾" value="厨余垃圾" />
            <el-option label="其他垃圾" value="其他垃圾" />
          </el-select>
        </el-form-item>
        <el-form-item label="物品名称">
          <el-input v-model="knowledgeForm.name" placeholder="例如：塑料瓶" />
        </el-form-item>
        <el-form-item label="分类说明">
          <el-input type="textarea" v-model="knowledgeForm.description" placeholder="说明如何分类..." />
        </el-form-item>
        <el-form-item label="投放建议">
          <el-input type="textarea" v-model="knowledgeForm.suggestion" placeholder="建议如何投放..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="knowledgeDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitKnowledgeForm">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 批量新增知识库的弹窗 -->
    <el-dialog v-model="batchAddDialogVisible" title="📋 批量新增知识条目" width="600px">
      <el-alert
          title="使用说明"
          type="info"
          description="每行一条，格式：分类,物品名称,分类说明,投放建议（用英文逗号分隔）"
          :closable="false"
          style="margin-bottom: 16px;"
      />
      <el-input
          v-model="batchAddText"
          type="textarea"
          :rows="12"
          placeholder="示例：&#10;可回收物,纸类,报纸纸箱书本,保持清洁干燥压扁后投放&#10;有害垃圾,电池,干电池充电电池,放入红色垃圾桶"
          style="font-family: monospace;"
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="batchAddDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitBatchAdd" :loading="batchAddLoading">
            确认批量新增 ({{ batchAddText.split('\n').filter(line => line.trim()).length }} 条)
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { useUserStore } from '../store/userStore'
import * as echarts from 'echarts'

const userStore = useUserStore()
const API_BASE_URL = 'http://192.168.58.128:8080/api'

const activeTab = ref('overview')
const filterCategory = ref('')
const selectedDate = ref('')
const sortOrder = ref('desc')
const searchKeyword = ref('')
const searchKeyword2 = ref('')
const searchUsername = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedRecords = ref([])
const shareLoading = ref(false)
const deleteLoading = ref(false)
const exportLoading = ref(false)
const tableRef = ref(null)

// 日志相关
const logSearchUsername = ref('')
const logSearchOperation = ref('')
const logSearchIp = ref('')
const logSelectedDate = ref('')
const logSortOrder = ref('desc')
const logs = ref([])
const logTotal = ref(0)
const logPage = ref(1)
const logSize = ref(20)
const logExportLoading = ref(false)
const logDeleteLoading = ref(false)
const logTableRef = ref(null)
const selectedLogs = ref([])

// 收藏管理相关
const favSearchUsername = ref('')
const favSearchItemName = ref('')
const favCategory = ref('')
const favSelectedDate = ref('')
const favSortOrder = ref('desc')
const favList = ref([])
const favTotal = ref(0)
const favPage = ref(1)
const favSize = ref(20)
const favExportLoading = ref(false)
const favDeleteLoading = ref(false)
const favShareLoading = ref(false)
const favTableRef = ref(null)
const selectedFavorites = ref([])

// 知识库管理相关
const knowledgeKeyword = ref('')
const knowledgeOrder = ref('desc')
const knowledgeList = ref([])
const knowledgeTotal = ref(0)
const knowledgeDialogVisible = ref(false)
const knowledgeDialogTitle = ref('')
const knowledgeForm = ref({ category: '', name: '', description: '', suggestion: '' })
const selectedKnowledge = ref([])
const knowledgeTableRef = ref(null)
const knowledgeDeleteLoading = ref(false)
const knowledgeShareLoading = ref(false)
const knowledgeExportLoading = ref(false)

// 批量新增相关
const batchAddDialogVisible = ref(false)
const batchAddText = ref('')
const batchAddLoading = ref(false)

// 用户管理多条件搜索参数
const searchEmail = ref('')
const filterRole = ref('')
const userSortOrder = ref('asc')
const selectedUsers = ref([])
const userExportLoading = ref(false)

// 图表引用
const pieChartRef = ref(null)
const barChartRef = ref(null)
let pieChart = null
let barChart = null

const stats = ref({
  userCount: 0,
  recordCount: 0,
  favoriteCount: 0,
  categoryCount: 0,
  categoryStats: [],
  trendStats: []
})
const users = ref([])
const records = ref([])

const filteredUsers = computed(() => {
  return users.value
})

// ===== 统计 =====
const loadStats = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/admin/stats`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      stats.value = response.data.data
      stats.value.categoryCount = stats.value.categoryStats?.length || 0
      nextTick(() => {
        initCharts()
      })
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// ===== 图表 =====
const initCharts = () => {
  if (pieChartRef.value) {
    if (pieChart) pieChart.dispose()
    pieChart = echarts.init(pieChartRef.value)
    const pieData = stats.value.categoryStats || []
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        type: 'pie',
        radius: '50%',
        label: {
          show: true,
          formatter: '{b}\n{d}%'
        },
        data: pieData.map(item => ({
          name: item.name,
          value: item.value,
          itemStyle: { color: item.color }
        })),
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
      }]
    })
    window.addEventListener('resize', () => pieChart?.resize())
  }

  if (barChartRef.value) {
    if (barChart) barChart.dispose()
    barChart = echarts.init(barChartRef.value)
    const trendData = stats.value.trendStats || []
    let maxY = 2;
    if (trendData.length > 0) {
      let maxVal = Math.max(...trendData.map(item => item.count));
      if (maxVal > 2) maxY = maxVal + 2;
    }

    barChart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: trendData.map(item => item.date)
      },
      yAxis: {
        type: 'value',
        name: '识别数量',
        minInterval: 1
      },
      series: [{
        type: 'bar',
        data: trendData.map(item => item.count || 0),
        itemStyle: { color: '#409EFF' }
      }]
    })
    window.addEventListener('resize', () => barChart?.resize())
  }
}

// ===== 用户管理 =====
const loadUsers = async () => {
  try {
    const params = {}
    if (searchKeyword.value) params.username = searchKeyword.value
    if (searchEmail.value) params.email = searchEmail.value
    if (filterRole.value) params.role = filterRole.value
    if (selectedDate.value) params.date = selectedDate.value
    params.sortOrder = userSortOrder.value

    const response = await axios.get(`${API_BASE_URL}/admin/users`, {
      params: params,
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      users.value = response.data.data
      selectedUsers.value = []
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  }
}

const handleUserSelectionChange = (selection) => {
  selectedUsers.value = selection
}

const batchDeleteUsers = async () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请先选择要删除的用户')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedUsers.value.length} 个用户吗？此操作不可恢复！`,
        '确认删除',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  try {
    const ids = selectedUsers.value.map(u => u.id)
    await axios.delete(`${API_BASE_URL}/admin/users/batch`, {
      data: { ids: ids },
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    ElMessage.success(`成功删除 ${selectedUsers.value.length} 个用户！`)
    loadUsers()
  } catch (error) {
    console.error('批量删除用户失败:', error)
    ElMessage.error('批量删除用户失败，请稍后重试')
  }
}

const deleteUser = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await axios.delete(`${API_BASE_URL}/admin/users/${id}`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    ElMessage.success('用户删除成功')
    loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
      ElMessage.error('删除用户失败')
    }
  }
}

const exportUsersCSV = async () => {
  userExportLoading.value = true
  try {
    const params = {}
    if (searchKeyword.value) params.username = searchKeyword.value
    if (searchEmail.value) params.email = searchEmail.value
    if (filterRole.value) params.role = filterRole.value
    if (selectedDate.value) params.date = selectedDate.value
    params.sortOrder = userSortOrder.value

    if (selectedUsers.value.length > 0) {
      params.ids = selectedUsers.value.map(u => u.id).join(',')
    }

    const response = await axios.get(`${API_BASE_URL}/admin/users/export`, {
      params: params,
      headers: { 'Authorization': `Bearer ${userStore.token}` },
      responseType: 'blob'
    })

    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `用户管理_${new Date().toISOString().slice(0,10)}.csv`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    ElMessage.success('导出成功！')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    userExportLoading.value = false
  }
}

// ===== 识别记录管理 =====
const loadRecords = async () => {
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      sortOrder: sortOrder.value
    }
    if (filterCategory.value) params.category = filterCategory.value
    if (selectedDate.value) params.date = selectedDate.value
    if (searchKeyword2.value) {
      params.keyword = searchKeyword2.value
    }
    if (searchUsername.value) {
      params.username = searchUsername.value
    }

    const response = await axios.get(`${API_BASE_URL}/admin/records`, {
      params: params,
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })

    if (response.data.code === 200) {
      const data = response.data.data.data || []
      total.value = response.data.data.total || 0

      if (data.length > 0) {
        const recordIds = data.map(r => r.id)
        try {
          const favResponse = await axios.post(`${API_BASE_URL}/favorites/check/batch`,
              { recordIds: recordIds },
              { headers: { 'Authorization': `Bearer ${userStore.token}` } }
          )
          if (favResponse.data.code === 200) {
            const favMap = favResponse.data.data || {}
            data.forEach(r => {
              r.isFavorited = favMap[r.id] || false
            })
          }
        } catch (error) {
          console.error('检查收藏状态失败:', error)
        }
      }

      records.value = data
      selectedRecords.value = []
    }
  } catch (error) {
    console.error('加载识别记录失败:', error)
    ElMessage.error('加载识别记录失败')
  }
}

// ===== 表格选中事件 =====
const handleSelectionChange = (selection) => {
  selectedRecords.value = selection
  console.log('✅ 当前选中记录数:', selection.length)
}

// ===== 导出识别记录 CSV =====
const exportCSV = async () => {
  exportLoading.value = true

  try {
    const params = {}

    if (filterCategory.value) params.category = filterCategory.value
    if (selectedDate.value) params.date = selectedDate.value
    if (searchKeyword2.value) params.keyword = searchKeyword2.value
    if (searchUsername.value) params.username = searchUsername.value
    params.sortOrder = sortOrder.value

    if (selectedRecords.value && selectedRecords.value.length > 0) {
      params.ids = selectedRecords.value.map(r => r.id).join(',')
      console.log('✅ 准备导出的记录ID:', params.ids)
    } else {
      console.log('ℹ️ 未选中任何记录，将导出全部（按当前筛选条件）')
    }

    const response = await axios.get(`${API_BASE_URL}/admin/records/export`, {
      params: params,
      headers: {
        'Authorization': `Bearer ${userStore.token}`
      },
      responseType: 'blob'
    })

    if (response.data.type === 'application/json') {
      const reader = new FileReader()
      reader.onload = () => {
        try {
          const errorData = JSON.parse(reader.result)
          ElMessage.error(errorData.message || '导出失败，请重试')
        } catch {
          ElMessage.error('导出失败，响应格式异常')
        }
      }
      reader.readAsText(response.data)
      return
    }

    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `识别记录_${new Date().toISOString().slice(0,10)}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功！')

  } catch (error) {
    console.error('❌ 导出失败:', error)
    if (error.response) {
      ElMessage.error(`导出失败：${error.response.status} ${error.response.statusText}`)
    } else if (error.request) {
      ElMessage.error('导出失败：网络请求未收到响应，请检查后端服务')
    } else {
      ElMessage.error(`导出失败：${error.message}`)
    }
  } finally {
    exportLoading.value = false
  }
}

const clearSelection = () => {
  if (tableRef.value) {
    tableRef.value.clearSelection()
  }
  selectedRecords.value = []
}

const deleteRecord = async (id) => {
  if (!confirm('确定要删除该记录吗？')) return
  try {
    await axios.delete(`${API_BASE_URL}/admin/records/${id}`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    ElMessage.success('删除成功')
    loadRecords()
  } catch (error) {
    console.error('删除记录失败:', error)
    ElMessage.error('删除记录失败')
  }
}

const batchDeleteRecords = async () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请先选择要删除的记录')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedRecords.value.length} 条记录吗？此操作不可恢复！`,
        '确认删除',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  deleteLoading.value = true
  try {
    const ids = selectedRecords.value.map(r => r.id)
    await axios.delete(`${API_BASE_URL}/admin/records/batch`, {
      data: { ids: ids },
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    ElMessage.success(`成功删除 ${selectedRecords.value.length} 条记录！`)
    clearSelection()
    loadRecords()
  } catch (error) {
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败，请稍后重试')
  } finally {
    deleteLoading.value = false
  }
}

const shareToEmail = async () => {
  if (selectedRecords.value.length === 0) {
    ElMessage.warning('请先选择要分享的记录')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要将选中的 ${selectedRecords.value.length} 条记录发送到邮箱吗？`,
        '确认分享',
        { confirmButtonText: '确定发送', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  shareLoading.value = true
  try {
    const ids = selectedRecords.value.map(r => r.id)
    await axios.post(`${API_BASE_URL}/admin/records/share`,
        { ids: ids },
        { headers: { 'Authorization': `Bearer ${userStore.token}` } }
    )
    ElMessage.success(`成功发送 ${selectedRecords.value.length} 条记录到您的邮箱！`)
    clearSelection()
  } catch (error) {
    console.error('分享失败:', error)
    ElMessage.error('分享失败，请检查是否已绑定邮箱或网络连接')
  } finally {
    shareLoading.value = false
  }
}

// ===== 操作日志 =====
const loadLogs = async () => {
  try {
    const params = { page: logPage.value, size: logSize.value }
    if (logSearchUsername.value) params.username = logSearchUsername.value
    if (logSearchOperation.value) params.operation = logSearchOperation.value
    if (logSearchIp.value) params.ip = logSearchIp.value
    if (logSelectedDate.value) params.date = logSelectedDate.value
    params.sortOrder = logSortOrder.value

    const response = await axios.get(`${API_BASE_URL}/admin/logs`, {
      params: params,
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })

    if (response.data.code === 200) {
      logs.value = response.data.data.content || []
      logTotal.value = response.data.data.totalElements || 0
      selectedLogs.value = []
    }
  } catch (error) {
    console.error('加载操作日志失败:', error)
    ElMessage.error('加载操作日志失败')
  }
}

const handleLogSelectionChange = (selection) => {
  selectedLogs.value = selection
}

const batchDeleteLogs = async () => {
  if (selectedLogs.value.length === 0) {
    ElMessage.warning('请先选择要删除的记录')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedLogs.value.length} 条日志记录吗？此操作不可恢复！`,
        '确认删除',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  logDeleteLoading.value = true
  try {
    const ids = selectedLogs.value.map(l => l.id)
    await axios.delete(`${API_BASE_URL}/admin/logs/batch`, {
      data: { ids: ids },
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    ElMessage.success(`成功删除 ${selectedLogs.value.length} 条记录！`)
    clearLogSelection()
    loadLogs()
  } catch (error) {
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败，请稍后重试')
  } finally {
    logDeleteLoading.value = false
  }
}

const clearLogSelection = () => {
  if (logTableRef.value) {
    logTableRef.value.clearSelection()
  }
  selectedLogs.value = []
}

const exportLogsCSV = async () => {
  logExportLoading.value = true
  try {
    const params = {}
    if (logSearchUsername.value) params.username = logSearchUsername.value
    if (logSearchOperation.value) params.operation = logSearchOperation.value
    if (logSearchIp.value) params.ip = logSearchIp.value
    if (logSelectedDate.value) params.date = logSelectedDate.value
    params.sortOrder = logSortOrder.value

    if (selectedLogs.value.length > 0) {
      params.ids = selectedLogs.value.map(l => l.id).join(',')
    }

    const response = await axios.get(`${API_BASE_URL}/admin/logs/export`, {
      params: params,
      headers: { 'Authorization': `Bearer ${userStore.token}` },
      responseType: 'blob'
    })

    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `操作日志_${new Date().toISOString().slice(0,10)}.csv`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    ElMessage.success('导出成功！')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    logExportLoading.value = false
  }
}

// ===== 收藏管理 =====
const loadFavorites = async () => {
  try {
    const params = { page: favPage.value, size: favSize.value }

    if (favSearchUsername.value) params.username = favSearchUsername.value
    if (favSearchItemName.value) params.keyword = favSearchItemName.value
    if (favCategory.value) params.category = favCategory.value
    if (favSelectedDate.value) params.date = favSelectedDate.value
    params.sortOrder = favSortOrder.value

    const response = await axios.get(`${API_BASE_URL}/admin/favorites`, {
      params: params,
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })

    if (response.data.code === 200) {
      favList.value = response.data.data.content || []
      favTotal.value = response.data.data.totalElements || 0
      selectedFavorites.value = []
    }
  } catch (error) {
    console.error('加载收藏列表失败:', error)
    ElMessage.error('加载收藏列表失败')
  }
}

const handleFavSelectionChange = (selection) => {
  selectedFavorites.value = selection
}

const clearFavSelection = () => {
  if (favTableRef.value) {
    favTableRef.value.clearSelection()
  }
  selectedFavorites.value = []
}

const deleteFavorite = async (id) => {
  if (!confirm('确定要删除该收藏吗？')) return
  try {
    await axios.delete(`${API_BASE_URL}/admin/favorites/${id}`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    ElMessage.success('删除成功')
    loadFavorites()
  } catch (error) {
    console.error('删除收藏失败:', error)
    ElMessage.error('删除收藏失败')
  }
}

const batchDeleteFavorites = async () => {
  if (selectedFavorites.value.length === 0) {
    ElMessage.warning('请先选择要删除的收藏')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedFavorites.value.length} 条收藏记录吗？此操作不可恢复！`,
        '确认删除',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  favDeleteLoading.value = true
  try {
    const ids = selectedFavorites.value.map(f => f.id)
    await axios.delete(`${API_BASE_URL}/admin/favorites/batch`, {
      data: { ids: ids },
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    ElMessage.success(`成功删除 ${selectedFavorites.value.length} 条收藏记录！`)
    clearFavSelection()
    loadFavorites()
  } catch (error) {
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败，请稍后重试')
  } finally {
    favDeleteLoading.value = false
  }
}

const shareFavorites = async () => {
  if (selectedFavorites.value.length === 0) {
    ElMessage.warning('请先选择要分享的收藏')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要将选中的 ${selectedFavorites.value.length} 条收藏记录发送到邮箱吗？`,
        '确认分享',
        { confirmButtonText: '确定发送', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  favShareLoading.value = true
  try {
    const ids = selectedFavorites.value.map(f => f.id)
    await axios.post(`${API_BASE_URL}/admin/favorites/share`,
        { ids: ids },
        { headers: { 'Authorization': `Bearer ${userStore.token}` } }
    )
    ElMessage.success(`成功发送 ${selectedFavorites.value.length} 条收藏记录到您的邮箱！`)
    clearFavSelection()
  } catch (error) {
    console.error('分享失败:', error)
    ElMessage.error('分享失败，请检查是否已绑定邮箱或网络连接')
  } finally {
    favShareLoading.value = false
  }
}

const exportFavoritesCSV = async () => {
  favExportLoading.value = true
  try {
    const params = {}
    if (favSearchUsername.value) params.username = favSearchUsername.value
    if (favSearchItemName.value) params.keyword = favSearchItemName.value
    if (favCategory.value) params.category = favCategory.value
    if (favSelectedDate.value) params.date = favSelectedDate.value
    params.sortOrder = favSortOrder.value

    if (selectedFavorites.value.length > 0) {
      params.ids = selectedFavorites.value.map(f => f.id).join(',')
    }

    const response = await axios.get(`${API_BASE_URL}/admin/favorites/export`, {
      params: params,
      headers: { 'Authorization': `Bearer ${userStore.token}` },
      responseType: 'blob'
    })

    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `收藏管理_${new Date().toISOString().slice(0,10)}.csv`)
    document.body.appendChild(link)
    link.click()
    link.remove()
    ElMessage.success('导出成功！')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    favExportLoading.value = false
  }
}

// ===== 知识库管理 =====
const loadKnowledge = async () => {
  try {
    const params = {
      order: knowledgeOrder.value
    }
    if (knowledgeKeyword.value) params.keyword = knowledgeKeyword.value
    const response = await axios.get(`${API_BASE_URL}/admin/knowledge/list`, {
      params: params,
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    if (response.data.code === 200) {
      knowledgeList.value = response.data.data
      knowledgeTotal.value = knowledgeList.value.length
    }
  } catch (error) {
    console.error('加载知识库失败:', error)
    ElMessage.error('加载知识库失败')
  }
}

const openKnowledgeDialog = (mode, row) => {
  knowledgeDialogVisible.value = true
  if (mode === 'add') {
    knowledgeDialogTitle.value = '新增知识条目'
    knowledgeForm.value = { category: '', name: '', description: '', suggestion: '' }
  } else {
    knowledgeDialogTitle.value = '编辑知识条目'
    knowledgeForm.value = { ...row }
  }
}

const submitKnowledgeForm = async () => {
  if (!knowledgeForm.value.category || !knowledgeForm.value.name) {
    ElMessage.warning('分类和物品名称不能为空')
    return
  }
  try {
    if (knowledgeForm.value.id) {
      await axios.put(`${API_BASE_URL}/admin/knowledge/update`, knowledgeForm.value, {
        headers: { 'Authorization': `Bearer ${userStore.token}` }
      })
      ElMessage.success('更新成功')
    } else {
      await axios.post(`${API_BASE_URL}/admin/knowledge/add`, knowledgeForm.value, {
        headers: { 'Authorization': `Bearer ${userStore.token}` }
      })
      ElMessage.success('新增成功')
    }
    knowledgeDialogVisible.value = false
    loadKnowledge()
  } catch (error) {
    console.error('保存知识库失败:', error)
    ElMessage.error('保存失败，请检查输入')
  }
}

const deleteKnowledge = async (id) => {
  try {
    await axios.delete(`${API_BASE_URL}/admin/knowledge/${id}`, {
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    ElMessage.success('删除成功')
    loadKnowledge()
  } catch (error) {
    console.error('删除知识库失败:', error)
    ElMessage.error('删除失败')
  }
}

// ---- 知识库批量操作 ----
const handleKnowledgeSelectionChange = (selection) => {
  selectedKnowledge.value = selection
  console.log('✅ 当前选中知识条目数:', selection.length)
}

const clearKnowledgeSelection = () => {
  if (knowledgeTableRef.value) {
    knowledgeTableRef.value.clearSelection()
  }
  selectedKnowledge.value = []
}

const batchDeleteKnowledge = async () => {
  if (selectedKnowledge.value.length === 0) {
    ElMessage.warning('请先选择要删除的知识条目')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要删除选中的 ${selectedKnowledge.value.length} 条知识条目吗？此操作不可恢复！`,
        '确认删除',
        { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    return
  }

  knowledgeDeleteLoading.value = true
  try {
    const ids = selectedKnowledge.value.map(item => item.id)
    await axios.delete(`${API_BASE_URL}/admin/knowledge/batch`, {
      data: { ids: ids },
      headers: { 'Authorization': `Bearer ${userStore.token}` }
    })
    ElMessage.success(`成功删除 ${selectedKnowledge.value.length} 条知识条目！`)
    clearKnowledgeSelection()
    loadKnowledge()
  } catch (error) {
    console.error('批量删除失败:', error)
    ElMessage.error('批量删除失败，请稍后重试')
  } finally {
    knowledgeDeleteLoading.value = false
  }
}

const shareKnowledge = async () => {
  if (selectedKnowledge.value.length === 0) {
    ElMessage.warning('请先选择要分享的知识条目')
    return
  }

  try {
    await ElMessageBox.confirm(
        `确定要将选中的 ${selectedKnowledge.value.length} 条知识条目发送到邮箱吗？`,
        '确认分享',
        { confirmButtonText: '确定发送', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  knowledgeShareLoading.value = true
  try {
    const ids = selectedKnowledge.value.map(item => item.id)
    await axios.post(`${API_BASE_URL}/admin/knowledge/share`,
        { ids: ids },
        { headers: { 'Authorization': `Bearer ${userStore.token}` } }
    )
    ElMessage.success(`成功发送 ${selectedKnowledge.value.length} 条知识条目到您的邮箱！`)
    clearKnowledgeSelection()
  } catch (error) {
    console.error('分享失败:', error)
    ElMessage.error('分享失败，请检查是否已绑定邮箱或网络连接')
  } finally {
    knowledgeShareLoading.value = false
  }
}

const exportKnowledgeCSV = async () => {
  knowledgeExportLoading.value = true
  try {
    const params = {}
    if (knowledgeKeyword.value) params.keyword = knowledgeKeyword.value

    if (selectedKnowledge.value.length > 0) {
      params.ids = selectedKnowledge.value.map(item => item.id).join(',')
    }

    const response = await axios.get(`${API_BASE_URL}/admin/knowledge/export`, {
      params: params,
      headers: { 'Authorization': `Bearer ${userStore.token}` },
      responseType: 'blob'
    })

    if (response.data.type === 'application/json') {
      const reader = new FileReader()
      reader.onload = () => {
        try {
          const errorData = JSON.parse(reader.result)
          ElMessage.error(errorData.message || '导出失败，请重试')
        } catch {
          ElMessage.error('导出失败，响应格式异常')
        }
      }
      reader.readAsText(response.data)
      return
    }

    const blob = new Blob([response.data], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `知识库_${new Date().toISOString().slice(0,10)}.csv`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功！')
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    knowledgeExportLoading.value = false
  }
}

const openBatchAddDialog = () => {
  batchAddText.value = ''
  batchAddDialogVisible.value = true
}

const submitBatchAdd = async () => {
  const lines = batchAddText.value.split('\n').filter(line => line.trim())
  if (lines.length === 0) {
    ElMessage.warning('请至少输入一条数据')
    return
  }

  const items = []
  let hasError = false
  for (let i = 0; i < lines.length; i++) {
    const parts = lines[i].split(',').map(s => s.trim())
    if (parts.length < 2) {
      ElMessage.warning(`第 ${i + 1} 行格式错误：至少需要"分类,物品名称"`)
      hasError = true
      break
    }
    items.push({
      category: parts[0],
      name: parts[1],
      description: parts[2] || '',
      suggestion: parts[3] || ''
    })
  }

  if (hasError) return

  try {
    await ElMessageBox.confirm(
        `确定要批量新增 ${items.length} 条知识条目吗？`,
        '确认批量新增',
        { confirmButtonText: '确定新增', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  batchAddLoading.value = true
  try {
    await axios.post(`${API_BASE_URL}/admin/knowledge/batch-add`,
        items,
        { headers: { 'Authorization': `Bearer ${userStore.token}` } }
    )
    ElMessage.success(`成功新增 ${items.length} 条知识条目！`)
    batchAddDialogVisible.value = false
    loadKnowledge()
  } catch (error) {
    console.error('批量新增失败:', error)
    ElMessage.error('批量新增失败，请检查数据格式')
  } finally {
    batchAddLoading.value = false
  }
}

// ===== 工具函数 =====
const getCategoryType = (category) => {
  const map = {
    '可回收物': 'success',
    '有害垃圾': 'danger',
    '厨余垃圾': 'warning',
    '其他垃圾': 'info'
  }
  return map[category] || 'info'
}

const getCategoryTag = (category) => {
  const map = {
    '可回收物': 'success',
    '有害垃圾': 'danger',
    '厨余垃圾': 'warning',
    '其他垃圾': 'info'
  }
  return map[category] || ''
}

// ===== Tab 切换监听 =====
watch(activeTab, (newVal) => {
  if (newVal === 'overview') {
    nextTick(() => {
      initCharts()
    })
  }
  if (newVal === 'knowledge') {
    loadKnowledge()
  }
})

// ===== 生命周期 =====
onMounted(() => {
  if (userStore.token) {
    loadStats()
    loadUsers()
    loadRecords()
    loadLogs()
    loadFavorites()
  }
})
</script>

<style scoped>
.admin-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}
.stat-header {
  font-size: 16px;
  color: #666;
}
.stat-number {
  font-size: 36px;
  font-weight: bold;
  color: #333;
  text-align: center;
  padding: 20px 0;
}
.filter-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
  flex-wrap: wrap;
  gap: 10px;
}
.filter-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.filter-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.user-filter-area {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}
.log-filter-area {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
}
</style>