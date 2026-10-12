

<template>


  <el-row :gutter="20">
    <el-col  :span="span" :class="[span != '15' ? 'span1' : 'span1']">

      <el-table :data="tableData" stripe style="width: 100%">
        <el-table-column prop="bookingNumber" label="#"   />
        <el-table-column prop="name" label="Name" />
        <el-table-column prop="date" label="Date" />
        <el-table-column prop="from" label="From" />
        <el-table-column prop="to" label="To" />
        <el-table-column prop="bookingStatus" label="Status" >
          <template #default="scope">
            {{ scope.row.bookingStatus === "CONFIRMED" ? "✅" : "❌"}}
          </template>
        </el-table-column>
        <el-table-column prop="bookingClass" label="Booking class" />
        <el-table-column label="Operations" fixed="right" width="180" >
          <template #default="scope">
            <el-button size="small"
                       type="primary">
              更改预定
            </el-button>
            <el-button
                size="small"
                type="danger">
              退订
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="position: relative; width: 100%; height: 100%">
        <el-drawer
            class="drawerClass"
            style="position: absolute"
            :append-to-body="false"
            :modal="false"
            :show-close="false"
            :wrapperClosable="false"
            size="320px"
            v-model="drawer"
            direction="ltr"
        >
          <el-card class="box-card" style="position: relative">
            <template #header>
              <b>知识库管理</b>
            </template>

            <!-- 统计 -->
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="分块数">{{ kbStats.chunkCount }}</el-descriptions-item>
              <el-descriptions-item label="总字符">{{ kbStats.totalChars }}</el-descriptions-item>
            </el-descriptions>

            <!-- 检索测试 -->
            <div style="margin: 10px 0; display: flex; gap: 6px">
              <el-input v-model="kbQuery" placeholder="检索测试，如：行李超重" clearable size="small"
                        @keydown.enter="kbSearch" />
              <el-button size="small" type="primary" @click="kbSearch">检索</el-button>
              <el-button size="small" @click="kbRefresh">刷新</el-button>
            </div>

            <!-- 命中结果 -->
            <el-collapse v-if="kbResults.length" style="margin-bottom: 10px">
              <el-collapse-item v-for="(r, i) in kbResults" :key="i" :title="'命中 ' + (i + 1) + ' · ' + r.id">
                <span style="white-space: pre-wrap; font-size: 13px">{{ r.content }}</span>
              </el-collapse-item>
            </el-collapse>

            <!-- 新增知识 -->
            <el-divider content-position="left">新增知识</el-divider>
            <el-input v-model="kbNewContent" type="textarea" :rows="3"
                      placeholder="输入新规则，如：宠物托运需提前 48 小时申请" />
            <el-button size="small" type="success" style="margin-top: 6px" @click="kbAdd">提交入库</el-button>
          </el-card>
        </el-drawer>
        <div
            style="
              position: absolute;
              z-index: 999999999;
              cursor: pointer;
              top: 30%;
            "
            :class="[drawer ? 'imgright' : 'imgright1']"
            @click="clickImg"
        >
          <img
              v-show="!drawer"
              style="height: 40px; width: 25px"
              src="./assets/zhankai.png"
              alt=""
          />
          <img
              v-show="drawer"
              style="height: 40px; width: 25px"
              src="./assets/shousuo.png"
              alt=""
          />
        </div>
      </div>
    </el-col>

    <el-col  :span="span1" :class="[span1 != '8' ? 'span1' : 'span1']" style="background-color: aliceblue;">
      <div style="height: 500px;overflow: scroll">
        <el-timeline style="max-width: 100%">
          <el-timeline-item
              v-for="(activity, index) in activities"
              :key="index"
              :icon="activity.icon"
              :type="activity.type"
              :color="activity.color"
              :size="activity.size"
              :hollow="activity.hollow"
              :timestamp="activity.timestamp"
          >
            <span v-html="renderMarkdown(activity.content)"></span>
          </el-timeline-item>
        </el-timeline>
      </div>
      <div id="container">
        <div id="chat">
          <el-input
              v-model="msg"
              input-style="width: 100%;height:50px"
              :rows="2"
              type="text"
              placeholder="Please input"
              @keydown.enter="sendMsg();"
          />
          <el-button  @click="sendMsg()">发送</el-button>
        </div>
      </div>
    </el-col>
  </el-row>

</template>
<script lang="ts">
import { MoreFilled } from '@element-plus/icons-vue'
import {ref, onMounted} from "vue";
import axios from 'axios'//引入axios
import { marked } from 'marked'//引入markdown解析库

