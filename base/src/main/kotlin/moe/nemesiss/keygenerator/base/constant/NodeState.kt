package moe.nemesiss.keygenerator.base.constant

/**
 * State transformation graph:
 *
 *                        ---------------------------------- timeout after 30s
 *                        |                                        ↑
 *                        ↓      (receive REBALANCING)             |
 * STARTING ---------> JOINING ---------------------------> WAITING_REBALANCED ------ invalid groupId -----> FATAL
 *                       ↑  |                                      |
 *   (invalid heartbeat) |  | (receive JOIN_OK)                    | (receive join result)
 *                       |  ↓                                      ↓
 *                     RUNNING  <-----------------------------------
 */
enum class NodeState(val code: Int) {
    STARTING(0),
    JOINING(1),
    WAITING_REBALANCED(2),
    RUNNING(3),
    FATAL(5);

    companion object {
        fun code(code: Int): NodeState {
            return values().first { s -> s.code == code }
        }
    }
}
