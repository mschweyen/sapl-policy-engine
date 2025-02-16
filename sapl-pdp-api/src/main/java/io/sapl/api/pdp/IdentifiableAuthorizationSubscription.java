/*
 * Copyright © 2021 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.api.pdp;

import lombok.NonNull;
import lombok.Value;

/**
 * Holds an {@link AuthorizationSubscription SAPL authorization subscription} together
 * with an ID used to identify the authorization subscription and to assign the
 * authorization subscription its corresponding {@link AuthorizationDecision SAPL
 * authorization decision}.
 *
 * @see AuthorizationSubscription
 * @see IdentifiableAuthorizationDecision
 */
@Value
public class IdentifiableAuthorizationSubscription {

	@NonNull
	String authorizationSubscriptionId;

	@NonNull
	AuthorizationSubscription authorizationSubscription;

}
