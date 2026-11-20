package com.aliya.reviewservice.company;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

// Same pattern as job-service's CompanyClient (see DECISIONS.md ADR-010 in the main app):
// review-service has no reason to own company data, and company-service already exists and is
// reachable, so this replaces what used to be a local CompanyRepository.existsById(...) call
// with a real HTTP call to the service that actually owns that table.
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