export default {
  data() {
    return {
      span: 15,
      span1: 8,
      drawer: true,
    };
  },
  methods: {
    clickImg() {
      this.drawer = !this.drawer;
      if (this.drawer) {
        this.span = 15;
        this.span1 = 8;
      } else {
        this.span = 1;
        this.span1 = 23;
      }
    },
  },
  setup() {
    // markdown解析方法
    const renderMarkdown = (content) => {
      return marked.parse(content);
    };

    const activities = ref([
      {
        content: '⭐欢迎来到图灵航空✈！请问有什么可以帮您的?',
        timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
        color: '#0bbd87',
      },
    ]);
    const msg = ref('');
    const tableData = ref([]);
    let count = 2;
    let eventSource;

    const sendMsg = () => {
      if (eventSource) {
        eventSource.close();
      }

      activities.value.push(
          {
            content: `你:${msg.value}`,
            timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
            size: 'large',
            type: 'primary',
            icon: MoreFilled,
          },
      );

      activities.value.push(
          {
            content: 'waiting...',
            timestamp: new Date().toLocaleDateString() + " " + new Date().toLocaleTimeString(),
            color: '#0bbd87',
          },
      );

      // sse: 服务端推送 Server-Sent Events
      eventSource = new EventSource(`http://localhost:8080/ai/generateStreamAsString?message=${msg.value}&userId=${getUserId()}`);

      msg.value='';
      eventSource.onmessage = (event) => {
        if (event.data === '[complete]') {
          count = count + 2;
          eventSource.close();
          getBookings();  // 每次对话完后刷新列表
          return;
        }
        activities.value[count].content += event.data;
      };
      eventSource.onopen = (event) => {
        activities.value[count].content = '';
      };
    };

    const getBookings = () => {
      axios.get('http://localhost:8080/booking/list')
          .then((response) => {
            tableData.value = response.data;
          })
          .catch((error) => {
            console.error(error);
          });
    };

    const kbStats = ref({ chunkCount: 0, totalChars: 0 });
    const kbQuery = ref('');
    const kbResults = ref([]);
    const kbNewContent = ref('');

// 加载统计
    const loadKbStats = () => {
      axios.get('http://localhost:8080/kb/stats')
          .then(r => { kbStats.value = r.data; })
          .catch(e => console.error(e));
    };

// 检索测试
    const kbSearch = () => {
      axios.get('http://localhost:8080/kb/search', { params: { q: kbQuery.value, topK: 3 } })
          .then(r => { kbResults.value = r.data; })
          .catch(e => console.error(e));
    };

// 新增知识
    const kbAdd = () => {
      if (!kbNewContent.value.trim()) return;
      axios.post('http://localhost:8080/kb/add', { content: kbNewContent.value })
          .then(() => { kbNewContent.value = ''; loadKbStats(); })
          .catch(e => console.error(e));
    };

// 刷新（重新统计）
    const kbRefresh = () => loadKbStats();

// onMounted 里追加 loadKbStats()
    onMounted(() => {
      getBookings();
      loadKbStats();
    });

// return 里追加
    return {
      activities, msg, tableData, sendMsg, getBookings, renderMarkdown,
      kbStats, kbQuery, kbResults, kbNewContent, kbSearch, kbAdd, kbRefresh,
    };
  },
};
function getUserId() {
  let uid = localStorage.getItem('chat_user_id');
  if (!uid) {
    uid = 'u_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8);
    localStorage.setItem('chat_user_id', uid);
  }
  return uid;

}

</script>
<style scoped>
  * {
    margin: 0;
    padding: 0;
  }
  #chat button{
  position: absolute;
  margin-left: -60px;
  margin-top: 19px;
  }
  .span1 {
    transition: all 0.2s;
  }
  .imgright {
    right: 0;
    background-color: #f5f5f5;
    transition: all 0.2s;
  }
  .imgright1 {
    left: 0;
    background-color: #fff;
    transition: all 0.2s;
  }
  .drawerClass ::v-deep .el-drawer__container .el-drawer .el-drawer__header {
    display: none;
  }

  /* markdown 基础样式 */
  :deep(.el-timeline-item__content) {
    line-height: 1.6;
  }
  :deep(.el-timeline-item__content h1),
  :deep(.el-timeline-item__content h2),
  :deep(.el-timeline-item__content h3),
  :deep(.el-timeline-item__content h4),
  :deep(.el-timeline-item__content h5),
  :deep(.el-timeline-item__content h6) {
    margin: 12px 0 8px 0;
    font-weight: 600;
  }
  :deep(.el-timeline-item__content h1) { font-size: 1.5em; }
  :deep(.el-timeline-item__content h2) { font-size: 1.3em; }
  :deep(.el-timeline-item__content h3) { font-size: 1.2em; }
  :deep(.el-timeline-item__content p) {
    margin: 8px 0;
  }
  :deep(.el-timeline-item__content code) {
    background-color: #f5f5f5;
    padding: 2px 6px;
    border-radius: 4px;
    font-family: 'Courier New', monospace;
    font-size: 0.9em;
    color: #e6a23c;
  }
  :deep(.el-timeline-item__content pre) {
    background-color: #f5f5f5;
    padding: 12px;
    border-radius: 6px;
    overflow-x: auto;
    margin: 8px 0;
  }
  :deep(.el-timeline-item__content pre code) {
    background: none;
    padding: 0;
    color: #333;
  }
  :deep(.el-timeline-item__content ul),
  :deep(.el-timeline-item__content ol) {
    padding-left: 24px;
    margin: 8px 0;
  }
  :deep(.el-timeline-item__content li) {
    margin: 4px 0;
  }
  :deep(.el-timeline-item__content blockquote) {
    border-left: 4px solid #409eff;
    padding-left: 12px;
    margin: 8px 0;
    color: #666;
    background-color: #f0f9ff;
    padding: 8px 12px;
    border-radius: 0 4px 4px 0;
  }
  :deep(.el-timeline-item__content a) {
    color: #409eff;
    text-decoration: none;
  }
  :deep(.el-timeline-item__content a:hover) {
    text-decoration: underline;
  }
  :deep(.el-timeline-item__content table) {
    border-collapse: collapse;
    width: 100%;
    margin: 8px 0;
  }
  :deep(.el-timeline-item__content th),
  :deep(.el-timeline-item__content td) {
    border: 1px solid #ddd;
    padding: 8px 12px;
    text-align: left;
  }
  :deep(.el-timeline-item__content th) {
    background-color: #f5f5f5;
    font-weight: 600;
  }
  :deep(.el-timeline-item__content hr) {
    border: none;
    border-top: 1px solid #ddd;
    margin: 16px 0;
  }
  :deep(.el-timeline-item__content strong) {
    font-weight: 600;
  }
  :deep(.el-timeline-item__content em) {
    font-style: italic;
  }
</style>
