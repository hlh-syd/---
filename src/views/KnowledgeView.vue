<template>
  <div class="knowledge-layout">
    <SectionCard eyebrow="Folders" title="增加 / 删除 / 命名 知识库文件夹" subtitle="用系统页侧栏节奏管理文件夹层级。">
      <template #actions>
        <GlassButton variant="primary" @click="createFolder">
          <template #icon>
            <Plus style="width: 16px; height: 16px" />
          </template>
          新建文件夹
        </GlassButton>
      </template>

      <div class="folder-list">
        <button
          v-for="folder in store.knowledgeFolders"
          :key="folder.id"
          class="folder-list__item"
          :class="{ 'folder-list__item--active': store.activeFolderId === folder.id }"
          @click="store.activeFolderId = folder.id"
        >
          <div>
            <strong>{{ folder.name }}</strong>
            <p>{{ folder.files.length }} 份文件</p>
          </div>
          <div class="folder-list__actions">
            <button @click.stop="renameFolder(folder.id, folder.name)">命名</button>
            <button @click.stop="removeFolder(folder.id)">删除</button>
          </div>
        </button>
      </div>
    </SectionCard>

    <div class="responsive-stack">
      <SectionCard eyebrow="Files" title="知识库下上传 / 删除 / 批量删除" subtitle="使用任务页的强结构列表节奏管理文件。">
        <template #actions>
          <div class="knowledge-files__toolbar">
            <input ref="fileInput" hidden multiple type="file" @change="onPickFiles" />
            <GlassButton variant="ghost" @click="triggerUpload">
              <template #icon>
                <Upload style="width: 16px; height: 16px" />
              </template>
              上传
            </GlassButton>
            <GlassButton variant="default" @click="store.deleteSelectedFiles()">
              <template #icon>
                <Delete style="width: 16px; height: 16px" />
              </template>
              批量删除
            </GlassButton>
          </div>
        </template>

        <div class="knowledge-files__list">
          <article v-for="file in store.activeFolder.files" :key="file.id" class="knowledge-files__item">
            <label class="knowledge-files__check">
              <input :checked="file.selected" type="checkbox" @change="store.toggleFileSelection(file.id)" />
              <span></span>
            </label>
            <div class="knowledge-files__body">
              <strong>{{ file.name }}</strong>
              <p>{{ file.summary }}</p>
              <div class="knowledge-files__meta">{{ file.size }} · {{ file.updatedAt }}</div>
            </div>
            <button class="knowledge-files__delete" @click="store.deleteFile(file.id)">删除</button>
          </article>
        </div>
      </SectionCard>

      <div class="page-grid grid-2">
        <SectionCard eyebrow="Knowledge Search" title="知识库搜索" subtitle="对当前知识库做关键词检索。">
          <el-input v-model="store.knowledgeSearchKeyword" class="app-input" placeholder="输入关键词，例如 citation / 单细胞 / 综述" />
          <div class="knowledge-search__result">
            <span class="glass-chip">命中文档：{{ store.activeFolder.files[0]?.name ?? '暂无' }}</span>
            <span class="glass-chip">当前关键词：{{ store.knowledgeSearchKeyword }}</span>
          </div>
        </SectionCard>

        <SectionCard eyebrow="Knowledge Summary" title="知识库总结" subtitle="输出当前知识库的整体摘要。">
          <p class="knowledge-summary__text">{{ store.knowledgeSummary }}</p>
        </SectionCard>
      </div>
    </div>

    <SectionCard eyebrow="Q&A" title="基于知识库询问" subtitle="用液态玻璃聊天区风格承接知识库问答。">
      <div class="knowledge-chat">
        <div class="knowledge-chat__bubble knowledge-chat__bubble--user">
          {{ store.knowledgeQuestion }}
        </div>
        <div class="knowledge-chat__bubble knowledge-chat__bubble--assistant">
          当前文件夹更强调“检索增强 + 引文可信”，建议在回答里始终保留来源段落和证据切片。
        </div>
        <el-input v-model="store.knowledgeQuestion" class="app-input" placeholder="继续基于知识库发问" />
      </div>
    </SectionCard>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Delete, Plus, Upload } from '@element-plus/icons-vue';

