<template>
  <router-view />
</template>

<style>
:root {
  --brand: #1f7a6f;
  --brand-2: #2a9d8f;
  --accent: #f4a261;
  --ink: #1f2937;
  --muted: #6b7280;
  --page-bg: #f6f3ee;
  --soft-bg: #f1f6f9;
  --paper: #ffffff;
  --shadow-1: 0 10px 30px rgba(0, 0, 0, 0.08);
  --shadow-2: 0 8px 24px rgba(31, 122, 111, 0.18);
  --radius-lg: 16px;
  --radius-md: 12px;
  --radius-sm: 10px;
  --el-color-primary: #1f7a6f;
  --el-color-success: #2d9c5b;
  --el-color-warning: #f2994a;
  --el-color-danger: #eb5757;
  --el-font-family: 'Noto Sans SC', 'Space Grotesk', sans-serif;
}

* {
  box-sizing: border-box;
}

body, html, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: 'Noto Sans SC', 'Space Grotesk', sans-serif;
  color: var(--ink);
  background: var(--page-bg);
}
</style>
