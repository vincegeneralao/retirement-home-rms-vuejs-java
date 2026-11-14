package com.rms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rms.entity.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findAllByOrderByIdAsc();
}
