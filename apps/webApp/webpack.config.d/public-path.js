// `auto` resolves chunk / worker / wasm URLs from the entry bundle's runtime
// URL instead of the document root, so the app works both at site root (dev
// server) and under a GH Pages subpath (e.g. /sensee-app/).
config.output = {
    ...(config.output || {}),
    publicPath: 'auto',
};
