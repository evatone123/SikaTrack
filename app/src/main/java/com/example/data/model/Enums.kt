package com.example.data.model

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class AccountType(val displayName: String) {
    CASH("Cash"),
    BANK("Bank Account"),
    MOBILE_MONEY("Mobile Money"),
    SAVINGS("Savings"),
    CREDIT("Credit"),
    OTHER("Other")
}

enum class CategoryType {
    EXPENSE,
    INCOME
}

enum class DateFilterPeriod(val displayName: String) {
    ALL("All Time"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom")
}

enum class SortOrder(val displayName: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    HIGHEST_AMOUNT("Highest Amount"),
    LOWEST_AMOUNT("Lowest Amount")
}
