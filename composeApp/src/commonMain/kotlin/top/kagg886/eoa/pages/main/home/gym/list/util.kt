package top.kagg886.eoa.pages.main.home.gym.list

import top.kagg886.eoa.gym.GymSummary

fun GymSummary.asDisplayName(): String = if (id.endsWith("学年")) id else "${id}学年"
