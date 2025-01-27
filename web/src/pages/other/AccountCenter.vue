<script lang="ts" setup>
import { UserRole } from '@/data/api/api_user';
import { useSettingStore } from '@/data/stores/setting';
import { useUserDataStore } from '@/data/stores/user_data';

const setting = useSettingStore();
const userData = useUserDataStore();

const roleToReadableText = (role: UserRole) => {
  if (role === 'normal') return '普通用户';
  else if (role === 'trusted') return '信任用户';
  else if (role === 'maintainer') return '维护者';
  else if (role === 'admin') return '管理员';
  else if (role === 'banned') return '封禁用户';
  else return '未知';
};

const themeOptions = [
  { label: '亮色主题', value: false },
  { label: '暗色主题', value: true },
];
</script>

<template>
  <div class="layout-content">
    <template v-if="userData.isLoggedIn">
      <n-h1>
        @{{ userData.username }}
        <n-tag :bordered="false" size="small" style="margin-left: 4px">
          {{ roleToReadableText(userData.role!!) }}
        </n-tag>
      </n-h1>

      <n-p>
        <n-text depth="3" style="font-size: 12px">
          只有一个主题选项太奇怪了？因为我还没想好放什么
        </n-text>
      </n-p>

      <n-list bordered>
        <n-list-item>
          <advance-option title="主题">
            <n-radio-group v-model:value="setting.isDark" size="small">
              <n-radio-button
                v-for="option in themeOptions"
                :key="option.label"
                :value="option.value"
                :label="option.label"
              />
            </n-radio-group>
          </advance-option>
        </n-list-item>

        <n-list-item v-if="userData.isAdmin">
          <advance-option title="控制台">
            <n-flex>
              <c-button
                label="控制台"
                size="small"
                tag="a"
                href="/admin/user"
              />
              <c-button
                :label="`管理员模式-${userData.asAdmin}`"
                size="small"
                @click="userData.toggleAdminMode()"
              />
            </n-flex>
          </advance-option>
        </n-list-item>
      </n-list>
    </template>
    <n-result v-else status="error" title="未登录" />
  </div>
</template>
