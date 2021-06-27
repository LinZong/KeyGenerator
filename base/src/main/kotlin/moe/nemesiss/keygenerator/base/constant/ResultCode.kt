package moe.nemesiss.keygenerator.base.constant

object Join {
    // server-side
    // 无命名空间
    const val NO_NAMESPACE = -1
    // 有命名空间，但是groupId错
    const val INVALID_GROUP_ID = -2
    // 加入成功
    const val JOIN_OK = 0
    // 等待重平衡
    const val WAITING_REBALANCED = 1
    // 通知其他机器接受重平衡
    const val REBALANCE_NOTIFY = 2
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


object GetKey {
    const val OK = 0
    const val NOT_IN_RUNNING_STATE = 1
    const val INTERNAL_ERROR = 2
    const val TIME_OUT = 4
}