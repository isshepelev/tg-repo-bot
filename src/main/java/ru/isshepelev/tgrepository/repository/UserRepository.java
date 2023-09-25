package ru.isshepelev.tgrepository.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.isshepelev.tgrepository.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
}
