import { createApp } from 'vue';
import { createPinia } from 'pinia';
import { ElInput, ElOption, ElProgress, ElSelect } from 'element-plus';

import App from './App.vue';
import router from './router';

import 'element-plus/es/components/input/style/css';
import 'element-plus/es/components/option/style/css';
import 'element-plus/es/components/progress/style/css';
import 'element-plus/es/components/select/style/css';
import './styles/base.css';
import './styles/theme.css';

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.component('ElInput', ElInput);
app.component('ElSelect', ElSelect);
app.component('ElOption', ElOption);
app.component('ElProgress', ElProgress);

app.mount('#app');
