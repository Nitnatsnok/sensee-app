// SPA history fallback: Decompose Web Navigation puts the active route in the
// URL path (e.g. /practice). A page refresh or a direct deep link must still
// serve index.html instead of 404-ing on the dev server. Merge, don't replace —
// other configs (sqljs) populate adjacent fields.
config.devServer = {
    ...(config.devServer || {}),
    historyApiFallback: true,
};
