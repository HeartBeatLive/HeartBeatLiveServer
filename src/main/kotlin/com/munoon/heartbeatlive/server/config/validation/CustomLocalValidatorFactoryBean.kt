package com.munoon.heartbeatlive.server.config.validation

import org.hibernate.validator.internal.engine.DefaultClockProvider
import org.springframework.core.LocalVariableTableParameterNameDiscoverer
import org.springframework.core.PrioritizedParameterNameDiscoverer
import org.springframework.core.StandardReflectionParameterNameDiscoverer
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import javax.validation.ClockProvider
import javax.validation.Configuration
import javax.validation.ParameterNameProvider

class CustomLocalValidatorFactoryBean : LocalValidatorFactoryBean() {
    override fun getClockProvider(): ClockProvider = DefaultClockProvider.INSTANCE

    override fun postProcessConfiguration(configuration: Configuration<*>) {
        super.postProcessConfiguration(configuration)

        val discoverer = PrioritizedParameterNameDiscoverer()
        discoverer.addDiscoverer(SuspendAwareKotlinParameterNameDiscoverer())
        discoverer.addDiscoverer(StandardReflectionParameterNameDiscoverer())
        discoverer.addDiscoverer(LocalVariableTableParameterNameDiscoverer())

        val defaultProvider = configuration.defaultParameterNameProvider
        configuration.parameterNameProvider(object : ParameterNameProvider {
            override fun getParameterNames(constructor: Constructor<*>): List<String> {
                val paramNames: Array<String>? = discoverer.getParameterNames(constructor)
                return paramNames?.toList() ?: defaultProvider.getParameterNames(constructor)
            }

            override fun getParameterNames(method: Method): List<String> {
                val paramNames: Array<String>? = discoverer.getParameterNames(method)
                return paramNames?.toList() ?: defaultProvider.getParameterNames(method)
            }
        })
    }
}