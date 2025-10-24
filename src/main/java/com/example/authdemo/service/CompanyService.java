package com.example.authdemo.service;

import com.example.authdemo.model.Company;
import com.example.authdemo.repository.CompanyRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CompanyService{

    @Autowired
    private CompanyRepository companyRepository;


    // Registrace - OPRAVENÁ VERZE
    public boolean registerCompany(Company company) {
        // Kontrola IČO
        Optional<Company> existingCompanyByIco = companyRepository.findByIco(company.getIco());
        if (existingCompanyByIco.isPresent()) {
            System.out.println("IČO již existuje: " + company.getIco());
            return false;
        }

        companyRepository.save(company);
        return true;
    }


    public Optional<Company> findByIco(String ico) {
        return companyRepository.findByIco(ico);
    }

    public void deleteCompanyByUserId(Long userId) {
        try {
            Optional<Company> company = companyRepository.findByAdmin(userId);

            if (company.isPresent()) {
                companyRepository.delete(company.get());
                System.out.println("Company smazána pro admin userId: " + userId);
            }
        } catch (Exception e) {
            System.err.println("Chyba při mazání company: " + e.getMessage());
        }
    }
}