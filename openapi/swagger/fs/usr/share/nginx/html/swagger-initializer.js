// Replaces the swagger-ui image's own swagger-initializer.js.
//
// ODM's Swagger pages hide the 'Authorize' button and the per-operation lock icons: in a
// browser signed in to ODM, 'Try it out' is authenticated with the ODM session (ODM-13825).
// The specs keep their securitySchemes, so the clients openapi-generator builds from them
// (odm-api) still send API tokens.
//
// At container start the image's configurator rewrites only the marked block below, from the
// environment (URLS and the like), so the plugin cannot be listed there: SwaggerUIBundle is
// wrapped instead, before that block runs, to add it to whatever plugins the block passes.
window.onload = function() {
  const swaggerUIBundle = window.SwaggerUIBundle
  const hideAuthorization = () => ({
    wrapComponents: {
      authorizeBtn: () => () => null,
      authorizeOperationBtn: () => () => null,
    },
  })
  const SwaggerUIBundle = Object.assign(
    (config) => swaggerUIBundle({...config, plugins: [...(config.plugins || []), hideAuthorization]}),
    swaggerUIBundle,
  )
  // The bar that held the button is left empty, unless a spec declares servers to choose from.
  document.head.insertAdjacentHTML(
    'beforeend',
    '<style>.swagger-ui .scheme-container:has(> .schemes:empty) { display: none; }</style>',
  )

  //<editor-fold desc="Changeable Configuration Block">

  // the following lines will be replaced by docker/configurator, when it runs in a docker-container
  window.ui = SwaggerUIBundle({
    url: "https://petstore.swagger.io/v2/swagger.json",
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    plugins: [
      SwaggerUIBundle.plugins.DownloadUrl
    ],
    layout: "StandaloneLayout"
  });

  //</editor-fold>
};
