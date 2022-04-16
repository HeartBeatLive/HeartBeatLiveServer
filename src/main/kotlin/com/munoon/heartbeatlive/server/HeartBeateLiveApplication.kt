package com.munoon.heartbeatlive.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HeartBeateLiveApplication

fun main(args: Array<String>) {
	runApplication<HeartBeateLiveApplication>(*args)
}
