package com.jysohn0825.point.support.key

/** 분산 환경에서 name(채번 대상 네임스페이스) 기준으로 단조 증가하는 고유 키를 채번하는 포트. */
interface DistributedKeyGenerator {
    fun next(name: String): Long

    companion object {
        const val EARNING_KEY_NAME: String = "point-earning"
    }
}
