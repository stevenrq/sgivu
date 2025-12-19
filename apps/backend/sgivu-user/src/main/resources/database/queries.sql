SELECT u.username,
       p.first_name,
       p.last_name,
       STRING_AGG(DISTINCT r.name, ', ')    AS roles,
       STRING_AGG(DISTINCT perm.name, ', ') AS permissions
FROM users u
         JOIN
     persons p ON u.person_id = p.id
         JOIN
     users_roles ur ON u.person_id = ur.user_id
         JOIN
     roles r ON ur.role_id = r.id
         LEFT JOIN
     roles_permissions rp ON r.id = rp.role_id
         LEFT JOIN
     permissions perm ON rp.permission_id = perm.id
GROUP BY u.username, p.first_name, p.last_name
ORDER BY u.username;