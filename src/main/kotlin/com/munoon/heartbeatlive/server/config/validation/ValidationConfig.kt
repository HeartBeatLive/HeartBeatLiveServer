package com.munoon.heartbeatlive.server.config.validation

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.validation.MessageInterpolatorFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Role
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean

@Configuration(proxyBeanMethods = false)
class ValidationConfig {
    @Bean
    @Primary
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    fun defaultValidator(): LocalValidatorFactoryBean {
        val factoryBean = CustomLocalValidatorFactoryBean()
        factoryBean.messageInterpolator = MessageInterpolatorFactory().getObject()
        return factoryBean
    }
}