package com.cybershield.repository;

import com.cybershield.entity.User;
import com.cybershield.enums.RoleCode;
import com.cybershield.enums.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u join fetch u.role left join fetch u.department "
            + "where lower(u.username) = lower(:username)")
    Optional<User> findByUsernameWithRole(@Param("username") String username);

    @Query("select u from User u join fetch u.role left join fetch u.department where u.id = :id")
    Optional<User> findDetailById(@Param("id") Long id);

    Optional<User> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    @Query("select u from User u join fetch u.role where u.role.code = :code and u.status = :status order by u.fullName")
    List<User> findByRoleAndStatus(@Param("code") RoleCode code, @Param("status") UserStatus status);

    @Query("select u from User u join fetch u.role where u.role.code in :codes and u.status = :status order by u.fullName")
    List<User> findByRolesAndStatus(@Param("codes") List<RoleCode> codes, @Param("status") UserStatus status);

    @Query("select u from User u join fetch u.role where u.status = :status order by u.fullName")
    List<User> findByStatusWithRole(@Param("status") UserStatus status);

    @Query("select count(u) from User u where u.role.code = :code and u.status = :status")
    long countByRoleAndStatus(@Param("code") RoleCode code, @Param("status") UserStatus status);

    long countByStatus(UserStatus status);

    @Query(value = "select u from User u join fetch u.role left join fetch u.department "
            + "where (:q is null or lower(u.fullName) like lower(concat('%', :q, '%')) "
            + "   or lower(u.username) like lower(concat('%', :q, '%')) or lower(u.email) like lower(concat('%', :q, '%'))) "
            + "and (:role is null or u.role.code = :role) and (:status is null or u.status = :status)",
            countQuery = "select count(u) from User u "
            + "where (:q is null or lower(u.fullName) like lower(concat('%', :q, '%')) "
            + "   or lower(u.username) like lower(concat('%', :q, '%')) or lower(u.email) like lower(concat('%', :q, '%'))) "
            + "and (:role is null or u.role.code = :role) and (:status is null or u.status = :status)")
    Page<User> search(@Param("q") String q, @Param("role") RoleCode role, @Param("status") UserStatus status,
                      Pageable pageable);

    /** Users per role (JOIN + GROUP BY) for the manager dashboard. Row: [RoleCode, count]. */
    @Query("select u.role.code, count(u) from User u where u.status = com.cybershield.enums.UserStatus.ACTIVE "
            + "group by u.role.code")
    List<Object[]> countActiveByRole();
}
