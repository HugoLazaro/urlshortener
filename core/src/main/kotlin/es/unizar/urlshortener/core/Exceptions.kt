package es.unizar.urlshortener.core

class InvalidUrlException(val url: String) : Exception("[$url] does not follow a supported schema")

class RedirectionNotFound(val key: String) : Exception("[$key] is not known")

class UrlNotSafeException(val url: String) : Exception("[$url] is a not safe Url")

class UrlNotReachableException(val url: String) : Exception("[$url] is a not reachable Url")

class HashUsedException(val hash: String) : Exception("[$hash] is already mapped, so cant be mapped again")