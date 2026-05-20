// Merge instead of replacing — webpack production configs populate
// `resolve.alias` / `resolve.extensions` upstream, and clobbering them
// breaks module resolution.
config.resolve = {
    ...(config.resolve || {}),
    fallback: {
        ...((config.resolve && config.resolve.fallback) || {}),
        fs: false,
        path: false,
        crypto: false,
    }
};

const CopyWebpackPlugin = require('copy-webpack-plugin');
config.plugins.push(
    new CopyWebpackPlugin({
        patterns: [
            '../../node_modules/sql.js/dist/sql-wasm.wasm'
        ]
    })
);
