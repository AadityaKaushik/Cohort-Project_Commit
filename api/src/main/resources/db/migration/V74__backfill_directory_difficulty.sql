UPDATE company_role_patterns
SET difficulty_level = 2,
    difficulty_basis = 'The source describes the technical questions as easy to moderate.'
WHERE company_id = (SELECT id FROM company_profiles WHERE slug = 'cognizant');

UPDATE company_role_patterns
SET difficulty_level = 2,
    difficulty_basis = 'The source describes the online test as two to three easy to medium problems.'
WHERE company_id = (SELECT id FROM company_profiles WHERE slug = 'hexaware');
