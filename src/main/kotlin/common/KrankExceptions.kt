package com.github.abyssnlp.common

object KrankExceptions {
    class MetricsCollectionException(message: String) : RuntimeException(message)
    class MetricsCollectorNotImplementedException(message: String) : RuntimeException(message)
    class ResourceTypeNotSupportedException(message: String) : RuntimeException(message)
}
