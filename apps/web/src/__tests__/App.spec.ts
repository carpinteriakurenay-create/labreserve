import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import { createMemoryHistory, createRouter } from "vue-router";
import App from "@/App.vue";
import Home from "@/views/Home.vue";

const router = createRouter({
  history: createMemoryHistory(),
  routes: [{ path: "/", name: "home", component: Home }],
});

describe("App.vue", () => {
  it("mounts and renders router-view", async () => {
    await router.push("/");
    await router.isReady();
    const wrapper = mount(App, {
      global: { plugins: [router] },
    });
    expect(wrapper.exists()).toBe(true);
  });
});
