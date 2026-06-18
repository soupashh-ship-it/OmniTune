export default {
  use: {
    headless: true,
    viewport: { width: 1920, height: 1080 },
    video: 'on',
  },
  projects: [
    {
      name: 'chromium',
      use: { browserName: 'chromium' },
    },
  ],
};
