<template>
  <div class="page-grid grid-2">
    <SectionCard eyebrow="Edit Profile" title="编辑资料" subtitle="上传头像、编辑资料并保存。">
      <div class="profile-editor">
        <div class="profile-editor__avatar">
          <img :src="store.profile.avatar" alt="avatar" />
          <input ref="avatarInput" hidden accept="image/*" type="file" @change="onPickAvatar" />
          <GlassButton variant="ghost" @click="avatarInput?.click()">上传头像</GlassButton>
        </div>
        <div class="profile-editor__form">
          <el-input v-model="store.profile.name" class="app-input" placeholder="姓名" />
          <el-input v-model="store.profile.title" class="app-input" placeholder="职位" />
          <el-input v-model="store.profile.institution" class="app-input" placeholder="机构" />
          <el-input v-model="store.profile.bio" :autosize="{ minRows: 4, maxRows: 8 }" class="app-input" type="textarea" placeholder="个人简介" />
          <GlassButton variant="primary">保存</GlassButton>
        </div>
      </div>
    </SectionCard>

    <div class="responsive-stack">
      <SectionCard eyebrow="Link Account" title="关联微信 / 飞书" subtitle="保留两个指定平台的关联状态。">
        <div class="profile-links">
          <article v-for="link in store.profile.links" :key="link.name" class="profile-links__item">
            <div>
              <strong>{{ link.name }}</strong>
              <p>{{ link.account }}</p>
            </div>
            <GlassButton :variant="link.linked ? 'ghost' : 'default'" @click="store.toggleExternalLink(link.name)">
              {{ link.linked ? '已关联' : '去关联' }}
            </GlassButton>
          </article>
        </div>
      </SectionCard>

      <SectionCard eyebrow="Logout" title="退出登录" subtitle="保留清晰的退出动作。">
        <GlassButton variant="primary" @click="router.push('/login')">退出登录</GlassButton>
      </SectionCard>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';

import GlassButton from '@/components/ui/GlassButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import { useWorkspaceStore } from '@/stores/workspace';

const router = useRouter();
const store = useWorkspaceStore();
const avatarInput = ref<HTMLInputElement>();

const onPickAvatar = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];

  if (!file) {
    return;
  }

  const reader = new FileReader();
  reader.onload = () => {
    if (typeof reader.result === 'string') {
      store.updateAvatar(reader.result);
    }
  };
  reader.readAsDataURL(file);
  input.value = '';
};
</script>

<style scoped>
.profile-editor {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 24px;
}

.profile-editor__avatar {
  display: grid;
  gap: 16px;
  align-content: start;
}

.profile-editor__avatar img {
  width: 220px;
  height: 220px;
  object-fit: cover;
  border-radius: 34px;
  border: 1px solid rgba(205, 213, 228, 0.72);
}

.profile-editor__form {
  display: grid;
  gap: 14px;
}

.profile-links {
  display: grid;
  gap: 14px;
}

.profile-links__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(205, 213, 228, 0.72);
  background: rgba(248, 250, 255, 0.84);
}

.profile-links__item p {
  margin: 8px 0 0;
  color: var(--text-faint);
}

@media (max-width: 900px) {
  .profile-editor {
    grid-template-columns: 1fr;
  }

  .profile-editor__avatar img {
    width: 160px;
    height: 160px;
  }

  .profile-links__item {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
