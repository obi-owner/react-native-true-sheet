const path = require('path');
const { getDefaultConfig } = require('expo/metro-config');

const root = path.resolve(__dirname, '../..');
const pkg = require('../../package.json');

/**
 * Metro configuration
 * https://docs.expo.dev/guides/customizing-metro
 *
 * @type {Promise<import('metro-config').MetroConfig>}
 */
module.exports = (async () => {
  // react-native-monorepo-config is ESM-only; import dynamically from CJS.
  const { withMetroConfig } = await import('react-native-monorepo-config');

  const baseConfig = withMetroConfig(getDefaultConfig(__dirname), {
    root,
    dirname: __dirname,
  });

  // Extend resolver to handle subpath exports with source condition
  const originalResolveRequest = baseConfig.resolver.resolveRequest;

  baseConfig.resolver.resolveRequest = (context, moduleName, platform) => {
    // Handle subpath exports for the main package (e.g., @lodev09/react-native-true-sheet/reanimated)
    if (moduleName.startsWith(pkg.name + '/')) {
      context = {
        ...context,
        unstable_conditionNames: ['source', ...context.unstable_conditionNames],
      };
    }

    return originalResolveRequest(context, moduleName, platform);
  };

  return baseConfig;
})();
