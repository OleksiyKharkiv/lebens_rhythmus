package com.be;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * LR-031 Phase 1 — the "правильный" CI-guard the roadmap asked for, in
 * place of a point grep for a single dependency name. LR-023 wasn't just
 * "spring-boot-starter-data-rest is present" — it was that some HTTP path
 * outside the reviewed, intentional API surface was reachable at all. A
 * dependency-name grep only catches a repeat of the exact same cause; this
 * test catches the actual symptom class regardless of *how* a stray
 * mapping gets introduced (a new autoconfigured starter, a forgotten
 * class-level @RequestMapping, a copy-pasted controller, ...).
 * <p>
 * Every controller in this codebase is written under {@code /api/v1/**}
 * (verified by grepping every @RequestMapping/@GetMapping/etc. literal
 * across src/main/java as of this ticket — see docs/security/roadmap.md
 * Phase 1). Anything else that shows up in the main dispatcher's handler
 * mapping table is either a genuine, deliberate exception (the allow-list
 * below) or a regression this test exists to catch before it reaches prod.
 * <p>
 * Uses the MAIN application context's RequestMappingHandlerMapping
 * specifically. Note this does NOT double as an actuator-isolation check:
 * WebMvcEndpointHandlerMapping (actuator's own mapping class) is a sibling
 * of RequestMappingHandlerMapping, not a subtype — .getHandlerMethods()
 * here would never see actuator paths regardless of whether
 * management.server.port matches server.port or not. Actuator's isolation
 * from the public Service/Ingress is enforced separately, by
 * backend-service.yaml deliberately not exposing the management port (see
 * that file's own comment) — this test's job is the app's own
 * @RestController surface, not actuator's.
 * <p>
 * webEnvironment MOCK (the @SpringBootTest default), not NONE: NONE forces
 * WebApplicationType.NONE, which skips WebMvcAutoConfiguration entirely —
 * SecurityConfig's http.cors(...) then fails to find a CorsConfigurationSource
 * (it needs Spring MVC's HandlerMappingIntrospector). MOCK loads the full
 * web application context, including RequestMappingHandlerMapping, without
 * binding a real port — no need for RANDOM_PORT's actual server startup
 * just to read the mapping table.
 * <p>
 * architect-reviewer caught a real gap in an earlier version of this test
 * (2026-08-07): checking only the {@code requestMappingHandlerMapping}
 * bean's own path table would NOT have caught LR-023. spring-data-rest
 * registers its auto-exposed paths inside a package-private
 * {@code DelegatingHandlerMapping} bean (name {@code restHandlerMapping})
 * that wraps its own internal {@code RepositoryRestHandlerMapping}/
 * {@code BasePathAwareHandlerMapping} instances as plain {@code new}'d
 * objects — never registered with the ApplicationContext themselves, so no
 * amount of inspecting {@code RequestMappingHandlerMapping} could ever see
 * them (verified against spring-data-rest-webmvc's own source). The fix:
 * enumerate every {@code HandlerMapping} bean in the context by name and
 * assert the bean-name set is exactly the small, known, hand-verified set
 * below — any *new* HandlerMapping bean showing up at all (regardless of
 * its internal path table's shape) fails the test until explicitly
 * allow-listed here. The path-based check on the main
 * RequestMappingHandlerMapping stays too — it still catches hand-written-
 * controller drift outside /api/v1/**, a different failure mode.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ApiSurfaceAllowlistTest {

    // Spring Boot's default BasicErrorController — not app-specific
    // business logic, forwarded to internally by the servlet container on
    // an unhandled exception, never a URL a client requests directly.
    private static final Set<String> ALLOWED_EXACT_PATHS = Set.of("/error");

    // The complete set of HandlerMapping beans this app registers today —
    // not guessed, read directly off this test's own failure output the
    // first time it ran with an empty list. All framework-standard, none
    // app- or dependency-contributed beyond requestMappingHandlerMapping
    // itself:
    //   - requestMappingHandlerMapping: our own @RestController methods,
    //     checked path-by-path below.
    //   - resourceHandlerMapping / welcomePageHandlerMapping /
    //     welcomePageNotAcceptableHandlerMapping: Spring Boot's default
    //     static-resource and welcome-page infrastructure, inert here
    //     (this backend serves no static content — the SPA is a separate
    //     Nginx container per devops/helm/lr-app).
    //   - beanNameHandlerMapping / routerFunctionMapping: framework-
    //     registered unconditionally even with zero matching beans
    //     defined (legacy bean-name-based mapping, RouterFunction-style
    //     endpoints); both empty in this app.
    //   - healthEndpointWebMvcHandlerMapping: Actuator's own /actuator/
    //     health registration. Present here only because @SpringBootTest's
    //     MOCK web environment has no real embedded server to split onto
    //     management.server.port's separate child context the way the
    //     real deployed app does (see backend-deployment.yaml/values.yaml)
    //     — in production this mapping lives in a different context
    //     entirely, unreachable through the public Service/Ingress.
    //     Allow-listing it here is deliberately permissive for a bean
    //     that, if anything, is MORE isolated in the real environment
    //     than in this test — not a gap.
    // A future dependency that registers ITS OWN HandlerMapping bean
    // (spring-data-rest's restHandlerMapping being the motivating example)
    // shows up as a new, unrecognized name here and fails the test.
    private static final Set<String> ALLOWED_HANDLER_MAPPING_BEAN_NAMES = Set.of(
            "requestMappingHandlerMapping",
            "resourceHandlerMapping",
            "welcomePageHandlerMapping",
            "welcomePageNotAcceptableHandlerMapping",
            "beanNameHandlerMapping",
            "routerFunctionMapping",
            "healthEndpointWebMvcHandlerMapping");

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void everyMappedPath_isUnderApiV1_orExplicitlyAllowlisted() {
        List<String> offendingPaths = requestMappingHandlerMapping.getHandlerMethods().keySet().stream()
                .map(RequestMappingInfo::getPatternValues)
                .flatMap(Set::stream)
                .filter(path -> !path.startsWith("/api/v1"))
                .filter(path -> !ALLOWED_EXACT_PATHS.contains(path))
                .distinct()
                .toList();

        assertThat(offendingPaths)
                .as("every controller-mapped path must be under /api/v1/** or explicitly "
                        + "allow-listed in this test — an unreviewed path outside that surface "
                        + "is exactly how LR-023 (spring-data-rest auto-exposing /users, "
                        + "/participants, etc.) went undetected for ~9.5 months")
                .isEmpty();
    }

    @Test
    void noUnexpectedHandlerMappingBeanExists() {
        // Catches the actual LR-023 mechanism: a dependency contributing
        // its own HandlerMapping bean whose internal path table is
        // invisible to requestMappingHandlerMapping entirely (see class
        // javadoc). A new bean name here means a new source of HTTP
        // routing this test's other method cannot see into — surface it
        // for review rather than silently trusting it's harmless.
        Map<String, HandlerMapping> handlerMappings = applicationContext.getBeansOfType(HandlerMapping.class);

        assertThat(handlerMappings.keySet())
                .as("a new HandlerMapping bean was registered (%s) — likely a new dependency "
                        + "auto-exposing HTTP routes outside the reviewed /api/v1/** surface, the "
                        + "exact mechanism behind LR-023 (spring-data-rest's restHandlerMapping). "
                        + "Investigate what registered it before adding it to "
                        + "ALLOWED_HANDLER_MAPPING_BEAN_NAMES", handlerMappings.keySet())
                .isSubsetOf(ALLOWED_HANDLER_MAPPING_BEAN_NAMES);
    }
}
