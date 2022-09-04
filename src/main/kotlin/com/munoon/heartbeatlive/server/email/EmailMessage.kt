package com.munoon.heartbeatlive.server.email

sealed interface EmailMessage {
    val email: String
}

data class SubscriptionInvoicePaidEmailMessage(
    override val email: String
) : EmailMessage

data class SubscriptionInvoiceComingEmailMessage(
    override val email: String
) : EmailMessage

data class InvoiceFailedEmailMessage(
    override val email: String
) : EmailMessage

data class SubscriptionInvoiceSuccessfullyRefundedEmailMessage(
    override val email: String
) : EmailMessage

data class SubscriptionInvoiceFailedToRefundedEmailMessage(
    override val email: String
) : EmailMessage

data class ResetPasswordEmailMessage(
    override val email: String,
    val resetPasswordLink: String
) : EmailMessage