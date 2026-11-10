rootProject.name = "tmc"

include(
    // shared platform libraries (no business logic, see docs/architecture/module-map.md)
    ":platform:common-kernel",
    ":platform:messaging",
    ":platform:security",
    ":platform:test-support",
    // services
    ":services:gate-station",
    ":services:attendance",
    ":services:user-qr",
    ":services:identity",
    ":services:reporting",
    ":services:hr-integration",
    ":services:audit-admin",
    // simulators
    ":simulators:kiosk-simulator",
    ":simulators:device-simulator",
    ":simulators:turnstile-simulator",
)
