# Frontend

This folder contains the complete browser-facing layer:

- `templates/` — server-rendered Thymeleaf pages and layout fragments.
- `public/` — CSS, JavaScript modules, local fonts, icons, and optimized media.

The Spring Boot build in `../backend` packages these files into the executable application. For production, Vercel acts as the public frontend/reverse-proxy URL while the Java backend runs on a Java-capable container host. This preserves same-origin sessions and CSRF behavior.
