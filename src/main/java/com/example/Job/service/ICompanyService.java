package com.example.Job.service;

import com.example.Job.entity.Company;
import com.example.Job.models.dtos.CompanyRegister;
import com.example.Job.models.dtos.CompanyDetailResponse;

public interface ICompanyService {
    Company registerCompany(CompanyRegister registerRequest);

    CompanyDetailResponse getCompanyDetailById(Long companyId);

    Company getCompanyById(Long id);


}
