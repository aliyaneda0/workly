package com.aliya.jobservice.company;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

// job-service has no reason to own company data, and company-service already exists and is
// reachable (see DECISIONS.md ADR-009 in the main app) — so this replaces what used to be a
// local CompanyRepository.existsById(...) call with a real HTTP call to the service that
// actually owns that table. This is the project's first genuine inter-service call: plain
// RestTemplate + a configurable base URL (ROADMAP.md's "start with RestTemplate... then
// refactor to OpenFeign" plan), not yet Eureka-discovered — that's still a later Phase 3 step,
// and until then the base URL is just an env var.
//
// The traditional alternative here is for job-service to keep its own local copy/cache of
// company ids (a denormalized reference table, kept in sync some other way). Rejected: it
// reintroduces exactly the duplicated-data problem "database per service" exists to avoid, and
// company-service is already proven reachable (ADR-009) — there's no reason not to just ask it.
@Component
public class CompanyClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public CompanyClient(RestTemplate restTemplate,
                          @Value("${app.company-service.base-url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public boolean existsById(Long companyId) {
        try {
            restTemplate.getForEntity(baseUrl + "/companies/{id}", Void.class, companyId);
            return true;
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        }
    }
}
