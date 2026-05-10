package com.aliya.fetchjobapp.company;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;




@Service
public class CompanyServiceImpl implements CompanyService{

    private final CompanyRepository companyRepository;

    public CompanyServiceImpl(CompanyRepository companyRepository) {

        this.companyRepository = companyRepository;
    }


    @Override
    public List<CompanyDTO> findAll() {

        return companyRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());

    }



    @Override
    public CompanyDTO findById(Long id) {
        return companyRepository.findById(id)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    public CompanyDTO save(CompanyDTO companyDTO) {

        Company company = toEntity(companyDTO);
        Company saved = companyRepository.save(company);
        return toDTO(saved);
        
    }

    @Override
    public CompanyDTO update(Long id, CompanyDTO companyDTO) {
        Optional<Company> optionalCompany = companyRepository.findById(id);

        if(optionalCompany.isPresent()){
            Company company = optionalCompany.get();
            company.setCompanyName(companyDTO.getCompanyName());
            company.setDescription(companyDTO.getDescription());
            company.setLocation(companyDTO.getLocation());
            Company updated = companyRepository.save(company);

            return toDTO(updated);
        }

        return null;
    }

    @Override
    public boolean deleteById(Long id) {

        if(companyRepository.existsById(id)){

            companyRepository.deleteById(id);
            return true;
        }

        return false;
    }

    private CompanyDTO toDTO(Company company) {

        return new CompanyDTO(
                company.getId(),
                company.getCompanyName(),
                company.getDescription(),
                company.getLocation()

        );
    }

    private Company toEntity(CompanyDTO dto){

        Company company = new Company();

        company.setCompanyName(dto.getCompanyName());
        company.setDescription(dto.getDescription());
        company.setLocation(dto.getLocation());
        return company;
    }
}
