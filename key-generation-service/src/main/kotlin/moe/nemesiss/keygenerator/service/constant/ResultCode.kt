package moe.nemesiss.keygenerator.service.constant

object Join {
    const val NO_NAMESPACE = -1
    const val INVALID_GROUP_ID = -2
    const val JOIN_OK = 0
    const val WAITING_REBALANCED = 1
}


object Pong {
    const val OK = 0

    // server-size failed-code
    const val EPOCH_OUT_OF_SYNC = 1
    const val NOT_JOINED = 2

    // local-size failed-code
    const val EXCEPTION_THROWN = 3
    const val TIME_OUT = 4
    const val NOT_IN_RUNNING_STATE = 5
}