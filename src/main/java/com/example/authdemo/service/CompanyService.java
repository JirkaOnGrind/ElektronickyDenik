package com.example.authdemo.service;

import com.example.authdemo.model.Company;
import com.example.authdemo.model.User;
import com.example.authdemo.repository.CompanyRepository;
import com.example.authdemo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    public boolean registerCompany(Company company) {
        Optional<Company> existingCompanyByIco = companyRepository.findByIco(company.getIco());
        if (existingCompanyByIco.isPresent()) {
            return false;
        }
        companyRepository.save(company);
        return true;
    }

    public Optional<Company> findByIco(String ico) {
        return companyRepository.findByIco(ico);
    }

    public Optional<Company> findByAdminId(Long adminId) {
        return companyRepository.findByAdmin(adminId);
    }

    // --- NOVÁ METODA PRO NAČTENÍ PODLE KLÍČE ---
    public Optional<Company> findByKey(String key) {
        return companyRepository.findByKey(key);
    }
    // -------------------------------------------

    public void deleteCompanyByUserId(Long userId) {
        try {
            Optional<Company> company = companyRepository.findByAdmin(userId);
            if (company.isPresent()) {
                companyRepository.delete(company.get());
            }
        } catch (Exception e) {
            System.err.println("Chyba při mazání company: " + e.getMessage());
        }
    }

    @Transactional
    public String updateCompanyKey(Long adminId, String newKey) {
        Optional<Company> companyOpt = companyRepository.findByAdmin(adminId);
        if (companyOpt.isEmpty()) {
            return "Firma nenalezena.";
        }
        Company company = companyOpt.get();

        if (company.getKey().equals(newKey)) {
            return "Nový klíč je shodný s aktuálním.";
        }

        if (companyRepository.existsByKey(newKey)) {
            return "Tento klíč je již používán jinou společností.";
        }

        String oldKey = company.getKey();
        company.setKey(newKey);
        companyRepository.save(company);

        List<User> users = userRepository.findByKeyAndDeletedAtIsNull(oldKey);
        for (User user : users) {
            user.setKey(newKey);
        }
        userRepository.saveAll(users);

        return "success";
    }
    @Transactional
    public void deleteFullCompany(Long companyId) {
        Optional<Company> companyOpt = companyRepository.findById(companyId);
        if (companyOpt.isPresent()) {
            Company company = companyOpt.get();
            // Smažeme všechny uživatele s klíčem dané firmy
            List<User> users = userRepository.findByKeyAndDeletedAtIsNull(company.getKey());
            userRepository.deleteAll(users);

            // Smažeme samotnou firmu
            companyRepository.delete(company);
        }
    }
}