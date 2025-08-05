// metro.config.js
const { getDefaultConfig } = require('expo/metro-config');

const config = getDefaultConfig(__dirname);

// ⚠️ ESM 'exports' 필드 무시하고 CJS 진입점 사용
config.resolver.unstable_enablePackageExports = false;

module.exports = config;
