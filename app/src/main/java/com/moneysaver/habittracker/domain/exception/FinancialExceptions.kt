package com.moneysaver.habittracker.domain.exception

class InsufficientBalanceException(message: String) : Exception(message)

class EntityNotFoundException(message: String) : Exception(message)

class InvalidTransactionException(message: String) : Exception(message)
