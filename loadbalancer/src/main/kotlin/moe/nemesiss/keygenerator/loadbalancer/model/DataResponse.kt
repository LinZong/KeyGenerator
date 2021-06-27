package moe.nemesiss.keygenerator.loadbalancer.model

class DataResponse<T>(code: Int, message: String? = null, val data: T? = null) : Response(code, message)