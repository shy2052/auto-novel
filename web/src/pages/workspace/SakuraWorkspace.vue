<script lang="ts" setup>
import {
  DeleteOutlined,
  PlusOutlined,
  RefreshOutlined,
} from '@vicons/material';
import { useMessage } from 'naive-ui';
import { ref } from 'vue';

import { TranslateJob, useSakuraWorkspaceStore } from '@/data/stores/workspace';
import { createSegIndexedDbCache } from '@/data/translator';
import { useIsWideScreen } from '@/data/util';

import { computePercentage } from './components/util';

const message = useMessage();
const sakuraWorkspace = useSakuraWorkspaceStore();
const isWideScreen = useIsWideScreen(850);

const showCreateWorkerModal = ref(false);

type ProcessedJob = TranslateJob & {
  progress?: { finished: number; error: number; total: number };
};

const processedJobs = ref<Map<string, ProcessedJob>>(new Map());

const getNextJob = () => {
  const job = sakuraWorkspace.jobs.find(
    (it) => !processedJobs.value.has(it.task)
  );
  if (job !== undefined) {
    processedJobs.value.set(job.task, job);
  }
  return job;
};

const deleteJob = (task: string) => {
  if (processedJobs.value.has(task)) {
    message.error('任务被翻译器占用');
    return;
  }
  sakuraWorkspace.deleteJob(task);
};

const onProgressUpdated = (
  task: string,
  state:
    | { state: 'finish' }
    | { state: 'processed'; finished: number; error: number; total: number }
) => {
  if (state.state === 'finish') {
    const job = processedJobs.value.get(task)!!;
    processedJobs.value.delete(task);
    if (
      job.progress === undefined ||
      job.progress.finished < job.progress.total
    ) {
      sakuraWorkspace.addUncompletedJob(job as any);
    }
    sakuraWorkspace.deleteJob(task);
  } else {
    const job = processedJobs.value.get(task)!!;
    job.progress = {
      finished: state.finished,
      error: state.error,
      total: state.total,
    };
  }
};

const clearCache = async () => {
  const cache = await createSegIndexedDbCache('sakura-seg-cache');
  await cache.clear();
  message.success('缓存清除成功');
};
</script>

<template>
  <c-layout :sidebar="isWideScreen" :sidebar-width="320" class="layout-content">
    <n-h1>Sakura工作区</n-h1>
    <n-ul>
      <n-li>
        翻译任务运行在你的浏览器里面，关闭或者刷新本页面都会停止翻译。长时间挂机的话不要把本页面放在后台，防止被浏览器杀掉。
      </n-li>
      <n-li> 启动了的翻译器无法暂停或删除。等这句话没了就可以了。 </n-li>
      <n-li>
        Sakura部署教程参见
        <RouterNA to="/forum/656d60530286f15e3384fcf8" target="_blank">
          本地部署教程
        </RouterNA>
        和
        <RouterNA to="/forum/65719bf16843e12bd3a4dc98" target="_blank">
          租用显卡教程
        </RouterNA>
        。
      </n-li>
      <n-li>
        如果你想直接翻译网络小说/文库小说，请确保你的模型版本允许上传。当前推荐的版本：
        <n-a
          href="https://huggingface.co/SakuraLLM/Sakura-13B-LNovel-v0.9b-GGUF/blob/main/sakura-13b-lnovel-v0.9b-Q4_K_M.gguf"
          target="_blank"
        >
          v0.9b-Q4_K_M
        </n-a>
        。AWQ量化版本目前有bug，请不要使用。
      </n-li>
    </n-ul>

    <section-header title="翻译器">
      <c-button
        label="添加翻译器"
        :icon="PlusOutlined"
        @click="showCreateWorkerModal = true"
      />
      <c-button
        label="清空缓存"
        :icon="DeleteOutlined"
        async
        @click="clearCache"
      />
    </section-header>

    <n-empty
      v-if="sakuraWorkspace.workers.length === 0"
      description="没有翻译器"
    />
    <n-list>
      <n-list-item v-for="worker of sakuraWorkspace.workers" :key="worker.id">
        <sakura-worker
          :id="worker.id"
          :endpoint="worker.endpoint"
          :use-llama-api="worker.useLlamaApi ?? false"
          :get-next-job="getNextJob"
          @update:progress="onProgressUpdated"
        />
      </n-list-item>
    </n-list>

    <section-header title="任务队列" />
    <n-empty v-if="sakuraWorkspace.jobs.length === 0" description="没有任务" />
    <n-list>
      <n-list-item v-for="job of sakuraWorkspace.jobs" :key="job.task">
        <job-queue
          :job="job"
          :percentage="computePercentage(processedJobs.get(job.task)?.progress)"
          @top-job="sakuraWorkspace.topJob(job)"
          @delete-job="deleteJob(job.task)"
        />
      </n-list-item>
    </n-list>

    <section-header title="未完成任务">
      <c-button
        label="全部重试"
        :icon="RefreshOutlined"
        @click="sakuraWorkspace.retryAllUncompletedJobs()"
      />
      <c-button
        label="清空记录"
        :icon="DeleteOutlined"
        @click="sakuraWorkspace.deleteAllUncompletedJobs()"
      />
    </section-header>

    <n-empty
      v-if="sakuraWorkspace.uncompletedJobs.length === 0"
      description="没有任务"
    />
    <n-list>
      <n-list-item
        v-for="job of sakuraWorkspace.uncompletedJobs"
        :key="job.task"
      >
        <job-uncompleted
          :job="job"
          @retry-job="sakuraWorkspace.retryUncompletedJob(job)"
          @delete-job="sakuraWorkspace.deleteUncompletedJob(job)"
        />
      </n-list-item>
    </n-list>

    <template #sidebar>
      <local-volume-list type="sakura" />
    </template>
  </c-layout>

  <sakura-create-worker-modal v-model:show="showCreateWorkerModal" />
</template>
