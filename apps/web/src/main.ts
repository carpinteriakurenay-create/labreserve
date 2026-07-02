import { createApp } from "vue";
import { createPinia } from "pinia";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import { setRouter } from "./api/client";

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(ElementPlus);

// Wire router into API client for SPA-friendly 401 redirects
setRouter(router);

app.mount("#app");
