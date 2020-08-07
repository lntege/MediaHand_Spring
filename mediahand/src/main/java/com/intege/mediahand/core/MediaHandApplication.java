package com.intege.mediahand.core;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javafx.application.Application;

@SpringBootApplication
@EntityScan("com.intege.*")
@EnableJpaRepositories("com.intege.*")
@ComponentScan("com.intege.*")
@EnableTransactionManagement
public class MediaHandApplication {

	public static void main(String[] args) {
        Application.launch(JfxMediaHandApplication.class, args);
    }

}
