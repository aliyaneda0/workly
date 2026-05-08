package com.aliya.fetchjobapp.company;

import java.util.List;

public interface CompanyService {

    List<CompanyDTO>findAll();

    CompanyDTO findById(Long id);

    CompanyDTO save(CompanyDTO companyDTO);

    CompanyDTO update(Long id, CompanyDTO companyDTO);

    boolean deleteById(Long id);
}
