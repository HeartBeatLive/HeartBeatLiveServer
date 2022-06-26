Most of all errors also contains `message`, `extensions.classification`, `extensions.code`, `locations` and `path` fields,
but none of these fields are guaranteed.
Some errors contains additional fields in `extensions`, that are useful for error details recognition.

#### Error Classifications
| Name | Description |
| ---- | ----------- |
| BAD_REQUEST | Client send incorrect request. For example, client provided invalid argument value. |
| UNAUTHORIZED | User is unauthorized. |
| FORBIDDEN | Access is forbidden. For example, user do not have access to some method. |
| NOT_FOUND | Something is not found. |
| INTERNAL_ERROR | Some error occurs on server side. |

#### Error Codes
| Name | Description |
| ---- | ----------- |
| common.validation | Request validation was not passed. |
| common.access_denied | User do not have access to something. |
| common.unknown | Unknown exception happened. |
| heart_beat_sharing.not_found.by_public_code | Heart beat sharing is not found by sharing code. |
| heart_beat_sharing.not_found.by_id | Heart beat sharing is not found by identifier. |
| heart_beat_sharing.limit_exceeded | Heart beats sharing count limit is exceeded. |
| heart_beat_sharing.expired | Heart beat sharing is expired. |
| subscription.subscribers_limit_exceeded | User have too many subscribers. |
| subscription.subscriptions_limit_exceeded | User have too many subscriptions. |
| subscription.self_subscribe | User is trying to subscribe to himself. |
| subscription.not_found.by_id | Subscription is not found by specified identifier. |
| ban.banned | User was banned by another user. |
| ban.not_found.by_id | User ban was not found by specified identifier. |
| push_notification.not_found.by_id | Push notification with specified identifier is not found. |
| payment.provider.not_found | No active payment provider found. |
| payment.provider.unsupported | Payment provider that you are currently using is unsupported. |
| account_subscription.subscription_plan.not_found.by_code_name | Account subscription plan is not found by specified code name. |
| account_subscription.subscription_plan.price.not_found.by_id | Account subscription plan price is not found by specified identifier. |