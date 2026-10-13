package com.cohort.api.repository;

import com.cohort.api.entity.Contact;
import java.util.List;
import java.util.UUID;

public interface ContactRepository extends UserScopedRepository<Contact> {

    List<Contact> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Contact> findAllByUserIdAndCompanyIdOrderByCreatedAtDesc(UUID userId, UUID companyId);

    List<Contact> findAllByUserIdAndApplicationIdOrderByCreatedAtDesc(UUID userId, UUID applicationId);
}
