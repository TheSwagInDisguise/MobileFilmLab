# AI-development and asset disclosure

MobileFilmLab was developed **substantially with AI assistance**. AI-generated or AI-assisted work includes application code, algorithms and parameter translations, tests, build/release automation, documentation, branding exploration and presentation assets. Human direction, design decisions and initial handset feedback are part of the development process; that does not mean every generated line has been independently audited.

## What this means for testers

- This is an experimental open-source public beta, not a security-reviewed or universally validated camera product.
- Generated code, tests and documentation can contain mistakes or unsupported assumptions. Passing tests does not prove real-camera behaviour or establish image quality.
- Recipe translations approximate looks. They do not reproduce proprietary colour science or private camera pipelines.
- Please review source, report reproducible problems and keep another camera available for important or irreplaceable photographs.
- No AI service is called to process your photographs. The app's image processing is local; its only network feature is a manually requested public release check.

## Images and screenshots

The README's UI images are native Android/Compose renders of the real application components. Their camera scene is AI-generated, the camera metadata/control state is illustrative, and the scene is visibly marked `DEMO SCENE`. They are not phone screenshots, app-captured photographs or image-quality comparisons. See [screenshot provenance](SCREENSHOTS.md).

Historical [design explorations](../design/README.md) are generated proposals and mockups. They must not be presented as screenshots of the implemented app. Private handset/comparison photos are not included in public documentation.

AI assistance does not remove the project's obligations to credit upstream work or honour its licences. See [provenance](../UPSTREAM_ATTRIBUTION.md), [third-party notices](../THIRD_PARTY_NOTICES.md) and the [project licence](../LICENSE).