import GlassButton from '@/components/ui/GlassButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import { useWorkspaceStore } from '@/stores/workspace';

const store = useWorkspaceStore();
const fileInput = ref<HTMLInputElement>();

const createFolder = () => {
  const name = window.prompt('输入新文件夹名称');
  if (name) {
    store.addFolder(name);
  }
};

const renameFolder = (folderId: string, currentName: string) => {
  const name = window.prompt('输入新的文件夹名称', currentName);
  if (name) {
    store.renameFolder(folderId, name);
  }
};

const removeFolder = (folderId: string) => {
  store.removeFolder(folderId);
};

const triggerUpload = () => {
  fileInput.value?.click();
};

const onPickFiles = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []).map((file) => file.name);
  store.addFilesToActiveFolder(files);
  input.value = '';
};
</script>

<style scoped>
.knowledge-layout {
  display: grid;
  grid-template-columns: 320px minmax(0, 1.1fr) minmax(0, 0.88fr);
  gap: 20px;
}

.folder-list {
  display: grid;
  gap: 12px;
}

.folder-list__item {
  display: grid;
  gap: 12px;
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(202, 211, 228, 0.7);
  background: rgba(248, 250, 255, 0.85);
  text-align: left;
  cursor: pointer;
}

.folder-list__item--active {
  background: linear-gradient(180deg, rgba(66, 123, 255, 0.12), rgba(255, 255, 255, 0.9));
  border-color: rgba(130, 160, 255, 0.4);
}

.folder-list__item p {
  margin: 8px 0 0;
  color: var(--text-faint);
}

.folder-list__actions {
  display: flex;
  gap: 14px;
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
}

.knowledge-files__toolbar {
  display: flex;
  gap: 12px;
}

.knowledge-files__list {
  display: grid;
  gap: 14px;
}

.knowledge-files__item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: flex-start;
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(203, 212, 228, 0.72);
  background: rgba(248, 250, 255, 0.84);
}

.knowledge-files__check {
  position: relative;
  display: inline-flex;
  width: 24px;
  height: 24px;
}

.knowledge-files__check input {
  position: absolute;
  opacity: 0;
}

.knowledge-files__check span {
  width: 24px;
  height: 24px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: inset 0 0 0 1px rgba(178, 189, 211, 0.6);
}

.knowledge-files__check input:checked + span {
  background: linear-gradient(180deg, rgba(66, 123, 255, 0.88), rgba(49, 109, 255, 1));
  box-shadow: none;
}

.knowledge-files__body strong {
  display: block;
  font-size: 18px;
}

.knowledge-files__body p {
  margin: 10px 0 0;
  color: var(--text-soft);
  line-height: 1.7;
}

.knowledge-files__meta {
  margin-top: 12px;
  color: var(--text-faint);
  font-size: 13px;
}

.knowledge-files__delete {
  color: #c03a60;
  font-weight: 700;
  cursor: pointer;
}

.knowledge-search__result {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 16px;
}

.knowledge-summary__text {
  margin: 0;
  color: var(--text-soft);
  line-height: 1.85;
}

.knowledge-chat {
  display: grid;
  gap: 12px;
}

.knowledge-chat__bubble {
  padding: 18px 20px;
  border-radius: 26px;
  line-height: 1.75;
}

.knowledge-chat__bubble--user {
  margin-left: auto;
  max-width: 76%;
  background: rgba(66, 123, 255, 0.12);
  border: 1px solid rgba(130, 160, 255, 0.32);
}

.knowledge-chat__bubble--assistant {
  max-width: 88%;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(242, 247, 255, 0.76)),
    rgba(255, 255, 255, 0.35);
  border: 1px solid rgba(210, 217, 232, 0.78);
  backdrop-filter: blur(18px);
}

@media (max-width: 1280px) {
  .knowledge-layout {
    grid-template-columns: 1fr;
  }
}
</style>
